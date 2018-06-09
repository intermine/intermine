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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.intermine.objectstore.ObjectStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Factory class to create one instance of Solr Client
 *
 * @author arunans23
 */

public class SolrClientFactory
{
    private static final Logger LOG = Logger.getLogger(SolrClientFactory.class);

    private static SolrClient solrClient;

    private static String solrUrlString;

    private SolrClientFactory(){}

    /**
     *Static method to get the solr client instance
     *
     */
    public static SolrClient getClientInstance(ObjectStore objectStore){

        if(solrClient == null){

            solrUrlString = KeywordSearchPropertiesManager.getInstance(objectStore).getSolrUrl();

            solrClient = new HttpSolrClient.Builder(solrUrlString).build();
        }
        return solrClient;
    }

}
