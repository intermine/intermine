package org.intermine.api.searchengine.solr;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.searchengine.IndexHandler;
import org.intermine.objectstore.ObjectStore;

/**
 * Solr Implementation of IndexHandler
 *
 * @author arunans23
 */
public class SolrIndexHandler implements IndexHandler
{
    /**
     *
     * @param os Objectstore that is passed CreateSearchIndexTask
     */
    @Override
    public void createIndex(ObjectStore os) {

    }
}
