package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2021
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.metadata.*;
import org.intermine.pathquery.PathQuery;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Tests for the WebservicePythonCodeGenerator class.
 *
 * @author Alex Kalderimis
 */
public class WebservicePythonCodeGeneratorTest extends WebserviceJavaCodeGeneratorTest {

    public WebservicePythonCodeGeneratorTest(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        lang = "python";
        cg = new WebservicePythonCodeGenerator();
    }

    @Test
    public void testPrintOneAttribute() throws Exception {
        List<ClassDescriptor> cds = new ArrayList<ClassDescriptor>();
        List<AttributeDescriptor> ads = new ArrayList<AttributeDescriptor>();

        ads.add(new AttributeDescriptor("testatt", "java.lang.String", null));
        cds.add(
            new ClassDescriptor(
                "testns.testclass", null, false,
                ads,
                new HashSet<ReferenceDescriptor>(),
                new HashSet<CollectionDescriptor>(), null));

        Model m = new Model("model1", "testns", cds);

        PathQuery pq = new PathQuery(m);
        pq.addView("testclass.testatt");

        String result = cg.generate(new WebserviceCodeGenInfo(pq, null, null, null, true, null, "\n"));
        String[] lines = result.split("\n");

        int expectedLines = 27;

        Assert.assertEquals(expectedLines, lines.length);
        Assert.assertEquals("print(row[\"testatt\"])", lines[expectedLines - 1].trim());

        // System.out.println(result);
    }

    @Test
    public void testPrintThreeAttributes() throws Exception {
        List<ClassDescriptor> cds = new ArrayList<ClassDescriptor>();
        List<AttributeDescriptor> ads = new ArrayList<AttributeDescriptor>();

        ads.add(new AttributeDescriptor("testatt", "java.lang.String", null));
        ads.add(new AttributeDescriptor("testatt2", "java.lang.String", null));
        ads.add(new AttributeDescriptor("testatt3", "java.lang.String", null));

        cds.add(
            new ClassDescriptor(
                "testns.testclass", null, false,
                ads,
                new HashSet<ReferenceDescriptor>(),
                new HashSet<CollectionDescriptor>(), null));

        Model m = new Model("model1", "testns", cds);

        PathQuery pq = new PathQuery(m);
        pq.addViews("testclass.testatt", "testclass.testatt2", "testclass.testatt3");

        String result = cg.generate(new WebserviceCodeGenInfo(pq, null, null, null, true, null, "\n"));
        String[] lines = result.split("\n");

        int expectedLines = 27;

        Assert.assertEquals(expectedLines, lines.length);
        Assert.assertEquals(
            "print(row[\"testatt\"], row[\"testatt2\"], row[\"testatt3\"])", lines[expectedLines - 1].trim());

        // System.out.println(result);
    }
}
