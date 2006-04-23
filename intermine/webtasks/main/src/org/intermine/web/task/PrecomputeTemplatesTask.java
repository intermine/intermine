package org.intermine.web.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.web.MainHelper;
import org.intermine.web.PathNode;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.TemplateQuery;

/**
 * A Task that reads a list of queries from a properties file (eg. testmodel_precompute.properties)
 * and calls ObjectStoreInterMineImpl.precompute() using the Query.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTemplatesTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PrecomputeTemplatesTask.class);
    public static final String PRECOMPUTE_CATEGORY_TEMPLATE = "template";

    protected String alias;
    protected int minRows = -1;
    protected ObjectStoreSummary oss = null;
    protected ObjectStore os = null;
    protected String userProfileAlias;
    protected String username;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the alias of the userprofile object store.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Set the minimum row count for precomputed queries.  Queries that are estimated to have less
     * than this number of rows will not be precomputed.
     * @param minRows the minimum row count
     */
    public void setMinRows(Integer minRows) {
        this.minRows = minRows.intValue();
    }

    /**
     * Set the account name to laod template to.
     * @param user username to load templates into
     */
    public void setUsername(String user) {
        username = user;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        if (minRows == -1) {
            throw new BuildException("minRows attribute is not set");
        }

        ObjectStore objectStore;

        try {
            objectStore = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (!(objectStore instanceof ObjectStoreInterMineImpl)) {
            throw new BuildException(alias + " isn't an ObjectStoreInterMineImpl");
        }

        precomputeTemplates(objectStore, oss);
    }

    /**
     * Create precomputed tables for all template queries in the given ObjectStore.
     * @param os the ObjectStore to precompute in
     * @param oss the ObjectStoreSummary for os
     */
    protected void precomputeTemplates(ObjectStore os, ObjectStoreSummary oss) {
        Iterator iter = getPrecomputeTemplateQueries().entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            HashMap pathToQueryNode = new HashMap();
            TemplateQuery template = (TemplateQuery) entry.getValue();

            QueryAndIndexes qai = processTemplate(template);

            Query q = qai.getQuery();

            if (q.getConstraint() == null) {
                // see ticket #255
                LOG.warn("ignoring template \"" + template.getName()
                          + "\" because it is unconstrained");
                continue;
            }

            if (q.getConstraint() instanceof ConstraintSet) {
                if (((ConstraintSet) q.getConstraint()).getConstraints().size() == 0) {
                    // see ticket #255
                    LOG.warn("ignoring template \"" + template.getName()
                             + "\" because it is unconstrained");
                    continue;
                }
            }

            ResultsInfo resultsInfo;
            try {
                resultsInfo = os.estimate(qai.getQuery());
            } catch (ObjectStoreException e) {
                throw new BuildException("Exception while calling ObjectStore.estimate()", e);
            }

            if (resultsInfo.getRows() >= minRows) {
                LOG.info("precomputing template " + entry.getKey());
                precompute(os, qai.getQuery(), qai.getIndexes(), template.getName());
            }
        }
    }


    /**
     * For a template alter the query ready to pre-compute: remove editable constraints
     * and add editable fields to the select list.  Generate list of additional indexes
     * to create.
     * @param template the template query to alter
     * @return altered query and a list of indexes
     */
    protected QueryAndIndexes processTemplate(TemplateQuery template) {
        QueryAndIndexes qai = new QueryAndIndexes();
        HashMap pathToQueryNode = new HashMap();
        List indexes = new ArrayList();
        Query tmp = MainHelper.makeQuery(template.getQuery(), new HashMap(), pathToQueryNode);

        // find nodes with editable constraints to index and possibly add to select list
        Iterator niter = template.getNodes().iterator();
        while (niter.hasNext()) {
            PathNode node = (PathNode) niter.next();
            List ecs = template.getConstraints(node);
            if (ecs != null && ecs.size() > 0) {
                // node has editable constraints
                QueryNode qn = (QueryNode) pathToQueryNode.get(node.getPath());
                if (qn == null) {
                    throw new BuildException("no QueryNode for path " + node.getPath());
                }
                // this seems to exhibit a bug with repeated aliases in generated query
                // so add QueryField to select after creating new query
                //template.getQuery().getView().add(node.getPath());
                indexes.add(qn);
            }
        }

        // now generate query with editable constraints removed
        template.removeEditableConstraints();
        Query query = MainHelper.makeQuery(template.getQuery(), new HashMap(), new HashMap());
        qai.setQuery(query);

        // list of indexes needs to be QueryFields from generated query but list created from temp
        // query -> find equivalents from select list
        Iterator indexIter = indexes.iterator();
        while (indexIter.hasNext()) {
            QueryField oldQf = (QueryField) indexIter.next();
            QueryClass oldQc = (QueryClass) oldQf.getFromElement();
            QueryClass newQc = getQueryClassFromSet(query.getFrom(), oldQc);

            // we now have corresponding QueryClass from new query -> create QueryField
            QueryField newQf = new QueryField(newQc, oldQf.getFieldName());
            query.addToSelect(newQf);
            qai.addIndex(newQf);
        }
        return qai;
    }

    private QueryClass getQueryClassFromSet(Set set, QueryClass qc) {
        Iterator i = set.iterator();
        while (i.hasNext()) {
            QueryClass candidate = (QueryClass) i.next();
            if (sameQueryClass(candidate, qc)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean sameQueryClass(QueryClass a, QueryClass b) {
        return a.getType().equals(b.getType());
    }


    /**
     * Class to associate a query with a list of query nodes that will need to have
     * indexes created for them
     */
    protected class QueryAndIndexes
    {
        Query query = null;
        List indexes = new ArrayList();

        /**
         * add to the list of indexes
         * @param qn to add to list of indexes
         */
        public void addIndex(QueryNode qn) {
            this.indexes.add(qn);
        }

        /**
         * Return the list if indexes
         * @return list of indexes
         */
        public List getIndexes() {
            return this.indexes;
        }

        /**
         * set the query object
         * @param q the query object
         */
        public void setQuery(Query q) {
            this.query = q;
        }

        /**
         * get the query object
         * @return the query object
         */
        public Query getQuery() {
            return this.query;
        }

        /**
         * @see Object#equals
         */
        public boolean equals(Object o) {
            if (o instanceof QueryAndIndexes) {
                return query.equals(((QueryAndIndexes) o).query)
                    && indexes.equals(((QueryAndIndexes) o).indexes);
            }
            return false;
        }

        /**
         * @see Object#hashCode
         */
        public int hashCode() {
            return 3 * query.hashCode() + 7 * indexes.hashCode();
        }

        /**
         * @see Object#toString
         */
        public String toString() {
            return "query: " + query.toString() + ", indexes: " + indexes.toString();
        }
    }


    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     * @param os the ObjectStore to call precompute() on
     * @param query the query to precompute
     * @param indexes the index QueryNodes
     * @throws BuildException if the query cannot be precomputed.
     */
    protected void precompute(ObjectStore os, Query query, Collection indexes,
                              String name) throws BuildException {
        long start = System.currentTimeMillis();

        try {
            ((ObjectStoreInterMineImpl) os).precompute(query, indexes,
                                                       PRECOMPUTE_CATEGORY_TEMPLATE);
        } catch (ObjectStoreException e) {
            throw new BuildException("Exception while precomputing query: " + name
                                     + ", " + query + " with indexes " + indexes, e);
        }

        LOG.info("precompute(indexes) of took "
                 + (System.currentTimeMillis() - start) / 1000
                 + " seconds for: " + query);
    }

    /**
     * Get the built-in template queries.
     * @return Map from template name to TemplateQuery
     * @throws BuildException if an IO error occurs loading the template queries
     */
    protected Map getPrecomputeTemplateQueries() throws BuildException {
        ObjectStore os;
        ProfileManager pm;
        ObjectStoreWriter userProfileOS;
        try {
            os = ObjectStoreFactory.getObjectStore(alias);
            userProfileOS = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
            pm = new ProfileManager(os, userProfileOS);
        } catch (Exception err) {
            throw new BuildException("Exception creating objectstore/profile manager", err);
        }
        if (!pm.hasProfile(username)) {
            throw new BuildException("user profile doesn't exist for " + username);
        } else {
            LOG.warn("Profile for " + username + ", clearing template queries");
            Profile profile = pm.getProfile(username, pm.getPassword(username));
            return profile.getSavedTemplates();
        }
    }
}
