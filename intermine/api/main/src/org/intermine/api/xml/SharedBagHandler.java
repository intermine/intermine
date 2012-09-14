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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handle parsing of shared bags
 * @author dbutano
 */
public class SharedBagHandler extends DefaultHandler
{
    private List<Map<String, String>> sharedBags;
    private Map<String, String> sharedBag;

    /**
     * Constructor
     * @param sharedBags list of map (name, dateCreated) representing the shared bag
     */
    public SharedBagHandler(List<Map<String, String>> sharedBags) {
        this.sharedBags = sharedBags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("shared-bag".equals(qName)) {
            sharedBag = new HashMap<String, String>();
            sharedBag.put("name", attrs.getValue("name"));
            if (!StringUtils.isEmpty(attrs.getValue("date-created"))) {
                sharedBag.put("dateCreated", attrs.getValue("date-created"));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if ("shared-bag".equals(qName)) {
            sharedBags.add(sharedBag);
        }
    }
}
