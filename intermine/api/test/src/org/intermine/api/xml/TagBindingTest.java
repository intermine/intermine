package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

public class TagBindingTest extends InterMineAPITestCase
{
    private ProfileManager pm;
    private Map<String, List<FieldDescriptor>>  classKeys;

    public TagBindingTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        pm = im.getProfileManager();
    }

    public void testUnMarshal() throws Exception {
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream("TagBindingTest.xml"));
        int count = new TagBinding().unmarshal(pm, pm.getSuperuser(), reader);
        assertEquals(3, count);

        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = uosw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            Tag t = (Tag) resIter.next();
            assertEquals("class", t.getType());
            String id = t.getObjectIdentifier();
            if ("org.intermine.model.testmodel.Employee".equals(id) || "org.intermine.model.testmodel.Manager".equals(id)) {
                assertEquals("im:aspect:People", t.getTagName());
            } else if ("org.intermine.model.testmodel.Bank".equals(id)) {
                assertEquals("im:aspect:Entities", t.getTagName());
            } else {
                fail("Wrong objectIdentifier for tag encountered");
            }
        }
    }

    public void testMarshal() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("tags");
            Set<Tag> tags = getTags();
            for (Tag tag : tags) {
                TagBinding.marshal(tag, writer);
            }
            writer.writeEndElement();
            writer.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }


        InputStream is =
            getClass().getClassLoader().getResourceAsStream("TagBindingTest.xml");
        String expectedXml = IOUtils.toString(is);

        String actualXml = sw.toString().trim();
        System.out.println(normalise(actualXml));
        assertEquals("actual and expected XML should be the same", normalise(expectedXml), normalise(actualXml));
    }

    private static String normalise(String x) {
        return x.replaceAll(">\\s*<", "><") // Ignore whitespace between elements
                .replaceAll("\n", "")       // Remove all new-lines
                .replaceAll("\\s*/>", "/>") // Remove whitespace before />
                .replaceAll("\\s{2,}", " ") // Collapse white space to single space
                .replaceAll("date-created=\"\\d+\"", "date-created=\"XXXX\""); // Ignore all dates
    }

    private Set<Tag> getTags() {
        Set<Tag> tags = new HashSet<Tag>();
        Tag tag = new Tag();
        tag.setTagName("im:aspect:People");
        tag.setType("class");
        tag.setObjectIdentifier("org.intermine.model.testmodel.Employee");
        tags.add(tag);

        tag = new Tag();
        tag.setTagName("im:aspect:People");
        tag.setType("class");
        tag.setObjectIdentifier("org.intermine.model.testmodel.Manager");
        tags.add(tag);


        tag = new Tag();
        tag.setTagName("im:aspect:Entities");
        tag.setType("class");
        tag.setObjectIdentifier("org.intermine.model.testmodel.Bank");
        tags.add(tag);

        return tags;
    }
}
