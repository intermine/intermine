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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;

/**
 * Tests for EntrezOrganismRetriever.
 */
public class EntrezPublicationRetrieverTest extends ItemsTestCase
{
    String fullRecord = "false";

    public EntrezPublicationRetrieverTest(String arg) {
        super(arg);
    }

    public void testEntrezPublicationRetriever() throws Exception {
        EntrezPublicationsRetriever eor = new TestEntrezPublicationsRetriever();


        // Create temp file.
        File temp = File.createTempFile("EntrezPublicationsRetriever", ".tmp");
        // Delete temp file when program exits.
        temp.deleteOnExit();

        eor.setLoadFullRecord(fullRecord); // use eFetch URL instead of summary

        eor.setOsAlias("os.bio-test");
        eor.setOutputFile(temp.getPath());
        //eor.setOutputFile("entrez-pub-tgt-items.xml");
        eor.setCacheDirName("build/");
        eor.execute();
        Collection<Item> actual = FullParser.parse(new FileInputStream(temp));

        Set<Item> expected;
        if ("true".equals(fullRecord)) {
            expected = readItemSet("EntrezPublicationsFullRecord_tgt.xml");
        } else {
            expected = readItemSet("EntrezPublicationsSummary_tgt.xml");
        }

        assertEquals(expected, new HashSet<Item>(actual));
    }

    class TestEntrezPublicationsRetriever extends EntrezPublicationsRetriever
    {
        public TestEntrezPublicationsRetriever() {
            super();
            setOsAlias("os.bio-test");
            // setOutputFile("entrez-pub-tgt-items.xml");
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected List getPublications(ObjectStore os) {
            try {
                List<Item> items = FullParser.parse(getClass().getClassLoader().getResourceAsStream("EntrezPublicationsRetrieverTest_src.xml"));

                return FullParser.realiseObjects(items, Model.getInstanceByName("genomic"), false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("rawtypes")
        protected Reader getReader(Set ids) {
            if ("true".equals(fullRecord)) {
                return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("EntrezPublicationsRetrieverTest_efetch.xml"));
            }
            return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("EntrezPublicationsRetrieverTest_esummary.xml"));
        }
    }
}
