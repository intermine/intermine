package org.intermine.web.logic.widget;

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

import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * Represents a widget with no data, it just diplays the "content" string.
 *
 * @author Julie Sullivan
 */
public class HTMLWidget extends Widget
{
    /**
     * @param config widget config
     */
    public HTMLWidget(WidgetConfig config) {
        super(config);
    }

    @Override
    public List<List<String>> getExportResults(String[] selected)
        throws Exception {
        return null;
    }

    @Override
    public List<List<Object>> getResults() {
        return null;
    }

    @Override
    public boolean getHasResults() {
        return false;
    }

    @Override
    public int getNotAnalysed() {
        return 0;
    }

    @Override
    public void process() {
        // nothing to do
    }

    @Override
    public void setNotAnalysed(int notAnalysed) {
        // nothing to do
    }

    @Override
    public PathQuery getPathQuery() {
        return null;
    }
}
