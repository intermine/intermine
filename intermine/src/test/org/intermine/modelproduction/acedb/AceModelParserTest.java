package org.intermine.modelproduction.acedb;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Set;

public class AceModelParserTest extends TestCase
{
    public AceModelParserTest(String arg) {
        super(arg);
    }

    public void testParse1() throws Exception {
        String testText
            = "Flibble flobble\twotsit// flooble\n"
            + "        wurble\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        assertEquals(0, node.getIndent());
        assertEquals("Flibble", node.getName());
        assertEquals(ModelNode.ANN_CLASS, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(8, node.getIndent());
        assertEquals("flobble", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNotNull(node.getSibling());
        assertNotNull(node.getChild());
        assertEquals(16, node.getChild().getIndent());
        assertEquals("wotsit", node.getChild().getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getChild().getSibling());
        assertNull(node.getChild().getChild());
        node = node.getSibling();
        assertEquals(8, node.getIndent());
        assertEquals("wurble", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getSibling());
        assertNull(node.getChild());
    }

    public void testParse2() throws Exception {
        String testText
            = "Flibble flobble\twotsit// flooble\n"
            + "        wurble";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        assertEquals(0, node.getIndent());
        assertEquals("Flibble", node.getName());
        assertEquals(ModelNode.ANN_CLASS, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(8, node.getIndent());
        assertEquals("flobble", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNotNull(node.getSibling());
        assertNotNull(node.getChild());
        assertEquals(16, node.getChild().getIndent());
        assertEquals("wotsit", node.getChild().getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getChild().getSibling());
        assertNull(node.getChild().getChild());
        node = node.getSibling();
        assertEquals(8, node.getIndent());
        assertEquals("wurble", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getSibling());
        assertNull(node.getChild());
    }

    public void testParseError1() throws Exception {
        String testText
            = "Flibble flooble\n"
            + "   Flobble\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        try {
            Set parsed = parser.parse(in);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Unmatched indentation", e.getMessage());
        }
    }

    public void testParseError2() throws Exception {
        String testText = "?Flibble flobble Text REPEAT Text";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        try {
            AceModelParser parser = new AceModelParser();
            Set parsed = parser.parse(in);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Keyword \"REPEAT\" before \"Text\" not recognised.", e.getMessage());
        }
    }

    public void testParse3() throws Exception {
        String testText
            = "?Class supertag UNIQUE subtag1\n"
            + "                       subtag2\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        assertEquals(0, node.getIndent());
        assertEquals("?Class", node.getName());
        assertEquals(ModelNode.ANN_CLASS, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(7, node.getIndent());
        assertEquals("supertag", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(16, node.getIndent());
        assertEquals("UNIQUE", node.getName());
        assertEquals(ModelNode.ANN_KEYWORD, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(23, node.getIndent());
        assertEquals("subtag1", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNotNull(node.getSibling());
        assertNull(node.getChild());
        node = node.getSibling();
        assertEquals(23, node.getIndent());
        assertEquals("subtag2", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getSibling());
        assertNull(node.getChild());
    }

    public void testParse4() throws Exception {
        String testText
            = "?Class supertag UNIQUE Text XREF something REPEAT\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        assertEquals(0, node.getIndent());
        assertEquals("?Class", node.getName());
        assertEquals(ModelNode.ANN_CLASS, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(7, node.getIndent());
        assertEquals("supertag", node.getName());
        assertEquals(ModelNode.ANN_TAG, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(16, node.getIndent());
        assertEquals("UNIQUE", node.getName());
        assertEquals(ModelNode.ANN_KEYWORD, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(23, node.getIndent());
        assertEquals("Text", node.getName());
        assertEquals(ModelNode.ANN_REFERENCE, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(28, node.getIndent());
        assertEquals("XREF", node.getName());
        assertEquals(ModelNode.ANN_KEYWORD, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(33, node.getIndent());
        assertEquals("something", node.getName());
        assertEquals(ModelNode.ANN_XREF, node.getAnnotation());
        assertNull(node.getSibling());
        assertNotNull(node.getChild());
        node = node.getChild();
        assertEquals(43, node.getIndent());
        assertEquals("REPEAT", node.getName());
        assertEquals(ModelNode.ANN_KEYWORD, node.getAnnotation());
        assertNull(node.getSibling());
        assertNull(node.getChild());
    }

    public void testConvert1() throws Exception {
        ModelNode node = new ModelNode(0, "NotAClass");
        node.setAnnotation(ModelNode.ANN_NONE);
        try {
            AceModelParser parser = new AceModelParser();
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Not a class", e.getMessage());
        }
    }

    public void testConvert2() throws Exception {
        String testText
            = "?Class Tag UNIQUE Text\n"
            + "           SomethingWrong\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        try {
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Unsuitable node next to TAG-UNIQUE", e.getMessage());
        }
    }

    public void testConvert3() throws Exception {
        String testText = "?Class Tag UNIQUE\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        try {
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("UNIQUE cannot be a leaf node", e.getMessage());
        }
    }

    public void testConvert4() throws Exception {
        ModelNode node = new ModelNode(0, "?Class");
        node.setAnnotation(ModelNode.ANN_CLASS);
        node.setChild(new ModelNode(8, "Wrong"));
        node.getChild().setAnnotation(ModelNode.ANN_NONE);
        try {
            AceModelParser parser = new AceModelParser();
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown node", e.getMessage());
        }
    }

    public void testConvert5() throws Exception {
        String testText
            = "?Class Tag UNIQUE Text\n"
            + "                  Text\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        try {
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Another node next to a reference", e.getMessage());
        }
    }

    public void testConvert6() throws Exception {
        String testText = "?Class Tag Text UNIQUE\n";
        BufferedReader in = new BufferedReader(new StringReader(testText));
        AceModelParser parser = new AceModelParser();
        Set parsed = parser.parse(in);
        assertEquals(1, parsed.size());
        ModelNode node = (ModelNode) parsed.iterator().next();
        try {
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("UNIQUE cannot be a leaf node", e.getMessage());
        }
    }

    public void testConvert7() throws Exception {
        ModelNode node = new ModelNode(0, "?Class");
        node.setAnnotation(ModelNode.ANN_CLASS);
        node.setChild(new ModelNode(8, "Text"));
        node.getChild().setAnnotation(ModelNode.ANN_REFERENCE);
        node.getChild().setChild(new ModelNode(13, "UNIQUE"));
        node.getChild().getChild().setAnnotation(ModelNode.ANN_KEYWORD);
        node.getChild().getChild().setChild(new ModelNode(20, "Wrong"));
        node.getChild().getChild().getChild().setAnnotation(ModelNode.ANN_NONE);
        try {
            AceModelParser parser = new AceModelParser();
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid node type after a reference and UNIQUE", e.getMessage());
        }
    }

    public void testConvert8() throws Exception {
        ModelNode node = new ModelNode(0, "?Class");
        node.setAnnotation(ModelNode.ANN_CLASS);
        node.setChild(new ModelNode(8, "Text"));
        node.getChild().setAnnotation(ModelNode.ANN_REFERENCE);
        node.getChild().setChild(new ModelNode(20, "Wrong"));
        node.getChild().getChild().setAnnotation(ModelNode.ANN_NONE);
        try {
            AceModelParser parser = new AceModelParser();
            parser.nodeClassToDescriptor(node);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid node type after a reference", e.getMessage());
        }
    }

    public void testFormatAceName() throws Exception {
        assertEquals("AceClass", AceModelParser.formatAceName("Class"));
        assertEquals("AceId", AceModelParser.formatAceName("Id"));
        assertEquals("X1name", AceModelParser.formatAceName("1name"));
    }

    public void testUnformatAceName() throws Exception {
        assertEquals("Class", AceModelParser.unformatAceName("AceClass"));
        assertEquals("Id", AceModelParser.unformatAceName("AceId"));
        assertEquals("1name", AceModelParser.unformatAceName("x1name"));
    }


}
