package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.HashSet;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collection;

import org.intermine.metadata.Model;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;

import org.flymine.model.genomic.Publication;
import org.flymine.model.genomic.Author;

public class UpdatePublicationsTest extends TestCase
{
    public void testUpdatePublications() throws Exception {
        StringWriter sw = new StringWriter();
        new TestUpdatePublications(null, sw).execute();

        List expected = FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/UpdatePublicationsTest_tgt.xml"));
        
        assertEquals(new HashSet(expected), new HashSet(FullParser.parse(new ByteArrayInputStream(sw.toString().getBytes()))));
    }

    class TestUpdatePublications extends UpdatePublications
    {
        public TestUpdatePublications(ObjectStore os, Writer writer) {
            super(os, writer);
        }

        protected List getPublications() {
            try {
                List items =  FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/UpdatePublicationsTest_src.xml"));
                return FullParser.realiseObjects(items, Model.getInstanceByName("genomic"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected Reader getReader(Set ids) {
            return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/UpdatePublicationsTest_esummary.xml"));
        }
    }
}
