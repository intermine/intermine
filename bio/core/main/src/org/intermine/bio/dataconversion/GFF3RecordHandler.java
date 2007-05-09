package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.Attribute;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;

/**
 * Permits specific operations to be performed when processing an line of GFF3.
 * GFF3Converter sets the core items created in the handler, the handler can
 * alter, remove from or add to the set of items.
 *
 * @author Richard Smith
 */
public class GFF3RecordHandler
{
    protected Map items = new HashMap();
    private Item sequence;
    protected Item analysis;
    private Model tgtModel;
    private ItemFactory itemFactory;
    private Item organism;
    private ReferenceList evidenceReferenceList = new ReferenceList("evidence");

    protected Map tgtSeqs = new HashMap();
    private Item tgtOrganism;
    private Reference tgtOrgRef;
    protected Item tgtSequence;
    private int itemid = 0;
    private Item dataSource;
    private Item dataSet;

    /**
     * Construct with the model to create items in (for type checking).
     * @param tgtModel the model for which items will be created
     */
    public GFF3RecordHandler(Model tgtModel) {
        this.tgtModel = tgtModel;
    }

    /**
     * Method to perform additional operations for a GFF3Record.  Access to core items
     * is possible via getters() (sequence and computational analysis items cannot be
     * altered but are available read-only).  Additional items to store can be placed
     * in the items map, keys can be anything except a string starting with '_'.
     * @param record the GFF line being processed
     */
    public void process(GFF3Record record) {
        // empty
    }

    /**
     * Set the Map of GFF identifiers to Item identifier.  The Map should be used to get/add any
     * item identifiers for features used so that multiple Items aren't created for the same
     * feature.
     * @param identifierMap map from GFF ID to item identifier for all features
     */
    public void setIdentifierMap(Map identifierMap) {
        // empty
     }

    /**
     * Return the Model that was passed to the constructor.
     * @return the Model
     */
    public Model getTargetModel() {
        return tgtModel;
    }

    /**
     * Set sequence item created for this record, should not be edited in handler.
     * @param sequence the sequence item
     */
    public void setSequence(final Item sequence) {
        this.sequence = sequence;
    }

    /**
     * Return the sequence Item set by setSequence()
     * @return the sequence Item
     */
    protected Item getSequence() {
        return sequence;
    }

    /**
     * Set organism item, this is global across record handler and final
     * @param organism the organism item
     */
    public void setOrganism(final Item organism) {
        this.organism = organism;
    }

    /**
     * Return the organism Item set by setOrganism()
     * @return the organism Item
     */
    protected Item getOrganism() {
        return organism;
    }

    /**
     * Set the location item for this record.
     * @param location the location item
     */
    public void setLocation(Item location) {
        items.put("_location", location);
    }

    /**
     * Return the location Item set by setLocation()
     * @return the location Item
     */
    protected Item getLocation() {
        return (Item) items.get("_location");
    }

    /**
     * Set the feature item for this record.
     * @param feature the feature item
     */
    public void setFeature(Item feature) {
        items.put("_feature", feature);
    }

    /**
     * Return the feature Item set by setFeature()
     * @return the feature Item
     */
    protected Item getFeature() {
        return (Item) items.get("_feature");
    }

    /**
     * Remove the feature item that was set with setFeature().
     */
    protected void removeFeature() {
        items.remove("_feature");
    }

    /**
     * Set the ComputationalAnalysis item created for this record, should not be edited in handler.
     * @param analysis the ComputationalAnalysis item
     */
    public void setAnalysis(final Item analysis) {
        this.analysis = analysis;
    }

    /**
     * Add an Evidence Item to this handler, to be retrieved later with getEvidenceReferenceList().
     * @param evidence the evidence
     */
    public void addEvidence(Item evidence) {
        evidenceReferenceList.addRefId(evidence.getIdentifier());
    }

    /**
     * Return a ReferenceList containing the evidence Items ids set by addEvidence()
     * @return the ReferenceList
     */
    public ReferenceList getEvidenceReferenceList() {
        return evidenceReferenceList;
    }

    /**
     * Reset the list of evidence items.
     */
    public void clearEvidenceReferenceList() {
        evidenceReferenceList = new ReferenceList("evidence");
    }

    /**
     * Set the ComputationalResult item for this record.
     * @param result the ComputationalResult item
     */
    public void setResult(Item result) {
        items.put("_result", result);
    }

    /**
     * Return the result Item set by setResult()
     * @return the result Item
     */
    protected Item getResult() {
        return (Item) items.get("_result");
    }

    /**
     * Get the SimpleRelation item from feature to parent feature for this record.
     * @param relation the relation item
     */
    public void addParentRelation(Item relation) {
        items.put("_relation" + relation.getIdentifier(), relation);
    }

    /**
     * Return the SimpleRelation Item set by addParentRelation()
     * @return the location Item
     */
    protected Set getParentRelations() {
        Set entrySet = items.entrySet();
        if (entrySet != null) {
            Iterator entryIter = entrySet.iterator();
            Set relations = new HashSet();
            while (entryIter.hasNext()) {
                Map.Entry entry = (Map.Entry) entryIter.next();
                if (((String) entry.getKey()).startsWith("_relation")) {
                    relations.add(entry.getValue());
                }
            }
            return relations;
        }
        return null;
    }

     /**
     * Get the tgtOrganism item to use in this handler
     * @param tgtOrganism the tgtOrganism item
     */
    public void setTgtOrganism(Item tgtOrganism) {
        items.put("_tgtOrganism", tgtOrganism);
    }

    /**
     * Return the tgtOrganism Item set by setTgtOrganism()
     * @return the tgtOrganism Item
     */
    protected Item getTgtOrganism() {
        return tgtOrganism;
    }

    /**
     * Set tgtSequence item created for this record, should not be edited in handler.
     * @param tgtSequence the sequence item
     */
    public void setTgtSequence(Item tgtSequence) {
        items.put("_tgtSequence", tgtSequence);
    }

    /**
     * Get the target Sequence Item set by setTgtSequence().
     * @return the target Sequence Item
     */
    protected Item getTgtSequence() {
        return tgtSequence;
    }

    /**
     * Set the tgtLocation item for this record.
     * @param tgtLocation the location item
     */
    public void setTgtLocation(Item tgtLocation) {
        items.put("_tgtLocation", tgtLocation);
    }

    /**
     * Return the tgtLocation Item set by setTgtLocation()
     * @return the tgtLocation Item
     */
    protected Item getTgtLocation() {
        return (Item) items.get("_tgtLocation");
    }

    /**
     * Set the ItemFactory to use in this handler.
     * @param itemFactory the ItemFactory
     */
    public void setItemFactory(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /**
     * Get the ItemFactory for this handler.
     * @return the ItemFactory
     */
    protected ItemFactory getItemFactory() {
        return itemFactory;
    }

    /**
     * Remove all items held locally in handler.
     */
    public void clear() {
        this.items = new HashMap();
        this.sequence = null;
        this.analysis = null;
    }

    /**
     * Return all items set and created in handler in this run - excludes sequence and
     * ComputationalAnalysis items.
     * @return a set of items
     */
    public Collection getItems() {
        return items.values();
    }

    /**
     * Add a new Item to the Collection returned by getItems().
     * @param item the Item to add
     */
    public void addItem(Item item) {
        items.put(item.getIdentifier(), item);
    }

    /**
     * Return items that need extra processing that can only be done after all other GFF features
     * have been read.
     * @return extra Items
     */
    public Collection getFinalItems() {
        return new ArrayList();
    }

    /**
     * Clear the list of final items.
     */
    public void clearFinalItems() {
        // do nothing
    }

    /**
     * Given a map from class name to reference name populate the reference for
     * a particular class with the parents of any SimpleRelations.
     * @param references map from classname to name of reference/collection to populate
     */
    protected void setReferences(Map references) {
        Item feature = getFeature();

        // set additional references from parents according to references map
        String clsName = classFromURI(feature.getClassName());
        if (references.containsKey(clsName) && getParentRelations() != null) {
            ClassDescriptor cld =
                tgtModel.getClassDescriptorByName(tgtModel.getPackageName() + "." + clsName);

            String refName = (String) references.get(clsName);
            Iterator parentIter = getParentRelations().iterator();

            if (cld.getReferenceDescriptorByName(refName, true) != null && parentIter.hasNext()) {
                Item relation = (Item) parentIter.next();
                feature.setReference(refName, relation.getReference("object").getRefId());
                if (parentIter.hasNext()) {
                    throw new RuntimeException("Feature has multiple relations for reference: "
                                               + refName + " for feature: " + feature.getClassName()
                                               + ", " + feature.getIdentifier() + ", "
                                               + feature.getAttribute("identifier").getValue());
                }
            } else if (cld.getCollectionDescriptorByName(refName, true) != null
                       && parentIter.hasNext()) {
                List refIds = new ArrayList();
                while (parentIter.hasNext()) {
                    refIds.add(((Item) parentIter.next()).getReference("object").getRefId());
                }
                feature.addCollection(new ReferenceList(refName, refIds));
            } else if (parentIter.hasNext()) {
                throw new RuntimeException("No '" + refName + "' reference/collection found in "
                                      + "class: " + clsName + " - is map configured correctly?");
            }
        }
    }

    /**
     * if the feature is CrossGenomeMatch, more specific properties added to
     * the feature through the handler.
     * @param feature item feature
     * @param orgAbb string organism abbreviation
     * @param seqIdentifier sequence identifier
     * @param seq item sequence
     * @param locString location string got from converter
     */
    protected void setCrossGenomeMatch(Item feature, String orgAbb, String seqIdentifier,
                                       Item seq, String locString) {
        String clsName = classFromURI(feature.getClassName());
        String seqClsName = classFromURI(seq.getClassName());
        if (clsName.equals("CrossGenomeMatch")) {
            if (orgAbb != null) {
                tgtOrganism = getTargetOrganism(orgAbb);

                Item targetSeq = getTargetSeq(seqIdentifier, seqClsName, orgAbb);

                String locStart = locString.split(" ")[1];
                String locEnd = locString.split(" ")[2];
                String locStrand = locString.split(" ")[3];

                Item targetLocation = createItem("Location", createIdentifier());
                if (Integer.parseInt(locStart) < Integer.parseInt(locEnd)) {
                    targetLocation.setAttribute("start", locStart);
                    targetLocation.setAttribute("end", locEnd);
                } else {
                    targetLocation.setAttribute("start", locEnd);
                    targetLocation.setAttribute("end", locStart);
                }

                if (locStrand != null && locStrand.equals("+")) {
                    targetLocation.setAttribute("strand", "1");
                } else if (locStrand != null && locStrand.equals("-")) {
                    targetLocation.setAttribute("strand", "-1");
                } else {
                    targetLocation.setAttribute("strand", "0");
                }

                targetLocation.setReference("object", targetSeq.getIdentifier());
                targetLocation.setReference("subject", feature.getIdentifier());
                setTgtLocation(targetLocation);

                feature.setReference("chromosome", seq.getIdentifier());
                feature.setReference("chromosomeLocation", getLocation().getIdentifier());
                feature.setReference("targetOrganism", tgtOrganism.getIdentifier());
                feature.setReference("targetLocatedSequenceFeature", targetSeq.getIdentifier());
                feature.setReference("targetLocatedSequenceFeatureLocation",
                                      targetLocation.getIdentifier());
            } else {
                throw new NullPointerException("No target organism for " + feature);
            }
        }
    }

    /**
     * @param identifier target sequence identifier
     * @param seqClsName passed by converter param, normally chromosome,
     * in case of opposumchain, it is scaffold
     * @param orgAbb organism abbreivation
     * @return target sequence
     */
     private Item getTargetSeq(String identifier, String seqClsName, String orgAbb) {
        Item tseq = (Item) tgtSeqs.get(identifier);
        if (tseq == null) {
            if (identifier.startsWith("scaffold_")) {
                tseq = createItem("Scaffold", createIdentifier());
                tseq.setAttribute("identifier", identifier.substring("scaffold_".length()));
            } else {
                tseq = createItem(seqClsName, createIdentifier());
                tseq.setAttribute("identifier", identifier);
            }
            tseq.addReference(getTargetOrgRef(orgAbb));
            tgtSeqs.put(identifier, tseq);
            setTgtSequence(tseq);
        }
        return tseq;
    }

    /**
     * @param orgAbb organism abbreivation
     * @return tgtOrganism
     */
    private Item getTargetOrganism(String orgAbb) {
        if (tgtOrganism == null) {
            tgtOrganism = createItem("Organism", createIdentifier());
            tgtOrganism.addAttribute(new Attribute("abbreviation", orgAbb));
            setTgtOrganism(tgtOrganism);
        }
        return tgtOrganism;
    }

    /**
     * @param orgAbb organism abbreivation
     * @return tgtOrganismReference
     */
    private Reference getTargetOrgRef(String orgAbb) {
        if (tgtOrgRef == null) {
            tgtOrgRef = new Reference("organism", getTargetOrganism(orgAbb).getIdentifier());
        }
        return tgtOrgRef;
    }

    /**
     * @param uri string
     * @return classname
     */
    private String classFromURI(String uri) {
        return uri.split("#")[1].split("[.]")[0];
    }

    /**
     * Create an item with given className and item identifier
     * @param className the class to create
     * @param identifier the Item identifier of the new Item
     * @return the created item
     */
    private Item createItem(String className, String identifier) {
        return itemFactory.makeItem(identifier, tgtModel.getNameSpace() + className, "");
    }

    /**
     * Create an item with given className and a new unique identifier
     * @param className the class to create
     * @return the created item
     */
    public Item createItem(String className) {
        return createItem(className, createIdentifier());
    }

    /**
     * create item identifier
     * @return identifier
     */
    private String createIdentifier() {
        return "1_" + itemid++;
    }

    /**
     * Get the DataSource to use while processing.
     * @return the DataSource
     */
    public Item getDataSource() {
        return dataSource;
    }

    /**
     * Set the DataSource to use while processing.  The converter will store() the DataSource.
     * @param dataSource the DataSource
     */
    public void setDataSource(Item dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Get the DataSet to use while processing.  The converter will store() the DataSet.
     * @return the DataSet
     */
    public Item getDataSet() {
        return dataSet;
    }

    /**
     * Set the DataSet to use while processing.  Called by the converter.
     * @param dataSet the DataSet
     */
    public void setDataSet(Item dataSet) {
        this.dataSet = dataSet;
    }
    

    /**
     * Create and add a synonym Item from the given information.
     * @param subject the subject of the new Synonym
     * @param type the Synonym type
     * @param value the Synonym value
     * @return the new Synonym Item
     */
    public Item addSynonym(Item subject, String type, String value) {
        String tgtNs = tgtModel.getNameSpace().toString();
        Item synonym = getItemFactory().makeItem(null, tgtNs + "Synonym", "");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("subject", subject.getIdentifier());
        synonym.setReference("source", getDataSource().getIdentifier());
        addItem(synonym);
        return synonym;
    }
}
