package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.web.logic.export.RowFormatter;
import org.intermine.web.logic.export.RowFormatterImpl;

/**
 * Formats data to comma separated data format.
 *
 * @author Jakub Kulaviak
 **/
public class CSVFormatter extends FlatFileFormatter
{
	public CSVFormatter() {
		setRowFormatter(new RowFormatterImpl(",", true));
	}
}
