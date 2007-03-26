package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.TypeUtil;
import org.intermine.web.bag.BagElement;
import org.intermine.web.bag.IdUpgrader;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineBagBinding;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

import java.io.Reader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

/**
 * Code for reading and writing Profile objects as XML.
 *
 * @author Kim Rutherford
 */

public class ProfileBinding
{
    private static final Logger LOG = Logger.getLogger(ProfileBinding.class);

    /**
     * Convert a Profile to XML and write XML to given writer.
     * @param profile the UserProfile
     * @param os the ObjectStore to use when looking up the ids of objects in bags
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(Profile profile, ObjectStore os,
                               XMLStreamWriter writer) {
        marshal(profile, os, writer, true, true, true, true, true, false);
    }

    /**
     * Convert a Profile to XML and write XML to given writer.
     * @param profile the UserProfile
     * @param os the ObjectStore to use when looking up the ids of objects in bags
     * @param writer the XMLStreamWriter to write to
     * @param writeUserAndPassword write username and password
     * @param writeQueries save saved queries
     * @param writeTemplates write saved templates
     * @param writeBags write saved bags
     * @param writeTags write saved tags
     * @param onlyConfigTags if true, only save tags that contain a ':'
     */
    public static void marshal(Profile profile, ObjectStore os,
                               XMLStreamWriter writer,
                               boolean writeUserAndPassword,
                               boolean writeQueries,
                               boolean writeTemplates,
                               boolean writeBags,
                               boolean writeTags,
                               boolean onlyConfigTags) {
        try {
            writer.writeStartElement("userprofile");

            if (writeUserAndPassword) {
                writer.writeAttribute("password", profile.getPassword());
                writer.writeAttribute("username", profile.getUsername());
            }

            if (writeBags) {
                ItemFactory itemFactory = new ItemFactory(os.getModel());

                Set idSet = new HashSet();

                getProfileObjectIds(profile, os, idSet);

                if (!idSet.isEmpty()) {
                    List objects = os.getObjectsByIds(idSet);

                    writer.writeStartElement("items");
                    Iterator objectsIter = objects.iterator();

                    while (objectsIter.hasNext()) {
                        ResultsRow rr = (ResultsRow) objectsIter.next();
                        InterMineObject o = (InterMineObject) rr.get(0);
                        Item item = itemFactory.makeItem(o);
                        FullRenderer.renderImpl(writer, item, false);
                    }

                    writer.writeEndElement();
                }

                writer.writeStartElement("bags");
                for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    String bagName = (String) entry.getKey();
                    InterMineBag bag = (InterMineBag) entry.getValue();

                    if (bag != null) {
                        InterMineBagBinding.marshal(bag, bagName, writer);
                    } else {
                        LOG.error("bag was null for bagName: " + bagName
                                  + " username: " + profile.getUsername());
                    }
                }
                writer.writeEndElement();
            } else {
                writer.writeEmptyElement("items");
                writer.writeEmptyElement("bags");
            }

            writer.writeStartElement("queries");
            if (writeQueries) {
                for (Iterator i = profile.getSavedQueries().entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    SavedQuery query = (SavedQuery) entry.getValue();

                    SavedQueryBinding.marshal(query, writer);
                }
            }
            writer.writeEndElement();

            writer.writeStartElement("template-queries");
            if (writeTemplates) {
                for (Iterator i = profile.getSavedTemplates().entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    TemplateQuery template = (TemplateQuery) entry.getValue();

                    TemplateQueryBinding.marshal(template, writer);
                }
            }
            writer.writeEndElement();

            writer.writeStartElement("tags");
            if (writeTags) {
                List tags = profile.getProfileManager().getTags(null, null, null,
                                                                profile.getUsername());
                for (Iterator i = tags.iterator(); i.hasNext();) {
                    Tag tag = (Tag) i.next();
                    if (!onlyConfigTags || tag.getTagName().indexOf(":") >= 0) {
                        TagBinding.marshal(tag, writer);
                    }
                }
            }
            writer.writeEndElement();

            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        }
    }

    /**
     * Get the ids of objects in all bags and all objects mentioned in primary keys of those
     * items (recursively).
     * @param profile read the object in the bags from this Profile
     * @param os the ObjectStore to use when following references
     * @param idsToSerialise object ids are added to this Set
     */
    private static void getProfileObjectIds(Profile profile, ObjectStore os, Set idsToSerialise) {
        for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            InterMineBag bag = (InterMineBag) entry.getValue();

            Iterator iter = bag.iterator();

            while (iter.hasNext()) {
                BagElement bagElement = (BagElement) iter.next();
                InterMineObject object;
                Integer id = bagElement.getId();
                try {
                    object = os.getObjectById(id);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("Unable to find object for id: " + id, e);
                }
                if (object == null) {
                    throw new RuntimeException("Unable to find object for id: " + id);
                }
                getIdsFromObject(object, os.getModel(), idsToSerialise);
            }
        }
    }

    /**
     * For the given object, add its ID and all IDs of all objects in any of its primary keys to
     * idsToSerialise.
     * @param object the InterMineObject to add
     * @param model the Model to use for looking up ClassDescriptors
     * @param idsToSerialise object ids are added to this Set
     */
    protected static void getIdsFromObject(InterMineObject object, Model model,
                                           Set idsToSerialise) {
        idsToSerialise.add(object.getId());
        Set cds = model.getClassDescriptorsForClass(object.getClass());
        Iterator cdIter = cds.iterator();

        while (cdIter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) cdIter.next();
            Map primaryKeyMap = PrimaryKeyUtil.getPrimaryKeys(cd);
            Iterator primaryKeyIter = primaryKeyMap.values().iterator();

            while (primaryKeyIter.hasNext()) {
                PrimaryKey pk = (PrimaryKey) primaryKeyIter.next();
                Set fieldNames = pk.getFieldNames();
                Iterator fieldNameIter = fieldNames.iterator();

                while (fieldNameIter.hasNext()) {
                    String fieldName = (String) fieldNameIter.next();
                    FieldDescriptor fd = cd.getFieldDescriptorByName(fieldName);

                    if (fd instanceof ReferenceDescriptor) {
                        InterMineObject referencedObject;
                        try {
                            referencedObject =
                                (InterMineObject) TypeUtil.getFieldValue(object, fieldName);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Unable to access field " + fieldName
                                                       + " in object: " + object);
                        }

                        if (referencedObject != null) {
                            // recurse
                            getIdsFromObject(referencedObject, model, idsToSerialise);
                        }
                    }
                }
            }
        }
    }

    /**
     * Read a Profile from an XML stream Reader.  Note that Tags from the XML are stored immediately
     * using the ProfileManager.
     * @param reader contains the Profile XML
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param os ObjectStore used to resolve object ids
     * @param username default username - used if there is no username in the XML
     * @param password default password
     * @param tags a set to populate with user tags
     * @param classKeys class key fields in model
     * @return the new Profile
     */
    public static Profile unmarshal(Reader reader, ProfileManager profileManager, ObjectStore os,
                                    String username, String password, Set tags, Map classKeys) {
        try {
            IdUpgrader idUpgrader = new IdUpgrader() {
                public Set getNewIds(InterMineObject oldObject, ObjectStore os) {
                    throw new RuntimeException("Shouldn't call getNewIds() in a"
                                               + " running webapp");
                }
            };
            ProfileHandler profileHandler =
                new ProfileHandler(profileManager, idUpgrader, username, password, tags, classKeys);
            SAXParser.parse(new InputSource(reader), profileHandler);
            return profileHandler.getProfile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
