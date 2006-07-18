/*
 * Created on Feb 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.intermine.dataconversion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import antlr.DumpASTVisitor;

import junit.framework.TestCase;

/**
 */
public class ItemPathParserTest extends TestCase
{
    public void testSmallPath() throws Exception {
        parse("transcript.xref.external_db");
    }
    
    public void testBigPath() throws Exception {
        parse("(transcript.translation <- object_xref.ensembl).xref.external_db");
    }
    
    public void testBigPathNestedReverseRef() throws Exception {
        parse("((gene <- transcript.gene).translation <- object_xref.ensembl).xref.external_db");
    }
    
    public void testSmallPathWithConstraints() throws Exception {
        parse("xref.external_db[field='value']");
    }
    
    public void testSmallPathWithAndedConstraints() throws Exception {
        parse("xref.external_db[field='value' && field2='value2'].asdf");
    }
    
    public void testSubPathConstraint() throws Exception {
        parse("xref.external_db[some.thing='sdaf'].asdf");
    }
    
    public void testSubPathVariableConstraint() throws Exception {
        parse("xref.external_db[some.thing=$0].asdf");
    }
    
    public void testVariableConstraint() throws Exception {
        parse("xref.external_db[thing=$0].asdf");
    }
    
    public void test2VariableConstraints() throws Exception {
        parse("xref.external_db[thing=$0 && something.abc=$1].asdf");
    }
    
    private void parse(String path) throws Exception {
        InputStream is = new ByteArrayInputStream(path.getBytes());
        ItemPathLexer lexer = new ItemPathLexer(is);
        ItemPathParser parser = new ItemPathParser(lexer);
        parser.expr();
        
        //DumpASTVisitor visitor = new DumpASTVisitor();
        //visitor.visit(parser.getAST());
    }
}
//((gene <- transcript.gene).translation <- object_xref.ensembl).xref.external_db
