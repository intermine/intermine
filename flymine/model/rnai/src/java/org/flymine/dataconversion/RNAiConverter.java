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
import java.util.Arrays;

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
    protected Map synonyms = new HashMap();
    protected Item organism, expt, db;
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
                String[] array = line.split("\t", -1); //keep trailing empty Strings
                Item gene = newGene(array[3], array[6]);
                addPhenotype(gene, array[4]);
                if (array.length > 13 && !array[13].trim().equals("")) {
                    addSynonym(gene, array[13]);
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
     * Add a synonym to a gene
     * @param gene a gene Item
     * @param syn the actual synonym for the gene
     * @throws ObjectStoreException if an error occurs when storing the Item
     */
    protected void addSynonym(Item gene, String syn) throws ObjectStoreException {
        ReferenceList refs = gene.getCollection("synonyms");
        if (refs == null) {
            refs = new ReferenceList();
            refs.setName("synonyms");
            gene.addCollection(refs);
        }
        Item synonym = newSynonym(syn, gene);
        if (!refs.getRefIds().contains(synonym.getIdentifier())) {
            refs.addRefId(synonym.getIdentifier());
        }
    }

    /**
     * Add a phenotype to a gene
     * @param gene a gene Item
     * @param code the phenotype code
     * @throws ObjectStoreException if an error occurs storing the Item
     */
    protected void addPhenotype(Item gene, String code) throws ObjectStoreException {
        ReferenceList refs = gene.getCollection("phenotypes");
        if (refs == null) {
            refs = new ReferenceList();
            refs.setName("phenotypes");
            gene.addCollection(refs);
        }
        Item phenotype =  newPhenotype(code, gene);
        if (!refs.getRefIds().contains(phenotype.getIdentifier())) {
            refs.addRefId(phenotype.getIdentifier());
        }
    }

    /**
     * Convenience method to create a new gene Item
     * @param sequenceName the WormBase sequence name
     * @param commonName the CGC-Approved gene name
     * @return a new gene Item
     * @throws ObjectStoreException if an error occurs when storing the Item
     */
    protected Item newGene(String sequenceName, String commonName)  throws ObjectStoreException {
        if (genes.containsKey(sequenceName)) {
            return (Item) genes.get(sequenceName);
        }
        Item item = newItem("Gene");
        item.addAttribute(new Attribute("sequenceName", sequenceName));
        item.addAttribute(new Attribute("commonName", commonName));
        item.addReference(new Reference("organism", organism.getIdentifier()));
        genes.put(sequenceName, item);
        return item;
    }

    /**
     * Convenience method to create and store a new phenotype Item
     * @param code the phenotype code
     * @param subject the phenotype's subject item
     * @return a new phenotype Item
     * @throws ObjectStoreException if an error occurs in storing the Utem
     */
    protected Item newPhenotype(String code, Item subject) throws ObjectStoreException {
        if (phenotypes.containsKey(code)) {
            return (Item) phenotypes.get(code);
        }
        Item item = newItem("Phenotype");
        item.addAttribute(new Attribute("code", code));
        item.addReference(new Reference("subject", subject.getIdentifier()));
        item.addCollection(new ReferenceList("evidence",
                                             Arrays.asList(new Object[] {expt.getIdentifier()})));
        writer.store(ItemHelper.convert(item));
        phenotypes.put(code, item);
        return item;
    }

    /**
     * Convenience method to create and store a new synonym Item
     * @param synonym the actual synonym
     * @param subject the synonym's subject item
     * @return a new synonym Item
     * @throws ObjectStoreException if an error occurs in storing the Utem
     */
    protected Item newSynonym(String synonym, Item subject) throws ObjectStoreException {
        if (synonyms.containsKey(synonym)) {
            return (Item) synonyms.get(synonym);
        }
        Item item = newItem("Synonym");
        item.addAttribute(new Attribute("synonym", synonym));
        item.addReference(new Reference("subject", subject.getIdentifier()));
        item.addReference(new Reference("source", db.getIdentifier()));
        writer.store(ItemHelper.convert(item));
        synonyms.put(synonym, item);
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

        db = newItem("Database");
        db.addAttribute(new Attribute("title", "WormBase"));
        db.addAttribute(new Attribute("url", "http://www.wormbase.org"));
        writer.store(ItemHelper.convert(db));

        Item pub = newItem("Publication");
        pub.addAttribute(new Attribute("title", "Systematic functional analysis of the "
                                       + "Caenorhabditis elegans genome using RNAi"));
        pub.addAttribute(new Attribute("journal", "Nature"));
        pub.addAttribute(new Attribute("volume", "16"));
        pub.addAttribute(new Attribute("issue", "6920"));
        pub.addAttribute(new Attribute("year", "2003"));
        pub.addAttribute(new Attribute("pages", "231-7"));
        pub.addAttribute(new Attribute("pubMedId", "12529635"));
        addAuthor(pub, "Kamath RS");
        addAuthor(pub, "Fraser AG");
        addAuthor(pub, "Dong Y");
        addAuthor(pub, "Poulin G");
        addAuthor(pub, "Durbin R");
        addAuthor(pub, "Gotta M");
        addAuthor(pub, "Kanapin A");
        addAuthor(pub, "Le Bot N");
        addAuthor(pub, "Moreno S");
        addAuthor(pub, "Sohrmann M");
        addAuthor(pub, "Welchman DP");
        addAuthor(pub, "Zipperlen P");
        addAuthor(pub, "Ahringer J");
        writer.store(ItemHelper.convert(pub));

        expt = newItem("RNAiExperiment");
        expt.addReference(new Reference("publication", pub.getIdentifier()));
        writer.store(ItemHelper.convert(expt));
    }

    /**
     * Convenience method to create and store a new author Item
     * @param pub the publication associated with the author
     * @param name the author's name
     * @throws ObjectStoreException if an error occurs in storing the Utem
     */
    protected void addAuthor(Item pub, String name) throws ObjectStoreException {
        Item item = newItem("Author");
        item.addAttribute(new Attribute("name", name));
        item.addCollection(new ReferenceList("publications", Arrays.asList(new Object[]
            {pub.getIdentifier()})));
        writer.store(ItemHelper.convert(item));

        ReferenceList refs = pub.getCollection("authors");
        if (refs == null) {
            refs = new ReferenceList();
            refs.setName("authors");
            pub.addCollection(refs);
        }
        if (!refs.getRefIds().contains(item.getIdentifier())) {
            refs.addRefId(item.getIdentifier());
        }
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

