package org.intermine.api.template;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.iharder.Base64;

import org.apache.log4j.Logger;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.TemplateSummary;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.template.TemplateQuery;

/**
 * This class manages summaries of possible values for editable constraints for template queries.
 *
 * @author Matthew Wakeling
 */
public class TemplateSummariser
{
    private static final Logger LOG = Logger.getLogger(TemplateSummariser.class);
    protected ObjectStore os;
    protected ObjectStoreWriter osw;
    protected Map<TemplateQuery, HashMap<String, List<Object>>> possibleValues
        = new IdentityHashMap<TemplateQuery, HashMap<String, List<Object>>>();
    protected final int maxSummaryValues = 200;

    /**
     * Construct a TemplateSummariser.
     *
     * @param os ObjectStore containing production data
     * @param osw ObjectStoreWriter containing ProfileManager data
     * @param oss A summary of the ObjectStore - will be null
     */
    public TemplateSummariser(ObjectStore os, ObjectStoreWriter osw, ObjectStoreSummary oss) {
        this.os = os;
        this.osw = osw;
    }

    /**
     * Populates the possibleValues data for a given TemplateQuery from the os.
     *
     * @param templateQuery a TemplateQuery to summarise
     * @throws ObjectStoreException if something goes wrong
     */
    public void summarise(ApiTemplate templateQuery) throws ObjectStoreException {
        HashMap<String, List<Object>> templatePossibleValues = possibleValues.get(templateQuery);
        if (templatePossibleValues == null) {
            templatePossibleValues = new HashMap<String, List<Object>>();
            possibleValues.put(templateQuery, templatePossibleValues);
        }
        for (String node : templateQuery.getEditablePaths()) {
            Path path;
            try {
                path = templateQuery.makePath(node);
            } catch (PathException e) {
                throw new ObjectStoreException(e);
            }
            Query q = TemplatePrecomputeHelper.getPrecomputeQuery(templateQuery, null, node);
            LOG.info("Summarising template " + templateQuery.getName() + " by running query: " + q);
            List<ResultsRow<Object>> results = os.execute(q, 0, maxSummaryValues, true, false,
                    ObjectStore.SEQUENCE_IGNORE);
            if (results.size() < maxSummaryValues) {
                if (path.endIsAttribute() || results.isEmpty()) {
                    List<Object> values = new ArrayList<Object>();
                    for (ResultsRow<Object> row : results) {
                        values.add(row.get(0));
                    }
                    templatePossibleValues.put(node, values);
                } else {
                    LOG.warn("Editable node " + node + " in template " + templateQuery.getName()
                            + " cannot be summarised as it is a LOOKUP "
                            + "constraint, although it has only " + results.size()
                            + " possible values. Consider changing the node that the constraint is "
                            + "attached to");
                }
            }
        }
        // Now write the summary to the user profile database.
        try {
            osw.beginTransaction();
            SavedTemplateQuery savedTemplateQuery = templateQuery.getSavedTemplateQuery();
            if (savedTemplateQuery != null) {
                Query q = new Query();
                QueryClass qc = new QueryClass(TemplateSummary.class);
                q.addFrom(qc);
                q.addToSelect(qc);
                q.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "template"),
                            ConstraintOp.CONTAINS, savedTemplateQuery));
                for (Object old : osw.getObjectStore().executeSingleton(q)) {
                    osw.delete((TemplateSummary) old);
                }
            }
            TemplateSummary templateSummary = new TemplateSummary();
            templateSummary.setTemplate(savedTemplateQuery);
            String summaryText = Base64.encodeObject(templatePossibleValues);
            if (summaryText == null) {
                throw new RuntimeException("Serialised summary is null");
            }
            templateSummary.setSummary(summaryText);
            osw.store(templateSummary);
        } catch (ObjectStoreException e) {
            if (osw.isInTransaction()) {
                osw.abortTransaction();
            }
            LOG.error("ObjectStoreException while storing summary for " + templateQuery.getName());
            throw e;
        } catch (RuntimeException e) {
            if (osw.isInTransaction()) {
                osw.abortTransaction();
            }
            LOG.error("ObjectStoreException while storing summary for " + templateQuery.getName());
            throw e;
        } finally {
            if (osw.isInTransaction()) {
                osw.commitTransaction();
            }
        }
    }

    /**
     * Returns true if the given template has been summarised.
     *
     * @param templateQuery a TemplateQuery
     * @return a boolean
     */
    public boolean isSummarised(ApiTemplate templateQuery) {
        return getPossibleValues(templateQuery) != null;
    }

    /**
     * Returns the possible values that a particular path in a template query can take,
     * for dropdowns on template forms.
     *
     * @param templateQuery a TemplateQuery
     * @param path the path that is being constrained
     * @return a List of possible values
     */
    public List<Object> getPossibleValues(ApiTemplate templateQuery, String path) {
        Map<String, List<Object>> templatePossibleValues = getPossibleValues(templateQuery);
        if (templatePossibleValues != null) {
            return templatePossibleValues.get(path);
        }
        return null;
    }

    /**
     * Returns a Map of the possible values for editable nodes on a template query.
     *
     * @param templateQuery a TemplateQuery
     * @return a Map from String path to List
     */
    public Map<String, List<Object>> getPossibleValues(ApiTemplate templateQuery) {
        HashMap<String, List<Object>> templatePossibleValues = possibleValues.get(templateQuery);
        if (templateQuery != null && templatePossibleValues == null) {
            SavedTemplateQuery template = templateQuery.getSavedTemplateQuery();
            if (template != null) {
                try {
                    Iterator<TemplateSummary> summaryIter = template.getSummaries().iterator();
                    if (summaryIter.hasNext()) {
                        TemplateSummary summary = summaryIter.next();
                        templatePossibleValues = (HashMap<String, List<Object>>) Base64
                            .decodeToObject(summary.getSummary());
                    }
                } catch (Exception err) {
                    // Ignore rows that don't unmarshal (they probably reference
                    // another model.
                    LOG.warn("Failed to unmarshal saved template query: "
                             + template.getTemplateQuery(), err);
                }
            }
            possibleValues.put(templateQuery, templatePossibleValues);
        }
        return templatePossibleValues;
    }
}
