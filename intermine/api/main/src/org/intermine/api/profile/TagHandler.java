package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.api.tag.TagNames;
import org.intermine.model.userprofile.Tag;
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
    @SuppressWarnings("rawtypes")
    private Set tags;
    private int count;

    /**
     * Constructor
     * @param userName the name of the user whose profile is being read
     * @param tags will be populated with any tags to add to the target profile
     */
    public TagHandler(String userName, @SuppressWarnings("rawtypes") Set tags) {
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
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("tag".equals(qName)) {
            tagName = attrs.getValue("name");
            tagName = translateTagName(tagName);
            tagObjectIdentifier = attrs.getValue("objectIdentifier");
            tagType = attrs.getValue("type");
        }
        super.startElement(uri, localName, qName, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
        super.endElement(uri, localName, qName);
        if ("tag".equals(qName)) {
            Tag tag = new Tag();
            tag.setTagName(tagName);
            tag.setObjectIdentifier(tagObjectIdentifier);
            tag.setType(tagType);

            // either put tags in set to be added to unmarshalled profile or, when
            // called from ImportTagAction save the tags straight away.
            if (tags != null) {
                tags.add(tag);
            } else {
                TagManager tagManager = new TagManagerFactory(profileManager).getTagManager();
                if (tagManager.getTags(tagName, tagObjectIdentifier,
                                           tagType, userName).isEmpty()) {
                    tagManager.addTag(tagName, tagObjectIdentifier, tagType, userName);
                    count++;
                }
            }
            reset();
        }
    }

    /**
     * Translates specific old tag names used for internal InterMine usage like favorite,
     * aspect: ... to new tag names that must start with 'im:'
     * Calling this method should be removed after some time, when old profiles with old
     * tag names won't be loaded.
     * @param oldName old name
     * @return name with prefix 'im'
     */
    private String translateTagName(String oldName) {
        if ("favourite".equalsIgnoreCase(oldName)) {
            return TagNames.IM_FAVOURITE;
        }
        if ("hidden".equalsIgnoreCase(oldName)) {
            return TagNames.IM_HIDDEN;
        }
        if (oldName.toLowerCase().startsWith("aspect:")) {
            return TagNames.IM_ASPECT_PREFIX + oldName.substring("aspect:".length());
        }
        if ("placement:summary".equalsIgnoreCase(oldName)) {
            return TagNames.IM_SUMMARY;
        }
        return oldName;
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
