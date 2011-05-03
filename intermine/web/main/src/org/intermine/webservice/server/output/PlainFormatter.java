package org.intermine.webservice.server.output;

import java.util.List;
import java.util.Map;

public class PlainFormatter extends Formatter {

    @Override
    public String formatHeader(Map<String, Object> attributes) {
        return "";
    }

    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        boolean needsComma = false;
        for (String item: resultRow) {
            sb.append(item);
            if (needsComma) {
                sb.append(",");
            }
            needsComma = true;
        }
        return sb.toString();
    }

    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        return "";
    }


}
