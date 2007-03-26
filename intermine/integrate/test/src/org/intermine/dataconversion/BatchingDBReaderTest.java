package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;

public class BatchingDBReaderTest extends DBReaderTestCase
{
    public BatchingDBReaderTest(String arg) {
        super(arg);
    }

    public DBReader getDBReader() throws Exception {
        return new BatchingDBReader(db, Model.getInstanceByName("testmodel"));
    }
}
