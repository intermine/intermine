package org.intermine.webservice.server.output;

import java.util.List;
import java.util.Map;


public class HTMLTableFormatter extends XMLFormatter {

    public static final String KEY_COLUMN_HEADERS = "headers";


    protected String getRootElement() {
        return "table";
    }

    protected String getRowElement() {
        return "tr";
    }

    protected String getItemElement() {
        return "td";
    }
    
    protected String getErrorElement() {
        return "div";
    }

    protected String getMessageElement() {
        return "h3";
    }

    protected String getCauseElement() {
        return "p";
    }

    @Override
    protected String getProcessingInstruction() {
        return "";
    }

    protected void handleHeaderAttributes(Map<String, Object> attributes, StringBuilder sb) {
        sb.append(">");
        if (attributes != null && attributes.containsKey(KEY_COLUMN_HEADERS)) {
            List<String> headers = (List<String>) attributes.get(KEY_COLUMN_HEADERS);
            sb.append("<thead><tr>");
            for (String header: headers) {
                addElement(sb, "th", header);
            }
            sb.append("</tr></thead>");
        }
    }
}
