package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.junit.BeforeClass;

public class TruncatedObjectStoreInterMineImplTest extends ObjectStoreInterMineImplTestCase
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        oneTimeSetUp("os.truncunittest", "osw.truncunittest", "testmodel", "testmodel_data.xml");
    }
}
