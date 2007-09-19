package org.intermine.bio.ontology;

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
import java.io.StringReader;
import java.util.Set;

import junit.framework.TestCase;


public class DagValidatorTest extends TestCase
{
    private DagParser parser;
    private DagValidator validator;

    public DagValidatorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        parser = new DagParser();
        validator = new DagValidator();
    }

    public void testValidDag() throws Exception {

    }

    public void testDuplicateNamesValid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1\n"
            + "  %term2 ; id2\n"
            + "   %term3 ; id3\n";

        Set root1 = parser.processForClassHeirarchy(new BufferedReader(new StringReader(test1)));
        assertTrue(root1.size() == 1);
        assertTrue(validator.duplicateNames(root1));
    }

    public void testDuplicateNamesInvalid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1\n"
            + "  %term2 ; id2\n"
            + "   %term3 ; id1\n";

        Set root1 = parser.processForClassHeirarchy(new BufferedReader(new StringReader(test1)));
        assertFalse(validator.duplicateNames(root1));
    }


    public void testDuplicateIdsValid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1\n"
            + "  %term2 ; id2\n"
            + "   %term3 ; id3\n";

        Set root1 = parser.processForClassHeirarchy(new BufferedReader(new StringReader(test1)));
        assertTrue(root1.size() == 1);
        assertTrue(validator.duplicateIds(root1));
    }

    public void testDuplicateIdsInvalid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1\n"
            + "  %term2 ; id2\n"
            + "   %term1 ; id3\n";

        Set root1 = parser.processForClassHeirarchy(new BufferedReader(new StringReader(test1)));
        assertFalse(validator.duplicateIds(root1));
    }


    public void testSynonymsAreTermsValid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1 ; synonym:term8\n"
            + "  %term2 ; id2\n"
            + "   %term3 ; id3\n";

        Set root1 = parser.processForClassHeirarchy(new BufferedReader(new StringReader(test1)));
        assertTrue(root1.size() == 1);
        assertTrue(validator.synonymsAreTerms(root1));
    }

    public void testSynonymsAreTermsInvalid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1 ; synonym:term3\n"
            + "  %term2 ; id2\n"
            + "   %term3 ; id3\n";

        Set root1 = parser.processForClassHeirarchy(new BufferedReader(new StringReader(test1)));
        assertFalse(validator.synonymsAreTerms(root1));
    }

    public void testOrphanPartofsValid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " %term2 ; id2\n"
            + "  <term1 ; id1\n"
            + " %term1 ; id1\n";


        parser.readTerms(new BufferedReader(new StringReader(test1)));
        Set root1 = parser.rootTerms;
        assertTrue(validator.orphanPartOfs(root1));
    }

    public void testOrphanPartofsInValid() throws Exception {
        String test1 = "!Term Ontology\n"
            + "$Test Ontology ; id0\n"
            + " <term1 ; id1 ; synonym:term3\n"
            + "  <term2 ; id2\n"
            + "  %term3 ; id3\n";

        parser.readTerms(new BufferedReader(new StringReader(test1)));
        Set root1 = parser.rootTerms;
        assertFalse(validator.orphanPartOfs(root1));
    }

}
