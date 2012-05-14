package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;

/**
 * DataConverter to parse an INPARANOID Orthologue/Paralogue "sqltable" data file into Items
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class InparanoidConverter extends BioFileConverter
{
    protected static final String PROP_FILE = "inparanoid_config.properties";
    protected Map<String, Item> bioEntities = new HashMap<String, Item>();
    protected Item pub, evidence;
    protected Map<String, Item> sources = new LinkedHashMap<String, Item>();
    protected Map<String, String> orgSources = new HashMap<String, String>();
    protected Map<String, String> taxonIds = new HashMap<String, String>();
    protected Map<String, String> attributes = new HashMap<String, String>();
    // which objects to create from which source
    protected Map<String, String> createObjects = new HashMap<String, String>();
    private File genePeptideDir = null;
    private Map<String, Map<String, String>> peptideGeneMaps = null;
    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";
    //private static final Logger LOG = Logger.getLogger(InparanoidConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public InparanoidConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, "InParanoid", "InParanoid data set");
        setupItems();
        readConfig();
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
                throw new IllegalArgumentException("Unable to find 'taxonId' property for code: "
                                                   + code + " in file: " + PROP_FILE);
            }
            taxonId = taxonId.trim();
            String source = codeProps.getProperty("source");
            if (source == null) {
                throw new IllegalArgumentException("Unable to find 'source' property for code: "
                                                   + code + " in file: " + PROP_FILE);
            }
            String attribute = codeProps.getProperty("attribute");
            if (attribute == null) {
                attribute = "primaryIdentifier";
            }
            String object = codeProps.getProperty("object");
            if (object == null) {
                object = "transcript";
            }

            source = source.trim();
            taxonId = taxonId.trim();
            attribute = attribute.trim();
            code = code.trim();
            taxonIds.put(code, taxonId);
            orgSources.put(taxonId, source);
            attributes.put(code, attribute);
            createObjects.put(code, object);
        }
    }

    /**
     * Set a directory where files mapping gene ids to peptide ids can be found.  The files are
     * expected to be in standard BioMart export style:
     *      gene_id     peptide_id
     * For multiple peptides per gene the gene_id will appear on multiple lines  For no peptide
     * column 2 is empty.
     * @param genePeptideDir directory containing gene peptide mappings files
     */
    public void setGenePeptideFiles(File genePeptideDir) {
        this.genePeptideDir = genePeptideDir;
    }


    /**
     * Given a directory for gene/peptide mappings: find files, determine taxon id from file name
     * and read files contents into a map from peptide id to gene id.
     * @throws IOException if we can't read the file
     */
    private void readGenePeptideMappings()
        throws IOException {
         // contruct here whatever so calling method can test null
        peptideGeneMaps = new HashMap<String, Map<String, String>>();

        if (genePeptideDir != null) {
            if (genePeptideDir.isDirectory()) {
                for (File file : genePeptideDir.listFiles()) {
                    String fileName = file.getName();
                    if (!fileName.endsWith("gene_peptide.txt")) {
                        continue;
                    }
                    String taxonId = fileName.substring(0, fileName.indexOf('_'));
                    Map<String, String> peptideGeneMap = new HashMap<String, String>();
                    peptideGeneMaps.put(taxonId, peptideGeneMap);

                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
                    while (lineIter.hasNext()) {
                        String[] line = (String[]) lineIter.next();
                        if (line.length >= 2) {
                            String geneId = line[0];
                            String peptideId = line[1];
                            peptideGeneMap.put(peptideId, geneId);
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // 1. create all bios and scores for cluster, in sets for each organism: orgA and orgB
        // 2. call homolog creation method (orgA, orgB) then (orgB, orgA)

        if (peptideGeneMaps == null) {
            readGenePeptideMappings();
        }

        int lineNum = 0;
        String line, lastCode = null, oldIndex = null, index = null;

        Item bio = null;
        boolean isGene, onFirstOrganism = true;
        Set<String> abortClusters = new HashSet<String>();
        List<BioAndScores> orgA = new ArrayList<BioAndScores>(),
        orgB = new ArrayList<BioAndScores>();

        BufferedReader br = new BufferedReader(reader);
        while ((line = br.readLine()) != null) {
            lineNum++;
            String[] array = line.split("\t");

            if (array.length < 5) {
                throw new IllegalArgumentException("Line " + lineNum
                                                   + " does not have at least five elements: "
                                                   + line);
            }

            index = array[0];

            // not all rows have a bootsrap score
            String bootstrap = null;
            if (array.length > 5 && StringUtils.isNotEmpty(array[5])) {
                try {
                    bootstrap = array[5].substring(0, array[5].indexOf('%'));
                } catch (Exception e) {
                    throw new RuntimeException("Error getting bootstrap score from line: "
                                               + lineNum + " of file: "
                                               + getCurrentFile().getName());
                }
            }
            String score = array[3];
            String identifier = array[4];

            // code tells us which organism data is from
            String code = null;
            if (array[2].indexOf('.') > 0) {
                code = array[2].substring(0, array[2].indexOf('.'));
            } else {
                code = array[2];
            }

            // work out if this is a Gene or Protein and create item
            if (createObjects.get(code) != null) {
                if ("Gene".equals(createObjects.get(code))) {
                    bio = newBioEntity(identifier, attributes.get(code),
                            getOrganism(code), "Gene");
                    isGene = true;
                } else {
                    // if we have found a mapping file for this organism then convert protein
                    // to genes and don't create proteins
                    String taxonId = taxonIds.get(code);

                    if (peptideGeneMaps.containsKey(taxonId)) {
                        isGene = true;
                        Map<String, String> peptideGeneMap = peptideGeneMaps.get(taxonId);
                        if (peptideGeneMap.containsKey(identifier)) {
                            // found corresponding gene, so create it
                            String geneId = peptideGeneMap.get(identifier);
                            bio = newBioEntity(geneId, attributes.get(code),
                                    getOrganism(code), "Gene");
                        } else {
                            // no peptide id found so remove whole cluster
                            // TODO this could be more selective about clusters it aborts, i.e. if
                            // the invalid id is a paralogue could still create orthologues.
                            abortClusters.add(index);
                        }
                    } else {
                        // create protein as a last resort
                        bio = newBioEntity(identifier, attributes.get(code),
                                getOrganism(code), "Protein");
                        isGene = false;
                    }
                }
            } else {
                throw new RuntimeException("No configuration provided for organism code: " + code);
            }

            String orgName = code.substring(3);
            BioAndScores bands = null;
            if (!abortClusters.contains(index)) {
                bands = new BioAndScores(bio.getIdentifier(), score, bootstrap,
                                                  isGene, orgName);
            }
            // Three situations possible:
            if (!index.equals(oldIndex)) {
                // we have finished a group, create and store homologues
                // call twice to create in both directions (special test for first cluster)
                if ((oldIndex != null || !onFirstOrganism) && !abortClusters.contains(oldIndex)) {
                    createHomologues(orgA, orgB, oldIndex);
                    createHomologues(orgB, orgA, oldIndex);
                }

                // reset for next group
                onFirstOrganism = true;
                orgA = new ArrayList<BioAndScores>();
                orgB = new ArrayList<BioAndScores>();
            } else if (!code.equals(lastCode)) {
                // we are on the first line of the second organism in group
                onFirstOrganism = false;
            } else {
                // we are on a paralogue of the first or second bio, do nothing now
            }

            // store the bios and scores by organism
            if (!abortClusters.contains(index)) {
                if (onFirstOrganism) {
                    orgA.add(bands);
                } else {
                    orgB.add(bands);
                }
            }

            oldIndex = index;
            lastCode = code;
        }

        if (lineNum > 0 && !abortClusters.contains(oldIndex)) {
            // make sure final group gets stored
            createHomologues(orgA, orgB, oldIndex);
            createHomologues(orgB, orgA, oldIndex);
        }
    }


    // homolog creation method:
    // foreach orgA
    //   foreach orgA
    //   if this.score = 1 or other score = 1
    //     create an inParalogue
    //   foreach orgB
    //     if both scores are 1
    //       create an orthologue
    //     else if this.score or other.score = 1
    //       create an inParalogue
    //     else
    //       do nothing (these are two separate inParalogues of main orthologue)

    private void createHomologues(List<BioAndScores> orgA, List<BioAndScores> orgB, String index)
        throws ObjectStoreException {
        // generate a name for the cluster based on organisms (in order) and index
        String cluster = orgA.get(0).getOrganism() + "-" + orgB.get(0).getOrganism() + ":" + index;

        Set<String> alreadyDone = new HashSet<String>();
        for (BioAndScores thisBio : orgA) {
            // create paralogues with other orgA bios
            for (BioAndScores otherBio : orgA) {
                if (thisBio == otherBio) {
                    continue;
                }
                // only create paralogues between 'ortholgoues' in the cluster and other bios
                if ((Double.parseDouble(thisBio.score) == 1)
                                || (Double.parseDouble(otherBio.score) == 1)) {
                    // reverse the cluster name if already created this pair in opposite direction
                    String nameToUse;
                    if (alreadyDone.contains("" + otherBio.bioIdentifier + thisBio.bioIdentifier)) {
                        nameToUse = orgB.get(0).getOrganism() + "-" + orgA.get(0).getOrganism()
                            + ":" + index;
                    } else {
                        nameToUse = cluster;
                    }
                    store(createHomologue(thisBio, otherBio, "inParalogue", nameToUse));
                    alreadyDone.add("" + thisBio.bioIdentifier + otherBio.bioIdentifier);
                }
            }
            // create orthologues and paralogues to bios in other organism
            for (BioAndScores otherBio : orgB) {
                // create an orthologue where both bios are a main bio in cluster,
                // create a paralogue if only one of the bios is a 'main' bio in cluster
                if ((Double.parseDouble(thisBio.score) == 1)
                                && (Double.parseDouble(otherBio.score) == 1)) {
                    store(createHomologue(thisBio, otherBio, "orthologue", cluster));
                } else if ((Double.parseDouble(thisBio.score) == 1)
                                || (Double.parseDouble(otherBio.score) == 1)) {
                    store(createHomologue(thisBio, otherBio, "inParalogue", cluster));
                }
            }
        }
    }

    // create and store a Homologue item
    private Item createHomologue(BioAndScores first, BioAndScores second,
                                 String type, String cluster) {
        Item homologue = createItem("Homologue");

        // at least one score will be 1, if an inParalogue then we want the score that isn't 1
        String score = "" + Math.min(Double.parseDouble(first.getScore()),
                                     Double.parseDouble(second.getScore()));
        homologue.setAttribute("inParanoidScore", score);
        if (first.isGene()) {
            homologue.setReference("gene", first.getBio());
        } else {
            homologue.setReference("protein", first.getBio());
        }

        if (second.isGene()) {
            homologue.setReference("homologue", second.getBio());
        } else {
            homologue.setReference("homologueProtein", second.getBio());
        }

        if ("orthologue".equals(type) && first.getBootstrap() != null) {
            homologue.setAttribute("bootstrapScore", first.getBootstrap());
        }
        if ("orthologue".equals(type) && second.getBootstrap() != null) {
            homologue.setAttribute("homologueBootstrapScore", second.getBootstrap());
        }

        homologue.setAttribute("type", type);
        homologue.setAttribute("clusterName", cluster);
        homologue.addToCollection("evidence", evidence);
        return homologue;
    }

    /**
     * Convenience method to create and cache Genes/Proteins by identifier
     * @param identifier identifier for the new Gene/Protein
     * @param organismRefId id representing the organism object
     * @param type create either a Gene or Protein
     * @param attribute the attribute of the BioEntity set, e.g. identifier or primaryIdentifier
     * @return a new Gene/Protein Item
     * @throws ObjectStoreException if an error occurs in storing
     * @throws SAXException if something goes horribly wrong
     */
    protected Item newBioEntity(String identifier, String attribute, String organismRefId,
            String type)
        throws ObjectStoreException, SAXException {

        // lookup by identifier and type, sometimes same id for protein and gene
        String key = type + identifier;
        if (bioEntities.containsKey(key)) {
            return bioEntities.get(key);
        }
        Item item = createItem(type);
        item.setAttribute(attribute, identifier);
        item.setReference("organism", organismRefId);
        store(item);
        bioEntities.put(key, item);
        return item;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "11743721");
        store(pub);
        Item evidenceCode = createItem("OrthologueEvidenceCode");
        evidenceCode.setAttribute("abbreviation", EVIDENCE_CODE_ABBR);
        evidenceCode.setAttribute("name", EVIDENCE_CODE_NAME);
        store(evidenceCode);
        evidence = createItem("OrthologueEvidence");
        evidence.setReference("evidenceCode", evidenceCode);
        evidence.addToCollection("publications", pub);
        store(evidence);
    }

    private class BioAndScores
    {
        private String bioIdentifier, score, bootstrap, organism;
        private boolean isGene;

        public BioAndScores(String bioIdentifier, String score, String bootstrap,
                            boolean isGene, String organism) {
            this.bioIdentifier = bioIdentifier;
            this.score = score;
            this.bootstrap = bootstrap;
            this.isGene = isGene;
            this.organism = organism;
        }

        public String getBio() {
            return bioIdentifier;
        }

        public String getScore() {
            return score;
        }

        public String getBootstrap() {
            return bootstrap;
        }

        public boolean isGene() {
            return isGene;
        }

        public String getOrganism() {
            return organism;
        }

        public String toString() {
            return bioIdentifier + " " + organism + " " + score;
        }
    }

    /**
     * @param code inparanoid's code for an organism
     * @return ID representing the stored organism object
     */
    public String getOrganism(String code) {
        String taxonId = taxonIds.get(code);
        if (taxonId == null) {
            throw new IllegalArgumentException("Unable to find taxonId for code: "
                    + code + ", check properties: " + PROP_FILE);
        }
        String refId = super.getOrganism(taxonId);
        return refId;
    }
}
