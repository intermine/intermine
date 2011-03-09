/**
 *
 */
package org.intermine.webservice.server.output;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author alex
 *
 */
public class JSONFormatter extends Formatter {

    private boolean hasCallback = false;

    /**
     * The key for the callback
     */
    public static final String KEY_CALLBACK = "callback";

    /**
     * Constructor
     */
    public JSONFormatter() {
        //empty constructor
    }

    /**
     * Add the opening brace, and a call-back if any
     * @see org.intermine.webservice.server.output.Formatter#formatHeader(java.util.Map)
     * @return the header
     * @param attributes the attributes passed in from the containing output
     */
    @Override
    public String formatHeader(Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder();
        if (attributes.get(KEY_CALLBACK) != null) {
            hasCallback = true;
            sb.append(attributes.get(KEY_CALLBACK)).append("(");
        }
        sb.append("{");
        return sb.toString();
    }

    /**
     * In normal cases a list with a single JSON string item is expected.
     * But just in case, this formatter will simply join any strings
     * it gets given, delimiting with a comma. It is the responsibility of
     * whoever is feeding me these lines to add any necessary commas between
     * them.
     * @see org.intermine.webservice.server.output.Formatter#formatResult(java.util.List)
     * @param resultRow the row as a list of strings
     * @return A formatted result line, or the empty string if the row is empty
     */
    @Override
    public String formatResult(List<String> resultRow) {
        if (resultRow.isEmpty()) { return ""; }
        Iterator<String> iter = resultRow.iterator();
        StringBuffer buffer = new StringBuffer(iter.next());
        while (iter.hasNext()) {
            buffer.append(",").append(iter.next());
        }
        return buffer.toString();
    }

    /**
     * Put on the final brace, and close the call-back bracket if needed
     * @see org.intermine.webservice.server.output.Formatter#formatFooter()
     * @return The formatted footer string.
     */
    @Override
    public String formatFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("}");
        if (hasCallback) {
            sb.append(");");
        }
        return sb.toString();
    }

}
