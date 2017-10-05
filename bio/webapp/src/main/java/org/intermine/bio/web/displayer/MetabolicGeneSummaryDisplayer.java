package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
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
        summary.addCollectionCount("Pathways", "Reactome, KEGG", "pathways", "pathways");
        // 2. Diseases count
        summary.addCollectionCount("Diseases", "OMIM", "diseases", "diseases");
        // 3. Mouse Alleles count
        if (summary.isThisAMouser()) {
            summary.addCollectionCount("Mouse Alleles (MGI)", "mouse alleles", "alleles",
                    "MouseAllelesDisplayer");
        } else {
            summary.addCollectionCount("Mouse Alleles (MGI)", "mouse alleles",
                    allelesPathQuery(summary.getObjectId()), "MouseAllelesDisplayer");
        }
        // 4. GOTerm count
        summary.addCollectionCount("Gene Ontology", "&nbsp;",
                goTermPathQuery(summary.getObjectId()), "GeneOntologyDisplayer");

        // on sapien pages:
        if (summary.isThisAHuman()) {
            // ArrayExpress Gene Expression Tissues & Tissues
            ArrayList arr = new ArrayList();
            arr.add(this.arrayAtlasExpressionTissues(summary));
            arr.add(this.arrayAtlasExpressionDiseases(summary));
            summary.addCustom("Expression", "Array Express (E-MTAB 62)",
                    arr, "GeneExpressionAtlasTissuesDisplayer",
                    "metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp");
        }

        request.setAttribute("summary", summary);
    }


    private Object arrayAtlasExpressionTissues(GeneSummary summary) {
        PathQuery query = new PathQuery(im.getModel());
        query.addViews("Gene.atlasExpression.expression");

        query.addOrderBy("Gene.atlasExpression.pValue", OrderDirection.ASC);

        query.addConstraint(Constraints.eq("Gene.id", summary.getObjectId().toString()), "A");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.pValue", "1E-4"), "B");
        query.addConstraint(Constraints.eq("Gene.atlasExpression.type", "organism_part"), "D");
        query.addConstraint(Constraints.greaterThan("Gene.atlasExpression.tStatistic", "4"), "E");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.tStatistic", "-4"), "F");
        query.addConstraint(Constraints.neq("Gene.atlasExpression.condition", "(empty)"), "G");
        query.setConstraintLogic("A and B and D and (E or F) and G");

        ExportResultsIterator results;
        try {
            results = summary.getExecutor().execute(query);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        Integer up = 0;
        Integer down = 0;
        while (results.hasNext()) {
            List<ResultElement> item = results.next();
            String expression = item.get(0).getField().toString();
            if ("UP".equals(expression)) {
                up += 1;
            } else {
                down += 1;
            }
        }

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("up", up);
        map.put("down", down);

        return map;
    }

    private Object arrayAtlasExpressionDiseases(GeneSummary summary) {
        PathQuery query = new PathQuery(im.getModel());
        query.addViews("Gene.atlasExpression.expression");

        query.addOrderBy("Gene.atlasExpression.pValue", OrderDirection.ASC);

        query.addConstraint(Constraints.eq("Gene.id", summary.getObjectId().toString()), "A");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.pValue", "1e-4"), "B");
        query.addConstraint(Constraints.eq("Gene.atlasExpression.type", "disease_state"), "D");
        query.addConstraint(Constraints.greaterThan("Gene.atlasExpression.tStatistic", "4"), "E");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.tStatistic", "-4"), "F");
        query.addConstraint(Constraints.neq("Gene.atlasExpression.condition", "(empty)"), "G");
        query.setConstraintLogic("A and B and D and (E or F) and G");

        ExportResultsIterator results;
        try {
            results = summary.getExecutor().execute(query);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        Integer up = 0;
        Integer down = 0;
        while (results.hasNext()) {
            List<ResultElement> item = results.next();
            String expression = item.get(0).getField().toString();
            if ("UP".equals(expression)) {
                up += 1;
            } else {
                down += 1;
            }
        }

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("up", up);
        map.put("down", down);

        return map;
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
    private PathQuery allelesPathQuery(Integer objectId) {
        PathQuery query = new PathQuery(im.getModel());
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
    private PathQuery goTermPathQuery(Integer objectId) {
        PathQuery query = new PathQuery(im.getModel());
        query.addViews("Gene.goAnnotation.ontologyTerm.name");
        query.addOrderBy("Gene.goAnnotation.ontologyTerm.name", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Gene.id", objectId.toString()));
        // parents have to be main ontology, to exclude the root terms
        query.addConstraint(Constraints.oneOfValues("Gene.goAnnotation.ontologyTerm.parents.name",
                GeneOntologyDisplayer.ONTOLOGIES));
        // not a NOT relationship
        query.addConstraint(Constraints.isNull("Gene.goAnnotation.qualifier"));
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
        private PathQueryExecutor executor = null;
        private LinkedHashMap<String, HashMap<String, Object>> storage;

        /**
         *
         * @param imObj InterMineObject
         * @param request Request
         */
        public GeneSummary(InterMineObject imObj, HttpServletRequest request) {
            this.imObj = imObj;
            storage = new LinkedHashMap<String, HashMap<String, Object>>();
            executor = im.getPathQueryExecutor(SessionMethods.getProfile(request.getSession()));
        }

        /**
         * Add a custom object to the displayer.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param data to save on the wrapper object
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         * @param jsp to include that knows how to display us
         */
        public void addCustom(String key, String description,
                Object data, String anchor, String jsp) {
            storage.put(key, createWrapper("custom", data, anchor, description, jsp));
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
                            param), anchor, description, null));
                } catch (ObjectStoreException e) {
                    LOG.error("Problem running PathQuery " + e.toString());
                }
            } else if (param instanceof String) {
                Collection<?> coll = null;
                try {
                    coll = (Collection<?>) imObj.getFieldValue(param.toString());
                    if (coll != null) {
                        storage.put(key, createWrapper("integer", coll.size(), anchor,
                                description, null));
                    }
                } catch (IllegalAccessException e) {
                    LOG.error("The field " + param + " does not exist");
                }
            } else {
                storage.put(key, createWrapper("unknown", param, anchor, description, null));
            }
        }

        private HashMap<String, Object> createWrapper(String type, Object data, String anchor,
                String description, String jsp) {
            HashMap<String, Object> inner = new HashMap<String, Object>();
            inner.put("type", type);
            inner.put("data", data);
            inner.put("anchor", anchor);
            inner.put("description", description);
            if (jsp != null) {
                inner.put("jsp", jsp);
            }
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
                ExportResultsIterator results;
                try {
                    results = executor.execute((PathQuery) param);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException(e);
                }

                HashMap<String, Integer> temp = new HashMap<String, Integer>();
                while (results.hasNext()) {
                    List<ResultElement> item = results.next();
                    String value = item.get(0).getField().toString();
                    if (!temp.keySet().contains(value)) {
                        temp.put(value, 0);
                    }
                    temp.put(value, temp.get(value) + 1);
                }
                storage.put(key, createWrapper("map", temp, anchor, description, null));
            } else {
                storage.put(key, createWrapper("unknown", param, anchor, description, null));
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
            storage.put(key, createWrapper("image", link, anchor, description, null));
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
         * @return true if we are on a mouseified gene
         */
        public Boolean isThisAMouser() {
            try {
                return "Mus".equals(((InterMineObject) imObj.getFieldValue("organism"))
                        .getFieldValue("genus"));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
        *
        * @return true if we are on a sapien gene
        */
        public Boolean isThisAHuman() {
            try {
                return "Homo".equals(((InterMineObject) imObj.getFieldValue("organism"))
                        .getFieldValue("genus"));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
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
         * @return PathQuery Executor
         */
        public PathQueryExecutor getExecutor() {
            return executor;
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
