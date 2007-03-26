package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.intermine.testing.OneTimeTestCase;

public class IntegrationWriterDataTrackingImplNoIdsTest extends IntegrationWriterDataTrackingImplTest
{
    public IntegrationWriterDataTrackingImplNoIdsTest(String arg) {
        super(arg);
        doIds = false;
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(IntegrationWriterDataTrackingImplNoIdsTest.class);
    }

    public void testCircularRecursionBug() throws Exception {
        // This doesn't work. See ticket #702
    }
}
