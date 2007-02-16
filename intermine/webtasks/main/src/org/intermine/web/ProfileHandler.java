package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.web.bag.IdUpgrader;
import org.intermine.web.bag.InterMineBagHandler;
import org.intermine.xml.full.FullHandler;
import org.intermine.xml.full.FullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param classKeys class key fields in model
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader, Map classKeys) {
        this(profileManager, idUpgrader, null, null, new HashSet(), classKeys);
    }

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param idUpgrader the IdUpgrader to use to find objects in the new ObjectStore that
     * correspond to object in old bags.
     * @param defaultUsername default username
     * @param defaultPassword default password
     * @param tags a set to populate with user tags
     * @param classKeys class key fields in model
     */
    public ProfileHandler(ProfileManager profileManager, IdUpgrader idUpgrader,
            String defaultUsername, String defaultPassword, Set tags, Map classKeys) {
        super();
        this.profileManager = profileManager;
        this.idUpgrader = idUpgrader;
        items = new ArrayList();
        this.username = defaultUsername;
        this.password = defaultPassword;
        this.tags = tags;
        this.classKeys = classKeys;
    }

    /**
     * Return the de-serialised Profile.
     * @return the new Profile
     */
    public Profile getProfile() {
        return new Profile(profileManager, username, null, password, savedQueries, savedBags,
                           savedTemplates);
    }

    /**
     * Return a set of Tag objects to add to the Profile.
     * @return the set Tags
     */
    public Set getTags() {
        return tags;
    }

    /**
     * @see DefaultHandler#startElement
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
                                                 profileManager.getObjectStore(), savedBags, null);
        }
        if (qName.equals("template-queries")) {
            savedTemplates = new LinkedHashMap();
            subHandler = new TemplateQueryBinding.TemplateQueryHandler(savedTemplates, savedBags,
                                                                       classKeys);
        }
        if (qName.equals("queries")) {
            savedQueries = new LinkedHashMap();
            subHandler = new SavedQueryBinding.SavedQueryHandler(savedQueries, savedBags,
                                                                 classKeys);
        }
        if (qName.equals("tags")) {
            subHandler = new TagBinding.TagHandler(username, tags);
        }
        if (subHandler != null) {
            subHandler.startElement(uri, localName, qName, attrs);
        }
    }

    /**
     * @see DefaultHandler#endElement
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
                idObjectMap.put(object.getId() + "", object);
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
