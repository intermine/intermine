package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.web.logic.export.RowFormatter;
import org.intermine.web.logic.export.RowFormatterImpl;



/**
 * Formats data to comma separated data format.
 *
 * @author Jakub Kulaviak
 **/
public class CSVFormatter extends Formatter
{

    /** {@inheritDoc}} **/
    @Override
    public String formatHeader(Map<String, String> attributes) {
        return "";
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        RowFormatter labourer = new RowFormatterImpl(",", true);
        return labourer.format((List) resultRow);
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatFooter() {
        return "";
    }
}
