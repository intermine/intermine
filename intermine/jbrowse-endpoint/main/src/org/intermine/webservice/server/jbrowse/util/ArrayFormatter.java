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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Not good practice, but this is how JBrowse wants results, so when in
 * Rome.
 * @author Alex Kalderimis
 *
 */
public class ArrayFormatter extends org.intermine.webservice.server.output.Formatter
{

    @Override
    public String formatHeader(Map<String, Object> attributes) {
        return "[";
    }

    @Override
    public String formatResult(List<String> resultRow) {
        return StringUtils.join(resultRow, ",");
    }

    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        if (errorCode >= 400) {
            JSONObject error = new JSONObject();
            try {
                error.put("message", errorMessage);
                error.put("code", errorCode);
                return error.toString() + "]";
            } catch (JSONException e) {
                return "{\"message\":\"JSON formatting error\",\"code\":500\"}";
            }
        }
        return "]";
    }

}
