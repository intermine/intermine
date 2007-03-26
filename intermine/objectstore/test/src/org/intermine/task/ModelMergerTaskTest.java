package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.xml.InterMineModelParser;

import junit.framework.TestCase;

/**
 * Test for ModelMergerTask.
 * 
 * @author Thomas Riley
 */
public class ModelMergerTaskTest extends TestCase
{
    String inputModel =
        "<model name=\"testmodel\" namespace=\"testmodel#\">"
            + "<class name=\"A\" extends=\"C\" is-interface=\"false\">"
                + "<reference name=\"ref1\" referenced-type=\"C\"/>"
                + "<collection name=\"col1\" referenced-type=\"C\"/>"
                + "<attribute name=\"attrib1\" type=\"java.lang.Integer\"/>"
            + "</class>"
            + "<class name=\"C\" is-interface=\"false\"></class>"
            + "<class name=\"D\" is-interface=\"true\"></class>"
        + "</model>";
    String additions =
        "<model name=\"testmodel\" namespace=\"testmodel#\">"
        + "<class name=\"A\" extends=\"B\" is-interface=\"false\">"
            + "<reference name=\"ref1\" referenced-type=\"C\"/>"
            + "<collection name=\"col2\" referenced-type=\"C\"/>"
            + "<attribute name=\"attrib2\" type=\"java.lang.String\"/>"
        + "</class>"
        + "<class name=\"B\" is-interface=\"false\"></class>"
    + "</model>";
    String expected =
        "<model name=\"testmodel\" namespace=\"testmodel#\">"
        + "<class name=\"A\" extends=\"B\" is-interface=\"false\">"
            + "<reference name=\"ref1\" referenced-type=\"C\"/>"
            + "<collection name=\"col1\" referenced-type=\"C\"/>"
            + "<collection name=\"col2\" referenced-type=\"C\"/>"
            + "<attribute name=\"attrib1\" type=\"java.lang.Integer\"/>"
            + "<attribute name=\"attrib2\" type=\"java.lang.String\"/>"
        + "</class>"
        + "<class name=\"C\" is-interface=\"false\"></class>"
        + "<class name=\"D\" is-interface=\"true\"></class>"
        + "<class name=\"B\" is-interface=\"false\"></class>"
    + "</model>";
    
    File input, addition, output;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        // write to temp files
        input = File.createTempFile("input", ".xml");
        addition = File.createTempFile("additions", ".xml");
        output = File.createTempFile("output", ".xml");
        
        writeToFile(input, inputModel);
        writeToFile(addition, additions);
    }
    
    private void writeToFile(File file, String data) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.close();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        input.delete();
        addition.delete();
        output.delete();
        super.tearDown();
    }

    public void testExecute() throws Exception {
        ModelMergerTask task = new ModelMergerTask();
        task.setAdditionsFile(addition);
        task.setInputModelFile(input);
        task.setOutputFile(output);
        
        long startTime = System.currentTimeMillis();
        task.execute();
        System.out.println("" + (System.currentTimeMillis() - startTime));
        
        InterMineModelParser parser = new InterMineModelParser();
        Model result = parser.process(new FileReader(output));
        Model exp = parser.process(new StringReader(expected));
        assertEquals(exp, result);
    }
}
