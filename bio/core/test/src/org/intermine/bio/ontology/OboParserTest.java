package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

public class OboParserTest extends TestCase
{
    private OboParser parser;

    public OboParserTest(String arg) {
        super(arg);
    }

    public void setUp() {
        parser = new OboParser();
    }




    public void testBasicStructure() throws Exception {
        parser.processOntology(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo")));
        Set terms = parser.getOboTerms();
        //assertEquals("GO:0000004", ((OboTerm) terms.iterator().next()).getId());

        terms = new HashSet(parser.terms.values());
        assertEquals(4, terms.size()); // 4 terms total

        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
        OboTerm dt2 = (OboTerm) parser.terms.get("GO:0000002");
        OboTerm dt3 = (OboTerm) parser.terms.get("GO:0000003");
        OboTerm dt4 = (OboTerm) parser.terms.get("GO:0000004");

        assertNotNull(dt1);
        assertNotNull(dt2);
        assertNotNull(dt3);
        assertNotNull(dt4);

//        assertTrue(dt1.getChildren().contains(dt2));
//        assertTrue(dt1.getChildren().contains(dt4));
//        assertTrue(dt2.getChildren().contains(dt3));
//        assertTrue(dt1.getComponents().contains(dt3));
    }

    public void testSynonyms() throws Exception {
        parser.processOntology(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo")));

        OboTerm dt3 = (OboTerm) parser.terms.get("GO:0000003");

        assertEquals(6, dt3.getSynonyms().size());

        HashSet expSyns = new LinkedHashSet();
        expSyns.add(new OboTermSynonym("some_value", "synonym"));
        expSyns.add(new OboTermSynonym("related_value", "related_synonym"));
        expSyns.add(new OboTermSynonym("exact_value", "exact_synonym"));
        expSyns.add(new OboTermSynonym("broad_value", "broad_synonym"));
        expSyns.add(new OboTermSynonym("narrow_value", "narrow_synonym"));
        expSyns.add(new OboTermSynonym("GO:0019952", "alt_id"));

        assertEquals(expSyns, dt3.getSynonyms());
    }

    public void testDescriptions() throws Exception {
        parser.processOntology(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo")));
        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
        OboTerm dt2 = (OboTerm) parser.terms.get("GO:0000002");
        OboTerm dt3 = (OboTerm) parser.terms.get("GO:0000003");

        assertEquals("iosis, mediated byhe cytoskeleton.", dt1.getDescription());
        assertEquals("The maintenance of the structure and integrity of the mitochondrial genome.", dt2.getDescription());
        assertEquals("", dt3.getDescription());
    }

    public void testNamespaces() throws Exception {
        parser.processOntology(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo")));

        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
        OboTerm dt2 = (OboTerm) parser.terms.get("GO:0000002");
        OboTerm dt3 = (OboTerm) parser.terms.get("GO:0000003");

        assertEquals("gene_ontology", dt1.getNamespace());
        assertEquals("other_namespace", dt2.getNamespace());
        assertEquals("gene_ontology", dt3.getNamespace());
    }

//    public void testNoDefaultNS() throws Exception {
//        String noDefaultNS = "format-version: 1.0\n" +
//                "date: 28:07:2005 15:19\n" +
//                "saved-by: midori\n" +
//                "\n" +
//                "[Term]\n" +
//                "id: GO:0000001\n" +
//                "name: mitochondrion inheritance\n";
//
//        Set terms = parser.processOntology(new StringReader(noDefaultNS));
//        assertEquals(1, terms.size());
//        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
//        assertEquals("", dt1.getNamespace());
//    }

    public void testGetTermIdNameMap() throws Exception {
        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));

        Map idNames = parser.getTermIdNameMap(new StringReader(test));

        HashMap expecting = new HashMap();
        expecting.put("GO:0000001", "mitochondrion inheritance");
        expecting.put("GO:0000002", "mitochondrial genome maintenance");
        expecting.put("GO:0000003", "reproduction");
        expecting.put("GO:0000004", "partoftest");

        assertEquals(expecting, idNames);
    }

//    public void testGetTermToParentTermSetMap() throws Exception {
//        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));
//        parser.readTerms(new BufferedReader(new StringReader(test)));
//
//        Map<String, Set> expected = new HashMap<String, Set>();
//        expected.put("GO:0000001", new HashSet());
//        expected.put("GO:0000002", new HashSet(Arrays.asList(new Object[] {"GO:0000001"})));
//        expected.put("GO:0000003", new HashSet(Arrays.asList(new Object[] {"GO:0000001", "GO:0000002"})));
//        expected.put("GO:0000004", new HashSet(Arrays.asList(new Object[] {"GO:0000001", "GO:0000002", "GO:0000003"})));
//        assertEquals(expected, parser.getTermToParentTermSetMap());
//    }

    public void testUnescape() {
        assertEquals("\n", parser.unescape("\\n"));
        assertEquals(" ", parser.unescape("\\W"));
        assertEquals("\t", parser.unescape("\\t"));
        assertEquals(":", parser.unescape("\\:"));
        assertEquals(",", parser.unescape("\\,"));
        assertEquals("\"", parser.unescape("\\\""));
        assertEquals("\\", parser.unescape("\\\\"));
        assertEquals("()", parser.unescape("\\(\\)"));
        assertEquals("[]", parser.unescape("\\[\\]"));
        assertEquals("{}", parser.unescape("\\{\\}"));

        // pass-thru
        assertEquals("\n", parser.unescape("\n"));
        assertEquals(" ", parser.unescape(" "));
        assertEquals("\t", parser.unescape("\t"));
        assertEquals(":", parser.unescape(":"));
        assertEquals(",", parser.unescape(","));
        assertEquals("\"", parser.unescape("\""));
        assertEquals("()", parser.unescape("()"));
        assertEquals("[]", parser.unescape("[]"));
        assertEquals("{}", parser.unescape("{}"));

        assertEquals("a\\bc:d,e[f)g{h i\tj\nk", parser.unescape("a\\\\b\\c\\:d\\,e\\[f\\)g\\{h\\Wi\\tj\\nk"));
    }

    public void testAddSynonyms() throws Exception {
        OboTerm term = new OboTerm("id", "name");
        parser.addSynonyms(term,
                Arrays.asList(new String[]{"\"no escapes\" []",
                        " \"one \\\" escape\" [asdf]",
                        " \"late quotes\" [as\\\"df] \"",
                        "\"nothing trailing\""}), "synonym_type");

        assertEquals(4, term.getSynonyms().size());

        HashSet expect = new HashSet();
        expect.add(new OboTermSynonym("no escapes", "synonym_type"));
        expect.add(new OboTermSynonym("one \" escape", "synonym_type"));
        expect.add(new OboTermSynonym("late quotes", "synonym_type"));
        expect.add(new OboTermSynonym("nothing trailing", "synonym_type"));

        assertEquals(expect, term.getSynonyms());
    }

    public void testDodgySynonym() {
        OboTerm term = new OboTerm("id", "name");
        parser.addSynonyms(term,
                Arrays.asList(new String[]{"xxxxxxxx"}), "synonym_type");
        assertEquals(0, term.getSynonyms().size());
    }

    public void testXrefs() throws Exception {
        OboTerm term = new OboTerm("id", "name");
        parser.readConfig();
        parser.addXrefs(term, Arrays.asList(new String[]{"FBbt:000", "monkey"}));
        assertEquals(1, term.getXrefs().size());
    }


//    public void testIsObsolete() {
//        Map tagValues;
//
//        tagValues = new MultiValueMap();
//        tagValues.put("is_obsolete", "true");
//        assertTrue(OboParser.isObsolete(tagValues));
//
//        tagValues = new MultiValueMap();
//        tagValues.put("is_obsolete", "TRUE");
//        assertTrue(OboParser.isObsolete(tagValues));
//
//        tagValues = new MultiValueMap();
//        tagValues.put("is_obsolete", "true");
//        tagValues.put("is_obsolete", "false");
//        assertTrue(OboParser.isObsolete(tagValues));
//
//        tagValues = new MultiValueMap();
//        tagValues.put("is_obsolete", "FALSE");
//        assertFalse(OboParser.isObsolete(tagValues));
//
//        tagValues = new MultiValueMap();
//        tagValues.put("is_obsolete", "false");
//        assertFalse(OboParser.isObsolete(tagValues));
//
//        tagValues = new MultiValueMap();
//        tagValues.put("is_obsolete", "FALSE");
//        tagValues.put("is_obsolete", "true");
//        assertFalse(OboParser.isObsolete(tagValues));
//
//        tagValues = new MultiValueMap();
//        assertFalse(OboParser.isObsolete(tagValues));
//    }
}
