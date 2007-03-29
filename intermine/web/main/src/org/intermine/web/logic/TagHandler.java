package org.intermine.web.logic;

import java.util.Set;

import org.intermine.model.userprofile.Tag;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of PathQueryHandler to handle parsing TemplateQueries
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
     * @see DefaultHandler#startElement
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
     * @see DefaultHandler#endElement
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
    
    public int getCount() {
        return count;
    }
}