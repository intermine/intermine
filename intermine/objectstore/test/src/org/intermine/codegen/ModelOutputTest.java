package org.intermine.codegen;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.FileReader;
import java.io.File;
import java.util.HashSet;


import org.intermine.metadata.Model;

public class ModelOutputTest extends TestCase
{
    TestModelOutput mo;
    File file;

    public ModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        Model model = new Model("model", "http://www.intermine.org/model/testmodel", new HashSet());
        file =  File.createTempFile("dummy", "tmp");
        mo = new TestModelOutput(model, file);
    }

    public void tearDown() throws Exception {
        file.delete();
    }

    public void testOutputToFile() throws Exception {
        File f = File.createTempFile("model_output_test", "tmp");
        String testString = "testing...";
        ModelOutput.outputToFile(f, testString);
        FileReader reader = new FileReader(f);
        char[] text = new char[1024];
        reader.read(text);
        assertEquals(testString, new String(text).trim());
        f.delete();
    }
}
