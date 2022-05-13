package org.intermine.web.fair;

/*
 * Copyright (C) 2002-2022 FlyMine
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
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.rdf.PurlConfig;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
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
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Map;
import java.util.Iterator;

/**
 * Class providing schema/bioschemas markups
 * @author Daniela Butano
 */
public final class SemanticMarkupFormatter
{
    private static final String SCHEMA = "https://schema.org";
    private static final String DATASET_TYPE = "DataSet";
    private static final String BIOCHEMENTITY_TYPE = "BioChemEntity";
    private static final String PROTEIN_TYPE = "Protein";
    private static final String GENE_TYPE = "Gene";
    private static final String INTERMINE_CITE = "http://www.ncbi.nlm.nih.gov/pubmed/23023984";
    private static final String INTERMINE_REGISTRY = "https://registry.intermine.org/";
    private static String baseURI = null;
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
        baseURI = new PermanentURIHelper(request).getPermanentBaseURI();
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
        semanticMarkup.put("identifier", identifier);
        semanticMarkup.put("@id", url);
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
     * @param profile the profile
     * @return the list of dataset
     */
    private static List<Map<String, Object>> formatDataSets(HttpServletRequest request,
                                                            Profile profile) {
        List<Map<String, Object>> dataSets = new ArrayList<>();
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        pathQuery.addViews("DataSet.name", "DataSet.description", "DataSet.url",
                "DataSet.licence");
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
                String licence = (String) elem.get(3).getField();
                dataSets.add(formatDataSet(name, description, url, licence, request));
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
     * @param licence the dataset licence
     * @param request the HttpServletRequest
     * @return the map representing the dataset
     */
    private static Map<String, Object> formatDataSet(String name, String description, String url,
                                      String licence, HttpServletRequest request) {
        Map<String, Object> semanticMarkup = new LinkedHashMap<>();
        semanticMarkup.put("@context", SCHEMA);
        semanticMarkup.put("@type", DATASET_TYPE);
        semanticMarkup.put("name", name);
        if (description != null && !description.isEmpty()) {
            semanticMarkup.put("description", description);
        } else {
            semanticMarkup.put("description", name);
        }
        if (licence != null && !licence.isEmpty()) {
            semanticMarkup.put("licence", licence);
        }

        PermanentURIHelper helper = new PermanentURIHelper(request);
        InterMineLUI lui = new InterMineLUI("DataSet", name);
        String imUrlPage = helper.getPermanentURL(lui);
        semanticMarkup.put("identifier", lui.toString());
        semanticMarkup.put("url", imUrlPage);
        semanticMarkup.put("@id", imUrlPage);
        //we use the dataset's url to set the identifier
        if (url != null && !url.trim().equals("")) {
            semanticMarkup.put("sameAs", url);
        }

        return semanticMarkup;
    }

    /**
     * Build the dataset markups given the intermine entity
     * @param request the HttpServletRequest
     * @param entity InterMine ID
     * @return the map representing the dataset
     */
    private static Map<String, Object> formatDataSet(HttpServletRequest request,
                                                     InterMineObject entity) {
        try {
            String name = (String) entity.getFieldValue("name");
            String description = (String) entity.getFieldValue("description");
            String url = (String) entity.getFieldValue("url");
            String licence = (String) entity.getFieldValue("licence");
            return formatDataSet(name, description, url, licence, request);
        } catch (IllegalAccessException iae) {
            LOG.warn("Failed to access object with id: " + entity.getId(), iae);
            return null;
        }
    }

    /**
     * Returns bioschema.org markups to be added to the report page of bio entities
     * @param request the HttpServletRequest
     * @param id intermine internal id
     *
     * @return the map containing the markups
     */
    public static Map<String, Object> formatBioEntity(HttpServletRequest request, int id) {
        if (!isEnabled()) {
            return null;
        }
        InterMineAPI im = InterMineContext.getInterMineAPI();
        ObjectStore os = im.getObjectStore();
        InterMineObject entity = null;
        String type = null;

        try {
            entity = os.getObjectById(id);
            type = DynamicUtil.getSimpleClass(entity).getSimpleName();
            if ("DataSet".equalsIgnoreCase(type)) {
                return formatDataSet(request, entity);
            }
        } catch (ObjectStoreException ose) {
            LOG.warn("Failed to find object with id: " + id, ose);
        }

        Map<String, Object> semanticMarkup = formatBioEntity(entity);
        if (semanticMarkup == null) {
            return null;
        }
        semanticMarkup.put("@context", SCHEMA);
        Properties props = PropertiesUtil.getProperties();
        semanticMarkup.put("version", props.getProperty("project.releaseVersion"));
        Map<String, String> isPartOf = new LinkedHashMap<>();
        isPartOf.put("@type", DATASET_TYPE);
        isPartOf.put("name", props.getProperty("project.title"));
        isPartOf.put("description", props.getProperty("project.subTitle"));
        semanticMarkup.put("isPartOf", isPartOf);

        return semanticMarkup;
    }

    /**
     * Build the dataset markups given the intermine entity
     * @param imObject InterMine object
     * @return the map representing the dataset
     */
    private static Map<String, Object> formatBioEntity(InterMineObject imObject) {
        String type = DynamicUtil.getSimpleClass(imObject).getSimpleName();

        if ("Gene".equalsIgnoreCase(type)) {
            return formatGene(imObject, false);
        } else if ("Protein".equalsIgnoreCase(type)) {
            return formatProtein(imObject, false);
        } else if (isBioChemEntityType(type)) {
            return formatBioChemEntity(imObject);
        } else {
            return null;
        }
    }

    private static Map<String, Object> formatGene(InterMineObject gene, boolean onlyMinimun) {
        Map<String, Object> geneMarkups = new LinkedHashMap<>();
        geneMarkups.put("@type", GENE_TYPE);
        setBasicProperties(gene.getId(), geneMarkups);
        setName(gene, geneMarkups);
        setSameAs(gene, geneMarkups);
        try {
            geneMarkups.put("description", (String) gene.getFieldValue("description"));
            setTaxonomicRange(gene, geneMarkups);
            if (!onlyMinimun) {
                //isPartOfBioChemEntity -> chromosome
                ProxyReference chromRef = (ProxyReference) gene.getFieldProxy("chromosome");
                if (chromRef != null) {
                    InterMineObject chromosome = chromRef.getObject();
                    geneMarkups.put("isPartOfBioChemEntity", formatBioChemEntity(chromosome));
                }
                //encodesBioChemEntity -> proteins
                ProxyCollection proteinsCollections =
                        (ProxyCollection) gene.getFieldProxy("proteins");
                if (proteinsCollections != null) {
                    Iterator it = proteinsCollections.iterator();
                    while (it.hasNext()) {
                        InterMineObject protein = (InterMineObject) it.next();
                        geneMarkups.put("encodesBioChemEntity", formatProtein(protein, true));
                    }
                }
            }
            return geneMarkups;
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static void setBasicProperties(Integer intermineId, Map<String, Object> entityMarkups) {
        InterMineLUI lui = (new InterMineLUIConverter()).getInterMineLUI(intermineId);
        if (lui != null) {
            String url = baseURI.concat(lui.toString());
            entityMarkups.put("url", url);
            entityMarkups.put("@id", url);
            entityMarkups.put("identifier", lui.toString());
        }
    }

    private static void setName(InterMineObject bioEntity, Map<String, Object> entityMarkups) {
        try {
            String symbol = (String) bioEntity.getFieldValue("symbol");
            String primaryIdentifier = (String) bioEntity.getFieldValue("primaryIdentifier");
            entityMarkups.put("name", (!StringUtils.isEmpty(symbol)) ? symbol : primaryIdentifier);
        }  catch (IllegalAccessException iae) {
            LOG.warn("Failed to access object with id: " + bioEntity.getId(), iae);
        }
    }

    private static void setSameAs(InterMineObject bioEntity, Map<String, Object> entityMarkups) {
        String externalIdentifier = PurlConfig.getExternalIdentifier(bioEntity);
        if (externalIdentifier != null) {
            entityMarkups.put("sameAs", externalIdentifier);
        }
    }

    private static void setTaxonomicRange(InterMineObject imObject, Map<String, Object> markups) {
        try {
            ProxyReference organismRef = (ProxyReference) imObject.getFieldProxy("organism");
            if (organismRef != null) {
                InterMineObject organism = organismRef.getObject();
                InterMineLUI lui = new InterMineLUIConverter().getInterMineLUI(organism.getId());
                if (lui != null) {
                    markups.put("taxonomicRange", baseURI.concat(lui.toString()));
                }
            }
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
    }

    private static Map<String, Object> formatProtein(InterMineObject protein, boolean onlyMinimun) {
        Map<String, Object> proteinMarkups = new LinkedHashMap<>();
        proteinMarkups.put("@type", PROTEIN_TYPE);
        setBasicProperties(protein.getId(), proteinMarkups);
        setName(protein, proteinMarkups);
        setSameAs(protein, proteinMarkups);
        setTaxonomicRange(protein, proteinMarkups);

        try {
            if (!onlyMinimun) {
                //isEncodedByBioChemEntity -> genes
                ProxyCollection genesCollections = (ProxyCollection) protein.getFieldProxy("genes");
                if (genesCollections != null) {
                    Iterator it = genesCollections.iterator();
                    while (it.hasNext()) {
                        InterMineObject gene = (InterMineObject) it.next();
                        proteinMarkups.put("isEncodedByBioChemEntity", formatGene(gene, true));
                    }
                }
            }
            return proteinMarkups;

        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static Map<String, Object> formatBioChemEntity(InterMineObject bioChemEntity) {
        Map<String, Object> bioChemEntityMarkups = new LinkedHashMap<>();
        bioChemEntityMarkups.put("@type", BIOCHEMENTITY_TYPE);
        setBasicProperties(bioChemEntity.getId(), bioChemEntityMarkups);
        setName(bioChemEntity, bioChemEntityMarkups);
        setSameAs(bioChemEntity, bioChemEntityMarkups);
        return bioChemEntityMarkups;
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

    /**
     * Check if the inputType can be markup with BioChemEntity
     * @return true if can be markup (because it extends BioEntity)
     */
    private static boolean isBioChemEntityType(String inputType) {
        try {
            if (ClassDescriptor.findInherithance(Model.getInstanceByName("genomic"),
                    inputType, "BioEntity")) {
                return true;
            }
        } catch (MetaDataException mde) {
            LOG.warn("Type " + inputType + " is not in the model");
            return false;
        }
        return false;
    }
}
