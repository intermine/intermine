package org.intermine.api.template;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

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
                    templateQuery.getPossibleValues().put(node.getPathString(), values);
                } else {
                    LOG.error("Editable node " + node.getPathString() + " in template "
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
            String summaryText = Base64.encodeObject(templateQuery.getPossibleValues());
            if (summaryText == null) {
                throw new RuntimeException("Serialised summary is null");
            }
            templateSummary.setSummary(Base64.encodeObject(templateQuery.getPossibleValues()));
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
}
