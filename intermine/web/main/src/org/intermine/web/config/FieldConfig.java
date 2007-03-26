package org.intermine.web.config;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * The webapp configuration for one field of a class.
 *
 * @author Kim Rutherford
 */

public class FieldConfig
{
    private String fieldExpr;
    private boolean doNotTruncate;
    private boolean showInSummary = true;
    private boolean showInInlineCollection = true;
    private boolean showInResults = true;
    private boolean sectionOnRight;
    private String sectionTitle;
    private boolean openByDefault;
    private String fieldExporter;
    private String displayer;

    /**
     * Set the JSTL expression for this field which will be evalutated in the request context when
     * the JSP is viewed.
     * @param fieldExpr the expression
     */
    public void setFieldExpr(String fieldExpr) {
        this.fieldExpr = fieldExpr;
    }

    /**
     * Return the JSTL expression for the object.
     * @return the expression
     */
    public String getFieldExpr() {
        return fieldExpr;
    }

    /**
     * If set to true, don't truncate long fields or put in a [View all...] link in the object
     * details page.
     * @param doNotTruncate do not truncate if true
     */
    public void setDoNotTruncate(boolean doNotTruncate) {
        this.doNotTruncate = doNotTruncate;
    }

    /**
     * Return the value of the doNotTruncate flag.
     * @return the value of the flag
     */
    public boolean getDoNotTruncate() {
        return doNotTruncate;
    }

    /**
     * Set the showInSummary flag.  If true, show this field in the summary section of the object
     * details page.
     * @param showInSummary the new value of the flag
     */
    public void setShowInSummary(boolean showInSummary) {
        this.showInSummary = showInSummary;
    }

    /**
     * Return the value of the showInSummary flag
     * @return the value of the flag
     */
    public boolean getShowInSummary() {
        return showInSummary;
    }

    /**
     * Set the showInInlineCollection flag.  If true, show this field in inline collections on the
     * object details page. 
     * @param showInInlineCollection the new value of the flag
     */
    public void setShowInInlineCollection(boolean showInInlineCollection) {
        this.showInInlineCollection = showInInlineCollection;
    }

    /**
     * Return the value of the showInInlineCollection flag
     *
     * @return the value of the flag
     */
    public boolean getShowInInlineCollection() {
        return showInInlineCollection;
    }

    /**
     * Set the showInResults flag.  If true, show this field when the corresponding object is shown
     * in a results table.
     * @param showInResults the new value of the flag
     */
    public void setShowInResults(boolean showInResults) {
        this.showInResults = showInResults;
    }

    /**
     * Return the value of the showInResults flag
     * @return the value of the flag
     */
    public boolean getShowInResults() {
        return showInResults;
    }

    /**
     * Set the class name of the FieldExporter to use when this field is viewed.
     * @param fieldExporter the FieldExporter name
     */
    public void setFieldExporter(String fieldExporter) {
        this.fieldExporter = fieldExporter;
    }

    /**
     * Return the class name of the FieldExporter to use when this field is viewed.
     * @return the FieldExporter name
     */
    public String getFieldExporter() {
        return fieldExporter;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object otherObject) {
        if (otherObject instanceof FieldConfig) {
            FieldConfig otherFc = (FieldConfig) otherObject;

            if (otherFc.fieldExporter == null && fieldExporter != null
                || otherFc.fieldExporter != null && fieldExporter == null) {
                return false;
            }

            if (otherFc.fieldExporter != null) {
                if (!otherFc.fieldExporter.equals(fieldExporter)) {
                    return false;
                }
            }

            return otherFc.fieldExpr.equals(fieldExpr)
                && otherFc.showInSummary == showInSummary
                && otherFc.showInInlineCollection == showInInlineCollection
                && otherFc.showInResults == showInResults;
        } else {
            return false;
        }
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return toString().hashCode();
    }
    
    /**
     * @see java.lang.String#toString
     */
    public String toString() {
        return "<fieldconfig fieldExpr=\"" + fieldExpr + "\" displayer=\"" + displayer
               + "\" doNotTruncate=\"" + doNotTruncate + "\" showInSummary=\"" + showInSummary
               + "\" showInInlineCollection=\"" + showInInlineCollection + "\" showInResults=\""
               + showInResults + "\""
               + (fieldExporter == null ? "" : " fieldExporter=" + fieldExporter) + "/>";
    }

    /**
     * Get whether this field should be uncollapsed if
     * sectionOnRight is true.
     * @return true if field should be uncollapsed by default
     */
    public boolean isOpenByDefault() {
        return openByDefault;
    }

    /**
     * Set whether this field should be uncollapsed if
     * sectionOnRight is true.
     * @param openByDefault true if field should be uncollapsed by default
     */
    public void setOpenByDefault(boolean openByDefault) {
        this.openByDefault = openByDefault;
    }

    /**
     * Whether this field should be rendered in its own section on the right side
     * of the page.
     * @return true if field should be rendered in its own section
     */
    public boolean isSectionOnRight() {
        return sectionOnRight;
    }

    /**
     * Set whether this field should be rendered in its own section on
     * the right side of the page.
     * @param sectionOnRight whether or not field should be rendered in its own section
     */
    public void setSectionOnRight(boolean sectionOnRight) {
        this.sectionOnRight = sectionOnRight;
    }

    /**
     * Get the section title (if sectionOnRight == true).
     * @return section title
     */
    public String getSectionTitle() {
        return sectionTitle;
    }

    /**
     * Set the section title (if sectionOnRight == true).
     * @param sectionTitle section title
     */
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    /**
     * Get the displayer for that field
     * 
     * @return the path to the jsp displayer
     */
    public String getDisplayer() {
        return displayer;
    }

    /**
     * Set the displayer
     * 
     * @param displayer
     *            the path to the jsp displayer
     */
    public void setDisplayer(String displayer) {
        this.displayer = displayer;
    }
}
