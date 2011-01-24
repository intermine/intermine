package org.intermine.webservice.server.output;

import java.util.List;

import org.intermine.webservice.server.exceptions.ServiceException;

public class JSONCountFormatter extends JSONFormatter {

    @Override
    public String formatResult(List<String> resultRow) {
        if (resultRow.size() != 1) {
            throw new ServiceException("Something is wrong - I got this"
                    + "result row: " + resultRow.toString()
                    + ", but I was expecting a count");
        } else {
            return "count:" + resultRow.get(0);
        }
    }

}
