package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.junit.BeforeClass;

public class ObjectStoreInterMineImplTest extends ObjectStoreInterMineImplTestCase
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        oneTimeSetUp("os.unittest", "osw.unittest", "testmodel", "testmodel_data.xml");
    }
}
