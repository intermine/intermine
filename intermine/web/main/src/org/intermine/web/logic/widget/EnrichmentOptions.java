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

/**
 * The type of options that enrichment widgets expect to receive.
 * @author Alex Kalderimis
 *
 */
public interface EnrichmentOptions extends WidgetOptions
{

    /** @return the maximum acceptable p-value. (a value between 0 - 1) **/
    double getMaxPValue();

    /** @return the correction algorithm to be used, eg. Holm-Bonferroni. **/
    String getCorrection(); // TODO: this should be an enum!!

    /** @return the extra correction coefficient **/
    String getExtraCorrectionCoefficient();
}
