package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;

import java.io.BufferedReader;
import java.io.Reader;

import org.biojava.bio.Annotation;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.Location;

/**
 * DataConverter to parse an EMBL/Genbank/other file using BioJava and generate genomic model
 * items. 
 * @author Kim Rutherford
 */
public class BioJavaFlatFileConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Map bioEntities = new HashMap();
    protected Item db;
    protected Map ids = new HashMap();
    protected ItemFactory itemFactory;
    protected Map taxonIds = new HashMap();

    private Map genes = new HashMap();
    private Map organisms = new HashMap();
    private Map chromosomes = new HashMap();
    private Map proteins = new HashMap();
    private Set items = new HashSet();

    private Map cdsFeatures = new HashMap();

    private Map mrnaFeatures = new HashMap();

    private static final String TAXON_PREFIX = "taxon";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     */
    public BioJavaFlatFileConverter(ItemWriter writer) {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        BufferedReader br = new BufferedReader(reader);
        SequenceIterator sequences = SeqIOTools.readEmbl(br);
        while(sequences.hasNext()){
            Item chr = makeChromosome();
            Item org = null;

            try {
                Sequence seq = sequences.nextSequence();
                Iterator iter = seq.features();
                while (iter.hasNext()) {
                    Feature feature = (Feature) iter.next();
                    String type =  feature.getType();
                    
                    System.err.println("got: " + feature);
                    
                    System.err.println("type: " + type);

                    
                    Location location = feature.getLocation();
                    System.err.println("location: " + location);
                    
                    Annotation annotation = feature.getAnnotation();
                    
                    Iterator annoKeyIter = annotation.keys().iterator();
                    
                    while (annoKeyIter.hasNext()) {
                        Object key = annoKeyIter.next();
                        System.err.println("  " + key + ": " + annotation.getProperty(key));
                    }
                    
                    if (type.equals("source")) {
                        org = handleSourceFeature(feature, chr);
                        continue;
                    } 
                    
                    if (type.equals("CDS")) {
                        handleCDS(feature, chr);
                    } else {
                        if (type.equals("gene")) {
                            handleGene(feature, chr);
                        }
                    }
                }
                
                Iterator itemIter = items.iterator();
                while (itemIter.hasNext()) {
                    Item item = (Item) itemIter.next();
                    if (item.canReference("organism")) {
                        item.setReference("organism", org);
                    }
                    writer.store(ItemHelper.convert(item));
                }
            } catch (BioException ex) {
                //not in GenBank format
                ex.printStackTrace();
            } catch (NoSuchElementException ex) {
                //request for more sequence when there isn't any
                ex.printStackTrace();
            }
        }
    }

    /**
     * Create (or find in the cache) a CDS Item for the given feature
     * @param chr 
     */
    private void handleCDS(Feature feature, Item chr) {
        Annotation annotation = feature.getAnnotation();
        String geneName = (String) annotation.getProperty("gene");
        Item cds = getCDS(geneName);
        cds.setReference("gene", getGene(geneName));
        
        Item protein = getProtein(geneName + "_protein");
        cds.setReference("protein", protein);
        String uniprotId = getDbxref(feature, "UniProtKB/TrEMBL");
        protein.setAttribute("primaryAccession", uniprotId);
        Item mRNA = makeItem("MRNA");

        String mrnaName = getUniqueName(mrnaFeatures, geneName + "_MRNA");
        mRNA.setAttribute("identifier", mrnaName);
        mrnaFeatures.put(mrnaName, mRNA);
        mRNA.setReference("protein", protein);
        makeLocation(feature, cds, chr);
    }
    
    private Item getCDS(String geneName) {
        String cdsName = geneName + "_CDS";
        Item cds = makeItem("CDS");
        cdsName = getUniqueName(cdsFeatures, cdsName);
        cdsFeatures.put(cdsName, cds);

        cds.setAttribute("identifier", cdsName);
        
        return cds;
    }

    private String getUniqueName(Map features, String name) {
        if (features.containsKey(name)) {
            int maxIndex = 10000;
            int i = 2;
            for (; i < maxIndex; i++) {
                String testName = name + " " + i;
                if (!features.containsKey(testName)) {
                    return testName;
                }
            }
            throw new RuntimeException("can't find a unique name for CDS: " + name);
        }
        return name;
    }

    private String getDbxref(Feature feature, String dbName) {
        List dbxrefs = getPropList(feature.getAnnotation(), "db_xref");
        Iterator dbxrefsIter = dbxrefs.iterator();
        String prefix = dbName + ":";
        while (dbxrefsIter.hasNext()) {
            String dbxref = (String) dbxrefsIter.next();
            if (dbxref.startsWith(prefix)) {
                return dbxref.substring(prefix.length());
            }
        }
        return null;
    }

    private Item getProtein(String name) {
        if (proteins.containsKey(name)) {
            return (Item) proteins.get(name);
        }
        Item protein = makeItem("Protein");
        protein.setAttribute("name", name);
        proteins.put(name, protein);
        return protein;
    }

    private Item getGene(String name) {
        if (genes.containsKey(name)) {
            return (Item) genes.get(name);
        }
        Item gene = makeItem("Gene");
        gene.setAttribute("organismDbId", name);
        genes.put(name, gene);
        return gene;
    }
    
    private Item makeItem(String c) {
        Item item = itemFactory.makeItemForClass(GENOMIC_NS + c);
        items.add(item);
        return item;
    }

    /**
     * @param feature
     */
    private void handleGene(Feature feature, Item chr) {
        Annotation annotation = feature.getAnnotation();
        String geneName = (String) getPropList(annotation, "gene").get(0);
        Item gene = getGene(geneName);
        makeLocation(feature, gene, chr);
    }

    private List getPropList(Annotation annotation, String propName) {
        Object prop;
        try {
            prop = annotation.getProperty(propName);
        } catch (NoSuchElementException e) {
            return new ArrayList();
        }        
        if (prop instanceof List) {
            return (List) prop;
        } else {
            List retList = new ArrayList();
            retList.add(prop);
            return retList;
        }
    }

    private Item makeLocation(Feature feature, Item lsf, Item chr) {
        Location location = feature.getLocation();
        Item locItem = makeItem("Location");
        locItem.setAttribute("start", location.getMin() + "");
        locItem.setAttribute("end", location.getMax() + "");
        locItem.setReference("object", chr);
        locItem.setReference("subject", lsf);
        return locItem;
    }

    private Item handleSourceFeature(Feature feature, Item chr) {
        Annotation annotation = feature.getAnnotation();
        String map = (String) annotation.getProperty("map");
        String chromosomeName = (String) annotation.getProperty("map");
        if (map != null) {
            chromosomeName += " " + map;
        }

        chromosomeName = getUniqueName(chromosomes, chromosomeName);
        chr.setAttribute("identifier", chromosomeName);
        chromosomes.put(chromosomeName, chr);
        
        String taxonId = getDbxref(feature, TAXON_PREFIX);

        if (taxonId == null) {
            throw new RuntimeException("no dbxref starting with " + TAXON_PREFIX);
        } else {
            return getOrganism(taxonId);
        }
    }

    /**
     * Make (or find in the cache) an Organism Item for the given taxonId.
     */
    private Item getOrganism(String taxonId) {
        if (organisms .containsKey(taxonId)) {
            return (Item) organisms.get(taxonId);
        }
        Item organism = makeItem("Organism");
        organism.setAttribute("taxonId", taxonId);
        organisms.put(taxonId, organism);
        return organism;
    }

    private Item makeChromosome() {
        Item chr = makeItem("Chromosome");
        return chr;
    }

    /**
     * @see FileConverter#close()
     */
    public void close() throws ObjectStoreException {
        store(bioEntities.values());

    }
}

