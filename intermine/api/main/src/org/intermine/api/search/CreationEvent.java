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
 * A representation of a notification a web searchable object has been created.
 *
 * @author Alex Kalderimis.
 *
 */
public class CreationEvent extends OriginatingEvent
{

    /**
     * Constructor.
     * @param origin The web searchable object that has sprung into existence.
     */
    public CreationEvent(WebSearchable origin) {
        super(origin);
    }

}
