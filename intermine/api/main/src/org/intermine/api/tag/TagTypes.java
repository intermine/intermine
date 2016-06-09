package org.intermine.api.tag;

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
 * Known tag types.
 * @author Thomas Riley
 */
public final class TagTypes
{
    /**
     * Collection.
     */
    public static final String COLLECTION = "collection";

    /**
     * Reference.
     */
    public static final String REFERENCE = "reference";

    /**
     * Template.
     */
    public static final String TEMPLATE = "template";

    /**
     * Bag.
     */
    public static final String BAG = "bag";

    /**
     * A Class/ClassDescriptor.
     */
    public static final String CLASS = "class";

    private TagTypes() {
        // just don't
    }
}
