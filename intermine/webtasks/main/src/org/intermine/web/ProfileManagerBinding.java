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

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.SAXParser;
import org.intermine.web.bag.PkQueryIdUpgrader;
import org.intermine.web.logic.bag.IdUpgrader;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Code for reading and writing ProfileManager objects as XML
 *
 * @author Kim Rutherford
 */

public class ProfileManagerBinding
{
    /**
     * Convert the contents of a ProfileManager to XML and write the XML to the given writer.
     * @param profileManager the ProfileManager
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ProfileManager profileManager, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("userprofiles");
            List usernames = profileManager.getProfileUserNames();

            Iterator iter = usernames.iterator();

            while (iter.hasNext()) {
                Profile profile = profileManager.getProfile((String) iter.next());
                ProfileBinding.marshal(profile, profileManager.getObjectStore(), writer);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read a ProfileManager from an XML stream Reader
     * @param reader contains the ProfileManager XML
     * @param profileManager the ProfileManager to store the unmarshalled Profiles to
     * @param osw ObjectStoreWriter used to resolve object ids and write bags
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param classKeys class key fields in model
     */
    public static void unmarshal(Reader reader, ProfileManager profileManager,
            ObjectStoreWriter osw, PkQueryIdUpgrader idUpgrader, Map classKeys) {
        try {
            ProfileManagerHandler profileManagerHandler =
                new ProfileManagerHandler(profileManager, idUpgrader, classKeys, osw);
            SAXParser.parse(new InputSource(reader), profileManagerHandler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}

/**
 * Extension of DefaultHandler to handle parsing ProfileManagers
 * @author Kim Rutherford
 */
class ProfileManagerHandler extends DefaultHandler
{
    private ProfileHandler profileHandler = null;
    private ProfileManager profileManager = null;
    private IdUpgrader idUpgrader;
    private Map classKeys;
    private ObjectStoreWriter osw;
    
    /**
     * Create a new ProfileManagerHandler
     * @param profileManager the ProfileManager to store the unmarshalled Profile to
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param classKeys class key fields in model
     */
    public ProfileManagerHandler(ProfileManager profileManager, IdUpgrader idUpgrader,
            Map classKeys, ObjectStoreWriter osw) {
        super();
        this.profileManager = profileManager;
        this.idUpgrader = idUpgrader;
        this.classKeys = classKeys;
        this.osw = osw;
    }

    /**
     * @see DefaultHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("userprofile")) {
            profileHandler = new ProfileHandler(profileManager, idUpgrader, classKeys, osw);
        }
        if (profileHandler != null) {
            profileHandler.startElement(uri, localName, qName, attrs);
        }
    }

    /**
     * @throws SAXException
     * @see DefaultHandler#endElement
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (qName.equals("userprofile")) {
            Profile profile = profileHandler.getProfile();
            profileManager.createProfile(profile);
            Iterator tagIter = profileHandler.getTags().iterator();
            while (tagIter.hasNext()) {
                Tag tag = (Tag) tagIter.next();
                profileManager.addTag(tag.getTagName(), tag.getObjectIdentifier(), tag.getType(),
                                      profile.getUsername());

            }
            profileHandler = null;
        }
        if (profileHandler != null) {
            profileHandler.endElement(uri, localName, qName);
        }
    }
}
