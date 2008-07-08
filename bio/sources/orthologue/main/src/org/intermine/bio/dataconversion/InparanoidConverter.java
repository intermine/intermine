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

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
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
    protected Item dataSet, pub;
    protected Map organisms = new LinkedHashMap();
    protected Map<String, Item> sources = new LinkedHashMap<String, Item>();
    protected Map orgSources = new HashMap();
    protected Map taxonIds = new HashMap();
    protected Map attributes = new HashMap();
    protected Map createObjects = new HashMap(); // which objects to create from which source

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
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // 1. create all bios and scores for cluster, in sets for each organism: orgA and orgB
        // 2. call homolog creation method (orgA, orgB) then (orgB, orgA)

        int lineNum = 0;
        String line, lastCode = null, oldIndex = null, index = null;

        Item bio = null;
        boolean isGene, onFirstOrganism = true;
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

            // code tells us which organism data is from
            String code = null;
            if (array[2].indexOf('.') > 0) {
                code = array[2].substring(0, array[2].indexOf('.'));
            } else {
                code = array[2];
            }

            // work out if this is a Gene or Translation and create item
            if (createObjects.get(code) != null) {
                if (createObjects.get(code).equals("Gene")) {
                    bio = newBioEntity(array[4], (String) attributes.get(code),
                                       getOrganism(code), "Gene");
                    isGene = true;
                } else {
                    bio = newBioEntity(array[4], (String) attributes.get(code),
                                       getOrganism(code), "Translation");
                    isGene = false;
                }
            } else {
                throw new RuntimeException("No configuration provided for organism code: " + code);
            }
            String orgName = code.substring(3);
            BioAndScores bands = new BioAndScores(bio.getIdentifier(), score, bootstrap,
                                                  isGene, orgName);

            // Three situations possible:
            if (!index.equals(oldIndex)) {
                // we have finished a group, create and store homologues
                // call twice to create in both directions (special test for first cluster)
                if (oldIndex != null || !onFirstOrganism) {
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
            if (onFirstOrganism) {
                orgA.add(bands);
            } else {
                orgB.add(bands);
            }

            oldIndex = index;
            lastCode = code;
        }

        if (lineNum > 0) {
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
            homologue.setReference("translation", first.getBio());
        }

        if (second.isGene()) {
            homologue.setReference("homologue", second.getBio());
        } else {
            homologue.setReference("homologueTranslation", second.getBio());
        }

        if (type.equals("orthologue") && first.getBootstrap() != null) {
            homologue.setAttribute("bootstrapScore", first.getBootstrap());
        }
        if (type.equals("orthologue") && second.getBootstrap() != null) {
            homologue.setAttribute("homologueBootstrapScore", second.getBootstrap());
        }

        homologue.setAttribute("type", type);
        homologue.setAttribute("clusterName", cluster);

        homologue.addToCollection("dataSets", dataSet.getIdentifier());
        homologue.addToCollection("publications", pub.getIdentifier());
        return homologue;
    }



    /**
     * Convenience method to create and cache Genes/Proteins by identifier
     * @param value identifier for the new Gene/Translation
     * @param organism the Organism for this Gene/Translation
     * @param type create either a Gene or Translation
     * @param attribute the attribute of the BioEntity set, e.g. identifier or primaryIdentifier
     * @return a new Gene/Translation Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newBioEntity(String value, String attribute, Item organism, String type)
        throws ObjectStoreException {

        // lookup by identifier and type, sometimes same id for translation and gene
        String key = type + value;
        if (bioEntities.containsKey(key)) {
            return (Item) bioEntities.get(key);
        }

        Item item = createItem(type);
        item.setAttribute(attribute, value);
        item.setReference("organism", organism.getIdentifier());
        item.setCollection("dataSets",
                            new ArrayList(Collections.singleton(dataSet.getIdentifier())));
        store(item);                                                 // Stores BioEntity
        bioEntities.put(key, item);

        // create a synonm - lookup source according to organism
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", value);
        synonym.setReference("subject", item.getIdentifier());
        Item source = getSourceForOrganism(organism.getAttribute("taxonId").getValue());
        synonym.setReference("source", source.getIdentifier());
        item.setCollection("dataSets",
                           new ArrayList(Collections.singleton(dataSet.getIdentifier())));
        store(synonym);                                              // Stores Synonym -> BioEntity

        return item;
    }

    // get source for synonyms, depends on organism
    private Item getSourceForOrganism(String taxonId) throws ObjectStoreException {
        String sourceName = (String) orgSources.get(taxonId);
        if (sourceName == null) {
                throw new IllegalArgumentException("unable to find source name for organism: "
                                                   + taxonId);
        }
        Item source = sources.get(sourceName);
        if (source == null) {
            source = createItem("DataSource");
            source.setAttribute("name", sourceName);
            store(source);
            sources.put(sourceName, source);
        }
        return source;
    }

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
        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "InParanoid data set");
        store(dataSet);
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

