package org.intermine.webservice.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.TestUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.Node;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.PathQueryBinding;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateQueryBinding;

import junit.framework.TestCase;


/**
 * @author Jakub Kulaviak
 **/
public class TemplateResultLinkGeneratorTest extends TestCase
{

    public void testGenerateServiceLink() {
        TemplateQuery tmpl = new TemplateQuery("template1", "title", 
                "description", "comments", getPathQuery(), "keywords");
        tmpl.addNode("");
        String link = new TemplateResultLinkGenerator().generateServiceLink("http://localhost:8080/query", tmpl);
        assertEquals("http://localhost:8080/query/data/template/results?" +
        		"name=template1&op1=%3D&value1=Drosophila_melanogaster&" +
        		"extraValue1=Drosophila_extraValue", link);
    }

    private PathQuery getPathQuery() {
        PathQuery ret = new PathQuery(TestUtil.getModel());
        ret.addNode("Gene");
        Constraint c = new Constraint(ConstraintOp.EQUALS, "Drosophila_melanogaster", true, 
                "description", "code", "identifier", "Drosophila_extraValue");        
        ret.getNode("Gene").getConstraints().add(c);
        return ret;
    }
    
    
}
