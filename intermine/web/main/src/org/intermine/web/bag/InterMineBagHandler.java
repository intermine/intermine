package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.model.InterMineObject;
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
    private String bagDescription;
    private InterMineBag bag;
    private Map idToObjectMap;
    private IdUpgrader idUpgrader;
    
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
     * @param idUpgrader bag object id upgrader
     * @param idToObjectMap
     *            a Map from id to InterMineObject. This is used to create
     *            template objects to pass to createPKQuery() so that old bags
     *            can be used with new ObjectStores.
     */
    public InterMineBagHandler(ObjectStore uos, ObjectStore os, Map bags, Integer userId,
                               Map idToObjectMap, IdUpgrader idUpgrader) {
        this.uos = uos;
        this.os = os;
        this.bags = bags;
        this.userId = userId;
        this.idUpgrader = idUpgrader;
        this.idToObjectMap = idToObjectMap;
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
                bagDescription = attrs.getValue("description");
            }

            if (qName.equals("bagElement")) {
                String type = attrs.getValue("type");
                Integer id = new Integer(attrs.getValue("id"));

                if (bag == null) {
                    bag = new InterMineBag(userId, bagName, bagType, uos, os,
                                           Collections.EMPTY_SET);
                    bag.setDescription(bagDescription);
                }

                if (os.getObjectById(id) == null && idToObjectMap.containsKey(id)) {
                    // the id isn't in the database and we have an Item representing the object from
                    // a previous database
                    InterMineObject oldObject = (InterMineObject) idToObjectMap.get(id);

                    Set newIds = idUpgrader.getNewIds(oldObject, os);
                    Iterator newIdIter = newIds.iterator();
                    while (newIdIter.hasNext()) {
                        bag.add(new BagElement((Integer) newIdIter.next(), type));
                    }
                } else {
                    bag.add(new BagElement(id, type));
                }
            }
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    /**
     * @see DefaultHandler#endElement(String, String, String)
     */
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("bag")) {
            bags.put(bagName, bag);
            bag = null;
        }
    }
}
