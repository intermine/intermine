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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag.BagValue;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
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
    private String bagState;
    private InterMineBag bag;
    private int elementsInOldBag;
    private Set<BagValue> bagValues;
    private Map<String, Set<BagValue>> bagContents;
    private Map<String, List<FieldDescriptor>>  classKeys;

    /**
     * Create a new InterMineBagHandler object.
     *
     * @param uosw UserProfile ObjectStoreWriter
     * @param osw ObjectStoreWriter used to resolve object ids and write to the objectstore bag
     * @param bags Map from bag name to InterMineIdBag - results are added to this Map
     * @param userId the id of the user
     * @param idToObjectMap a Map from id to InterMineObject. This is used to create template
     * objects to pass to createPKQuery() so that old bags can be used with new ObjectStores.
     */
    public InterMineBagHandler(ObjectStoreWriter uosw, ObjectStoreWriter osw,
            Map<String, InterMineBag> bags, Map<String, Set<BagValue>> bagsValues, Integer userId) {
        this.uosw = uosw;
        this.osw = osw;
        this.bags = bags;
        this.bagContents = bagsValues;
        this.userId = userId;
        this.model = osw.getModel();
        Properties classKeyProps = new Properties();
        try {
            InputStream inputStream = this.getClass().getClassLoader()
                                      .getResourceAsStream("class_keys.properties");
            classKeyProps.load(inputStream);
        } catch (IOException ioe) {
            new BuildException("class_keys.properties not found", ioe);
        }
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
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
                bagValues = new HashSet<BagValue>();
                bagName = attrs.getValue("name");
                bagType = attrs.getValue("type");
                bagDescription = attrs.getValue("description");
                bagState = attrs.getValue("status");
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
                            dateCreated, BagState.NOT_CURRENT, osw.getObjectStore(),
                            userId, uosw, ClassKeyHelper.getKeyFieldNames(classKeys, bagType));
                } else {
                    LOG.warn("Not upgrading bag: " + bagName + " for user: " + userId
                            + " - " + bagType + " no longer in model.");
                }
            }

            if ("bagValue".equals(qName) && bag != null) {
                elementsInOldBag++;
                String value = attrs.getValue("value");
                String extra = attrs.getValue("extra");
                bagValues.add(bag.new BagValue(value, extra));
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
                if (bag != null && !bagValues.isEmpty()) {
                    bags.put(bagName, bag);
                    bagContents.put(bagName, bagValues);
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
