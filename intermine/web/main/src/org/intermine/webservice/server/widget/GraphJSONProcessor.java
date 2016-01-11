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

import org.json.JSONArray;

/**
 * A widget result processor that produces chart results in JSON.
 * @author Alex Kalderimis
 *
 */
public final class GraphJSONProcessor implements WidgetResultProcessor
{
    private static final WidgetResultProcessor INSTANCE = new GraphJSONProcessor();

    private GraphJSONProcessor() {
        // hidden
    }

    /** @return A widget result processor of some kind **/
    public static WidgetResultProcessor instance() {
        return INSTANCE;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        JSONArray ja = new JSONArray(row);
        return new LinkedList<String>(Arrays.asList(ja.toString()));
    }
}
