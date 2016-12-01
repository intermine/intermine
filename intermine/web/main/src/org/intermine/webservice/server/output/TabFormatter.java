package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.web.logic.export.RowFormatterImpl;

/**
 * Formats data to tab separated data format.
 * @author Jakub Kulaviak
 **/
public class TabFormatter extends FlatFileFormatter
{
    /**
     * Constructor.
     */
    public TabFormatter() {
        setRowFormatter(new RowFormatterImpl("\t", true));
    }

    /**
     * Construct, specifying whether or not to quote.
     * @param quoted Whether or not to quote each field.
     */
    public TabFormatter(boolean quoted) {
        setRowFormatter(new RowFormatterImpl("\t", quoted));
    }
}
