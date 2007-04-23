package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of DefaultHandler to handle parsing full XML.
 *
 * @author Kim Rutherford
 */

public class FullHandler extends DefaultHandler
{
    private List items = null;
    private Item currentItem = null;
    private ItemFactory itemFactory;
    private String currentCollectionName;

    /**
     * Create a new FullHandler object.
     */
    public FullHandler () {
        super();
        itemFactory = new ItemFactory();
        items = new ArrayList();
    }

    /**
     * Return the Items that have been read with this handler.
     * @return the new Items
     */
    public List getItems() {
        return items;
    }
    

    /**
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {

        if (qName.equals("item")) {
            currentItem = itemFactory.makeItem();
            currentItem.setClassName(attrs.getValue("class"));
            currentItem.setIdentifier(attrs.getValue("id"));
            if (attrs.getValue("implements") != null) {
                currentItem.setImplementations(attrs.getValue("implements"));
            }
        }
        if (qName.equals("attribute")) {
            currentItem.setAttribute(attrs.getValue("name"), attrs.getValue("value"));
        }
        if (qName.equals("reference")) {
            if (currentCollectionName == null) {
                currentItem.setReference(attrs.getValue("name"), attrs.getValue("ref_id"));
            } else {
                // a reference element within a collection element
                currentItem.addToCollection(currentCollectionName, attrs.getValue("ref_id"));
            }
        }
        if (qName.equals("collection")) {
            currentCollectionName = attrs.getValue("name");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("item")) {
            finishedItem(currentItem);
            currentItem = null;
        }
        if (qName.equals("collection")) {
            currentCollectionName = null;
        }
    }

    /**
     * Method that does something useful with the finished Item
     *
     * @param item an Item
     */
    public void finishedItem(Item item) {
        items.add(item);
    }
}
