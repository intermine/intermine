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
import java.io.File;
import java.io.FileNotFoundException;
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

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;
import org.intermine.xml.full.Item;

/**
 * DataConverter to parse an INPARANOID Orthologue/Paralogue "sqltable" data file into Items
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class InparanoidConverter extends FileConverter
{
    protected static final String PROP_FILE = "inparanoid_config.properties";
    protected Map bioEntities = new HashMap();
    protected Item dataSource, dataSet, pub, evidence;
    protected Map organisms = new LinkedHashMap();
    protected Map<String, Item> sources = new LinkedHashMap<String, Item>();
    protected Map orgSources = new HashMap();
    protected Map taxonIds = new HashMap();
    protected Map attributes = new HashMap();
    protected Map createObjects = new HashMap(); // which objects to create from which source
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
        super(writer, model);
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
        Enumeration propNames = props.propertyNames();

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
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void readGenePeptideMappings() throws FileNotFoundException, IOException {
        peptideGeneMaps = new HashMap();  // contruct here whatever so calling method can test null

        if (genePeptideDir != null) {
            if (genePeptideDir.isDirectory()) {
                for (File file : genePeptideDir.listFiles()) {
                    String fileName = file.getName();
                    if (!fileName.endsWith("gene_peptide.txt")) {
                        continue;
                    }
                    String taxonId = fileName.substring(0, fileName.indexOf('_'));
                    Map<String, String> peptideGeneMap = new HashMap();
                    peptideGeneMaps.put(taxonId, peptideGeneMap);

                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
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
        Set<String> abortClusters = new HashSet();
        List<BioAndScores> orgA = new ArrayList(), orgB = new ArrayList();

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
            if (array.length > 5 && array[5] != null && !array[5].equals("")) {
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
                if (createObjects.get(code).equals("Gene")) {
                    bio = newBioEntity(identifier, (String) attributes.get(code),
                            getOrganism(code), "Gene");
                    isGene = true;
                } else {
                    // if we have found a mapping file for this organism then convert protein
                    // to genes and don't create proteins
                    String taxonId = (String) taxonIds.get(code);

                    if (peptideGeneMaps.containsKey(taxonId)) {
                        isGene = true;
                        Map<String, String> peptideGeneMap = peptideGeneMaps.get(taxonId);
                        if (peptideGeneMap.containsKey(identifier)) {
                            // found corresponding gene, so create it
                            String geneId = peptideGeneMap.get(identifier);
                            bio = newBioEntity(geneId, (String) attributes.get(code),
                                    getOrganism(code), "Gene");
                        } else {
                            // no peptide id found so remove whole cluster
                            // TODO this could be more selective about clusters it aborts, i.e. if
                            // the invalid id is a paralogue could still create orthologues.
                            abortClusters.add(index);
                        }

                    } else {

                        // create protein as a last resort
                        bio = newBioEntity(identifier, (String) attributes.get(code),
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
                orgA = new ArrayList();
                orgB = new ArrayList();
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

        Set alreadyDone = new HashSet();
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

        if (type.equals("orthologue") && first.getBootstrap() != null) {
            homologue.setAttribute("bootstrapScore", first.getBootstrap());
        }
        if (type.equals("orthologue") && second.getBootstrap() != null) {
            homologue.setAttribute("homologueBootstrapScore", second.getBootstrap());
        }

        homologue.setAttribute("type", type);
        homologue.setAttribute("clusterName", cluster);
        homologue.addToCollection("dataSets", dataSet);
        homologue.addToCollection("evidence", evidence);
        return homologue;
    }

    /**
     * Convenience method to create and cache Genes/Proteins by identifier
     * @param identifier identifier for the new Gene/Protein
     * @param organism the Organism for this Gene/Protein
     * @param type create either a Gene or Protein
     * @param attribute the attribute of the BioEntity set, e.g. identifier or primaryIdentifier
     * @return a new Gene/Protein Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newBioEntity(String identifier, String attribute, Item organism, String type)
        throws ObjectStoreException {

        // lookup by identifier and type, sometimes same id for protein and gene
        String key = type + identifier;
        if (bioEntities.containsKey(key)) {
            return (Item) bioEntities.get(key);
        }

        Item item = createItem(type);
        item.setAttribute(attribute, identifier);
        item.setReference("organism", organism.getIdentifier());
        item.addToCollection("dataSets", dataSet);
        store(item);                                                 // Stores BioEntity
        bioEntities.put(key, item);

        // create a synonm - lookup source according to organism
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", identifier);
        synonym.setReference("subject", item.getIdentifier());
        // TODO should we change dataset instead?
        //Item source = getSourceForOrganism(organism.getAttribute("taxonId").getValue());
        //synonym.setReference("source", source.getIdentifier());
        synonym.addToCollection("dataSets", dataSet);
        store(synonym);                                              // Stores Synonym -> BioEntity

        return item;
    }

    // get source for synonyms, depends on organism
//    private Item getSourceForOrganism(String taxonId) throws ObjectStoreException {
//        String sourceName = (String) orgSources.get(taxonId);
//        if (sourceName == null) {
//                throw new IllegalArgumentException("unable to find source name for organism: "
//                                                   + taxonId);
//        }
//        Item source = sources.get(sourceName);
//        if (source == null) {
//            source = createItem("DataSource");
//            source.setAttribute("name", sourceName);
//            store(source);
//            sources.put(sourceName, source);
//        }
//        return source;
//    }

    private Item getOrganism(String code) throws ObjectStoreException {
        String taxonId = (String) taxonIds.get(code);
        if (taxonId == null) {
            throw new IllegalArgumentException("Unable to find taxonId for code: "
                                               + code + ", check properties: " + PROP_FILE);
        }

        Item organism = (Item) organisms.get(taxonId);
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonId);
            store(organism);
            organisms.put(taxonId, organism);
        }
        return organism;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "11743721");
        store(pub);
        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "InParanoid");
        store(dataSource);
        dataSet = createItem("DataSet");
        dataSet.setAttribute("name", "InParanoid data set");
        dataSet.setReference("dataSource", dataSource);
        store(dataSet);

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
}

