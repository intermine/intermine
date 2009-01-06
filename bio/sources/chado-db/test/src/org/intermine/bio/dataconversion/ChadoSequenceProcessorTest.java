package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.dataconversion.ChadoSequenceProcessor.FeatureData;
import org.intermine.dataconversion.ItemsTestCase;

public class ChadoSequenceProcessorTest extends ItemsTestCase
{
    public ChadoSequenceProcessorTest(String arg) {
        super(arg);
    }

    public void testFeatureDataFlags() {
        FeatureData fdat = new FeatureData();

        assertEquals(false, fdat.getFlag(FeatureData.DATASET_SET));
        assertEquals(false, fdat.getFlag(FeatureData.EVIDENCE_CREATED));
        fdat.setFlag(FeatureData.DATASET_SET, true);
        assertEquals(true, fdat.getFlag(FeatureData.DATASET_SET));
        assertEquals(false, fdat.getFlag(FeatureData.EVIDENCE_CREATED));
        fdat.setFlag(FeatureData.DATASET_SET, false);
        assertEquals(false, fdat.getFlag(FeatureData.DATASET_SET));
        assertEquals(false, fdat.getFlag(FeatureData.EVIDENCE_CREATED));
    }
}
