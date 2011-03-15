package org.intermine.webservice.server.output;

import java.util.List;
import java.util.Map;

import org.intermine.web.logic.export.RowFormatter;
import org.intermine.webservice.server.StatusDictionary;

/**
 * A class to define behaviour common to flat files (csv, tsv)
 * @author alex
 *
 */
public abstract class FlatFileFormatter extends Formatter {

    private static final String ERROR_INTRO = "[ERROR] ";
    /** The key for the header columns **/
    public static final String COLUMN_HEADERS = "view";
    protected RowFormatter labourer = null;

    protected RowFormatter getRowFormatter() {
        return labourer;
    }

    protected void setRowFormatter(RowFormatter fmtr) {
        labourer = fmtr;
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatHeader(Map<String, Object> attributes) {
        if (attributes != null && attributes.containsKey(COLUMN_HEADERS)) {
            List<Object> columns = (List<Object>) attributes.get(COLUMN_HEADERS);
            return getRowFormatter().format(columns);
        }
        return "";
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatResult(List<String> resultRow) {
        return getRowFormatter().format((List) resultRow);
    }

    /** {@inheritDoc}} **/
    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        StringBuilder sb = new StringBuilder();
        if (errorCode != Output.SC_OK) {
            sb.append(ERROR_INTRO);
            sb.append(StatusDictionary.getDescription(errorCode));
            sb.append("\n");
            sb.append(ERROR_INTRO).append(errorMessage);
        }
        return sb.toString();
    }

}
