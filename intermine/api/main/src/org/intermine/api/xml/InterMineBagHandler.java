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
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.InvalidBag;
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
 * @author Alex Kalderimis
 */
public class InterMineBagHandler extends DefaultHandler
{
    private static final Logger LOG = Logger.getLogger(InterMineBagHandler.class);

    private ObjectStoreWriter uosw;
    private ObjectStoreWriter osw;
    private Map<String, InterMineBag> bags;
    private Map<String, InvalidBag> invalidBags;
    private Integer userId;
    private Model model;

    private String bagName;
    private String bagType;
    private String bagDescription;
    private String bagState;
    private InterMineBag bag;
    private InvalidBag invalidBag;
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
     * @param invalidBags Accumulator for the bags of this user that are no longer valid.
     * @param userId the id of the user
     * @param bagsValues a Map from bag name to sets of bag values.
     */
    public InterMineBagHandler(ObjectStoreWriter uosw, ObjectStoreWriter osw,
            Map<String, InterMineBag> bags, Map<String, InvalidBag> invalidBags,
            Map<String, Set<BagValue>> bagsValues, Integer userId) {
        this.uosw = uosw;
        this.osw = osw;
        this.bags = bags;
        this.invalidBags = invalidBags;
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
     * Create a new InterMineBagHandler object.
     *
     * @param uosw UserProfile ObjectStoreWriter
     * @param osw ObjectStoreWriter used to resolve object ids and write to the objectstore bag
     * @param bags Map from bag name to InterMineIdBag - results are added to this Map
     * @param bagsValues a Map from bagName to a set of bag values.
     */
    public InterMineBagHandler(ObjectStoreWriter uosw, ObjectStoreWriter osw,
            Map<String, InterMineBag> bags, Map<String, InvalidBag> invalidBags,
            Map<String, Set<BagValue>> bagsValues) {
        this(uosw, osw, bags, invalidBags, bagsValues, null);
    }

    /**
     * {@inheritDoc}InvalidBag
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
                            uosw, ClassKeyHelper.getKeyFieldNames(classKeys, bagType));
                } else {
                    invalidBag = new InvalidBag(bagName, bagType, bagDescription,
                            dateCreated, osw.getObjectStore(), uosw);
                    LOG.warn(bagName + " for user: " + userId + " is invalid: "
                            + bagType + " is not in the model.");
                }
            }

            if ("bagValue".equals(qName) && bagValues != null) {
                elementsInOldBag++;
                String value = attrs.getValue("value");
                String extra = attrs.getValue("extra");
                bagValues.add(new BagValue(value, extra));
            }
        } catch (Exception e) {
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
                String debugMsg = "";
                if (bag != null && !bagValues.isEmpty()) {
                    bags.put(bagName, bag);
                    bagContents.put(bagName, bagValues);
                    debugMsg = "bag with " + bag.size() + " elements";
                }
                if (invalidBag != null) {
                    invalidBags.put(bagName, invalidBag);
                    bagContents.put(bagName, bagValues);
                    debugMsg = "invalid bag with " + bagValues.size() + " old values";
                }
                System.out.println("XML bag \"" + bagName + "\" contained " + elementsInOldBag
                        + " elements; created " + debugMsg);
                LOG.debug("XML bag \"" + bagName + "\" contained " + elementsInOldBag
                        + " elements; created " + debugMsg);
                bag = null;
                invalidBag = null;
                elementsInOldBag = 0;
            }
        } catch (ObjectStoreException e) {
            throw new SAXException(e);
        }
    }
}
