package org.intermine.web.logic.tagging;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.profile.ProfileManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of PathQueryHandler to handle parsing TemplateQueries
 * @author Xavier Watkins
 */
public class TagHandler extends DefaultHandler
{
    private String tagName;
    private String tagObjectIdentifier;
    private String tagType;
    private ProfileManager profileManager;
    private String userName;
    private Set tags;
    private int count;

    /**
     * Constructor
     * @param userName the name of the user whose profile is being read
     * @param tags will be populated with any tags to add to the target profile
     */
    public TagHandler(String userName, Set tags) {
        this.userName = userName;
        this.tags = tags;
        reset();
    }

    /**
     * Constructor
     * @param profileManager add each Tag using this ProfileManager
     * @param userName the name of the user whose profile is being read
     */
    public TagHandler(ProfileManager profileManager, String userName) {
        this.profileManager = profileManager;
        this.userName = userName;
        reset();
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("tag")) {
            tagName = attrs.getValue("name");
            tagObjectIdentifier = attrs.getValue("objectIdentifier");
            tagType = attrs.getValue("type");
        }
        super.startElement(uri, localName, qName, attrs);
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
        super.endElement(uri, localName, qName);
        if (qName.equals("tag")) {
            Tag tag = new Tag();
            tag.setTagName(tagName);
            tag.setObjectIdentifier(tagObjectIdentifier);
            tag.setType(tagType);

            // either put tags in set to be added to unmarshalled profile or, when
            // called from ImportTagAction save the tags straight away.
            if (tags != null) {
                tags.add(tag);
            } else {
                if (profileManager.getTags(tagName, tagObjectIdentifier,
                                           tagType, userName).isEmpty()) {
                    profileManager.addTag(tagName, tagObjectIdentifier, tagType, userName);
                    count++;
                }
            }
            reset();
        }
    }

    private void reset() {
        tagName = "";
        tagObjectIdentifier = "";
        tagType = "";
    }
    
    /**
     * Return a count of the number of tags read.
     * @return the number of tags read
     */
    public int getCount() {
        return count;
    }
}
