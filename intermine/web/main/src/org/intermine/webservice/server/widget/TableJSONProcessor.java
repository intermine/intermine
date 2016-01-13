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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * Class to process the result returned by a table widgets in JSON format
 * @author dbutano
 *
 */
public class TableJSONProcessor implements WidgetResultProcessor
{

    private static final TableJSONProcessor INSTANCE = new TableJSONProcessor();

    /** @return a table json processor **/
    public static TableJSONProcessor instance() {
        return INSTANCE;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        Map<String, Object> backingMap = new HashMap<String, Object>();
        int rowSize = row.size();
        List<Object> descriptionList = new ArrayList<Object>();
        for (int index = 0; index < rowSize - 2; index++) {
            descriptionList.add(row.get(index));
        }
        backingMap.put("descriptions", descriptionList);
        backingMap.put("identifier", row.get(rowSize - 2));
        backingMap.put("matches", row.get(rowSize - 1));

        JSONObject jo = new JSONObject(backingMap);
        return new LinkedList<String>(Arrays.asList(jo.toString()));
    }
}
