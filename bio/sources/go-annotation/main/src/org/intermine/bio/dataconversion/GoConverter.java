package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.ontology.OboParser;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse a go annotation file into Items
 *
 *
 * @author Andrew Varley
 * @author Peter Mclaren - some additions to record the parents of a go term.
 * @author Julie Sullivan - changed evidence to be collection
 */
public class GoConverter extends FileConverter
{
    protected static final String PROP_FILE = "go-annotation_config.properties";

    private OboParser oboParser = null;
    protected File ontology;

    protected Map<String, Item> goTerms = new LinkedHashMap<String, Item>();
    protected Map<GoTermToGene, Item> goAnnoItems;
    private Map<String, String> goEvidence = new LinkedHashMap<String, String>();
    private Map<String, Item> datasources = new LinkedHashMap<String, Item>();
    private Map<String, String> datasets = new LinkedHashMap<String, String>();
    private Map<String, String> publications = new LinkedHashMap<String, String>();
    private Map<String, Item> organisms = new LinkedHashMap<String, Item>();
    private Map<String, String> termIdNameMap = null;
    private Map<String, String> geneAttributes = new HashMap<String, String>();
    private Map<String, WithType> withTypes = new LinkedHashMap<String, WithType>();
    private Map<String, String> synonymTypes = new HashMap<String, String>();
    protected Map<String, ItemWrapper> productWrapperMap = new LinkedHashMap<String, ItemWrapper>();
    private Map<GoTermToGene, PlaceHolder> holderMap =
        new LinkedHashMap<GoTermToGene, PlaceHolder>();
    protected Set<String> productIds = new HashSet<String>();
    private Map<String, Set> goTermId2ParentTermIdSetsMap = null;
    private static final Logger LOG = Logger.getLogger(GoConverter.class);
    protected IdResolverFactory resolverFactory;

    // TODO: datasources Map to contains ids not items? - need the dataset later on
    // TODO: store product after each one finished? - 'with' field may be a problem

    /**
     * Constructor
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws Exception if an error occurs in storing or finding Model
     */
    public GoConverter(ItemWriter writer, Model model) throws Exception {
        super(writer, model);
        addWithType("FB", "Gene", "primaryIdentifier");
        addWithType("UniProt", "Protein", "accession");
        synonymTypes.put("protein", "accession");
        synonymTypes.put("Protein", "accession");
        synonymTypes.put("gene", "identifier");
        synonymTypes.put("Gene", "identifier");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();

        readConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        //LOG.info("productWrapperMap.size() = " + productWrapperMap.size());
        // store all gene/protein data at the end of conversion because of  allGoAnnotation
        for (ItemWrapper nextWrapper : productWrapperMap.values()) {
            Item nextGeneProduct = nextWrapper.getItem();
            store(nextGeneProduct);
            store(nextWrapper.getSynonym());
        }
        super.close();
    }

    // read config file that has specific settings for each organism, key is taxon id
    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }
        Enumeration propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String taxonId = (String) propNames.nextElement();
            taxonId = taxonId.substring(0, taxonId.indexOf("."));
            Properties taxonProps = PropertiesUtil.stripStart(taxonId,
                PropertiesUtil.getPropertiesStartingWith(taxonId, props));
            String geneAttribute = taxonProps.getProperty("geneAttribute").trim();
            if (geneAttribute == null) {
                throw new IllegalArgumentException("Unable to find geneAttribute property for "
                                                   + "taxon: " + taxonId + " in file: "
                                                   + PROP_FILE);
            }
            if (!(geneAttribute.equals("identifier")
                            || geneAttribute.equals("primaryIdentifier"))) {
                throw new IllegalArgumentException("Invalid geneAttribute value for taxon: "
                                                   + taxonId + " was: " + geneAttribute);
            }
            geneAttributes.put(taxonId, geneAttribute);
        }
    }

    /**
     * Set the GO ontology file to be read from DAG format.
     *
     * @param ontology the GO ontology file
     */
    public void setOntologyfile(File ontology) {
        this.ontology = ontology;
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws ObjectStoreException, IOException {

        // Renew this at the start of processing each file
        holderMap = new LinkedHashMap<GoTermToGene, PlaceHolder>();
        goAnnoItems = new LinkedHashMap<GoTermToGene, Item>();

        // on first call: parse ontology file and set up maps
        if (goTermId2ParentTermIdSetsMap == null) {
            if (ontology == null) {
                throw new IllegalArgumentException("ontology must be specified");
            }
            try {
                if (ontology.getName().endsWith(".obo")) {
                    oboParser = new OboParser();
                    termIdNameMap = oboParser.getTermIdNameMap(new FileReader(ontology));
                    goTermId2ParentTermIdSetsMap = oboParser.getTermToParentTermSetMap();
                } else {
                    throw new IllegalArgumentException("Don't know how to deal with ontology file:"
                                                       + ontology.getAbsolutePath());
                }
            } catch (Exception e) {
                throw new IOException("" + e);
            }
        }

        BufferedReader br = new BufferedReader(reader);
        String line, lastProductId = null;

        // loop through entire file
        while ((line = br.readLine()) != null) {
            if (line.startsWith("!")) {
                continue;
            }
            String[] array = line.split("\t", -1); //keep trailing empty Strings
            if (array.length < 13) {
                throw new IllegalArgumentException("Not enough elements (should be > 13) in line: "
                                                   + line);
            }

            // We only want to create GOAnnotation objects applied to genes and proteins
            // some file entries apply to type 'transcript' and possibly others
            if (!("gene".equalsIgnoreCase(array[11])
                    || "protein".equalsIgnoreCase(array[11]))) {
                LOG.info("Ignored line with type: " + array[11]);
                continue;
            }

            String productId = array[1];

            // Wormbase has some proteins with UniProt accessions and some with WB:WP ids,
            // hack here to get just the UniProt ones.
            if ("protein".equalsIgnoreCase(array[11]) && !array[0].startsWith("UniProt")) {
                continue;
            }

            // if we move onto a new product id store the last one, we require that files
            // are ordered by product id
            if (lastProductId != null && !lastProductId.equals(productId)) {
                storeGoAnnotation();

                if (productIds.contains(productId)) {
                    throw new IllegalArgumentException("Product was found twice in file but not in "
                                               + "consecutive entries: " + productId + " in file: "
                                               + getCurrentFile() + ".  To save memory"
                                               + " we assume the file is ordered");
                }
                productIds.add(productId);

                // we have moved onto next product, clear product specific maps to save memory
                holderMap = new LinkedHashMap<GoTermToGene, PlaceHolder>();
                goAnnoItems = new LinkedHashMap<GoTermToGene, Item>();
            }

            lastProductId = productId;
            String goId = array[4];
            String qualifier = array[3];
            String strEvidence = array[6];
            String evidenceId = null;
            if (strEvidence != null && !strEvidence.equals("")) {
                evidenceId = newGoEvidence(strEvidence);
            }
            String type = array[11];

            // create unique key for go annotation (gene + go term)
            GoTermToGene key = new GoTermToGene(productId, goId, qualifier);

            // A term can be applied to a product with multiple publications/evidence codes, this
            // is done with an extra line in the association file.  We create just one GoAnnotation
            // object but have collections of publications and goEvidenceCodes
            if (!holderMap.containsKey(key)) {

                // get the rest of the data
                String ds = array[14];
                Item newDatasource = newDatasource(ds);
                String newPublicationId = newPublication(array[5]);
                Item newGoTerm = newGoTerm(goId, newDatasource);
                ReferenceList newGoEvidenceColl =
                    new ReferenceList("goEvidenceCodes", new ArrayList());
                if (evidenceId != null) {
                    newGoEvidenceColl.addRefId(evidenceId);
                }
                Item newOrganism = newOrganism(array[12]);

                // new gene/protein
                ItemWrapper newProductWrapper = newProduct(productId, type, newOrganism,
                                                newDatasource, true, null);

                // temporary object while we are rattling through the file
                // needed because we may have extra publications

                // check for null productWrapper - where idResolver could not find a current id
                if (newProductWrapper != null) {
                    PlaceHolder newPlaceHolder =
                        new PlaceHolder(qualifier, newDatasource, newPublicationId,
                                        newGoEvidenceColl, newProductWrapper, newGoTerm,
                                        array[7], newOrganism);
                    holderMap.put(key, newPlaceHolder);
                }

            } else {
                // we have already seen this product/go term pair so add extra pubs/evidence
                PlaceHolder holder = holderMap.get(key);

                String pubRefId = newPublication(array[5]);

                // add extra publication
                if (pubRefId != null) {
                    holder.getPubs().addRefId(pubRefId);
                // add reference to new evidence object
                } else if (evidenceId != null) {
                    holder.getGoEvidenceColl().addRefId(evidenceId);
                }
            }
        }

        // store the final product
        storeGoAnnotation();
    }


    // Store GoAnnotation items, need to keep products (genes/proteins) until the end.
    private void storeGoAnnotation() throws ObjectStoreException {

        // TODO: does this need to be done in separate loops?

        // loop through once to create GO annotation items
        for (PlaceHolder nextPlaceHolder : holderMap.values()) {
            // create assignment for this term
            newGoAnnotation(nextPlaceHolder);
        }

        // loop through again to create GO annotaion items to assign parents
        for (PlaceHolder nextPlaceHolder : holderMap.values()) {
            // if the qualifier for this assignment is NOT then isn't valid to assign parents
            String qually = nextPlaceHolder.getQualifier().toLowerCase();
            if (!qually.contains("not")) {
                newParentGoAnnotation(goTermId2ParentTermIdSetsMap, nextPlaceHolder);
            }
        }

        // store all the items we have created
        for (Item goAnnotation : goAnnoItems.values()) {
            store(goAnnotation);
        }
    }


    /**
     * iterate through the list of parents and process those.
     * @param placeHolder Object that holds all go annotation related information
     * @param goTermId2ParentTermId map of go terms to parent go terms
     * @throws ObjectStoreException if there is a problem
     */
    protected void newParentGoAnnotation(Map<String, Set> goTermId2ParentTermId,
                                         PlaceHolder placeHolder)
        throws ObjectStoreException {
        String goId = placeHolder.getGoTerm().getAttribute("identifier").getValue();
        if (goTermId2ParentTermId.containsKey(goId)) {
            Set parentTermIdsSet = goTermId2ParentTermId.get(goId);
            if (parentTermIdsSet != null) {
                processParents(placeHolder, parentTermIdsSet);
            }
        }
    }


    //------------------------- Produce a new GOAnnotation object -------------------------
    private void newGoAnnotation(PlaceHolder placeHolder) {

        boolean isProductTypeGene = (placeHolder.getGeneProductWrapper().getItem().getClassName()
                        .indexOf("Gene") >= 0);

        // go term id
        String goId = placeHolder.getGoTerm().getAttribute("identifier").getValue();

        ReferenceList actualGoTerms =  new ReferenceList("actualGoTerms", new ArrayList());

        // create new go annotation object
        Item currentGoItem = newGoAnnotationItem(goId, "true", placeHolder.getGoEvidenceColl(),
            placeHolder.getGeneProductWrapper().getItem().getIdentifier(),
            placeHolder.getGoTerm().getIdentifier(),
            actualGoTerms);

        // add this term to actual go terms list
        actualGoTerms.addRefId(placeHolder.getGoTerm().getIdentifier());

        // If the qualifier is not a NOT.
        if (!"".equals(placeHolder.getQualifier())) {
            currentGoItem.setAttribute("qualifier", placeHolder.getQualifier());
        }

        // if it has a name, add
        if (termIdNameMap.containsKey(goId)) {
            currentGoItem.setAttribute("name", termIdNameMap.get(goId));
        }

        // with objects
        if (!"".equals(placeHolder.getWithText())) {
            currentGoItem.setAttribute("withText", placeHolder.getWithText());
            List<Item> with = createWithObjects(placeHolder.getWithText(),
                                          placeHolder.getOrganism(),
                                          placeHolder.getDatasource());

            if (with.size() != 0) {
                List idList = new ArrayList();
                for (Item withObj : with) {
                    idList.add(withObj.getIdentifier());
                }
                currentGoItem.addCollection(new ReferenceList("with", idList));
            }
        }

        // dataset for goannotation object
        List<String> refIds = placeHolder.getDatasource().getCollection("dataSets").getRefIds();
        currentGoItem.setCollection("dataSets", refIds);

        // add item to gene go collections
        if (isProductTypeGene) {
            placeHolder.getGeneProductWrapper().getItem().addToCollection("goAnnotation",
                                                                          currentGoItem);
            placeHolder.getGeneProductWrapper().getItem().addToCollection("allGoAnnotation",
                                                                          currentGoItem);
        } else {
            LOG.debug("Skipping setting go & allGo annotation collection for a:"
                      + placeHolder.getGeneProductWrapper().getItem().getClassName()
                      + " with ident:"
                      + placeHolder.getGeneProductWrapper().getItem().getIdentifier());
        }

        // put in our list to store later
        String geneId = placeHolder.getGeneProductWrapper().getItem().getIdentifier();
        GoTermToGene key = new GoTermToGene(geneId, goId, placeHolder.getQualifier());
        if (!goAnnoItems.containsKey(key)) {
            goAnnoItems.put(key, currentGoItem);
        }
    }

    //-------------- PRODUCE THE SET OF PARENT TERM LINKING GOANNOTATION OBJECTS ---------------
    private void processParents(PlaceHolder placeHolder, Set<String> parentTermIdsSet)
    throws ObjectStoreException {

        // loop over the set of Id's and create a GoAnnotation object that links the gene
        // geneProduct, the infered parent term and the current go term items.

        boolean isProductTypeGene = (placeHolder.getGeneProductWrapper().getItem()
                        .getClassName().indexOf("Gene") >= 0);

        for (String parentTermGoId : parentTermIdsSet) {
            Item nextParentGoTermId = newGoTerm(parentTermGoId, placeHolder.getDatasource());

            Item parentItem = null;
            String geneId = placeHolder.getGeneProductWrapper().getItem().getIdentifier();
            GoTermToGene key = new GoTermToGene(geneId, parentTermGoId, placeHolder.getQualifier());

            // gene&goterm combo already exists
            if (holderMap.containsKey(key)) {

                PlaceHolder parentPlaceHolder = holderMap.get(key);
                parentItem = parentPlaceHolder.getGoAnno();
                // add this go term to the parent's collection of children
                parentItem.getCollection("actualGoTerms").addRefId(placeHolder.getGoTerm()
                                                                   .getIdentifier());
                if (!goAnnoItems.containsKey(key)) {
                    goAnnoItems.put(key, parentItem);
                }

            } else {
                ReferenceList actualGoTerms = null;
                if (goAnnoItems.containsKey(key)) {
                    parentItem = goAnnoItems.get(key);
                    actualGoTerms = parentItem.getCollection("actualGoTerms");
                } else {
                    // start a list of kids for this new parent
                    actualGoTerms =  new ReferenceList("actualGoTerms", new ArrayList());

                    // create annotation object
                    parentItem = newGoAnnotationItem(
                                 parentTermGoId, "false", placeHolder.getGoEvidenceColl(),
                                 placeHolder.getGeneProductWrapper().getItem().getIdentifier(),
                                 nextParentGoTermId.getIdentifier(),
                                 actualGoTerms);

                    goAnnoItems.put(key, parentItem);
                }
                actualGoTerms.addRefId(placeHolder.getGoTerm().getIdentifier());
            }

            // add name to parent
            if (termIdNameMap.containsKey(parentTermGoId)) {
                parentItem.setAttribute("name", termIdNameMap.get(parentTermGoId));
            }

            // add parent to collection
            if (isProductTypeGene) {
                if (!placeHolder.getGeneProductWrapper().getItem().getCollection("allGoAnnotation")
                                .getRefIds().contains(parentItem.getIdentifier())) {
                    placeHolder.getGeneProductWrapper().getItem().addToCollection("allGoAnnotation",
                                                                                  parentItem);
                }
            } else {
                LOG.debug("Skipping adding a parent GoAnnotation ref to the "
                          + "allGoAnnotation collection of a non gene item.");
            }
        }
    }


    /**
     * Create a new go annotation item.  This is the actual item that will be stored.
     *
     * @param identifier          the identifier i.e. GO:0000001 etc etc
     * @param isPrimaryAssignment a boolean expressed as a string indicating that this is the
     *                            actual reference between gene and go term, and not just a inferred
     *                            parent relationship.
     * @param goEvidenceColl    collection of evidence terms related to this annotation
     * @param subject gene described by go term
     * @param property go term
     * @param actualGoTerms list of children goterms
     * @return the go annotation item
     */
    private Item newGoAnnotationItem(String identifier,
                                     String isPrimaryAssignment,
                                     ReferenceList goEvidenceColl,
                                     String subject,
                                     String property,
                                     ReferenceList actualGoTerms) {

        Item goAnnoItem = createItem("GOAnnotation");
        goAnnoItem.setAttribute("identifier", identifier);
        goAnnoItem.setAttribute("isPrimaryAssignment", isPrimaryAssignment);
        if (goEvidenceColl != null && !goEvidenceColl.getRefIds().isEmpty()) {
            goAnnoItem.addCollection(goEvidenceColl);
        }
        if (actualGoTerms != null) {
            goAnnoItem.addCollection(actualGoTerms);
        }
        goAnnoItem.setReference("subject", subject);
        goAnnoItem.setReference("property", property);
        return goAnnoItem;
    }


    private void addWithType(String prefix, String clsName, String fieldName) {
        withTypes.put(prefix, new WithType(clsName, fieldName));
    }


    /**
     * Given the 'with' text from a gene_association entry parse for recognised identifier
     * types and create Gene or Protein items accordingly.
     *
     * @param withText string from the gene_association entry
     * @param organism organism to reference
     * @param dataSource datasource item
     * @return a list of Items
     */
    protected List<Item> createWithObjects(String withText, Item organism, Item dataSource) {

        List<Item> withProductList = new ArrayList<Item>();
        try {
            String[] elements = withText.split("[; |,]");
            for (int i = 0; i < elements.length; i++) {
                String entry = elements[i].trim();
                // rely on the format being type:identifier
                if (entry.indexOf(':') > 0) {
                    String prefix = entry.substring(0, entry.indexOf(':'));
                    String value = entry.substring(entry.indexOf(':') + 1);

                    if (withTypes.containsKey(prefix) && (value != null) && (!value.equals(""))) {
                        WithType wt = withTypes.get(prefix);
                        ItemWrapper productWrapper = null;

                        // if a UniProt protein it may be from a differnet organism
                        // also FlyBase may be from a different Drosophila species
                        if (prefix.equals("UniProt")) {
                            productWrapper = newProduct(value, wt.clsName,
                                                        organism, dataSource, false, null);
                        } else if (prefix.equals("FB")) {
                            productWrapper = newProduct(value, wt.clsName, organism,
                                                        dataSource, true, "primaryIdentifier");
                        } else {
                            productWrapper = newProduct(value, wt.clsName,
                                                        organism, dataSource, true, null);
                        }
                        if (productWrapper != null) {
                            Item withProduct = productWrapper.getItem();
                            withProductList.add(withProduct);
                        }
                    } else {
                        LOG.debug("createWithObjects skipping a withType prefix:" + prefix);
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.error("createWithObjects broke with: " + withText);
            throw e;
        }
        return withProductList;
    }

    /**
     * Create a new geneProduct of a certain type (gene or protein) of a certain organism
     *
     * @param accession  the accession or actual identifier of the gene/protein (eg: FBgn0019981)
     * @param type       the type
     * @param organism the organism of the product, may be null if a protein
     * @param dataSource the id of the datasource the product is from.
     * @param createOrganism if true then reference the organism from created BioEntity
     * @param idField the attribute of created BioEntity to put identifier in
     * @return the geneProduct
     */
    protected ItemWrapper newProduct(String accession,
                                     String type,
                                     Item organism,
                                     Item dataSource,
                                     boolean createOrganism,
                                     String idField) {
        String clsName;
        // find gene attribute first to see if organism should be part of key
        if ("gene".equalsIgnoreCase(type)) {
            clsName = "Gene";
            String taxonId = organism.getAttribute("taxonId").getValue();
            if (idField == null) {
                idField = geneAttributes.get(taxonId);
                if (idField == null) {
                    throw new RuntimeException("Could not find a geneAttribute property for taxon: "
                                               + taxonId + " check properties file: " + PROP_FILE);
                }
            }

            // if a Dmel gene we need to use FlyBaseIdResolver to find a current id
            if (taxonId.equals("7227")) {
                IdResolver resolver = resolverFactory.getIdResolver();
                int resCount = resolver.countResolutions(taxonId, accession);
                if (resCount != 1) {
                    LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                             + accession + " count: " + resCount + " FBgn: "
                             + resolver.resolveId(taxonId, accession));
                    return null;
                }
                accession = resolver.resolveId(taxonId, accession).iterator().next();
            }
        } else if ("protein".equalsIgnoreCase(type)) {
            clsName = "Protein";
            idField = "primaryAccession";
        } else {
            throw new IllegalArgumentException("Unrecognised geneProduct type '" + type + "'");
        }

        boolean includeOrganism;
        if (idField.equals("primaryIdentifier") || type.equals("protein")) {
            includeOrganism = false;
        } else {
            includeOrganism = createOrganism;
        }
        String key = makeProductKey(accession, type, organism, includeOrganism);

        //Have we already seen this product somewhere before?
        // if so, return the product rather than creating a new one...
        if (productWrapperMap != null && productWrapperMap.containsKey(key)) {
            return productWrapperMap.get(key);
        }

        // if a Dmel gene we need to use FlyBaseIdResolver to find a current id

        Item product = createItem(clsName);
        if (organism != null && createOrganism) {
            product.setReference("organism", organism.getIdentifier());
        }
        product.setAttribute(idField, accession);

        // Record some evidence that says we got/matched the gene from GO data.
        String datasetId = dataSource.getCollection("dataSets").getRefIds().get(0);
        product.addToCollection("dataSets", datasetId);

        Item synonym = newSynonym(
                product.getIdentifier(),
                synonymTypes.get(type),
                accession,
                dataSource);

        ItemWrapper newProductWrapper = new ItemWrapper(key, product, synonym);
        productWrapperMap.put(key, newProductWrapper);

        return newProductWrapper;
    }

    private String makeProductKey(String identifier, String type, Item organism,
                                  boolean createOrganism) {

        if (type == null) {
            throw new IllegalArgumentException("No type provided when creating " + organism
                    + ": " + identifier);
        } else if (identifier == null) {
            throw new IllegalArgumentException("No identifier provided when creating "
                    + organism + ": " + type);
        }

        return identifier + type.toLowerCase() + ((createOrganism)
            ? organism.getIdentifier() : "");
    }

    private Item newGoTerm(String identifier, Item dataSource) throws ObjectStoreException {
        Item item = goTerms.get(identifier);
        if (item == null) {
            item = createItem("GOTerm");
            item.setAttribute("identifier", identifier);
            String datasetId = dataSource.getCollection("dataSets").getRefIds().get(0);
            item.setCollection("dataSets", new ArrayList(Collections.singleton(datasetId)));
            goTerms.put(identifier, item);
            store(item);
        }
        return item;
    }

    private String newGoEvidence(String code) throws ObjectStoreException {
        String refId = goEvidence.get(code);
        if (refId == null) {
            Item item = createItem("GOEvidenceCode");
            item.setAttribute("code", code);
            goEvidence.put(code, item.getIdentifier());
            store(item);
            refId = item.getIdentifier();
        }
        return refId;
    }

    private Item newDatasource(String code) throws ObjectStoreException {

        String title = null;
        // re-write some codes to better data source names
        if ("UniProtKB".equals(code)) {
            title = "UniProt";
        } else if ("FB".equals(code)) {
            title = "FlyBase";
        } else if ("WB".equals(code)) {
            title = "WormBase";
        } else if ("SP".equals(code)) {
            title = "UniProt";
        } else if (code.startsWith("GeneDB")) {
            title = "GeneDB";
        } else if ("SANGER".equals(code)) {
            title = "GeneDB";
        } else if ("GOA".equals(code)) {
            title = "Gene Ontology";
        } else if ("PINC".equals(code)) {
          title = "Proteome Inc.";
        } else if ("Pfam".equals(code)) {
            title = "PFAM"; // to merge with interpro
        } else {    // MGI, SGD
            title = code;
        }

        Item item = datasources.get(title);
        if (item == null) {
            item = createItem("DataSource");
            item.setAttribute("name", title);
            datasources.put(title, item);
            String key = "GO Annotation for " + title;
            String datasetId = newDataset(item.getIdentifier(), key);
            item.setCollection("dataSets", new ArrayList(Collections.singleton(datasetId)));
            store(item);
        }
        return item;
    }

    private String newDataset(String dataSourceRefId, String title)
    throws ObjectStoreException {
        String refId = datasets.get(title);
        if (refId == null) {
            Item item = createItem("DataSet");
            item.setAttribute("title", title);
            item.setReference("dataSource", dataSourceRefId);
            refId = item.getIdentifier();
            datasets.put(title, refId);
            store(item);
        }
        return refId;
    }


    private String newPublication(String codes) throws ObjectStoreException {
        String pubRefId = null;
        String[] array = codes.split("[|]");
        for (int i = 0; i < array.length; i++) {
            if (array[i].startsWith("PMID:")) {
                String pubMedId = array[i].substring(5);
                if (StringUtil.allDigits(pubMedId)) {
                    pubRefId = publications.get(pubMedId);
                    if (pubRefId == null) {
                        Item item = createItem("Publication");
                        item.setAttribute("pubMedId", pubMedId);
                        pubRefId = item.getIdentifier();
                        publications.put(pubMedId, pubRefId);
                        store(item);
                    }
                    break;
                }
            }
        }
        return pubRefId;
    }

    private Item newOrganism(String taxonId) throws ObjectStoreException {
        if (taxonId.equals("taxon:")) {
            throw new IllegalArgumentException("No taxon id supplied when creatin organism");
        }
        String taxonIdNew = taxonId.split(":")[1];
        if (taxonIdNew.contains("|")) {
            taxonIdNew = taxonIdNew.split("\\|")[0];
        }
        Item item = organisms.get(taxonIdNew);
        if (item == null) {
            item = createItem("Organism");
            item.setAttribute("taxonId", taxonIdNew);
            organisms.put(taxonIdNew, item);
            store(item);
        }
        return item;
    }

    // TODO set dataset
    private Item newSynonym(String subjectId, String type, String value, Item dataSource) {
        Item synonym = createItem("Synonym");
        synonym.setReference("subject", subjectId);
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("source", dataSource.getIdentifier());
        String datasetId = dataSource.getCollection("dataSets").getRefIds().get(0);
        synonym.setCollection("dataSets", new ArrayList(Collections.singleton(datasetId)));
        return synonym;
    }

    /**
     * Class that saves information for making GoAnnotationItem objects until the
     * entire file is processed.
     * <p/>
     * Was introduced to avoid storing the same item more than once...
     */
    class PlaceHolder
    {
        private String qualifier;
        private Item datasource;
        private ReferenceList pubs;
        private ReferenceList goEvidenceColl;
        private ItemWrapper geneProduct;
        private Item goTerm;
        private String withText;
        private Item organism;
        private Item goAnno;

        /**
         * @param qualifier   qualifier (eg NOT) or null
         * @param datasource  the datasource
         * @param publicationId the publication id
         * @param goEvidenceColl  collection of evidence objects for this go term
         * @param product     the product - typically a protein or a gene item
         * @param goTerm      the goTerm
         * @param withText    the 'with' column of gene_associationfile
         * @param organism    the current organism as an Item
         */
        public PlaceHolder(
                String qualifier, Item datasource, String publicationId,
                ReferenceList goEvidenceColl, ItemWrapper product, Item goTerm,
                String withText, Item organism) {
            this.qualifier = qualifier;
            this.datasource = datasource;
            pubs = new ReferenceList("publications", new ArrayList());
            pubs.addRefId(publicationId);
            this.goEvidenceColl = goEvidenceColl;
            this.geneProduct = product;
            this.goTerm = goTerm;
            this.withText = withText;
            this.organism = organism;
        }

        /**
         * @return the qualifier as a String
         */
        public String getQualifier() {
            return qualifier;
        }

        /**
         * @return the datasource item that represents we are sourcing the go annotations from.
         */
        public Item getDatasource() {
            return datasource;
        }

        /**
         * @return the Publication collection
         */
        public ReferenceList getPubs() {
            return pubs;
        }

        /**
         * @return the go evidence String
         */
        public ReferenceList getGoEvidenceColl() {
            return goEvidenceColl;
        }

        /**
         * @return an ItemWrapper that allows us to store the gene in a map to ensure it is only
         *         stored once - and after all the GoAnnotation items have been related to it.
         */
        public ItemWrapper getGeneProductWrapper() {
            return geneProduct;
        }

        /**
         * @return the go term that the go annotation object is linking to
         */
        public Item getGoTerm() {
            return goTerm;
        }

        /**
         * @return some more details
         */
        public String getWithText() {
            return withText;
        }

        /**
         * @return the current organism as an Item
         */
        public Item getOrganism() {
            return organism;
        }

        /**
         * Set the goAnno
         * @param goAnno the go Anno
         */
        public void setGoAnno(Item goAnno) {
            this.goAnno = goAnno;
        }

        /**
         * get the goAnno
         * @return the goAnno
         */
        public Item getGoAnno() {
            return goAnno;
        }

    }


    /**
     * Class to identify an Item using a unique key
     */
    class ItemWrapper
    {
        String key;
        Item item;
        Item synonym;

        /**
         * Constructor
         *
         * @param key  the key
         * @param item the Item
         * @param synonym the synonym
         */
        ItemWrapper(String key, Item item, Item synonym) {
            this.key = key;
            this.item = item;
            this.synonym = synonym;
        }

        /**
         * @return the key string that we will use in a map to id the object
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the item in question
         */
        public Item getItem() {
            return item;
        }

        /**
         * @return the synonym
         */
        public Item getSynonym() {
            return synonym;
        }
    }

    /**
     * Identify a GoTerm/geneProduct pair with qualifier
     * used to also use evidence code
     */
    class GoTermToGene
    {
        private String productId;
        private String goId;
        //private String code;
        private String qualifier;

        /**
         * Constructor
         *
         * @param productId gene/protein identifier
         * @param goId      GO term id
         * @param qualifier qualifier
         */
        GoTermToGene(String productId, String goId, String qualifier) {
            this.productId = productId;
            this.goId = goId;
            this.qualifier = qualifier;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o instanceof GoTermToGene) {
                GoTermToGene go = (GoTermToGene) o;
                return productId.equals(go.productId)
                        && goId.equals(go.goId)
                        && qualifier.equals(go.qualifier);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return ((3 * productId.hashCode())
                    + (5 * goId.hashCode())
                    + (7 * qualifier.hashCode()));
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            StringBuffer toStringBuff = new StringBuffer();

            toStringBuff.append("GoTermToGene - productId:");
            toStringBuff.append(productId);
            toStringBuff.append(" goId:");
            toStringBuff.append(goId);
            toStringBuff.append(" qualifier:");
            toStringBuff.append(qualifier);

            return toStringBuff.toString();
        }
    }

    /**
     * Class to hold information about a BioEntity item to create for a particular
     * identifier prefix in the gene_association 'with' column'.
     */
    class WithType
    {
        String clsName;
        String fieldName;

        /**
         * Constructor
         *
         * @param clsName   the classname
         * @param fieldName name of field to set
         */
        WithType(String clsName, String fieldName) {
            this.clsName = clsName;
            this.fieldName = fieldName;
        }
    }
}
