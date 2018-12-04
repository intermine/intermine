package org.intermine.web.fair;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.uri.InterMineLUI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.PermanentURIHelper;
import org.intermine.web.util.URLGenerator;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Class providing bioschema markups
 * @author Daniela Butano
 */
public final class SemanticMarkupUtil
{
    private static final String SCHEMA = "http://bioschemas.org";
    private static final String DATACATALOGUE_TYPE = "DataCatalogue";
    private static final String DATASET_TYPE = "DataSet";
    private static final String BIO_ENTITY_TYPE = "BioChemEntity";
    private static final String INTERMINE_CITE = "http://www.ncbi.nlm.nih.gov/pubmed/23023984";
    private static final Logger LOG = Logger.getLogger(SemanticMarkupUtil.class);

    private SemanticMarkupUtil() {
        // don't instantiate
    }

    /**
     * Returns schema.org markups to be added to the home page
     * @param request the HttpServletRequest
     *
     * @return the map containing the markups
     */
    public static Map<String, Object> getDataCatalogueMarkup(HttpServletRequest request) {
        Properties props = PropertiesUtil.getProperties();
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();
        //minimum  properties for bioschema.org
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATACATALOGUE_TYPE);
        semanticMarkup.put("description", props.getProperty("project.subTitle"));
        semanticMarkup.put("keywords", "Data warehouse, Data integration,"
                + "Bioinformatics software");
        semanticMarkup.put("name", props.getProperty("project.title"));
        semanticMarkup.put("provider", "addprovider");
        semanticMarkup.put("url", new URLGenerator(request).getPermanentBaseURL());

        //recommended properties by bioschema.org
        Map<String, String> citation = new LinkedHashMap<>();
        citation.put("@type", "CreativeWork");
        citation.put("identifier", INTERMINE_CITE);
        semanticMarkup.put("citation", citation);
        //semanticMarkup.put("dataset", );
        semanticMarkup.put("identifier", new PermanentURIHelper(request).getPermanentBaseURI());
        //semanticMarkup.put("publication", );
        return semanticMarkup;
    }

    /**
     * Returns schema.org markups to be added to the dataset report page
     * @param request the HttpServletRequest
     * @param name the dataset name
     *
     * @return the map containing the markups
     */
    public static Map<String, String> getDataSetMarkup(HttpServletRequest request, String name) {
        Properties props = PropertiesUtil.getProperties();
        Map<String, String> semanticMarkup = new LinkedHashMap<>();
        //minimum  properties for bioschema.org
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATASET_TYPE);
        semanticMarkup.put("description", "DataSet " + name);
        InterMineLUI lui = new InterMineLUI("DataSet", name);
        PermanentURIHelper helper = new PermanentURIHelper(request);
        semanticMarkup.put("identifier", helper.getPermanentURI(lui));
        semanticMarkup.put("keywords", "");
        semanticMarkup.put("name", name);
        semanticMarkup.put("url", helper.getPermanentURI(lui));

        //recommended properties by bioschema.or

        return semanticMarkup;
    }

    /**
     * Returns schema.org markups to be added to the bioentity report page
     * @param request the HttpServletRequest
     * @param type the bioentity type
     * @param primaryidentifier the primary identifier of the bioetntity
     *
     * @return the map containing the markups
     */
    public static Map<String, String> getBioEntityMarkup(HttpServletRequest request, String type,
            String primaryidentifier) {
        Map<String, String> semanticMarkup = new LinkedHashMap<>();
        //properties for bioschema.org
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", BIO_ENTITY_TYPE);
        String term = null;
        try {
            term = ClassDescriptor.findFairTerm(Model.getInstanceByName("genomic"), type);
            LOG.info("The term for the class " + type + " is: " + term);
        } catch (MetaDataException ex) {
            //error has been logged, no need to do more
        }
        if (term != null) {
            semanticMarkup.put("additionalType", term);
        }
        semanticMarkup.put("description",  type + " " + primaryidentifier);
        PermanentURIHelper helper = new PermanentURIHelper(request);
        InterMineLUI lui = new InterMineLUI(type, primaryidentifier);
        semanticMarkup.put("identifier", helper.getPermanentURI(lui));
        semanticMarkup.put("name", type + " " + primaryidentifier);
        semanticMarkup.put("url", helper.getPermanentURI(lui));

        return semanticMarkup;
    }
}
