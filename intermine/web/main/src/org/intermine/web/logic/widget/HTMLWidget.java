package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.web.logic.widget.config.WidgetConfig;

public class HTMLWidget extends Widget
{

    public HTMLWidget(WidgetConfig config) {
        super(config);
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<String> getElementInList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<List<String>> getExportResults(String[] selected) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<List<String[]>> getFlattenedResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getHasResults() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getNotAnalysed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void process() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void setNotAnalysed(int notAnalysed) {
        // TODO Auto-generated method stub
    }

}
