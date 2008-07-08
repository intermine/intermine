package org.intermine.webservice.client.template;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * The TemplateParameter class is simple class representing template parameter. Template parameter
 * is actually predefined constraint where you can specify constraint operation and constrained 
 * value. For example it can be constraint for gene length and you can specify if the gene length 
 * should be less, equal or greater then your specified value.
 * 
 * Parameters of some templates require extra value. See documentation of the template or InterMine
 * web page that generates for user template URL and displays it readily.
 *  
 * @author Jakub Kulaviak
 **/
public class TemplateParameter
{

    private String operation;
    
    private String value;

    private String extraValue;
    
    /**
     * @param operation operation 
     * @param value value
     * @param extraValue extra value
     */
    public TemplateParameter(String operation, String value, String extraValue) {
        super();
        this.operation = operation;
        this.value = value;
        this.extraValue = extraValue;
    }

    /**
     * @return extra value
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * @param extraValue extra value
     */
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }

    /**
     * @return operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Sets operation.
     * @param operation operation
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * Returns value.
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets value.
     * @param value value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @param operation operation
     * @param value value
     */
    public TemplateParameter(String operation, String value) {
        super();
        this.operation = operation;
        this.value = value;
    }    
}
