package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.bio.BioEntity;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;
import org.intermine.metadata.StringUtil;
import org.intermine.metadata.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse a go annotation file into Items.
 * *
 * @author Andrew Varley
 * @author Peter Mclaren - some additions to record the parents of a go term.
 * @author Julie Sullivan - updated to handle GAF 2.0
 * @author Xavier Watkins - refactored model
 */
public class GoConverter extends BioFileConverter
{
    protected static final String PROP_FILE = "go-annotation_config.properties";

    // configuration maps
    private Map<String, Config> configs = new HashMap<String, Config>();
    private static final Map<String, String> WITH_TYPES = new LinkedHashMap<String, String>();

    // maps retained across all files
    protected Map<String, String> goTerms = new LinkedHashMap<String, String>();
    private Map<String, String> evidenceCodes = new LinkedHashMap<String, String>();
    private Map<String, String> dataSets = new LinkedHashMap<String, String>();
    private Map<String, String> publications = new LinkedHashMap<String, String>();
    private Map<String, Item> organisms = new LinkedHashMap<String, Item>();
    protected Map<String, String> productMap = new LinkedHashMap<String, String>();
    private Set<String> dbRefs = new HashSet<String>();
    @SuppressWarnings("unused")
    private Map<String, String> databaseAbbreviations = new HashMap<String, String>();

    // maps renewed for each file
    private Map<GoTermToGene, Set<Evidence>> goTermGeneToEvidence
        = new LinkedHashMap<GoTermToGene, Set<Evidence>>();
    private Map<Integer, List<String>> productCollectionsMap;
    private Map<String, Integer> storedProductIds;

    // These should be altered for different ontologies:
    protected String termClassName = "GOTerm";
    protected String termCollectionName = "goAnnotation";
    protected String annotationClassName = "GOAnnotation";
    private String gaff = "2.0";
    private static final String DEFAULT_ANNOTATION_TYPE = "gene";
    private static final String DEFAULT_IDENTIFIER_FIELD = "primaryIdentifier";
    protected IdResolver rslv;
    private static Config defaultConfig = null;

    private static final Logger LOG = Logger.getLogger(GoConverter.class);

    /**
     * Constructor
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws Exception if an error occurs in storing or finding Model
     */
    public GoConverter(ItemWriter writer, Model model) throws Exception {
        super(writer, model);
        defaultConfig = new Config(DEFAULT_IDENTIFIER_FIELD, DEFAULT_IDENTIFIER_FIELD,
                DEFAULT_ANNOTATION_TYPE);
        readConfig();
    }

    /**
     * Sets the file format for the GAF.  2.0 is the default.
     *
     * @param gaff GO annotation file format
     */
    public void setGaff(String gaff) {
        this.gaff = gaff;
    }


    static {
        WITH_TYPES.put("FB", "Gene");
        WITH_TYPES.put("UniProt", "Protein");
    }

    // read config file that has specific settings for each organism, key is taxon id
    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }
        Enumeration<?> propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String key = (String) propNames.nextElement();
            String taxonId = key.substring(0, key.indexOf("."));
            Properties taxonProps = PropertiesUtil.stripStart(taxonId,
                    PropertiesUtil.getPropertiesStartingWith(taxonId, props));
            String identifier = taxonProps.getProperty("identifier");
            if (identifier == null) {
                throw new IllegalArgumentException("Unable to find geneAttribute property for "
                        + "taxon: " + taxonId + " in file: "
                        + PROP_FILE);
            }
            if (!("symbol".equals(identifier)
                    || "primaryIdentifier".equals(identifier)
                    || "secondaryIdentifier".equals(identifier)
                    || "primaryAccession".equals(identifier)
                    )) {
                throw new IllegalArgumentException("Invalid identifier value for taxon: "
                        + taxonId + " was: " + identifier);
            }

            String readColumn = taxonProps.getProperty("readColumn");
            if (readColumn != null) {
                readColumn = readColumn.trim();
                if (!("symbol".equals(readColumn) || "identifier".equals(readColumn))) {
                    throw new IllegalArgumentException("Invalid readColumn value for taxon: "
                            + taxonId + " was: " + readColumn);
                }
            }

            String annotationType = taxonProps.getProperty("typeAnnotated");
            if (annotationType == null) {
                LOG.info("Unable to find annotationType property for " + "taxon: " + taxonId
                        + " in file: " + PROP_FILE + ".  Creating genes by default.");
            }

            Config config = new Config(identifier, readColumn, annotationType);
            configs.put(taxonId, config);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws ObjectStoreException, IOException {

        // Create id resolver
        if (rslv == null) {
            rslv = IdResolverService.getIdResolverForMOD();
        }

        initialiseMapsForFile();

        BufferedReader br = new BufferedReader(reader);
        String line = null;

        // loop through entire file
        while ((line = br.readLine()) != null) {
            if (line.startsWith("!")) {
                continue;
            }
            String[] array = line.split("\t", -1); // keep trailing empty Strings
            if (array.length < 13) {
                throw new IllegalArgumentException("Not enough elements (should be > 13 not "
                        + array.length + ") in line: " + line);
            }

            String taxonId = parseTaxonId(array[12]);
            Config config = configs.get(taxonId);
            if (config == null) {
                config = defaultConfig;
                LOG.warn("No entry for organism with taxonId = '"
                        + taxonId + "' found in go-annotation config file.  Using default");
            }

            int readColumn = config.readColumn();
            String productId = array[readColumn];

            String goId = array[4];
            String qualifier = array[3];
            String strEvidence = array[6];
            String withText = array[7];
            String annotationExtension = null;
            if (array.length >= 16) {
                annotationExtension = array[15];
            }
            if (StringUtils.isNotEmpty(strEvidence)) {
                storeEvidenceCode(strEvidence);
            } else {
                throw new IllegalArgumentException("Evidence is a required column but not "
                        + "found for goterm " + goId + " and productId " + productId);
            }

            String type = config.annotationType;
            if ("1.0".equals(gaff)) {
                // type of gene product
                type = array[11];
            }

            // create unique key for go annotation
            GoTermToGene key = new GoTermToGene(productId, goId, qualifier, withText);

            String dataSourceCode = array[14]; // e.g. GDB, where uniprot collect the data from
            String dataSource = array[0]; // e.g. UniProtKB, where the goa file comes from
            Item organism = newOrganism(taxonId);
            String productIdentifier = newProduct(productId, type, organism,
                    dataSource, dataSourceCode, true, null);

            // null if resolver could not resolve an identifier
            if (productIdentifier != null) {

                // null if no pub found
                String pubRefId = newPublication(array[5]);

                // get evidence codes for this goterm|gene pair
                Set<Evidence> allEvidenceForAnnotation = goTermGeneToEvidence.get(key);

                // new evidence
                if (allEvidenceForAnnotation == null || !StringUtils.isEmpty(withText)) {
                    String goTermIdentifier = newGoTerm(goId, dataSource, dataSourceCode);
                    Evidence evidence = new Evidence(strEvidence, pubRefId, withText, organism,
                            dataSource, dataSourceCode);
                    allEvidenceForAnnotation = new LinkedHashSet<Evidence>();
                    allEvidenceForAnnotation.add(evidence);
                    goTermGeneToEvidence.put(key, allEvidenceForAnnotation);
                    Integer storedAnnotationId = createGoAnnotation(productIdentifier, type,
                            goTermIdentifier, organism, qualifier, dataSource, dataSourceCode,
                            annotationExtension);
                    evidence.setStoredAnnotationId(storedAnnotationId);
                } else {
                    boolean seenEvidenceCode = false;
                    Integer storedAnnotationId = null;

                    for (Evidence evidence : allEvidenceForAnnotation) {
                        String evidenceCode = evidence.getEvidenceCode();
                        storedAnnotationId = evidence.storedAnnotationId;
                        // already have evidence code, just add pub
                        if (evidenceCode.equals(strEvidence)) {
                            evidence.addPublicationRefId(pubRefId);
                            seenEvidenceCode = true;
                        }
                    }
                    if (!seenEvidenceCode) {
                        Evidence evidence = new Evidence(strEvidence, pubRefId, withText, organism,
                                dataSource, dataSourceCode);
                        evidence.storedAnnotationId = storedAnnotationId;
                        allEvidenceForAnnotation.add(evidence);
                    }
                }
            }
        }
        storeProductCollections();
        storeEvidence();
    }

    /**
     * Reset maps that don't need to retain their contents between files.
     */
    protected void initialiseMapsForFile() {
        goTermGeneToEvidence = new LinkedHashMap<GoTermToGene, Set<Evidence>>();
        productCollectionsMap = new LinkedHashMap<Integer, List<String>>();
        storedProductIds = new HashMap<String, Integer>();
    }

    private void storeProductCollections() throws ObjectStoreException {
        for (Map.Entry<Integer, List<String>> entry : productCollectionsMap.entrySet()) {
            Integer storedProductId = entry.getKey();
            List<String> annotationIds = entry.getValue();
            ReferenceList goAnnotation = new ReferenceList(termCollectionName, annotationIds);
            store(goAnnotation, storedProductId);
        }
    }

    private void storeEvidence() throws ObjectStoreException {
        for (Set<Evidence> annotationEvidence : goTermGeneToEvidence.values()) {
            List<String> evidenceRefIds = new ArrayList<String>();
            Integer goAnnotationRefId = null;
            for (Evidence evidence : annotationEvidence) {
                Item goevidence = createItem("GOEvidence");
                goevidence.setReference("code", evidenceCodes.get(evidence.getEvidenceCode()));
                List<String> publicationEvidence = evidence.getPublications();
                if (!publicationEvidence.isEmpty()) {
                    goevidence.setCollection("publications", publicationEvidence);
                }

                // with objects
                if (!StringUtils.isEmpty(evidence.withText)) {
                    goevidence.setAttribute("withText", evidence.withText);
                    List<String> with = createWithObjects(evidence.withText, evidence.organism,
                            evidence.dataSource, evidence.dataSourceCode);
                    if (!with.isEmpty()) {
                        goevidence.addCollection(new ReferenceList("with", with));
                    }
                }

                store(goevidence);
                evidenceRefIds.add(goevidence.getIdentifier());
                goAnnotationRefId = evidence.getStoredAnnotationId();
            }

            ReferenceList refIds = new ReferenceList("evidence",
                    new ArrayList<String>(evidenceRefIds));
            store(refIds, goAnnotationRefId);
        }
    }

    private Integer createGoAnnotation(String productIdentifier, String productType,
            String termIdentifier, Item organism, String qualifier, String dataSource,
            String dataSourceCode, String annotationExtension) throws ObjectStoreException {
        Item goAnnotation = createItem(annotationClassName);
        goAnnotation.setReference("subject", productIdentifier);
        goAnnotation.setReference("ontologyTerm", termIdentifier);

        if (!StringUtils.isEmpty(qualifier)) {
            goAnnotation.setAttribute("qualifier", qualifier);
        }
        if (!StringUtils.isEmpty(annotationExtension)) {
            goAnnotation.setAttribute("annotationExtension", annotationExtension);
        }

        goAnnotation.addToCollection("dataSets", getDataset(dataSource, dataSourceCode));
        if ("gene".equals(productType)) {
            addProductCollection(productIdentifier, goAnnotation.getIdentifier());
        }
        Integer storedAnnotationId = store(goAnnotation);
        return storedAnnotationId;
    }

    private void addProductCollection(String productIdentifier, String goAnnotationIdentifier) {
        Integer storedProductId = storedProductIds.get(productIdentifier);
        List<String> annotationIds = productCollectionsMap.get(storedProductId);
        if (annotationIds == null) {
            annotationIds = new ArrayList<String>();
            productCollectionsMap.put(storedProductId, annotationIds);
        }
        annotationIds.add(goAnnotationIdentifier);
    }

    /**
     * Given the 'with' text from a gene_association entry parse for recognised identifier
     * types and create Gene or Protein items accordingly.
     *
     * @param withText string from the gene_association entry
     * @param organism organism to reference
     * @param dataSource the name of goa file source
     * @param dataSourceCode short code to describe data source
     * @throws ObjectStoreException if problem when storing
     * @return a list of Items
     */
    protected List<String> createWithObjects(String withText, Item organism,
            String dataSource, String dataSourceCode) throws ObjectStoreException {

        List<String> withProductList = new ArrayList<String>();
        try {
            String[] elements = withText.split("[; |,]");
            for (int i = 0; i < elements.length; i++) {
                String entry = elements[i].trim();
                // rely on the format being type:identifier
                if (entry.indexOf(':') > 0) {
                    String prefix = entry.substring(0, entry.indexOf(':'));
                    String value = entry.substring(entry.indexOf(':') + 1);

                    if (WITH_TYPES.containsKey(prefix) && StringUtils.isNotEmpty(value)) {
                        String className = WITH_TYPES.get(prefix);
                        String productIdentifier = null;

                        // if a UniProt protein it may be from a different organism
                        // also FlyBase may be from a different Drosophila species
                        if ("UniProt".equals(prefix)) {
                            productIdentifier = newProduct(value, className,
                                    organism, dataSource, dataSourceCode,
                                    false, null);
                        } else if ("FB".equals(prefix)) {
                            // if organism is D. melanogaster then create with gene
                            // TODO could still be wrong as the FBgn could be a different species
                            if ("7227".equals(organism.getAttribute("taxonId").getValue())) {
                                productIdentifier = newProduct(value, className, organism,
                                        dataSource, dataSourceCode, true, "primaryIdentifier");
                            }
                        } else {
                            productIdentifier = newProduct(value, className, organism,
                                    dataSource, dataSourceCode, true, null);
                        }
                        if (productIdentifier != null) {
                            withProductList.add(productIdentifier);
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

    private String newProduct(String identifier, String type, Item organism,
            String dataSource, String dataSourceCode, boolean createOrganism,
            String field) throws ObjectStoreException {
        String idField = field;
        String accession = identifier;
        String clsName = null;
        // find gene attribute first to see if organism should be part of key
        if ("gene".equalsIgnoreCase(type)) {
            clsName = "Gene";
            String taxonId = organism.getAttribute("taxonId").getValue();
            if (idField == null) {
                Config config = configs.get(taxonId);
                if (config == null) {
                    config = defaultConfig;
                }
                idField = config.identifier;
                if (idField == null) {
                    throw new RuntimeException("Could not find a identifier property for taxon: "
                            + taxonId + " check properties file: " + PROP_FILE);
                }
            }

            if (rslv != null && rslv.hasTaxon(taxonId)) {
                if ("10116".equals(taxonId)) { // RGD doesn't have prefix in its annotation data
                    accession = "RGD:" + accession;
                }
                int resCount = rslv.countResolutions(taxonId, accession);

                if (resCount != 1) {
                    LOG.info("RESOLVER: failed to resolve gene to one identifier, "
                            + "ignoring gene: " + accession + " count: " + resCount + " ID: "
                            + rslv.resolveId(taxonId, accession));
                    return null;
                }
                accession = rslv.resolveId(taxonId, accession).iterator().next();
            }
        } else if ("protein".equalsIgnoreCase(type)) {
            // TODO use values in config
            clsName = "Protein";
            idField = "primaryAccession";
        } else {
            String typeCls = TypeUtil.javaiseClassName(type);

            if (getModel().getClassDescriptorByName(typeCls) != null) {
                Class<?> cls = getModel().getClassDescriptorByName(typeCls).getType();
                if (BioEntity.class.isAssignableFrom(cls)) {
                    clsName = typeCls;
                }
            }
            if (clsName == null) {
                throw new IllegalArgumentException("Unrecognised annotation type '" + type + "'");
            }
        }

        boolean includeOrganism;
        if ("primaryIdentifier".equals(idField) || "protein".equals(type)) {
            includeOrganism = false;
        } else {
            includeOrganism = createOrganism;
        }
        String key = makeProductKey(accession, type, organism, includeOrganism);

        //Have we already seen this product somewhere before?
        // if so, return the product rather than creating a new one...
        if (productMap.containsKey(key)) {
            return productMap.get(key);
        }

        // if a Dmel gene we need to use FlyBaseIdResolver to find a current id

        Item product = createItem(clsName);
        if (organism != null && createOrganism) {
            product.setReference("organism", organism.getIdentifier());
        }
        product.setAttribute(idField, accession);

        String dataSetIdentifier = getDataset(dataSource, dataSourceCode);
        product.addToCollection("dataSets", dataSetIdentifier);

        Integer storedProductId = store(product);
        storedProductIds.put(product.getIdentifier(), storedProductId);
        productMap.put(key, product.getIdentifier());
        return product.getIdentifier();
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

//    private String resolveTerm(String identifier) {
//        String goId = identifier;
//        if (rslv != null) {
//            int resCount = rslv.countResolutions("0", identifier);
//
//            if (resCount > 1) {
//                LOG.info("RESOLVER: failed to resolve ontology term to one identifier, "
//                        + "ignoring term: " + identifier + " count: " + resCount + " : "
//                        + rslv.resolveId("0", identifier));
//                return null;
//            }
//            if (resCount == 1) {
//                goId = rslv.resolveId("0", identifier).iterator().next();
//            }
//        }
//        return goId;
//    }

    private String newGoTerm(String identifier, String dataSource,
            String dataSourceCode) throws ObjectStoreException {
        if (identifier == null) {
            return null;
        }

        String goTermIdentifier = goTerms.get(identifier);
        if (goTermIdentifier == null) {
            Item item = createItem(termClassName);
            item.setAttribute("identifier", identifier);
            item.addToCollection("dataSets", getDataset(dataSource, dataSourceCode));
            store(item);

            goTermIdentifier = item.getIdentifier();
            goTerms.put(identifier, goTermIdentifier);
        }
        return goTermIdentifier;
    }

    private void storeEvidenceCode(String code) throws ObjectStoreException {
        if (evidenceCodes.get(code) == null) {
            Item item = createItem("GOEvidenceCode");
            item.setAttribute("code", code);
            evidenceCodes.put(code, item.getIdentifier());
            store(item);
        }
    }

    private String getDataSourceCodeName(String sourceCode) {
        String title = sourceCode;

        // re-write some codes to better data source names
        if ("UniProtKB".equalsIgnoreCase(sourceCode)) {
            title = "UniProt";
        } else if ("FB".equalsIgnoreCase(sourceCode)) {
            title = "FlyBase";
        } else if ("WB".equalsIgnoreCase(sourceCode)) {
            title = "WormBase";
        } else if ("SP".equalsIgnoreCase(sourceCode)) {
            title = "UniProt";
        } else if (sourceCode.startsWith("GeneDB")) {
            title = "GeneDB";
        } else if ("SANGER".equalsIgnoreCase(sourceCode)) {
            title = "GeneDB";
        } else if ("GOA".equalsIgnoreCase(sourceCode)) {
            title = "Gene Ontology";
        } else if ("PINC".equalsIgnoreCase(sourceCode)) {
            title = "Proteome Inc.";
        } else if ("Pfam".equalsIgnoreCase(sourceCode)) {
            title = "PFAM"; // to merge with interpro
        }
        return title;
    }

    private String getDataset(String dataSource, String code)
        throws ObjectStoreException {
        String dataSetIdentifier = dataSets.get(code);
        if (dataSetIdentifier == null) {
            String dataSourceName = getDataSourceCodeName(code);
            String title = "GO Annotation from " + dataSourceName;
            Item item = createItem("DataSet");
            item.setAttribute("name", title);
            item.setReference("dataSource", getDataSource(getDataSourceCodeName(dataSource)));
            dataSetIdentifier = item.getIdentifier();
            dataSets.put(code, dataSetIdentifier);
            store(item);
        }
        return dataSetIdentifier;
    }

    private String newPublication(String codes) throws ObjectStoreException {
        String pubRefId = null;
        String[] array = codes.split("[|]");
        Set<String> xrefs = new HashSet<String>();
        Item item = null;
        for (int i = 0; i < array.length; i++) {
            if (array[i].startsWith("PMID:")) {
                String pubMedId = array[i].substring(5);
                if (StringUtil.allDigits(pubMedId)) {
                    pubRefId = publications.get(pubMedId);
                    if (pubRefId == null) {
                        item = createItem("Publication");
                        item.setAttribute("pubMedId", pubMedId);
                        pubRefId = item.getIdentifier();
                        publications.put(pubMedId, pubRefId);

                    }
                }
            } else {
                xrefs.add(array[i]);
            }
        }
        ReferenceList refIds = new ReferenceList("crossReferences");

        // PMID may be first or last so we can't process xrefs until we've looked at all IDs
        if (StringUtils.isNotEmpty(pubRefId)) {
            for (String xref : xrefs) {
                refIds.addRefId(createDbReference(xref));
            }
        }
        if (item != null) {
            item.addCollection(refIds);
            store(item);
        }
        return pubRefId;
    }

    private String createDbReference(String value)
        throws ObjectStoreException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String dataSource = null;
        if (!dbRefs.contains(value)) {
            Item item = createItem("DatabaseReference");
            // FB:FBrf0055969
            if (value.contains(":")) {
                String[] bits = value.split(":");
                if (bits.length == 2) {
                    String db = bits[0];
                    dataSource = getDataSourceCodeName(db);
                    value = bits[1];
                }
            }
            item.setAttribute("identifier", value);
            if (StringUtils.isNotEmpty(dataSource)) {
                item.setReference("source", getDataSource(dataSource));
            }
            dbRefs.add(value);
            store(item);
            return item.getIdentifier();
        }
        return null;
    }

    private Item newOrganism(String taxonId) throws ObjectStoreException {
        Item item = organisms.get(taxonId);
        if (item == null) {
            item = createItem("Organism");
            item.setAttribute("taxonId", taxonId);
            organisms.put(taxonId, item);
            store(item);
        }
        return item;
    }

    private String parseTaxonId(String input) {
        if ("taxon:".equals(input)) {
            throw new IllegalArgumentException("Invalid taxon id read: " + input);
        }
        String taxonId = input.split(":")[1];
        if (taxonId.contains("|")) {
            taxonId = taxonId.split("\\|")[0];
        }
        return taxonId;
    }

    private class Evidence
    {
        private List<String> publicationRefIds = new ArrayList<String>();
        private String evidenceCode = null;
        private Integer storedAnnotationId = null;
        private String withText = null;
        private Item organism = null;
        private String dataSourceCode = null;
        private String dataSource = null;


        //dataSource, dataSourceCode

        protected Evidence(String evidenceCode, String publicationRefId, String withText,
                Item organism, String dataset, String datasource) {
            this.evidenceCode = evidenceCode;
            this.withText = withText;
            this.organism = organism;
            this.dataSourceCode = dataset;
            this.dataSource = datasource;
            addPublicationRefId(publicationRefId);
        }

        protected void addPublicationRefId(String publicationRefId) {
            if (publicationRefId != null) {
                publicationRefIds.add(publicationRefId);
            }
        }

        protected List<String> getPublications() {

            return publicationRefIds;
        }

        protected String getEvidenceCode() {
            return evidenceCode;
        }

        @SuppressWarnings("unused")
        protected String getWithText() {
            return withText;
        }

        @SuppressWarnings("unused")
        protected String getDataset() {
            return dataSourceCode;
        }

        @SuppressWarnings("unused")
        protected String getDatasource() {
            return dataSource;
        }

        @SuppressWarnings("unused")
        protected Item getOrganism() {
            return organism;
        }


        /**
         * @return the storedAnnotationId
         */
        protected Integer getStoredAnnotationId() {
            return storedAnnotationId;
        }

        /**
         * @param storedAnnotationId the storedAnnotationId to set
         */
        protected void setStoredAnnotationId(Integer storedAnnotationId) {
            this.storedAnnotationId = storedAnnotationId;
        }
    }


    /**
     * Identify a GoTerm/geneProduct pair with qualifier
     * used to also use evidence code
     */
    private class GoTermToGene
    {
        private String productId;
        private String goId;
        private String qualifier;
        private String withText;

        /**
         * Constructor
         *
         * @param productId gene/protein identifier
         * @param goId      GO term id
         * @param qualifier qualifier
         */
        GoTermToGene(String productId, String goId, String qualifier, String withText) {
            this.productId = productId;
            this.goId = goId;
            this.qualifier = qualifier;
            this.withText = withText;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof GoTermToGene) {
                GoTermToGene go = (GoTermToGene) o;
                return productId.equals(go.productId)
                        && goId.equals(go.goId)
                        && qualifier.equals(go.qualifier)
                        && withText.equals(go.withText);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return ((3 * productId.hashCode())
                    + (5 * goId.hashCode())
                    + (7 * qualifier.hashCode())
                    + (11 * withText.hashCode()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuffer toStringBuff = new StringBuffer();

            toStringBuff.append("GoTermToGene - productId:");
            toStringBuff.append(productId);
            toStringBuff.append(" goId:");
            toStringBuff.append(goId);
            toStringBuff.append(" qualifier:");
            toStringBuff.append(qualifier);
            toStringBuff.append(" withText:");
            toStringBuff.append(withText);
            return toStringBuff.toString();
        }
    }

    /**
     * Class to hold the config info for each taxonId.
     */
    private class Config
    {
        protected String annotationType;
        protected String identifier;
        protected String readColumn;

        /**
         * Constructor.
         *
         * @param annotationType type of object being annotated, gene or protein
         * @param identifier which identifier to set, primaryIdentifier or symbol
         * @param readColumn which identifier column to read, identifier or symbol
         */
        Config(String identifier, String readColumn, String annotationType) {
            this.annotationType = annotationType;
            this.identifier = identifier;
            this.readColumn = readColumn;
        }

        /**
         * @return 1 = use identifier column, 2 = use symbol column
         */
        protected int readColumn() {
            if (StringUtils.isNotEmpty(readColumn) && "symbol".equals(readColumn)) {
                return 2;
            }
            return 1;
        }
    }
}
