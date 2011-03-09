package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.bag.IdUpgrader;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
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
    private static final Logger LOG = Logger.getLogger(InterMineBagHandler.class);

    private ObjectStoreWriter uosw;
    private ObjectStoreWriter osw;
    private Map<String, InterMineBag> bags;
    private Integer userId;
    private Model model;

    private String bagName;
    private String bagType;
    private String bagDescription;
    private InterMineBag bag;
    private Map<Integer, InterMineObject> idToObjectMap;
    private IdUpgrader idUpgrader;
    private int elementsInOldBag;
    private Set<Integer> bagContents;

    /**
     * Create a new InterMineBagHandler object.
     *
     * @param uosw UserProfile ObjectStoreWriter
     * @param osw ObjectStoreWriter used to resolve object ids and write to the objectstore bag
     * @param bags Map from bag name to InterMineIdBag - results are added to this Map
     * @param userId the id of the user
     * @param idUpgrader bag object id upgrader
     * @param idToObjectMap a Map from id to InterMineObject. This is used to create template
     * objects to pass to createPKQuery() so that old bags can be used with new ObjectStores.
     */
    public InterMineBagHandler(ObjectStoreWriter uosw, ObjectStoreWriter osw,
            Map<String, InterMineBag> bags, Integer userId,
            Map<Integer, InterMineObject> idToObjectMap, IdUpgrader idUpgrader) {
        this.uosw = uosw;
        this.osw = osw;
        this.bags = bags;
        this.userId = userId;
        this.idUpgrader = idUpgrader;
        this.idToObjectMap = idToObjectMap;
        this.model = osw.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(@SuppressWarnings("unused") String uri,
            @SuppressWarnings("unused") String localName, String qName,
            Attributes attrs) throws SAXException {
        try {
            if ("bag".equals(qName)) {
                bagContents = new HashSet<Integer>();
                bagName = attrs.getValue("name");
                bagType = attrs.getValue("type");
                bagDescription = attrs.getValue("description");
                Date dateCreated;
                try {
                    dateCreated = new Date(Long.parseLong(attrs.getValue("date-created")));
                } catch (NumberFormatException e) {
                    dateCreated = null;
                }
                // only upgrade bags whose type is still in the model
                String bagClsName = model.getPackageName() + "." + bagType;
                if (model.hasClassDescriptor(bagClsName)) {
                    bag = new InterMineBag(bagName, bagType, bagDescription,
                            dateCreated, osw.getObjectStore(), userId, uosw);
                } else {
                    LOG.warn("Not upgrading bag: " + bagName + " for user: " + userId
                            + " - " + bagType + " no longer in model.");
                }
            }

            if ("bagElement".equals(qName) && bag != null) {
                elementsInOldBag++;
                Integer id = new Integer(attrs.getValue("id"));

                if (idUpgrader.doUpgrade() && idToObjectMap.containsKey(id)) {
                    // try to find an equivalent object in the new database

                    InterMineObject oldObject = idToObjectMap.get(id);

                    Set<Integer> newIds = idUpgrader.getNewIds(oldObject, osw);
                    bagContents.addAll(newIds);
                } else {
                    // we aren't upgrading so just find the object by id
                    if (osw.getObjectById(id) != null) {
                        bagContents.add(id);
                    }
                }
            }
        } catch (ObjectStoreException e) {
            throw new SAXException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(@SuppressWarnings("unused") String uri,
            @SuppressWarnings("unused") String localName,
            String qName) throws SAXException {
        try {
            if ("bag".equals(qName)) {
                if (bag != null && !bagContents.isEmpty()) {
                    osw.addAllToBag(bag.getOsb(), bagContents);
                    bags.put(bagName, bag);
                }
                LOG.debug("XML bag \"" + bagName + "\" contained " + elementsInOldBag
                        + " elements, created bag with " + (bag == null ? "null"
                                : "" + bag.size()) + " elements");
                bag = null;
                elementsInOldBag = 0;
            }
        } catch (ObjectStoreException e) {
            throw new SAXException(e);
        }
    }
}
