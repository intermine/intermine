package org.intermine.webservice.server.widget;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

public class AvailableWidgetsService extends JSONService {

    public AvailableWidgetsService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        WebConfig webConfig = SessionMethods.getWebConfig(request);
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

    protected Output makeXMLOutput(PrintWriter out) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new WidgetXMLFormatter());
    }

    private class WidgetXMLFormatter extends XMLFormatter {

        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");
        }

    }


}
