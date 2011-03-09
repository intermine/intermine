package org.intermine.api.search;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An interface implemented by objects that we want to index with lucene.
 * @author Kim Rutherford
 */
public interface WebSearchable
{
    /**
     * The name (or identifier) usd as the primary key for storing this object.
     * @return the name
     */
    String getName();

    /**
     * The user-friendly title for this object.
     * @return the title
     */
    String getTitle();

    /**
     * Return the description of this object
     * @return the description
     */
    String getDescription();
}
