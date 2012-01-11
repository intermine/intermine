package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 *
 * @author radek
 *
 */
public class MetabolicGeneSummaryDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(MetabolicGeneSummaryDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public MetabolicGeneSummaryDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        GeneSummary summary = new GeneSummary(reportObject.getObject(), request);

        // 1. Pathways count
        summary.addCollectionCount("Pathways", "description", "pathways", "pathways");
        // 2. Diseases count
        summary.addCollectionCount("Diseases", "description", "diseases", "diseases");
        // 3. Mouse Alleles count
        summary.addCollectionCount("Mouse Alleles", "description",
                allelesPathQuery(summary.getNewPathQuery(),
                summary.getObjectId()), "alleles");
        // 4. GOTerm count
        summary.addCollectionCount("Gene Ontology", "description", "goAnnotation",
                "GeneOntologyDisplayer");

        request.setAttribute("summary", summary);
    }

    /**
     * EMTAB-62 link generator from ebi.ac.uk
     * @param primaryId
     * @return
     * @deprecated because the image is too big
     */
    @SuppressWarnings("unused")
    @java.lang.Deprecated
    private String emtabExpression(String primaryId) {
        if (primaryId != null) {
            return "http://www.ebi.ac.uk/gxa/webanatomogram/" + primaryId + ".png";
        }
        return null;
    }

    /**
     * Generate PathQuery to Mousey Alleles
     * @param query
     * @param objectId
     * @return
     */
    private PathQuery allelesPathQuery(PathQuery query, Integer objectId) {
        query.addViews("Gene.homologues.homologue.alleles.primaryIdentifier");
        query.addConstraint(Constraints.eq("Gene.homologues.homologue.organism.shortName",
                "M. musculus"), "A");
        query.addConstraint(Constraints.eq("Gene.id", objectId.toString()), "B");
        query.setConstraintLogic("A and B");

        return query;
    }

    /**
     * Generate PathQuery to GOTerms
     * @param query
     * @param objectId
     * @return
     */
    @SuppressWarnings("unused")
    private PathQuery goTermPathQuery(PathQuery query, Integer objectId) {
        query.addViews("Gene.goAnnotation.ontologyTerm.namespace");
        query.addOrderBy("Gene.goAnnotation.ontologyTerm.namespace", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Gene.id", objectId.toString()));

        return query;
    }

    /**
     *
     * Internal wrapper.
     * @author radek
     *
     */
    public class GeneSummary
    {
        private InterMineObject imObj;
        private HttpServletRequest request;
        private PathQueryExecutor executor;
        private Model model = null;
        private LinkedHashMap<String, HashMap<String, Object>> storage;

        /**
         *
         * @param imObj InterMineObject
         * @param request Request
         */
        public GeneSummary(InterMineObject imObj, HttpServletRequest request) {
            this.imObj = imObj;
            this.request = request;
            storage = new LinkedHashMap<String, HashMap<String, Object>>();
        }

        /**
         * Add collection count to the summary.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param param can be a fieldName or a PathQuery
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         */
        public void addCollectionCount(String key, String description, Object param,
                String anchor) {
            if (param instanceof PathQuery) {
                try {
                    storage.put(key, createWrapper("integer", executor.count((PathQuery)
                            param), anchor, description));
                } catch (ObjectStoreException e) {
                    LOG.error("Problem running PathQuery " + e.toString());
                }
            } else if (param instanceof String) {
                Collection<?> coll = null;
                try {
                    if ((coll = (Collection<?>) imObj.getFieldValue((String) param)) != null) {
                        storage.put(key, createWrapper("integer", coll.size(), anchor,
                                description));
                    }
                } catch (IllegalAccessException e) {
                    LOG.error("The field " + param + " does not exist");
                }
            } else {
                storage.put(key, createWrapper("unknown", param, anchor, description));
            }
        }

        private HashMap<String, Object> createWrapper(String type, Object data, String anchor,
                String description) {
            HashMap<String, Object> inner = new HashMap<String, Object>();
            inner.put("type", type);
            inner.put("data", data);
            inner.put("anchor", anchor);
            inner.put("description", description);
            return inner;
        }

        /**
         * Add collection distinct count to the summary. Will get the distinct value referenced
         * and get their count.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param param can be a fieldName or a PathQuery
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         */
        public void addCollectionDistinctCount(String key, String description, Object param,
                String anchor) {
            if (param instanceof PathQuery) {
                ExportResultsIterator results = executor.execute((PathQuery) param);

                HashMap<String, Integer> temp = new HashMap<String, Integer>();
                while (results.hasNext()) {
                    List<ResultElement> item = results.next();
                    String value = item.get(0).getField().toString();
                    if (!temp.keySet().contains(value)) {
                        temp.put(value, 0);
                    }
                    temp.put(value, temp.get(value) + 1);
                }
                storage.put(key, createWrapper("map", temp, anchor, description));
            } else {
                storage.put(key, createWrapper("unknown", param, anchor, description));
            }
        }

        /**
         * Add a link to an image for the summary.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param link refers to the src attr of the img element
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         */
        public void addImageLink(String key, String link, String anchor, String description) {
            storage.put(key, createWrapper("image", link, anchor, description));
        }

        /**
         * Give us a new PathQuery to work on.
         * @return PathQuery
         */
        public PathQuery getNewPathQuery() {
            if (model == null) {
                HttpSession session = request.getSession();
                final InterMineAPI im = SessionMethods.getInterMineAPI(session);
                model = im.getModel();
                executor = im.getPathQueryExecutor(SessionMethods.getProfile(session));
            }
            return new PathQuery(model);
        }

        /**
         *
         * @return InterMineObject ID
         */
        public Integer getObjectId() {
            return imObj.getId();
        }

        /**
         *
         * @return ReportObject primaryIdentifier
         */
        public String getPrimaryId() {
            try {
                return (String) imObj.getFieldValue("primaryIdentifier");
            } catch (IllegalAccessException e) {
                LOG.error("The field primaryIdentifier does not exist");
            }
            return null;
        }

        /**
         *
         * @return Map of the fields configged here for the JSP to traverse
         */
        public LinkedHashMap<String, HashMap<String, Object>> getFields() {
            return storage;
        }

    }

}
