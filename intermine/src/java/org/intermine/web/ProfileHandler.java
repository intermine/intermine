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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.web.bag.InterMineBagBinding;
import org.intermine.xml.full.FullHandler;

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
    private Map savedQueries;
    private Map savedBags;
    private Map savedTemplates;
    private List items;

    /**
     * The current child handler.  If we have just seen a "bags" element, it will be an
     * InterMineBagBinding.BagHandler.  If "template-queries" it will be an
     * TemplateQueryBinding.TemplateQueryHandler.  If "queries" it will be a
     * PathQueryBinding.PathQueryHandler.  If subHandler is not null subHandler.startElement() and
     * subHandler.endElement(), etc will be called from this class.
     */
    DefaultHandler subHandler = null;

    /**
     * Create a new ProfileHandler
     * @param profileManager the ProfileManager to pass to the Profile constructor
     */
    public ProfileHandler(ProfileManager profileManager) {
        super();
        this.profileManager = profileManager;
        items = new ArrayList();
    }

    /**
     * Return the de-serialised Profile.
     * @return the new Profile
     */
    public Profile getProfile() {
        return new Profile(profileManager, username, password, savedQueries, savedBags,
                           savedTemplates);
    }

    /**
     * @see DefaultHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("userprofile")) {
            username = attrs.getValue("username");
            password = attrs.getValue("password");
        }
        if (qName.equals("items")) {
            subHandler = new FullHandler();
        }
        if (qName.equals("bags")) {
            savedBags = new LinkedHashMap();
            subHandler = new InterMineBagBinding.BagHandler(profileManager.getObjectStore(),
                                                            savedBags);
        }
        if (qName.equals("template-queries")) {
            savedTemplates = new LinkedHashMap();
            subHandler = new TemplateQueryBinding.TemplateQueryHandler(savedTemplates);
        }
        if (qName.equals("queries")) {
            savedQueries = new LinkedHashMap();
            subHandler = new PathQueryHandler(savedQueries);
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
        }
        if (qName.equals("bags") || qName.equals("template-queries")
            || qName.equals("queries") || qName.equals("items")) {
            subHandler = null;
        }

        if (subHandler != null) {
            subHandler.endElement(uri, localName, qName);
        }
    }
}