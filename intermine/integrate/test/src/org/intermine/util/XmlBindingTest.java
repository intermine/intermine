package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.metadata.Model;


public class XmlBindingTest extends XMLTestCase {
    protected XmlBinding binding;

    public void setUp() throws Exception {
        binding = new XmlBinding(Model.getInstanceByName("testmodel"));
    }

    public void testRoundTrip() throws Exception {
        StringWriter sw = new StringWriter();
        InputStream original = getClass().getClassLoader().getResourceAsStream("testmodel_data.xml");
        XMLUnit.setIgnoreWhitespace(true);
        Collection unmarshalled = (Collection) binding.unmarshal(original);
        setIds(unmarshalled);
        binding.marshal(unmarshalled, sw);

        BufferedReader originalReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("testmodel_data.xml")));
        StringBuffer originalBuffer = new StringBuffer();
        int c = originalReader.read();
        while (c != -1) {
            originalBuffer.append((char) c);
            c = originalReader.read();
        }
        //System.out.println("Original: " + originalBuffer.toString());
        //System.out.println("Generated: " + sw.toString());
        Diff diff = new Diff(originalBuffer.toString(), sw.toString());
        DetailedDiff detail = new DetailedDiff(diff);
        detail.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        assertTrue(detail.getAllDifferences().toString(), detail.similar());
        original = getClass().getClassLoader().getResourceAsStream("testmodel_data.xml");
    }


    protected void setIds(Collection c) throws Exception {
        int i=1;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            TypeUtil.setFieldValue(iter.next(), "id", new Integer(i++));
        }
    }
}
