package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2022 FlyMine
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

/**
 * Singleton class to create one instance of Solr Client
 *
 * @author arunans23
 */

final class SolrClientHandler
{
    private static final Logger LOG = Logger.getLogger(SolrClientHandler.class);

    private static SolrClient solrClient;

    private SolrClientHandler() { }

    /**
     *Static method to get the solr client instance
     * @param solrUrlString Url address of solr instance
     *                      eg : "http://localhost:8983/solr/autocomplete"
     * @return solrClient instance
     */
    public static SolrClient getClientInstance(String solrUrlString) {

        if (solrClient == null) {
            synchronized (SolrClientHandler.class) {
                if (solrClient == null) {
                    solrClient = new HttpSolrClient.Builder(solrUrlString).build();
                }

            }
        }
        return solrClient;
    }

}
