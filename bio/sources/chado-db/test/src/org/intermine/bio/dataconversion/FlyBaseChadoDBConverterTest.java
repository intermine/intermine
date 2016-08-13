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

public class FlyBaseChadoDBConverterTest extends ItemsTestCase
{
    public FlyBaseChadoDBConverterTest(String arg) {
        super(arg);
    }

    public void testAddCVTermColon() {
        assertEquals("some_value", FlyBaseProcessor.addCVTermColon("some_value"));
        assertEquals("FBbt:00000001", FlyBaseProcessor.addCVTermColon("FBbt:00000001"));
        assertEquals("FBbt:00000001", FlyBaseProcessor.addCVTermColon("FBbt00000001"));
    }
}
