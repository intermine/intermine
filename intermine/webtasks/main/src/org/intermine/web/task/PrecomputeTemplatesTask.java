package org.intermine.web.task;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplatePrecomputeHelper;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ParallelPrecomputer;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.api.template.TemplateManager;

/**
 * A Task that reads a list of queries from a properties file (eg. testmodel_precompute.properties)
 * and calls ObjectStoreInterMineImpl.precompute() using the Query.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTemplatesTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PrecomputeTemplatesTask.class);

    /**
     * The precompute category to use for templates.
     */
    public static final String PRECOMPUTE_CATEGORY_TEMPLATE = "template";

    protected String alias;
    protected int minRows = -1;
    protected ObjectStore os = null;
    protected ObjectStoreWriter userProfileOS = null;
    protected String userProfileAlias;
    protected String username;
    protected String ignore = "";
    protected Set<String> ignoreNames = new HashSet<String>();
    protected boolean doSummarise = true;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set a comma separated list of template names to ignore - i.e. not precompute.
     * @param ignore the list to ignore
     */
    public void setIgnore(String ignore) {
        this.ignore = ignore;
        LOG.info("Set templates to ignore value: " + ignore);
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
     * Set the summarise flag.
     * @param summarise if true, summarise while precomputing
     */
    public void setSummarise(String summarise) {
        if ("false".equals(summarise)) {
            doSummarise = false;
        } else {
            doSummarise = true;
        }
        LOG.info("Set summarise to " + doSummarise + " (with string " + summarise + ")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        if (minRows == -1) {
            throw new BuildException("minRows attribute is not set");
        }

        if (!StringUtils.isBlank(ignore)) {
            String[] bits = ignore.split(",");
            for (String ignoreName : bits) {
                ignoreNames.add(ignoreName.trim().toLowerCase());
            }
        }
        LOG.info("Parsed template names to ignore: " + ignoreNames);

        try {
            os = ObjectStoreFactory.getObjectStore(alias);
            if (!(os instanceof ObjectStoreInterMineImpl)) {
                throw new BuildException(alias + " isn't an ObjectStoreInterMineImpl");
            }
            userProfileOS = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
            precomputeTemplates();
        } catch (Exception err) {
            throw new BuildException("Exception creating objectstore/profile manager", err);
        } finally {
            if (userProfileOS != null) {
                try {
                    userProfileOS.close();
                } catch (ObjectStoreException e) {
                    // At this stage, we really don't care
                }
            }
        }

    }

    /**
     * Create precomputed tables for all template queries in the given ObjectStore.
     */
    protected void precomputeTemplates() {
        List<ApiTemplate> toSummarise = new ArrayList<ApiTemplate>();
        List<ParallelPrecomputer.Job> jobs = new ArrayList<ParallelPrecomputer.Job>();
        for (Map.Entry<String, ApiTemplate> entry : getPrecomputeTemplateQueries().entrySet()) {
            ApiTemplate template = entry.getValue();

            // check if we should ignore this template (maybe it won't precompute)
            if (ignoreNames.contains(template.getName().toLowerCase())) {
                LOG.warn("template was in ignore list: " + template.getName());
                continue;
            }

            // if the template isn't valid according to the current model, log it and move on
            if (!template.isValid()) {
                LOG.warn("template does not validate against the model: " + template.getName());
                List<String> problems = template.verifyQuery();
                for (String problem : problems) {
                    LOG.warn("Problem with " + template.getName() + ": " + problem);
                }
                continue;
            }

            List<QueryNode> indexes = new ArrayList<QueryNode>();
            Query q = TemplatePrecomputeHelper.getPrecomputeQuery(template, indexes, null);

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

            toSummarise.add(template);

            jobs.add(new ParallelPrecomputer.Job(template.getName(), q, indexes, false,
                        PRECOMPUTE_CATEGORY_TEMPLATE));
        }
        ParallelPrecomputer pp = new ParallelPrecomputer((ObjectStoreInterMineImpl) os, 4);
        try {
            pp.precompute(jobs);
        } catch (ObjectStoreException e) {
            throw new BuildException(e);
        }
        // TODO:  don't require servlet context to create oss, we can't get it here yet
        ObjectStoreSummary oss = null;
        TemplateSummariser summariser = new TemplateSummariser(os, userProfileOS, oss);
        for (ApiTemplate template : toSummarise) {
            if (doSummarise) {
                try {
                    summariser.summarise(template);
                } catch (ObjectStoreException e) {
                    LOG.error("Exception while summarising template " + template.getName(), e);
                }
            }
        }
    }

    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     * @param query the query to precompute
     * @param indexes the index QueryNodes
     * @param name the name of the query we are precomputing (used for documentation is an exception
     * is thrown
     * @throws BuildException if the query cannot be precomputed.
     */
    protected void precompute(Query query, Collection<QueryNode> indexes, String name) {
        long start = System.currentTimeMillis();

        try {
            ObjectStoreInterMineImpl osInterMineImpl = ((ObjectStoreInterMineImpl) os);
            if (!osInterMineImpl.isPrecomputed(query, PRECOMPUTE_CATEGORY_TEMPLATE)) {
                osInterMineImpl.precompute(query, indexes,
                                                       PRECOMPUTE_CATEGORY_TEMPLATE);
            } else {
                LOG.info("Skipping template " + name + " - already precomputed.");
            }
        } catch (ObjectStoreException e) {
            LOG.error("Exception while precomputing query: " + name + ", " + query
                    + " with indexes " + indexes, e);
        }

        LOG.info("precompute(indexes) of took "
                 + (System.currentTimeMillis() - start) / 1000
                 + " seconds for: " + query);
    }

    /**
     * Get the super user's public templates
     * @return Map from template name to TemplateQuery
     * @throws BuildException if an IO error occurs loading the template queries
     */
    protected Map<String, ApiTemplate> getPrecomputeTemplateQueries() {
        ProfileManager pm = new ProfileManager(os, userProfileOS);
        Profile profile = pm.getSuperuserProfile();
        TemplateManager templateManager = new TemplateManager(profile, os.getModel());
        return templateManager.getGlobalTemplates(true);
    }
}
