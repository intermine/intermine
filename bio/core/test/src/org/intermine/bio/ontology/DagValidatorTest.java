package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collection;

import junit.framework.TestCase;


public class DagValidatorTest extends TestCase
{
    private OboParser parser;
    private DagValidator validator;
    private String ENDL = System.getProperty("line.separator");

    public DagValidatorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        parser = new OboParser();
        validator = new DagValidator();
    }


    public void testOrphanPartofsValid() throws Exception {
        String test = "!Term Ontology" + ENDL
            + "[Term]" + ENDL
            + "id: id1" + ENDL
            + "name: term1" + ENDL
            + "[Term]" + ENDL
            + "id: id2" + ENDL
            + "name: term2" + ENDL
            + "is_a: id1" + ENDL
            + "relationship: regulates id1" + ENDL;

        parser.readTerms(new BufferedReader(new StringReader(test)));
        Collection root1 = parser.rootTerms.values();
        assertTrue(validator.orphanPartOfs(root1));
    }

    public void testOrphanPartofsInValid() throws Exception {
        String test = "!Term Ontology" + ENDL
            + "[Term]" + ENDL
            + "id: id1" + ENDL
            + "name: term1" + ENDL
            + "[Term]" + ENDL
            + "id: id2" + ENDL
            + "name: term2" + ENDL
            + "relationship: negatively_regulates id1";

        parser.readTerms(new BufferedReader(new StringReader(test)));
        Collection root1 = parser.rootTerms.values();
        assertFalse(validator.orphanPartOfs(root1));
    }

}
