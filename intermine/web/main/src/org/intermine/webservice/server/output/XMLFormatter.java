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
 * Formats data to xml format.
 * @author Jakub Kulaviak
 **/
public class XMLFormatter extends Formatter
{

    /** {@inheritDoc}} **/
    @Override
    public String formatHeader(Map<String, String> attributes) {
        StringBuilder sb = new  StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<ResultSet ");
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                sb.append(key + "=\"" + attributes.get(key) + "\" ");
            }
        }
        sb.append(">");
        return sb.toString();
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Result>");
        for (String s : resultRow) {
            sb.append("<i>");
            sb.append(s);
            sb.append("</i>");
        }
        sb.append("</Result>");
        return sb.toString();
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatFooter() {
        return "</ResultSet>";
    }
}
