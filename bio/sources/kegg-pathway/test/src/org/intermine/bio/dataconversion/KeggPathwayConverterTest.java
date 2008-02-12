package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class KeggPathwayConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public KeggPathwayConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        File resources = new File ("test/resources");
        if (!resources.exists()) {
            // a hack - look in test-all instead because we're running the bio tests
            resources = new File("../sources/kegg-pathway/test/resources");
        } else if (!resources.exists()) {
            resources = new File ("resources");
        } else if (!resources.exists()) {
            fail("can't find the resources directory");
        }

        Collection<File> allfiles = listFiles(resources, null, true);

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        KeggPathwayConverter converter = new KeggPathwayConverter(itemWriter,
                                                        Model.getInstanceByName("genomic"));
        converter.setSrcDataDir(resources.toString());

        for (File file: allfiles) {
            if(file.getPath().matches(".*\\.svn.*") || file.getName().matches(".*\\.svn.*")) {
                continue;
            }
            if(file.isDirectory()) {
                continue;
            }

            Reader reader = new FileReader(file);
            converter.setCurrentFile(file);
            converter.process(reader);
        }

        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "/tmp/kegg-tgt-items.xml");

        assertEquals(readItemSet("kegg-tgt-items.xml"), itemWriter.getItems());
    }

    public static Collection<File> listFiles(File directory, FilenameFilter filter, boolean recurse) {
        Vector<File> files = new Vector<File>();
        File[] entries = directory.listFiles();
        for (File entry : entries) {
            // If there is no filter or the filter accepts the
            // file / directory, add it to the list
            if (filter == null || filter.accept(directory, entry.getName())) {
                files.add(entry);
            }

            // If the file is a directory and the recurse flag
            // is set, recurse into the directory
            if (recurse && entry.isDirectory()) {
                files.addAll(listFiles(entry, filter, recurse));
            }
        }
        // Return collection of files
        return files;
    }
}
