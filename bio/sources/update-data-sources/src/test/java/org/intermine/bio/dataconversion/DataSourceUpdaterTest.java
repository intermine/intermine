package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataSource;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.xml.full.FullParser;

/**
 * Tests for DataSourceUpdater.
 */
public class DataSourceUpdaterTest extends ItemsTestCase
{
    public DataSourceUpdaterTest(String arg) {
        super(arg);
    }

    public void testDataSourceUpdaterTest() throws Exception {
        DataSourceUpdater converter = new TestDataSourceUpdater();
        converter.setOsAlias("os.bio-test");

        // Create temp file.
        File temp = File.createTempFile("DataSourceUpdater", ".tmp");
        // Delete temp file when program exits.
        temp.deleteOnExit();

        converter.setOutputFile(temp.getPath());
        
        File dbrefs = File.createTempFile("dbxrefs", "");
        FileOutputStream out = new FileOutputStream(dbrefs);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("dbxref.txt"), out);
        out.close();
        dbrefs.deleteOnExit();
        
        converter.setDataSourceFile(dbrefs.getPath());
        converter.execute();

        Set expected = readItemSet("DataSourceUpdaterTest_tgt.xml");
        Collection actual = FullParser.parse(new FileInputStream(temp));

        Assert.assertEquals(expected, new HashSet(actual));
    }

    class TestDataSourceUpdater extends DataSourceUpdater
    {
        public TestDataSourceUpdater() {
            super();
            setOsAlias("os.bio-test");
            setOutputFile("TestDataSourceUpdater_dummy");
        }

        protected Set getDataSources(ObjectStore os) {
            Set set = new HashSet();
            set.add("InterPro");
            set.add("UniProt");
            return set;
        }
    }
}
