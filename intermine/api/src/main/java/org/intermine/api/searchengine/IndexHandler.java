package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.solr.client.solrj.SolrServerException;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for handling indexes.
 *
 * @author arunans23
 */

public interface IndexHandler
{
    /**
     * Main method to create the index
     * Used mostly in post process tasks
     *
     * @param os Objectstore that is passed CreateSearchIndexTask
     * @param classKeys
     *                  classKeys from InterMineAPI, map of classname to all key field
     *                  descriptors
     * @throws IOException IOException is thrown from Objectstore
     * @throws SolrServerException is thrown from solr
     */
    void createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys)
            throws IOException, SolrServerException;

}
