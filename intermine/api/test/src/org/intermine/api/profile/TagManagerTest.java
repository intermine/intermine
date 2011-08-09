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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;

public class TagManagerTest extends InterMineAPITestCase
{

    private Profile bobProfile;
    private ProfileManager pm;
    private TagManager manager;
    public TagManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        pm = im.getProfileManager();
        bobProfile = new Profile(pm, "bob", 101, "bob_pass", new HashMap(), new HashMap(), new HashMap(), true);
        pm.createProfile(bobProfile);
        manager = im.getTagManager();
    }

    public void testDeleteTag() {
        manager.addTag("list1Tag", "list1", "bag", "bob");
        // test that tag was added successfully
        assertEquals(1, manager.getTags("list1Tag", "list1", "bag", "bob").size());
        manager.deleteTag("list1Tag", "list1", "bag", "bob");
        // test that tag was deleted
        assertEquals(0, manager.getTags("list1Tag", "list1", "bag", "bob").size());
    }

    public void testGetTags() throws Exception {
        manager.addTag("list1Tag", "list1", "bag", "bob");
        manager.addTag("list2Tag", "list2", "bag", "bob");
        manager.addTag("list3Tag", "list3", "bag", "bob");

        List<Tag> tags = manager.getTags(null, null, null, "bob");
        assertEquals(3, tags.size());
        assertTrue("Tag added to database but not retrieved.", tagExists(tags, "list1Tag", "list1", "bag", "bob"));
        assertTrue("Tag added to database but not retrieved.", tagExists(tags, "list2Tag", "list2", "bag", "bob"));
        assertTrue("Tag added to database but not retrieved.", tagExists(tags, "list3Tag", "list3", "bag", "bob"));
    }

    public void testAddTag() {
        Tag createdTag = manager.addTag("wowTag", "list1", "bag", "bob");
        Tag retrievedTag = manager.getTags("wowTag", "list1", "bag", "bob").get(0);
        assertEquals(createdTag, retrievedTag);
        assertEquals(createdTag.getTagName(), "wowTag");
        assertEquals(createdTag.getObjectIdentifier(), "list1");
        assertEquals(createdTag.getType(), "bag");
        assertEquals(createdTag.getUserProfile().getUsername(), "bob");
    }

    public void testGetTagById() {
        Tag tag = manager.addTag("list1Tag", "list1", "bag", "bob");
        Tag retrievedTag = manager.getTagById(tag.getId());
        assertEquals(tag, retrievedTag);
    }

    // Verifies that tag name can only contain A-Z, a-z, 0-9, '_', '-', ' ', ':', '.'
    public void testIsValidTagName() {
        assertTrue(TagManager.isValidTagName("validTagName_.- :1"));
        assertFalse(TagManager.isValidTagName("invalidTagName@"));
    }

    private boolean tagExists(List<Tag> tags, String name, String taggedObject, String type,
            String userName) {
        for (Tag tag : tags) {
            if (tag.getTagName().equals(name)
                    && tag.getObjectIdentifier().equals(taggedObject)
                    && tag.getType().equals(type)
                    && tag.getUserProfile().getUsername().equals(userName)) {
                return true;
            }
        }
        return false;
    }
}
