package org.intermine.task;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.*;
import java.util.Collections;

import junit.framework.TestCase;

import org.intermine.metadata.InterMineModelParser;
import org.intermine.metadata.Model;

/**
 * Test for ModelMergerTask.
 *
 * @author Thomas Riley
 */
public class ModelMergerTaskTest extends TestCase {

    public void testExecute() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        File output = File.createTempFile("output", ".xml");

        ModelMergerTask task = new ModelMergerTask();
        task.setAdditionsFiles(Collections.singletonList("xml/ModelMergerTaskTestAdditions.xml"));
        task.setInputModelFile(new File(cl.getResource("xml/ModelMergerTaskTestInput.xml").getPath()));
        task.setOutputFile(output);

        task.execute();

        InterMineModelParser parser = new InterMineModelParser();
        Model result = parser.process(new FileReader(output));
        Model exp = parser.process(new InputStreamReader(cl.getResourceAsStream("xml/ModelMergerTaskTestExpected.xml")));
        assertEquals(exp, result);
    }
}
