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
 * A representation of a notification that something has happened that originated at a particular
 * web searchable object.
 * @author Alex Kalderimis.
 *
 */
public abstract class OriginatingEvent implements ChangeEvent
{

    protected final WebSearchable origin;

    /**
     * Constructor.
     * @param origin The web searchable involved. Must not be null.
     */
    public OriginatingEvent(WebSearchable origin) {
        if (origin == null) {
            throw new IllegalArgumentException("'origin' cannot be null");
        }
        this.origin = origin;
    }

    /**
     * Get the web searchable involved in this event.
     * @return The object this event is about.
     */
    public WebSearchable getOrigin() {
        return origin;
    }
}
