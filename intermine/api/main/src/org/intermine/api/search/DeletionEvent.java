package org.intermine.api.search;

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
 * A representation of a notification that a websearchable has been deleted.
 * @author ajk59
 *
 */
public class DeletionEvent extends OriginatingEvent
{

    /**
     * Constructor.
     * @param origin The websearchable object that has just been deleted.
     */
    public DeletionEvent(WebSearchable origin) {
        super(origin);
    }
}
