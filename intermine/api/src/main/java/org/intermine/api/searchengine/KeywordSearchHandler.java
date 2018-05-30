package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;

/**
 * Interface for handling indexes.
 *
 * @author arunans23
 */

public class KeywordSearchHandler
{
    /**
     *
     * @param queryString Objectstore that is passed CreateSearchIndexTask
     */
    public void doFilteredSearch(String queryString);

}
