package org.intermine.web;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.web.logic.bag.IdUpgrader;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.InterMineBagHandler;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.query.SavedQueryHandler;
import org.intermine.web.logic.tagging.TagHandler;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateQueryHandler;
import org.intermine.xml.full.FullHandler;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of DefaultHandler to handle parsing Profiles
 *
 * @author Kim Rutherford
 */
class ProfileHandler extends DefaultHandler
{
    private ProfileManager profileManager;
    private String username;
    private String password;
    private Map<String, SavedQuery> savedQueries;
    private Map<String, InterMineBag> savedBags;
    private Map<String, TemplateQuery> savedTemplates;
    private Set tags;
    private List<Item> items;
    private Map<Integer, InterMineObject> idObjectMap;
    private IdUpgrader idUpgrader;
    private ObjectStoreWriter osw;

    /**
     * The current child handler.  If we have just seen a "bags" element, it will be an
     * InterMineBagBinding.InterMineBagHandler.  If "template-queries" it will be an
     * TemplateQueryBinding.TemplateQueryHandler.  If "queries" it will be a
     * PathQueryBinding.PathQueryHandler.  If subHandler is not null subHandler.startElement() and
     * subHandler.endElement(), etc will be called from this class.
     */
    DefaultHandler subHandler = null;
    private boolean abortOnError;

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param osw an ObjectStoreWriter to the production database, to write bags
     * @param abortOnError if true, throw an exception if there is a problem.  If false, log the
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader,
                          ObjectStoreWriter osw, boolean abortOnError) {
        this(profileManager, idUpgrader, null, null, new HashSet(), osw, abortOnError);
    }

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param defaultUsername default username
     * @param defaultPassword default password
     * @param tags a set to populate with user tags
     * @param osw an ObjectStoreWriter to the production database, to write bags
     * @param abortOnError if true, throw an exception if there is a problem.  If false, log the
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader,
                          String defaultUsername, String defaultPassword, Set tags,
                          ObjectStoreWriter osw,
                          boolean abortOnError) {
        super();
        this.profileManager = profileManager;
        this.idUpgrader = idUpgrader;
        items = new ArrayList<Item>();
        this.username = defaultUsername;
        this.password = defaultPassword;
        this.tags = tags;
        this.osw = osw;
        this.abortOnError = abortOnError;
    }

    /**
     * Create a new ProfileHandler.  Throw an exception if there is a problem while reading
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param defaultUsername default username
     * @param defaultPassword default password
     * @param tags a set to populate with user tags
     * @param osw an ObjectStoreWriter to the production database, to write bags
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader,
                          String defaultUsername, String defaultPassword, Set tags,
                          ObjectStoreWriter osw) {
        this(profileManager, idUpgrader, defaultPassword, defaultPassword, tags,
             osw, true);
    }

    /**
     * Return the de-serialised Profile.
     * @return the new Profile
     */
    public Profile getProfile() {
        Profile retval = new Profile(profileManager, username, null, password, savedQueries,
                                     savedBags, savedTemplates);
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
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("userprofile")) {
            if (attrs.getValue("username") != null) {
                username = attrs.getValue("username");
            }
            if (attrs.getValue("password") != null) {
                password = attrs.getValue("password");
            }
        }
        if (qName.equals("items")) {
            subHandler = new FullHandler();
        }
        if (qName.equals("bags")) {
            savedBags = new LinkedHashMap();
            subHandler = new InterMineBagHandler(profileManager.getProfileObjectStoreWriter(),
                    osw, savedBags, null, idObjectMap, idUpgrader);
        }
        if (qName.equals("template-queries")) {
            savedTemplates = new LinkedHashMap();
            subHandler = new TemplateQueryHandler(savedTemplates, savedBags);
        }
        if (qName.equals("queries")) {
            savedQueries = new LinkedHashMap();
            subHandler = new SavedQueryHandler(savedQueries, savedBags);
        }
        if (qName.equals("tags")) {
            subHandler = new TagHandler(username, tags);
        }
        if (subHandler != null) {
            subHandler.startElement(uri, localName, qName, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (qName.equals("items")) {
            items = ((FullHandler) subHandler).getItems();
            idObjectMap = new HashMap();
            Model model = profileManager.getObjectStore().getModel();
            List<InterMineObject> objects;
            try {
                objects = FullParser.realiseObjects(items, model, true, abortOnError);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("unexpected exception", e);
            }

            for (InterMineObject object: objects) {
                idObjectMap.put(object.getId(), object);
            }
        }
        if (qName.equals("bags") || qName.equals("template-queries")
            || qName.equals("queries") || qName.equals("items") || qName.equals("tags")) {
            subHandler = null;
        }

        if (subHandler != null) {
            subHandler.endElement(uri, localName, qName);
        }
    }
}
