package org.intermine.web.logic.export.rowformatters;

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
 * Row formatter formats line as comma separated strings.
 * @author Jakub Kulaviak
 **/
public class CSVRowFormatter extends RowFormatterImpl
{

    /**
     * Constructor.
     */
    public CSVRowFormatter() {
        super(",", true);
    }
}
