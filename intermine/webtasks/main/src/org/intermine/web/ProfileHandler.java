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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.web.logic.bag.IdUpgrader;
import org.intermine.web.logic.bag.InterMineBagHandler;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.SavedQueryHandler;
import org.intermine.web.logic.tagging.TagHandler;
import org.intermine.web.logic.template.TemplateQueryHandler;
import org.intermine.xml.full.FullHandler;
import org.intermine.xml.full.FullParser;

import javax.servlet.ServletContext;

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
    private Map savedQueries, classKeys;
    private Map savedBags;
    private Map savedTemplates;
    private Set tags;
    private List items;
    private Map idObjectMap;
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
    private final ServletContext servletContext;

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param servletContext global ServletContext object
     * @param osw an ObjectStoreWriter to the production database, to write bags
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader, 
                          ServletContext servletContext, ObjectStoreWriter osw) {
        this(profileManager, idUpgrader, null, null, new HashSet(), servletContext, osw);
    }

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param defaultUsername default username
     * @param defaultPassword default password
     * @param tags a set to populate with user tags
     * @param servletContext global ServletContext object
     * @param osw an ObjectStoreWriter to the production database, to write bags
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader,
                          String defaultUsername, String defaultPassword, Set tags,
                          ServletContext servletContext, ObjectStoreWriter osw) {
        super();
        this.profileManager = profileManager;
        this.idUpgrader = idUpgrader;
        this.servletContext = servletContext;
        items = new ArrayList();
        this.username = defaultUsername;
        this.password = defaultPassword;
        this.tags = tags;
        this.classKeys = classKeys;
        this.osw = osw;
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
    public Set getTags() {
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
            subHandler = new InterMineBagHandler(profileManager.getUserProfileObjectStore(),
                    osw, savedBags, null, idObjectMap, idUpgrader);
        }
        if (qName.equals("template-queries")) {
            savedTemplates = new LinkedHashMap();
            subHandler = new TemplateQueryHandler(savedTemplates, savedBags, servletContext);
        }
        if (qName.equals("queries")) {
            savedQueries = new LinkedHashMap();
            subHandler = new SavedQueryHandler(savedQueries, savedBags, servletContext);
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
            List objects;
            try {
                objects = FullParser.realiseObjects(items, model, true);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("cannot turn items into objects", e);
            }
            Iterator objectIter = objects.iterator();
            while (objectIter.hasNext()) {
                InterMineObject object = (InterMineObject) objectIter.next();
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
