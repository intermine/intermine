package org.intermine.webservice.client.template;

import java.util.Collection;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * The TemplateParameter class is simple class representing template parameter. Template parameter
 * is predefined constraint where you can specify constraint operation and constrained
 * value. For example it can be constraint for gene length and you can specify if the gene length
 * should be less, equal or greater then your specified value.
 *
 * Parameters of some templates require extra value. See documentation of the template or InterMine
 * web page that generates template URL for user and displays it readily.
 *
 * @author Jakub Kulaviak
 **/
public class TemplateParameter
{

    private final String operation;

    private final String value;

    private final String extraValue;

    private final String pathId;

    private final Collection<String> values;

    private final String code;

    /**
     * Create a new TemplateParameter.
     * @param pathId The path-string for this constraint.
     * @param operation The operation you wish to use.
     * @param value The value the constraint should be run against.
     * @param extraValue Any extra constraining value this operation might need.
     */
    public TemplateParameter(String pathId, String operation,
            String value, String extraValue, String code) {
        super();
        this.pathId = pathId;
        this.operation = operation;
        this.value = value;
        this.values = null;
        this.extraValue = extraValue;
        this.code = code;
    }

    public TemplateParameter(String pathId, String operation, Collection<String> values, String code) {
        super();
        this.pathId = pathId;
        this.operation = operation;
        this.values = values;
        this.value = null;
        this.extraValue = null;
        this.code = code;
    }

    /**
     * Create a new TemplateParameter.
     * @param pathId The path-string for this constraint.
     * @param operation The operation you wish to use.
     * @param value The value the constraint should be run against.
     */
    public TemplateParameter(String pathId, String operation, String value, String code) {
        super();
        this.operation = operation;
        this.value = value;
        this.pathId = pathId;
        this.extraValue = null;
        this.values = null;
        this.code = code;
    }

    /** @return the provided code **/
    public String getCode() {
        return code;
    }

    /**
     * @return the path-string associated with this constraint.
     */
    public String getPathId() {
        return pathId;
    }

    /**
     * @return extra value
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * @return The collection of multi-values
     */
    public Collection<String> getValues() {
        return values;
    }

    public boolean isMultiValue() {
        return values != null;
    }

    /**
     * @return operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns value.
     * @return value
     */
    public String getValue() {
        return value;
    }
}
