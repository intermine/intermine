package org.intermine.web.logic.profile;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.userprofile.UserProfile;

/**
 * A class for check the validity of tags.
 * @author kmr
 */
public interface TagChecker
{
    /**
     * Returns true if and only the given arguments are valid fields for a tag of this type.
     * @param tagName the name of the new tag
     * @param type the tag type (eg. "collection", "bag")
     * @param objectIdentifier the String version of the identifier of the object to tag (eg.
     * "Department.name")
     * @param userProfile the UserProfile to associate this tag with
     * @throws RuntimeException if the this parameters are inconsistent
     */
    public void isValid(String tagName, String type, String objectIdentifier,
                        UserProfile userProfile);
}
