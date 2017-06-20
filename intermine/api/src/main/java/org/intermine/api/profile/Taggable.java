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

/**
 * An interface for items that may have tags stored for them in the user profile database.
 * @author Alex Kalderimis
 *
 */
public interface Taggable
{

    /**
     * The name (or identifier) used as the primary key for storing this object.
     * @return the name
     */
    String getName();

    /**
     * Poly-Morphic Constant.
     * @return The appropriate tag-type for this kind of object.
     */
    String getTagType();
}
