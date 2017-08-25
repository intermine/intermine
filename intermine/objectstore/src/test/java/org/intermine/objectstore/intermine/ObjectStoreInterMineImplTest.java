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

import java.util.*;

import org.intermine.metadata.Model;
import org.intermine.objectstore.*;
import org.junit.BeforeClass;

public class ObjectStoreInterMineImplTest extends ObjectStoreInterMineCommonTests
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        os = (ObjectStoreInterMineImpl)ObjectStoreFactory.getObjectStore("os.unittest");
        Model model = Model.getInstanceByName("testmodel/testmodel");
        Collection items = ObjectStoreTestUtils.loadItemsFromXml(model, "testmodel_data.xml");
        ObjectStoreTestUtils.setIdsOnItems(items);
        data = ObjectStoreTestUtils.mapItemsToNames(items);
        System.out.println(data.size() + " entries in data mapItemsToNames");
        storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        ObjectStoreTestUtils.storeData(storeDataWriter, data);
    }
}
