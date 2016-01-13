package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Formattable;
import java.util.Formatter;

import org.intermine.pathquery.PathQuery;

/**
 * A Class for generating JavaScript that would run a given query.
 * @author Alexis Kalderimis
 *
 */
public class WebserviceJavaScriptCodeGenerator implements WebserviceCodeGenerator
{
    private static String error(String message) {
        return JSStrings.getString("ERROR", message);
    }

    private static String errorList(Collection<String> problems) {
        StringBuilder sb = new StringBuilder(JSStrings.getString("ERROR_LIST_INTRO"));
        for (String p: problems) {
            sb.append(JSStrings.getString("ERROR_LIST_ITEM", p));
        }
        sb.append(JSStrings.getString("ERROR_LIST_END"));
        return sb.toString();
    }

    /**
     * This method will generate code that can be run in a browser.
     *
     * @param wsCodeGenInfo a WebserviceCodeGenInfo object
     * @return the code as a string
     */
    @Override
    public String generate(WebserviceCodeGenInfo wsCodeGenInfo) {

        PathQuery query = wsCodeGenInfo.getQuery();

        if (query == null) {
            return error(JSStrings.getString("IS_NULL"));
        }
        if (!query.isValid()) {
            return errorList(query.verifyQuery());
        }
        if (query.getView().isEmpty()) {
            return error(JSStrings.getString("NO_FIELDS"));
        }

        final String url = wsCodeGenInfo.getServiceBaseURL();
        final String cdnLocation = wsCodeGenInfo.getProperty("head.cdn.location",
                "http://cdn.intermine.org");
        final String json = query.getJson();
        final String token = wsCodeGenInfo.getUserToken();

        StringBuffer sb = new StringBuffer()
              .append(JSStrings.getString("PRELUDE"))
              .append(String.format(JSStrings.getString("IMPORTS"), cdnLocation))
              .append(JSStrings.getString("PLACEHOLDER"))
              .append(JSStrings.getString("SCRIPT", new StringLiteral(url),
                  new StringLiteral(token), json));

        return sb.toString().replaceAll("\n", wsCodeGenInfo.getLineBreak());
    }

    private class StringLiteral implements Formattable
    {

        private String value;

        StringLiteral(String value) {
            this.value = value;
        }

        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            // Ignore flags, width, precision.
            if (value == null) {
                formatter.format("null");
            } else {
                formatter.format("'%s'", value);
            }
        }
    }
}
