package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
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
    protected IdResolverFactory flybaseResolverFactory, ontologyResolverFactory;
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

        // only construct factory here so can be replaced by mock factory in tests
        flybaseResolverFactory = new FlyBaseIdResolverFactory("gene");
        ontologyResolverFactory = new OntologyIdResolverFactory("GO");
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
            String taxonId = (String) propNames.nextElement();
            taxonId = taxonId.substring(0, taxonId.indexOf("."));

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

            // Wormbase has some proteins with UniProt accessions and some with WB:WP ids,
            // hack here to get just the UniProt ones.
            if (("protein".equalsIgnoreCase(type) && !array[0].startsWith("UniProt"))
                    || (!"protein".equalsIgnoreCase(type) && array[0].startsWith("UniProt"))) {
                continue;
            }

            // create unique key for go annotation
            GoTermToGene key = new GoTermToGene(productId, goId, qualifier);

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
                if (allEvidenceForAnnotation == null) {
                    String goTermIdentifier = newGoTerm(goId, dataSource, dataSourceCode);
                    Evidence evidence = new Evidence(strEvidence, pubRefId);
                    allEvidenceForAnnotation = new LinkedHashSet<Evidence>();
                    allEvidenceForAnnotation.add(evidence);
                    goTermGeneToEvidence.put(key, allEvidenceForAnnotation);
                    Integer storedAnnotationId = createGoAnnotation(
                            productIdentifier, type, goTermIdentifier,
                            organism, qualifier, withText, dataSource,
                            dataSourceCode, annotationExtension);
                    evidence.setStoredAnnotationId(storedAnnotationId);
                } else {
                    boolean seenEvidenceCode = false;
                    Integer storedAnnotationId = null;
                    for (Evidence evidence : allEvidenceForAnnotation) {
                        String evidenceCode = evidence.getEvidenceCode();
                        // already have evidence code, just add pub
                        if (evidenceCode.equals(strEvidence)) {
                            evidence.addPublicationRefId(pubRefId);
                            seenEvidenceCode = true;
                        }
                        storedAnnotationId = evidence.storedAnnotationId;
                    }
                    if (!seenEvidenceCode) {
                        Evidence evidence = new Evidence(strEvidence, pubRefId);
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
            String termIdentifier, Item organism, String qualifier, String withText,
            String dataSource, String dataSourceCode, String annotationExtension)
        throws ObjectStoreException {
        Item goAnnotation = createItem(annotationClassName);
        goAnnotation.setReference("subject", productIdentifier);
        goAnnotation.setReference("ontologyTerm", termIdentifier);

        if (!StringUtils.isEmpty(qualifier)) {
            goAnnotation.setAttribute("qualifier", qualifier);
        }
        if (!StringUtils.isEmpty(annotationExtension)) {
            goAnnotation.setAttribute("annotationExtension", annotationExtension);
        }
        // with objects
        if (!StringUtils.isEmpty(withText)) {
            goAnnotation.setAttribute("withText", withText);
            List<String> with = createWithObjects(withText, organism, dataSource, dataSourceCode);
            if (!with.isEmpty()) {
                goAnnotation.addCollection(new ReferenceList("with", with));
            }
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
            String dataSource, String dataSourceCode)
        throws ObjectStoreException {

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

            // if a Dmel gene we need to use FlyBaseIdResolver to find a current id
            if ("7227".equals(taxonId)) {
                IdResolver resolver = flybaseResolverFactory.getIdResolver(false);
                if (resolver != null) {
                    int resCount = resolver.countResolutions(taxonId, accession);

                    if (resCount != 1) {
                        LOG.info("RESOLVER: failed to resolve gene to one identifier, "
                                 + "ignoring gene: " + accession + " count: " + resCount + " FBgn: "
                                 + resolver.resolveId(taxonId, accession));
                        return null;
                    }
                    accession = resolver.resolveId(taxonId, accession).iterator().next();
                }
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

    private String resolveTerm(String identifier) {
        String goId = identifier;
        IdResolver resolver = ontologyResolverFactory.getIdResolver(false);
        if (resolver != null) {
            int resCount = resolver.countResolutions("0", identifier);

            if (resCount > 1) {
                LOG.info("RESOLVER: failed to resolve ontology term to one identifier, "
                         + "ignoring term: " + identifier + " count: " + resCount + " : "
                         + resolver.resolveId("0", identifier));
                return null;
            }
            if (resCount == 1) {
                goId = resolver.resolveId("0", identifier).iterator().next();
            }
        }
        return goId;
    }

    private String newGoTerm(String identifier, String dataSource,
            String dataSourceCode) throws ObjectStoreException {

        String goId = resolveTerm(identifier);

        if (goId == null) {
            return null;
        }

        String goTermIdentifier = goTerms.get(goId);
        if (goTermIdentifier == null) {
            Item item = createItem(termClassName);
            item.setAttribute("identifier", goId);
            item.addToCollection("dataSets", getDataset(dataSource, dataSourceCode));
            store(item);

            goTermIdentifier = item.getIdentifier();
            goTerms.put(goId, goTermIdentifier);
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
        if ("UniProtKB".equals(sourceCode)) {
            title = "UniProt";
        } else if ("FB".equals(sourceCode)) {
            title = "FlyBase";
        } else if ("WB".equals(sourceCode)) {
            title = "WormBase";
        } else if ("SP".equals(sourceCode)) {
            title = "UniProt";
        } else if (sourceCode.startsWith("GeneDB")) {
            title = "GeneDB";
        } else if ("SANGER".equals(sourceCode)) {
            title = "GeneDB";
        } else if ("GOA".equals(sourceCode)) {
            title = "Gene Ontology";
        } else if ("PINC".equals(sourceCode)) {
            title = "Proteome Inc.";
        } else if ("Pfam".equals(sourceCode)) {
            title = "PFAM"; // to merge with interpro
        }
        return title;
    }

    private String getDataSourceName(String dataSource) {
        if ("UniProtKB".equals(dataSource)) {
            return "UniProt";
        } else if ("FB".equals(dataSource)) {
            return "FlyBase";
        }
        return dataSource;
    }

    private String getDataset(String dataSource, String code)
        throws ObjectStoreException {
        String dataSetIdentifier = dataSets.get(code);
        if (dataSetIdentifier == null) {
            String dataSourceName = getDataSourceCodeName(code);
            String title = "GO Annotation from " + dataSourceName;
            Item item = createItem("DataSet");
            item.setAttribute("name", title);
            item.setReference("dataSource", getDataSource(getDataSourceName(dataSource)));
            dataSetIdentifier = item.getIdentifier();
            dataSets.put(code, dataSetIdentifier);
            store(item);
        }
        return dataSetIdentifier;
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
                    return pubRefId;
                }
            }
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

        public Evidence(String evidenceCode, String publicationRefId) {
            this.evidenceCode = evidenceCode;
            addPublicationRefId(publicationRefId);
        }

        public void addPublicationRefId(String publicationRefId) {
            if (publicationRefId != null) {
                publicationRefIds.add(publicationRefId);
            }
        }

        public List<String> getPublications() {
            return publicationRefIds;
        }

        public String getEvidenceCode() {
            return evidenceCode;
        }

        /**
         * @return the storedAnnotationId
         */
        public Integer getStoredAnnotationId() {
            return storedAnnotationId;
        }

        /**
         * @param storedAnnotationId the storedAnnotationId to set
         */
        public void setStoredAnnotationId(Integer storedAnnotationId) {
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
        @Override
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
        @Override
        public int hashCode() {
            return ((3 * productId.hashCode())
                    + (5 * goId.hashCode())
                    + (7 * qualifier.hashCode()));
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
