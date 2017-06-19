package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * A processor for returning results from graph widgets in XML.
 * @author Alex Kalderimis
 *
 */
public class GraphXMLProcessor implements WidgetResultProcessor
{

    private boolean headerPrinted = false;

    /** Constructor, obvs **/
    public GraphXMLProcessor() {
        super();
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        String cellTag = headerPrinted ? "i" : "h";
        String rowTag = headerPrinted ? "result" : "header";
        headerPrinted = true;
        StringBuilder sb = new StringBuilder("<" + rowTag + ">");
        sb.append(formatCell(cellTag, row));
        sb.append("</" + rowTag + ">");
        return new LinkedList<String>(Arrays.asList(sb.toString()));
    }

    @SuppressWarnings("rawtypes")
    private String formatCell(String name, Object contents) {
        StringBuffer sb = new StringBuffer();
        if (contents instanceof List) {
            for (Object o: (List) contents) {
                sb.append(formatCell(name, o));
            }
        } else {
            sb.append("<" + name + ">");
            sb.append(StringEscapeUtils.escapeXml(contents.toString()));
            sb.append("</" + name + ">");
        }
        return sb.toString();

    }
}

