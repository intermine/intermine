package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.collections.MultiHashMap;
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
        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));

        Set terms = parser.processForLabellingOntology(new StringReader(test));
        assertEquals(1, terms.size()); // should be one root term
        
        assertEquals("GO:0000001", ((DagTerm) terms.iterator().next()).getId());
        
        terms = new HashSet(parser.terms.values());
        assertEquals(3, terms.size()); // 3 terms total
        
        DagTerm dt1 = (DagTerm) parser.terms.get("GO:0000001");
        DagTerm dt2 = (DagTerm) parser.terms.get("GO:0000002");
        DagTerm dt3 = (DagTerm) parser.terms.get("GO:0000003");
        
        assertNotNull(dt1);
        assertNotNull(dt2);
        assertNotNull(dt3);
        
        assertTrue(dt1.getChildren().contains(dt2));
        assertTrue(dt2.getChildren().contains(dt3));
        assertTrue(dt1.getComponents().contains(dt3));
    }
    
    public void testSynonyms() throws Exception {
        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));
        Set terms = parser.processForLabellingOntology(new StringReader(test));
        
        DagTerm dt3 = (DagTerm) parser.terms.get("GO:0000003");
        
        assertEquals(5, dt3.getSynonyms().size());
        
        HashSet expSyns = new HashSet();
        expSyns.add(new OboTermSynonym("some_value", "synonym"));
        expSyns.add(new OboTermSynonym("exact_value", "exact_synonym"));
        expSyns.add(new OboTermSynonym("related_value", "related_synonym"));
        expSyns.add(new OboTermSynonym("broad_value", "broad_synonym"));
        expSyns.add(new OboTermSynonym("narrow_value", "narrow_synonym"));
        
        assertEquals(expSyns, dt3.getSynonyms());
    }
    
    public void testDescriptions() throws Exception {
        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));
        Set terms = parser.processForLabellingOntology(new StringReader(test));
        
        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
        OboTerm dt2 = (OboTerm) parser.terms.get("GO:0000002");
        OboTerm dt3 = (OboTerm) parser.terms.get("GO:0000003");
        
        assertEquals("iosis, mediated byhe cytoskeleton.", dt1.getDescription());
        assertEquals("The maintenance of the structure and integrity of the mitochondrial genome.", dt2.getDescription());
        assertEquals("", dt3.getDescription());
    }
    
    public void testNamespaces() throws Exception {
        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));
        Set terms = parser.processForLabellingOntology(new StringReader(test));
        
        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
        OboTerm dt2 = (OboTerm) parser.terms.get("GO:0000002");
        OboTerm dt3 = (OboTerm) parser.terms.get("GO:0000003");
        
        assertEquals("gene_ontology", dt1.getNamespace());
        assertEquals("other_namespace", dt2.getNamespace());
        assertEquals("gene_ontology", dt3.getNamespace());
    }
    
    public void testNoDefaultNS() throws Exception {
        String noDefaultNS = "format-version: 1.0\n" + 
                "date: 28:07:2005 15:19\n" + 
                "saved-by: midori\n" +
                "\n" + 
                "[Term]\n" + 
                "id: GO:0000001\n" + 
                "name: mitochondrion inheritance\n";
        
        Set terms = parser.processForLabellingOntology(new StringReader(noDefaultNS));
        assertEquals(1, terms.size());
        OboTerm dt1 = (OboTerm) parser.terms.get("GO:0000001");
        assertEquals("", dt1.getNamespace());
    }
    
    public void testGetTermIdNameMap() throws Exception {
        String test = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("OboParserTest.obo"));
        
        Map idNames = parser.getTermIdNameMap(new StringReader(test));
        
        HashMap expecting = new HashMap();
        expecting.put("GO:0000001", "mitochondrion inheritance");
        expecting.put("GO:0000002", "mitochondrial genome maintenance");
        expecting.put("GO:0000003", "reproduction");
        
        assertEquals(expecting, idNames);
    }
    
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
    
    public void testAddSynonyms() {
        DagTerm term = new DagTerm("id", "name");
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
        DagTerm term = new DagTerm("id", "name");
        parser.addSynonyms(term,
                Arrays.asList(new String[]{"xxxxxxxx"}), "synonym_type");
        assertEquals(0, term.getSynonyms().size());
    }
    
    public void testIsObsolete() {
        Map tagValues;
        
        tagValues = new MultiHashMap();
        tagValues.put("is_obsolete", "true");
        assertTrue(parser.isObsolete(tagValues));
        
        tagValues = new MultiHashMap();
        tagValues.put("is_obsolete", "TRUE");
        assertTrue(parser.isObsolete(tagValues));
        
        tagValues = new MultiHashMap();
        tagValues.put("is_obsolete", "true");
        tagValues.put("is_obsolete", "false");
        assertTrue(parser.isObsolete(tagValues));
        
        tagValues = new MultiHashMap();
        tagValues.put("is_obsolete", "FALSE");
        assertFalse(parser.isObsolete(tagValues));
        
        tagValues = new MultiHashMap();
        tagValues.put("is_obsolete", "false");
        assertFalse(parser.isObsolete(tagValues));
        
        tagValues = new MultiHashMap();
        tagValues.put("is_obsolete", "FALSE");
        tagValues.put("is_obsolete", "true");
        assertFalse(parser.isObsolete(tagValues));
        
        tagValues = new MultiHashMap();
        assertFalse(parser.isObsolete(tagValues));
    }
}
