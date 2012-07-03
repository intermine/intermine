package org.intermine.webservice.server.output;

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.LinkRedirectManager;
import org.intermine.api.results.ResultCell;
import org.intermine.model.InterMineObject;
import org.intermine.web.logic.PortalHelper;
import org.json.JSONObject;

public class TableCellFormatter
{

    private static final String CELL_KEY_URL = "url";
    private static final String CELL_KEY_VALUE = "value";
    private static final String CELL_KEY_CLASS = "class";
    private static final String CELL_KEY_COLUMN = "column";
    private static final String CELL_KEY_ID = "id";

    private final LinkRedirectManager redirector;
    private final InterMineAPI im;
    
    public TableCellFormatter(InterMineAPI im) {
        this.im = im;
        this.redirector = im.getLinkRedirector();
    }
    
    public Map<String, Object> toMap(ResultCell cell) {
        Map<String, Object> mapping = new HashMap<String, Object>();
        if (cell == null) {
            mapping.put(CELL_KEY_URL, null);
            mapping.put(CELL_KEY_VALUE, null);
            mapping.put(CELL_KEY_COLUMN, null);
        } else {
            String link = null;
            // Try to generate a link using the redirector.
            if (redirector != null && cell.getObject() instanceof InterMineObject) {
                link = redirector.generateLink(im, (InterMineObject) cell.getObject());
            }
            if (link == null) {
                link = PortalHelper.generateReportPath(cell);
            }
            mapping.put(CELL_KEY_URL, link);
            mapping.put(CELL_KEY_CLASS, cell.getType());
            mapping.put(CELL_KEY_ID, cell.getId());
            mapping.put(CELL_KEY_COLUMN, cell.getPath().toStringNoConstraints());
            mapping.put(CELL_KEY_VALUE, cell.getField());
        }
        return mapping;
    }

    /**
     * Get the JSONObject that represents each cell in the results row
     * @param cell The result element with the data
     * @return A JSONObject
     */
    public JSONObject toJSON(ResultCell cell) {
        Map<String, Object> mapping = toMap(cell);
        JSONObject ret = new JSONObject(mapping);
        return ret;
    }
}
