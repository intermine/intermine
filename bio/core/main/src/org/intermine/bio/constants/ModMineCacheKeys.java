package org.intermine.bio.constants;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Constants used to identify count types stored in modMine cache.
 *
 * @author Richard Smith
 */
public final class ModMineCacheKeys
{
    private ModMineCacheKeys() {
        // don't instantiate
    }

    /**
     * Key to submission feature count cache entry.
     */
    public static final String SUB_FEATURE_COUNT = "submissionFeatureCount";

     /**
      * Key to experiment feature count cache entry.
      */
    public static final String EXP_FEATURE_COUNT = "experimentFeatureCount";

    /**
     * Key to experiment unique feature count cache entry.
     */
    public static final String UNIQUE_EXP_FEATURE_COUNT = "uniqueExperimentFeatureCount";

    /**
     * Key to submission expression level per feature type cache entry.
     */
    public static final String SUB_FEATURE_EXPRESSION_LEVEL_COUNT
        = "submissionFeatureExpressionLevelCount";

    /**
     * Key to submission located feature type cache entry.
     */
    public static final String SUB_LOCATED_FEATURE_TYPE
        = "submissionLocatedFeatureTypes";

    /**
     * Key to submission located feature type cache entry.
     */
    public static final String SUB_SEQUENCED_FEATURE_TYPE
        = "submissionSequencedFeatureTypes";

    
    
    /**
     * Key to submission file source count cache entry.
     */
    public static final String SUB_FILE_SOURCE_COUNT
        = "submissionFileSourceCounts";


}
