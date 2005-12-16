package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;

/**
 * DataConverter to parse an INPARANOID Orthologue/Paralogue "sqltable" data file into Items
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class InparanoidConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Map bioEntities = new HashMap();
    protected Item db, analysis;
    protected Map ids = new HashMap();
    protected Map organisms = new LinkedHashMap();
    protected ItemFactory itemFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public InparanoidConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        setupItems();
    }

    /**
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        String line, species = null, oldIndex = null;
        Item bio = null;

        BufferedReader br = new BufferedReader(reader);
        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t");
            String index = array[0];
            if (!index.equals(oldIndex)) {
                oldIndex = index;
                species = array[2];
                bio = newBioEntity(array[4], getOrganism(species));
                continue;
            }

            Item newBio = newBioEntity(array[4], getOrganism(array[2]));
            Item result = newResult(array[3]);

            // if BioEntity is a Translation then need to set [object|subject]Translation,
            // if it is a Gene then set [object|subject]
            String bioRef = (bio.getClassName().equals(GENOMIC_NS + "Gene")) ? "" : "Translation";
            String newBioRef =
                (newBio.getClassName().equals(GENOMIC_NS + "Gene")) ? "" : "Translation";

            // create two organisms with subjectTranslation and objectTranslation reversed
            Item item = createItem(species.equals(array[2]) ? "Paralogue" : "Orthologue");
            item.setReference("subject" + newBioRef, newBio.getIdentifier());
            item.setReference("object" + bioRef, bio.getIdentifier());
            item.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {db.getIdentifier(), result.getIdentifier()})));
            writer.store(ItemHelper.convert(item));

            item = createItem(species.equals(array[2]) ? "Paralogue" : "Orthologue");
            item.setReference("subject" + bioRef, bio.getIdentifier());
            item.setReference("object" + newBioRef, newBio.getIdentifier());
            item.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {db.getIdentifier(), result.getIdentifier()})));
            writer.store(ItemHelper.convert(item));

            if (!species.equals(array[2])) {
                species = array[2];
                bio = newBio;
            }
        }
    }

    /**
     * @see FileConverter#close
     */
    public void close() throws ObjectStoreException {
        store(organisms.values());
        store(bioEntities.values());
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
     * @param identifier identifier for the new Gene/Protein
     * @param organism the Organism for this protein
     * @return a new Gene/Protein Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newBioEntity(String identifier, Item organism) throws ObjectStoreException {
        if (bioEntities.containsKey(identifier)) {
            return (Item) bioEntities.get(identifier);
        }
        Item item = null;
        if ("CE".equals(organism.getAttribute("abbreviation").getValue())) {
            item = createItem("Gene");
        } else {
            item = createItem("Translation");
        }
        item.setAttribute("identifier", identifier);
        item.setReference("organism", organism.getIdentifier());
        bioEntities.put(identifier, item);

        return item;
    }

    /**
     * Convenience method to create a new analysis result
     * @param confidence the INPARANOID confidence for the result
     * @return a new ComputationalResult Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newResult(String confidence) throws ObjectStoreException {
        Item item = createItem("ComputationalResult");
        item.setAttribute("confidence", confidence);
        item.setReference("analysis", analysis.getIdentifier());
        writer.store(ItemHelper.convert(item));
        return item;
    }

    private Item getOrganism(String abbrev) {
        if (abbrev.startsWith("ens") || abbrev.startsWith("mod")) {
            abbrev = abbrev.substring(3);
        }
        if (abbrev.length() != 2) {
            throw new IllegalArgumentException("invalid organism abbreviation: " + abbrev);
        }
        // HACK Inparanoid files use abbreviation AG to refer to Anopheles gambiae PEST,
        // we use AGP for this.
        if ("AG".equals(abbrev)) {
            abbrev = "AGP";
        }

        Item organism = (Item) organisms.get(abbrev);
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("abbreviation", abbrev);
            organisms.put(abbrev, organism);
        }
        return organism;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        Item pub = createItem("Publication");
        pub.setAttribute("pubMedId", "11743721");

        analysis = createItem("ComputationalAnalysis");
        analysis.setAttribute("algorithm", "INPARANOID");
        analysis.setReference("publication", pub.getIdentifier());

        db = createItem("DataSet");
        db.setAttribute("title", "InParanoid data set");

        List toStore = Arrays.asList(new Object[] {db, analysis, pub});
        for (Iterator i = toStore.iterator(); i.hasNext();) {
            writer.store(ItemHelper.convert((Item) i.next()));
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


}

