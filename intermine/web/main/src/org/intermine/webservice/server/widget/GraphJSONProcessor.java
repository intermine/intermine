package org.intermine.webservice.server.widget;

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
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;

public class GraphJSONProcessor implements WidgetResultProcessor
{
    private static final WidgetResultProcessor instance = new GraphJSONProcessor();

    private GraphJSONProcessor() {
        // Not to be instantiated.
    }

    public static WidgetResultProcessor instance() {
        return instance;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        JSONArray ja = new JSONArray(row);
        return new LinkedList<String>(Arrays.asList(ja.toString()));
    }
}
