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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

/**
 * A service for listing the available widgets.
 * @author Alex Kalderimis
 *
 */
public class AvailableWidgetsService extends JSONService
{

    /**
     * Constructor
     * @param im The InterMine application object.
     */
    public AvailableWidgetsService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        WebConfig webConfig = InterMineContext.getWebConfig();
        Map<String, WidgetConfig> widgetDetails = webConfig.getWidgets();

        WidgetProcessor processor = getProcessor();
        Iterator<Entry<String, WidgetConfig>> it = widgetDetails.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, WidgetConfig> pair = it.next();
            List<String> row = processor.process(pair.getKey(), pair.getValue());
            if (!formatIsFlatFile() && it.hasNext()) {
                row.add("");
            }
            output.addResultItem(row);
        }
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"widgets\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        return attributes;
    }

    private WidgetProcessor getProcessor() {
        if (formatIsJSON()) {
            return JSONWidgetProcessor.instance();
        } else if (formatIsXML()) {
            return XMLWidgetProcessor.instance();
        } else {
            return FlatWidgetProcessor.instance();
        }
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new WidgetXMLFormatter());
    }

    private class WidgetXMLFormatter extends XMLFormatter
    {
        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");
        }

    }


}
