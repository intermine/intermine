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
    public static SolrClient getInstance(){

        if(solrClient == null){
            solrUrlString = getSolrUrlString();
            solrClient = new HttpSolrClient.Builder(solrUrlString).build();
        }
        return solrClient;
    }

    /**
    * Method to retrieve the solr Url from the properties file
     *
     * @return solrUrl string parsed from the properties file
    */
    private static String getSolrUrlString(){
        String configFileName = "keyword_search.properties";
        ClassLoader classLoader = SolrClientFactory.class.getClassLoader();
        InputStream configStream = classLoader.getResourceAsStream(configFileName);

        String solrUrl = null;

        if (configStream != null) {
             Properties properties = new Properties();
            try {
                properties.load(configStream);

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = (String) entry.getKey();
                    String value = ((String) entry.getValue()).trim();

                    if ("index.solrurl".equals(key) && !StringUtils.isBlank(value)) {
                        solrUrl = value;
                    }
                }
            } catch (IOException e){
                LOG.error("keyword_search.properties: error while loading file '" + configFileName
                        + "'", e);
            }
        }

        return solrUrl;
    }

}
