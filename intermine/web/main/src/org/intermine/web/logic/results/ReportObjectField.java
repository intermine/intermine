package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.TypeUtil;

/**
 * Object field.
 *
 * @author Radek Stepan
 */
public class ReportObjectField
{
    /**
     * @String field name (key, expression)
     */
    private String fieldName;

    /**
     * @Object field value
     */
    private Object fieldValue;

    /**
     * @String link to a custom field displayer
     */
    private String fieldDisplayerPage;

    /**
     * @String path string (e.g. Gene.primaryIdentifier)
     */
    private String pathString;

    /**
     * Constructor
     * @param objectType String
     * @param fieldName String
     * @param fieldValue Object
     * @param fieldDisplayerPage String
     */
    public ReportObjectField(String objectType, String fieldName,
            Object fieldValue, String fieldDisplayerPage) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.fieldDisplayerPage = fieldDisplayerPage;
        // form path string from an unqualified name and a field name
        this.pathString = TypeUtil.unqualifiedName(objectType) + "." + fieldName;
    }

    /**
     * Get field name
     * @return String
     */
    public String getName() {
        return fieldName;
    }

    /**
     * Get field value
     * @return Object
     */
    public Object getValue() {
        return fieldValue;
    }

    /**
     * Return path to a custom displayer
     * @return String
     */
    public String getDisplayerPage() {
        return fieldDisplayerPage;
    }

    /**
     * Does the field have a custom displayer defined for the value?
     * @return boolean
     */
    public boolean getValueHasDisplayer() {
        return fieldDisplayerPage != null;
    }

    /**
     * Get a path string to fetch field descriptions
     * @return String
     */
    public String getPathString() {
        return pathString;
    }

}
