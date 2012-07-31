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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.bio.util.BioConverterUtil;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class GFF3Converter extends DataConverter
{
    private static final Logger LOG = Logger.getLogger(GFF3Converter.class);
    private Reference orgRef;
    private String seqClsName, orgTaxonId;
    private Item organism, dataSet, dataSource;
    private Model tgtModel;
    private Map<String, Item> seqs = new HashMap<String, Item>();
    private Map<String, String> identifierMap = new HashMap<String, String>();
    private GFF3RecordHandler handler;
    private GFF3SeqHandler sequenceHandler;
    private boolean dontCreateLocations;
    protected IdResolverFactory resolverFactory;
    private final Map<String, Item> dataSets = new HashMap<String, Item>();
    private final Map<String, Item> dataSources = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer ItemWriter
     * @param seqClsName The class of the coordinate system for this GFF3 file (generally
     * Chromosome)
     * @param orgTaxonId The taxon ID of the organism we are loading
     * @param dataSourceName name for dataSource
     * @param dataSetTitle title for dataSet
     * @param tgtModel the model to create items in
     * @param handler object to perform optional additional operations per GFF3 line
     * @param sequenceHandler the GFF3SeqHandler use to create sequence Items
     * @throws ObjectStoreException if something goes wrong
     */
    public GFF3Converter(ItemWriter writer, String seqClsName, String orgTaxonId,
            String dataSourceName, String dataSetTitle, Model tgtModel,
            GFF3RecordHandler handler, GFF3SeqHandler sequenceHandler) throws ObjectStoreException {
        super(writer, tgtModel);
        this.seqClsName = seqClsName;
        this.orgTaxonId = orgTaxonId;
        this.tgtModel = tgtModel;
        this.handler = handler;
        this.sequenceHandler = sequenceHandler;

        organism = getOrganism();
        dataSource = getDataSourceItem(dataSourceName);
        dataSet = getDataSetItem(dataSetTitle, null, null, dataSource);

        if (sequenceHandler == null) {
            this.sequenceHandler = new GFF3SeqHandler();
        }

        setStoreHook(new BioStoreHook(tgtModel, dataSet.getIdentifier(),
                dataSource.getIdentifier(), BioConverterUtil.getOntology(this)));

        handler.setConverter(this);
        handler.setIdentifierMap(identifierMap);
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

            // we only care about dupes if we are NOT creating locations
            if (processedIds.contains(record.getId()) && dontCreateLocations) {
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
    public void storeAll() throws ObjectStoreException {
        // TODO should probably not store if an empty file
        Iterator<?> iter = handler.getFinalItems().iterator();
        while (iter.hasNext()) {
            store((Item) iter.next());
        }
        handler.clearFinalItems();
    }

    /**
     * process GFF3 record and give a xml presentation
     * @param record GFF3Record
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void process(GFF3Record record) throws ObjectStoreException {
        String identifier = record.getId();
        String refId = identifierMap.get(identifier);

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

        Set<Item> synonymsToAdd = new HashSet<Item>();

        Item feature = null;

        // new feature
        if (refId == null) {
            feature = createItem(className);
            refId = feature.getIdentifier();
        }

        if (!"chromosome".equals(record.getType()) && seq != null) {
            boolean makeLocation = record.getStart() >= 1 && record.getEnd() >= 1
                && !dontCreateLocations
                && handler.createLocations(record);
            if (makeLocation) {
                Item location = getLocation(record, refId, seq, cd);
                if (feature == null) {
                    // this feature has already been created and stored
                    // we only wanted the location, we're done here.
                    store(location);
                    return;
                }
                int length = getLength(record);
                feature.setAttribute("length", String.valueOf(length));
                handler.setLocation(location);
                if ("Chromosome".equals(seqClsName)
                        && (cd.getFieldDescriptorByName("chromosome") != null)) {
                    feature.setReference("chromosome", seq.getIdentifier());
                    feature.setReference("chromosomeLocation", location);
                }
            }
        }

        if (feature == null) {
            // this feature has already been created and stored
            // feature with discontinous location, this location wasn't valid for some reason
            return;
        }

        if (identifier != null) {
            feature.setAttribute("primaryIdentifier", identifier);
        }
        handler.setFeature(feature);
        identifierMap.put(identifier, feature.getIdentifier());
        if (names != null) {
            setNames(names, synonymsToAdd, record.getId(), feature, cd);
        }

        List<String> parents = record.getParents();
        if (parents != null && !parents.isEmpty()) {
            setRefsAndCollections(parents, feature);
        }

        feature.addReference(getOrgRef());
        feature.addToCollection("dataSets", dataSet);

        handler.addDataSet(dataSet);
        Double score = record.getScore();
        if (score != null && !"".equals(String.valueOf(score))) {
            feature.setAttribute("score", String.valueOf(score));
            feature.setAttribute("scoreType", record.getSource());
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
            Iterator<Item> iter = handler.getItems().iterator();
            while (iter.hasNext()) {
                store(iter.next());
            }
        } catch (ObjectStoreException e) {
            LOG.error("Problem writing item to the itemwriter");
            throw e;
        }
    }

    private Item getLocation(GFF3Record record, String refId, Item seq, ClassDescriptor cd) {
        Item location = createItem("Location");
        int start = record.getStart();
        int end = record.getEnd();
        if (record.getStart() < record.getEnd()) {
            location.setAttribute("start", String.valueOf(start));
            location.setAttribute("end", String.valueOf(end));
        } else {
            location.setAttribute("start", String.valueOf(end));
            location.setAttribute("end", String.valueOf(start));
        }
        if (record.getStrand() != null && "+".equals(record.getStrand())) {
            location.setAttribute("strand", "1");
        } else if (record.getStrand() != null && "-".equals(record.getStrand())) {
            location.setAttribute("strand", "-1");
        } else {
            location.setAttribute("strand", "0");
        }
        location.setReference("locatedOn", seq.getIdentifier());
        location.setReference("feature", refId);
        location.addToCollection("dataSets", dataSet);
        return location;
    }

    private void setRefsAndCollections(List<String> parents, Item feature) {
        String clsName = feature.getClassName();
        Map<String, String> refsAndCollections = handler.getRefsAndCollections();
        if (refsAndCollections != null && refsAndCollections.containsKey(clsName)
                && parents != null && !parents.isEmpty()) {
            ClassDescriptor cld =
                tgtModel.getClassDescriptorByName(tgtModel.getPackageName() + "." + clsName);
            String refName = refsAndCollections.get(clsName);
            Iterator<String> parentIter = parents.iterator();
            if (cld.getReferenceDescriptorByName(refName, true) != null) {
                String parent = parentIter.next();
                feature.setReference(refName, getRefId(parent));
                if (parentIter.hasNext()) {
                    String primaryIdent  = feature.getAttribute("primaryIdentifier").getValue();
                    throw new RuntimeException("Feature has multiple relations for reference: "
                            + refName + " for feature: " + feature.getClassName()
                            + ", " + feature.getIdentifier() + ", " + primaryIdent);
                }
            } else if (cld.getCollectionDescriptorByName(refName, true) != null) {
                List<String> refIds = new ArrayList<String>();
                while (parentIter.hasNext()) {
                    refIds.add(getRefId(parentIter.next()));
                }
                feature.setCollection(refName, refIds);
            } else if (parentIter.hasNext()) {
                throw new RuntimeException("No '" + refName + "' reference/collection found in "
                        + "class: " + clsName + " - is map configured correctly?");
            }
        }
    }

    private void setNames(List<?> names, Set<Item> synonymsToAdd, String recordId,
            Item feature, ClassDescriptor cd) {
        if (cd.getFieldDescriptorByName("symbol") == null) {
            String name = (String) names.get(0);
            feature.setAttribute("name", name);
            for (Iterator<?> i = names.iterator(); i.hasNext(); ) {
                String recordName = (String) i.next();
                if (!recordName.equals(recordId) && !recordName.equals(name)) {
                    synonymsToAdd.add(getSynonym(feature, recordName));
                }
            }
        } else {
            String symbol = (String) names.get(0);
            feature.setAttribute("symbol", (String) names.get(0));
            for (Iterator<?> i = names.iterator(); i.hasNext(); ) {
                String recordName = (String) i.next();
                if (!recordName.equals(recordId) && !recordName.equals(symbol)) {
                    synonymsToAdd.add(getSynonym(feature, recordName));
                }
            }
        }
    }

    /**
     * Perform any necessary clean-up after post-conversion
     * @throws Exception if an error occurs
     */
    @Override
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
            store(organism);
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

//        if (identifier.startsWith("chr")) {
//            identifier = identifier.substring(3);
//        }

        Item seq = seqs.get(identifier);
        if (seq == null) {
            seq = sequenceHandler.makeSequenceItem(this, identifier);
            // sequence handler may choose not to create sequence
            if (seq != null) {
                seq.addReference(getOrgRef());
                store(seq);
                seqs.put(identifier, seq);
            }
        }
        handler.setSequence(seq);
        return seq;
    }

    /**
     * Set the dontCreateLocations flag
     * @param dontCreateLocations if false, create Locations of features on chromosomes while
     * processing
     */
    public void setDontCreateLocations(boolean dontCreateLocations) {
        this.dontCreateLocations = dontCreateLocations;
    }

    /**
     * Create and add a synonym Item from the given information.
     * @param subject the subject of the new Synonym
     * @param value the Synonym value
     * @return the new Synonym Item
     */
    public Item getSynonym(Item subject, String value) {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("value", value);
        synonym.setReference("subject", subject.getIdentifier());
        return synonym;
    }

    /**
     * Return a DataSet item for the given title
     * @param name the DataSet name
     * @return the DataSet Item
     */
    public Item getDataSourceItem(String name) {
        Item item = dataSources.get(name);
        if (item == null) {
            item = createItem("DataSource");
            item.setAttribute("name", name);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSource with name: " + name, e);
            }
            dataSources.put(name, item);
        }
        return item;
    }

    /**
     * Return a DataSource item with the given details.
     * @param title the DataSet title
     * @param url the new url field, or null if the url shouldn't be set
     * @param description the new description field, or null if the field shouldn't be set
     * @param dataSourceItem the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    public Item getDataSetItem(String title, String url, String description, Item dataSourceItem) {
        Item item = dataSets.get(title);
        if (item == null) {
            item = createItem("DataSet");
            item.setAttribute("name", title);
            item.setReference("dataSource", dataSourceItem);
            if (url != null) {
                item.setAttribute("url", url);
            }
            if (description != null) {
                item.setAttribute("description", description);
            }
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            dataSets.put(title, item);
        }
        return item;
    }

    private int getLength(GFF3Record record) {
        int start = record.getStart();
        int end = record.getEnd();
        int length = Math.abs(end - start) + 1;
        return length;
    }

    private String getRefId(String identifier) {
        String refId = identifierMap.get(identifier);
        if (refId == null) {
//            identifierMap.put(identifier, refId);
            String msg = "Failed setting setRefsAndCollections() in GFF3Converter - processing"
                + " child before parent - " + identifier;
            throw new RuntimeException(msg);
        }
        return refId;
    }
}

