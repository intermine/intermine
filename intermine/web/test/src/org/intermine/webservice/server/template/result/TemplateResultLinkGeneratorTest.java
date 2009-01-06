package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.server.WebServiceConstants;


/**
 * @author Jakub Kulaviak
 **/
public class TemplateResultLinkGeneratorTest extends TestCase
{

    private String prefix = "http://localhost:8080/query/" + WebServiceConstants.MODULE_NAME;
    
    public void testExtraValueLink() {
        TemplateQuery tmpl = getTemplate(getExtraValueQuery());
        String link = new TemplateResultLinkGenerator().getLink("http://localhost:8080/query", tmpl);
        assertEquals(prefix + "/template/results?" +
        		"name=template1&op1=LOOKUP&value1=zen&" +
        		"extra1=Drosophila%3Fmelanogaster&size=" + TemplateResultLinkGenerator.DEFAULT_RESULT_SIZE + "&layout=minelink", link);
    }

    private PathQuery getExtraValueQuery() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        ret.addNode("Gene.name");
        Constraint c = new Constraint(ConstraintOp.LOOKUP, "zen", true, 
                "description", "code", "identifier", "Drosophila_melanogaster");        
        ret.getNode("Gene.name").getConstraints().add(c);
        return ret;
    }
    
    public void testMultipleConstraintsLink() {
        TemplateQuery tmpl = getTemplate(getMultipleConstraintQuery());
        String link = new TemplateResultLinkGenerator().getLink("http://localhost:8080/query", tmpl);
        assertEquals(prefix + "/template/results?" +
                "name=template1&op1=CONTAINS&value1=zen&op2=lt&value2=100" + 
                "&size=" + TemplateResultLinkGenerator.DEFAULT_RESULT_SIZE + "&layout=minelink", link);        
    }

    private PathQuery getMultipleConstraintQuery() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        ret.addNode("Gene.name");
        Constraint c1 = new Constraint(ConstraintOp.CONTAINS, "zen", true, 
                "description", "code", "identifier", null);        
        ret.getNode("Gene.name").getConstraints().add(c1);
        ret.addNode("Gene.length");
        Constraint c2 = new Constraint(ConstraintOp.LESS_THAN, "100", true, 
                "description", "code", "identifier", null);        
        ret.getNode("Gene.length").getConstraints().add(c2);
        return ret;        
    }
    
    private TemplateQuery getTemplate(PathQuery pathQuery) {
        TemplateQuery tmpl = new TemplateQuery("template1", "title", 
                "description", "comments", pathQuery, "keywords");
        tmpl.addNode("");
        return tmpl;
    }
    
}
