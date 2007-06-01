package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse an INPARANOID Orthologue/Paralogue "longsqltable" data file into Items
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class InparanoidConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected static final String PROP_FILE = "inparanoid_config.properties";
    protected Map bioEntities = new HashMap();
    protected Item db, pub;
    protected Map ids = new HashMap();
    protected Map organisms = new LinkedHashMap();
    protected ItemFactory itemFactory;
    protected Map sources = new LinkedHashMap();
    protected Map orgSources = new HashMap();
    protected Map taxonIds = new HashMap();
    protected Map attributes = new HashMap();
    protected Map createObjects = new HashMap(); // which objects to create from which source
    protected List<String> leftParalogues = new ArrayList<String>();
    protected List<String> rightParalogues = new ArrayList<String>();
    protected List<BioAndScores> firstParalogues = new ArrayList<BioAndScores>();
    protected List<BioAndScores> secondParalogues = new ArrayList<BioAndScores>();
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public InparanoidConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
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
                attribute = "identifier";
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
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        int lineNum = 0;
        String line, lastCode = null, oldIndex = null;      
        
        Item bio = null;
        BioAndScores firstBio = null, secondBio = null;
        boolean isGene, onFirstOrganism = true; 
        
        BufferedReader br = new BufferedReader(reader);
        while ((line = br.readLine()) != null) {
            lineNum++;
            String[] array = line.split("\t");

            if (array.length < 5) {
                throw new IllegalArgumentException("Line " + lineNum
                                                   + " does not have at least five elements: "
                                                   + line);
            }

            String index = array[0];
            String code = null;
            String bootstrap = null;
            if (array.length > 5) {
                bootstrap = array[5].substring(0, array[5].indexOf('%'));
            }
            String score = array[3];
            
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
            BioAndScores bands = new BioAndScores(bio.getIdentifier(), score, bootstrap, isGene);
            
            
            // Three situations possible:
            if (!index.equals(oldIndex)) {
                onFirstOrganism = true;
                
                if (oldIndex != null) {
                    // finish up and store the previous group
                    storeOrthologues(firstBio, secondBio); 
                }
                
                firstBio = bands;
                leftParalogues = new ArrayList<String>();
                rightParalogues = new ArrayList<String>();
                firstParalogues = new ArrayList<BioAndScores>();
                secondParalogues = new ArrayList<BioAndScores>();
            } else if (!code.equals(lastCode)) {
                // we are on the first line of the second organism in group
                secondBio = bands;
                
                onFirstOrganism = false;
                // could create an orthologue but don't know all inParalogues yet
                
            } else {
                // we are on a paralogue of the first or second bio
                      
                // create the paralogues
                Item leftPara, rightPara;
                if (onFirstOrganism) {
                    leftPara = createRelation("Paralogue", firstBio, bands, false, "");
                    rightPara = createRelation("Paralogue", firstBio, bands, true, "");
                    firstParalogues.add(bands);
                } else {
                    leftPara = createRelation("Paralogue", secondBio, bands, false, "");
                    rightPara = createRelation("Paralogue", secondBio, bands, true, "");
                    secondParalogues.add(bands);
                }
                
                // keep the paralogues in left/right paralogues
                leftParalogues.add(leftPara.getIdentifier());
                rightParalogues.add(rightPara.getIdentifier());
                
                getItemWriter().store(ItemHelper.convert(leftPara));
                getItemWriter().store(ItemHelper.convert(rightPara));
            }

            // clear old values and try to set new ones
            oldIndex = index;
            lastCode = code;

        }
        
        if (lineNum > 0) {
            // make sure final group gets stored
            storeOrthologues(firstBio, secondBio);
         }
    }

    private void storeOrthologues(BioAndScores firstBio, BioAndScores secondBio) 
    throws ObjectStoreException {
        List<Item> lefts = new ArrayList<Item>();
        List<Item> rights = new ArrayList<Item>();
        // create the main orthologues
        Item leftOrth = createRelation("Orthologue", firstBio, secondBio, false, "main");
        Item rightOrth = createRelation("Orthologue", firstBio, secondBio, true, "main");

        // set the inParalogues collection for main orthologue and store
        leftOrth.setCollection("paralogues", leftParalogues);
        rightOrth.setCollection("paralogues", rightParalogues);

        lefts.add(leftOrth);
        rights.add(rightOrth);

        // create coOrthologues for first organism
        for (BioAndScores first : firstParalogues) {
            Item coOrthLeft = createRelation("Orthologue", first, secondBio, false, "secondary");
            Item coOrthRight = createRelation("Orthologue", first, secondBio, true, "secondary");
            lefts.add(coOrthLeft);
            rights.add(coOrthRight);
        }
        
        // create coOrthologues for second organism
        for (BioAndScores second : secondParalogues) {
            Item coOrthLeft = createRelation("Orthologue", second, firstBio, true, "secondary");
            Item coOrthRight = createRelation("Orthologue", second, firstBio, false, "secondary");
            lefts.add(coOrthLeft);
            rights.add(coOrthRight);
        }
        
        // set coOrthologues collection to contain all left/right orthologues from group
        // except the current one.  Then store.
        for (Item orth : lefts) {
            List<String> coOrths = new ArrayList<String>();
            for (Item coOrth : lefts) {
                if (!coOrth.getIdentifier().equals(orth.getIdentifier())) {
                    coOrths.add(coOrth.getIdentifier());
                }
            }
            orth.setCollection("coOrthologues", coOrths);
            orth.setCollection("paralogues", leftParalogues);
            getItemWriter().store(ItemHelper.convert(orth));    
        }
        
        for (Item orth : rights) {
            List<String> coOrths = new ArrayList<String>();
            for (Item coOrth : rights) {
                if (!coOrth.getIdentifier().equals(orth.getIdentifier())) {
                    coOrths.add(coOrth.getIdentifier());
                }
            }
            orth.setCollection("coOrthologues", coOrths);
            orth.setCollection("paralogues", rightParalogues);
            getItemWriter().store(ItemHelper.convert(orth));    
        }
    }
    
    
    private Item createRelation(String className, BioAndScores first,
                               BioAndScores second, boolean reverse, String type) {
        Item relation = createItem(className);

        // the score is only relevant for the main orthologue
        if (!type.equals("secondary")) {
            relation.setAttribute("inParanoidScore", second.getScore());
        }
        
        // if not reversed then first is gene/translation and second is orthologue/paralogue
                
        if (first.isGene()) {
            relation.setReference(reverse ? className.toLowerCase() : "gene", first.getBio());
        } else {
            relation.setReference(reverse ? className.toLowerCase() + "Translation" 
                                          : "translation", first.getBio());
        }
        if (second.isGene()) {
            relation.setReference(reverse ? "gene" : className.toLowerCase(), second.getBio());
        } else {
            relation.setReference(reverse ? "translation" : className.toLowerCase() 
                                          + "Translation", second.getBio());
        }
        
        if (className.equals("Orthologue") && first.getBootstrap() != null) {
            relation.setAttribute(reverse ? className.toLowerCase() + "BootstrapScore"
                                          : "bootstrapScore", first.getBootstrap());
        }
        if (className.equals("Orthologue") && second.getBootstrap() != null) {
            relation.setAttribute(reverse ? "bootstrapScore" : className.toLowerCase() 
                                          + "BootstrapScore", second.getBootstrap());
        }
        if (className.equals("Orthologue")) {
            relation.setAttribute("type", type);
        }
        
        relation.addCollection(new ReferenceList("evidence",
            Arrays.asList(new Object[] {db.getIdentifier(), pub.getIdentifier()})));
        return relation;
    }
    
    
    /**
     * @see FileConverter#close()
     */
    public void close() throws ObjectStoreException {
        store(organisms.values());
        store(sources.values());
    }

    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        id = new Integer(id.intValue() + 1);
        ids.put(className, id);
        return id.toString();
    }

    /**
     * Convenience method to create and cache Genes/Proteins by identifier
     * @param value identifier for the new Gene/Translation
     * @param organism the Organism for this Gene/Translation
     * @param type create either a Gene or Translation
     * @param attribute the attribute of the BioEntity set, e.g. identifier or organismDbId
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
        getItemWriter().store(ItemHelper.convert(item));
        bioEntities.put(key, item);

        // create a synonm - lookup source according to organism
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", value);
        synonym.setReference("subject", item.getIdentifier());
        Item source = getSourceForOrganism(organism.getAttribute("taxonId").getValue());
        synonym.setReference("source", source.getIdentifier());
        getItemWriter().store(ItemHelper.convert(synonym));

        
        return item;
    }

    // get source for synonyms, depends on organism
    private Item getSourceForOrganism(String taxonId) {
        String sourceName = (String) orgSources.get(taxonId);
        if (sourceName == null) {
                throw new IllegalArgumentException("unable to find source name for organism: "
                                                   + taxonId);
        }
        Item source = (Item) sources.get(sourceName);
        if (source == null) {
            source = createItem("DataSource");
            source.setAttribute("name", sourceName);
            sources.put(sourceName, source);
        }
        return source;
    }

    private Item getOrganism(String code) {
        String taxonId = (String) taxonIds.get(code);
        if (taxonId == null) {
            throw new IllegalArgumentException("Unable to find taxonId for code: "
                                               + code + ", check properties: " + PROP_FILE);
        }

        Item organism = (Item) organisms.get(taxonId);
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonId);
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

        db = createItem("DataSet");
        db.setAttribute("title", "InParanoid data set");

        List toStore = Arrays.asList(new Object[] {db, pub});
        for (Iterator i = toStore.iterator(); i.hasNext();) {
            getItemWriter().store(ItemHelper.convert((Item) i.next()));
        }
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                    GENOMIC_NS + className, "");
    }

    private class BioAndScores 
    {
        private String bioIdentifier, score, bootstrap;
        private boolean isGene;
        
        public BioAndScores(String bioIdentifier, String score, String bootstrap,
                            boolean isGene) {
            this.bioIdentifier = bioIdentifier;
            this.score = score;
            this.bootstrap = bootstrap;
            this.isGene = isGene;
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
    }

}

