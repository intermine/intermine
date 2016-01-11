package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.List;
import java.util.Map;

import org.intermine.webservice.server.WebServiceInput;

/**
 * TemplateResultInput is parameter object representing parameters for
 * TemplateResultService web service.
 * @author Jakub Kulaviak
 **/
public class TemplateResultInput extends WebServiceInput
{
    private String name;

    private Map<String, List<ConstraintInput>> constraints;

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
    public void setConstraints(Map<String, List<ConstraintInput>> constraints) {
        this.constraints = constraints;
    }

    /**
     * Returns constraints.
     * @return constraints
     */
    public Map<String, List<ConstraintInput>> getConstraints() {
        return constraints;
    }
}
