package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.ItemsTestCase;

public class SequenceProcessorTest extends ItemsTestCase
{
    public SequenceProcessorTest(String arg) {
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

    public void testFeatureDataFlagsStrings() {
        FeatureData fdat = new FeatureData();

        assertEquals(false, fdat.getFlag(FeatureData.SECONDARY_IDENTIFIER_SET));
        assertEquals(false, fdat.getFlag("secondaryIdentifier"));
        assertEquals(false, fdat.getFlag(FeatureData.EVIDENCE_CREATED));
        fdat.setFlag("secondaryIdentifier", true);
        assertEquals(true, fdat.getFlag("secondaryIdentifier"));
        assertEquals(false, fdat.getFlag(FeatureData.EVIDENCE_CREATED));
        fdat.setFlag("secondaryIdentifier", false);
        assertEquals(false, fdat.getFlag("secondaryIdentifier"));
        assertEquals(false, fdat.getFlag(FeatureData.EVIDENCE_CREATED));
    }
}
