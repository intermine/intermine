package org.intermine.api.template;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathNode;

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
    protected Map<TemplateQuery, HashMap<String, List>> possibleValues
        = new IdentityHashMap<TemplateQuery, HashMap<String, List>>();

    /**
     * Construct a TemplateSummariser.
     *
     * @param os ObjectStore containing production data
     * @param osw ObjectStoreWriter containing ProfileManager data
     */
    public TemplateSummariser(ObjectStore os, ObjectStoreWriter osw) {
        this.os = os;
        this.osw = osw;
    }

    /**
     * Populates the possibleValues data for a given TemplateQuery from the os.
     *
     * @param templateQuery a TemplateQuery to summarise
     * @throws ObjectStoreException if something goes wrong
     */
    public void summarise(TemplateQuery templateQuery) throws ObjectStoreException {
        HashMap<String, List> templatePossibleValues = possibleValues.get(templateQuery);
        if (templatePossibleValues == null) {
            templatePossibleValues = new HashMap<String, List>();
            possibleValues.put(templateQuery, templatePossibleValues);
        }
        for (PathNode node : templateQuery.getEditableNodes()) {
            Query q = TemplatePrecomputeHelper.getPrecomputeQuery(templateQuery, null, node);
            LOG.info("Summarising template " + templateQuery.getName() + " by running query: " + q);
            List<ResultsRow> results = os.execute(q, 0, 20, true, false,
                    ObjectStore.SEQUENCE_IGNORE);
            if (results.size() < 20) {
                if (node.isAttribute() || results.isEmpty()) {
                    List values = new ArrayList();
                    for (ResultsRow row : results) {
                        values.add(row.get(0));
                    }
                    templatePossibleValues.put(node.getPathString(), values);
                } else {
                    LOG.warn("Editable node " + node.getPathString() + " in template "
                            + templateQuery.getName() + " cannot be summarised as it is a LOOKUP "
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
    public boolean isSummarised(TemplateQuery templateQuery) {
        return getPossibleValues(templateQuery) != null;
    }

    /**
     * Returns the possible values that a particular editable node in a template query can take,
     * for dropdowns on template forms.
     *
     * @param templateQuery a TemplateQuery
     * @param node the editable node
     * @return a List of possible values
     */
    public List getPossibleValues(TemplateQuery templateQuery, PathNode node) {
        Map<String, List> templatePossibleValues = getPossibleValues(templateQuery);
        if (templatePossibleValues != null) {
            return templatePossibleValues.get(node.getPathString());
        }
        return null;
    }

    /**
     * Returns a Map of the possible values for editable nodes on a template query.
     *
     * @param templateQuery a TemplateQuery
     * @return a Map from String path to List
     */
    public Map<String, List> getPossibleValues(TemplateQuery templateQuery) {
        HashMap<String, List> templatePossibleValues = possibleValues.get(templateQuery);
        if (templatePossibleValues == null) {
            SavedTemplateQuery template = templateQuery.getSavedTemplateQuery();
            if (template != null) {
                try {
                    Iterator summaryIter = template.getSummaries().iterator();
                    if (summaryIter.hasNext()) {
                        TemplateSummary summary = (TemplateSummary) summaryIter.next();
                        templatePossibleValues = (HashMap<String, List>) Base64
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
