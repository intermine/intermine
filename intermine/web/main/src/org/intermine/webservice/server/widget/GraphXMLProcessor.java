package org.intermine.webservice.server.widget;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

public class GraphXMLProcessor implements WidgetResultProcessor
{

    private boolean headerPrinted = false;

    public GraphXMLProcessor() {
        super();
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        String cellTag = headerPrinted ? "i" : "h";
        String rowTag = headerPrinted ? "result" : "header";
        headerPrinted = true;
        StringBuilder sb = new StringBuilder("<" + rowTag + ">");
        sb.append(formatCell(cellTag, row));
        sb.append("</" + rowTag + ">");
        return new LinkedList<String>(Arrays.asList(sb.toString()));
    }

    @SuppressWarnings("rawtypes")
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
}
