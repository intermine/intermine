package org.flymine.task;

import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.dummy.ObjectStoreWriterDummyImpl;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Gene;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * TSVFileReaderTaskTest class
 *
 * @author Kim Rutherford
 */

public class TSVFileReaderTaskTest extends TestCase
{
    private Model model = Model.getInstanceByName("genomic");

    public void testConfig() throws Exception {
        TSVFileReaderTask task = new TSVFileReaderTask();
        Project p = new Project();
        task.setProject(p);
        task.init();
                
        InputStream is =
            new FileInputStream("model/genomic/resources/test/TSVFileReaderTaskTest.properties");

        FileSet fs = new FileSet();

        fs.setDir(new File("model/genomic/resources/test"));
        fs.setIncludes("TSVFileReaderTaskTest.tsv");

        DelimitedFileConfiguration dfc = new DelimitedFileConfiguration(model, is);

        task.setOrganismAbbreviation("DM");
        task.addFileSet(fs);

        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setModel(Model.getInstanceByName("genomic"));
        ObjectStoreWriterDummyImpl osw = new ObjectStoreWriterDummyImpl(os);

        Gene gene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene1.setId(new Integer(101));
        gene1.setIdentifier("identifier1");
        
        Gene gene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene2.setId(new Integer(102));
        gene2.setIdentifier("identifier2");
        
        Gene gene3 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene3.setId(new Integer(103));
        gene3.setIdentifier("identifier3");
        
        
        ResultsRow rr1 = new ResultsRow();
        rr1.add(gene1.getIdentifier());
        rr1.add(gene1);
        os.addRow(rr1);
        ResultsRow rr2 = new ResultsRow();
        rr2.add(gene2.getIdentifier());
        rr2.add(gene2);
        os.addRow(rr2);
        ResultsRow rr3 = new ResultsRow();
        rr3.add(gene3.getIdentifier());
        rr3.add(gene3);
        os.addRow(rr3);
        os.cacheObjectById(gene1.getId(), gene1);
        os.cacheObjectById(gene2.getId(), gene2);
        os.cacheObjectById(gene3.getId(), gene3);
        
        os.setResultsSize(3);
        
        task.executeInternal(osw, dfc);
        
        Map storeObjects = osw.getStoredObjects();
        
        assertEquals("name1", ((Gene) storeObjects.get(new Integer(101))).getName());
    }
}
