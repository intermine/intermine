package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;

/**
 * DataConverter to parse an INPARANOID Orthologue/Paralogue "sqltable" data file into Items
 * @author Mark Woodbridge
 */
public class InparanoidConverter extends FileConverter
{
    protected static final String ORTHOLOGUE_NS = "http://www.flymine.org/model/genomic#";

    protected Map proteins = new HashMap();
    protected Item db, analysis;
    protected Map ids = new HashMap();
    protected Map organisms = new LinkedHashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public InparanoidConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
        setupItems();
    }

    /**
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        String line, species = null, oldIndex = null;
        Item protein = null;

        BufferedReader br = new BufferedReader(reader);
        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t");
            String index = array[0];
            if (!index.equals(oldIndex)) {
                oldIndex = index;
                species = array[2];
                protein = newProtein(array[4], getOrganism(species));
                continue;
            }

            Item newProtein = newProtein(array[4], getOrganism(array[2]));
            Item result = newResult(array[3]);

            // create two organisms with subjectProtein and objectProtein reversed
            Item item = newItem(species.equals(array[2]) ? "Paralogue" : "Orthologue");
            item.addReference(new Reference("subjectProtein", newProtein.getIdentifier()));
            item.addReference(new Reference("objectProtein", protein.getIdentifier()));
            item.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {db.getIdentifier(), result.getIdentifier()})));
            writer.store(ItemHelper.convert(item));

            item = newItem(species.equals(array[2]) ? "Paralogue" : "Orthologue");
            item.addReference(new Reference("subjectProtein", protein.getIdentifier()));
            item.addReference(new Reference("objectProtein", newProtein.getIdentifier()));
            item.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {db.getIdentifier(), result.getIdentifier()})));
            writer.store(ItemHelper.convert(item));

            if (!species.equals(array[2])) {
                species = array[2];
                protein = newProtein;
            }
        }
    }

    /**
     * @see FileConverter#close
     */
    public void close() throws ObjectStoreException {
        store(organisms.values());
        store(proteins.values());
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item newItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + newId(className));
        item.setClassName(ORTHOLOGUE_NS + className);
        item.setImplementations("");
        return item;
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
     * Convenience method to create and cache proteins by SwissProt id
     * @param swissProtId SwissProt identifier for the new Protein
     * @param organism the Organism for this protein
     * @return a new protein Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newProtein(String swissProtId, Item organism) throws ObjectStoreException {
        if (proteins.containsKey(swissProtId)) {
            return (Item) proteins.get(swissProtId);
        }
        Item item = newItem("Protein");
        item.addAttribute(new Attribute("primaryAccession", swissProtId));
        item.addReference(new Reference("organism", organism.getIdentifier()));
        proteins.put(swissProtId, item);
        return item;
    }

    /**
     * Convenience method to create a new analysis result
     * @param confidence the INPARANOID confidence for the result
     * @return a new ComputationalResult Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newResult(String confidence) throws ObjectStoreException {
        Item item = newItem("ComputationalResult");
        item.addAttribute(new Attribute("confidence", confidence));
        item.addReference(new Reference("analysis", analysis.getIdentifier()));
        writer.store(ItemHelper.convert(item));
        return item;
    }

    private Item getOrganism(String abbrev) {
        Item organism = (Item) organisms.get(abbrev);
        if (organism == null) {
            organism = newItem("Organism");
            organism.addAttribute(new Attribute("abbreviation", abbrev));
            organisms.put(abbrev, organism);
        }
        return organism;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        Item pub = newItem("Publication");
        pub.addAttribute(new Attribute("pubMedId", "11743721"));

        analysis = newItem("ComputationalAnalysis");
        analysis.addAttribute(new Attribute("algorithm", "INPARANOID"));
        analysis.addReference(new Reference("publication", pub.getIdentifier()));

        db = newItem("Database");
        db.addAttribute(new Attribute("title", "INPARANOID"));

        List toStore = Arrays.asList(new Object[] {db, analysis, pub});
        for (Iterator i = toStore.iterator(); i.hasNext();) {
            writer.store(ItemHelper.convert((Item) i.next()));
        }
    }

    private void addToCollection(Item item, String colName, String refid) {
        ReferenceList col = null;
        if (item.hasCollection(colName)) {
            col = item.getCollection(colName);
        } else {
            col = new ReferenceList();
            col.setName(colName);
        }
        col.addRefId(refid);
        item.addCollection(col);
    }
}

