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

import org.intermine.dataloader.DataLoaderHelper;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.IntToIntMap;
import org.intermine.util.TypeUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * A handler for turning XML bags data into an InterMineBag.
 *
 * @author Mark Woodbridge
 */
public class InterMineBagHandler extends DefaultHandler
{
    private static final Logger LOG = Logger.getLogger(InterMineBagHandler.class);

    private ObjectStore os;
    private Map bags;
    private String bagName;
    private InterMineBag bag;
    private Map idToObjectMap;
    private Model model;

    /**
     * Create a new InterMineBagHandler object.
     * @param os ObjectStore used to resolve object ids
     * @param bags Map from bag name to InterMineBag - results are added to this Map
     * @param idToObjectMap a Map from id to InterMineObject.  This is used to create template
     * objects to pass to createPKQuery() so that old bags can be used with new ObjectStores.
     */
    public InterMineBagHandler(ObjectStore os, Map bags, Map idToObjectMap) {
        this.os = os;
        this.bags = bags;
        this.model = os.getModel();
        this.idToObjectMap = idToObjectMap;
    }

    /**
     * @see DefaultHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        try {
            if (qName.equals("bag")) {
                bagName = attrs.getValue("name");
            }
            if (qName.equals("element")) {
                String type = attrs.getValue("type");
                String value = attrs.getValue("value");
                if (type.equals(InterMineObject.class.getName())) {
                    if (bag == null) {
                        bag = new InterMineIdBag();
                    } else if (bag instanceof InterMinePrimitiveBag) {
                        LOG.error("InterMineObject id " + value + " in bag of primitives");
                        return;
                    }

                    
                    if (idToObjectMap.containsKey(value)) {
                        Integer oldId = Integer.valueOf(value);
                        InterMineObject oldObject = (InterMineObject) idToObjectMap.get(oldId);

                        bag.addAll(getNewIds(oldObject, os));
                    } else {
                        bag.add(Integer.valueOf(value));
                    }
                } else {
                    if (bag == null) {
                        bag = new InterMinePrimitiveBag();
                    }  else if (bag instanceof InterMineIdBag) {
                        LOG.error("primitive " + value + " in bag of InterMineObjects");
                        return;
                    }
                    bag.add(TypeUtil.stringToObject(Class.forName(type), value));
                }
            }
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    /**
     * @see DefaultHandler#endElement
     */
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("bag")) {
            bags.put(bagName, bag);
            bag = null;
        }
    }

    /**
     * For the given object from an old ObjectStore, find the corresponding InterMineObjects in a
     * new ObjectStore.  Primary keys are used to find the objects.
     * @param oldObject the template object
     * @param os ObjectStore used to resolve objects
     * @return the set of new InterMineObjects
     */
    public static Set getNewIds(InterMineObject oldObject, ObjectStore os) {
        Query query;
        try {
            query = DataLoaderHelper.createPKQuery(os.getModel(), oldObject, null,
                                                   new IntToIntMap());
        } catch (MetaDataException e) {
           throw new RuntimeException("Unable to create query for new object", e);
        }

        SingletonResults results = new SingletonResults(query, os, os.getSequence());
        
        int size = results.size();
        
        if (size != 1) {
            throw new RuntimeException("createPKQuery() query didn't return 1 result for: "
                                       + oldObject.getId() + " (size was " + size + ")");
        } else {
            Set returnSet = new HashSet();

            Iterator iter = results.iterator();

            while (iter.hasNext()) {
                InterMineObject newObject = (InterMineObject) iter.next();

                returnSet.add(newObject.getId());
            }
            return returnSet;
        }
    }
}