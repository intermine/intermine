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
 * The interface for describing objects that listen to WebSearchables for events.
 *
 * The ChangeEvent interface is implemented by several subclasses, that should all be handled
 * appropriately. For an example implementation see SearchRepository.
 *
 * @author Alex Kalderimis.
 *
 */
public interface WebSearchWatcher
{

    /**
     * Receive notification of a change to a WebSearchable somewhere in the universe. It may or
     * may not be relevant to you. You will have to examine the event and decide what action to
     * take.
     *
     * @param e The event you care about.
     */
    void receiveEvent(ChangeEvent e);
}
