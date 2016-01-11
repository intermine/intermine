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

import org.intermine.api.profile.Taggable;

/**
 * An interface implemented by objects that we want to index with Lucene.
 *
 * @author Kim Rutherford
 * @author Alex Kalderimis
 */
public interface WebSearchable extends Taggable
{
    /**
     * The user-friendly title for this object.
     * @return the title
     */
    String getTitle();

    /**
     * Return the description of this object.
     * @return the description
     */
    String getDescription();

    /**
     * Add this observer to the list of interested parties. The observer should be notified
     * of every change event this web searchable object has cause to issue.
     * @param wsw The observer.
     */
    void addObserver(WebSearchWatcher wsw);

    /**
     * Remove this observer from the list of interested parties. The observer should not be notified
     * of any subsequent events this web searchable object has cause to generate.
     * @param wsw The observer.
     */
    void removeObserver(WebSearchWatcher wsw);

    /**
     * Notify all your observers of this event which originates at this web searchable.
     * @param e The event that has just occurred.
     */
    void fireEvent(OriginatingEvent e);


}
