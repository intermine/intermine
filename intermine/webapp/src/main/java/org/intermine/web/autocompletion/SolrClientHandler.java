package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import org.intermine.objectstore.ObjectStore;

/**
 * Singleton class to create one instance of Solr Client
 *
 * @author arunans23
 */

public class SolrClientHandler
{
    private static final Logger LOG = Logger.getLogger(SolrClientHandler.class);

    private static SolrClient solrClient;

    private static String solrUrlString;

    private SolrClientHandler(){}

    /**
     *Static method to get the solr client instance
     *
     * @param objectStore ObjectStore instance to pass into Properties Manager
     *
     */
    public static SolrClient getClientInstance(){

        if(solrClient == null){
            synchronized (SolrClientHandler.class){
                if (solrClient == null){
                    solrUrlString = "http://localhost:8983/solr/autocomplete";
                    solrClient = new HttpSolrClient.Builder(solrUrlString).build();
                }

            }
        }
        return solrClient;
    }

}