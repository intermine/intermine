package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.intermine.api.profile.TagMapper.Field;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.junit.Before;
import org.junit.Test;

public class TagMapperTest {

    private static final int N = 10;
    private List<Tag> tags = null;

    @Before
    public void setup() {
        UserProfile u = new UserProfile();
        u.setUsername("user");
        List<Tag> tags = new ArrayList<Tag>(N);
        for (int i = 0; i < N; i++) {
            Tag t = new Tag();
            t.setTagName("tagName_" + i);
            t.setObjectIdentifier("objectIdentifier_" + i);
            t.setType("type_" + i);
            t.setUserProfile(u);
            tags.add(t);
        }
        this.tags = tags;
    }

    @Test
    public void badField() {
        try {
            TagMapper.getMapper(null);
            fail("Expected failure");
        } catch (NullPointerException e) {
            // expected behaviour.
        }
    }

    @Test
    public void apacheCollectionsIntegration() {
        Transformer tm = TagMapper.getMapper(Field.Name);
        @SuppressWarnings("unchecked")
        Collection<String> names = CollectionUtils.collect(tags, tm);
        List<String> expected = Arrays.asList(
                "tagName_0",
                "tagName_1",
                "tagName_2",
                "tagName_3",
                "tagName_4",
                "tagName_5",
                "tagName_6",
                "tagName_7",
                "tagName_8",
                "tagName_9"
        );
        assertEquals(expected, names);
    }

    @Test
    public void testGetNames() {
        List<String> names = TagMapper.getMapper(Field.Name).map(tags);
        assertEquals(N, names.size());
        List<String> expected = Arrays.asList(
                "tagName_0",
                "tagName_1",
                "tagName_2",
                "tagName_3",
                "tagName_4",
                "tagName_5",
                "tagName_6",
                "tagName_7",
                "tagName_8",
                "tagName_9"
        );
        assertEquals(expected, names);
    }

    @Test
    public void testGetIds() {
        List<String> names = TagMapper.getMapper(Field.ID).map(tags);
        assertEquals(N, names.size());
        List<String> expected = Arrays.asList(
                "objectIdentifier_0",
                "objectIdentifier_1",
                "objectIdentifier_2",
                "objectIdentifier_3",
                "objectIdentifier_4",
                "objectIdentifier_5",
                "objectIdentifier_6",
                "objectIdentifier_7",
                "objectIdentifier_8",
                "objectIdentifier_9"
        );
        assertEquals(expected, names);
    }


    @Test
    public void testGetTypes() {
        List<String> names = TagMapper.getMapper(Field.Type).map(tags);
        assertEquals(N, names.size());
        List<String> expected = Arrays.asList(
                "type_0",
                "type_1",
                "type_2",
                "type_3",
                "type_4",
                "type_5",
                "type_6",
                "type_7",
                "type_8",
                "type_9"
        );
        assertEquals(expected, names);
    }

    @Test
    public void testGetOwner() {
        Set<String> names = new HashSet<String>(TagMapper.getMapper(Field.Owner).map(tags));
        assertEquals(1, names.size());
        assertEquals("user", names.iterator().next());
    }

}
