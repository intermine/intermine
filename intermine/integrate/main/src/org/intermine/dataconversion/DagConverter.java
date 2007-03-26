package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.DagTerm;
import org.intermine.ontology.DagTermSynonym;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
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
    protected static final String ONTOLOGY_TERM_SYNONYM =
        "http://www.flymine.org/model/genomic#OntologyTermSynonym";
    
    protected String dagFilename;
    protected String termClass;
    protected int uniqueId = 0;
    protected int uniqueSynId = 0;
    protected Map nameToTerm = new HashMap();
    protected Map synToItem = new HashMap();
    protected Item ontology;
    protected ItemFactory itemFactory = ItemFactory.NULL_MODEL_ITEM_FACTORY;

    /**
     * Constructor for this class.
     *
     * @param writer an ItemWriter used to handle the resultant Items
     * @param dagFilename the name of the DAG file
     * @param dagName the title of the dag, as present in any static data
     * @param url the URL of the source of this ontology
     * @param termClass the class of the Term
     */
    public DagConverter(ItemWriter writer, String dagFilename, String dagName, String url,
                        String termClass) {
        super(writer);
        this.dagFilename = dagFilename;
        this.termClass = termClass;

        ontology = itemFactory.makeItem("0_" + (uniqueId++));
        ontology.setClassName(ONTOLOGY);
        ontology.setImplementations("");
        ontology.addAttribute(new Attribute("title", dagName));
        ontology.addAttribute(new Attribute("url", url));
    }

    /**
     * Process every DAG term and output it as a Item.
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        nameToTerm = new HashMap();
        synToItem = new HashMap();
        process(findRootTerms(new File(dagFilename)));
    }
    
    /**
     * Parse root DagTerms from the input ontology.
     * 
     * @param inputFile input dag file
     * @return collection of root DagTerms
     * @throws IOException if something goes 
     */
    protected Collection findRootTerms(File inputFile) throws IOException {
        return new DagParser().processForLabellingOntology(new FileReader(inputFile));
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
        for (Iterator i = synToItem.values().iterator(); i.hasNext();) {
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
            item = itemFactory.makeItem("0_" + (uniqueId++));
            item.setClassName(termClass);
            item.setImplementations("");
            nameToTerm.put(termId, item);
            configureItem(termId, item, term);
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
     * Set up attributes and references for the Item created from a DagTerm. Subclasses
     * can override this method to perform extra setup, for example by casting the
     * DagTerm to some someclass and retrieving extra attributes. Subclasses should call this
     * inherited method first. This method will call process() for each child/component term,
     * resulting in recursion.
     * 
     * @param termId the term id
     * @param item the Item created (see termClass field for type)
     * @param term the source DagTerm
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void configureItem(String termId, Item item, DagTerm term)
        throws ObjectStoreException {
        item.addAttribute(new Attribute("name", term.getName()));
        item.addReference(new Reference("ontology", ontology.getIdentifier()));
        if (term.getId() != null) {
            item.addAttribute(new Attribute("identifier", term.getId()));
        }
        ReferenceList parentRelations = new ReferenceList("parentRelations");
        item.addCollection(parentRelations);
        ReferenceList childRelations = new ReferenceList("childRelations");
        item.addCollection(childRelations);
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
        iter = term.getSynonyms().iterator();
        while (iter.hasNext()) {
            DagTermSynonym syn = (DagTermSynonym) iter.next();
            Item synItem = (Item) synToItem.get(syn);
            if (synItem == null) {
                synItem = itemFactory.makeItem("1_" + (uniqueSynId++));
                synItem.setClassName(ONTOLOGY_TERM_SYNONYM);
                synItem.setImplementations("");
                synToItem.put(syn, synItem);
                configureSynonymItem(syn, synItem, term);
            }
            addToCollection(item, "synonyms", synItem);
        }
    }
    
    /**
     * Set up attributes and references for the Item created from a DagTermSynonym. Subclasses
     * can override this method to perform extra setup, for example by casting the
     * DagTermSynonym to some someclass and retrieving extra attributes. Subclasses should call this
     * inherited method first.
     * 
     * @param syn the DagTermSynonym object (or a subclass of)
     * @param item the Item created to store the synonym
     * @param term the source DagTerm
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void configureSynonymItem(DagTermSynonym syn, Item item, DagTerm term)
        throws ObjectStoreException {
        item.addAttribute(new Attribute("name", syn.getName()));
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
        Item relation = itemFactory.makeItem("0_" + (uniqueId++));
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
