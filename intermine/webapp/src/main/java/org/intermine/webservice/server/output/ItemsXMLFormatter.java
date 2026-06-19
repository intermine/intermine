package org.intermine.webservice.server.output;

import java.util.List;

public class ItemsXMLFormatter extends XMLFormatter
{
    /** {@inheritDoc}} **/
    @Override
    protected String getRootElement() {
        return "items";
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        for (String item: resultRow) {
            sb.append(item);
        }

        // System.out.println("formatResult: ");
        // System.out.println(sb.toString());

        return sb.toString();
    }
}
