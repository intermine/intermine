package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriter;

/**
 * Factory class for creating TagManager objects.
 * 
 * @author Jakub Kulaviak
 *
 */
public class TagManagerFactory 
{

    private static TagManager tagManager;

    /**
     * Constructor.
     * @param profileWriter user profile object store writer
     */
    public TagManagerFactory(ObjectStoreWriter profileWriter) {
        if (tagManager == null) {
            tagManager = new TagManager(profileWriter);
        }
    }
    
    /**
     * 
     * @return tag manager
     */
    public TagManager getTagManager() {
        return tagManager;
    }
}
