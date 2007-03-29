package org.intermine.web.logic.query;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Extension of PathQueryHandler to handle parsing SavedQuerys
 */
public class SavedQueryHandler extends PathQueryHandler
{
    Map queries;
    Date dateExecuted;
    Date dateCreated;
    String queryName;

    /**
     * Constructor
     * @param queries Map from saved query name to SavedQuery
     * @param savedBags Map from bag name to bag
     * @param classKeys class key fields for the model
     */
    public SavedQueryHandler(Map queries, Map savedBags, Map classKeys) {
        super(new HashMap(), savedBags, classKeys);
        this.queries = queries;
    }

    /**
     * @see DefaultHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("saved-query")) {
            queryName = attrs.getValue("name");
            if (attrs.getValue("date-created") != null) {
                dateCreated = new Date(Long.parseLong(attrs.getValue("date-created")));
            }
        }
        super.startElement(uri, localName, qName, attrs);
    }
    
    /**
     * @see DefaultHandler#endElement
     */
    public void endElement(String uri, String localName, String qName) {
        super.endElement(uri, localName, qName);
        if (qName.equals("saved-query")) {
            queries.put(queryName, new SavedQuery(queryName, dateCreated, query));
            dateCreated = null;
            dateExecuted = null;
        }
    }
}