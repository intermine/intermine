package org.intermine.web.fair;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;
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
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.registry.model.Instance;
import org.intermine.web.registry.model.RegistryResponse;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.core.SessionlessRequest;
import org.json.JSONObject;

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
public final class SemanticMarkupFormatter
{
    private static final String SCHEMA = "http://schema.org";
    private static final String BIO_SCHEMA = "http://bioschemas.org";
    private static final String DATASET_TYPE = "DataSet";
    private static final String BIO_ENTITY_TYPE = "BioChemEntity";
    private static final String PROTEIN_ENTITY_TYPE = "Protein";
    private static final String GENE_ENTITY_TYPE = "Gene";
    private static final String INTERMINE_CITE = "http://www.ncbi.nlm.nih.gov/pubmed/23023984";
    private static final String INTERMINE_REGISTRY = "https://registry.intermine.org/";
    private static final Logger LOG = Logger.getLogger(SemanticMarkupFormatter.class);

    private SemanticMarkupFormatter() {
        // don't instantiate
    }

    /**
     * Return the mine instance identifier, e.g. registry.org/flymine
     * @param request the http request
     * @return the identifier
     */
    private static Instance getMineInstance(HttpServletRequest request) {
        Instance imInstance = null;
        if (!(request instanceof SessionlessRequest)) {
            HttpSession session = request.getSession();
            imInstance = SessionMethods.getBasicInstanceInfo(session);
            if (imInstance != null) {
                return imInstance;
            }
        }

        String mineURL = new URLGenerator(request).getPermanentBaseURL();
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(INTERMINE_REGISTRY
                    + "service/namespace?url=" + mineURL).request().get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                JSONObject result = new JSONObject(response.readEntity(String.class));
                String namespace = result.getString("namespace");
                LOG.info("Namespace is: " + namespace);
                response = client.target(INTERMINE_REGISTRY
                        + "service/instances/" + namespace).request().get();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    RegistryResponse registryRes = response.readEntity(RegistryResponse.class);
                    imInstance = registryRes.getInstance();
                }
                if (!(request instanceof SessionlessRequest)) {
                    SessionMethods.setBasicInstanceInfo(request.getSession().getServletContext(),
                            imInstance);
                }
            }
        } catch (RuntimeException ex) {
            LOG.error("Problems connecting to InterMine registry");
            return null;
        }
        return imInstance;
    }

    /**
     * Returns schema.org markups to be added to the home page
     * @param request the HttpServletRequest
     * @param profile the profile
     *
     * @return the map containing the markups
     */
    public static Map<String, Object> formatInstance(HttpServletRequest request, Profile profile) {
        if (!isEnabled()) {
            return null;
        }
        Instance instance = getMineInstance(request);

        Properties props = PropertiesUtil.getProperties();
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATASET_TYPE);
        semanticMarkup.put("name", props.getProperty("project.title"));
        semanticMarkup.put("description", props.getProperty("project.subTitle"));
        semanticMarkup.put("keywords", "Data warehouse, Data integration,"
                + "Bioinformatics software");
        String url = new URLGenerator(request).getPermanentBaseURL();
        String identifier = (instance != null)
                ? INTERMINE_REGISTRY + instance.getNamespace()
                : url;
        semanticMarkup.put("@id", identifier);
        semanticMarkup.put("url", url);

        //citation
        Map<String, String> citation = new LinkedHashMap<>();
        citation.put("@type", "CreativeWork");
        citation.put("@id", INTERMINE_CITE);
        citation.put("url", INTERMINE_CITE);
        semanticMarkup.put("citation", citation);

        //contactPoint/support
        Map<String, String> contactPoint = new LinkedHashMap<>();
        contactPoint.put("@type", "Person");
        contactPoint.put("name", "Support");
        if (instance != null && !StringUtils.isEmpty(instance.getMaintainerEmail())) {
            contactPoint.put("email", instance.getMaintainerEmail());
        } else {
            contactPoint.put("email", "support@intermine.org");
        }
        semanticMarkup.put("contactPoint", contactPoint);

        //Provider
        Map<String, String> provider = new LinkedHashMap<>();
        provider.put("@type", "Organization");
        if (instance != null && !StringUtils.isEmpty(instance.getMaintainerOrgName())) {
            provider.put("name", instance.getMaintainerOrgName());
        } else {
            provider.put("name", "InterMine");
        }
        if (instance != null && !StringUtils.isEmpty(instance.getMaintainerUrl())) {
            provider.put("url", instance.getMaintainerUrl());
        } else {
            provider.put("url", "http://intermine.org");
        }
        semanticMarkup.put("provider", provider);

        //datasets
        semanticMarkup.put("isBasedOn", formatDataSets(request, profile));
        return semanticMarkup;
    }

    /**
     * Return the list of dataset to be added to the main dataset type
     * @param request the http request
     * @return the list of dataset
     */
    private static List<Map<String, Object>> formatDataSets(HttpServletRequest request,
                                                            Profile profile) {
        List<Map<String, Object>> dataSets = new ArrayList<>();
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        pathQuery.addViews("DataSet.name", "DataSet.description", "DataSet.url");
        pathQuery.addOrderBy("DataSet.name", OrderDirection.ASC);
        InterMineAPI im = InterMineContext.getInterMineAPI();
        PathQueryExecutor executor = new PathQueryExecutor(im.getObjectStore(),
                profile, null, im.getBagManager());
        try {
            ExportResultsIterator iterator = executor.execute(pathQuery);
            while (iterator.hasNext()) {
                List<ResultElement> elem = iterator.next();
                String name = (String) elem.get(0).getField();
                String description = (String) elem.get(1).getField();
                String url = (String) elem.get(2).getField();
                dataSets.add(formatDataSet(name, description, url, request));
            }
        } catch (ObjectStoreException ex) {
            //
        }
        return dataSets;
    }

    /**
     * Build the dataset schema.org markups
     * @param name the dataset name
     * @param description the dataset description
     * @param url the dataset url
     * @param request the HttpServletRequest
     * @return the map representing the dataset
     */
    public static Map<String, Object> formatDataSet(String name, String description, String url,
                                      HttpServletRequest request) {
        if (!isEnabled()) {
            return null;
        }
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATASET_TYPE);
        semanticMarkup.put("name", name);
        if (description != null && !description.isEmpty()) {
            semanticMarkup.put("description", description);
        } else {
            semanticMarkup.put("description", name);
        }

        PermanentURIHelper helper = new PermanentURIHelper(request);
        String imUrlPage = helper.getPermanentURL(new InterMineLUI("DataSet", name));
        semanticMarkup.put("@id", imUrlPage);
        semanticMarkup.put("url", imUrlPage);

        //we use the dataset's url to set the identifier
        if (url != null && !url.trim().equals("")) {
            semanticMarkup.put("sameAs", url);
        }

        return semanticMarkup;
    }

    /**
     * Returns bioschema.org markups to be added to the report page of bio entities
     * @param request the HttpServletRequest
     * @param type the of the bioentity
     * @param id intermine internal id
     * @param profile the profile
     *
     * @return the map containing the markups
     *
     * @throws MetaDataException if the type is wrong
     */
    public static Map<String, Object> formatBioEntity(HttpServletRequest request, String type,
                                              int id, Profile profile) throws MetaDataException {
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
                InterMineLUI lui = (new InterMineLUIConverter(profile))
                        .getInterMineLUI(type, id);
                if (lui != null) {
                    semanticMarkup.put("@id", lui.getIdentifier());
                    PermanentURIHelper helper = new PermanentURIHelper(request);
                    semanticMarkup.put("url", helper.getPermanentURL(lui));
                }
            } catch (ObjectStoreException ex) {
                LOG.error("Problem retrieving the identifier for the entity with ID: " + id);
            }
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
        InterMineAPI im = InterMineContext.getInterMineAPI();
        PathQueryExecutor executor = new PathQueryExecutor(im.getObjectStore(),
                PathQueryAPI.getProfile(), null, im.getBagManager());
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
