package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.ontology.OboParser;
import org.intermine.bio.ontology.OboRelation;
import org.intermine.bio.ontology.OboTerm;
import org.intermine.bio.ontology.OboTermSynonym;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * Convert tree of OboTerms into Items.
 *
 * @author Thomas Riley
 * @see DagConverter
 */
public class OboConverter extends DataConverter
{
    private static final Logger LOG = Logger.getLogger(DataConverter.class);

    protected String dagFilename;
    protected String termClass;
    protected int uniqueId = 0;
    protected int uniqueSynId = 0;
    protected Collection<OboTerm> oboTerms;
    protected List<OboRelation> oboRelations;
    protected Map<String, Item> nameToTerm = new HashMap<String, Item>();
    protected Map<OboTermSynonym, Item> synToItem = new HashMap<OboTermSynonym, Item>();
    protected Item ontology;
    private boolean createRelations = true;

    /**
     * Constructor for this class.
     *
     * @param writer an ItemWriter used to handle the resultant Items
     * @param model the Model
     * @param dagFilename the name of the DAG file
     * @param dagName the title of the dag, as present in any static data
     * @param url the URL of the source of this ontology
     * @param termClass the class of the Term
     */
    public OboConverter(ItemWriter writer, Model model, String dagFilename, String dagName,
                        String url, String termClass) {
        super(writer, model);
        this.dagFilename = dagFilename;
        this.termClass = termClass;

        ontology = createItem("Ontology");
        ontology.addAttribute(new Attribute("name", dagName));
        ontology.addAttribute(new Attribute("url", url));
    }

    /**
     * Set to false to prevent storing OntologyRelation objects that include the relationship types
     * between terms.
     * @param createrelations property to parse
     */
    public void setCreaterelations(String createrelations) {
        if ("true".equals(createrelations)) {
            this.createRelations = true;
        } else {
            this.createRelations = false;
        }
    }

    /**
     * Process every DAG term and output it as a Item.
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        nameToTerm = new HashMap<String, Item>();
        synToItem = new HashMap<OboTermSynonym, Item>();
        OboParser parser = new OboParser();
        parser.processOntology(new FileReader(new File(dagFilename)));
        parser.processRelations(dagFilename);
        oboTerms = parser.getOboTerms();
        oboRelations = parser.getOboRelations();
        storeItems();
    }

    /**
     * Convert DagTerms into Items and relation Items, and write them to the ItemWriter
     *
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void storeItems() throws ObjectStoreException {
        long startTime = System.currentTimeMillis();
        store(ontology);
        for (OboTerm term : oboTerms) {
            process(term);
        }
        for (OboRelation oboRelation : oboRelations) {
            processRelation(oboRelation);
        }
        for (Item termItem: nameToTerm.values()) {
            store(termItem);
        }
        for (Item synItem : synToItem.values()) {
            store(synItem);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Ran storeItems, took: " + timeTaken + " ms");
    }

    /**
     * @param oboTerms the oboTerms to set
     */
    public void setOboTerms(Collection<OboTerm> oboTerms) {
        this.oboTerms = oboTerms;
    }

    /**
     * @param oboRelations the OboRelations to set
     */
    public void setOboRelations(List<OboRelation> oboRelations) {
        this.oboRelations = oboRelations;
    }

    /**
     * Convert a OboTerm into an Item and relation Items, and write the relations to the writer.
     *
     * @param term a DagTerm
     * @return an Item representing the term
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected Item process(OboTerm term) throws ObjectStoreException {
        if (term.isObsolete()) {
            LOG.info("Not processing obsolete OBO term: " + term.getId() + " " + term.getName());
            return null;
        }
        String termId = (term.getId() == null ? term.getName() : term.getId());
        Item item = nameToTerm.get(termId);
        if (item == null) {
            item = createItem(termClass);
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
    protected void configureItem(String termId, Item item, OboTerm term)
        throws ObjectStoreException {
        item.addAttribute(new Attribute("name", term.getName()));
        item.addReference(new Reference("ontology", ontology.getIdentifier()));
        if (term.getId() != null) {
            item.addAttribute(new Attribute("identifier", term.getId()));
        }
        for (OboTermSynonym syn : term.getSynonyms()) {
            Item synItem = synToItem.get(syn);
            if (synItem == null) {
                synItem = createItem("OntologyTermSynonym");
                synToItem.put(syn, synItem);
                configureSynonymItem(syn, synItem, term);
            }
            item.addToCollection("synonyms", synItem);
        }
        OboTerm oboterm = term;
        if (!StringUtils.isEmpty(oboterm.getNamespace())) {
            item.setAttribute("namespace", oboterm.getNamespace());
        }
        if (!StringUtils.isEmpty(oboterm.getDescription())) {
            item.setAttribute("description", oboterm.getDescription());
        }
        item.setAttribute("obsolete", "" + oboterm.isObsolete());
        // Term is its own parent
        item.addToCollection("parents", item);
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
    protected void configureSynonymItem(OboTermSynonym syn, Item item, OboTerm term)
        throws ObjectStoreException {
        item.setAttribute("name", syn.getName());
        item.setAttribute("type", syn.getType());
    }

    /**
     * Process and store OboRelations
     * @param oboRelation the relation to process
     * @throws ObjectStoreException if problem storing
     */
    protected void processRelation(OboRelation oboRelation)
        throws ObjectStoreException {
        // create the relation item
        if (nameToTerm.get(oboRelation.getParentTermId()) != null
            && nameToTerm.get(oboRelation.getChildTermId()) != null) {

            // add parent to term for easier querying in webapp
            nameToTerm.get(oboRelation.getChildTermId()).addToCollection("parents",
                    nameToTerm.get(oboRelation.getParentTermId()));

            if (createRelations) {
                Item relation = createItem("OntologyRelation");
                relation.setReference("parentTerm", nameToTerm
                        .get(oboRelation.getParentTermId()));
                relation.setReference("childTerm", nameToTerm.get(oboRelation.getChildTermId()));
                relation.setAttribute("relationship", oboRelation.getRelationship().getName());
                relation.setAttribute("direct", Boolean.toString(oboRelation.isDirect()));
                relation.setAttribute("redundant", Boolean.toString(oboRelation.isRedundant()));

                // Set the reverse reference
                nameToTerm.get(oboRelation.getParentTermId())
                    .addToCollection("relations", relation);
                nameToTerm.get(oboRelation.getChildTermId())
                    .addToCollection("relations", relation);
                store(relation);
            }
        } else {
            LOG.info("GOTerm id not found for relation " + oboRelation.getParentTermId() + " "
                     + oboRelation.getChildTermId());
        }
    }
}
