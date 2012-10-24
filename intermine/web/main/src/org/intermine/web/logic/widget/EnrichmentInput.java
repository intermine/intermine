package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * Define methods to access data an enrichment calculation needs to be provided with.
 * Implementations may have their own variations of the sample and population.  An example would be
 * calculating the enrichment of certain departments in a sample companies (where each company may
 * contains several departments).  The population is all companies (or all that have any
 * departments) and the sample is a subset of interest of those companies.
 *
 * @author Richard Smith
 */
public interface EnrichmentInput
{

    /**
     * The population size, N.  In our example this is the total number of companies, or possibly
     * the total number of companies that have at least one Department.
     * @return the population size
     */
    int getPopulationSize();

    /**
     * The sample size, n.  The number of items from the whole population that are in the sample, in
     * our example this is the number of companies in the subset.
     * @return the sample size
     */
    int getSampleSize();

    /**
     * For each attribute give a count of items in the sample that have that attribute assigned to
     * them, k.  In our example: for each department the count of companies in the sample that
     * contain the department.
     * @return a map from attribute value to the count of items in the sample with that value
     */
    Map<String, Integer> getAnnotatedCountsInSample();

    /**
     * For each attribute give a count of items in the whole population that have that attribute
     * assigned to them, M.  In our example: for each department the count of companies that contain
     * the department.
     * @return a map from attribute value to the count of items in the population with that value
     */
    Map<String, Integer> getAnnotatedCountsInPopulation();

    /**
     * Get additional display labels for attribute values in the enrichment analysis, these may be
     * names where the values analysed were identifiers.
     * @return labels for each attribute value in the sample
     */
    Map<String, String> getLabels();

    /**
     * The test count represents the number of tests that will be performed.  This is the number of
     * unique attribute values applied across all items in the population.  For example, a count of
     * the unique departments observed in all companies in the population.
     * @return the number of tests to be performed
     */
    int getTestCount();

}
