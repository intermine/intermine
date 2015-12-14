package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * DataConverter to parse UniProt data into items.  Improved version of UniProtConverter.
 *
 * Differs from UniProtConverter in that this Converter creates proper protein items.
 * UniProtConverter creates protein objects, that are really uniprot entries.
 * @author Julie Sullivan
 */
public class UniprotConverter extends BioDirectoryConverter
{
    private static final UniprotConfig CONFIG = new UniprotConfig();
    private static final Logger LOG = Logger.getLogger(UniprotConverter.class);
    private Map<String, String> pubs = new HashMap<String, String>();
    private Set<Item> synonymsAndXrefs = new HashSet<Item>();
    // taxonId -> [md5Checksum -> stored protein identifier]
    private Map<String, Map<String, String>> sequences = new HashMap<String, Map<String, String>>();
    // md5Checksum -> sequence item identifier  (ensure all sequences are unique across organisms)
    private Map<String, String> allSequences = new HashMap<String, String>();
    private Map<String, String> ontologies = new HashMap<String, String>();
    private Map<String, String> keywords = new HashMap<String, String>();
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> goterms = new HashMap<String, String>();
    private Map<String, String> goEvidenceCodes = new HashMap<String, String>();
    private Map<String, String> ecNumbers = new HashMap<String, String>();
    private Map<String, String> proteins = new HashMap<String, String>();
    private static final int POSTGRES_INDEX_SIZE = 2712;

    // don't allow duplicate identifiers
    private Set<String> identifiers = null;

    private boolean creatego = false;
    private boolean loadfragments = false;
    private boolean allowduplicates = false;
    private boolean loadtrembl = true;
    private Set<String> taxonIds = null;

    protected IdResolver rslv;
    private static final String FLY = "7227";
    private String datasourceRefId = null;
    private static final Map<String, String> GENE_PREFIXES = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public UniprotConverter(ItemWriter writer, Model model) {
        super(writer, model, "UniProt", "Swiss-Prot data set");
        OrganismRepository.getOrganismRepository();
    }

    static {
        GENE_PREFIXES.put("10116", "RGD:");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(File dataDir) throws Exception {

        try {
            datasourceRefId = getDataSource("UniProt");
            setOntology("UniProtKeyword");
        } catch (SAXException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Map<String, File[]> taxonIdToFiles = parseFileNames(dataDir.listFiles());

        // init id resolver
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }

        if (taxonIds != null) {
            for (String taxonId : taxonIds) {
                if (taxonIdToFiles.get(taxonId) == null) {
                    LOG.error("no files found for " + taxonId);
                }
                processFiles(taxonIdToFiles.get(taxonId));
            }
        } else {
            // if files aren't in taxonId_sprot|trembl format, assume they are in uniprot_sprot.xml
            // format
            File[] files = dataDir.listFiles();
            File[] sortedFiles = new File[2];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String filename = file.getName();
                // process sprot, then trembl
                if ("uniprot_sprot.xml".equals(filename)) {
                    sortedFiles[0] = file;
                } else if ("uniprot_trembl.xml".equals(filename)) {
                    if (loadtrembl) {
                        sortedFiles[1] = file;
                    }
                }
            }
            processFiles(sortedFiles);
        }
    }

    // process the sprot file, then the trembl file
    private void processFiles(File[] files) {
        if (files == null) {
            LOG.error("no data files found ");
            return;
        }
        for (int i = 0; i <= 1; i++) {
            File file = files[i];
            if (file == null) {
                continue;
            }
            UniprotHandler handler = new UniprotHandler();
            try {
                System .out.println("Processing file: " + file.getPath());
                Reader reader = new FileReader(file);
                SAXParser.parse(new InputSource(reader), handler);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        // reset all variables here, new organism
        sequences = new HashMap<String, Map<String, String>>();
        genes = new HashMap<String, String>();
        proteins = new HashMap<String, String>();
    }

    /**
     * sprot data files need to be processed immediately before trembl ones
     * not all organisms are going to have both files
     *
     * UniProtFilterTask has already been run so all files are assumed to be valid.
     *
     *  expected syntax : 7227_uniprot_sprot.xml
     *  [TAXONID]_uniprot_[SOURCE].xml
     *  SOURCE: sprot or trembl
     *
     * @param fileList list of files to parse
     * @return list of files ordered to that SwissProt files are parsed first
     */
    protected Map<String, File[]> parseFileNames(File[] fileList) {
        Map<String, File[]> files = new HashMap<String, File[]>();
        if (fileList == null) {
            return null;
        }
        for (File file : fileList) {
            String[] bits = file.getName().split("_");
            String taxonId = bits[0];
            if (bits.length != 3) {
                LOG.info("Bad file found:  "  + file.getName()
                        + ", expected a filename like 7227_uniprot_sprot.xml");
                continue;
            }
            String source = bits[2].replace(".xml", "");
            // process trembl last because trembl has duplicates of sprot proteins
            if (!"sprot".equals(source) && !"trembl".equals(source)) {
                LOG.info("Bad file found:  "  + file.getName()
                        +  " (" + bits[2] + "), expecting sprot or trembl ");
                continue;
            }

            if (!loadtrembl && "trembl".equals(source)) {
                continue;
            }

            int i = ("sprot".equals(source) ? 0 : 1);
            if (!files.containsKey(taxonId)) {
                File[] sourceFiles = new File[2];
                sourceFiles[i] = file;
                files.put(taxonId, sourceFiles);
            } else {
                File[] fileArray = files.get(taxonId);
                fileArray[i] = file;
            }
        }
        return files;
    }

    /**
     * Toggle whether or not to import interpro data
     * @param createinterpro String whether or not to import interpro data (true/false)
     * @deprecated The UniProt data source does not create interpro domains any longer. Please
     * use the InterPro data source instead.
     */
    @Deprecated
    public void setCreateinterpro(String createinterpro) {
        final String msg = "UniProt data source does not create protein domains any longer. "
                + "Please use the InterPro data source instead.";
        throw new IllegalArgumentException(msg);
    }

    /**
     * Toggle whether or not to import GO data
     * @param creatego whether or not to import GO terms (true/false)
     */
    public void setCreatego(String creatego) {
        if ("true".equalsIgnoreCase(creatego)) {
            this.creatego = true;
        } else {
            this.creatego = false;
        }
    }

    /**
     * Toggle whether or not to load trembl data for all given organisms
     * @param loadtrembl whether or not to load trembl data
     */
    public void setLoadtrembl(String loadtrembl) {
        if ("true".equalsIgnoreCase(loadtrembl)) {
            this.loadtrembl = true;
        } else {
            this.loadtrembl = false;
        }
    }

    /**
     * Toggle whether or not to allow duplicate sequences
     * @param allowduplicates whether or not to allow duplicate sequences
     */
    public void setAllowduplicates(String allowduplicates) {
        if ("true".equalsIgnoreCase(allowduplicates)) {
            this.allowduplicates = true;
        } else {
            this.allowduplicates = false;
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
        addStrains();
    }

    /**
     * Toggle whether or not to load fragments.  default to false.
     *
     * @param loadfragments if true, will load all proteins even if isFragment = true
     */
    public void setLoadfragments(String loadfragments) {
        if ("true".equalsIgnoreCase(loadfragments)) {
            this.loadfragments = true;
        } else {
            this.loadfragments = false;
        }
    }

    private void addStrains() {
        Set<String> originalTaxonIds = new HashSet<String>(taxonIds);
        for (String taxonId : originalTaxonIds) {
            String strain = CONFIG.getStrain(taxonId);
            if (StringUtils.isNotEmpty(strain)) {
                taxonIds.add(strain);
            }
        }
    }

    /* converts the XML into UniProt entry objects.  run once per file */
    private class UniprotHandler extends DefaultHandler
    {
        private UniprotEntry entry;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        private int entryCount = 0;
        private DiseaseHolder disease = null;

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {

            String previousQName = null;
            if (!stack.isEmpty()) {
                previousQName = stack.peek();
            }
            attName = null;
            if ("entry".equals(qName)) {
                entry = new UniprotEntry();
                String dataSetTitle = getAttrValue(attrs, "dataset") + " data set";
                entry.setDatasetRefId(getDataSet(dataSetTitle, datasourceRefId));
            } else if ("fullName".equals(qName) && stack.search("protein") == 2
                    &&  ("recommendedName".equals(previousQName)
                            || "submittedName".equals(previousQName))) {
                attName = "proteinName";
            } else if (("fullName".equals(qName) || "shortName".equals(qName))
                    && stack.search("protein") == 2
                    && ("alternativeName".equals(previousQName)
                            || "recommendedName".equals(previousQName)
                            || "submittedName".equals(previousQName))) {
                attName = "synonym";
            } else if ("fullName".equals(qName)
                    && "recommendedName".equals(previousQName)
                    && stack.search("component") == 2) {
                attName = "component";
            } else if ("name".equals(qName) && "entry".equals(previousQName)) {
                attName = "primaryIdentifier";
            } else if ("ecNumber".equals(qName)) {
                attName = "ecNumber";
            } else if ("accession".equals(qName)) {
                attName = "value";
            } else if ("dbReference".equals(qName) && "organism".equals(previousQName)) {
                entry.setTaxonId(parseTaxonId(getAttrValue(attrs, "id")));
            } else if ("id".equals(qName)  && "isoform".equals(previousQName)) {
                // TODO only use the first isoform
                // how does xml parser work for multiple isoforms?
                attName = "isoform";
            } else if ("sequence".equals(qName)  && "isoform".equals(previousQName)) {
                String sequenceType = getAttrValue(attrs, "type");
                // ignore "external" types
                if ("displayed".equals(sequenceType)) {
                    entry.addCanonicalIsoform(entry.getAttribute());
                } else if ("described".equals(sequenceType)) {
                    entry.addIsoform(entry.getAttribute());
                }
            } else if ("sequence".equals(qName)) {
                String strLength = getAttrValue(attrs, "length");
                String strMass = getAttrValue(attrs, "mass");
                if (strLength != null) {
                    entry.setLength(strLength);
                    attName = "residues";
                }
                if (strMass != null) {
                    entry.setMolecularWeight(strMass);
                }
                boolean isFragment = false;
                if (getAttrValue(attrs, "fragment") != null) {
                    isFragment = true;
                }
                entry.setFragment(isFragment);
            } else if ("feature".equals(qName) && getAttrValue(attrs, "type") != null) {
                Item feature = getFeature(getAttrValue(attrs, "type"), getAttrValue(attrs,
                    "description"), getAttrValue(attrs, "status"));
                entry.addFeature(feature);
            } else if (("begin".equals(qName) || "end".equals(qName))
                    && entry.processingFeature()
                    && getAttrValue(attrs, "position") != null) {
                entry.addFeatureLocation(qName, getAttrValue(attrs, "position"));
            } else if ("position".equals(qName) && entry.processingFeature()
                    && getAttrValue(attrs, "position") != null) {
                entry.addFeatureLocation("begin", getAttrValue(attrs, "position"));
                entry.addFeatureLocation("end", getAttrValue(attrs, "position"));
            } else if ("dbReference".equals(qName) && "citation".equals(previousQName)
                    && "PubMed".equals(getAttrValue(attrs, "type"))) {
                entry.addPub(getPub(getAttrValue(attrs, "id")));
            } else if ("comment".equals(qName)
                    && StringUtils.isNotEmpty(getAttrValue(attrs, "type"))) {
                entry.setCommentType(getAttrValue(attrs, "type"));
            } else if ("text".equals(qName) && "comment".equals(previousQName)) {
                attName = "text";
                String commentEvidence = getAttrValue(attrs, "evidence");
                if (StringUtils.isNotEmpty(commentEvidence)) {
                    entry.setCommentEvidence(commentEvidence);
                }
            } else if ("keyword".equals(qName)) {
                attName = "keyword";
            } else if ("dbReference".equals(qName) && "entry".equals(previousQName)) {
                entry.addDbref(getAttrValue(attrs, "type"), getAttrValue(attrs, "id"));
            } else if ("property".equals(qName) && "dbReference".equals(previousQName)) {
                String type = getAttrValue(attrs, "type");
                if (type.equals(CONFIG.getGeneDesignation())) {
                    entry.addGeneDesignation(getAttrValue(attrs, "value"));
                } else if ("evidence".equals(type)) {
                    entry.addGOEvidence(entry.getDbref(), getAttrValue(attrs, "value"));
                }
            } else if ("name".equals(qName) && "gene".equals(previousQName)) {
                attName = getAttrValue(attrs, "type");
            } else if ("evidence".equals(qName) && "entry".equals(previousQName)) {
                String evidenceCode = getAttrValue(attrs, "key");
                String pubmedString = getAttrValue(attrs, "attribute");
                if (StringUtils.isNotEmpty(evidenceCode) && StringUtils.isNotEmpty(pubmedString)) {
                    String pubRefId = getEvidence(pubmedString);
                    entry.addPubEvidence(evidenceCode, pubRefId);
                }
            } else if ("disease".equals(previousQName) && ("name".equals(qName)
                    || "acronym".equals(qName) || "description".equals(qName))) {
                attName = "disease";
            // <dbReference type="MIM" id="601665"/>
            } else if ("dbReference".equals(qName) && "disease".equals(previousQName)) {
                if (disease == null) {
                    disease = new DiseaseHolder();
                }
                String type = getAttrValue(attrs, "type");
                String id = getAttrValue(attrs, "id");
                disease.setIdentifier(type + ":" + id);
            } else if ("dbreference".equals(qName) || "comment".equals(qName)
                    || "isoform".equals(qName) || "gene".equals(qName) || "disease".equals(qName)) {
                // set temporary holder variables to null
                entry.reset();
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }

        private String parseTaxonId(String taxonId) {
            String mainTaxonId = CONFIG.getStrain(taxonId);
            if (StringUtils.isNotEmpty(mainTaxonId)) {
                return mainTaxonId;
            }
            return taxonId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);
            stack.pop();
            if (attName == null && attValue.toString() == null) {
                return;
            }

            String previousQName = null;
            if (!stack.isEmpty()) {
                previousQName = stack.peek();
            }

            if ("sequence".equals(qName)) {
                entry.setSequence(attValue.toString().replaceAll("\n", ""));
            } else if (StringUtils.isNotEmpty(attName) && "proteinName".equals(attName)) {
                entry.setName(attValue.toString());
            } else if (StringUtils.isNotEmpty(attName) && "synonym".equals(attName)) {
                entry.addProteinName(attValue.toString());
            } else if (StringUtils.isNotEmpty(attName) && "ecNumber".equals(attName)) {
                entry.addECNumber(attValue.toString());
            } else if ("text".equals(qName) && "comment".equals(previousQName)) {
                StringBuilder commentText = new StringBuilder();
                commentText.append(attValue.toString());
                if (commentText.length() > 0) {
                    Item item = createItem("Comment");
                    String commentType = entry.getCommentType();
                    item.setAttribute("type", commentType);
                    if (commentText.length() > POSTGRES_INDEX_SIZE) {
                        // comment text is a string
                        String ellipses = "...";
                        String choppedComment = commentText.substring(
                                0, POSTGRES_INDEX_SIZE - ellipses.length());
                        item.setAttribute("description", choppedComment + ellipses);
                    } else {
                        if ("disease".equals(commentType) && disease != null) {
                            commentText.append(" " + disease.toString());
                        }
                        item.setAttribute("description", commentText.toString());
                    }
                    String refId = item.getIdentifier();
                    try {
                        Integer objectId = store(item);
                        entry.addCommentRefId(refId, objectId);
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e);
                    }
                }
            } else if ("name".equals(qName) && "gene".equals(previousQName)) {
                entry.addGeneName(attName, attValue.toString());
            } else if ("keyword".equals(qName)) {
                entry.addKeyword(getKeyword(attValue.toString()));
            } else if (StringUtils.isNotEmpty(attName)
                    && "primaryIdentifier".equals(attName)) {
                entry.setPrimaryIdentifier(attValue.toString());
            } else if ("accession".equals(qName)) {
                String accession = attValue.toString();
                entry.addAccession(accession);
                if (accession.equals(entry.getPrimaryAccession())) {
                    checkUniqueIdentifier(entry, accession);
                }
            } else if (StringUtils.isNotEmpty(attName) && "component".equals(attName)
                    && "fullName".equals(qName)
                    && "recommendedName".equals(previousQName)
                    && stack.search("component") == 2) {
                entry.addComponent(attValue.toString());
            } else if (StringUtils.isNotEmpty(attName) && "disease".equals(attName)
                    && ("name".equals(qName) || "acronym".equals(qName)
                            || "description".equals(qName))
                    && "disease".equals(previousQName)) {
                String value = attValue.toString();
            } else if ("id".equals(qName) && "isoform".equals(previousQName)) {
                String accession = attValue.toString();

                // 119 isoforms have commas in their IDs
                if (accession.contains(",")) {
                    String[] accessions = accession.split("[, ]+");
                    accession = accessions[0];
                    for (int i = 1; i < accessions.length; i++) {
                        entry.addIsoformSynonym(accessions[i]);
                    }
                }

                // attribute should be empty, unless isoform has two <id>s
                if (entry.getAttribute() == null) {
                    entry.addAttribute(accession);
                } else {
                    // second <id> value is ignored and added as a synonym
                    entry.addIsoformSynonym(accession);
                }
            } else if ("entry".equals(qName)) {
                try {
                    processCommentEvidence(entry);
                    Set<UniprotEntry> isoforms = processEntry(entry);
                    if (isoforms != null) {
                        for (UniprotEntry isoform : isoforms) {
                            processEntry(isoform);
                        }
                    }
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            int st = start;
            int l = length;
            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (l > 0) {
                    boolean whitespace = false;
                    switch(ch[st]) {
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
                    ++st;
                    --l;
                }

                if (l > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, st, l);
                    attValue.append(s);
                }
            }
        }


        private Set<UniprotEntry> processEntry(UniprotEntry uniprotEntry)
            throws SAXException, ObjectStoreException {
            entryCount++;
            if (entryCount % 10000 == 0) {
                LOG.info("Processed " + entryCount + " entries.");
            }
            Set<UniprotEntry> isoforms = new HashSet<UniprotEntry>();
            // have we already seen a protein for this organism with the same sequence?
            if (!uniprotEntry.isIsoform() && !allowduplicates
                    && seenSequence(uniprotEntry.getTaxonId(), uniprotEntry.getMd5checksum())) {
                // if we have seen this sequence before for this organism just add the
                // primaryAccession of this protein as a synonym for the one already stored.
                Map<String, String> orgSequences = sequences.get(uniprotEntry.getTaxonId());
                if (orgSequences != null
                        && orgSequences.containsKey(uniprotEntry.getMd5checksum())) {
                    Item synonym = createSynonym(orgSequences.get(uniprotEntry.getMd5checksum()),
                            uniprotEntry.getPrimaryAccession(), false);
                    synonymsAndXrefs.add(synonym);
                }
                return isoforms;
            }

            if (uniprotEntry.hasDatasetRefId() && uniprotEntry.hasPrimaryAccession()
                    && !uniprotEntry.isDuplicate()) {

                if (!loadfragments && "true".equalsIgnoreCase(uniprotEntry.isFragment())) {
                    return Collections.emptySet();
                }

                setDataSet(uniprotEntry.getDatasetRefId());
                for (String isoformAccession: uniprotEntry.getIsoforms()) {
                    isoforms.add(uniprotEntry.createIsoformEntry(isoformAccession));
                }

                Item protein = createItem("Protein");

                /* primaryAccession, primaryIdentifier, name, etc */
                processIdentifiers(protein, uniprotEntry);

                processECNumbers(protein, uniprotEntry);

                String isCanonical = (uniprotEntry.isIsoform() ? "false" : "true");
                protein.setAttribute("isUniprotCanonical", isCanonical);

                /* sequence */
                if (!uniprotEntry.isIsoform()) {
                    processSequence(protein, uniprotEntry);
                }

                protein.setReference("organism", getOrganism(uniprotEntry.getTaxonId()));

                /* publications */
                if (uniprotEntry.getPubs() != null) {
                    protein.setCollection("publications", uniprotEntry.getPubs());
                }

                /* comments */
                if (uniprotEntry.hasComments()) {
                    protein.setCollection("comments", uniprotEntry.getComments());
                    processCommentEvidence(uniprotEntry);
                }

                /* keywords */
                if (uniprotEntry.getKeywords() != null) {
                    protein.setCollection("keywords", uniprotEntry.getKeywords());
                }

                /* features */
                processFeatures(protein, uniprotEntry);

                /* components */
                if (uniprotEntry.getComponents() != null
                        && !uniprotEntry.getComponents().isEmpty()) {
                    processComponents(protein, uniprotEntry);
                }

                // record that we have seen this sequence for this organism
                addSeenSequence(uniprotEntry.getTaxonId(), uniprotEntry.getMd5checksum(),
                        protein.getIdentifier());

                /* canonical */
                if (uniprotEntry.isIsoform()) {
                    // the uniprot accession is parsed in the getIdentifiers() method here
                    // so don't move this
                    String canonicalAccession = uniprotEntry.getUniprotAccession();
                    String canonicalRefId = proteins.get(canonicalAccession);
                    if (canonicalRefId == null) {
                        throw new RuntimeException("parsing an isoform without a parent "
                                + canonicalAccession);
                    }
                    protein.setReference("canonicalProtein", canonicalRefId);
                } else {
                    /* canonical protein so isoforms can refer to it */
                    proteins.put(uniprotEntry.getPrimaryAccession(), protein.getIdentifier());
                }

                try {
                    /* dbrefs (go terms, refseq) */
                    processDbrefs(protein, uniprotEntry);

                    /* genes */
                    processGene(protein, uniprotEntry);

                    store(protein);

                    // create synonyms for accessions and store xrefs and synonyms we've collected
                    processSynonyms(protein.getIdentifier(), uniprotEntry);

                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                synonymsAndXrefs = new HashSet<Item>();
            }
            return isoforms;
        }

        private void processCommentEvidence(UniprotEntry uniprotEntry)
            throws ObjectStoreException {
            Map<Integer, List<String>> commentEvidence = uniprotEntry.getCommentEvidence();
            for (Map.Entry<Integer, List<String>> e : commentEvidence.entrySet()) {
                Integer intermineObjectId = e.getKey();
                List<String> evidenceCodes = e.getValue();
                List<String> pubRefIds = new ArrayList<String>();
                for (String code : evidenceCodes) {
                    String pubRefId = uniprotEntry.getPubRefId(code);
                    if (pubRefId != null) {
                        pubRefIds.add(pubRefId);
                    } else {
                        LOG.error("bad evidence code:" + code + " for "
                                + uniprotEntry.getPrimaryAccession());
                    }
                }
                if (!pubRefIds.isEmpty()) {
                    ReferenceList publications = new ReferenceList("publications",
                        new ArrayList<String>(pubRefIds));
                    store(publications, intermineObjectId);
                }
            }
        }

        private void processSequence(Item protein, UniprotEntry uniprotEntry) {
            String seqIdentifier = getSequenceIdentfier(uniprotEntry.getMd5checksum(),
                    uniprotEntry.getSequence(), uniprotEntry.getLength());
            protein.setAttribute("length", uniprotEntry.getLength());
            protein.setReference("sequence", seqIdentifier);
            protein.setAttribute("molecularWeight", uniprotEntry.getMolecularWeight());
            protein.setAttribute("md5checksum", uniprotEntry.getMd5checksum());
        }

        private String getSequenceIdentfier(String md5Checksum, String residues, String length) {
            if (!allSequences.containsKey(md5Checksum)) {
                Item item = createItem("Sequence");
                item.setAttribute("residues", residues);
                item.setAttribute("length", length);
                item.setAttribute("md5checksum", md5Checksum);
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException(e);
                }
                allSequences.put(md5Checksum, item.getIdentifier());
            }
            return allSequences.get(md5Checksum);
        }

        private void processIdentifiers(Item protein, UniprotEntry uniprotEntry) {
            protein.setAttribute("name", uniprotEntry.getName());
            protein.setAttribute("isFragment", uniprotEntry.isFragment());
            protein.setAttribute("uniprotAccession", uniprotEntry.getUniprotAccession());
            String primaryAccession = uniprotEntry.getPrimaryAccession();
            protein.setAttribute("primaryAccession", primaryAccession);
            protein.setAttribute("secondaryIdentifier", primaryAccession);

            String primaryIdentifier = uniprotEntry.getPrimaryIdentifier();
            protein.setAttribute("uniprotName", primaryIdentifier);

            // primaryIdentifier must be unique, so append isoform suffix, eg -1
            if (uniprotEntry.isIsoform()) {
                primaryIdentifier = getIsoformIdentifier(primaryAccession, primaryIdentifier);
            }
            protein.setAttribute("primaryIdentifier", primaryIdentifier);
        }

        private void processECNumbers(Item protein, UniprotEntry uniprotEntry)
            throws SAXException {
            List<String> ecs = uniprotEntry.getECNumbers();
            if (ecs == null || ecs.isEmpty()) {
                return;
            }
            for (String identifier : ecs) {
                String refId = ecNumbers.get(identifier);

                if (refId == null) {
                    Item item = createItem("ECNumber");
                    item.setAttribute("identifier", identifier);
                    ecNumbers.put(identifier, item.getIdentifier());
                    try {
                        store(item);
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e);
                    }
                    refId = item.getIdentifier();
                }
                protein.addToCollection("ecNumbers", refId);
            }
        }

        private String getIsoformIdentifier(String primaryAccession, String primaryIdentifier) {
            String isoformIdentifier = primaryIdentifier;
            String[] bits = primaryAccession.split("\\-");
            if (bits.length == 2) {
                isoformIdentifier += "-" + bits[1];
            }
            return isoformIdentifier;
        }

        private void processComponents(Item protein, UniprotEntry uniprotEntry)
            throws SAXException {
            for (String componentName : uniprotEntry.getComponents()) {
                Item component = createItem("Component");
                component.setAttribute("name", componentName);
                component.setReference("protein", protein);
                try {
                    store(component);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
        }

        private void processFeatures(Item protein, UniprotEntry uniprotEntry)
            throws SAXException {
            List<String> featureTypes = CONFIG.getFeatureTypes();
            for (Item feature : uniprotEntry.getFeatures()) {
                // only store the features of interest
                if (featureTypes.isEmpty() || featureTypes.contains(
                        feature.getAttribute("type").getValue())) {
                    feature.setReference("protein", protein);
                    try {
                        store(feature);
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e);
                    }
                }
            }
        }

        private void processSynonyms(String proteinRefId, UniprotEntry uniprotEntry)
            throws ObjectStoreException {

            // accessions
            for (String accession : uniprotEntry.getAccessions()) {
                createSynonym(proteinRefId, accession, true);
            }

            // primaryIdentifier if isoform
            if (uniprotEntry.isIsoform()) {
                String isoformIdentifier =
                    getIsoformIdentifier(uniprotEntry.getPrimaryAccession(),
                            uniprotEntry.getPrimaryIdentifier());
                createSynonym(proteinRefId, isoformIdentifier, true);
            }

            // name <recommendedName> or <alternateName>
            for (String name : uniprotEntry.getProteinNames()) {
                createSynonym(proteinRefId, name, true);
            }

            // isoforms with extra identifiers
            List<String> isoformSynonyms = uniprotEntry.getIsoformSynonyms();
            if (isoformSynonyms != null && !isoformSynonyms.isEmpty()) {
                for (String identifier : isoformSynonyms) {
                    createSynonym(proteinRefId, identifier, true);
                }
            }

            // store xrefs and other synonyms we've created elsewhere
            for (Item item : synonymsAndXrefs) {
                if (item == null) {
                    continue;
                }
                store(item);
            }
        }

        private void processDbrefs(Item protein, UniprotEntry uniprotEntry)
            throws ObjectStoreException {
            Map<String, Set<String>> dbrefs = uniprotEntry.getDbrefs();
            for (Map.Entry<String, Set<String>> dbref : dbrefs.entrySet()) {
                String key = dbref.getKey();
                Set<String> values = dbref.getValue();
                for (String identifier : values) {
                    setCrossReference(protein.getIdentifier(), identifier, key, false);
                }
            }
        }

        // if cross references not listed in CONFIG, load all
        private void setCrossReference(String subjectId, String value, String dataSource,
                boolean store) throws ObjectStoreException {
            List<String> xrefs = CONFIG.getCrossReferences();
            if (xrefs.isEmpty() || xrefs.contains(dataSource)) {
                Item item = createCrossReference(subjectId, value, dataSource, store);
                if (item != null) {
                    synonymsAndXrefs.add(item);
                }
            }
        }

        private void processGoAnnotation(UniprotEntry uniprotEntry, Item gene)
            throws SAXException {
            Map<String, Set<String>> dbrefs = uniprotEntry.getDbrefs();
            for (Map.Entry<String, Set<String>> dbref : dbrefs.entrySet()) {
                String key = dbref.getKey();
                Set<String> values = dbref.getValue();
                if ("GO".equalsIgnoreCase(key)) {
                    for (String goTerm : values) {
                        String code = getGOEvidenceCode(entry.getGOEvidence(goTerm));
                        Item goEvidence = createItem("GOEvidence");
                        goEvidence.setReference("code", code);

                        Item goAnnotation = createItem("GOAnnotation");
                        goAnnotation.setReference("subject", gene);
                        goAnnotation.setReference("ontologyTerm", getGoTerm(goTerm));
                        goAnnotation.addToCollection("evidence", goEvidence);
                        gene.addToCollection("goAnnotation", goAnnotation);
                        try {
                            store(goEvidence);
                            store(goAnnotation);
                        } catch (ObjectStoreException e) {
                            throw new SAXException(e);
                        }
                    }
                }
            }
        }

        // gets the unique identifier and list of identifiers to set
        // loops through each gene entry, assigns refId to protein
        private void processGene(Item protein, UniprotEntry uniprotEntry)
            throws ObjectStoreException {
            String taxId = uniprotEntry.getTaxonId();
            String uniqueIdentifierField = getUniqueField(taxId);
            Set<String> geneIdentifiers = getGeneIdentifiers(uniprotEntry, uniqueIdentifierField);
            if (geneIdentifiers == null || geneIdentifiers.isEmpty()) {
                LOG.error("no valid gene identifiers found for "
                        + uniprotEntry.getPrimaryAccession());
                return;
            }
            boolean hasMultipleGenes = (geneIdentifiers.size() == 1 ? false : true);
            Item gene = null;
            for (String identifier : geneIdentifiers) {
                if (StringUtils.isEmpty(identifier)) {
                    continue;
                }
                if (GENE_PREFIXES.containsKey(taxId)) {
                    // Prepend RGD:
                    identifier = GENE_PREFIXES.get(taxId) + identifier;
                }
                gene = getGene(protein, uniprotEntry, identifier, taxId,
                        uniqueIdentifierField);
                // if we only have one gene, store later, we may have other gene fields to update
                if (gene != null && hasMultipleGenes) {
                    addPubs2Gene(uniprotEntry, gene);
                    store(gene);
                }
            }

            if (gene != null && !hasMultipleGenes) {
                Set<String> geneFields = getOtherFields(taxId);
                for (String geneField : geneFields) {
                    geneIdentifiers = getGeneIdentifiers(uniprotEntry, geneField);
                    if (geneIdentifiers == null) {
                        continue;
                    }
                    for (String geneIdentifier : geneIdentifiers) {
                        if (StringUtils.isEmpty(geneIdentifier)) {
                            continue;
                        }
                        if (GENE_PREFIXES.containsKey(taxId)) {
                            // Prepend RGD:
                            geneIdentifier = GENE_PREFIXES.get(taxId) + geneIdentifier;
                        }

                        if ("primaryIdentifier".equals(geneField)) {
                            String resolvedId = resolveGene(taxId, geneIdentifier);
                            if (resolvedId == null) {
                                LOG.info("Can not resolve " + geneIdentifier);
                            } else {
                                gene.setAttribute(geneField, resolvedId);
                            }
                        } else {
                            gene.setAttribute(geneField, geneIdentifier);
                        }

                    }
                }
                addPubs2Gene(uniprotEntry, gene);
                store(gene);
            }
        }

        /**
         * @param uniprotEntry
         * @param gene
         */
        private void addPubs2Gene(UniprotEntry uniprotEntry, Item gene) {
            if (uniprotEntry.getPubs() != null) {
                Iterator<String> genePubs = uniprotEntry.getPubs().iterator();
                while (genePubs.hasNext()) {
                    String refId = genePubs.next();
                    gene.addToCollection("publications", refId);
                }
            }
        }

        private Item getGene(Item protein, UniprotEntry uniprotEntry, String geneIdentifier,
                String taxId, String uniqueIdentifierField) {
            String identifier = resolveGene(taxId, geneIdentifier);
            if (identifier == null) {
                return null;
            }

            String geneRefId = genes.get(identifier);
            if (geneRefId == null) {
                Item gene = createItem("Gene");
                gene.setAttribute(uniqueIdentifierField, identifier);
                gene.setReference("organism", getOrganism(taxId));
                if (creatego) {
                    try {
                        processGoAnnotation(uniprotEntry, gene);
                    } catch (SAXException e) {
                        LOG.error("couldn't process GO annotation for gene - " + identifier);
                    }
                }
                geneRefId = gene.getIdentifier();
                genes.put(identifier, geneRefId);
                protein.addToCollection("genes", geneRefId);
                return gene;
            }
            protein.addToCollection("genes", geneRefId);
            return null;
        }

        private Set<String> getGeneIdentifiers(UniprotEntry uniprotEntry, String identifierField) {
            String taxId = uniprotEntry.getTaxonId();

            // which part of XML file to get values (eg. FlyBase, ORF, etc)
            String method = getGeneConfigMethod(taxId, identifierField);
            String value = getGeneConfigValue(taxId, identifierField);
            Set<String> geneIdentifiers = new HashSet<String>();
            if ("name".equals(method)) {
                geneIdentifiers = getByName(uniprotEntry, taxId, value);
            } else if ("gene-designation".equals(method)) {
                geneIdentifiers.addAll(uniprotEntry.getGeneDesignation(value));
            } else if ("dbref".equals(method)) {
                geneIdentifiers = getByDbref(uniprotEntry, value);
            } else {
                LOG.error("error processing config for organism " + taxId);
            }

            return geneIdentifiers;
        }

        private String getGeneConfigMethod(String taxId, String uniqueIdentifierField) {
            // how to get the identifier, eg. dbref OR name
            String method = CONFIG.getIdentifierMethod(taxId, uniqueIdentifierField);
            if (method == null) {
                // use default set in config file, if this organism isn't configured
                method = CONFIG.getIdentifierMethod("default", uniqueIdentifierField);
                if (method == null) {
                    throw new RuntimeException("error processing line in config file for organism "
                                               + taxId);
                }
            }
            return method;
        }

        private String getGeneConfigValue(String taxId, String uniqueIdentifierField) {
            // what value to use with method, eg. "FlyBase" or "ORF"
            String value = CONFIG.getIdentifierValue(taxId, uniqueIdentifierField);
            if (value == null) {
                value = CONFIG.getIdentifierValue("default", uniqueIdentifierField);
                if (value == null) {
                    throw new RuntimeException("error processing line in config file for organism "
                                               + taxId);
                }
            }
            return value;
        }

        private Set<String> getByName(UniprotEntry uniprotEntry, String taxId, String value) {
            if (uniprotEntry.getGeneNames() == null || uniprotEntry.getGeneNames().isEmpty()) {
                LOG.error("No gene names for " + taxId + ". protein accession:"
                        + uniprotEntry.getPrimaryAccession());
                return null;
            }
            Set<String> geneNames = uniprotEntry.getGeneNames().get(value);
            return geneNames;
        }

        private Set<String> getByDbref(UniprotEntry uniprotEntry, String value) {
            Set<String> geneIdentifiers = new HashSet<String>();
            if ("Ensembl".equals(value)) {
                // See #2122
                geneIdentifiers.addAll(uniprotEntry.getGeneDesignation(value));
            } else {
                Map<String, Set<String>> dbrefs = uniprotEntry.getDbrefs();
                final String msg = "no " + value
                    + " identifier found for gene attached to protein: "
                    + uniprotEntry.getPrimaryAccession();
                if (dbrefs == null || dbrefs.isEmpty()) {
                    LOG.error(msg);
                    return null;
                }
                Set<String> dbrefValues = dbrefs.get(value);
                if (dbrefValues == null || dbrefValues.isEmpty()) {
                    LOG.error(msg);
                    return null;
                }
                geneIdentifiers = dbrefs.get(value);
            }
            return geneIdentifiers;
        }

        // which gene.identifier field has to be unique
        private String getUniqueField(String taxId) {
            String uniqueIdentifierField = CONFIG.getUniqueIdentifier(taxId);
            if (uniqueIdentifierField == null) {
                uniqueIdentifierField = CONFIG.getUniqueIdentifier("default");
            }
            return uniqueIdentifierField;
        }

        // for this organism, set the following gene fields
        private Set<String> getOtherFields(String taxId) {
            Set<String> geneFields = CONFIG.getGeneIdentifierFields(taxId);
            if (geneFields == null) {
                geneFields = CONFIG.getGeneIdentifierFields("default");
            }
            return geneFields;
        }

        private String resolveGene(String taxId, String identifier) {
            if (FLY.equals(taxId)) {
                return resolveFlyGene(taxId, identifier);
            }
            return identifier;
        }

        private String resolveFlyGene(String taxId, String identifier) {
            if (rslv == null || !rslv.hasTaxon(taxId)) {
                // no id resolver available, so return the original identifier
                return identifier;
            }
            int resCount = rslv.countResolutions(taxId, identifier);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + identifier + " count: " + resCount + " FBgn: "
                         + rslv.resolveId(taxId, identifier));
                return null;
            }
            return rslv.resolveId(taxId, identifier).iterator().next();
        }
    }

    private void addSeenSequence(String taxonId, String md5checksum, String proteinIdentifier) {
        Map<String, String> orgSequences = sequences.get(taxonId);
        if (orgSequences == null) {
            orgSequences = new HashMap<String, String>();
            sequences.put(taxonId, orgSequences);
        }
        if (!orgSequences.containsKey(md5checksum)) {
            orgSequences.put(md5checksum, proteinIdentifier);
        }
    }

    private boolean seenSequence(String taxonId, String md5checksum) {
        Map<String, String> orgSequences = sequences.get(taxonId);
        if (orgSequences == null) {
            orgSequences = new HashMap<String, String>();
            sequences.put(taxonId, orgSequences);
        }
        return orgSequences.containsKey(md5checksum);
    }

    private String getKeyword(String title)
        throws SAXException {
        String refId = keywords.get(title);
        if (refId == null) {
            Item item = createItem("OntologyTerm");
            item.setAttribute("name", title);
            item.setReference("ontology", ontologies.get("UniProtKeyword"));
            refId = item.getIdentifier();
            keywords.put(title, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }

    // putting publications in map for later use
    // key = EC1, value = reference to publication
    // used by comments
    private String getEvidence(String attribute) throws SAXException {
        if (attribute.contains("=")) {
            String[] bits = attribute.split("=");
            if (bits.length == 2) {
                String pubMedId = bits[1];
                if (StringUtils.isNotEmpty(pubMedId)) {
                    return getPub(pubMedId);
                }
            }
        }
        return null;
    }

    private String getPub(String pubMedId)
        throws SAXException {
        String refId = pubs.get(pubMedId);

        if (refId == null) {
            Item item = createItem("Publication");
            item.setAttribute("pubMedId", pubMedId);
            pubs.put(pubMedId, item.getIdentifier());
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            refId = item.getIdentifier();
        }

        return refId;
    }

    // value is NAS:FlyBase
    private String getGOEvidenceCode(String value)
        throws SAXException {
        String[] bits = value.split(":");
        String code = "";
        if (bits == null) {
            code = value;
        } else {
            code = bits[0];
        }
        String refId = goEvidenceCodes.get(code);
        if (refId == null) {
            Item item = createItem("GOEvidenceCode");
            item.setAttribute("code", code);
            refId = item.getIdentifier();
            goEvidenceCodes.put(code, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }
        return refId;
    }

    private String getGoTerm(String identifier)
        throws SAXException {
        String refId = goterms.get(identifier);
        if (refId == null) {
            Item item = createItem("GOTerm");
            item.setAttribute("identifier", identifier);
            refId = item.getIdentifier();
            goterms.put(identifier, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }

    private String setOntology(String title)
        throws SAXException {
        String refId = ontologies.get(title);
        if (refId == null) {
            Item ontology = createItem("Ontology");
            ontology.setAttribute("name", title);
            ontologies.put(title, ontology.getIdentifier());
            try {
                store(ontology);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }

    private Item getFeature(String type, String description, String status)
        throws SAXException {
        //        List<String> featureTypes = CONFIG.getFeatureTypes();
        //        if (featureTypes.isEmpty() || featureTypes.contains(type)) {
        Item feature = createItem("UniProtFeature");
        feature.setAttribute("type", type);
        String keywordRefId = getKeyword(type);
        feature.setReference("feature", keywordRefId);
        String featureDescription = description;
        if (status != null) {
            featureDescription = (description == null ? status : description
                    + " (" + status + ")");
        }
        if (!StringUtils.isEmpty(featureDescription)) {
            feature.setAttribute("description", featureDescription);
        }
        return feature;
        //        }
        //        return null;
    }

    /**
     * Get a value from SAX attributes and trim() the returned string.
     * @param attrs SAX Attributes map
     * @param name the attribute to fetch
     * @return attValue
     */
    private static String getAttrValue(Attributes attrs, String name) {
        if (attrs.getValue(name) != null) {
            return attrs.getValue(name).trim();
        }
        return null;
    }

    private void checkUniqueIdentifier(UniprotEntry entry, String identifier) {
        if (StringUtils.isNotEmpty(identifier)) {
            if (!isUniqueIdentifier(identifier)) {
                entry.setDuplicate(true);
            }
        }
    }

    private boolean isUniqueIdentifier(String identifier) {
        if (identifiers == null) {
            identifiers = new HashSet<String>();
        } else if (identifiers.contains(identifier)) {
            LOG.error("not assigning duplicate identifier:  " + identifier);
            return false;
        }
        identifiers.add(identifier);
        return true;
    }

    /**
     * temporarily hold disease item until stored
     */
    protected class DiseaseHolder
    {
        private String name;
        private String acronym;
        private String description;
        private String identifier;

        /**
         * Constructor
         */
        protected DiseaseHolder() {

        }

        /**
         * @return name of disease
         */
        protected String getName() {
            return name;
        }

        /**
         * @param field which field to update
         * @param value to assign
         */
        protected void setDisease(String field, String value) {
            if ("name".equals(field)) {
                name = value;
            } else if ("acronym".equals(field)) {
                acronym = value;
            } else if ("description".equals(field)) {
                description = value;
            }
        }

        /**
         * @param identifier MI identifier
         */
        protected void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(identifier);

            if (StringUtils.isNotEmpty(name)) {
                sb.append(name);
            }
            if (StringUtils.isNotEmpty(acronym)) {
                sb.append(acronym);
            }
            if (StringUtils.isNotEmpty(description)) {
                sb.append(description);
            }
            return sb.toString();
        }
    }
}
