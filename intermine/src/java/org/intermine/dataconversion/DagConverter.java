package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.DagTerm;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * Processes list of root DagTerms to produce data
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class DagConverter extends DataConverter
{
    protected static final String ONTOLOGY_RELATION =
        "http://www.flymine.org/model/genomic#OntologyRelation";
    protected static final String ONTOLOGY = "http://www.flymine.org/model/genomic#Ontology";

    protected String dagFilename;
    protected String termClass;
    protected int uniqueId = 0;
    protected Map nameToTerm = new HashMap();
    protected Item ontology;

    /**
     * Constructor for this class.
     *
     * @param writer an ItemWriter used to handle the resultant Items
     * @param dagFilename the name of the DAG file
     * @param dagName the title of the dag, as present in any static data
     * @param termClass the class of the Term
     */
    public DagConverter(ItemWriter writer, String dagFilename, String dagName, String termClass) {
        super(writer);
        this.dagFilename = dagFilename;
        this.termClass = termClass;

        ontology = new Item();
        ontology.setIdentifier("0_" + (uniqueId++));
        ontology.setClassName(ONTOLOGY);
        ontology.setImplementations("");
        ontology.addAttribute(new Attribute("title", dagName));
    }

    /**
     * Process every DAG term and output it as a Item.
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        nameToTerm = new HashMap();
        process(new DagParser().processForLabellingOntology(new FileReader(dagFilename)));
    }

    /**
     * Convert DagTerms into Items and relation Items, and write them to the ItemWriter
     *
     * @param rootTerms a Collection of DagTerms
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void process(Collection rootTerms) throws ObjectStoreException {
        for (Iterator i = rootTerms.iterator(); i.hasNext();) {
            process((DagTerm) i.next());
        }
        writer.store(ItemHelper.convert(ontology));
        for (Iterator i = nameToTerm.values().iterator(); i.hasNext();) {
            writer.store(ItemHelper.convert((Item) i.next()));
        }
    }

    /**
     * Convert a DagTerm into an Item and relation Items, and write the relations to the writer.
     *
     * @param term a DagTerm
     * @return an Item representing the term
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected Item process(DagTerm term) throws ObjectStoreException {
        String termId = (term.getId() == null ? term.getName() : term.getId());
        Item item = (Item) nameToTerm.get(termId);
        if (item == null) {
            item = new Item();
            item.setIdentifier("0_" + (uniqueId++));
            item.setClassName(termClass);
            item.setImplementations("");
            item.addAttribute(new Attribute("name", term.getName()));
            item.addReference(new Reference("ontology", ontology.getIdentifier()));
            if (term.getId() != null) {
                item.addAttribute(new Attribute("identifier", term.getId()));
            }
            ReferenceList parentRelations = new ReferenceList("parentRelations");
            item.addCollection(parentRelations);
            ReferenceList childRelations = new ReferenceList("childRelations");
            item.addCollection(childRelations);
            nameToTerm.put(termId, item);
            Iterator iter = term.getChildren().iterator();
            while (iter.hasNext()) {
                DagTerm subTerm = (DagTerm) iter.next();
                Item subItem = process(subTerm);
                relate(item, subItem, "is_a");
            }
            iter = term.getComponents().iterator();
            while (iter.hasNext()) {
                DagTerm subTerm = (DagTerm) iter.next();
                Item subItem = process(subTerm);
                relate(item, subItem, "part_of");
            }
        } else {
            if ((!term.getName().equals(item.getAttribute("name").getValue()))
                    || ((item.getAttribute("identifier") == null) && (term.getId() != null))
                    || ((item.getAttribute("identifier") != null)
                        && (!item.getAttribute("identifier").getValue().equals(term.getId())))) {
                throw new IllegalArgumentException("Dag is invalid - terms (" + term.getName()
                        + ", " + term.getId() + ") and (" + item.getAttribute("name").getValue()
                        + ", " + (item.getAttribute("identifier") == null ? "null"
                            : item.getAttribute("identifier").getValue()) + ") clash");
            }
        }
        return item;
    }

    /**
     * Create (and store) a relation between two DagTerms.
     *
     * @param item the parent item
     * @param subItem the child item
     * @param type the String type of the relationship (is_a, part_of, etc)
     * @throws ObjectStoreException if an error occurs while writing to the ItemWriter
     */
    protected void relate(Item item, Item subItem, String type) throws ObjectStoreException {
        Item relation = new Item();
        relation.setIdentifier("0_" + (uniqueId++));
        relation.setClassName(ONTOLOGY_RELATION);
        relation.setImplementations("");
        relation.addAttribute(new Attribute("type", type));
        relation.addReference(new Reference("childTerm", subItem.getIdentifier()));
        relation.addReference(new Reference("parentTerm", item.getIdentifier()));
        ReferenceList parentRelations = subItem.getCollection("parentRelations");
        parentRelations.addRefId(relation.getIdentifier());
        ReferenceList childRelations = item.getCollection("childRelations");
        childRelations.addRefId(relation.getIdentifier());
        writer.store(ItemHelper.convert(relation));
    }
}
