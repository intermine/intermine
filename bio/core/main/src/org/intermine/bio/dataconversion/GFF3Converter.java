package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.bio.util.BioConverterUtil;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class GFF3Converter
{
    private static final Logger LOG = Logger.getLogger(GFF3Converter.class);
    private static final Map<String, String> SO_TERMS = new HashMap<String, String>();
    private static String ontologyRefId = null;
    private Reference orgRef;
    private ItemWriter writer;
    private String seqClsName, orgTaxonId;
    private Item organism, dataSet, dataSource, seqDataSource;
    private Model tgtModel;
    private int itemid = 0;
    private Map<String, Item> seqs = new HashMap<String, Item>();
    private Map<String, String> identifierMap = new HashMap<String, String>();
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

        setOntology();

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", dataSourceName);
        writer.store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.setAttribute("name", dataSetTitle);
        dataSet.setReference("dataSource", dataSource);
        writer.store(ItemHelper.convert(dataSet));

        if (!seqDataSourceName.equals(dataSourceName)) {
            seqDataSource = createItem("DataSource");
            seqDataSource.setAttribute("name", seqDataSourceName);
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
        boolean duplicates = false;
        Set<String> processedIds = new HashSet<String>();
        Set<String> duplicatedIds = new HashSet<String>();
        for (Iterator<?> i = GFF3Parser.parse(bReader); i.hasNext();) {
            record = (GFF3Record) i.next();

            if (processedIds.contains(record.getId())) {
                duplicates = true;
                duplicatedIds.add(record.getId());
            } else {
                if (record.getId() != null) {
                    processedIds.add(record.getId());
                }
            }
            if (!duplicates) {
                process(record);
            }
            opCount++;
            if (opCount % 1000 == 0) {
                now = System.currentTimeMillis();
                LOG.info("processed " + opCount + " lines --took " + (now - start) + " ms");
                start = System.currentTimeMillis();
            }
        }
        if (duplicates) {
            LOG.error("Duplicated IDs in GFF file: " + duplicatedIds);
            throw new IllegalArgumentException("Duplicated IDs in GFF file: " + duplicatedIds);
        }
    }

    /**
     * store all the items
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void store() throws ObjectStoreException {
        // TODO should probably not store if an empty file
        Iterator<?> iter = handler.getFinalItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            writer.store(ItemHelper.convert(item));
        }
        handler.clearFinalItems();
    }

    private void setSOTerm(Item item) {
        if (item.canHaveReference("sequenceOntologyTerm")
                && !item.hasReference("sequenceOntologyTerm")) {
            String soTermId = getSoTerm(item);
            if (!StringUtils.isEmpty(soTermId)) {
                item.setReference("sequenceOntologyTerm", soTermId);
            }
        }
    }

    private void setOntology() {
        if (ontologyRefId == null) {
            Item item = createItem("Ontology");
            item.setAttribute("name", "Sequence Ontology");
            item.setAttribute("url", "http://www.sequenceontology.org");
            try {
                writer.store(ItemHelper.convert(item));
            } catch (ObjectStoreException e) {
                throw new RuntimeException("Can't store ontology", e);
            }
            ontologyRefId = item.getIdentifier();
        }
    }

    private String getSoTerm(Item item) {
        String soName = null;
        try {
            soName = BioConverterUtil.javaNameToSO(item.getClassName());
            if (soName == null) {
                return null;
            }
            String soRefId = SO_TERMS.get(soName);
            if (StringUtils.isEmpty(soRefId)) {
                Item soterm = createItem("SOTerm");
                soterm.setAttribute("name", soName);
                soterm.setReference("ontology", ontologyRefId);
                writer.store(ItemHelper.convert(soterm));
                soRefId = soterm.getIdentifier();
                SO_TERMS.put(soName, soRefId);
            }
            return soRefId;
        } catch (IOException e) {
            return null;
        } catch (ObjectStoreException e) {
            return null;
        }
    }


    /**
     * process GFF3 record and give a xml presentation
     * @param record GFF3Record
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void process(GFF3Record record) throws ObjectStoreException {
        // get rid of previous record Items from handler
        handler.clear();
        List<?> names = record.getNames();

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
        String identifier = record.getId();
        Set<Item> synonymsToAdd = new HashSet<Item>();
        Item feature;
        // need to look up item id for this feature as may have already been a parent reference
        if (identifier != null) {
            feature = createItem(className, getIdentifier(identifier));
            feature.setAttribute("primaryIdentifier", identifier);
        } else {
            feature = createItem(className);
        }
        handler.setFeature(feature);

        if (names != null) {
            if (cd.getFieldDescriptorByName("symbol") == null) {
                feature.setAttribute("name", (String) names.get(0));
                for (Iterator<?> i = names.iterator(); i.hasNext(); ) {
                    String recordName = (String) i.next();
                    Item synonym = createItem("Synonym");
                    if (!recordName.equals(record.getId())) {
                        synonym.setReference("subject", feature.getIdentifier());
                        synonym.setAttribute("value", recordName);
                        synonym.setAttribute("type", "name");
                        synonym.addToCollection("dataSets", dataSet);
                        synonymsToAdd.add(synonym);
                    }
                }
            } else {
                feature.setAttribute("symbol", (String) names.get(0));
                for (Iterator<?> i = names.iterator(); i.hasNext(); ) {
                    String recordName = (String) i.next();
                    if (!recordName.equals(record.getId())) {
                        Item synonym = createItem("Synonym");
                        synonym.setReference("subject", feature.getIdentifier());
                        synonym.setAttribute("value", recordName);
                        synonym.setAttribute("type", "symbol");
                        synonym.addToCollection("dataSets", dataSet);
                        synonymsToAdd.add(synonym);
                    }
                }
            }
        }

        List<String> parents = record.getParents();
        if (parents != null && !parents.isEmpty()) {
            setRefsAndCollections(parents, feature);
        }

        feature.addReference(getOrgRef());
        feature.addToCollection("dataSets", dataSet);
        setSOTerm(feature);
        if (!record.getType().equals("chromosome") && seq != null) {
            boolean makeLocation = record.getStart() >= 1 && record.getEnd() >= 1
                && !dontCreateLocations
                && handler.createLocations(record);
                if (makeLocation) {
                    Item relation = createItem("Location");
                    int start = record.getStart();
                    int end = record.getEnd();
                    if (record.getStart() < record.getEnd()) {
                        relation.setAttribute("start", String.valueOf(start));
                        relation.setAttribute("end", String.valueOf(end));
                    } else {
                        relation.setAttribute("start", String.valueOf(end));
                        relation.setAttribute("end", String.valueOf(start));
                    }
                    if (record.getStrand() != null && record.getStrand().equals("+")) {
                        relation.setAttribute("strand", "1");
                    } else
                        if (record.getStrand() != null && record.getStrand().equals("-")) {
                            relation.setAttribute("strand", "-1");
                        } else {
                            relation.setAttribute("strand", "0");
                        }
                    int length = Math.abs(end - start) + 1;
                    feature.setAttribute("length", String.valueOf(length));

                    relation.setReference("locatedOn", seq.getIdentifier());
                    relation.setReference("feature", feature.getIdentifier());
                    relation.addToCollection("dataSets", dataSet);

                    handler.setLocation(relation);
                    if (seqClsName.equals("Chromosome")
                            && (cd.getFieldDescriptorByName("chromosome") != null)) {
                        feature.setReference("chromosome", seq.getIdentifier());
                        if (makeLocation) {
                            feature.setReference("chromosomeLocation", relation);
                        }
                    }
                }
        }
        handler.addDataSet(dataSet);
        Double score = record.getScore();
        if (score != null && !String.valueOf(score).equals("")) {
            feature.setAttribute("score", String.valueOf(score));
            feature.setAttribute("scoreType", record.getSource());
        }
        if (feature.hasAttribute("secondaryIdentifier")) {
            Item synonym = createItem("Synonym");
            synonym.setReference("subject", feature.getIdentifier());
            String value = feature.getAttribute("secondaryIdentifier").getValue();
            synonym.setAttribute("value", value);
            synonym.setAttribute("type", "identifier");
            synonym.addToCollection("dataSets", dataSet);
            synonymsToAdd.add(synonym);
        }
        if (feature.hasAttribute("primaryIdentifier")) {
            Item synonym = createItem("Synonym");
            synonym.setReference("subject", feature.getIdentifier());
            String value = feature.getAttribute("primaryIdentifier").getValue();
            synonym.setAttribute("value", value);
            synonym.setAttribute("type", "identifier");
            synonym.addToCollection("dataSets", dataSet);
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
        if (handler.getPublicationReferenceList().getRefIds().size() > 0) {
            feature.addCollection(handler.getPublicationReferenceList());
        }
        handler.clearPublicationReferenceList();

        try {
            Iterator<?> iter = handler.getItems().iterator();
            while (iter.hasNext()) {
                Item item = (Item) iter.next();
                writer.store(ItemHelper.convert(item));
            }
        } catch (ObjectStoreException e) {
            LOG.error("Problem writing item to the itemwriter");
            throw e;
        }
    }

    private void setRefsAndCollections(List<String> parents, Item feature) {
        String clsName = feature.getClassName();
        Map<String, String> refsAndCollections = handler.getRefsAndCollections();

        if (refsAndCollections.containsKey(clsName) && !parents.isEmpty()) {
            ClassDescriptor cld =
                tgtModel.getClassDescriptorByName(tgtModel.getPackageName() + "." + clsName);
            String refName = (String) refsAndCollections.get(clsName);
            Iterator<String> parentIter = parents.iterator();
            if (cld.getReferenceDescriptorByName(refName, true) != null) {
                String parentRefId = parentIter.next();
                feature.setReference(refName, parentRefId);
                if (parentIter.hasNext()) {
                    String primaryIdent  = feature.getAttribute("primaryIdentifier").getValue();
                    throw new RuntimeException("Feature has multiple relations for reference: "
                            + refName + " for feature: " + feature.getClassName()
                            + ", " + feature.getIdentifier() + ", " + primaryIdent);
                }
            } else if (cld.getCollectionDescriptorByName(refName, true) != null) {
                List<String> refIds = new ArrayList<String>();
                while (parentIter.hasNext()) {
                    refIds.add(parentIter.next());
                }
                feature.setCollection(refName, refIds);
            } else if (parentIter.hasNext()) {
                throw new RuntimeException("No '" + refName + "' reference/collection found in "
                        + "class: " + clsName + " - is map configured correctly?");
            }

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
            organism.setAttribute("taxonId", orgTaxonId);
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
     * @return return/create item of class seqClsName for given identifier
     * @throws ObjectStoreException if the Item can't be stored
     */
    private Item getSeq(String id)
    throws ObjectStoreException {
        // the seqHandler may have changed the id used, e.g. if using an IdResolver
        String identifier = sequenceHandler.getSeqIdentifier(id);

        if (identifier == null) {
            return null;
        }

        if (identifier.startsWith("chr")) {
            identifier = identifier.substring(3);
        }

        Item seq = (Item) seqs.get(identifier);
        if (seq == null) {
            seq = sequenceHandler.makeSequenceItem(this, identifier);
            // sequence handler may choose not to create sequence
            if (seq != null) {
                seq.addReference(getOrgRef());
                seq.addToCollection("dataSets", getDataSet());
                setSOTerm(seq);
                writer.store(ItemHelper.convert(seq));

                Item synonym = createItem("Synonym");
                synonym.setReference("subject", seq.getIdentifier());
                synonym.setAttribute("value", identifier);
                synonym.setAttribute("type", "identifier");
                synonym.addToCollection("dataSets", getDataSet());
                handler.addItem(synonym);
                seqs.put(identifier, seq);
            }
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
        return itemFactory.makeItem(identifier, className, "");
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

