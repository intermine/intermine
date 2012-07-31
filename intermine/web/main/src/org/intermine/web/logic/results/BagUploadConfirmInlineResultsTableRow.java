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

/**
 * Used on BagUploadConfirmIssueController (List upload confirm step) to add an identifier the
 *  user was trying to resolve with the number of rows in the ResultsTable corresponding to the
 *  said identifier.
 *
 * @author radek
 */
public class BagUploadConfirmInlineResultsTableRow extends InlineResultsTableRow
{

    /** @var identifier we were trying to resolve */
    String identifier = null;
    /** @var number of rows in the containing table this identifier corresponds to */
    Integer rowSpan = null;
    /** @var show the identifier in this row? */
    Boolean showIdentifier = false;

    /**
     * Set to show the identifier in this row
     * @param showIdentifier switch
     */
    public void setShowIdentifier(Boolean showIdentifier) {
        this.showIdentifier = showIdentifier;
    }

    /**
     * Used from JSP
     *
     * @return true if we have an identifier set, ie the first row of a table that corresponds to
     *  an identifier we have tried to upload
     */
    public Boolean getShowIdentifier() {
        return showIdentifier;
    }

    /**
     * Set identifier
     * @param identifier to be set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Used from JSP
     *
     * @return identifier the user uploaded, check with getShowIdentifier() first
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set how many rows this identifier spans
     * @param rowSpan Integer
     */
    public void setRowSpan(Integer rowSpan) {
        this.rowSpan = rowSpan;
    }

    /**
     * Used from JSP in rowspan attr of <td>
     *
     * @return number of rows this identifier spans
     */
    public Integer getRowSpan() {
        return this.rowSpan;
    }

}
