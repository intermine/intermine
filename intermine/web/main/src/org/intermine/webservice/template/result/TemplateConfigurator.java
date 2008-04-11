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

import java.util.Iterator;
import java.util.List;

import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.WebServiceException;


/**
 * Configures original template. Old constraints are replaced with the similar 
 * new constraints, that have different values.
 * @author Jakub Kulaviak
 **/
public class TemplateConfigurator
{ 

    private Iterator<ConstraintLoad> newConstraintIt;    
   
    /**
     * Makes copy of original template and configures it with new constraints that have
     * new values. New constraints must correspond to old constraints. Actually this creates
     * very similar TemplateQuery with only different values.
     * @param origTemplate original template
     * @param newConstraints new constraints
     * @return new template
     */
    public TemplateQuery getConfiguredTemplate(TemplateQuery origTemplate,
            List<ConstraintLoad> newConstraints) {
        TemplateQuery template = (TemplateQuery) origTemplate.clone();
        newConstraintIt = newConstraints.iterator();
        List<Constraint> contraints = template.getAllEditableConstraints(); 
        for (int i = 0; i < contraints.size(); i++) {
            Constraint c = contraints.get(i);
            ConstraintLoad load = nextNewConstraint();
            Object extraValue = 
                (load.getExtraValue() != null) ? load.getExtraValue(): c.getExtraValue();
            Constraint newConstraint = new Constraint(load.getConstraintOp(), load.getValue(), 
                    true, c.getDescription(), c.getCode(), c.getIdentifier(), extraValue); 
            contraints.set(i, newConstraint);
        }
        return template;
    }

    private ConstraintLoad nextNewConstraint() {
        if (newConstraintIt.hasNext()) {
            return newConstraintIt.next();
        } else {
            throw new WebServiceException("There is insufficient number of constraints in your " 
                    + "request. Template has more constrains than you have specified.");
        }
    }
}
