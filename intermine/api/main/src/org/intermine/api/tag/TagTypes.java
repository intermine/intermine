package org.intermine.api.tag;

/*
 * Copyright (C) 2002-2012 FlyMine
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
public interface TagTypes
{
    /**
     * Collection.
     */
    String COLLECTION = "collection";

    /**
     * Reference.
     */
    String REFERENCE = "reference";

    /**
     * Template.
     */
    String TEMPLATE = "template";

    /**
     * Bag.
     */
    String BAG = "bag";

    /**
     * A Class/ClassDescriptor.
     */
    String CLASS = "class";
}
