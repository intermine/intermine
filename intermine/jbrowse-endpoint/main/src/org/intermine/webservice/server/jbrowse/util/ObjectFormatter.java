package org.intermine.webservice.server.jbrowse.util;

import static org.apache.commons.lang.StringEscapeUtils.escapeJava;

import java.util.Map;

import org.intermine.webservice.server.output.JSONFormatter;

/**
 * Simpler version of JSONFormatter that doesn't add any
 * extra information, except in case of error.
 * @author alex
 *
 */
public class ObjectFormatter extends JSONFormatter {

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
