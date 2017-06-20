package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.webservice.server.WebServiceConstants;


/**
 * @author Jakub Kulaviak, dbutano
 **/
public class TemplateResultLinkGeneratorTest extends TestCase
{

    private final String prefix = "http://localhost:8080/query/" + WebServiceConstants.MODULE_NAME;
    public void testExtraValueLink() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        PathConstraint c1 = new PathConstraintLookup("Gene.name", "zen", "Drosophila_melanogaster");
        TemplateQuery tmpl = new TemplateQuery("template1", "title", "comments", ret);
        tmpl.addConstraint(c1);
        tmpl.setEditable(c1, true);
        String link = new TemplateResultLinkGenerator().getHtmlLink("http://localhost:8080/query",
                                                                    tmpl);

        String expected = prefix + "/template/results?name=template1&constraint1=Gene.name&op1=LOOKUP&value1=zen&"
        + "extra1=Drosophila_melanogaster&format=html&size=" + TemplateResultLinkGenerator.DEFAULT_RESULT_SIZE + "&layout=minelink|paging";

        assertEquals(expected, link);
    }

    public void testMultipleConstraintsLink() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        PathConstraint c1 = new PathConstraintAttribute("Gene.name", ConstraintOp.MATCHES, "zen");
        PathConstraint c2 = new PathConstraintAttribute("Gene.length", ConstraintOp.LESS_THAN,
                                                        "100");
        TemplateQuery tmpl = new TemplateQuery("template1", "title", "comments", ret);
        tmpl.addConstraint(c1);
        tmpl.setEditable(c1, true);
        tmpl.addConstraint(c2);
        tmpl.setEditable(c2, true);
        String link = new TemplateResultLinkGenerator()
                          .getHtmlLink("http://localhost:8080/query", tmpl);
        String expected = prefix + "/template/results?name=template1"
            + "&constraint1=Gene.name&op1=LIKE&value1=zen"
            + "&constraint2=Gene.length&op2=lt&value2=100"
            + "&format=html&size=" + TemplateResultLinkGenerator.DEFAULT_RESULT_SIZE
            + "&layout=minelink|paging";

        assertEquals(expected, link);
    }

}
