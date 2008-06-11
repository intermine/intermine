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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse UniProt data into items
 * @author Richard Smith
 */
public class UniprotConverter extends FileConverter
{
    //TODO: This should come from props files!!!!
    protected static final String PROP_FILE = "uniprot_config.properties";
    private static final Logger LOG = Logger.getLogger(UniprotConverter.class);
    private Map<String, String> pubMaster = new HashMap<String, String>();
    private Map<String, String> orgMaster = new HashMap<String, String>();
    private Map<String, Item> dbMaster = new HashMap<String, Item>();
    private Map<String, Item> dsMaster = new HashMap<String, Item>();
    private Map<String, Item> ontoMaster = new HashMap<String, Item>();
    private Map<String, String> geneMaster = new LinkedHashMap<String, String>();
    private Map<String, Item> interproMaster = new HashMap<String, Item>();
    private Map<String, String> commentMaster = new HashMap<String, String>();
    private Set<String> geneIdentifiers = new LinkedHashSet<String>();

    // map of taxonId to object which determine which data to use for which organism
    private Map<String, UniProtGeneDataMap> geneDataMaps =
        new HashMap<String, UniProtGeneDataMap>();

    // datasources that designate gene names  e.g. WormBase, Ensembl
    private Set<String> geneSources = new HashSet<String>();
    private Map<String, Item> keyMaster = new HashMap<String, Item>();

    private boolean createInterpro = false;
    private Set<String> taxonIds = null;
    private Set<String> doneTaxonIds = new HashSet<String>();
    private boolean useSplitFiles = true;
    protected IdResolverFactory resolverFactory;
    private IdResolver resolver;
    private static final Set<String> FEATURE_TYPES = new HashSet<String>();

    static {
        FEATURE_TYPES.add("initiator methionine");    // VDAC_DROME
        FEATURE_TYPES.add("signal peptide");            // AMYA_DROME
        FEATURE_TYPES.add("propeptide");                // ACES_DROME
        FEATURE_TYPES.add("short sequence motif");       // A4_DROME
        FEATURE_TYPES.add("transit peptide");          // MMSA_DROME
        FEATURE_TYPES.add("chain");                      // ADCY2_DROME
        FEATURE_TYPES.add("peptide");                  // CCAP_DROME
        FEATURE_TYPES.add("topological domain");      // 5HT2A_DROME
        FEATURE_TYPES.add("transmembrane region");    // 5HT2A_DROME
        FEATURE_TYPES.add("active site");             // AMYA_DROME
        FEATURE_TYPES.add("metal ion-binding site");     // ADCY2_DROME
        FEATURE_TYPES.add("binding site");             // MMSA_DROME
        FEATURE_TYPES.add("site");                        // VDAC_DROME
        FEATURE_TYPES.add("modified residue");         // AMYA_DROME
        FEATURE_TYPES.add("lipid moiety-binding region"); // ACES_DROME
        FEATURE_TYPES.add("glycosylation site");      // 5HT2A_DROME
        FEATURE_TYPES.add("splice variant");           // zen
        FEATURE_TYPES.add("sequence variant");         // AMYA_DROME
        FEATURE_TYPES.add("unsure residue");            // none
    }

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public UniprotConverter(ItemWriter writer, Model model) {
        super(writer, model);

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        boolean doProcess = true;
        if (useSplitFiles) {
            doProcess = false;
            String fileName = getCurrentFile().getName();
            String taxonId = fileName.substring(0, fileName.indexOf("_"));
            if (taxonIds.contains(taxonId)) {
                doProcess = true;
                doneTaxonIds.add(taxonId);
            } else {
                System .out.println("Not reading from " + fileName
                                    + " - not in list of organisms.");
                LOG.error("Not reading from " + fileName + " - not in list of organisms.");
            }
        }

        if (doProcess) {
            readConfig();
            UniprotHandler handler = new UniprotHandler(getItemWriter());

            try {
                SAXParser.parse(new InputSource(reader), handler);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (useSplitFiles) {
            if (!doneTaxonIds.containsAll(taxonIds)) {
                throw new Exception("Did not process all required taxonIds. Required = " + taxonIds
                        + ", done = " + doneTaxonIds);
            }
        }
    }

    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }
        Enumeration<?> propNames = props.propertyNames();

        while (propNames.hasMoreElements()) {
            String code = (String) propNames.nextElement();
            code = code.substring(0, code.indexOf("."));
            Properties codeProps = PropertiesUtil.stripStart(code,
                PropertiesUtil.getPropertiesStartingWith(code, props));
            String taxonId = codeProps.getProperty("taxonid");
            if (taxonId == null) {
                throw new IllegalArgumentException("Unable to find 'taxonid' property for "
                                                 + "code: " + code + " in file: " + PROP_FILE);
            }
            taxonId = taxonId.trim();

            /* which variable to use as the uniqueGeneIdentifier */
            String attribute = codeProps.getProperty("attribute");
            if (attribute == null) {
                throw new IllegalArgumentException("Unable to find 'attribute' property for "
                                                 + "code: " + code + " in file: " + PROP_FILE);
            }

            attribute = attribute.trim();
            UniProtGeneDataMap geneDataMap = new UniProtGeneDataMap(attribute);

            /* Sources appear as source for gene name synonym and
             * are used for links out from the webapp. */
            String source = codeProps.getProperty("source");
            if (source != null) {
                source = source.trim();
                geneDataMap.setSource(source);
                geneSources.add(source);
            }

            /* which variable to use as the genePrimaryIdentifier */
            String primaryIdentifierSrcType = codeProps.getProperty("primaryIdentifierSrcType");
            if (primaryIdentifierSrcType != null) {
                primaryIdentifierSrcType = primaryIdentifierSrcType.trim();
                String primaryIdentifierSrc = codeProps.getProperty("primaryIdentifierSrc");
                if (primaryIdentifierSrc == null) {
                    throw new IllegalArgumentException("Unable to find 'primaryIdentifierSrc' "
                                                       + "property for code: " + code + " in file: "
                                                       + PROP_FILE);
                }
                primaryIdentifierSrc = primaryIdentifierSrc.trim();
                geneDataMap.setPrimaryIdentifier(primaryIdentifierSrcType, primaryIdentifierSrc);
                if (primaryIdentifierSrcType.equals("datasource")) {
                    geneSources.add(primaryIdentifierSrc);
                }
            }

            /* which variable to use as the geneIdentifier */
            String identifierSrcType = codeProps.getProperty("identifierSrcType");
            if (identifierSrcType != null) {
                identifierSrcType = identifierSrcType.trim();
                String identifierSrc = codeProps.getProperty("identifierSrc");
                if (identifierSrc == null) {
                    throw new IllegalArgumentException("Unable to find 'identifierSrc' property for"
                                                     + " code: " + code + " in file: " + PROP_FILE);
                }
                identifierSrc = identifierSrc.trim();
                geneDataMap.setIdentifier(identifierSrcType, identifierSrc);
                if (identifierSrcType.equals("datasource")) {
                    geneSources.add(identifierSrc);
                }
            }
            geneDataMaps.put(taxonId, geneDataMap);
        }
    }

    /**
     * Toggle whether or not to import interpro data
     * @param createinterpro String whether or not to import interpro data (true/false)
     */
    public void setCreateinterpro(String createinterpro) {
        if (createinterpro.equals("true")) {
            this.createInterpro = true;
        } else {
            this.createInterpro = false;
        }

    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setUniprotOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    /**
     * Sets the parameter that determines whether the files in the split directory will be read, or
     * the files in the root directory will be used.
     * @param useSplitFiles if true the files in /split will be loaded and if false the files in
     * the root directory will be loaded
     */
    public void setUseFilter(String useFilter) {
        if (useFilter.equals("true")) {
            this.useSplitFiles = false;
        } else {
            this.useSplitFiles = true;
        }
    }

    private class UniprotHandler extends DefaultHandler
    {
        // the below are reset for each protein
        private Item protein;
        private Item sequence;
        private String comment;
        private Item feature;
        private Item interpro;  // protein feature
        private Map<String, Item> synonyms;
        private Map<String, Item> genes;
        private StringBuffer descr;
        private String taxonId;
        private String dbName;
        private String evidence;
        private boolean hasPrimary;

        // maps genes for this protein to that gene's lists of names, identifiers, etc
        private Map<String, Map<String, String>> geneTOgeneNameTypeToName;
        private Map<String, Map<String, String>> geneTOgeneDesignations;

        // reset for each gene
        private Map<String, String> geneNameTypeToName; // ORF, primary, etc value for gene name
        private Set<String> geneNames;                  // list of names for this gene
        private Map<String, String> geneDesignations;        // gene names from each database
        private String possibleGeneIdSource = null; // ie FlyBase, Ensembl, etc.
        private String possibleGeneId = null;       // temp holder for gene identifier
                                             // until "gene designation" is verified on next line

        // master lists - only one is created
        private Item datasource;
        private Item dataset;

        private ItemWriter writer;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        private ArrayList<Item> delayedItems = new ArrayList<Item>();
        private boolean isProtein = false;

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public UniprotHandler(ItemWriter writer) {
            this.writer = writer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            attName = null;
            try {
                if (qName.equals("entry")) { // <entry>
                    if (attrs.getValue("dataset") != null) { // TODO only store swiss prot / trembl?
                        isProtein = true;
                        initProtein();
                        dataset = getDataSet(attrs.getValue("dataset"));
                    } else {
                        isProtein = false;
                    }
                }
                if (isProtein) {
                    // <entry><protein>
                    if (qName.equals("protein")) {
                        String isFragment = "false";
                        // check for <protein type="fragment*">
                        if (attrs.getValue("type") != null) {
                            String type = attrs.getValue("type");
                            if (type.startsWith("fragment")) {
                                isFragment = "true";
                            }
                        }
                        protein.setAttribute("isFragment", isFragment);
                        // <entry><protein><name>
                    } else if (qName.equals("name") && stack.peek().equals("protein")) {
                        attName = "name";
                        evidence = attrs.getValue("evidence");
                        // <entry><name>
                    } else if (qName.equals("name") && stack.peek().equals("entry")) {
                        attName = "primaryIdentifier";
                        // <entry><accession>
                    } else if (qName.equals("accession")) {
                        attName = "value";
                        // <entry><sequence>
                    } else if (qName.equals("sequence")) {
                        String strLength = attrs.getValue("length");
                        String strMass = attrs.getValue("mass");
                        if (strLength != null) {
                            sequence = createItem("Sequence");
                            sequence.setAttribute("length", strLength);
                            protein.setAttribute("length", strLength);
                            attName = "residues";
                        }
                        if (strMass != null) {
                            protein.setAttribute("molecularWeight", strMass);
                        }
                        // <entry><feature>
                    } else if (qName.equals("feature")
                                    && attrs.getValue("type") != null
                                    && FEATURE_TYPES.contains(attrs.getValue("type"))) {
                        String strType = attrs.getValue("type");
                        String strName = attrs.getValue("description");
                        String strStatus = null;
                        feature = createItem("UniProtFeature");
                        feature.addReference(new Reference("protein", protein.getIdentifier()));
                        if (!protein.hasCollection("features")) {
                            protein.addCollection(new ReferenceList("features",
                                                                    new ArrayList<String>()));
                        }
                        protein.getCollection("features").addRefId(feature.getIdentifier());
                        feature.setAttribute("type", strType);
                        Item keyword = getKeyword(strType);
                        feature.addReference(new Reference("feature", keyword.getIdentifier()));
                        if (attrs.getValue("status") != null) {
                            strStatus = attrs.getValue("status");
                            if (strName != null) {
                                strName += " (" + strStatus + ")";
                            } else {
                                strName = strStatus;
                            }
                        }
                        if (!StringUtils.isEmpty(strName)) {
                            feature.setAttribute("description", strName);
                        }
                        // <entry><feature><location><start||end>
                    } else if ((qName.equals("begin") || qName.equals("end")
                                    || qName.equals("position"))
                                    && stack.peek().equals("location")
                                    && attrs.getValue("position") != null && feature != null) {
                        if (qName.equals("begin") || qName.equals("end")) {
                            feature.setAttribute(qName, attrs.getValue("position"));
                        } else {
                            feature.setAttribute("begin", attrs.getValue("position"));
                            feature.setAttribute("end", attrs.getValue("position"));
                        }
                    // <entry><dbreference type="InterPro" >
                    } else if (createInterpro
                                    && qName.equals("dbReference")
                                    && attrs.getValue("type").equals("InterPro")) {
                        interpro = getInterpro(attrs.getValue("id").toString());
                        if (!protein.hasCollection("proteinDomains")) {
                            protein.addCollection(new ReferenceList("proteinDomains",
                                                                    new ArrayList<String>()));
                        }
                        protein.getCollection("proteinDomains").addRefId(interpro.getIdentifier());
                    // <entry><dbreference type="InterPro"><property type="entry name" value="***"/>
                    } else if (createInterpro
                                    && qName.equals("property")
                                    && attrs.getValue("type").equals("entry name")
                                    && stack.peek().equals("dbReference")) {
                        if (interpro != null) {
                            interpro.setAttribute("shortName", attrs.getValue("value").toString());
                            writer.store(ItemHelper.convert(interpro));
                            interpro = null;
                        }
                        // <entry><organism><dbreference>
                    } else if (qName.equals("dbReference") && stack.peek().equals("organism")) {
                        taxonId = attrs.getValue("id");
                        String refId = getOrganism(taxonId);
                        protein.setReference("organism", refId);
                        UniProtGeneDataMap geneDataMap = geneDataMaps.get(taxonId);
                        boolean noDatabase = false;
                        if (geneDataMap != null) {
                            dbName = geneDataMap.getSource();
                            if (dbName == null) {
                                noDatabase = true;
                            }
                        } else {    // there was no data in the config file
                            geneDataMap = new UniProtGeneDataMap("UniProt");
                            noDatabase = true;
                        }
                        if (noDatabase) {
                            geneDataMap.setSource("UniProt");
                            String message = "No gene source database defined for organism: "
                                + taxonId + ", using UniProt.[" + geneDataMap.toString()  + "]";
                            LOG.warn(message);
                            dbName = "UniProt";
                        }
                        // <entry><reference><citation><dbreference>
                    } else if (hasPrimary  && qName.equals("dbReference")
                                    && stack.peek().equals("citation")
                                    && attrs.getValue("type").equals("PubMed")) {
                        String pubId = getPub(attrs.getValue("id"));
                        if (!protein.hasCollection("publications")) {
                            protein.addCollection(new ReferenceList("publications",
                                                                    new ArrayList<String>()));
                        }
                        protein.getCollection("publications").addRefId(pubId);
                        // <entry><comment>
                    } else if (qName.equals("comment") && attrs.getValue("type") != null) {
                        comment = attrs.getValue("type");
                        // <entry><comment><text>
                    } else if (qName.equals("text") && stack.peek().equals("comment")) {
                        attName = "text";
                        // <entry><keyword>
                    } else if (qName.equals("keyword")) {
                        attName = "keyword";
                        // <entry><gene>
                    } else if (qName.equals("gene")) {
                        initGene();
                        // <entry><gene><name>
                    } else if (qName.equals("name") && stack.peek().equals("gene")) {
                        // will be primary, ORF, synonym or ordered locus
                        attName = attrs.getValue("type");
                        // <dbreference type="EC">
                    } else if (qName.equals("dbReference") && attrs.getValue("type").equals("EC")) {
                        String ecNumber = attrs.getValue("id");
                        if (ecNumber != null) {
                            protein.setAttribute("ecNumber", ecNumber);
                        }
                        // <dbreference type="FlyBase/UniProt/etc.." id="*" key="12345">
                    } else if (qName.equals("dbReference")
                                    && geneSources.contains(attrs.getValue("type"))) {
                        // could be identifier but check next tag to see if this is a gene desig
                        possibleGeneId = attrs.getValue("id");
                        possibleGeneIdSource = attrs.getValue("type");
                        //    <dbreference><property type="gene designation" value="*">
                    } else if (qName.equals("property") && stack.peek().equals("dbReference")
                                    && attrs.getValue("type").equals("gene designation")
                                    && geneNames.contains(attrs.getValue("value"))) {
                        /* for everyone but homo sapiens & honeybees */
                        if (possibleGeneIdSource != null && possibleGeneId != null) {
                            geneDesignations.put(possibleGeneIdSource, new String(possibleGeneId));
                        }
                        // <dbreference type="RefSeq">
                    } else if (qName.equals("dbReference")
                                    && attrs.getValue("type").equals("RefSeq")) {
                        String refSeqId = attrs.getValue("id");
                        if (refSeqId != null) {
                            refSeqId.trim();
                            Item syn = createSynonym(protein.getIdentifier(), "identifier",
                                                     refSeqId.trim(), datasource.getIdentifier());
                            if (syn != null) {
                                writer.store(ItemHelper.convert(syn));
                            }
                        }
                        //    <dbreference><property type="organism name" value="Homo sapiens"/>
                    } else if (qName.equals("property") && stack.peek().equals("dbReference")
                                    && attrs.getValue("type").equals("organism name")
                                    && (attrs.getValue("value").equals("Homo sapiens")
                                    || attrs.getValue("value").equals("Apis mellifera"))) {
                        if ((possibleGeneIdSource != null) && (possibleGeneId != null)) {
                            // we probably don't have a <gene> reference
                            initGene();
                            Item gene = createItem("Gene");
                            genes.put(gene.getIdentifier(), gene);
                            // associate gene with lists
                            geneTOgeneNameTypeToName.put(gene.getIdentifier(), geneNameTypeToName);
                            geneTOgeneDesignations.put(gene.getIdentifier(), geneDesignations);
                            geneDesignations.put(possibleGeneIdSource, new String(possibleGeneId));
                        }
                    }
                }
                if (qName.equals("uniprot")) {  // <uniprot>
                    initData();
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (length > 0) {
                    boolean whitespace = false;
                    switch(ch[start]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        whitespace = true;
                        break;
                    default:
                        break;
                    }
                    if (!whitespace) {
                        break;
                    }
                    ++start;
                    --length;
                }

                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    attValue.append(s);
                }
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);

            try {
                stack.pop();
                if (isProtein) {
                // <entry>
                if (qName.equals("entry")) {

                    // only store the protein if it has a primary accession value
                    if (hasPrimary) {
                        protein.setAttribute("description", descr.toString());
                        ReferenceList evidenceColl =
                            new ReferenceList("evidence", new ArrayList<String>());
                        protein.addCollection(evidenceColl);
                        evidenceColl.addRefId(dataset.getIdentifier());

                        // now that we know the taxonID, we can store the genes
                        if (hasPrimary && !genes.isEmpty()) {
                            Iterator<Item> i = genes.values().iterator();
                            while (i.hasNext()) {
                                Item gene = i.next();
                                finaliseGene(gene, protein.getReference("organism").getRefId());
                            }
                        }
                        // <entry><name> is a synonym
                        String proteinPrimaryIdentifier =
                            protein.getAttribute("primaryIdentifier").getValue();
                        Item syn = createSynonym(protein.getIdentifier(), "identifier",
                                                 proteinPrimaryIdentifier,
                                                 datasource.getIdentifier());
                        if (protein == null) {
                            throw new RuntimeException("Lost protein:" + proteinPrimaryIdentifier);
                        } else {
                            writer.store(ItemHelper.convert(protein));
                        }
                        if (syn != null) {
                            writer.store(ItemHelper.convert(syn));
                        }
                    } else {
                       LOG.info("Entry " + protein.getAttribute("name")
                                + " does not have any accessions");
                    }
                    for (Item item : delayedItems) {
                        writer.store(ItemHelper.convert(item));
                    }
                    delayedItems.clear();
                // <entry><sequence>
                } else if (hasPrimary && qName.equals("sequence")) {
                    if (attName != null) {
                        sequence.setAttribute(attName, attValue.toString().replaceAll("\n", ""));
                        protein.addReference(new Reference("sequence", sequence.getIdentifier()));
                        writer.store(ItemHelper.convert(sequence));
                    } else {
                        LOG.debug("Sequence for " + protein.getAttribute("name").getValue()
                                + " does not have a length");
                    }
                // <entry><protein><name>
                } else if (hasPrimary && qName.equals("name") && stack.peek().equals("protein")) {

                    String proteinName = attValue.toString();

                    if (!protein.hasAttribute("name")) {
                        protein.setAttribute(attName, proteinName);
                        descr.append(proteinName);
                    } else {
                        descr.append(" (" + proteinName + ")");
                    }

                    // all names are synonyms
                    if (evidence != null) {
                        proteinName += " (Evidence " + evidence + ")";
                    }
                    Item syn = createSynonym(protein.getIdentifier(), "name", proteinName,
                                  datasource.getIdentifier());
                    if (syn != null) {
                        delayedItems.add(syn);
                    }

                // <entry><comment><text>
                } else if (hasPrimary && qName.equals("text") && attName != null) {
                    String commentText = attValue.toString();
                    if (comment != null && commentText != null) {
                        String refId = getComment(comment, dataset.getIdentifier(), commentText);
                        if (!protein.hasCollection("comments")) {
                            protein.addCollection(new ReferenceList("comments",
                                                                    new ArrayList<String>()));
                        }
                        protein.getCollection("comments").addRefId(refId);
                    }

                // <entry><gene><name>
                } else if (qName.equals("name") && stack.peek().equals("gene")) {

                    String type = attName;
                    String name = attValue.toString();

                    // See #1199 - remove organism prefixes ("AgaP_" or "Dmel_")
                    name = name.replaceAll("^[A-Z][a-z][a-z][A-Za-z]_", "");

                    geneNames.add(new String(name));

                    // genes can have more than one synonym, so use name as key for map
                    if (!type.equals("synonym")) {
                        geneNameTypeToName.put(type, name);
                    } else {
                        geneNameTypeToName.put(name, name);
                    }

                // <entry><gene>
                } else if (qName.equals("gene")) {

                    Item gene = createItem("Gene");
                    genes.put(gene.getIdentifier(), gene);

                    // associate gene with lists
                    geneTOgeneNameTypeToName.put(gene.getIdentifier(), geneNameTypeToName);
                    geneTOgeneDesignations.put(gene.getIdentifier(), geneDesignations);

                // <entry><keyword>
                } else if (qName.equals("keyword")) {
                    if (attName != null) {
                        Item keyword = getKeyword(attValue.toString());
                        if (!protein.hasCollection("keywords")) {
                            protein.addCollection(new ReferenceList("keywords",
                                                                    new ArrayList<String>()));
                        }
                        protein.getCollection("keywords").addRefId(keyword.getIdentifier());
                    }
                // <entry><feature>
                } else if (qName.equals("feature") && feature != null) {

                    delayedItems.add(feature);
                    feature = null;

                // <entry><name>
                } else if (qName.equals("name")) {

                    if (attName != null) {
                        protein.setAttribute(attName, attValue.toString());
                    }

                // <entry><accession>
                } else if (qName.equals("accession") && !attValue.toString().equals("")) {

                    Item syn = createSynonym(protein.getIdentifier(), "accession",
                                           attValue.toString(), datasource.getIdentifier());
                    if (syn != null) {

                        // if this is the first accession value, its the primary accession
                        if (protein.getAttribute("primaryAccession") == null) {
                            protein.setAttribute("primaryAccession", attValue.toString());
                            hasPrimary = true;
                        }
                        if (hasPrimary) {
                            delayedItems.add(syn);
                        }
                    }
                }
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
       }

        private void initData()
        throws SAXException    {
            try {
                datasource = getDataSource("UniProt");
                setOnto("UniProtKeyword");
            } catch (Exception e) {
                throw new SAXException(e);
            }

        }

        // if synonym new, create and put in synonyms map for this <entry>
        private Item createSynonym(String subjectId, String type, String value, String dbId) {
            String key = subjectId + type + value + dbId;
            if (!synonyms.containsKey(key)) {
                Item syn = createItem("Synonym");
                syn.addReference(new Reference("subject", subjectId));
                syn.setAttribute("type", type);
                syn.setAttribute("value", value);
                syn.addReference(new Reference("source", dbId));
                synonyms.put(key, syn);
                return syn;
            } else {
                return null;
            }
        }


        // clears all protein-related lists/values
        // called when new protein is created
        private void initProtein() {

            protein = createItem("Protein");

            genes = new LinkedHashMap<String, Item>();
            synonyms = new LinkedHashMap<String, Item>();
            descr = new StringBuffer();
            taxonId = null;
            dbName = null;
            comment = null;
            feature = null;
            sequence = null;
            hasPrimary = false;
            possibleGeneIdSource = null;
            possibleGeneId = null;

            // maps gene to that gene's lists
            geneTOgeneNameTypeToName = new HashMap<String, Map<String, String>>();
            geneTOgeneDesignations = new HashMap<String, Map<String, String>>();
        }


        private void initGene() {

            // list of possible names for this gene
            geneNames = new LinkedHashSet<String>();
            // ORF, primary, etc name value for gene
            geneNameTypeToName = new LinkedHashMap<String, String>();
            // gene names from each database
            geneDesignations = new LinkedHashMap<String, String>();

        }

        private Item getDataSource(String title)
            throws SAXException {
            Item database = dbMaster.get(title);
            try {

                if (database == null) {
                    database = createItem("DataSource");
                    database.setAttribute("name", title);
                    dbMaster.put(title, database);
                    writer.store(ItemHelper.convert(database));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return database;
        }

        private Item getKeyword(String title)
        throws SAXException {
            Item keyword = keyMaster.get(title);
            try {

                if (keyword == null) {
                    keyword = createItem("OntologyTerm");
                    keyword.setAttribute("name", title);
                    Item ontology = ontoMaster.get("UniProtKeyword");
                    keyword.addReference(new Reference("ontology", ontology.getIdentifier()));
                    keyMaster.put(title, keyword);
                    writer.store(ItemHelper.convert(keyword));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return keyword;
        }

        private String getComment(String type, String source, String text)
        throws SAXException {
            String key = type + source + text;
            String refId = commentMaster.get(key);
            try {
                if (refId == null) {
                    Item item = createItem("Comment");
                    item.setAttribute("type", type);
                    item.setAttribute("text", text);
                    refId = item.getIdentifier();
                    commentMaster.put(key, refId);
                    writer.store(ItemHelper.convert(item));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return refId;
        }

        private Item getInterpro(String identifier) {
            Item item = interproMaster.get(identifier);
            if (item == null) {
                item = createItem("ProteinDomain");
                item.setAttribute("primaryIdentifier", identifier);
                interproMaster.put(identifier, item);
            }
            return item;
        }

        private String getOrganism(String orgId)
        throws SAXException {
            String refId = orgMaster.get(orgId);
            try {
                if (refId == null) {
                    Item item = createItem("Organism");
                    item.setAttribute("taxonId", orgId);
                    orgMaster.put(orgId, item.getIdentifier());
                    writer.store(ItemHelper.convert(item));
                    refId = item.getIdentifier();
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return refId;
        }


        private String getPub(String pubMedId)
        throws SAXException {
            String refId = pubMaster.get(pubMedId);
            try {
                if (refId == null) {
                    Item item = createItem("Publication");
                    item.setAttribute("pubMedId", pubMedId);
                    pubMaster.put(pubMedId, item.getIdentifier());
                    writer.store(ItemHelper.convert(item));
                    refId = item.getIdentifier();
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return refId;
        }

        private Item getDataSet(String title)
            throws SAXException {
            Item ds = dsMaster.get(title);
            try {

                if (ds == null) {
                    ds = createItem("DataSet");
                    ds.setAttribute("title", title + " data set");
                    ds.setReference("dataSource", datasource);
                    dsMaster.put(title, ds);

                    ReferenceList evidenceColl =
                        new ReferenceList("evidence", new ArrayList<String>());
                    protein.addCollection(evidenceColl);
                    evidenceColl.addRefId(ds.getIdentifier());
                    writer.store(ItemHelper.convert(ds));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return ds;
        }

        private Item setOnto(String title)
        throws SAXException {

            Item ontology = ontoMaster.get(title);
            try {
                if (ontology == null) {
                    ontology = createItem("Ontology");
                    ontology.setAttribute("title", title);
                    ontoMaster.put(title, ontology);
                    writer.store(ItemHelper.convert(ontology));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return ontology;
        }

        private void finaliseGene(Item gene, String orgId)
            throws SAXException {
            try {
                // Gene.identifier = <entry><gene><name type="ORF">
                String geneSecondaryIdentifier = null;
                // Gene.name = <entry><gene><name type="primary">
                String primaryGeneName = null;

                // get list for this gene
                Map<String, String> nameTypeToName =
                    geneTOgeneNameTypeToName.get(gene.getIdentifier());
                Map<String, String> designations =
                    geneTOgeneDesignations.get(gene.getIdentifier());

                // loop through each name for this gene
                String notCG = null;
                Iterator<String> i = nameTypeToName.keySet().iterator();
                while (i.hasNext()) {
                    String type = i.next();
                    String name = nameTypeToName.get(type);
                    if (type.equals("primary")) {
                        primaryGeneName = name;
                    } else if (type.equals("ORF")) {
                        if (taxonId.equals("7227") && !name.startsWith("CG")) {
                            notCG = name;
                        } else {
                            geneSecondaryIdentifier = name;
                        }
                    }
                }
                // Some UniProt entries have CGxxx as Dmel_CGxxx - need to strip prefix
                // so that they match identifiers from other sources.  Some genes have
                // embl identifiers and no FlyBase id, ignore these.
                if (geneSecondaryIdentifier == null && notCG != null) {
                    if (notCG.startsWith("Dmel_")) {
                        geneSecondaryIdentifier = notCG.substring(5);
                    } else {
                        LOG.info("Found a Drosophila gene without a CG identifer: " + notCG);
                    }
                }

                // define a gene identifier we always expect to find that is unique to this gene
                // is different for each organism
                String uniqueGeneIdentifier = null;
                // genePrimaryIdentifier = <entry><dbReference><type="FlyBase/WormBase/..">
                //            where designation = primary gene name
                String genePrimaryIdentifier = null;
                // use map to find out where to get ids
                UniProtGeneDataMap geneDataMap = geneDataMaps.get(taxonId);

                if (geneDataMap != null) {
                    /* set vars if they come from datasource or name */
                    genePrimaryIdentifier = setGeneVars(designations, nameTypeToName, null,
                                                 geneDataMap.getPrimaryIdentifierSrcType(),
                                                 geneDataMap.getPrimaryIdentifierSrc(),
                                                 genePrimaryIdentifier);

                    geneSecondaryIdentifier = setGeneVars(designations, nameTypeToName, null,
                                                   geneDataMap.getIdentifierSrcType(),
                                                   geneDataMap.getIdentifierSrc(),
                                                   geneSecondaryIdentifier);

                    /* set vars if they come from another variable */
                    Map<String, String> variableLookup = new LinkedHashMap<String, String>();
                    variableLookup.put("geneIdentifier", geneSecondaryIdentifier);
                    variableLookup.put("genePrimaryIdentifier", genePrimaryIdentifier);
                    variableLookup.put("primaryGeneName", primaryGeneName);

                    genePrimaryIdentifier = setGeneVars(null, null, variableLookup,
                                                   geneDataMap.getPrimaryIdentifierSrcType(),
                                                   geneDataMap.getPrimaryIdentifierSrc(),
                                                   genePrimaryIdentifier);

                    geneSecondaryIdentifier = setGeneVars(null, null, variableLookup,
                                                 geneDataMap.getIdentifierSrcType(),
                                                 geneDataMap.getIdentifierSrc(),
                                                 geneSecondaryIdentifier);

                    /* organism specific */
                    if (taxonId.equals("10116")) { // Rattus norvegicus
                        if (genePrimaryIdentifier != null
                            && !genePrimaryIdentifier.startsWith("RGD:")) {
                            genePrimaryIdentifier = "RGD:" + genePrimaryIdentifier;
                        }
                    } else if (taxonId.equals("3702")) { // Arabidopsis thaliana
                        if (genePrimaryIdentifier != null) {
                            genePrimaryIdentifier = genePrimaryIdentifier.toUpperCase();
                        }
                    }

                    variableLookup = new LinkedHashMap<String, String>();
                    variableLookup.put("geneIdentifier", geneSecondaryIdentifier);
                    variableLookup.put("genePrimaryIdentifier", genePrimaryIdentifier);
                    variableLookup.put("primaryGeneName", primaryGeneName);

                    uniqueGeneIdentifier = variableLookup.get(geneDataMap.getAttribute());
                    variableLookup = null;
                }

                if (taxonId.equals("7227")) {
                    uniqueGeneIdentifier = resolveGene(genePrimaryIdentifier,
                                                       geneSecondaryIdentifier, primaryGeneName);
                    genePrimaryIdentifier = uniqueGeneIdentifier;
                }

                // we don't want to store the CGs for dmel
                if (taxonId.equals("7227")) {
                    geneSecondaryIdentifier = null;
                }

                // uniprot data source has primary key of Gene.primaryIdentifier
                // only create gene if a value was found
                if (uniqueGeneIdentifier != null) {
                    String geneItemId = geneMaster.get(uniqueGeneIdentifier);

                    // UniProt sometimes has same identifier paired with two primaryIdentifiers
                    // causes problems merging other data sources.  Simple check to prevent
                    // creating a gene with a duplicate identifier.

                    if ((geneItemId == null) && geneIdentifiers.contains(geneSecondaryIdentifier)) {
                        LOG.warn("already created a gene for identifier: " + geneSecondaryIdentifier
                                 + " with different primaryIdentifier, discarding this one");
                    } else {
                        if (geneItemId == null) {
                            if (genePrimaryIdentifier != null) {
                                if (genePrimaryIdentifier.equals("")) {
                                    LOG.info("genePrimaryIdentifier was empty string");
                                } else {
                                    gene.setAttribute("primaryIdentifier", genePrimaryIdentifier);
                                    Item syn = null;
                                    if (!taxonId.equals("7227")) {
                                        syn = createSynonym(gene.getIdentifier(), "identifier",
                                                             genePrimaryIdentifier,
                                                             getDataSource(dbName).getIdentifier());
                                    }
                                    if (syn != null) {
                                        delayedItems.add(syn);
                                    }
                                }
                            }
                            if (geneSecondaryIdentifier != null) {
                                gene.setAttribute("secondaryIdentifier",
                                                      geneSecondaryIdentifier);


                                // don't create duplicate synonym
                                if (!geneSecondaryIdentifier.equals(genePrimaryIdentifier)
                                                && !geneSecondaryIdentifier.equals("")) {

                                    Item syn = createSynonym(gene.getIdentifier(), "identifier",
                                                             geneSecondaryIdentifier,
                                                             getDataSource(dbName).getIdentifier());
                                    if (syn != null) {
                                        delayedItems.add(syn);
                                    }
                                }
                                // keep a track of non-null gene identifiers
                                geneIdentifiers.add(geneSecondaryIdentifier);
                            }
                            // Problem with gene names for drosophila - ignore
                            if (primaryGeneName != null &&  !primaryGeneName.equals("")
                                            && !taxonId.equals("7227")) {
                                gene.setAttribute("symbol", primaryGeneName);
                            }
                            geneMaster.put(uniqueGeneIdentifier, gene.getIdentifier());
                            if (!protein.hasCollection("genes")) {
                                protein.addCollection(new ReferenceList("genes",
                                                                        new ArrayList<String>()));
                            }
                            protein.getCollection("genes").addRefId(gene.getIdentifier());
                            gene.setReference("organism", orgId);
                            writer.store(ItemHelper.convert(gene));
                            i = nameTypeToName.keySet().iterator();
                            while (i.hasNext() && !taxonId.equals("7227")) {
                                String synonymDescr = "";
                                String type = i.next();
                                String name = nameTypeToName.get(type);
                                if (type.equals("ordered locus")) {
                                    synonymDescr = "ordered locus";
                                } else {
                                    synonymDescr =  "symbol";
                                }
                                // all gene names are synonyms
                                // ORF is already identifer, so skip
                                // TODO if name is empty something has gone wrong
                                if (!type.equals("ORF") && !name.equals("")) {
                                    Item syn = createSynonym(gene.getIdentifier(), synonymDescr,
                                                             name,
                                                             getDataSource(dbName).getIdentifier());
                                    if (syn != null) {
                                        writer.store(ItemHelper.convert(syn));
                                    }
                                }
                            }
                        } else {
                            // this gene has already been stored and is attached to another protein
                            if (!protein.hasCollection("genes")) {
                                protein.addCollection(new ReferenceList("genes",
                                                                        new ArrayList<String>()));
                            }
                            protein.getCollection("genes").addRefId(geneItemId);
                        }
                    }
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private String setGeneVars(Map<String, String> designations,
                                   Map<String, String> nameTypeToName,
                                   Map<String, String> variableLookup,
                                   String srcType, String src, String var) {
            if (srcType == null) {
                return var;
            }
            if (srcType.equals("datasource") && designations != null) {
                return designations.get(src);
            } else if (srcType.equals("name") && nameTypeToName != null) {
                return nameTypeToName.get(src);
            } else if (srcType.equals("variable") && variableLookup != null) {
                if (src.equals("NULL")) {
                    return null;
                }
                return variableLookup.get(src);
            }
            return var;
        }

        /**
         * Convenience method for creating a new Item
         * @param className the name of the class
         * @return a new Item
         */
        protected Item createItem(String className) {
            return UniprotConverter.this.createItem(className);
        }

        private String resolveGene(String primaryIdentifier, String secondaryIdentifier,
                                   String name) {
            resolver = resolverFactory.getIdResolver();
            //            resolver = resolverFactory.getIdResolver(false);
//            // we aren't using a resolver so just return what we were given
//            if (resolver == null) {
//                return primaryIdentifier;
//            }
            String flyBaseLookUpId = null;
            if (primaryIdentifier != null) {
                flyBaseLookUpId = primaryIdentifier;
            } else if (secondaryIdentifier != null) {
                flyBaseLookUpId = secondaryIdentifier;
            } else if (name != null) {
                flyBaseLookUpId = name;
            } else {
                return null;
            }

            int resCount = resolver.countResolutions(taxonId, flyBaseLookUpId);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene"
                         + ": "
                         + flyBaseLookUpId + " count: " + resCount + " FBgn: "
                         + resolver.resolveId(taxonId, flyBaseLookUpId));
                return null;
            } else {
                return resolver.resolveId(taxonId, flyBaseLookUpId).iterator().next();
            }
        }
    }



        /**
         * Which datasource to use with which organism
         * @author Julie Sullivan
         */
        public static class UniProtGeneDataMap
        {
            private String attribute;
            private String source;
            private String primaryIdentifierSrc;
            private String primaryIdentifierSrcType;
            private String identifierSrcType;
            private String identifierSrc;

            /**
             * Constructor
             * @param attribute Which variable to use for the gene's unique identifier
             */
            public UniProtGeneDataMap(String attribute) {
                this.attribute = attribute;
            }

            /**
             * What to use as the uniqueGeneIdentifier
             * e.g. genePrimaryIdentifier or geneIdentifier
             * @return What to use as the uniqueGeneIdentifier
             */
            public String getAttribute () {
                return attribute;
            }

            /**
             * @param source Sources appear as source for gene name synonym and
             * are used for links out from the webapp
             */
            public void setSource (String source) {
                this.source = source;
            }

            /**
             * @return Sources appear as source for gene name synonym and are used for links out
             * from the webapp
             */
            public String getSource () {
                return source;
            }

            /**
             * @param primaryIdentifierSrcType What kind of source to use, e.g. variable,
             * datasource, or name
             * @param primaryIdentifierSrc What source to use, e.g. WormBase or ORF
             */
            public void  setPrimaryIdentifier(String primaryIdentifierSrcType,
                                              String primaryIdentifierSrc) {
                this.primaryIdentifierSrcType = primaryIdentifierSrcType;
                this.primaryIdentifierSrc = primaryIdentifierSrc;
            }

            /**
             * @return What type of source to use to set genePrimaryIdentifier,
             * e.g. variable, datasource, or name
             */
            public String getPrimaryIdentifierSrcType() {
                return primaryIdentifierSrcType;
            }

            /**
             * @return What source to use to set genePrimaryIdentifier, e.g. WormBase, ORF, etc
             */
            public String getPrimaryIdentifierSrc() {
                return primaryIdentifierSrc;
            }

            /**
             * @param identifierSrcType What kind of source to use, e.g. variable or datasource
             * @param identifierSrc Which source to use, e.g. genePrimaryIdentifier, Ensembl
             */
            public void  setIdentifier(String identifierSrcType, String identifierSrc) {
                this.identifierSrcType = identifierSrcType;
                this.identifierSrc = identifierSrc;
            }

            /**
             * @return What kind of source to use to set geneIdentifier, e.g. variable or datasource
             */
            public String getIdentifierSrcType() {
                return identifierSrcType;
            }

            /**
             * @return Which source to use to set geneIdentifier, e.g. genePrimaryIdentifier,
             * Ensembl
             */
            public String getIdentifierSrc() {
                return identifierSrc;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "attribute: " + attribute
                + ", source: " + source
                + ", primaryIdentifierSrcType: " + primaryIdentifierSrcType
                + ", primaryIdentifierSrc: " + primaryIdentifierSrc
                + ", identifierSrcType: " + identifierSrcType
                + ", identifierSrc: " + identifierSrc;

            }
        }

}
