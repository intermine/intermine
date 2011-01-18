package org.intermine.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collection;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;

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

        // read the original into a string
        BufferedReader originalReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("testmodel_data.xml"), "UTF8"));
        StringWriter originalWriter = new StringWriter();
        int c = originalReader.read();
        while (c != -1) {
            originalWriter.write(c);
            c = originalReader.read();
        }
        //System.out.println("Original: " + originalWriter.toString());
        //System.out.println("Generated: " + sw.toString());
        Diff diff = new Diff(originalWriter.toString(), sw.toString());
        DetailedDiff detail = new DetailedDiff(diff);
        detail.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        assertTrue(detail.getAllDifferences().toString() + ": Original: " + originalWriter.toString() + ", Generated: " + sw.toString(), detail.similar());
    }


    protected void setIds(Collection c) throws Exception {
        int i=1;
        for (Object o : c) {
            if (o instanceof InterMineObject) {
                ((InterMineObject) o).setId(new Integer(i++));
            }
        }
    }
}
