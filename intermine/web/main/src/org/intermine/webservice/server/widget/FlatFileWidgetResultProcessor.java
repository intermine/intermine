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

import java.util.LinkedList;
import java.util.List;

/**
 * A widget result processor that produces results in flat file format (tsv/csv)
 * @author Alex Kalderimis
 *
 */
public final class FlatFileWidgetResultProcessor implements WidgetResultProcessor
{

    private static final WidgetResultProcessor INSTANCE = new FlatFileWidgetResultProcessor();

    private FlatFileWidgetResultProcessor() {
        // Do not instantiate.
    }

    /** @return a widget result processor of some kind **/
    public static WidgetResultProcessor instance() {
        return INSTANCE;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        List<String> strings = new LinkedList<String>();
        for (Object o: row) {
            strings.add(o.toString());
        }
        return strings;
    }

}
