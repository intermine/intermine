package org.intermine.webservice.server.jbrowse.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import static org.apache.commons.lang.StringEscapeUtils.escapeJava;

import java.util.Map;

import org.intermine.webservice.server.output.JSONFormatter;

/**
 * Simpler version of JSONFormatter that doesn't add any
 * extra information, except in case of error.
 * @author alex
 *
 */
public class ObjectFormatter extends JSONFormatter
{

    @Override
    public String formatHeader(Map<String, Object> attributes) {
        return "{";
    }

    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        StringBuilder sb = new StringBuilder();
        if (errorCode >= 400) {
            sb.append("\"error\":\"" + escapeJava(errorMessage) + "\"");
            sb.append(",\"statusCode\":" + errorCode);
        }
        sb.append("}");
        return sb.toString();
    }
}
