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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.ItemHelper;

/**
 * DataConverter to parse an INPARANOID Orthologue/Paralogue "sqltable" data file into Items
 * @author Mark Woodbridge
 */
public class OrthologueConverter extends FileConverter
{
    protected static final String ORTHOLOGUE_NS = "http://flymine.org/model/genomic#";

    protected Map proteins = new HashMap();
    protected Item db, analysis;
    protected int id = 0;
    
    /**
     * Constructor
     * @param reader Reader of input data in 5-column tab delimited format
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected OrthologueConverter(BufferedReader reader, ItemWriter writer)
        throws ObjectStoreException {
        super(reader, writer);
        setupItems();
    }

    /**
     * @see DataConverter#process
     */
    public void process() throws Exception {
        try {
            String line, species = null, oldIndex = null;
            Item protein = null;

            while ((line = reader.readLine()) != null) {
                String[] array = line.split("\t");
                String index = array[0];
                if (!index.equals(oldIndex)) {
                    oldIndex = index;
                    species = array[2];
                    protein = newProtein(array[4]);
                    continue;
                }

                Item newProtein = newProtein(array[4]);

                Item item = newItem(species.equals(array[2]) ? "Paralogue" : "Orthologue");
                item.addReference(new Reference("subject", newProtein.getIdentifier()));
                item.addReference(new Reference("object", protein.getIdentifier()));
                item.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                    {db.getIdentifier(), newResult(array[3]).getIdentifier()})));
                writer.store(ItemHelper.convert(item));
            
                if (!species.equals(array[2])) {
                    species = array[2];
                    protein = newProtein;
                }
            }
        } finally {
            writer.close();
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
        item.setClassName(ORTHOLOGUE_NS + className);
        item.setImplementations("");
        return item;
    }
    
    /**
     * Convenience method to create and cache proteins by SwissProt id
     * @param swissProtId SwissProt identifier for the new Protein
     * @return a new protein Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newProtein(String swissProtId) throws ObjectStoreException {
        if (proteins.containsKey(swissProtId)) {
            return (Item) proteins.get(swissProtId);
        }
        Item item = newItem("Protein");
        item.addAttribute(new Attribute("swissprotId", swissProtId));
        writer.store(ItemHelper.convert(item));
        proteins.put(swissProtId, item);
        return item;
    }

    /**
     * Convenience method to create a new analysis result
     * @param confidence the INPARANOID confidence for the result
     * @return a new INPARANOIDResult Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newResult(String confidence) throws ObjectStoreException {
        Item item = newItem("INPARANOIDResult");
        item.addAttribute(new Attribute("confidence", confidence));
        item.addReference(new Reference("analysis", analysis.getIdentifier()));
        writer.store(ItemHelper.convert(item));
        return item;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        Item pub = newItem("Publication");
        pub.addAttribute(new Attribute("title", "Automatic clustering of orthologs and"
                                        + "in-paralogs from pairwise species comparisons"));
        pub.addAttribute(new Attribute("journal", "Journal of Molecular Biology"));
        pub.addAttribute(new Attribute("volume", "314"));
        pub.addAttribute(new Attribute("issue", "5"));
        pub.addAttribute(new Attribute("year", "2001"));
        pub.addAttribute(new Attribute("pages", "1041-1052"));
        Item author1 = newItem("Author"), author2 = newItem("Author"), author3 = newItem("Author");
        author1.addAttribute(new Attribute("name", "Maido Remm"));
        author2.addAttribute(new Attribute("name", "Christian E. V. Storm"));
        author3.addAttribute(new Attribute("name", "Erik L. L. Sonnhammer"));
        ReferenceList authors = new ReferenceList("authors", Arrays.asList(new Object[]
            {author1.getIdentifier(), author2.getIdentifier(), author3.getIdentifier()}));
        pub.addCollection(authors);

        analysis = newItem("INPARANOIDAnalysis");
        analysis.addAttribute(new Attribute("algorithm", "INPARANOID"));
        analysis.addReference(new Reference("publication", pub.getIdentifier()));
        
        db = newItem("Database");
        db.addAttribute(new Attribute("title", "INPARANOID"));
        db.addAttribute(new Attribute("url", "http://inparanoid.cgb.ki.se"));
        
        List toStore = Arrays.asList(new Object[] {db, analysis, author1, author2, author3, pub});
        for (Iterator i = toStore.iterator(); i.hasNext();) {
            writer.store(ItemHelper.convert((Item) i.next()));
        }
    }
}

