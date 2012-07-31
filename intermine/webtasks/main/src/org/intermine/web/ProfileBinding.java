package org.intermine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.xml.InterMineBagBinding;
import org.intermine.api.xml.SavedQueryBinding;
import org.intermine.api.xml.TagBinding;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.util.SAXParser;
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

    /**
     * Convert a Profile to XML and write XML to given writer.
     * @param profile the UserProfile
     * @param os the ObjectStore to use when looking up the ids of objects in bags
     * @param writer the XMLStreamWriter to write to
     * @param version the version number of the xml format, an attribute of the profile manager
     * @param classkeys the classKey
     */
    public static void marshal(Profile profile, ObjectStore os, XMLStreamWriter writer,
            int version, Map<String, List<FieldDescriptor>> classkeys) {
        marshal(profile, os, writer, true, true, true, true, true, false, version, classkeys);
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
     * @param classKeys has to be setted if you save bags
     * @param version the version number of the xml format, an attribute of the profile manager
     */
    public static void marshal(Profile profile, ObjectStore os, XMLStreamWriter writer,
            boolean writeUserAndPassword, boolean writeQueries, boolean writeTemplates,
            boolean writeBags, boolean writeTags, boolean onlyConfigTags, int version, Map<String,
            List<FieldDescriptor>> classKeys) {

        try {
            writer.writeCharacters("\n");
            writer.writeStartElement("userprofile");

            if (writeUserAndPassword) {
                writer.writeAttribute("password", profile.getPassword());
                writer.writeAttribute("username", profile.getUsername());
                if (profile.getApiKey() != null) {
                    writer.writeAttribute("apikey", profile.getApiKey());
                }
                writer.writeAttribute("localAccount", String.valueOf(profile.isLocal()));
                writer.writeAttribute("superUser", String.valueOf(profile.isSuperuser()));
            }

            if (writeBags) {
                writer.writeCharacters("\n");
                writer.writeStartElement("bags");
                for (Map.Entry<String, InterMineBag> entry : profile.getSavedBags().entrySet()) {
                    String bagName = entry.getKey();
                    InterMineBag bag = entry.getValue();
                    bag.setKeyFieldNames(ClassKeyHelper.getKeyFieldNames(classKeys,
                                         bag.getQualifiedType()));
                    if (bag != null) {
                        InterMineBagBinding.marshal(bag, writer);
                    } else {
                        LOG.error("bag was null for bagName: " + bagName
                                  + " username: " + profile.getUsername());
                    }
                }
                writer.writeEndElement();
            } else {
                //writer.writeEmptyElement("items");
                writer.writeEmptyElement("bags");
            }

            writer.writeCharacters("\n");
            writer.writeStartElement("queries");
            if (writeQueries) {
                for (SavedQuery query : profile.getSavedQueries().values()) {
                    SavedQueryBinding.marshal(query, writer, version);
                }
            }
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeStartElement("template-queries");
            if (writeTemplates) {
                for (TemplateQuery template : profile.getSavedTemplates().values()) {
                    TemplateQueryBinding.marshal(template, writer, version);
                }
            }
            writer.writeEndElement();
            writer.writeCharacters("\n");
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
            // end <tags>
            writer.writeEndElement();
            // end <userprofile>
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        }
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
            ProfileHandler profileHandler =
                new ProfileHandler(profileManager, username, password, tags, osw, version);
            SAXParser.parse(new InputSource(reader), profileHandler);
            return profileHandler.getProfile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
