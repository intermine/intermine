package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.List;

/**
 * Formats data to Items XML format.
 * @author See version control
 *
 **/
public class ItemsXMLFormatter extends XMLFormatter
{
    /** {@inheritDoc}} **/
    @Override
    protected String getRootElement() {
        return "items";
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        for (String item: resultRow) {
            sb.append(item);
        }

        return sb.toString();
    }
}
