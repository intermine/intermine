package org.intermine.api.config;

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
 * A single constant.
 *
 * @author Richard Smith
 */
public final class Constants
{
    private Constants() {
        // don't instantiate
    }

    /**
     * Batch size for the underlying objectstore
     */
    public static final int BATCH_SIZE = 500;

    // the below are used on the list and report pages to build queries to use on other mines

    /**
     * returns available queryable values for mine
     */
    public static final String VALUES_TEMPLATE = "im_available_organisms";

    /**
    * template to run to get map of values associated with this mine
    * gene.organism --> gene.homologue.organism
    */
    public static final String MAP_TEMPLATE = "im_available_homologues";

    /**
     * checks for object in other mine
     */
    public static final String REPORT_TEMPLATE = "im_available_gene";

    /**
     * queries for data related to current object
     */
    public static final String RELATED_DATA_TEMPLATE = "im_gene_orthologue";

    /**
     * constrain by object identifier
     */
    public static final String IDENTIFIER_CONSTRAINT
        = "&constraint1=Gene.primaryIdentifier&op1=eq&value1=";

    /**
     * LOOKUP constraint
     */
    public static final String LOOKUP_CONSTRAINT = "&constraint1=Gene&op1=LOOKUP&value1=";

    /**
     * Constrain by an extra value
     */
    public static final String EXTRA_VALUE_CONSTRAINT
        = "&constraint2=Gene.organism.shortName&op2=eq&value2=";

    /**
     * Constraint for related data template
     */
    public static final String RELATED_DATA_CONSTRAINT_1
        = "&constraint1=Gene.homologues.homologue&op1=LOOKUP&value1=";

    /**
     * Constraint for related data template
     */
    public static final String RELATED_DATA_CONSTRAINT_2
        = "&constraint2=Gene.homologues.homologue.organism.shortName&op2=eq&value2=";
}
