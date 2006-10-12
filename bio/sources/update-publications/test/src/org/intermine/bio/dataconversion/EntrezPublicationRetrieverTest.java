package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import junit.framework.TestCase;
import junit.framework.Assert;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.xml.full.FullParser;

import org.flymine.model.genomic.Organism;

/**
 * Tests for EntrezOrganismRetriever.
 */
public class EntrezPublicationRetrieverTest extends TestCase
{
    public void testEntrezOrganismRetriever() throws Exception {
        EntrezPublicationsRetriever eor = new TestEntrezPublicationsRetriever();
        eor.setOsAlias("os.bio-test");

        // Create temp file.
        File temp = File.createTempFile("EntrezPublicationsRetriever", ".tmp");
        // Delete temp file when program exits.
        temp.deleteOnExit();

        eor.setOutputFile(temp.getPath());

        eor.execute();

        List expected = FullParser.parse(getClass().getClassLoader().getResourceAsStream("EntrezPublicationsRetrieverTest_tgt.xml"));

        Collection actual = FullParser.parse(new FileInputStream(temp));

        Assert.assertEquals(new HashSet(expected), new HashSet(actual));
    }

    class TestEntrezPublicationsRetriever extends EntrezPublicationsRetriever
    {
        public TestEntrezPublicationsRetriever() {
            super();
            setOsAlias("os.bio-test");
            setOutputFile("/tmp/TestEntrezPublicationsRetriever_dummy");
        }

        protected Map getOrganisms(ObjectStore os) {
            try {
                List items = FullParser.parse(getClass().getClassLoader().getResourceAsStream("EntrezPublicationsRetrieverTest_src.xml"));
                List objects =
                    FullParser.realiseObjects(items, Model.getInstanceByName("genomic"), false);
                Map map = new HashMap();
                Iterator i = objects.iterator();
                while (i.hasNext()) {
                    Object obj = i.next();
                    Organism org = (Organism) obj;
                    map.put(org.getTaxonId(), org);
                }
                return map;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected Reader getReader(Set ids) {
            return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("EntrezPublicationsRetrieverTest_esummary.xml"));
        }
    }
}
