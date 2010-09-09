package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.api.template.TemplateQuery;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.WebServiceConstants;


/**
 * @author Jakub Kulaviak
 **/
public class TemplateResultLinkGeneratorTest extends TestCase
{

    private String prefix = "http://localhost:8080/query/" + WebServiceConstants.MODULE_NAME;
    
    public void testExtraValueLink() {
        TemplateQuery tmpl = getTemplate(getExtraValueQuery());
        String link = new TemplateResultLinkGenerator().getHtmlLink("http://localhost:8080/query", tmpl);
        assertEquals(link, prefix + "/template/results?"
                + "name=template1&constraint1=Gene.name&op1=LOOKUP&value1=zen&"
                + "extra1=Drosophila_melanogaster&size="
        		+ TemplateResultLinkGenerator.DEFAULT_RESULT_SIZE + "&layout=minelink|paging", link);
    }

    private PathQuery getExtraValueQuery() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        ret.addConstraint(new PathConstraintLookup("Gene.name", "zen", "Drosophila_melanogaster"));
        return ret;
    }
    
    public void testMultipleConstraintsLink() {
        TemplateQuery tmpl = getTemplate(getMultipleConstraintQuery());
        String link = new TemplateResultLinkGenerator().getHtmlLink("http://localhost:8080/query", tmpl);
        System.out.println(link);
        String expected = prefix + "/template/results?name=template1"
            + "&constraint1=Gene.length&op1=lt&value1=100" 
            + "&constraint2=Gene.name&op2=CONTAINS&value2=zen"
            + "&size=" + TemplateResultLinkGenerator.DEFAULT_RESULT_SIZE + "&layout=minelink|paging";
        System.out.println(expected);
        assertEquals(expected, link);        
    }

    private PathQuery getMultipleConstraintQuery() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        ret.addConstraint(new PathConstraintAttribute("Gene.name", ConstraintOp.MATCHES, "zen"));
        ret.addConstraint(new PathConstraintAttribute("Gene.length", ConstraintOp.LESS_THAN, "100"));
        return ret;        
    }
    
    private TemplateQuery getTemplate(PathQuery pathQuery) {
        TemplateQuery tmpl = new TemplateQuery("template1", "title", 
                "comments", pathQuery);
        return tmpl;
    }
    
}
