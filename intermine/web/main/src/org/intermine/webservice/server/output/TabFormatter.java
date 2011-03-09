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



/**
 * Formats data to tab separated data format.
 * @author Jakub Kulaviak
 **/
public class TabFormatter extends Formatter
{

    /** {@inheritDoc}} **/
    @Override
    public String formatHeader(Map<String, String> attributes) {
        return "";
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultRow.size(); i++) {
            sb.append(resultRow.get(i));
            if (i != resultRow.size() - 1) {
                sb.append("\t");
            }
        }
        return sb.toString();
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatFooter() {
        return "";
    }
}
