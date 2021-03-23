package org.intermine.api.searchengine.solr;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.intermine.api.searchengine.KeywordSearchPropertiesManager;
import org.intermine.objectstore.ObjectStore;

/**
 * Singleton class to create one instance of Solr Client
 *
 * @author arunans23
 */

public final class SolrClientManager
{
    private static final Logger LOG = Logger.getLogger(SolrClientManager.class);

    private static SolrClient solrClient;

    private static String solrUrlString;

    private SolrClientManager() { }

    /**
     *Static method to get the solr client instance
     *
     * @param objectStore ObjectStore instance to pass into Properties Manager
     * @return solrClient that is created
     *
     */
    public static SolrClient getClientInstance(ObjectStore objectStore) {

        if (solrClient == null) {
            synchronized (SolrClientManager.class) {
                if (solrClient == null) {
                    solrUrlString = KeywordSearchPropertiesManager
                            .getInstance(objectStore).getSolrUrl();
                    solrClient = new HttpSolrClient.Builder(solrUrlString).build();
                }

            }
        }
        return solrClient;
    }

}
