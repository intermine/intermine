package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.bag.InvitationHandler;
import org.intermine.api.profile.BagSet;
import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.InvalidBag;
import org.intermine.api.profile.PreferencesHandler;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.TagHandler;
import org.intermine.api.template.TemplateHelper;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of DefaultHandler to handle parsing Profiles
 *
 * @author Kim Rutherford
 * @author dbutano
 */
class ProfileHandler extends DefaultHandler
{
    private ProfileManager profileManager;
    private String username;
    private String password;
    private Map<String, SavedQuery> savedQueries;
    private Map<String, InterMineBag> savedBags;
    private Map<String, List> sharedBagsByUser;
    private List<Map<String, String>> sharedBags;
    private Map<String, InvalidBag> invalidBags;
    private Map<String, TemplateQuery> savedTemplates = new HashMap<String, TemplateQuery>();
    private Set<Tag> tags;
    private Map<String, Set<BagValue>> bagsValues;
    private final Map<String, String> preferences = new HashMap<String, String>();
    private ObjectStoreWriter osw;
    private int version;
    private String apiKey = null;
    private boolean isLocal = true;
    private boolean isSuperUser;
    private final InvitationHandler invitationHandler = new InvitationHandler();

    /**
     * Constructor.
     *
     * @return invitation handler
     */
    public InvitationHandler getInvitationHandler() {
        return invitationHandler;
    }

    /**
     * The current child handler.  If we have just seen a "bags" element, it will be an
     * InterMineBagBinding.InterMineBagHandler.  If "template-queries" it will be an
     * TemplateQueryBinding.TemplateQueryHandler.  If "queries" it will be a
     * PathQueryBinding.PathQueryHandler.  If subHandler is not null subHandler.startElement() and
     * subHandler.endElement(), etc will be called from this class.
     */
    DefaultHandler subHandler = null;

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param osw an ObjectStoreWriter to the production database, to write bags
     * @param version the version of the profile xml, an attribute on the profile manager xml
     * @param sharedBagsByUser list of bags shared by user
     */
    public ProfileHandler(ProfileManager profileManager, ObjectStoreWriter osw,
                          int version, Map<String, List> sharedBagsByUser) {
        this(profileManager, null, null, new HashSet(), osw, version, sharedBagsByUser);
    }

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param defaultUsername default username
     * @param defaultPassword default password
     * @param tags a set to populate with user tags
     * @param osw an ObjectStoreWriter to the production database, to write bags
     * @param version the version of the profile xml, an attribute on the profile manager xml
     * @param sharedBagsByUser list of bags shared by user
     */
    public ProfileHandler(ProfileManager profileManager, String defaultUsername,
            String defaultPassword, Set<Tag> tags, ObjectStoreWriter osw, int version,
            Map<String, List> sharedBagsByUser) {
        super();
        this.profileManager = profileManager;
        this.username = defaultUsername;
        this.password = defaultPassword;
        this.tags = tags;
        this.osw = osw;
        this.version = version;
        this.sharedBagsByUser = sharedBagsByUser;
    }

    /**
     * Return the de-serialised Profile.
     * @return the new Profile
     * @throws SAXException if error reading XML
     */
    public Profile getProfile() throws SAXException {
        Profile retval = new Profile(profileManager, username, null, password, savedQueries,
                                     new BagSet(savedBags, invalidBags),
                                     TemplateHelper.upcast(savedTemplates), apiKey,
                                     isLocal, isSuperUser);
        try {
            retval.getPreferences().putAll(preferences);
        } catch (RuntimeException e) {
            throw new SAXException(e);
        }
        return retval;
    }

    /**
     * Return a set of Tag objects to add to the Profile.
     * @return the set Tags
     */
    public Set<Tag> getTags() {
        return tags;
    }

    /**
     * Return a map of bag values for each bag.
     * @return the map of bag values
     */
    public Map<String, Set<BagValue>> getBagsValues() {
        return bagsValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("userprofile".equals(qName)) {
            if (attrs.getValue("username") != null) {
                username = attrs.getValue("username");
            }
            if (attrs.getValue("password") != null) {
                password = attrs.getValue("password");
            }
            if (attrs.getValue("apikey") != null) {
                apiKey = attrs.getValue("apikey");
            }
            if (attrs.getValue("localAccount") != null) {
                isLocal = Boolean.parseBoolean(attrs.getValue("localAccount"));
            }
            if (attrs.getValue("superUser") != null) {
                isSuperUser = Boolean.parseBoolean(attrs.getValue("superUser"));
            }
        }
        if ("bags".equals(qName)) {
            savedBags = new LinkedHashMap();
            invalidBags = new LinkedHashMap();
            bagsValues = new LinkedHashMap();
            subHandler = new InterMineBagHandler(
                    profileManager.getProfileObjectStoreWriter(), osw,
                    savedBags, invalidBags, bagsValues);
        }
        if ("shared-bags".equals(qName)) {
            sharedBags = new ArrayList<Map<String, String>>();
            subHandler = new SharedBagHandler(sharedBags);
        }
        if ("template-queries".equals(qName)) {
            savedTemplates = new LinkedHashMap();
            subHandler = new TemplateQueryHandler(savedTemplates, version);
        }
        if ("queries".equals(qName)) {
            savedQueries = new LinkedHashMap();
            subHandler = new SavedQueryHandler(savedQueries, savedBags, version);
        }
        if ("tags".equals(qName)) {
            subHandler = new TagHandler(username, tags);
        }
        if ("preferences".equals(qName)) {
            subHandler = new PreferencesHandler(preferences);
        }
        if ("invitations".equals(qName)) {
            subHandler = invitationHandler;
        }
        if (subHandler != null) {
            subHandler.startElement(uri, localName, qName, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if ("bags".equals(qName) || ("template-queries").equals(qName)
            || "queries".equals(qName) ||  "items".equals(qName) || "tags".equals(qName)
            || "preferences".equals(qName) || "invitations".equals(qName)) {
            subHandler = null;
        }
        if ("shared-bags".equals(qName)) {
            if (sharedBagsByUser  != null) {
                sharedBagsByUser.put(username, sharedBags);
            }
        }
        if (subHandler != null) {
            subHandler.endElement(uri, localName, qName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (subHandler != null) {
            subHandler.characters(ch, start, length);
        }
    }
}
