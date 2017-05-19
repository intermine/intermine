package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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
import org.intermine.metadata.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 * @author Fengyuan Hu
 * @author Vivek Krishnakumar
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
    private final Map<String, Item> dataSets = new HashMap<String, Item>();
    private final Map<String, Item> dataSources = new HashMap<String, Item>();

    protected static final String PROP_FILE = "gff_config.properties";
    protected Map<String, Set<String>> configTerm = new HashMap<String, Set<String>>();
    protected Map<String, Set<String>> configExclude = new HashMap<String, Set<String>>();
    protected Map<String, Map<String, String>> configAttr =
            new HashMap<String, Map<String, String>>();
    protected Map<String, Map<String, String>> configAttrClass =
            new HashMap<String, Map<String, String>>();

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
        readConfig();
    }

    /**
     * read in config file
     */
    protected void readConfig() {
        Properties gffConfig = new Properties();
        try {
            gffConfig.load(getClass().getClassLoader().getResourceAsStream(
                    PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("I/O Problem loading properties '"
                    + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry : gffConfig.entrySet()) {
            if (entry.getKey().toString().contains("terms")) {
                if (entry.getValue() != null || !((String) entry.getValue()).trim().isEmpty()) {
                    String[] termArray = ((String) entry.getValue()).trim().split(",");
                    for (int i = 0; i < termArray.length; i++) { // Trim each string in the array
                        termArray[i] = termArray[i].trim();
                    }
                    configTerm.put(
                            entry.getKey().toString().split("\\.")[0],
                            new HashSet<String>(Arrays.asList(termArray)));
                }
            } else if (entry.getKey().toString().contains("excludes")) {
                if (entry.getValue() != null || !((String) entry.getValue()).trim().isEmpty()) {
                    String[] excludeArray = ((String) entry.getValue()).trim().split(",");
                    for (int i = 0; i < excludeArray.length; i++) { // Trim each string in the array
                        excludeArray[i] = excludeArray[i].trim();
                    }
                    configExclude.put(
                            entry.getKey().toString().split("\\.")[0],
                            new HashSet<String>(Arrays.asList(excludeArray)));
                }
            } else if (entry.getKey().toString().contains("attributes")) {
                if (entry.getValue() != null || !((String) entry.getValue()).trim().isEmpty()) {
                    String keyStr = entry.getKey().toString();
                    String[] keyBits = keyStr.split("\\.");
                    String taxonid = keyStr.split("\\.")[0];

                    if ("attributes".equals(keyStr.split("\\.")[1])) {
                        String attr = entry.getKey().toString().split("\\.")[2];
                        if (keyBits.length > 3) {
                            attr = keyStr.substring(
                                    keyStr.indexOf("attributes.") + 11,
                                    keyStr.length());
                        }
                        String field = ((String) entry.getValue()).trim();
                        if (configAttr.get(taxonid) == null) {
                            Map<String, String> attrMap = new HashMap<String, String>();
                            attrMap.put(field, attr);
                            configAttr.put(taxonid, attrMap);

                            Map<String, String> clsMap = new HashMap<String, String>();
                            clsMap.put(field, "all");
                            configAttrClass.put(taxonid, clsMap);
                        } else {
                            configAttr.get(taxonid).put(field, attr);
                            configAttrClass.get(taxonid).put(field, "all");
                        }
                    } else if ("attributes".equals(keyStr.split("\\.")[2])) {
                        String cls = keyStr.split("\\.")[1];
                        String attr = entry.getKey().toString().split("\\.")[3];
                        if (keyBits.length > 4) {
                            attr = keyStr.substring(
                                    keyStr.indexOf("attributes.") + 11,
                                    keyStr.length());
                        }
                        String field = ((String) entry.getValue()).trim();
                        if (configAttr.get(taxonid) == null) {
                            Map<String, String> attrMap = new HashMap<String, String>();
                            attrMap.put(field, attr);
                            configAttr.put(taxonid, attrMap);

                            Map<String, String> clsMap = new HashMap<String, String>();
                            clsMap.put(field, cls);
                            configAttrClass.put(taxonid, clsMap);
                        } else {
                            configAttr.get(taxonid).put(field, attr);
                            configAttrClass.get(taxonid).put(field, cls);
                        }
                    }
                }
            }
        }
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
     * @throws IOException
     */
    public void process(GFF3Record record) throws ObjectStoreException {
        String term = record.getType();
        // don't process terms in the exclude list
        if (configExclude != null && !configExclude.isEmpty()) {
            if (configExclude.containsKey(this.orgTaxonId)) {
                if (configExclude.get(this.orgTaxonId).contains(term)) {
                    return;
                }
            }
        }

        if (configTerm != null && !configTerm.isEmpty()) {
            if (configTerm.containsKey(this.orgTaxonId)) {
                if (!configTerm.get(this.orgTaxonId).contains(term)) {
                    return;
                }
            }
        }

        // By default, use ID field in attributes
        String primaryIdentifier = record.getId();
        // If pid set in gff_config.propeties, look for the attribute field, e.g. locus_tag
        if (configAttr.containsKey(this.orgTaxonId)) {
            if (configAttr.get(this.orgTaxonId).containsKey("primaryIdentifier")) {
                primaryIdentifier = getPrimaryIdentifier(record, term);
            }
        }
        String refId = identifierMap.get(primaryIdentifier);
        handler.clear(); // get rid of previous record Items from handler
        Item seq = getSeq(record.getSequenceID());
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
        if (refId == null) {         // new feature
            feature = createItem(className);
            refId = feature.getIdentifier();
        }

        if (!"chromosome".equals(term) && seq != null) {
            createLocation(record, refId, seq, cd, feature);
        }

        if (feature == null) {
            // this feature has already been created and stored
            // feature with discontinous location, this location wasn't valid for some reason
            return;
        }

        if (primaryIdentifier != null) {
            feature.setAttribute("primaryIdentifier", primaryIdentifier);
        }
        handler.setFeature(feature);
        identifierMap.put(primaryIdentifier, feature.getIdentifier());

        List<?> names = record.getNames();
        String symbol = null;
        List<String> synonyms = new ArrayList<String>();
        // get the attribute set for symbol
        if (configAttr.containsKey(this.orgTaxonId)) {
            if (configAttr.get(this.orgTaxonId).containsKey("symbol")) {
                symbol = getSymbol(record, term);
            }
        }
        // get the attribute set for synonym
        if (configAttr.containsKey(this.orgTaxonId)) {
            synonyms = createSynonyms(record, term);
        }
        if (names != null) {
            setNames(names, symbol, synonyms, synonymsToAdd, primaryIdentifier, feature, cd);
        }
        // Other attributes
        List<String> primeAttrList = Arrays.asList("primaryIdentifier", "symbol", "synonym");
        if (configAttr.containsKey(this.orgTaxonId)) {
            addOtherAttributes(record, term, feature, primeAttrList);
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

    private String getSymbol(GFF3Record record, String term) {
        String cls = configAttrClass.get(this.orgTaxonId).get("symbol");
        String symbol = null;
        if ("all".equals(cls) || term.equals(cls)) {
            String symbolAttr = configAttr.get(this.orgTaxonId).get("symbol");
            if (symbolAttr.contains("Dbxref") && record.getDbxrefs() != null) {
                String symbolAttrPrefix = symbolAttr.split("\\.")[1];
                for (Iterator<?> i = record.getDbxrefs().iterator(); i.hasNext(); ) {
                    String xref = (String) i.next();
                    if (xref.contains(symbolAttrPrefix)) {
                        symbol = xref.split(":")[1];
                        break;
                    }
                }
            } else {
                if (record.getAttributes().get(symbolAttr) != null) {
                    symbol = record.getAttributes().get(symbolAttr).get(0);
                }
            }
        }
        return symbol;
    }

    private String getPrimaryIdentifier(GFF3Record record, String term) {
        String primaryIdentifier = null;
        String cls = configAttrClass.get(this.orgTaxonId).get("primaryIdentifier");
        if ("all".equals(cls) || term.equals(cls)) {
            String pidAttr = configAttr.get(this.orgTaxonId).get("primaryIdentifier");
            if (pidAttr.contains("Dbxref") && record.getDbxrefs() != null) {
                String pidAttrPrefix = pidAttr.split("\\.")[1];
                for (Iterator<?> i = record.getDbxrefs().iterator(); i.hasNext(); ) {
                    String xref = (String) i.next();
                    if (xref.contains(pidAttrPrefix)) {
                        primaryIdentifier = xref.split(":")[1];
                        break;
                    }
                }
            } else {
                if (record.getAttributes().get(pidAttr) != null) {
                    primaryIdentifier = record.getAttributes().get(pidAttr).get(0);
                }
            }
        }
        return primaryIdentifier;
    }

    private List<String> createSynonyms(GFF3Record record, String term) {
        List<String> synonyms = new ArrayList<String>();
        if (configAttr.get(this.orgTaxonId).containsKey("synonym")) {
            String cls = configAttrClass.get(this.orgTaxonId).get("synonym");
            if ("all".equals(cls) || term.equals(cls)) {
                String synonymAttr = configAttr.get(this.orgTaxonId).get(
                        "synonym");
                if (synonymAttr.contains("Dbxref")
                        && record.getDbxrefs() != null) {
                    String synonymAttrPrefix = synonymAttr.split("\\.")[1];
                    Set<String> synSet = new HashSet<String>();
                    for (Iterator<?> i = record.getDbxrefs().iterator(); i
                            .hasNext();) {
                        String xref = (String) i.next();
                        if (xref.contains(synonymAttrPrefix)) {
                            synSet.add(xref.split(":")[1]);
                        }
                    }
                    synonyms.addAll(synSet);
                } else {
                    synonyms = record.getAttributes().get(synonymAttr);
                }
            }
        }
        return synonyms;
    }

    private void addOtherAttributes(GFF3Record record, String term,
            Item feature, List<String> primeAttrList) {
        Map<String, String> attrMapOrg = configAttr.get(this.orgTaxonId);
        Map<String, String> attrMapClone = new HashMap<String, String>();
        // Deep copy of a map
        for (Entry<String, String> e : attrMapOrg.entrySet()) {
            attrMapClone.put(e.getKey(), e.getValue());
        }

        for (String pa : primeAttrList) {
            attrMapClone.remove(pa);
        }

        for (Entry<String, String> e : attrMapClone.entrySet()) {
            String cls = configAttrClass.get(this.orgTaxonId).get(e.getKey());
            if ("all".equals(cls) || term.equals(cls)) {
                String attr = e.getValue();
                if (attr.contains("Dbxref") && record.getDbxrefs() != null) {
                    String attrPrefix = attr.split("\\.")[1];
                    for (Iterator<?> i = record.getDbxrefs().iterator(); i.hasNext(); ) {
                        String xref = (String) i.next();
                        if (xref.contains(attrPrefix)) {
                            if (feature.checkAttribute(e.getKey())) {
                                feature.setAttribute(e.getKey(), xref.split(":")[1]);
                            }
                            break;
                        }
                    }
                } else {
                    if (record.getAttributes().get(attr) != null) {
                        String attrVal = record.getAttributes().get(attr).get(0);
                        if (attrVal != null) {
                            if (feature.checkAttribute(e.getKey())) {
                                feature.setAttribute(e.getKey(), attrVal);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createLocation(GFF3Record record, String refId, Item seq,
            ClassDescriptor cd, Item feature) throws ObjectStoreException {
        boolean makeLocation = record.getStart() >= 1 && record.getEnd() >= 1
                && !dontCreateLocations
                && handler.createLocations(record);
        if (makeLocation) {
            Item location = getLocation(record, refId, seq);
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

    private Item getLocation(GFF3Record record, String refId, Item seq) {
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

    private void setNames(List<?> names, String symbol, List<String> synonyms,
            Set<Item> synonymsToAdd, String primaryIdentifier, Item feature, ClassDescriptor cd) {
        if (cd.getFieldDescriptorByName("symbol") == null) { // if symbol is not in the model
            String name = (String) names.get(0);
            feature.setAttribute("name", name);
            for (Iterator<?> i = names.iterator(); i.hasNext(); ) {
                String recordName = (String) i.next();
                if (!recordName.equals(primaryIdentifier) && !recordName.equals(name)) {
                    synonymsToAdd.add(getSynonym(feature, recordName));
                }
            }

            if (synonyms != null) {
                for (Iterator<?> i = synonyms.iterator(); i.hasNext(); ) {
                    String recordName = (String) i.next();
                    if (!recordName.equals(primaryIdentifier) && !recordName.equals(name)) {
                        synonymsToAdd.add(getSynonym(feature, recordName));
                    }
                }
            }
        } else {
            if (symbol == null) {
                feature.setAttribute("symbol", (String) names.get(0));
            } else {
                feature.setAttribute("symbol", symbol);
            }

            for (Iterator<?> i = names.iterator(); i.hasNext(); ) {
                String recordName = (String) i.next();
                if (!recordName.equals(primaryIdentifier) && !recordName.equals(symbol)) {
                    synonymsToAdd.add(getSynonym(feature, recordName));
                }
            }

            if (synonyms != null) {
                for (Iterator<?> i = synonyms.iterator(); i.hasNext(); ) {
                    String recordName = (String) i.next();
                    if (!recordName.equals(primaryIdentifier) && !recordName.equals(symbol)) {
                        synonymsToAdd.add(getSynonym(feature, recordName));
                    }
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

    private static int getLength(GFF3Record record) {
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
