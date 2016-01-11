package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Represents an additional converter config
 *
 * @author julie
 */
public class AdditionalConverter
{
    private String constraintPath;
    private String targetType;
    private String className;
    private String title;
    private String urlField;

    /**
     * Create a new AdditionalConverter object.
     * @param constraintPath path to constraint, eg. Employee.department
     * @param targetType type of object to convert
     * @param className name of converter, eg. org.intermine.bio.web.logic.OrthologueConverter
     * @param title heading for JSP, eg. Orthologues
     * @param urlField parameter in portal URL
     */
    public AdditionalConverter(String constraintPath, String targetType, String className,
            String title, String urlField) {
        this.constraintPath = constraintPath;
        this.targetType = targetType;
        this.className = className;
        this.title = title;
        this.urlField = urlField;
    }

    /**
     * @return path to constrain, eg. Employee.department
     */
    public String getConstraintPath() {
        return constraintPath;
    }

    /**
     * @return simple name of class to query for, eg. Employee
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * @return class name for converter, eg. org.intermine.bio.web.logic.OrthologueConverter
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return title of converter
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return url params for this converter
     */
    public String getUrlField() {
        return urlField;
    }
}
