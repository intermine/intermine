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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

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
public class OrthologueConverter extends FileConverter
{
    protected static final String ORTHOLOGUE_NS = "http://www.flymine.org/model/genomic#";

    protected int id = 0;
    protected Map sources = new HashMap();
    protected Map analyses = new HashMap();
    protected Map ids = new HashMap();
    protected Item organism1;
    protected Item organism2;
    protected Map genes = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public OrthologueConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
    }

    /**
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        if (id > 0) {
            throw new RuntimeException("Cannot run process() more than one");
        }

        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();

        organism1 = createOrganism(param1);
        organism2 = createOrganism(param2);

        // throw an exception if organisms not set
        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t");
            Set items = new HashSet();

            Item gene1 = createGene(array[0], organism1);
            Item gene2 = createGene(array[1], organism2);
            Item result = createResult(getAnalysis(array[2], array[3], array[4]),
                                       getSource(array[5]));
            items.add(ItemHelper.convert(result));
            Item orth1 = createOrthologue(gene1, gene2, result, getSource(array[5]));
            items.add(ItemHelper.convert(orth1));
            Item orth2 = createOrthologue(gene2, gene1, result, getSource(array[5]));
            items.add(ItemHelper.convert(orth2));
            addToCollection(gene1, "objects", orth2.getIdentifier());
            addToCollection(gene1, "subjects", orth1.getIdentifier());
            addToCollection(gene2, "objects", orth1.getIdentifier());
            addToCollection(gene2, "subjects", orth2.getIdentifier());

            writer.storeAll(items);
        }

        Set extras = new HashSet();
        Iterator iter = genes.values().iterator();
        while (iter.hasNext()) {
            extras.add(ItemHelper.convert((Item) iter.next()));
        }
        iter = sources.values().iterator();
        while (iter.hasNext()) {
            extras.add(ItemHelper.convert((Item) iter.next()));
        }
        iter = analyses.values().iterator();
        while (iter.hasNext()) {
            extras.add(ItemHelper.convert((Item) iter.next()));
        }
        extras.add(ItemHelper.convert(organism1));
        extras.add(ItemHelper.convert(organism2));
        writer.storeAll(extras);
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

    private Item createOrganism(String abbrev) {
        if (abbrev == null) {
            throw new IllegalArgumentException("Cannot create an organism with no name");
        }
        Item organism = newItem("Organism");
        organism.addAttribute(new Attribute("abbreviation", abbrev));
        return organism;
    }

    private Item createOrthologue(Item gene1, Item gene2, Item result, Item source) {
        Item orth = newItem("Orthologue");
        orth.addReference(new Reference("object", gene1.getIdentifier()));
        orth.addReference(new Reference("subject", gene2.getIdentifier()));
        orth.addCollection(new ReferenceList("evidence", new ArrayList(Arrays.asList(
                                 new Object[] {result.getIdentifier(), source.getIdentifier()}))));
        return orth;
    }

    private Item createGene(String name, Item organism) {
        String key = name + organism.getIdentifier();
        Item gene = (Item) genes.get(key);
        if (gene == null) {
            gene = newItem("Gene");
            gene.addAttribute(new Attribute("identifier", name));
            gene.addReference(new Reference("organism", organism.getIdentifier()));
            genes.put(key, gene);
        }
        return gene;
    }

    private Item getAnalysis(String type, String algorithm, String method) {
        Item analysis = (Item) analyses.get(algorithm);
        if (analysis == null) {
            analysis = newItem("ComputationalAnalysis");
            analysis.addAttribute(new Attribute("description", method));
            analysis.addAttribute(new Attribute("algorithm", algorithm));
            analyses.put(algorithm, analysis);
        }
        return analysis;
    }

    private Item getSource(String name) {
        if (name.equals("Ensembl Database")) {
            name = "ensembl";
        }
        Item source = (Item) sources.get(name);
        if (source == null) {
            source = newItem("Database");
            source.addAttribute(new Attribute("title", name));
            sources.put(name, source);
        }
        return source;
    }

    private Item createResult(Item analysis, Item source) {
        Item result = newItem("ComputationalResult");
        result.addReference(new Reference("analysis", analysis.getIdentifier()));
        result.addReference(new Reference("source", source.getIdentifier()));
        return result;
    }

    private void addToCollection(Item item, String colName, String toAdd) {
        ReferenceList col = item.getCollection(colName);
        if (col != null) {
            col.addRefId(toAdd);
        } else {
            col = new ReferenceList(colName, new ArrayList(Collections.singletonList(toAdd)));
            item.addCollection(col);
        }
    }

}

