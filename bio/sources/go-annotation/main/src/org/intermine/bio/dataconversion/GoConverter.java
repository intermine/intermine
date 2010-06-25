package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
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
 * DataConverter to parse a go annotation file into Items
 *
 *
 * @author Andrew Varley
 * @author Peter Mclaren - some additions to record the parents of a go term.
 * @author Julie Sullivan - changed evidence to be collection
 * @author Xavier Watkins - refactored model
 */
public class GoConverter extends BioFileConverter
{
    protected static final String PROP_FILE = "go-annotation_config.properties";

    // configuration maps
    private Map<String, String> geneAttributes = new HashMap<String, String>();
    private Map<String, String> readColumns = new HashMap<String, String>();
    private Map<String, WithType> withTypes = new LinkedHashMap<String, WithType>();
    private Map<String, String> synonymTypes = new HashMap<String, String>();

    // maps retained across all files
    protected Map<String, String> goTerms = new LinkedHashMap<String, String>();
    private Map<String, String> goEvidence = new LinkedHashMap<String, String>();
    private Map<String, String> dataSets = new LinkedHashMap<String, String>();
    private Map<String, String> publications = new LinkedHashMap<String, String>();
    private Map<String, Item> organisms = new LinkedHashMap<String, Item>();
    protected Map<String, String> productMap = new LinkedHashMap<String, String>();

    // maps renewed for each file
    private Map<GoTermToGene, AssignmentEvidence> assignmentEvidenceMap =
        new LinkedHashMap<GoTermToGene, AssignmentEvidence>();
    private Map<Integer, List<String>> productCollectionsMap;
    private Map<String, Integer> storedProductIds;

    // These should be altered for different ontologies:
    protected String termClassName = "GOTerm";
    protected String termCollectionName = "goAnnotation";
    protected String annotationClassName = "GOAnnotation";

    protected IdResolverFactory flybaseResolverFactory;
    protected IdResolverFactory hgncResolverFactory;
    private Set<String> resolverFails = new HashSet<String>();

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
        addWithType("FB", "Gene", "primaryIdentifier");
        addWithType("UniProt", "Protein", "accession");
        synonymTypes.put("protein", "accession");
        synonymTypes.put("Protein", "accession");
        synonymTypes.put("gene", "identifier");
        synonymTypes.put("Gene", "identifier");

        // only construct factory here so can be replaced by mock factory in tests
        flybaseResolverFactory = new FlyBaseIdResolverFactory("gene");
        hgncResolverFactory = new HgncIdResolverFactory();

        readConfig();
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
            String geneAttribute = taxonProps.getProperty("geneAttribute").trim();
            if (geneAttribute == null) {
                throw new IllegalArgumentException("Unable to find geneAttribute property for "
                                                   + "taxon: " + taxonId + " in file: "
                                                   + PROP_FILE);
            }
            if (!(geneAttribute.equals("symbol")
                            || geneAttribute.equals("primaryIdentifier"))) {
                throw new IllegalArgumentException("Invalid geneAttribute value for taxon: "
                                                   + taxonId + " was: " + geneAttribute);
            }
            geneAttributes.put(taxonId, geneAttribute);

            String readColumn = taxonProps.getProperty("readColumn");
            if (readColumn != null) {
                readColumn = readColumn.trim();
                if (!(readColumn.equals("symbol") || readColumn.equals("identifier"))) {
                    throw new IllegalArgumentException("Invalid readColumn value for taxon: "
                            + taxonId + " was: " + readColumn);
                }
                readColumns.put(taxonId, readColumn);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
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

            // We only want to create GOAnnotation objects applied to genes and proteins
            // some file entries apply to type 'transcript' and possibly others
            if (!("gene".equalsIgnoreCase(array[11])
                    || "protein".equalsIgnoreCase(array[11]))) {
                LOG.info("Ignored line with type: " + array[11]);
                continue;
            }


            String taxonId = parseTaxonId(array[12]);
            int readColumn = 1;
            if (readColumns.containsKey(taxonId)) {
                if (readColumns.get(taxonId).equals("symbol")) {
                    readColumn = 2;
                }
            }
            String productId = array[readColumn];

            // Wormbase has some proteins with UniProt accessions and some with WB:WP ids,
            // hack here to get just the UniProt ones.
            if ("protein".equalsIgnoreCase(array[11]) && !array[0].startsWith("UniProt")) {
                continue;
            }

            String goId = array[4];
            String qualifier = array[3];
            String strEvidence = array[6];
            String withText = array[7];
            String evidenceId = null;
            if (strEvidence != null && !strEvidence.equals("")) {
                evidenceId = newGoEvidence(strEvidence);
            }
            String type = array[11];

            // create unique key for go annotation
            GoTermToGene key = new GoTermToGene(productId, goId, qualifier);

            String dataSourceCode = array[14];
            Item organism = newOrganism(taxonId);
            String productIdentifier = newProduct(productId, type, organism,
                    dataSourceCode, true, null);

            // null if resolver could not resolve an identifier
            if (productIdentifier != null) {
                AssignmentEvidence assignmentEvidence = assignmentEvidenceMap.get(key);
                if (assignmentEvidence == null) {
                    // this assignment has not been seen before
                    // get the rest of the data
                    String goTermIdentifier = newGoTerm(goId, dataSourceCode);

                    Integer storedAnnotationId = createGoAnnotation(productIdentifier, type,
                            goTermIdentifier, organism,
                            qualifier, withText, dataSourceCode);

                    assignmentEvidence = new AssignmentEvidence(storedAnnotationId);
                    assignmentEvidenceMap.put(key, assignmentEvidence);
                }

                // add evidence to new or existing assignment
                String newPublicationId = newPublication(array[5]);
                if (newPublicationId != null) {
                    assignmentEvidence.addPublicationIdentifier(newPublicationId);
                }

                if (strEvidence != null && !strEvidence.equals("")) {
                    evidenceId = newGoEvidence(strEvidence);
                }
                if (evidenceId != null) {
                    assignmentEvidence.addEvidenceCodeIdentifier(evidenceId);
                }
            }
        }
        storeProductCollections();
        storeAssignmentEvidence();
    }

    /**
     * Reset maps that don't need to retain their contents between files.
     */
    protected void initialiseMapsForFile() {
        assignmentEvidenceMap = new LinkedHashMap<GoTermToGene, AssignmentEvidence>();
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


    private void storeAssignmentEvidence() throws ObjectStoreException {
        for (AssignmentEvidence evidence : assignmentEvidenceMap.values()) {

            if (!evidence.publicationIdentifiers.isEmpty()) {
                ReferenceList publications = new ReferenceList("publications",
                        new ArrayList<String>(evidence.getPublicationIdentifiers()));
                store(publications, evidence.storedAnnotationId);
            }

            ReferenceList evidenceCodes = new ReferenceList("goEvidenceCodes",
                    new ArrayList<String>(evidence.getEvidenceCodeIdentifiers()));
            store(evidenceCodes, evidence.storedAnnotationId);
        }
    }

    private Integer createGoAnnotation(String productIdentifier, String productType,
            String termIdentifier, Item organism, String qualifier, String withText,
            String dataSourceCode)
        throws ObjectStoreException {
        Item goAnnotation = createItem(annotationClassName);
        goAnnotation.setReference("subject", productIdentifier);
        goAnnotation.setReference("ontologyTerm", termIdentifier);

        if (!StringUtils.isEmpty(qualifier)) {
            goAnnotation.setAttribute("qualifier", qualifier);
        }

        // with objects
        if (!StringUtils.isEmpty(withText)) {
            goAnnotation.setAttribute("withText", withText);
            List<String> with = createWithObjects(withText,
                                          organism,
                                          dataSourceCode);

            if (!with.isEmpty()) {
                goAnnotation.addCollection(new ReferenceList("with", with));
            }
        }

        goAnnotation.addToCollection("dataSets", getDataset(dataSourceCode));

        if (productType.equals("gene")) {
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

    private void addWithType(String prefix, String clsName, String fieldName) {
        withTypes.put(prefix, new WithType(clsName, fieldName));
    }


    /**
     * Given the 'with' text from a gene_association entry parse for recognised identifier
     * types and create Gene or Protein items accordingly.
     *
     * @param withText string from the gene_association entry
     * @param organism organism to reference
     * @param dataSourceCode short code to describe data source
     * @throws ObjectStoreException if problem when storing
     * @return a list of Items
     */
    protected List<String> createWithObjects(String withText, Item organism, String dataSourceCode)
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

                    if (withTypes.containsKey(prefix) && (value != null) && (!value.equals(""))) {
                        WithType wt = withTypes.get(prefix);
                        String productIdentifier = null;

                        // if a UniProt protein it may be from a different organism
                        // also FlyBase may be from a different Drosophila species
                        if (prefix.equals("UniProt")) {
                            productIdentifier = newProduct(value, wt.clsName,
                                                        organism, dataSourceCode, false, null);
                        } else if (prefix.equals("FB")) {
                            // if organism is D. melanogaster then create with gene
                            // TODO could still be wrong as the FBgn could be a different species
                            if (organism.getAttribute("taxonId").getValue().equals("7227")) {
                                productIdentifier = newProduct(value, wt.clsName, organism,
                                        dataSourceCode, true, "primaryIdentifier");
                            }
                        } else {
                            productIdentifier = newProduct(value, wt.clsName,
                                                        organism, dataSourceCode, true, null);
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


    private String newProduct(String identifier,
                                     String type,
                                     Item organism,
                                     String dataSourceCode,
                                     boolean createOrganism,
                                     String field) throws ObjectStoreException {
        String idField = field;
        String accession = identifier;
        String clsName = null;
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
            } else if (taxonId.equals("9606")) {
                IdResolver resolver = hgncResolverFactory.getIdResolver(true);
                if (resolver != null) {
                    int resCount = resolver.countResolutions(taxonId, accession);

                    if (resCount != 1) {
                        if (!resolverFails.contains(accession)) {
                            LOG.info("RESOLVER: HGNC failed to resolve gene to one identifier, "
                                    + "ignoring gene: " + accession + " count: " + resCount
                                    + " symbol: " + resolver.resolveId(taxonId, accession));
                            resolverFails.add(accession);
                        }
                        return null;
                    }
                    String previous = accession;
                    accession = resolver.resolveId(taxonId, accession).iterator().next();
                    if (!accession.equals(previous)) {
                        LOG.info("RESOLVER: HGNC successfully resolved: " + previous + " to: "
                                + accession);
                    }
                }
            }
        } else if ("protein".equalsIgnoreCase(type)) {
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
                throw new IllegalArgumentException("Unrecognised geneProduct type '" + type + "'");
            }
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
        if (productMap.containsKey(key)) {
            return productMap.get(key);
        }

        // if a Dmel gene we need to use FlyBaseIdResolver to find a current id

        Item product = createItem(clsName);
        if (organism != null && createOrganism) {
            product.setReference("organism", organism.getIdentifier());
        }
        product.setAttribute(idField, accession);

        String dataSetIdentifier = getDataset(dataSourceCode);
        product.addToCollection("dataSets", dataSetIdentifier);

        Integer storedProductId = store(product);
        storedProductIds.put(product.getIdentifier(), storedProductId);
        productMap.put(key, product.getIdentifier());

        Item synonym = newSynonym(
                product.getIdentifier(),
                synonymTypes.get(type),
                accession,
                dataSetIdentifier);
        store(synonym);

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

    private String newGoTerm(String goId, String dataSourceCode) throws ObjectStoreException {
        String goTermIdentifier = goTerms.get(goId);
        if (goTermIdentifier == null) {
            Item item = createItem(termClassName);
            item.setAttribute("identifier", goId);
            item.addToCollection("dataSets", getDataset(dataSourceCode));
            store(item);

            goTermIdentifier = item.getIdentifier();
            goTerms.put(goId, goTermIdentifier);
        }
        return goTermIdentifier;
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

    private String getDataSourceName(String sourceCode) {
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

    private String getDataset(String code)
        throws ObjectStoreException {
        String dataSetIdentifier = dataSets.get(code);
        if (dataSetIdentifier == null) {
            String dataSourceName = getDataSourceName(code);
            String title = "GO Annotation from " + dataSourceName;
            Item item = createItem("DataSet");
            item.setAttribute("name", title);
            item.setReference("dataSource", getDataSource(dataSourceName));
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
                    break;
                }
            }
        }
        return pubRefId;
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
        if (input.equals("taxon:")) {
            throw new IllegalArgumentException("Invalid taxon id read: " + input);
        }
        String taxonId = input.split(":")[1];
        if (taxonId.contains("|")) {
            taxonId = taxonId.split("\\|")[0];
        }
        return taxonId;
    }

    private Item newSynonym(String subjectId, String type, String value, String datasetId) {
        Item synonym = createItem("Synonym");
        synonym.setReference("subject", subjectId);
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.addToCollection("dataSets", datasetId);
        return synonym;
    }

    private class AssignmentEvidence
    {
        private Integer storedAnnotationId;
        private Set<String> publicationIdentifiers = new HashSet<String>();
        private Set<String> evidenceCodeIdentifiers = new HashSet<String>();

        public AssignmentEvidence(Integer storedAnnotationId) {
            this.storedAnnotationId = storedAnnotationId;
        }

        public void addEvidenceCodeIdentifier(String evidenceCodeIdentifier) {
            evidenceCodeIdentifiers.add(evidenceCodeIdentifier);
        }

        public void addPublicationIdentifier(String publicationIdentifier) {
            publicationIdentifiers.add(publicationIdentifier);
        }

        public Set<String> getPublicationIdentifiers() {
            return publicationIdentifiers;
        }

        public Set<String> getEvidenceCodeIdentifiers() {
            return evidenceCodeIdentifiers;
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
