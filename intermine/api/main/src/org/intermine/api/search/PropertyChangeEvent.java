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
 * A representation of a notification that a property of a web searchable has changed. The
 * property that has changed could be any one of the required fields in the WebSearchable interface.
 *
 * @author Alex Kalderimis
 *
 */
public class PropertyChangeEvent extends OriginatingEvent
{

    /**
     * Constructor.
     * @param origin The web searchable object whose property has changed.
     */
    public PropertyChangeEvent(WebSearchable origin) {
        super(origin);
    }

}
