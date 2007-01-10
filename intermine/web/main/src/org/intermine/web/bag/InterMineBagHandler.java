package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A handler for turning XML bags data into an InterMineIdBag.
 *
 * @author Mark Woodbridge
 * @author Kim Rutherford
 */
public class InterMineBagHandler extends DefaultHandler
{

    private ObjectStore uos;
    private ObjectStore os;
    private Map bags;
    private Integer userId;

    private String bagName;
    private String bagType;
    private InterMineBag bag;
//    private List row;

    /**
     * Create a new InterMineBagHandler object.
     * @param uos
     *            UserProfile ObjectStore
     * @param os
     *            ObjectStore used to resolve object ids
     * @param bags
     *            Map from bag name to InterMineIdBag - results are added to this
     *            Map
     * @param userId the id of the user
     */
    public InterMineBagHandler(ObjectStore uos, ObjectStore os, Map bags, Integer userId) {
        this.uos = uos;
        this.bags = bags;
        this.userId = userId;
    }

    /**
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        try {
            if (qName.equals("bag")) {
                bagName = attrs.getValue("name");
                bagType = attrs.getValue("type");
            }

            if (qName.equals("bagElement")) {
                String type = attrs.getValue("type");
                Integer id = new Integer(attrs.getValue("id"));

                if (bag == null) {
                    bag = new InterMineBag(userId, bagName, bagType, uos, os,
                                           Collections.EMPTY_SET);
                }
                
//                if (row == null) {
//                    row = new ArrayList();
//                }
                
                bag.add(new BagElement(id, type));
            }
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    /**
     * @see DefaultHandler#endElement
     */
    public void endElement(String uri, String localName, String qName) {
//        if (qName.equals("row")) {
//            bag.add(row);
//            row = null;
//        }
        if (qName.equals("bag")) {
            bags.put(bagName, bag);
            bag = null;
        }
    }
}
