package org.intermine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import org.intermine.model.testmodel.*;
import org.intermine.metadata.Model;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;


public class XmlBindingTest extends XMLTestCase {
    protected XmlBinding binding;
    protected File tempFile;

    public void setUp() throws Exception {
        binding = new XmlBinding(Model.getInstanceByName("testmodel"));
        tempFile = File.createTempFile("temp", ".xml");
    }

    public void tearDown() throws Exception {
        tempFile.delete();
    }


    public void testRoundTrip() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        InputStream original = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        Collection unmarshalled = (Collection) binding.unmarshal(original);
        setIds(unmarshalled);
        binding.marshal(unmarshalled, new BufferedWriter(new FileWriter(tempFile)));

        original = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        assertXMLEqual(new InputStreamReader(original), new FileReader(tempFile));
    }


    protected void setIds(Collection c) throws Exception {
        int i=1;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            TypeUtil.setFieldValue(iter.next(), "id", new Integer(i++));
        }
    }
}
