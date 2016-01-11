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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.Transformer;
import org.intermine.model.userprofile.Tag;

/**
 * A class that handles transforming a list of tags into a list of strings of different types.
 * @author Alex Kalderimis
 *
 */
public final class TagMapper implements Transformer
{

    /** The valid fields that can be read from a tag **/
    public enum Field {
        /** The tag name **/
        Name,
        /** The identifier of the tagged object. **/
        ID,
        /** The type of this tag **/
        Type,
        /** The owner of the tag **/
        Owner
    }

    private final Field field;

    private TagMapper(Field field) {
        this.field = field;
    }

    /**
     * Get a tag mapper.
     * @param field The kind of field this mapper reads.
     * @return The mapper.
     */
    public static TagMapper getMapper(Field field) {
        if (field == null) {
            throw new NullPointerException("field must be one of Name, ID, Type or Owner.");
        }
        return new TagMapper(field);
    }

    @Override
    public String transform(Object obj) {
        if (obj == null || !(obj instanceof Tag)) {
            throw new IllegalArgumentException("tag must be a Tag, got " + obj);
        }
        return transform((Tag) obj);
    }

    /**
     * Transform a tag into a string of some kind.
     * @param tag The input tag. Must not be null.
     * @return The output string. Might be null.
     */
    public String transform(Tag tag) {
        if (tag == null) {
            throw new NullPointerException("tag must not be null.");
        }
        switch (field) {
            case Name:
                return tag.getTagName();
            case ID:
                return tag.getObjectIdentifier();
            case Type:
                return tag.getType();
            case Owner:
                return tag.getUserProfile().getUsername();
            default:
                throw new IllegalStateException("Unknown field: " + field);
        }
    }

    /**
     * Safely transform a list of tags into a list of string, preserving type information.
     *
     * The lists produced by this method are intended to be read only. If you intend on modifying
     * the list in any way, you must construct a new collection from the result.
     *
     * @param tags The input list of tags. Must not be null.
     * @return The list of strings, never null.
     */
    public List<String> map(List<Tag> tags) {
        if (tags == null) {
            throw new NullPointerException("tags must not be null");
        }
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> retval = new ArrayList<String>(tags.size());
        for (Tag tag: tags) {
            retval.add(transform(tag));
        }
        return retval;
    }

}
