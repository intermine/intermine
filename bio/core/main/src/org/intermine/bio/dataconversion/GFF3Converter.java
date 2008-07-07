package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class GFF3Converter
{
    private static final Logger LOG = Logger.getLogger(GFF3Converter.class);

    private Reference orgRef;
    private ItemWriter writer;
    private String seqClsName, orgTaxonId;
    private Item organism, dataSet, dataSource, seqDataSource;
    private Model tgtModel;
    private int itemid = 0;
    private Map analyses = new HashMap();
    private Map seqs = new HashMap();
    private Map identifierMap = new HashMap();
    private GFF3RecordHandler handler;
    private ItemFactory itemFactory;
    private GFF3SeqHandler sequenceHandler;
    private boolean dontCreateLocations;
    protected IdResolverFactory resolverFactory;

    /**
     * Constructor
     * @param writer ItemWriter
     * @param seqClsName The class of the coordinate system for this GFF3 file (generally
     * Chromosome)
     * @param orgTaxonId The taxon ID of the organism we are loading
     * @param dataSourceName name for dataSource
     * @param dataSetTitle title for dataSet
     * @param seqDataSourceName name of source for synonym on sequence (col 1), often different
     * to dataSourceName
     * @param tgtModel the model to create items in
     * @param handler object to perform optional additional operations per GFF3 line
     * @param sequenceHandler the GFF3SeqHandler use to create sequence Items
     * @throws ObjectStoreException if something goes wrong
     */

    public GFF3Converter(ItemWriter writer, String seqClsName, String orgTaxonId,
            String dataSourceName, String dataSetTitle, String seqDataSourceName, Model tgtModel,
            GFF3RecordHandler handler, GFF3SeqHandler sequenceHandler) throws ObjectStoreException {
        this.writer = writer;
        this.seqClsName = seqClsName;
        this.orgTaxonId = orgTaxonId;
        this.tgtModel = tgtModel;
        this.handler = handler;
        this.sequenceHandler = sequenceHandler;
        this.itemFactory = new ItemFactory(tgtModel, "1_");

        organism = getOrganism();

        dataSource = createItem("DataSource");
        dataSource.addAttribute(new Attribute("name", dataSourceName));
        writer.store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.addAttribute(new Attribute("title", dataSetTitle));
        dataSet.setReference("dataSource", dataSource);
        writer.store(ItemHelper.convert(dataSet));

        if (!seqDataSourceName.equals(dataSourceName)) {
            seqDataSource = createItem("DataSource");
            seqDataSource.addAttribute(new Attribute("name", seqDataSourceName));
            writer.store(ItemHelper.convert(seqDataSource));
        } else {
            seqDataSource = dataSource;
        }

        if (sequenceHandler == null) {
            this.sequenceHandler = new GFF3SeqHandler();
        }

        handler.setItemFactory(itemFactory);
        handler.setIdentifierMap(identifierMap);
        handler.setDataSource(dataSource);
        handler.setDataSet(dataSet);
        handler.setOrganism(organism);
    }

    /**
     * Parse a bufferedReader and process GFF3 record
     * @param bReader the Reader
     * @throws java.io.IOException if an error occurs reading GFF
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void parse(BufferedReader bReader)  throws IOException, ObjectStoreException {
        GFF3Record record;
        long start, now, opCount;

        opCount = 0;
        start = System.currentTimeMillis();
        for (Iterator i = GFF3Parser.parse(bReader); i.hasNext();) {
            record = (GFF3Record) i.next();
            process(record);
            opCount++;
            if (opCount % 10000 == 0) {
                now = System.currentTimeMillis();
                LOG.info("processed " + opCount + " lines --took " + (now - start) + " ms");
                start = System.currentTimeMillis();
            }
        }
    }

    /**
     * store all the items
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void store() throws ObjectStoreException {
        // TODO should probably not store if an empty file
        Iterator iter = handler.getFinalItems().iterator();
        while (iter.hasNext()) {
            writer.store(ItemHelper.convert((Item) iter.next()));
        }

        handler.clearFinalItems();
    }

    /**
     * process GFF3 record and give a xml presentation
     * @param record GFF3Record
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void process(GFF3Record record) throws ObjectStoreException {
        // get rid of previous record Items from handler
        handler.clear();
        List names = record.getNames();
        List parents = record.getParents();

        Item seq = getSeq(record.getSequenceID());

        String term = record.getType();
        String className = TypeUtil.javaiseClassName(term);
        String fullClassName = tgtModel.getPackageName() + "." + className;

        ClassDescriptor cd = tgtModel.getClassDescriptorByName(fullClassName);

        if (cd == null) {
            throw new IllegalArgumentException("no class found in model for: " + className
                                               + " (original GFF record type: " + term + ") for "
                                               + "record: " + record);
        }

        Set<Item> synonymsToAdd = new HashSet();
        Item feature;
        // need to look up item id for this feature as may have already been a parent reference
        if (record.getId() != null) {
            feature = createItem(className, getIdentifier(record.getId()));
            feature.addAttribute(new Attribute("primaryIdentifier", record.getId()));
        } else {
            feature = createItem(className);
        }

        handler.setFeature(feature);

        if (names != null) {
            if (cd.getFieldDescriptorByName("symbol") == null) {
                feature.addAttribute(new Attribute("name", (String) names.get(0)));
                for (Iterator i = names.iterator(); i.hasNext(); ) {
                    String recordName = (String) i.next();
                    Item synonym = createItem("Synonym");
                    if (!recordName.equals(record.getId())) {
                        synonym.addReference(new Reference("subject", feature.getIdentifier()));
                        synonym.addAttribute(new Attribute("value", recordName));
                        synonym.addAttribute(new Attribute("type", "name"));
                        synonym.addReference(new Reference("source", dataSource.getIdentifier()));
                        synonymsToAdd.add(synonym);
                    }
                }
            } else {
                feature.addAttribute(new Attribute("symbol", (String) names.get(0)));
                for (Iterator i = names.iterator(); i.hasNext(); ) {
                    String recordName = (String) i.next();
                    if (!recordName.equals(record.getId())) {
                        Item synonym = createItem("Synonym");
                        synonym.addReference(new Reference("subject", feature.getIdentifier()));
                        synonym.addAttribute(new Attribute("value", recordName));
                        synonym.addAttribute(new Attribute("type", "symbol"));
                        synonym.addReference(new Reference("source", dataSource.getIdentifier()));
                        synonymsToAdd.add(synonym);
                    }
                }
            }
        }

        feature.addReference(getOrgRef());
        feature.setCollection("dataSets",
                              new ArrayList(Collections.singleton(dataSet.getIdentifier())));

        // if parents -> create a SimpleRelation
        if (record.getParents() != null) {
            Set seenParents = new HashSet();
            for (Iterator i = parents.iterator(); i.hasNext();) {
                String parentName = (String) i.next();
                // add check for duplicate parent IDs to cope with pseudoobscura GFF
                if (!seenParents.contains(parentName)) {
                    Item simpleRelation = createItem("SimpleRelation");
                    simpleRelation.setReference("object", getIdentifier(parentName));
                    simpleRelation.setReference("subject", feature.getIdentifier());
                    handler.addParentRelation(simpleRelation);
                    seenParents.add(parentName);
                }
            }
        }


        Item relation;

        if (!record.getType().equals("chromosome")) {
            if (record.getStart() < 1 || record.getEnd() < 1 || dontCreateLocations
                || !handler.createLocations(record)) {
                relation = createItem("SimpleRelation");
            } else {
                relation = createItem("Location");
                int start = record.getStart();
                int end = record.getEnd();
                if (record.getStart() < record.getEnd()) {
                    relation.addAttribute(new Attribute("start", String.valueOf(start)));
                    relation.addAttribute(new Attribute("end", String.valueOf(end)));
                } else {
                    relation.addAttribute(new Attribute("start", String.valueOf(end)));
                    relation.addAttribute(new Attribute("end", String.valueOf(start)));
                }
                if (record.getStrand() != null && record.getStrand().equals("+")) {
                    relation.addAttribute(new Attribute("strand", "1"));
                } else
                    if (record.getStrand() != null && record.getStrand().equals("-")) {
                        relation.addAttribute(new Attribute("strand", "-1"));
                    } else {
                        relation.addAttribute(new Attribute("strand", "0"));
                    }

                if (record.getPhase() != null) {
                    relation.addAttribute(new Attribute("phase", record.getPhase()));
                }
            }
            relation.addReference(new Reference("object", seq.getIdentifier()));
            relation.addReference(new Reference("subject", feature.getIdentifier()));
            relation.addCollection(new ReferenceList("dataSets", Arrays.asList(new String[]
                {
                    dataSet.getIdentifier()
                })));
            handler.setLocation(relation);
        }

        handler.addDataSet(dataSet);

        if (record.getScore() != null) {
            Item computationalResult = createItem("ComputationalResult");
            if (String.valueOf(record.getScore()) != null) {
                computationalResult.addAttribute(new Attribute("type", "score"));
                computationalResult.addAttribute(new Attribute("score",
                                                 String.valueOf(record.getScore())));
            }

            //no sense to create ComputationalAnalysis if there is no ComputationalResult
            if (record.getSource() != null) {
                Item computationalAnalysis = getComputationalAnalysis(record.getSource());
                computationalResult.addReference(new Reference("analysis",
                                                 computationalAnalysis.getIdentifier()));
                handler.setAnalysis(computationalAnalysis);
            }

            handler.setResult(computationalResult);
            handler.addEvidence(computationalResult);
        } else {
            if (record.getSource() != null && !record.getSource().equals("FlyBase")) {
                // this special case added to cope with pseudoobscura data
                Item computationalResult = createItem("ComputationalResult");

                Item computationalAnalysis = getComputationalAnalysis(record.getSource());
                computationalResult.addReference(new Reference("analysis",
                                                 computationalAnalysis.getIdentifier()));
                handler.setAnalysis(computationalAnalysis);

                handler.setResult(computationalResult);
                handler.addEvidence(computationalResult);

                feature.addAttribute(new Attribute("curated", "false"));
            }
        }

        String orgAbb = null;
        String tgtSeqIdentifier = null;
        if (record.getAttributes().get("Organism") != null) {
            orgAbb = (String) ((List) record.getAttributes().get("Organism")).get(0);
        }
        if (record.getAttributes().get(seqClsName) != null) {
            tgtSeqIdentifier = (String) ((List) record.getAttributes().get(seqClsName)).get(0);
        }
        String tgtLocation = record.getTarget();
        if (orgAbb != null && tgtSeqIdentifier != null && tgtLocation != null) {
            handler.setCrossGenomeMatch(feature, orgAbb, tgtSeqIdentifier, seq, tgtLocation);
        }

        if (feature.hasAttribute("secondaryIdentifier")) {
            Item synonym = createItem("Synonym");
            synonym.addReference(new Reference("subject", feature.getIdentifier()));
            String value = feature.getAttribute("secondaryIdentifier").getValue();
            synonym.addAttribute(new Attribute("value", value));
            synonym.addAttribute(new Attribute("type", "identifier"));
            synonym.addReference(new Reference("source", dataSource.getIdentifier()));
            synonymsToAdd.add(synonym);
        }

        if (feature.hasAttribute("primaryIdentifier")) {
            Item synonym = createItem("Synonym");
            synonym.addReference(new Reference("subject", feature.getIdentifier()));
            String value = feature.getAttribute("primaryIdentifier").getValue();
            synonym.addAttribute(new Attribute("value", value));
            synonym.addAttribute(new Attribute("type", "identifier"));
            synonym.addReference(new Reference("source", dataSource.getIdentifier()));
            synonymsToAdd.add(synonym);
        }

        for (Item synonym : synonymsToAdd) {
            handler.addItem(synonym);
        }

        handler.process(record);
        if (handler.getDataSetReferenceList().getRefIds().size() > 0) {
            feature.addCollection(handler.getDataSetReferenceList());
        }
        handler.clearDataSetReferenceList();

        if (handler.getEvidenceReferenceList().getRefIds().size() > 0) {
            feature.addCollection(handler.getEvidenceReferenceList());
        }
        handler.clearEvidenceReferenceList();

        if (handler.getPublicationReferenceList().getRefIds().size() > 0) {
            feature.addCollection(handler.getPublicationReferenceList());
        }
        handler.clearPublicationReferenceList();

        try {
            Iterator iter = handler.getItems().iterator();
            while (iter.hasNext()) {
                Item item = (Item) iter.next();
                writer.store(ItemHelper.convert(item));
            }
        } catch (ObjectStoreException e) {
            LOG.error("Problem writing item to the itemwriter");
            throw e;
        }
    }


    private String getIdentifier(String id) {
        String identifier = (String) identifierMap.get(id);
        if (identifier == null) {
            identifier = createIdentifier();
            identifierMap.put(id, identifier);
        }
        return identifier;
    }


    /**
     * Perform any necessary clean-up after post-conversion
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        // empty - overridden as necessary
    }

    /**
     * Return the DataSet Item created for this GFF3Converter from the data set title passed
     * to the constructor.
     * @return the DataSet item
     */
    public Item getDataSet() {
        return dataSet;
    }

    /**
     * Return the DataSource Item created for this GFF3Converter from the data source name passed
     * to the constructor.
     * @return the DataSource item
     */
    public Item getDataSource() {
        return dataSource;
    }

    /**
     * Return the organism Item created for this GFF3Converter from the organism abbreviation passed
     * to the constructor.
     * @return the organism item
     * @throws ObjectStoreException if the Organism item can't be stored
     */
    public Item getOrganism() throws ObjectStoreException {
        if (organism == null) {
            organism = createItem("Organism");
            organism.addAttribute(new Attribute("taxonId", orgTaxonId));
            writer.store(ItemHelper.convert(organism));
        }
        return organism;
    }

    /**
     * Return the sequence class name that was passed to the constructor.
     * @return the class name
     */
    public String getSeqClsName() {
        return seqClsName;
    }

    /**
     * Return the
     * @return the target Model
     */
    public Model getTgtModel() {
        return tgtModel;
    }

    /**
     * @return organism reference
     * @throws ObjectStoreException if the Organism Item can't be stored
     */
    private Reference getOrgRef() throws ObjectStoreException {
        if (orgRef == null) {
            orgRef = new Reference("organism", getOrganism().getIdentifier());
        }
        return orgRef;
    }

    /**
     * @return ComputationalAnalysis item created/from map
     * @throws ObjectStoreException if the Item can't be stored
     */
    private Item getComputationalAnalysis(String algorithm) throws ObjectStoreException {
        Item analysis = (Item) analyses.get(algorithm);
        if (analysis == null) {
            analysis = createItem("ComputationalAnalysis");
            analysis.addAttribute(new Attribute("algorithm", algorithm));
            writer.store(ItemHelper.convert(analysis));
            analyses.put(algorithm, analysis);
        }
        return analysis;
    }

    /**
     * @return return/create item of class seqClsName for given identifier
     * @throws ObjectStoreException if the Item can't be stored
     */
    private Item getSeq(String identifier) throws ObjectStoreException {
        if (identifier.startsWith("chr")) {
            identifier = identifier.substring(3);
        }
        Item seq = (Item) seqs.get(identifier);
        if (seq == null) {
            seq = sequenceHandler.makeSequenceItem(this, identifier);
            seq.addReference(getOrgRef());
            writer.store(ItemHelper.convert(seq));

            Item synonym = createItem("Synonym");
            synonym.addReference(new Reference("subject", seq.getIdentifier()));
            synonym.addAttribute(new Attribute("value", identifier));
            synonym.addAttribute(new Attribute("type", "identifier"));
            synonym.addReference(new Reference("source", seqDataSource.getIdentifier()));
            handler.addItem(synonym);
            seqs.put(identifier, seq);
        }
        handler.setSequence(seq);
        return seq;
    }

    /**
     * Create an item with given className
     * @param className the new class name
     * @return the created item
     */
    protected Item createItem(String className) {
        return createItem(className, createIdentifier());
    }

    /**
     * Create an item with given className and item identifier
     * @param className the class of the new Item
     * @param identifier the identifier of the new Item
     * @return the created item
     */
    Item createItem(String className, String identifier) {
        return itemFactory.makeItem(identifier, tgtModel.getNameSpace() + className, "");
    }

    private String createIdentifier() {
        return "0_" + itemid++;
    }

    /**
     * Set the dontCreateLocations flag
     * @param dontCreateLocations if false, create Locations of features on chromosomes while
     * processing
     */
    public void setDontCreateLocations(boolean dontCreateLocations) {
        this.dontCreateLocations = dontCreateLocations;
    }

}

