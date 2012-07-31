package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;

/**
 * Object field, used in header the summary of ReportObject
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

    /** @var shall we truncate the field value? */
    private boolean fieldDoNotTruncate;

    private String label = null;

    /**
     * Constructor
     * @param objectType unqualified class name
     * @param fieldName String
     * @param fieldValue Object
     * @param fieldDisplayerPage String
     * @param doNotTruncate bool
     */
    public ReportObjectField(String objectType, String fieldName,
            Object fieldValue, String fieldDisplayerPage, boolean doNotTruncate) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.fieldDisplayerPage = fieldDisplayerPage;
        this.fieldDoNotTruncate = doNotTruncate;
        this.pathString = objectType + "." + fieldName;
    }

    public ReportObjectField(String objectType, String fieldName,
            Object fieldValue, String fieldDisplayerPage, boolean doNotTruncate, String label) {
        this(objectType, fieldName, fieldValue, fieldDisplayerPage, doNotTruncate);
        this.label = label;
    }

    /**
     * Get the label to display in the webapp for this field. If there is
     * no label, returns the name of the field instead.
     * @return A human readable label.
     */
    public String getDisplayName() {
        if (label != null) {
            return label;
        } else {
            String[] parts = StringUtils.splitByCharacterTypeCamelCase(fieldName);
            String[] ucFirstParts = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                ucFirstParts[i] = StringUtils.capitalize(parts[i]);
            }
            return StringUtils.join(ucFirstParts, " ");
        }
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
     *
     * @return true if do not truncate
     */
    public boolean getDoNotTruncate() {
        return fieldDoNotTruncate;
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
