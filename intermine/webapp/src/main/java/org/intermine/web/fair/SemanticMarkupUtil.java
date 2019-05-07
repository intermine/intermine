package org.intermine.web.fair;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.api.uri.InterMineLUI;
import org.intermine.api.uri.InterMineLUIConverter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.PermanentURIHelper;
import org.intermine.web.util.URLGenerator;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class providing schema/bioschemas markups
 * @author Daniela Butano
 */
public final class SemanticMarkupUtil
{
    private static final String SCHEMA = "http://schema.org";
    private static final String BIO_SCHEMA = "http://bioschemas.org";
    private static final String DATACATALOG_TYPE = "DataCatalog";
    private static final String DATASET_TYPE = "DataSet";
    private static final String BIO_ENTITY_TYPE = "BioChemEntity";
    private static final String PROTEIN_ENTITY_TYPE = "Protein";
    private static final String GENE_ENTITY_TYPE = "Gene";
    private static final String INTERMINE_CITE = "http://www.ncbi.nlm.nih.gov/pubmed/23023984";
    private static final String INTERMINE_REGISTRY = "https://registry.intermine.org/";
    private static final Logger LOG = Logger.getLogger(SemanticMarkupUtil.class);

    private SemanticMarkupUtil() {
        // don't instantiate
    }

    /**
     * Return the mine instance identifier, e.g. registry.org/flymine
     * @param request the http request
     * @return the identifier
     */
    private static String getMineIdentifier(HttpServletRequest request) {
        HttpSession session = null;
        ServletContext context = null;
        try {
            session = request.getSession();
        } catch (RuntimeException ex) {
        //if request.getSession() is call via ws it will throw an exception
        //in that case we do not cache the mineIdentifier in the context
        }
        if (session != null) {
            context = session.getServletContext();
            if (context != null && context.getAttribute("mineIdentifier") != null) {
                return (String) context.getAttribute("mineIdentifier");
            }
        }

        String mineIdentifier = null;
        String namespace = null;
        String mineURL = new URLGenerator(request).getPermanentBaseURL();
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(INTERMINE_REGISTRY
                    + "service/namespace?url=" + mineURL).request().get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                JSONObject result = new JSONObject(response.readEntity(String.class));
                namespace = result.getString("namespace");
            }
        } catch (RuntimeException ex) {
            LOG.error("Problems connecting to InterMine registry");
        }
        mineIdentifier = (namespace != null && !namespace.trim().isEmpty())
                ? INTERMINE_REGISTRY + namespace
                : mineURL;
        if (context != null) {
            context.setAttribute("mineIdentifier", mineIdentifier);
        }
        LOG.info("Mine identifier is: " + mineIdentifier);
        return mineIdentifier;
    }

    /**
     * Return the list of dataset to be added to the datacatalog type
     * @param request the http request
     * @return the list of dataset
     */
    private static List<Map<String, Object>> getDatSets(HttpServletRequest request) {
        List<Map<String, Object>> dataSets = new ArrayList<>();
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        pathQuery.addViews("DataSet.name", "DataSet.description", "DataSet.url");
        pathQuery.addOrderBy("DataSet.name", OrderDirection.ASC);
        PathQueryExecutor executor = new PathQueryExecutor(PathQueryAPI.getObjectStore(),
                PathQueryAPI.getProfile(), null, PathQueryAPI.getBagManager());
        try {
            ExportResultsIterator iterator = executor.execute(pathQuery);
            Map<String, Object> dataset = null;
            while (iterator.hasNext()) {
                dataset = new LinkedHashMap<>();
                List<ResultElement> elem = iterator.next();
                String name = (String) elem.get(0).getField();
                String description = (String) elem.get(1).getField();
                String url = (String) elem.get(2).getField();
                buildDataSetMarkup(dataset, name, description, url, request);
                dataSets.add(dataset);
            }
        } catch (ObjectStoreException ex) {
            //
        }
        return dataSets;
    }

    /**
     * Returns schema.org markups to be added to the home page
     * @param request the HttpServletRequest
     *
     * @return the map containing the markups
     */
    public static Map<String, Object> getDataCatalogMarkup(HttpServletRequest request) {
        if (!isEnabled()) {
            return null;
        }
        Properties props = PropertiesUtil.getProperties();
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATACATALOG_TYPE);
        semanticMarkup.put("name", props.getProperty("project.title"));
        semanticMarkup.put("description", props.getProperty("project.subTitle"));
        semanticMarkup.put("keywords", "Data warehouse, Data integration,"
                + "Bioinformatics software");
        semanticMarkup.put("identifier", getMineIdentifier(request));
        semanticMarkup.put("url", new URLGenerator(request).getPermanentBaseURL());

        //citation
        Map<String, String> citation = new LinkedHashMap<>();
        citation.put("@type", "CreativeWork");
        citation.put("identifier", INTERMINE_CITE);
        semanticMarkup.put("citation", citation);
        //organization
        Map<String, String> organization = new LinkedHashMap<>();
        organization.put("@type", "Organization");
        organization.put("name", "InterMine");
        organization.put("url", "http://intermine.org");
        semanticMarkup.put("provider", organization);
        //provider
        Map<String, String> provider = new LinkedHashMap<>();
        provider.put("@type", "Person");
        provider.put("name", "InterMine support");
        provider.put("email", "support@intermine.org");
        semanticMarkup.put("provider", provider);
        //sourceOrganization
        Map<String, String> sourceOrganization = new LinkedHashMap<>();
        sourceOrganization.put("@type", "Organization");
        sourceOrganization.put("name", "University of Cambridge");
        sourceOrganization.put("url", "https://www.gen.cam.ac.uk/");
        semanticMarkup.put("sourceOrganization", sourceOrganization);

        //datasets
        semanticMarkup.put("dataset", getDatSets(request));
        return semanticMarkup;
    }

    /**
     * Returns schema.org markups to be added to the dataset report page
     * @param request the HttpServletRequest
     * @param name the dataset name
     * @param description the dataset description
     * @param url the dataset url
     *
     * @return the map containing the markups
     */
    public static Map<String, Object> getDataSetMarkup(HttpServletRequest request, String name,
                                                       String description, String url) {
        if (!isEnabled()) {
            return null;
        }
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();
        buildDataSetMarkup(semanticMarkup, name, description, url, request);

        Map<String, String> dataCatalog = new LinkedHashMap<>();
        dataCatalog.put("@type", DATACATALOG_TYPE);
        dataCatalog.put("identifier", getMineIdentifier(request));
        semanticMarkup.put("includedInDataCatalog", dataCatalog);

        return semanticMarkup;
    }

    /**
     * Build the dataset schema.org markups
     * @param semanticMarkup the map where the markup are added
     * @param name the dataset name
     * @param description the dataset description
     * @param url the dataset url
     * @param request the HttpServletRequest
     *
     */
    private static void buildDataSetMarkup(Map<String, Object> semanticMarkup, String name,
                String description, String url, HttpServletRequest request) {
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATASET_TYPE);
        semanticMarkup.put("name", name);
        semanticMarkup.put("description", description);

        PermanentURIHelper helper = new PermanentURIHelper(request);
        String imUrlPage = helper.getPermanentURL(new InterMineLUI("DataSet", name));
        semanticMarkup.put("url", imUrlPage);

        //we use the dataset's url to set the identifier
        if (url != null && !url.trim().equals("")) {
            semanticMarkup.put("identifier", url);
        } else {
            semanticMarkup.put("identifier", imUrlPage);
        }
    }

    /**
     * Returns bioschema.org markups to be added to the report page of bio entities
     * @param request the HttpServletRequest
     * @param type the of the bioentity
     * @param id intermine internal id
     *
     * @return the map containing the markups
     *
     * @throws MetaDataException if the type is wrong
     */
    public static Map<String, Object> getBioEntityMarkup(HttpServletRequest request, String type,
                                                         int id) throws MetaDataException {
        if (!isEnabled()) {
            return null;
        }
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();

        if (ClassDescriptor.findInherithance(
                Model.getInstanceByName("genomic"), StringUtils.capitalize(type),
                "BioEntity")) {
            semanticMarkup.put("@context", BIO_SCHEMA);
            if ("Gene".equalsIgnoreCase(type)) {
                semanticMarkup.put("@type", Arrays.asList(GENE_ENTITY_TYPE, BIO_ENTITY_TYPE));
            } else if ("Protein".equalsIgnoreCase(type)) {
                semanticMarkup.put("@type", Arrays.asList(PROTEIN_ENTITY_TYPE, BIO_ENTITY_TYPE));
            } else {
                semanticMarkup.put("@type", BIO_ENTITY_TYPE);
            }
            semanticMarkup.put("name", getNameAttribute(type, id));
            try {
                InterMineLUI lui = (new InterMineLUIConverter()).getInterMineLUI(type, id);
                if (lui != null) {
                    semanticMarkup.put("identifier", lui.getIdentifier());
                    PermanentURIHelper helper = new PermanentURIHelper(request);
                    semanticMarkup.put("url", helper.getPermanentURL(lui));
                }
            } catch (ObjectStoreException ex) {
                LOG.error("Problem retrieving the identifier for the entity with ID: " + id);
            }

            /* bioschems suggested to remove it, need to be discussed
            String term = null;
            try {
                term = ClassDescriptor.findFairTerm(Model.getInstanceByName("genomic"), type);
                LOG.info("The term for the class " + type + " is: " + term);
            } catch (MetaDataException ex) {
                //error has been logged, no need to do more
            }
            if (term != null) {
                semanticMarkup.put("additionalType", term);
            }*/
        }
        return semanticMarkup;
    }

    /**
     * Return the value which will be assigned to the property name on the BioChemEntity type
     * @param type the type of the entity:Protein, Gene, BioEntity
     * @param id the interMineId which identifies the entity
     * @return the value of the property name in schema.org markup
     */
    private static String getNameAttribute(String type, int id) {
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String constraintPath;
        if ("Gene".equalsIgnoreCase(type)) {
            pathQuery.addView("Gene.symbol");
            constraintPath = "Gene.id";
        } else {
            pathQuery.addView("BioEntity.name");
            constraintPath = "BioEntity.id";
        }
        pathQuery.addConstraint(Constraints.eq(constraintPath, Integer.toString(id)));
        if (!pathQuery.isValid()) {
            LOG.info("The PathQuery :" + pathQuery.toString() + " is not valid");
            return null;
        }
        PathQueryExecutor executor = new PathQueryExecutor(PathQueryAPI.getObjectStore(),
                PathQueryAPI.getProfile(), null, PathQueryAPI.getBagManager());
        try {
            ExportResultsIterator iterator = executor.execute(pathQuery);
            if (iterator.hasNext()) {
                ResultElement cell = iterator.next().get(0);
                return (String) cell.getField();
            }
        } catch (ObjectStoreException ex) {
            LOG.info("Problem retrieving entity with type " + type + " and id " + id);
            return null;
        }
        return null;
    }

    /**
     * Return true if markup are enabled (disabled by default)
     * @return true if markup are enabled
     */
    public static boolean isEnabled() {
        Properties props = InterMineContext.getWebProperties();
        if (props.containsKey("markup.webpages.enable")
                && "true".equals(props.getProperty("markup.webpages.enable").trim())) {
            return true;
        }
        return false;
    }
}
