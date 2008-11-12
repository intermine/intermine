package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.List;

import org.intermine.webservice.server.WebServiceInput;

/**
 * TemplateResultInput is parameter object representing parameters for 
 * TemplateResultService web service.  
 * @author Jakub Kulaviak
 **/
public class TemplateResultInput extends WebServiceInput 
{
    private String name;
    
    private List<ConstraintLoad> constraints;
    
    private boolean computeTotalCount = false;

    private String layout;
    
    /**
     * @return layout string specifying result table layout
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout string specifying result table layout
     */
    public void setLayout(String layout) {
        this.layout = layout;
    }

    /**
     * Returns template name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets template name.
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets constraints.
     * @param constraints constraints
     */
    public void setConstraints(List<ConstraintLoad> constraints) {
        this.constraints = constraints;
    }

    /**
     * Returns constraints.
     * @return constraints
     */
    public List<ConstraintLoad> getConstraints() {
        return constraints;
    }

    /**
     * 
     * @return true if total count  should be displayed else false
     */
    public boolean isComputeTotalCount() {
        return computeTotalCount;
    }

    /**
     * 
     * @param computeTotalCount set true if total count should be displayed else false
     */
    public void setComputeTotalCount(boolean computeTotalCount) {
        this.computeTotalCount = computeTotalCount;
    }
}
