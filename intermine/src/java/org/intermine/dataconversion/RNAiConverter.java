package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.ItemHelper;

/**
 * DataConverter to parse an RNAi data file into Items
 * @author Andrew Varley
 */
public class RNAiConverter extends FileConverter
{
    protected static final String RNAI_NS = "http://www.flymine.org/model/rnai#";
    
    protected Map genes = new HashMap();
    protected Map phenotypes = new HashMap();
    protected Item organism, pub, expt;
    protected int id = 0;

    /**
     * Constructor
     * @param reader Reader of input data in tab delimited format
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected RNAiConverter(BufferedReader reader, ItemWriter writer)
        throws ObjectStoreException {
        super(reader, writer);
        setupItems();
    }

    /**
     * @see DataConverter#process
     */
    public void process() throws Exception {
        try {
            //intentionally throw away first line
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] array = line.split("\t");
                Item gene = newGene(array[3]);
                Item phenotype =  newPhenotype(array[4]);
                ReferenceList refs = gene.getCollection("phenotypes");
                if (refs == null) {
                    refs = new ReferenceList();
                    refs.setName("phenotypes");
                    gene.addCollection(refs);
                }
                //make an attempt to ignore inexplicable duplicate lines in file
                if (!refs.getRefIds().contains(phenotype.getIdentifier())) {
                    refs.addRefId(phenotype.getIdentifier());
                }
            }
            
            for (Iterator i = genes.values().iterator(); i.hasNext();) {
                writer.store(ItemHelper.convert((Item) i.next()));
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Convenience method to create a new gene Item
     * @param identifier the wormbase sequence id
     * @return a new gene Item
     */
    protected Item newGene(String identifier)  {
        if (genes.containsKey(identifier)) {
            return (Item) genes.get(identifier);
        }
        Item item = newItem("Gene");
        item.addAttribute(new Attribute("identifier", identifier));
        item.addReference(new Reference("organism", organism.getIdentifier()));
        genes.put(identifier, item);
        return item;
    }

    /**
     * Convenience method to create and store a new phenotype Item
     * @param code the phenotype code
     * @return a new phenotype Item
     * @throws ObjectStoreException if an error occurs in storing the Utem
     */
    protected Item newPhenotype(String code) throws ObjectStoreException {
        if (phenotypes.containsKey(code)) {
            return (Item) phenotypes.get(code);
        }
        Item item = newItem("Phenotype");
        item.addAttribute(new Attribute("code", code));
        writer.store(ItemHelper.convert(item));
        phenotypes.put(code, item);
        return item;
    }

    /**
     * Convenience method to create common Items
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        organism = newItem("Organism");
        organism.addAttribute(new Attribute("name", "Caenorhabdits elegans"));
        organism.addAttribute(new Attribute("shortName", "C. elegans"));
        organism.addAttribute(new Attribute("taxonId", "6239"));
        writer.store(ItemHelper.convert(organism));

        pub = newItem("Publication");
        pub.addAttribute(new Attribute("title", "Systematic functional analysis of the "
                                       + "Caenorhabditis elegans genome using RNAi"));
        pub.addAttribute(new Attribute("journal", "Nature"));
        pub.addAttribute(new Attribute("volume", "16"));
        pub.addAttribute(new Attribute("issue", "6920"));
        pub.addAttribute(new Attribute("year", "2003"));
        pub.addAttribute(new Attribute("pages", "231-7"));
        pub.addAttribute(new Attribute("pubMedId", "12529635"));
        writer.store(ItemHelper.convert(pub));

        expt = newItem("RNAiExperiment");
        expt.addAttribute(new Attribute("publication", pub.getIdentifier()));
        writer.store(ItemHelper.convert(expt));
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item newItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + (id++));
        item.setClassName(RNAI_NS + className);
        item.setImplementations("");
        return item;
    }
}

