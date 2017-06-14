package org.intermine.web.struts;

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
 * A form to hold options for CSV and tab separated output.
 * @author Kim Rutherford
 */
public class CSVExportForm extends TableExportForm
{
    private static final long serialVersionUID = 1L;

    private static final String CSV_FORMAT = "format";

    /**
     * Return the format field ("csv", "tab", ...)
     * @return the format
     */
    public String getFormat() {
        return (String) getExtraParams().get(CSV_FORMAT);
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return getFormat();
    }

    /**
     * Set the format.
     * @param format the new format
     */
    public void setFormat(String format) {
        getExtraParams().put(CSV_FORMAT, format);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() {
        super.initialise();
        getExtraParams().put(CSV_FORMAT, "csv");
    }

}
