package org.intermine.webservice.server.output;

import java.util.List;
import java.util.Map;

import org.intermine.web.logic.export.RowFormatter;
import org.intermine.web.logic.export.RowFormatterImpl;
import org.intermine.webservice.server.StatusDictionary;

public abstract class FlatFileFormatter extends Formatter {

	private static final String errorIntro = "[ERROR] ";
	public static final String HEADER_COLUMNS = "view";
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
    	if (attributes != null && attributes.containsKey(HEADER_COLUMNS)) {
    		List<Object> columns = (List<Object>) attributes.get(HEADER_COLUMNS);
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
    		sb.append(errorIntro);
	        sb.append(StatusDictionary.getDescription(errorCode));
	        sb.append("\n");
    		sb.append(errorIntro).append(errorMessage);
    	}
    	return sb.toString();
    }
	
}
