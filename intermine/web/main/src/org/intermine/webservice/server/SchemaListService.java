package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONArray;

/**
 * Serve up the list of schemata that we have.
 * @author Alexis Kalderimis
 *
 */
public class SchemaListService extends WebService
{

    /**
     * Constructor
     * @param im InterMine settings
     */
    public SchemaListService(InterMineAPI im) {
        super(im);
    }

    /*
     * @see org.intermine.webservice.server.WebService#execute(
     * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void execute() throws Exception {

        Set<String> schemata = new HashSet<String>(
            Arrays.asList(webProperties.getProperty("schema.filenames", "").split(",")));
        output.setHeaderAttributes(getHeaderAttributes());

        JSONArray ja = new JSONArray(schemata);
        output.addResultItem(Collections.singletonList(ja.toString()));
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"schemata:\"");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        }
        return attributes;
    }

    @Override
    protected String getDefaultFileName() {
        return "schemata.json";
    }

    @Override
    public int getFormat() {
        if (hasCallback()) {
            return JSONP_FORMAT;
        } else {
            return JSON_FORMAT;
        }
    }

}
