package org.intermine.webservice.server.widget;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;

public class XMLWidgetProcessor implements WidgetProcessor {

    private static final WidgetProcessor instance = new XMLWidgetProcessor();

    private XMLWidgetProcessor() {
        // Do not construct;
    }

    public static WidgetProcessor instance() {
        return instance;
    }

    @Override
    public List<String> process(String name, WidgetConfig widgetConfig) {
        StringBuilder sb = new StringBuilder("<result>");
        sb.append(formatCell("name", name));
        sb.append(formatCell("title", widgetConfig.getTitle()));
        sb.append(formatCell("description", widgetConfig.getDescription()));
        sb.append(formatCell("target", getClasses(widgetConfig.getTypeClass())));
        sb.append("</result>");
        return new LinkedList<String>(Arrays.asList(sb.toString()));
    }

    private String formatCell(String name, Object contents) {
        StringBuffer sb = new StringBuffer();
        if (contents instanceof List) {
            for (Object o: (List) contents) {
                sb.append(formatCell(name, o));
            }
        } else {
            sb.append("<" + name + ">");
            sb.append(StringEscapeUtils.escapeXml(contents.toString()));
            sb.append("</" + name + ">");
        }
        return sb.toString();
    }

    private List<String> getClasses(String tc) {
        List<String> ret = new LinkedList<String>();
        for (String s: StringUtils.split(tc, ",")) {
            ret.add(s.substring(s.lastIndexOf('.') + 1));
        }
        return ret;
    }

}
