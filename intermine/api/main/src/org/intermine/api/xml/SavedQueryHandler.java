package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Extension of PathQueryHandler to handle parsing SavedQuerys
 * @author Xavier Watkins
 */
public class SavedQueryHandler extends PathQueryHandler
{
    private Map<String, SavedQuery> queries;
    private Date dateCreated;
    private String queryName;

    /**
     * Constructor
     * @param queries Map from saved query name to SavedQuery
     * @param savedBags Map from bag name to bag
     * @param version the version of the xml, an attribute on the profile manager
     */
    public SavedQueryHandler(Map<String, SavedQuery> queries,
            @SuppressWarnings("unused") Map savedBags, int version) {
        super(new HashMap<String, PathQuery> (), version);
        this.queries = queries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("saved-query".equals(qName)) {
            queryName = attrs.getValue("name");
            if (!StringUtils.isEmpty(attrs.getValue("date-created"))) {
                dateCreated = new Date(Long.parseLong(attrs.getValue("date-created")));
            }
        }
        super.startElement(uri, localName, qName, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if ("saved-query".equals(qName)) {
            queries.put(queryName, new SavedQuery(queryName, dateCreated, query));
            dateCreated = null;
        }
    }
}
