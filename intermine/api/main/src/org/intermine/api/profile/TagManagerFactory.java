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

    private static ObjectStoreWriter profileOsWriter;

    /**
     * Constructor.
     * @param profileWriter user profile object store writer
     */
    public TagManagerFactory(ObjectStoreWriter profileWriter) {
        init(profileWriter);
    }

    private void init(ObjectStoreWriter profileWriter) {
        // if there is different profileWriter than used before use this one
        // else use cached tag manager
        if (profileWriter != profileOsWriter) {
            profileOsWriter = profileWriter;
            tagManager = new TagManager(profileWriter);
        }
    }

    /**
     * Constructor.
     * @param manager profile manager
     */
    public TagManagerFactory(ProfileManager manager) {
        if (manager != null) { // The anonymous profile has no profile manager.
            init(manager.getProfileObjectStoreWriter());
        }
    }

    /**
     * @return tag manager
     */
    public TagManager getTagManager() {
        return tagManager;
    }
}
