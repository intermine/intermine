package org.intermine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.intermine.api.bag.IdUpgrader;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.InterMineBagBinding;
import org.intermine.api.xml.SavedQueryBinding;
import org.intermine.api.xml.TagBinding;
import org.intermine.api.xml.TemplateQueryBinding;
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
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.xml.sax.InputSource;

/**
 * Code for reading and writing Profile objects as XML.
 *
 * @author Kim Rutherford
 * @author Richard Smith
 */
public final class ProfileBinding
{
    private ProfileBinding() {
    }

    private static final Logger LOG = Logger.getLogger(ProfileBinding.class);
    private static HashMap<Class<?>, Set<FieldDescriptor>> primaryKeyFieldsCache =
        new HashMap<Class<?>, Set<FieldDescriptor>>();

    /**
     * Convert a Profile to XML and write XML to given writer.
     * @param profile the UserProfile
     * @param os the ObjectStore to use when looking up the ids of objects in bags
     * @param writer the XMLStreamWriter to write to
     * @param version the version number of the xml format, an attribute of the profile manager
     */
    public static void marshal(Profile profile, ObjectStore os, XMLStreamWriter writer,
            int version) {
        marshal(profile, os, writer, true, true, true, true, true, false, version);
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
     * @param version the version number of the xml format, an attribute of the profile manager
     */
    public static void marshal(Profile profile, ObjectStore os, XMLStreamWriter writer,
            boolean writeUserAndPassword, boolean writeQueries, boolean writeTemplates,
            boolean writeBags, boolean writeTags, boolean onlyConfigTags, int version) {

        try {
            writer.writeStartElement("userprofile");

            if (writeUserAndPassword) {
                writer.writeAttribute("password", profile.getPassword());
                writer.writeAttribute("username", profile.getUsername());
            }

            if (writeBags) {
                writeItemsForBagIds(os, profile, writer);

                writer.writeStartElement("bags");
                for (Map.Entry<String, InterMineBag> entry : profile.getSavedBags().entrySet()) {
                    String bagName = entry.getKey();
                    InterMineBag bag = entry.getValue();
                    if (bag != null) {
                        InterMineBagBinding.marshal(bag, writer);
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
                for (SavedQuery query : profile.getSavedQueries().values()) {
                    SavedQueryBinding.marshal(query, writer, version);
                }
            }
            writer.writeEndElement();

            writer.writeStartElement("template-queries");
            if (writeTemplates) {
                for (TemplateQuery template : profile.getSavedTemplates().values()) {
                    TemplateQueryBinding.marshal(template, writer, version);
                }
            }
            writer.writeEndElement();

            writer.writeStartElement("tags");
            TagManager tagManager =
                new TagManagerFactory(profile.getProfileManager()).getTagManager();
            if (writeTags) {
                List<Tag> tags = tagManager.getUserTags(profile.getUsername());
                for (Tag tag : tags) {
                    if (!onlyConfigTags || tag.getTagName().indexOf(":") >= 0) {
                        TagBinding.marshal(tag, writer);
                    }
                }
            }
            writer.writeEndElement();  // end <tags>

            writer.writeEndElement();  // end <userprofile>
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        }
    }


    private static void writeItemsForBagIds(ObjectStore os, Profile profile,
            XMLStreamWriter writer) throws ObjectStoreException, XMLStreamException {
        Set<Integer> idsOfAllBagElements = getProfileObjectIds(profile, os);

        if (!idsOfAllBagElements.isEmpty()) {
            List<InterMineObject> objectsToWrite = os.getObjectsByIds(idsOfAllBagElements);

            writer.writeStartElement("items");

            for (InterMineObject objToWrite : objectsToWrite) {
                writeItemPrimaryKeyFields(os, objToWrite, writer);
            }
            writer.writeEndElement();
        }
    }

    // we actually need to write out the primary key fields, these are the only needed for upgrade
    private static void writeItemPrimaryKeyFields(ObjectStore os, InterMineObject objToWrite,
            XMLStreamWriter writer) {
        Model model = os.getModel();
        ItemFactory itemFactory = new ItemFactory(model);
        Set<String> fieldsToWrite = getPrimaryKeyFieldnamesForClass(model, objToWrite.getClass());
        Item item = itemFactory.makeItemImpl(objToWrite, fieldsToWrite);
        FullRenderer.renderImpl(writer, item);
    }

    /**
     * Get the ids of objects in all bags and all objects mentioned in primary keys of those
     * items.
     * @param profile read the object in the bags from this Profile
     * @param os the ObjectStore to use when following references
     */
    private static Set<Integer> getProfileObjectIds(Profile profile, ObjectStore os) {
        Set<Integer> idsToSerialise = new HashSet<Integer>();
        List<Integer> idsToPreFetch = new ArrayList<Integer>();
        try {

            for (InterMineBag bag : profile.getSavedBags().values()) {
                idsToPreFetch.addAll(bag.getContentsAsIds());
            }

            // pre-fetch objects from all bags into the cache
            os.getObjectsByIds(idsToPreFetch);

            for (InterMineBag bag : profile.getSavedBags().values()) {
                for (Integer id : bag.getContentsAsIds()) {
                    InterMineObject object;
                    try {
                        object = os.getObjectById(id);
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException("Unable to find object for id: " + id, e);
                    }
                    if (object == null) {
                        LOG.error("Unable to find object for id: " + id
                                                   + " profile: " + profile.getUsername()
                                                   + " bag: " + bag.getName());
                    } else {
                        getIdsFromObject(object, os.getModel(), idsToSerialise);
                    }
                }
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to find object for ids: " + idsToPreFetch, e);
        }
        return idsToSerialise;
    }

    /**
     * For the given object, add its ID and all IDs of all objects in any of its primary keys to
     * idsToSerialise.
     * @param object the InterMineObject to add
     * @param model the Model to use for looking up ClassDescriptors
     * @param idsToSerialise object ids are added to this Set
     */
    protected static void getIdsFromObject(InterMineObject object, Model model,
                                           Set<Integer> idsToSerialise) {
        idsToSerialise.add(object.getId());
        for (FieldDescriptor fd
                : getPrimaryKeyFieldDescriptorsForClass(model, object.getClass())) {
            if (fd instanceof ReferenceDescriptor) {
                String fieldName = fd.getName();
                InterMineObject referencedObject;
                try {
                    referencedObject = (InterMineObject) object.getFieldValue(fieldName);
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


    private static Set<FieldDescriptor> getPrimaryKeyFieldDescriptorsForClass(Model model,
            Class<?> lookupClass) {
        Set<FieldDescriptor> primaryKeyFields = primaryKeyFieldsCache.get(lookupClass);
        if (primaryKeyFields == null) {
            primaryKeyFields = new HashSet<FieldDescriptor>();
            for (ClassDescriptor cld : model.getClassDescriptorsForClass(lookupClass)) {
                for (PrimaryKey pk : PrimaryKeyUtil.getPrimaryKeys(cld).values()) {
                    for (String fieldName : pk.getFieldNames()) {
                        FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
                        primaryKeyFields.add(fd);
                    }
                }
            }
            primaryKeyFieldsCache.put(lookupClass, primaryKeyFields);
        }
        return primaryKeyFields;
    }

    private static Set<String> getPrimaryKeyFieldnamesForClass(Model model, Class<?> lookupClass) {
        Set<String> primaryKeyFieldNames = new HashSet<String>();
        for (FieldDescriptor fd : getPrimaryKeyFieldDescriptorsForClass(model, lookupClass)) {
            primaryKeyFieldNames.add(fd.getName());
        }
        return primaryKeyFieldNames;
    }

    /**
     * Read a Profile from an XML stream Reader.  Note that Tags from the XML are stored immediately
     * using the ProfileManager.
     * @param reader contains the Profile XML
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param username default username - used if there is no username in the XML
     * @param password default password
     * @param tags a set to populate with user tags
     * @param osw an ObjectStoreWriter for the production database, to write bags
     * @param version the version of the XML format, an attribute on the ProfileManager
     * @return the new Profile
     */
    public static Profile unmarshal(Reader reader, ProfileManager profileManager, String username,
            String password, Set<Tag> tags, ObjectStoreWriter osw, int version) {
        try {
            IdUpgrader idUpgrader = IdUpgrader.ERROR_UPGRADER;
            ProfileHandler profileHandler =
                new ProfileHandler(profileManager, idUpgrader, username, password, tags, osw, false,
                        version);
            SAXParser.parse(new InputSource(reader), profileHandler);
            return profileHandler.getProfile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
