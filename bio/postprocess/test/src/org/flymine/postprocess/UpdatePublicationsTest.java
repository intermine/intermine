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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.Assert;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.xml.full.FullParser;

public class UpdatePublicationsTest extends TestCase
{
    public void testUpdatePublications() throws Exception {
        StringWriter sw = new StringWriter();
        new TestUpdatePublications(ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test"), sw).execute();

        List expected = FullParser.parse(getClass().getClassLoader().getResourceAsStream("UpdatePublicationsTest_tgt.xml"));

        Assert.assertEquals(new HashSet(expected), new HashSet(FullParser.parse(new ByteArrayInputStream(sw.toString().getBytes()))));
    }

    class TestUpdatePublications extends UpdatePublications
    {
        public TestUpdatePublications(ObjectStore os, Writer writer) {
            super(os, writer);
        }

        protected List getPublications() {
            try {
                List items = FullParser.parse(getClass().getClassLoader().getResourceAsStream("UpdatePublicationsTest_src.xml"));
                return FullParser.realiseObjects(items, Model.getInstanceByName("genomic"), false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected Reader getReader(Set ids) {
            return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("UpdatePublicationsTest_esummary.xml"));
        }
    }
}
