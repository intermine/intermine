package org.intermine.webservice.client.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * <h1>A simple class representing the group of parameters that define a template constraint</h1>
 *
 * <p>A template parameter is a predefined constraint where you can specify the constraint
 * operation and the constrained value. For example it can be constraint for gene length
 * and you can specify if the gene length should be less, equal or greater then your
 * specified value. The user needs to supply the path identifier to identify the constraint, and
 * the code when there are multiple constraints on the same path. The user is free to supply
 * compatible values for the operation and value.</p>
 *
 * <p>Some constraint operators (such as <code>LOOKUP</code>) allow an extra value. See the
 * documentation for the template or any InterMine web page that generates template URLs
 * for examples.</p>
 *
 * <h3>Usage:</h3>
 *
 * <pre>
 * TemplateService service = getTemplateService();
 * String templateName = "some-template-name";
 * List<TemplateParameter> params = Arrays.asList(
 *      new TemplateParameter("Employee.age", "gt", "10", "B"),
 *      new TemplateParameter("Employee.age", "lt", "60", "C"));
 * List<List<String>> results = service.getAllResults(templateName, params);
 * </pre>
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
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
     * Create a new TemplateParameter for a simple value constraint.
     * @param path The path-string for this constraint.
     * @param op The operation you wish to use.
     * @param value The value the constraint should be run against.
     * @param extra Any extra constraining value this operation might need.
     * @param code The identifying code for this constraint. Needed when two constraints constrain
     *             the same path.
     */
    public TemplateParameter(String path, String op, String value, String extra, String code) {
        this.pathId = path;
        this.operation = op;
        this.value = value;
        this.values = null;
        this.extraValue = extra;
        this.code = code;
    }

    /**
     * Create a new TemplateParameter for a multi-value constraint.
     * @param path The path-string for this constraint.
     * @param op The operation you wish to use.
     * @param values The values the constraint should be run against. Must not be null, or empty.
     * @param code The identifying code for this constraint. Needed when two constraints constrain
     *             the same path.
     */
    public TemplateParameter(String path, String op, Collection<String> values, String code) {
        if (values == null || values.isEmpty()) {
            throw new NullPointerException("values must not be null, or empty.");
        }
        this.pathId = path;
        this.operation = op;
        this.values = new LinkedHashSet<String>(values);
        this.value = null;
        this.extraValue = null;
        this.code = code;
    }

    /**
     * Create a new TemplateParameter for a unary constraint (such as <code>IS NULL</code>).
     * @param path The path-string for this constraint.
     * @param op The operation you wish to use.
     * @param value The value the constraint should be run against.
     * @param code The identifying code for this constraint. Needed when two constraints constrain
     *             the same path.
     */
    public TemplateParameter(String path, String op, String value, String code) {
        this.operation = op;
        this.value = value;
        this.pathId = path;
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

    /** @return whether this constraint is a multi-value constraint **/
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
