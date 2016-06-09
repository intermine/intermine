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
 * A representation of a notification that a tag has been changed on a web-searchable item.
 *
 * @author Alex Kalderimis.
 *
 */
public class TaggingEvent extends OriginatingEvent
{

    /**
     * The possible actions that this event can represent.
     */
    public enum TagChange { ADDED, REMOVED };

    protected final String tagName;
    protected final TagChange action;

    /**
     * Constructor.
     * @param origin The item involved in the event.
     * @param tagName The name of the tag involved.
     * @param action Whethe the tag was added or removed.
     */
    public TaggingEvent(WebSearchable origin, String tagName, TagChange action) {
        super(origin);
        if (tagName == null) {
            throw new IllegalArgumentException("'tagName' cannot be null");
        }
        this.tagName = tagName;
        this.action = action;
    }

    /**
     * @return the name of the tag involved.
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * @return the action this represents.
     */
    public TagChange getAction() {
        return action;
    }
}
