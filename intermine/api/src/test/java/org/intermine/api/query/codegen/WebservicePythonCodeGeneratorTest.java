package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2018
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
    public void testPrint() throws Exception {
        List<ClassDescriptor> cds = new ArrayList<ClassDescriptor>();
        List<AttributeDescriptor> ads = new ArrayList<AttributeDescriptor>();

        ads.add(new AttributeDescriptor("testatt", "java.lang.String"));
        cds.add(
            new ClassDescriptor(
                "testns.testclass", null, false,
                ads,
                new HashSet<ReferenceDescriptor>(),
                new HashSet<CollectionDescriptor>()));

        Model m = new Model("model1", "testns", cds);

        PathQuery pq = new PathQuery(m);
        pq.addView("testclass.testatt");

        String result = cg.generate(new WebserviceCodeGenInfo(pq, null, null, null, true, null, "\n"));
        String[] lines = result.split("\n");

        Assert.assertEquals(23, lines.length);
        Assert.assertEquals("print row[\"testatt\"]", lines[22].trim());

        // System.out.println(result);
    }
}
