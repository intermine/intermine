package org.intermine.api.rdf;

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
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to manage identifiers
 * @author Daniela Butano
 */
public class PurlConfig
{
    private static HashMap<String, HashMap<String, String>> identifiers;
    private final static String ALL_ORGANISMS = "*";
    private final static String START_KEY = "<<";
    private final static String END_KEY = ">>";

    private static final Logger LOG = Logger.getLogger(PurlConfig.class);

    private PurlConfig() {
        identifiers = new HashMap<>();
        Properties purlProperties = new Properties();
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("purl.properties");
        if (is != null) {
            try {
                purlProperties.load(is);

                final String regexp = "identifier\\.([^.]+)" + "\\.([^.]+)(\\.url)";
                Pattern p = Pattern.compile(regexp);
                String entityType = null;
                String taxId = null;

                for (Map.Entry<Object, Object> entry : purlProperties.entrySet()) {
                    String key = (String) entry.getKey();
                    Matcher matcher = p.matcher(key);
                    if (matcher.matches()) {
                        entityType = matcher.group(1);
                        taxId = matcher.group(2);

                        String identifier = (String) entry.getValue();
                        HashMap<String, String> identifiersByTaxonomy = identifiers.get(entityType);
                        if (identifiersByTaxonomy == null) {
                            identifiersByTaxonomy = new HashMap<>();
                            identifiers.put(entityType, identifiersByTaxonomy);
                        }
                        identifiersByTaxonomy.put(taxId, identifier);
                    }
                }

            } catch (IOException e) {
                LOG.error("Issues reading the purl.properties", e);
            }
        }
    }


    /**
     * Return the external identifier as configured in purl.properties
     * @param imObject the im object
     * @return the external identifier or null
     */
    public static String getExternalIdentifier(InterMineObject imObject) {
        if (identifiers == null) {
            new PurlConfig();
        }
        String objectType = DynamicUtil.getSimpleClass(imObject).getSimpleName();
        HashMap<String, String> identifiersByTaxonomy = identifiers.get(objectType);
        if (identifiersByTaxonomy == null) {
            return null;
        } else {
            String taxonId = ALL_ORGANISMS;
            //identifiers valid for all organisms
            if (!identifiersByTaxonomy.containsKey(ALL_ORGANISMS)) {
                ProxyReference proxy = null;
                try {
                    proxy = (ProxyReference) imObject.getFieldProxy("organism");
                    if (proxy != null) {
                        InterMineObject organismObject = proxy.getObject();
                        taxonId = organismObject.getFieldValue("taxonId").toString();
                    }
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }

            String partialIdentifier = identifiersByTaxonomy.get(taxonId);
            return createIdentifier(imObject, partialIdentifier);
        }
    }

    //given something like https://identifiers.org/pubmed:<<pubMedId>> returns
    //https://identifiers.org/pubmed:10402955
    private static String createIdentifier(InterMineObject imObject,
                                           String partialIdentifier) {

        int startKeyIndex = partialIdentifier.indexOf(START_KEY);
        String accessionKey = partialIdentifier.substring(startKeyIndex + 2,
                partialIdentifier.indexOf(END_KEY));
        try {
            String accessionValue = (String) imObject.getFieldValue(accessionKey);
            return partialIdentifier.substring(0, startKeyIndex).concat(accessionValue);
        } catch (IllegalAccessException ex) {
            return null;
        }
    }
}
