package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.widget.CorrectionCoefficient;
import org.intermine.web.logic.widget.PopulationInfo;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * Implementation of the gene length coefficient
 * @author Daniela Butano
 *
 */
public class GeneLengthCorrectionCoefficient implements CorrectionCoefficient
{
    private WidgetConfig config;
    private ObjectStore os;
    private InterMineBag bag;
    private Integer countItemsWithLengthNotNull = null;
    private static final String GENE_LENGTH = "gene_length";
    private static final String GENE_LENGTH_CORRECTION = "gene_length_correction";
    private static final String PERCENTAGE_GENE_LENGTH_NOT_NULL = "percentage_gene_length_not_null";
    private static final String PATH_QUERY_GENE_LENGTH_NULL = "pathQueryGeneLengthNull";

    public GeneLengthCorrectionCoefficient() {
    }

    public GeneLengthCorrectionCoefficient(WidgetConfig config, ObjectStore os, InterMineBag bag) {
        this.config = config;
        this.os = os;
        this.bag = bag;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected(String correctionCoefficientInput) {
        if (correctionCoefficientInput != null
            && "true".equalsIgnoreCase(correctionCoefficientInput)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isApplicable() {
        if (countItemsWithLengthNotNull != null) {
            if (countItemsWithLengthNotNull != 0) {
                return true;
            }
            return false;
        } else {
            ClassDescriptor sequenceFeatureCd = os.getModel()
                .getClassDescriptorByName("SequenceFeature");
            ClassDescriptor typeDescriptor = os.getModel().getClassDescriptorByName(
                                             config.getTypeClass());
            if (((EnrichmentWidgetConfig) config).getCorrectionCoefficient() != null
                && typeDescriptor.getAllSuperDescriptors().contains(sequenceFeatureCd)) {
                //if there are at least one gene in the bag with length not null
                countItemsWithLengthNotNull = getCountItemsWithLengthNotNull();
                if (countItemsWithLengthNotNull != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Return the number of items contained in the bag with length not null
     * If the type bag is not a subclass of SequenceFeature, return 0
     * @return the number of items with length not null
     */
    private int getCountItemsWithLengthNotNull() {
        ClassDescriptor sequenceFeatureCd = os.getModel()
                .getClassDescriptorByName("SequenceFeature");
        if (bag.getClassDescriptors().contains(sequenceFeatureCd)) {
            Query q = new Query();
            try {
                Class<? extends InterMineObject> clazz =
                    (Class<InterMineObject>) Class.forName(bag.getQualifiedType());
                QueryClass qc = new QueryClass(clazz);
                QueryFunction count = new QueryFunction();
                q.addToSelect(count);
                q.addFrom(qc);
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                QueryField lenghtField = new QueryField(qc, "length");
                cs.addConstraint(new SimpleConstraint(lenghtField, ConstraintOp.IS_NOT_NULL));
                cs.addConstraint(new BagConstraint(qc, ConstraintOp.IN, bag.getOsb()));
                q.setConstraint(cs);
                SingletonResults result = os.executeSingleton(q);
                return ((Long) result.get(0)).intValue();
            } catch (ClassNotFoundException cnfe) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void updatePopulationQuery(Query q, Query subQ, QueryField qfCorrection) {
        if (qfCorrection != null) {
            subQ.addToSelect(qfCorrection);
            QueryField outerQfGenelength = new QueryField(subQ, qfCorrection);
            QueryFunction qfAverage = new QueryFunction(outerQfGenelength, QueryFunction.AVERAGE);
            q.addToSelect(qfAverage);
        }
    }

    @Override
    public void apply(
            Map<String, BigDecimal> pValuesPerTerm,
            PopulationInfo population,
            Map<String, PopulationInfo> annotatedPopulationInfo,
            Double maxValue) {
        BigDecimal pValue, pValueCorrected;
        BigDecimal maxDecimal = new BigDecimal(maxValue);
        String term;
        for (Map.Entry<String, BigDecimal> pValuePerTerm : pValuesPerTerm.entrySet()) {
            pValue = pValuePerTerm.getValue();
            term = pValuePerTerm.getKey();
            if (pValue.equals(BigDecimal.ZERO)) {
                pValuesPerTerm.put(term, BigDecimal.ZERO);
                continue;
            }
            PopulationInfo pi = annotatedPopulationInfo.get(term);
            if (pi != null) {
                float geneLengthPerTerm = (Float) pi.getExtraAttribute();
                int populationPerTerm = pi.getSize();
                float geneLength = population.getExtraAttribute();
                float geneLenghtProbability = (geneLengthPerTerm / geneLength);
                float populationCountProbability = (float) populationPerTerm / population.getSize();
                float correctionCoefficient =  geneLenghtProbability / populationCountProbability;
                pValueCorrected = pValue.multiply(new BigDecimal(correctionCoefficient));
                // only record result if MAXIMUM value is GREATER THAN the new pValue
                if (maxDecimal.compareTo(pValueCorrected) > 0) {
                    // pValues shouldn't be greater than 1, it makes people uncomfortable.
                    if (BigDecimal.ONE.compareTo(pValueCorrected) < 0) {
                        pValuesPerTerm.put(term, BigDecimal.ONE);
                    } else {
                        pValuesPerTerm.put(term, pValueCorrected);
                    }
                }
            } else {
                pValuesPerTerm.put(term, BigDecimal.ZERO);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<String, Object>> getOutputInfo(String geneLengthCorrectionInput) {
        Map<String, Object> geneLengthAttributes = new HashMap<String, Object>();
        Map<String, Map<String, Object>> extraAttributes = new HashMap<String,
                Map<String, Object>>();
        if (isApplicable()) {
            try {
                double percentageGeneWithLengthNull = getPercentageGeneWithLengthNull();
                if (percentageGeneWithLengthNull != 0) {
                    DecimalFormat df = new DecimalFormat("##.##");
                    df.setRoundingMode(RoundingMode.DOWN);
                    geneLengthAttributes.put(PERCENTAGE_GENE_LENGTH_NOT_NULL,
                        df.format(percentageGeneWithLengthNull) + "%");
                    geneLengthAttributes.put(PATH_QUERY_GENE_LENGTH_NULL,
                        getPathQueryForGenesWithLengthNull(
                            InterMineContext.getWebConfig()).toJson());
                } else {
                    geneLengthAttributes.put(PERCENTAGE_GENE_LENGTH_NOT_NULL, null);
                    geneLengthAttributes.put(PATH_QUERY_GENE_LENGTH_NULL, null);
                }
            } catch (ObjectStoreException os) {
                geneLengthAttributes.put(GENE_LENGTH_CORRECTION, null);
                geneLengthAttributes.put(PERCENTAGE_GENE_LENGTH_NOT_NULL, null);
                geneLengthAttributes.put(PATH_QUERY_GENE_LENGTH_NULL, null);
                extraAttributes.put(GENE_LENGTH, geneLengthAttributes);
            }
            if (geneLengthCorrectionInput == null) {
                geneLengthAttributes.put(GENE_LENGTH_CORRECTION, false);
            } else {
                geneLengthAttributes.put(GENE_LENGTH_CORRECTION,
                    new Boolean(geneLengthCorrectionInput));
            }
        } else {
            geneLengthAttributes.put(GENE_LENGTH_CORRECTION, null);
            geneLengthAttributes.put(PERCENTAGE_GENE_LENGTH_NOT_NULL, null);
            geneLengthAttributes.put(PATH_QUERY_GENE_LENGTH_NULL, null);
        }
        extraAttributes.put(GENE_LENGTH, geneLengthAttributes);
        return extraAttributes;
    }

    private double getPercentageGeneWithLengthNull() throws ObjectStoreException {
        int bagSize = bag.getSize();
        double rate = (double) (bagSize - countItemsWithLengthNotNull) / bagSize;
        return rate * 100;
    }

    /**
     * Returns the pathquery for genes length null based on the views set in config file
     * and the bag constraint
     * Executed when the user selects the peercentage of element in the gab with length null.
     * @param webConfig the web configuration
     * @return the query generated
     */
    private PathQuery getPathQueryForGenesWithLengthNull(WebConfig webConfig) {
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        String startClassSimpleName = config.getStartClass();
        String startClass = model.getPackageName() + "." + startClassSimpleName;
        Collection<FieldConfig> fieldConfigs = webConfig.getFieldConfigs(startClass);
        for (FieldConfig fieldConfig : fieldConfigs) {
            if (fieldConfig.getShowInSummary()) {
                q.addView(startClassSimpleName + "." + fieldConfig.getFieldExpr());
            }
        }
        // bag constraint
        q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        //constraints for gene length
        q.addConstraint(Constraints.isNull(config.getStartClass() + ".length"));

        return q;
    }

    /**
     * {@inheritDoc}
     */
    public QueryField updateQueryWithCorrectionCoefficient(Query query, QueryClass qc) {
        ConstraintSet cs = (ConstraintSet) query.getConstraint();
        QueryField qfCorrection = new QueryField(qc, "length");
        cs.addConstraint(new SimpleConstraint(qfCorrection, ConstraintOp.IS_NOT_NULL));
        return qfCorrection;
    }
}
