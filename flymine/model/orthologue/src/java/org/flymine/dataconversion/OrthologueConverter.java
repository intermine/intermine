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

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

    /**
     * Constructor
     * @param reader Reader of input data in 5-column tab delimited format
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected OrthologueConverter(BufferedReader reader, ItemWriter writer)
        throws ObjectStoreException {
        super(reader, writer);
    }

    public void setOrganism1(String name) {
        organism1 = createOrganism(name);
    }

    public void setOrganism2(String name) {
        organism2 = createOrganism(name);
    }

    private Item createOrganism(String name) {
        Item organism = newItem("Organism");
        organism.addAttribute(new Attribute("name", name));
        return organism;
    }

    /**
     * @see DataConverter#process
     */
    public void process() throws Exception {
        try {
            String line;
            // throw an exception if organisms not set
            while ((line = reader.readLine()) != null) {
                String[] array = line.split("\t");
                Set items = new HashSet();

                Item gene1 = createGene(array[0], organism1);
                Item gene2 = createGene(array[1], organism2);
                Item result = createResult(getAnalysis(array[2], array[3]), getSource(array[4]));
                items.add(ItemHelper.convert(result));
                Item orth1 = createOrthologue(gene1, gene2, result, getSource(array[4]));
                items.add(ItemHelper.convert(orth1));
                Item orth2 = createOrthologue(gene2, gene1, result, getSource(array[4]));
                items.add(ItemHelper.convert(orth2));
                gene1.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton(orth2.getIdentifier()))));
                gene1.addCollection(new ReferenceList("subjects", new ArrayList(Collections.singleton(orth1.getIdentifier()))));
                gene2.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton(orth1.getIdentifier()))));
                gene2.addCollection(new ReferenceList("subjects", new ArrayList(Collections.singleton(orth2.getIdentifier()))));

                items.add(ItemHelper.convert(gene1));
                items.add(ItemHelper.convert(gene2));

                writer.storeAll(items);
            }

            Set extras = new HashSet();
            Iterator iter = sources.values().iterator();
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

    private Item createOrthologue(Item gene1, Item gene2, Item result, Item source) {
        Item orth = newItem("Orthologue");
        orth.addReference(new Reference("object", gene1.getIdentifier()));
        orth.addReference(new Reference("subject", gene2.getIdentifier()));
        orth.addCollection(new ReferenceList("evidence", new ArrayList(Arrays.asList(
                                 new Object[] {result.getIdentifier(), source.getIdentifier()}))));
        return orth;
    }

    private Item createGene(String name, Item organism) {
        Item gene = newItem("Gene");
        gene.addAttribute(new Attribute("name", name));
        gene.addReference(new Reference("organism", organism.getIdentifier()));
        return gene;
    }

    private Item getAnalysis(String type, String method) {
        Item analysis = (Item) analyses.get(method);
        if (analysis == null) {
            analysis = newItem("ComputationalAnalysis");
            analysis.addAttribute(new Attribute("description", method));
            analyses.put(method, analysis);
        }
        return analysis;
    }

    private Item getSource(String name) {
        if (name.equals("Ensembl Database")) {
            name = "Ensembl";
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

}

