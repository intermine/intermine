package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

/**
 * @author "Xavier Watkins"
 * @author Daniela Butano
 *
 */
public class EnrichmentWidget extends Widget
{
    private static final Logger LOG = Logger.getLogger(EnrichmentWidget.class);
    private InterMineBag populationBag;
    private String filter;
    private EnrichmentResults results;
    private String errorCorrection;
    private double max = 0.05d;
    private boolean extraCorrectionCoefficient = false;
    private CorrectionCoefficient correctionCoefficient = null;
    private EnrichmentWidgetImplLdr ldr;
    private String pathConstraint;
    private ClassDescriptor typeDescriptor;
    private String ids;
    private String populationIds;

    /**
     * @param config widget config
     * @param interMineBag bag for this widget
     * @param populationBag the reference population
     * @param os object storegene
     * @param options the options for this widget.
     * @param ids list of IDs to analyse, use instead of intermine bag
     * @param populationIds use instead of populationBag
     *
     */
    public EnrichmentWidget(EnrichmentWidgetConfig config,
                            InterMineBag interMineBag,
                            InterMineBag populationBag,
                            ObjectStore os,
                            EnrichmentOptions options,
                            String ids,
                            String populationIds) {
        super(config);
        this.bag = interMineBag;
        this.populationBag = populationBag;
        this.os = os;
        this.typeDescriptor = os.getModel().getClassDescriptorByName(config.getTypeClass());
        this.errorCorrection = options.getCorrection();
        this.max = options.getMaxPValue();
        this.filter = options.getFilter();
        this.ids = ids;
        this.populationIds = populationIds;

        if (bag != null) {
            validateBagType();
        }
        String correctionCoefficientClassName = (config.getCorrectionCoefficient() != null)
                                               ? config.getCorrectionCoefficient().trim()
                                               : "";
        if (!correctionCoefficientClassName.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(correctionCoefficientClassName);
                Constructor<?> c = clazz.getConstructor(new Class[] {WidgetConfig.class,
                    ObjectStore.class, InterMineBag.class});
                correctionCoefficient =  (CorrectionCoefficient) c.newInstance(new Object[] {
                    config, os, bag});
                this.extraCorrectionCoefficient = correctionCoefficient
                    .isSelected(options.getExtraCorrectionCoefficient());
            } catch (ClassNotFoundException cnfe) {
                LOG.error(cnfe);
            } catch (Exception e) {
                LOG.error(e);
            }
        }
    }


    /** @param filter Set the filter to something else **/
    public void setFilter(String filter) {
        checkNotProcessed();
        this.filter = filter;
    }

    private void checkProcessed() {
        if (ldr == null) {
            throw new IllegalStateException("This widget has not been processed yet.");
        }
    }

    private void checkNotProcessed() {
        if (ldr != null) {
            throw new IllegalStateException("This widget has already been processed.");
        }
    }

    /**
     * Validate the bag type using the attribute typeClass set in the config file.
     * Throws a ResourceNotFoundException if it's not valid
     */
    private void validateBagType() {
        ClassDescriptor bagType = os.getModel().getClassDescriptorByName(bag.getType());
        if (bagType == null) {
            throw new IllegalArgumentException("This bag has a type not found in the current "
                                              + "model: " + bag.getType());
        }
        if ("InterMineObject".equals(typeDescriptor.getName())) {
            return; // This widget accepts anything, however useless.
        } else if (bagType.equals(typeDescriptor)) {
            return; // Exact match.
        } else if (bagType.getAllSuperDescriptors().contains(typeDescriptor)) {
            return; // Sub-class.
        }
        throw new IllegalArgumentException(
            String.format("The %s enrichment query only accepts lists of %s, but you provided a "
                + "list of %s ", config.getId(), config.getTypeClass(), bag.getType()));
    }

    @Override
    public void process() {
        checkNotProcessed();
        try {
            ldr = new EnrichmentWidgetImplLdr(bag, populationBag, os,
                  (EnrichmentWidgetConfig) config, filter, extraCorrectionCoefficient,
                  correctionCoefficient, ids, populationIds);
            EnrichmentInput input = new EnrichmentInputWidgetLdr(os, ldr);
            results = EnrichmentCalculation.calculate(input, max, errorCorrection,
                                           extraCorrectionCoefficient, correctionCoefficient);
            int size = 0;
            if (bag != null) {
                size = bag.getSize();
            } else if (ids != null) {
                String[] idArray = ids.split(",");
                size = idArray.length;
            }
            setNotAnalysed(size - results.getAnalysedTotal());
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getHasResults() {
        checkProcessed();
        return results.getPValues().size() > 0;
    }

    private Map<String, List<String>> getTermsToIdsForExport(List<String> selectedIds)
        throws Exception {
        checkProcessed();
        Query q = ldr.getExportQuery(selectedIds);

        Results res = os.execute(q);
        Iterator<?> iter = res.iterator();
        HashMap<String, List<String>> termsToIds = new HashMap<String, List<String>>();
        while (iter.hasNext()) {
            @SuppressWarnings("rawtypes")
            ResultsRow resRow = (ResultsRow) iter.next();
            String termId = resRow.get(0).toString();
            String id = resRow.get(1).toString();
            if (!termsToIds.containsKey(termId)) {
                termsToIds.put(termId, new ArrayList<String>());
            }
            termsToIds.get(termId).add(id);
        }

        return termsToIds;
    }

    @Override
    public List<List<String>> getExportResults(String[] selected) throws Exception {
        checkProcessed();
        Map<String, BigDecimal> pValues = results.getPValues();
        Map<String, String> labels = results.getLabels();
        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);

        Map<String, List<String>> termsToIds = getTermsToIdsForExport(selectedIds);

        for (String id : selectedIds) {
            if (labels.get(id) != null) {

                List<String> row = new LinkedList<String>();
                row.add(id);
                String label = labels.get(id);
                if (!label.equals(id)) {
                    row.add(label);
                }

                BigDecimal bd = pValues.get(id);
                row.add(new Double(bd.doubleValue()).toString());

                List<String> termIds = termsToIds.get(id);
                StringBuffer sb = new StringBuffer();
                for (String term : termIds) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(term);
                }
                row.add(sb.toString());

                exportResults.add(row);
            }
        }
        return exportResults;
    }

    @Override
    public List<List<Object>> getResults() throws Exception {
        checkProcessed();
        List<List<Object>> exportResults = new LinkedList<List<Object>>();
        if (results != null) {
            Map<String, BigDecimal> pValues = results.getPValues();
            Map<String, Integer> counts = results.getCounts();
            Map<String, String> labels = results.getLabels();
            for (String id : pValues.keySet()) {
                List<Object> row = new LinkedList<Object>();
                row.add(id);
                row.add(labels.get(id));
                row.add(pValues.get(id).doubleValue());
                row.add(counts.get(id));
                exportResults.add(row);
            }
        }
        return exportResults;
    }

    /**
     * Returns the pathConstraint based on the enrichmentIdentifier will be applied on the pathQUery
     * @return the pathConstraint generated
     */
    public String getPathConstraint() {
        return pathConstraint;
    }

    /**
     * Returns the pathquery based on the views set in config file and the bag constraint
     * Executed when the user selects any item in the matches column in the enrichment widget.
     * @return the query generated
     */
    public PathQuery getPathQuery() {
        PathQuery q = createPathQueryView(os, config);
        // bag constraint
        if (bag != null) {
            q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        } else if (ids != null) {
            // use list of IDs instead of bag
            String[] idStrings = ids.split(",");
            List<Integer> intermineIds = new ArrayList<Integer>();
            for (int i = 0; i < idStrings.length; i++) {
                try {
                    intermineIds.add(Integer.parseInt(idStrings[i]));
                } catch (NumberFormatException e) {
                    LOG.error("bad IDs for list in enrichment.", e);
                    return null;
                }
            }
            q.addConstraint(Constraints.inIds(config.getStartClass(), intermineIds));
        }
        //constraints for view (bdgp_enrichment)
        List<PathConstraint> pathConstraintsForView =
                ((EnrichmentWidgetConfig) config).getPathConstraintsForView();
        if (pathConstraintsForView != null) {
            for (PathConstraint pc : pathConstraintsForView) {
                q.addConstraint(pc);
            }
        }

        //add type constraints for subclasses
        String enrichIdentifier = ((EnrichmentWidgetConfig) config).getEnrichIdentifier();
        boolean subClassContraint = false;
        String subClassType = "";
        String subClassPath = "";
        if (enrichIdentifier != null && !"".equals(enrichIdentifier)) {
            enrichIdentifier = config.getStartClass() + "."
                + ((EnrichmentWidgetConfig) config).getEnrichIdentifier();
        } else {
            String enrichPath = config.getStartClass() + "."
                + ((EnrichmentWidgetConfig) config).getEnrich();
            if (WidgetConfigUtil.isPathContainingSubClass(os.getModel(), enrichPath)) {
                subClassContraint = true;
                subClassType = enrichPath.substring(enrichPath.indexOf("[") + 1,
                                                    enrichPath.indexOf("]"));
                subClassPath = enrichPath.substring(0, enrichPath.indexOf("["));
                enrichIdentifier = subClassPath + enrichPath.substring(enrichPath.indexOf("]") + 1);
            } else {
                enrichIdentifier = enrichPath;
            }
        }
        pathConstraint = enrichIdentifier;
        if (subClassContraint) {
            q.addConstraint(Constraints.type(subClassPath, subClassType));
        }
        return q;
    }

    /**
     * Returns the pathquery based on the startClassDisplay, constraintsForView set in config file
     * and the bag constraint
     * Executed when the user click on the matches column in the enrichment widget.
     * @return the query generated
     */
    public PathQuery getPathQueryForMatches() {
        Model model = os.getModel();
        PathQuery pathQuery = new PathQuery(model);
        String enrichIdentifier;
        boolean subClassContraint = false;
        String subClassType = "";
        String subClassPath = "";
        EnrichmentWidgetConfig ewc = ((EnrichmentWidgetConfig) config);
        if (ewc.getEnrichIdentifier() != null) {
            enrichIdentifier = config.getStartClass() + "."
                + ewc.getEnrichIdentifier();
        } else {
            String enrichPath = config.getStartClass() + "."
                + ewc.getEnrich();
            if (WidgetConfigUtil.isPathContainingSubClass(model, enrichPath)) {
                subClassContraint = true;
                subClassType = enrichPath.substring(enrichPath.indexOf("[") + 1,
                                                    enrichPath.indexOf("]"));
                subClassPath = enrichPath.substring(0, enrichPath.indexOf("["));
                enrichIdentifier = subClassPath + enrichPath.substring(enrichPath.indexOf("]") + 1);
            } else {
                enrichIdentifier = enrichPath;
            }
        }

        String startClassDisplayView = config.getStartClass() + "."
            + ewc.getStartClassDisplay();
        pathQuery.addView(enrichIdentifier);
        pathQuery.addView(startClassDisplayView);
        pathQuery.addOrderBy(enrichIdentifier, OrderDirection.ASC);
        // bag constraint
        if (bag != null) {
            pathQuery.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        } else if (ids != null) {
            // use list of IDs instead of bag
            String[] idStrings = ids.split(",");
            List<Integer> intermineIds = new ArrayList<Integer>();
            for (int i = 0; i < idStrings.length; i++) {
                try {
                    intermineIds.add(Integer.parseInt(idStrings[i]));
                } catch (NumberFormatException e) {
                    LOG.error("bad IDs for list in enrichment.", e);
                    return null;
                }
            }
            pathQuery.addConstraint(Constraints.inIds(config.getStartClass(), intermineIds));
        }


        //subclass constraint
        if (subClassContraint) {
            pathQuery.addConstraint(Constraints.type(subClassPath, subClassType));
        }
        //constraints for view
        List<PathConstraint> pathConstraintsForView =
            ewc.getPathConstraintsForView();
        if (pathConstraintsForView != null) {
            for (PathConstraint pc : pathConstraintsForView) {
                pathQuery.addConstraint(pc);
            }
        }
        return pathQuery;
    }

    /**
     * @return the correction coefficient used by the widget
     */
    public CorrectionCoefficient getExtraCorrectionCoefficient() {
        return correctionCoefficient;
    }
}
