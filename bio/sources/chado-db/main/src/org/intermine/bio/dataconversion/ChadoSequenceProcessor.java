package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.intermine.bio.chado.config.ConfigAction;
import org.intermine.bio.chado.config.CreateCollectionAction;
import org.intermine.bio.chado.config.CreateSynonymAction;
import org.intermine.bio.chado.config.DoNothingAction;
import org.intermine.bio.chado.config.SetFieldConfigAction;
import org.intermine.bio.util.OrganismData;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Transcript;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * A processor for the chado sequence module.
 * @author Kim Rutherford
 */
public class ChadoSequenceProcessor extends ChadoProcessor
{
    // incremented each time we make a new ChadoSequenceProcessor to make sure we have a unique
    // name for the temporary table
    private static int tempTableCount = 0;

    private static final Logger LOG = Logger.getLogger(ChadoSequenceProcessor.class);

    private Map<Integer, FeatureData> featureMap = new HashMap<Integer, FeatureData>();
    private Map<Integer, MultiKeyMap> config = new HashMap<Integer, MultiKeyMap>();
    private Map<Integer, Map<String, Integer>> chromosomeMaps =
            new HashMap<Integer, Map<String, Integer>>();

    // a map from chado pubmed id to item identifier for the publication
    private Map<Integer, String> publications = new HashMap<Integer, String>();

    private String tempTableName = null;

    private static final List<String> PARTOF_RELATIONS = Arrays.asList("partof", "part_of");

    // feature type to query from the feature table
    private static final List<String> DEFAULT_FEATURES = Arrays.asList(
            "gene", "mRNA", "transcript",
            "CDS", "intron", "exon", "EST",
            "five_prime_untranslated_region",
            "five_prime_UTR", "three_prime_untranslated_region",
            "three_prime_UTR", "origin_of_replication"
    );

    private static final List<String> CHROMOSOME_FEATURES =
        Arrays.asList("chromosome", "chromosome_arm", "ultra_scaffold", "golden_path_region");

    /**
     * An action that makes a synonym.
     */
    protected static final ConfigAction CREATE_SYNONYM_ACTION = new CreateSynonymAction();

    /**
     * An action that does nothing - used to ignore a synonym/dbxref/whatever instead of doing the
     * default.
     */
    protected static final ConfigAction DO_NOTHING_ACTION = new DoNothingAction();

    private static final String TEMP_FEATURE_TABLE_NAME_PREFIX = "intermine_chado_features_temp";

    private final MultiKeyMap singletonMap = new MultiKeyMap();

    /**
     * Create a new ChadoSequenceProcessor
     * @param chadoDBConverter the ChadoDBConverter that is controlling this processor
     */
    public ChadoSequenceProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
        synchronized (this) {
            tempTableCount++;
            tempTableName  = TEMP_FEATURE_TABLE_NAME_PREFIX + "_" + tempTableCount;
        }
    }

    /**
     * Return the config Map.
     * @param taxonId return the configuration for this organism
     * @return the Map
     */
    @SuppressWarnings("unchecked")
    protected Map<MultiKey, List<ConfigAction>> getConfig(int taxonId) {
        MultiKeyMap map = config.get(taxonId);
        if (map == null) {
            map = new MultiKeyMap();
            config.put(taxonId, map);
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override

    public void process(Connection connection) throws Exception {
        earlyExtraProcessing(connection);
        createFeatureTempTable(connection);
        processFeatureTable(connection);
        processFeatureCVTermTable(connection);
        processPubTable(connection);

        // process direct locations
        ResultSet directLocRes = getFeatureLocResultSet(connection);
        processLocationTable(connection, directLocRes);

        processRelationTable(connection);
        processDbxrefTable(connection);
        processSynonymTable(connection);
        processFeaturePropTable(connection);
        extraProcessing(connection, featureMap);
        finishedProcessing(connection, featureMap);
    }

    private void processFeatureTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Set<String> chromosomeFeatureTypesSet = new HashSet<String>(getChromosomeFeatureTypes());
        ResultSet res = getFeatureResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String name = res.getString("name");
            String uniqueName = res.getString("uniquename");
            String type = res.getString("type");
            String residues = res.getString("residues");
            Integer organismId = new Integer(res.getInt("organism_id"));
            if (chromosomeFeatureTypesSet.contains(type)) {
                addToChromosomeMaps(organismId, uniqueName, featureId);
            }
            int seqlen = 0;
            if (res.getObject("seqlen") != null) {
                seqlen = res.getInt("seqlen");
            }
            List<String> primaryIds = new ArrayList<String>();
            primaryIds.add(uniqueName);
            String interMineType = TypeUtil.javaiseClassName(fixFeatureType(type));
            uniqueName = fixIdentifier(interMineType, uniqueName);
            OrganismData organismData =
                getChadoDBConverter().getChadoIdToOrgDataMap().get(organismId);
            Item feature = makeFeature(featureId, type, interMineType, name, uniqueName, seqlen,
                                       organismData.getTaxonId());
            if (feature != null) {
                processAndStoreFeature(feature, featureId, uniqueName, name, seqlen, residues,
                                       interMineType, organismId);
                count++;
            }
        }
        LOG.info("created " + count + " features");
        res.close();
    }

    /**
     * Add the given chromosome feature_id, uniqueName and organismId to chromosomeMaps.
     */
    private void addToChromosomeMaps(Integer organismId, String chrUniqueName, Integer chrId) {
        Map<String, Integer> chromosomeMap;
        if (chromosomeMaps.containsKey(organismId)) {
            chromosomeMap = chromosomeMaps.get(organismId);
        } else {
            chromosomeMap = new HashMap<String, Integer>();
            chromosomeMaps.put(organismId, chromosomeMap);
        }
        chromosomeMap.put(chrUniqueName, chrId);
    }

    /**
     * Create and store a new InterMineObject given data from a row of the feature table in a
     * Chado database.
     * @param uniqueName the uniquename from Chado
     * @param name the name from Chado
     * @param seqlen the sequence length from Chado
     * @param residues the residues from Chado
     * @param interMineType the genomic model class name to use for the new feature
     * @param organismId the chado organism id
     * @throws ObjectStoreException if there is a problem while storing
     */
    private void processAndStoreFeature(Item feature, Integer featureId, String uniqueName,
                                        String name, int seqlen, String residues,
                                        String interMineType, Integer organismId)
    throws ObjectStoreException {
        OrganismData organismData = getChadoDBConverter().getChadoIdToOrgDataMap().get(organismId);
        int taxonId = organismData.getTaxonId();
        Item organismItem = getChadoDBConverter().getOrganismItem(taxonId);
        FeatureData fdat = new FeatureData();
        fdat.itemIdentifier = feature.getIdentifier();
        fdat.uniqueName = uniqueName;
        fdat.chadoFeatureName = name;
        fdat.interMineType = XmlUtil.getFragmentFromURI(feature.getClassName());
        fdat.organismData = organismData;
        feature.setReference("organism", organismItem);
        if (seqlen > 0) {
            feature.setAttribute("length", String.valueOf(seqlen));
            fdat.setFlag(FeatureData.LENGTH_SET, true);
        }
        ChadoDBConverter chadoDBConverter = getChadoDBConverter();

        String dataSourceName = chadoDBConverter.getDataSourceName();
        MultiKey nameKey = new MultiKey("feature", fdat.interMineType, dataSourceName, "name");
        List<ConfigAction> nameActionList = getConfig(taxonId).get(nameKey);

        Set<String> fieldValuesSet = new HashSet<String>();

        if (!StringUtils.isBlank(name)) {
            String fixedName = fixIdentifier(interMineType, name);
            if (nameActionList == null || nameActionList.size() == 0) {
                if (feature.checkAttribute("symbol")) {
                    fieldValuesSet.add(fixedName);
                    feature.setAttribute("symbol", fixedName);
                } else {
                    if (feature.checkAttribute("secondaryIdentifier")) {
                        fieldValuesSet.add(fixedName);
                        feature.setAttribute("secondaryIdentifier", fixedName);
                    } else {
                        // do nothing, if the name needs to go in a different attribute
                        // it will need to be configured
                    }
                }
            } else {
                for (ConfigAction action: nameActionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction attrAction =
                            (SetFieldConfigAction) action;
                        if (attrAction.isValidValue(fixedName)) {
                            String newFieldValue = attrAction.processValue(fixedName);
                            feature.setAttribute(attrAction.getFieldName(), newFieldValue);
                            fieldValuesSet.add(newFieldValue);
                            if (attrAction.getFieldName().equals("primaryIdentifier")) {
                                fdat.setFlag(FeatureData.IDENTIFIER_SET, true);
                            }
                        }
                    }
                }
            }
        }

        MultiKey uniqueNameKey =
            new MultiKey("feature", fdat.interMineType, dataSourceName,
                         "uniquename");
        List<ConfigAction> uniqueNameActionList =
            getConfig(taxonId).get(uniqueNameKey);
        if (uniqueNameActionList == null || uniqueNameActionList.size() == 0) {
            feature.setAttribute("primaryIdentifier", uniqueName);
            fieldValuesSet.add(uniqueName);
        } else {
            for (ConfigAction action: uniqueNameActionList) {
                if (action instanceof SetFieldConfigAction) {
                    SetFieldConfigAction attrAction = (SetFieldConfigAction) action;
                    if (attrAction.isValidValue(uniqueName)) {
                        String newFieldValue = attrAction.processValue(uniqueName);
                        feature.setAttribute(attrAction.getFieldName(), newFieldValue);
                        fieldValuesSet.add(newFieldValue);
                        if (attrAction.getFieldName().equals("primaryIdentifier")) {
                            fdat.setFlag(FeatureData.IDENTIFIER_SET, true);
                        }
                    }
                }
            }
        }

        if (feature.canHaveReference("sequence") && residues != null && residues.length() > 0) {
            Item sequence = getChadoDBConverter().createItem("Sequence");
            sequence.setAttribute("residues", residues);
            sequence.setAttribute("length", String.valueOf(seqlen));
            feature.setReference("sequence", sequence);
            getChadoDBConverter().store(sequence);
        }

        // don't set the evidence collection - that's done by processPubTable()
        fdat.intermineObjectId = store(feature, taxonId); // Stores Feature

        // always create a synonym for the uniquename
        boolean uniqueNameSet = false;
        if (fieldValuesSet.contains(uniqueName)) {
            uniqueNameSet = true;
        }
        Item uniqueNameSynonym =
            createSynonym(fdat, "identifier", uniqueName, uniqueNameSet, null);
        getChadoDBConverter().store(uniqueNameSynonym);

        if (!StringUtils.isBlank(name)) {
            String fixedName = fixIdentifier(interMineType, name);

            if (nameActionList == null || nameActionList.size() == 0) {
                nameActionList = new ArrayList<ConfigAction>();
                nameActionList.add(new CreateSynonymAction());
            }

            for (ConfigAction action : nameActionList) {
                if (action instanceof CreateSynonymAction) {
                    CreateSynonymAction createSynonymAction = (CreateSynonymAction) action;
                    if (createSynonymAction.isValidValue(fixedName)) {
                        String processedName = createSynonymAction.processValue(fixedName);
                        if (!fdat.existingSynonyms.contains(processedName)) {
                            boolean nameSet = fieldValuesSet.contains(processedName);
                            Item nameSynonym =
                                createSynonym(fdat, "name", processedName, nameSet, null);
                            getChadoDBConverter().store(nameSynonym);
                        }
                    }
                }
            }
        }
        featureMap.put(featureId, fdat);
    }

    /**
     * Store the feature Item.
     * @param feature the Item
     * @param taxonId the taxon id of this feature
     * @return the database id of the new Item
     * @throws ObjectStoreException if an error occurs while storing
     */
    protected Integer store(Item feature, int taxonId) throws ObjectStoreException {
        return getChadoDBConverter().store(feature);
    }

    /**
     * Make a new feature
     * @param featureId the chado feature id
     * @param chadoFeatureType the chado feature type (a SO term)
     * @param interMineType the InterMine type of the feature
     * @param name the name
     * @param uniqueName the uniquename
     * @param seqlen the sequence length (if known)
     * @param taxonId the NCBI taxon id of the current feature
     * @return the new feature
     */
    protected Item makeFeature(Integer featureId, String chadoFeatureType, String interMineType,
                               String name, String uniqueName,
                               int seqlen, int taxonId) {
        return getChadoDBConverter().createItem(interMineType);
    }

    /**
     * Get a list of the chado/so types of the LocatedSequenceFeatures we wish to load.  The list
     * will not include chromosome-like features (eg. "chromosome" and "chromosome_arm").
     * @return the list of features
     */
    protected List<String> getFeatures() {
        return DEFAULT_FEATURES;
    }

    /**
     * Get a list of the chado/so types of the Chromosome-like objects we wish to load.
     * (eg. "chromosome" and "chromosome_arm").
     * @return the list of features
     */
    protected List<String> getChromosomeFeatureTypes() {
        return CHROMOSOME_FEATURES;
    }

    /**
     * Return a list of types where one logical feature is represented as multiple rows in the
     * feature table.  An example is UTR features in flybase - if a UTR spans multiple exons, each
     * part is a separate row in the feature table.  In InterMine we represent the UTR as a single
     * object with a start and end so we need special handling for these types.
     * @return a list of segmented feature type
     */
    protected List<String> getSegmentedFeatures() {
        return new ArrayList<String>();
    }

    /**
     * Fix types from the feature table, perhaps by changing non-SO type into their SO equivalent.
     * Types that don't need fixing will be returned unchanged.
     * @param type the input type
     * @return the fixed type
     */
    protected String fixFeatureType(String type) {
        if (type.equals("five_prime_untranslated_region")) {
            return "five_prime_UTR";
        } else {
            if (type.equals("three_prime_untranslated_region")) {
                return "three_prime_UTR";
            } else {
                return type;
            }
        }
    }


    /**
     * Do any extra processing that is needed before the converter starts querying features
     * @param connection the Connection
     * @throws ObjectStoreException if there is a object store problem
     * @throws SQLException if there is a database problem
     */
    protected void earlyExtraProcessing(Connection connection)
        throws ObjectStoreException, SQLException {
        // override in subclasses as necessary
    }

    /**
     * Do any extra processing for this database, after all other processing is done
     * @param connection the Connection
     * @param featureDataMap a map from chado feature_id to data for that feature
     * @throws ObjectStoreException if there is a problem while storing
     * @throws SQLException if there is a problem
     */
    protected void extraProcessing(Connection connection,
                                   Map<Integer, FeatureData> featureDataMap)
        throws ObjectStoreException, SQLException {
        // override in subclasses as necessary
    }

    /**
     * Perform any actions needed after all processing is finished.
     * @param connection the Connection
     * @param featureDataMap a map from chado feature_id to data for that feature
     * @throws ObjectStoreException if there is a problem while storing
     * @throws SQLException if there is a problem
     */
    protected void finishedProcessing(Connection connection,
                                    Map<Integer, FeatureData> featureDataMap)
        throws ObjectStoreException, SQLException {
        // override in subclasses as necessary
    }

    /**
     * Process a featureloc table and create Location objects.
     * @param connection the Connectio
     * @param res a ResultSet that has the columns: featureloc_id, feature_id, srcfeature_id,
     *    fmin, fmax, strand
     * @throws SQLException if there is a problem while querying
     * @throws ObjectStoreException if there is a problem while storing
     */
    protected void processLocationTable(Connection connection, ResultSet res)
        throws SQLException, ObjectStoreException {
        int count = 0;
        int featureWarnings = 0;
        while (res.next()) {
            Integer featureLocId = new Integer(res.getInt("featureloc_id"));
            Integer featureId = new Integer(res.getInt("feature_id"));
            Integer srcFeatureId = new Integer(res.getInt("srcfeature_id"));
            int start = res.getInt("fmin") + 1;
            int end = res.getInt("fmax");
            if (start < 1 || end < 1) {
                // ignore as this location not legal in flymine
                continue;
            }
            int strand = res.getInt("strand");
            if (featureMap.containsKey(srcFeatureId)) {
                FeatureData srcFeatureData = featureMap.get(srcFeatureId);
                if (featureMap.containsKey(featureId)) {
                    FeatureData featureData = featureMap.get(featureId);
                    int taxonId = featureData.organismData.getTaxonId();
                    Item location =
                        makeLocation(start, end, strand, srcFeatureData, featureData, taxonId);
                    getChadoDBConverter().store(location);

                    final String featureClassName =
                        getModel().getPackageName() + "." + featureData.interMineType;
                    Class<?> featureClass;
                    try {
                        featureClass = Class.forName(featureClassName);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("unable to find class object for setting "
                                                   + "a chromosome reference", e);
                    }
                    if (LocatedSequenceFeature.class.isAssignableFrom(featureClass)) {
                        Integer featureIntermineObjectId = featureData.getIntermineObjectId();
                        if (srcFeatureData.interMineType.equals("Chromosome")) {
                            Reference chrReference = new Reference();
                            chrReference.setName("chromosome");
                            chrReference.setRefId(srcFeatureData.itemIdentifier);
                            getChadoDBConverter().store(chrReference, featureIntermineObjectId);
                        }
                        Reference locReference = new Reference();
                        locReference.setName("chromosomeLocation");
                        locReference.setRefId(location.getIdentifier());
                        getChadoDBConverter().store(locReference, featureIntermineObjectId);

                        if (!featureData.getFlag(FeatureData.LENGTH_SET)) {
                            setAttribute(featureData.intermineObjectId, "length",
                                         String.valueOf(end - start + 1));
                        }
                    } else {
                        LOG.warn("featureId (" + featureId + ") from location " + featureLocId
                                + " was expected to be a LocatedSequenceFeature");
                    }
                    count++;
                } else {
                    if (featureWarnings <= 20) {
                        if (featureWarnings < 20) {
                            LOG.warn("featureId (" + featureId + ") from location " + featureLocId
                                     + " was not found in the feature table");
                        } else {
                            LOG.warn("further location warnings ignored");
                        }
                        featureWarnings++;
                    }
                }
            } else {
                throw new RuntimeException("srcfeature_id (" + srcFeatureId + ") from location "
                                           + featureLocId + " was not found in the feature table");
            }
        }
        LOG.info("created " + count + " locations");
        res.close();
    }

    /**
     * Make a Location Relation between a LocatedSequenceFeature and a Chromosome.
     * @param start the start position
     * @param end the end position
     * @param strand the strand
     * @param srcFeatureData the FeatureData for the src feature (the Chromosome)
     * @param featureData the FeatureData for the LocatedSequenceFeature
     * @param taxonId the taxon id to use when finding the Chromosome for the Location
     * @return the new Location object
     * @throws ObjectStoreException if there is a problem while storing
     */
    protected Item makeLocation(int start, int end, int strand, FeatureData srcFeatureData,
                                FeatureData featureData, int taxonId)
        throws ObjectStoreException {
        Item location = getChadoDBConverter().makeLocation(srcFeatureData.itemIdentifier,
                                                           featureData.itemIdentifier,
                                                           start, end, strand, taxonId);
        return location;
    }

    /**
     * Use the feature_relationship table to set relations (references and collections) between
     * features.
     */
    private void processRelationTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getFeatureRelationshipResultSet(connection);
        Integer lastSubjectId = null;

        // a Map from transcript object id to count of exons
        Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();

        // Map from relation type to Map from object type to FeatureData - used to collect up all
        // the collection/reference information for one subject feature
        Map<String, Map<String, List<FeatureData>>> relTypeMap =
            new HashMap<String, Map<String, List<FeatureData>>>();
        int featureWarnings = 0;
        int collectionWarnings = 0;
        int count = 0;
        int collectionTotal = 0;
        while (res.next()) {
            Integer featRelationshipId = new Integer(res.getInt("feature_relationship_id"));
            Integer subjectId = new Integer(res.getInt("subject_id"));
            Integer objectId = new Integer(res.getInt("object_id"));
            String relationTypeName = res.getString("type_name");

            if (lastSubjectId != null && !subjectId.equals(lastSubjectId)) {
                if (!processCollectionData(lastSubjectId, relTypeMap, collectionWarnings)) {
                    collectionWarnings++;
                    if (collectionWarnings == 20) {
                        LOG.warn("ignoring further unknown feature warnings from "
                                 + "processCollectionData()");
                    }
                }
                collectionTotal += relTypeMap.size();
                relTypeMap = new HashMap<String, Map<String, List<FeatureData>>>();
            }
            if (featureMap.containsKey(subjectId)) {
                if (featureMap.containsKey(objectId)) {
                    FeatureData objectFeatureData = featureMap.get(objectId);
                    Map<String, List<FeatureData>> objectClassFeatureDataMap;
                    if (relTypeMap.containsKey(relationTypeName)) {
                        objectClassFeatureDataMap = relTypeMap.get(relationTypeName);
                    } else {
                        objectClassFeatureDataMap = new HashMap<String, List<FeatureData>>();
                        relTypeMap.put(relationTypeName, objectClassFeatureDataMap);
                    }

                    List<FeatureData> featureDataList;
                    if (objectClassFeatureDataMap.containsKey(objectFeatureData.interMineType)) {
                        featureDataList =
                            objectClassFeatureDataMap.get(objectFeatureData.interMineType);
                    } else {
                        featureDataList = new ArrayList<FeatureData>();
                        objectClassFeatureDataMap.put(objectFeatureData.interMineType,
                                                      featureDataList);
                    }
                    featureDataList.add(objectFeatureData);

                    // special case: collect data for setting Transcript.exonCount
                    Class<?> objectClass;
                    try {
                        objectClass = Class.forName(getModel().getPackageName() + "."
                                                    + objectFeatureData.interMineType);
                    } catch (ClassNotFoundException e) {
                        final String message =
                            "can't find class for " + objectFeatureData.interMineType
                            + "while processing relation: " + featRelationshipId;
                        throw new RuntimeException(message);
                    }
                    if (Transcript.class.isAssignableFrom(objectClass)) {
                        FeatureData subjectFeatureData = featureMap.get(subjectId);

                        // XXX FIXME TODO Hacky special case: count the exons so we can set
                        // exonCount later
                        if (subjectFeatureData.interMineType.equals("Exon")) {
                            if (!countMap.containsKey(objectFeatureData.intermineObjectId)) {
                                countMap.put(objectFeatureData.intermineObjectId, new Integer(1));
                            } else {
                                Integer currentVal =
                                    countMap.get(objectFeatureData.intermineObjectId);
                                countMap.put(objectFeatureData.intermineObjectId,
                                             new Integer(currentVal.intValue() + 1));
                            }
                        }
                    }
                } else {
                    if (featureWarnings <= 20) {
                        if (featureWarnings < 20) {
                            LOG.warn("object_id " + objectId + " from feature_relationship "
                                     + featRelationshipId + " was not found in the feature table");
                        } else {
                            LOG.warn("further feature_relationship warnings ignored");
                        }
                        featureWarnings++;
                    }
                }
            } else {
                if (featureWarnings <= 20) {
                    if (featureWarnings < 20) {
                        LOG.warn("subject_id " + subjectId + " from feature_relationship "
                                 + featRelationshipId
                                 + " was not found in the feature table");
                    } else {
                        LOG.warn("further feature_relationship warnings ignored");
                    }
                    featureWarnings++;
                }
            }
            count++;
            lastSubjectId = subjectId;
        }
        if (lastSubjectId != null) {
            processCollectionData(lastSubjectId, relTypeMap, collectionWarnings); // Stores stuff
            collectionTotal += relTypeMap.size();
        }
        LOG.info("processed " + count + " relations");
        LOG.info("total collection elements created: " + collectionTotal);
        res.close();

        // XXX FIXME TODO Hacky special case: set the exonCount fields
        for (Map.Entry<Integer, Integer> entry: countMap.entrySet()) {
            Integer featureId = entry.getKey();
            Integer collectionCount = entry.getValue();
            setAttribute(featureId, "exonCount", String.valueOf(collectionCount));
        }
    }

    /**
     * Create collections and references for the Item given by chadoSubjectId.
     * @param collectionWarnings
     */
    private boolean processCollectionData(Integer chadoSubjectId,
                                       Map<String, Map<String, List<FeatureData>>> relTypeMap,
                                       int collectionWarnings)
        throws ObjectStoreException {
        FeatureData subjectData = featureMap.get(chadoSubjectId);
        if (subjectData == null) {
            if (collectionWarnings < 20) {
                LOG.warn("unknown feature " + chadoSubjectId + " passed to processCollectionData - "
                         + "ignoring");
            }
            return false;
        }

        // map from collection name to list of item ids
        Map<String, List<String>> collectionsToStore = new HashMap<String, List<String>>();

        String subjectInterMineType = subjectData.interMineType;
        ClassDescriptor cd = getModel().getClassDescriptorByName(subjectInterMineType);
        Integer intermineObjectId = subjectData.intermineObjectId;
        for (Map.Entry<String, Map<String, List<FeatureData>>> entry: relTypeMap.entrySet()) {
            String relationType = entry.getKey();
            Map<String, List<FeatureData>> objectClassFeatureDataMap = entry.getValue();

            Set<Entry<String, List<FeatureData>>> mapEntries = objectClassFeatureDataMap.entrySet();
            for (Map.Entry<String, List<FeatureData>> featureDataMap: mapEntries) {
                String objectClass = featureDataMap.getKey();
                List<FeatureData> featureDataCollection = featureDataMap.getValue();
                List<FieldDescriptor> fds = null;

                FeatureData subjectFeatureData = featureMap.get(chadoSubjectId);
                // key example: ("relationship", "Translation", "producedby", "MRNA")
                MultiKey key = new MultiKey("relationship", subjectFeatureData.interMineType,
                                            relationType, objectClass);
                List<ConfigAction> actionList =
                    getConfig(subjectData.organismData.getTaxonId()).get(key);

                if (actionList != null) {
                    if (actionList.size() == 0
                        || actionList.size() == 1 && actionList.get(0) instanceof DoNothingAction) {
                        // do nothing
                        continue;
                    }
                    fds = new ArrayList<FieldDescriptor>();
                    for (ConfigAction action: actionList) {
                        if (action instanceof SetFieldConfigAction) {
                            SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                            String fieldName = setAction.getFieldName();
                            FieldDescriptor fd = cd.getFieldDescriptorByName(fieldName);
                            if (fd == null) {
                                throw new RuntimeException("can't find field " + fieldName
                                                           + " in class " + cd + " configured for "
                                                           + key);
                            } else {
                                fds.add(fd);
                            }
                        }
                    }
                    if (fds.size() == 0) {
                        throw new RuntimeException("no actions found for " + key);
                    }
                } else {
                    if (PARTOF_RELATIONS.contains(relationType)) {
                        // special case for part_of relations - try to find a reference or
                        // collection that has a name that looks right for these objects (of class
                        // objectClass).  eg.  If the subject is a Transcript and the objectClass
                        // is Exon then find collections called "exons", "geneParts" (GenePart is
                        // a superclass of Exon)
                        fds = getReferenceForRelationship(objectClass, cd);
                    } else {
                        continue;
                    }
                }

                if (fds.size() == 0) {
                    LOG.error("can't find collection for type " + relationType
                              + " in " + subjectInterMineType + " while processing feature "
                              + chadoSubjectId);
                    continue;
                }

                for (FieldDescriptor fd: fds) {
                    if (fd.isReference()) {
                        if (objectClassFeatureDataMap.size() > 1) {
                            throw new RuntimeException("found more than one object for reference "
                                                       + fd + " in class "
                                                       + subjectInterMineType
                                                       + " current subject identifier: "
                                                       + subjectData.uniqueName);
                        } else {
                            if (objectClassFeatureDataMap.size() == 1) {
                                Reference reference = new Reference();
                                reference.setName(fd.getName());
                                FeatureData referencedFeatureData = featureDataCollection.get(0);
                                reference.setRefId(referencedFeatureData.itemIdentifier);
                                getChadoDBConverter().store(reference, intermineObjectId);

                                // special case for 1-1 relations - we need to set the reverse
                                // reference
                                ReferenceDescriptor rd = (ReferenceDescriptor) fd;
                                ReferenceDescriptor reverseRD = rd.getReverseReferenceDescriptor();
                                if (reverseRD != null && !reverseRD.isCollection()) {
                                    Reference revReference = new Reference();
                                    revReference.setName(reverseRD.getName());
                                    revReference.setRefId(subjectData.itemIdentifier);
                                    Integer refObjectId = referencedFeatureData.intermineObjectId;
                                    getChadoDBConverter().store(revReference,
                                                                refObjectId);
                                }
                            }
                        }
                    } else {
                        List<String> itemIds;
                        if (collectionsToStore.containsKey(fd.getName())) {
                            itemIds = collectionsToStore.get(fd.getName());
                        } else {
                            itemIds = new ArrayList<String>();
                            collectionsToStore.put(fd.getName(), itemIds);
                        }
                        for (FeatureData featureData: featureDataCollection) {
                            itemIds.add(featureData.itemIdentifier);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, List<String>> entry: collectionsToStore.entrySet()) {
            ReferenceList referenceList = new ReferenceList();
            String collectionName = entry.getKey();
            referenceList.setName(collectionName);
            List<String> idList = entry.getValue();
            referenceList.setRefIds(idList);
            getChadoDBConverter().store(referenceList, intermineObjectId);

            // if there is a field called <classname>Count that matches the name of the collection
            // we just stored, set it
            String countName;
            if (collectionName.endsWith("s")) {
                countName = collectionName.substring(0, collectionName.length() - 1);
            } else {
                countName = collectionName;
            }
            countName += "Count";
            if (cd.getAttributeDescriptorByName(countName, true) != null) {
                setAttribute(intermineObjectId, countName, String.valueOf(idList.size()));
            }
        }

        return true;
    }

    /**
     * Search ClassDescriptor cd class for refs/collections with the right name for the objectType
     * eg. find CDSs collection for objectType = CDS and find gene reference for objectType = Gene.
     */
    private List<FieldDescriptor> getReferenceForRelationship(String objectType,
                                                              ClassDescriptor cd) {
        List<FieldDescriptor> fds = new ArrayList<FieldDescriptor>();
        LinkedHashSet<String> allClasses = new LinkedHashSet<String>();
        allClasses.add(objectType);
        try {
            Set<String> parentClasses = ClassDescriptor.findSuperClassNames(getModel(), objectType);
            allClasses.addAll(parentClasses);
        } catch (MetaDataException e) {
            throw new RuntimeException("class not found in the model", e);
        }

        for (String clsName: allClasses) {
            List<String> possibleRefNames = new ArrayList<String>();
            String unqualifiedClsName = TypeUtil.unqualifiedName(clsName);
            possibleRefNames.add(unqualifiedClsName);
            possibleRefNames.add(unqualifiedClsName + 's');
            possibleRefNames.add(StringUtil.decapitalise(unqualifiedClsName));
            possibleRefNames.add(StringUtil.decapitalise(unqualifiedClsName) + 's');
            for (String possibleRefName: possibleRefNames) {
                FieldDescriptor fd = cd.getFieldDescriptorByName(possibleRefName);
                if (fd != null) {
                    fds.add(fd);
                }
            }
        }
        return fds;
    }

    private void processDbxrefTable(Connection connection)
        throws SQLException, ObjectStoreException {


        ResultSet res = getDbxrefResultSet(connection);
        Set<String> existingAttributes = new HashSet<String>();
        Integer currentFeatureId = null;
        int count = 0;

        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String accession = res.getString("accession");
            String dbName = res.getString("db_name");
            Boolean isCurrent = res.getBoolean("is_current");

            if (currentFeatureId != null && currentFeatureId != featureId) {
                existingAttributes = new HashSet<String>();
            }

            if (featureMap.containsKey(featureId)) {
                FeatureData fdat = featureMap.get(featureId);
                accession  = fixIdentifier(fdat.interMineType, accession);
                MultiKey key = new MultiKey("dbxref", fdat.interMineType, dbName, isCurrent);
                int taxonId = fdat.organismData.getTaxonId();
                Map<MultiKey, List<ConfigAction>> orgConfig =
                    getConfig(taxonId);
                List<ConfigAction> actionList = orgConfig.get(key);

                if (actionList == null) {
                    // try ignoring isCurrent
                    MultiKey key2 = new MultiKey("dbxref", fdat.interMineType, dbName, null);
                    actionList = orgConfig.get(key2);
                }

                if (actionList == null) {
                    // no actions configured for this synonym
                    continue;
                }

                Set<String> fieldsSet = new HashSet<String>();

                for (ConfigAction action: actionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                        if (!existingAttributes.contains(setAction.getFieldName())) {
                            if (setAction.isValidValue(accession)) {
                                String newFieldValue = setAction.processValue(accession);
                                setAttribute(fdat.intermineObjectId, setAction.getFieldName(),
                                             newFieldValue);
                                existingAttributes.add(setAction.getFieldName());
                                fieldsSet.add(newFieldValue);
                                if (setAction.getFieldName().equals("primaryIdentifier")) {
                                    fdat.setFlag(FeatureData.IDENTIFIER_SET, true);
                                }
                            }
                        }
                    }
                }

                for (ConfigAction action: actionList) {
                    if (action instanceof CreateSynonymAction) {
                        CreateSynonymAction createSynonymAction = (CreateSynonymAction) action;
                        if (!createSynonymAction.isValidValue(accession)) {
                            continue;
                        }
                        String newFieldValue = createSynonymAction.processValue(accession);
                        if (fdat.existingSynonyms.contains(newFieldValue)) {
                            continue;
                        } else {
                            boolean isPrimary = false;
                            if (fieldsSet.contains(newFieldValue)) {
                                isPrimary = true;
                            }
                            Item synonym = createSynonym(fdat, "identifier", newFieldValue,
                                                         isPrimary, null);
                            getChadoDBConverter().store(synonym);
                            count++;
                        }
                    }
                }
            }

            currentFeatureId = featureId;
        }

        LOG.info("created " + count + " synonyms from the dbxref table");
        res.close();
    }

    private void processFeaturePropTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getFeaturePropResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String identifier = res.getString("value");

            if (identifier == null) {
                continue;
            }

            String propTypeName = res.getString("type_name");

            if (featureMap.containsKey(featureId)) {
                FeatureData fdat = featureMap.get(featureId);
                MultiKey key = new MultiKey("prop", fdat.interMineType, propTypeName);
                int taxonId = fdat.organismData.getTaxonId();
                List<ConfigAction> actionList = getConfig(taxonId).get(key);
                if (actionList == null) {
                    // no actions configured for this prop
                    continue;
                }

                Set<String> fieldsSet = new HashSet<String>();

                for (ConfigAction action: actionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                        if (setAction.isValidValue(identifier)) {
                            String newFieldValue = setAction.processValue(identifier);
                            setAttribute(fdat.intermineObjectId, setAction.getFieldName(),
                                         newFieldValue);
                            fieldsSet.add(newFieldValue);
                            if (setAction.getFieldName().equals("primaryIdentifier")) {
                                fdat.setFlag(FeatureData.IDENTIFIER_SET, true);
                            }
                        }
                    }
                }

                for (ConfigAction action: actionList) {
                    if (action instanceof CreateSynonymAction) {
                        CreateSynonymAction synonymAction = (CreateSynonymAction) action;
                        if (!synonymAction.isValidValue(identifier)) {
                            continue;
                        }
                        String newFieldValue = synonymAction.processValue(identifier);
                        Set<String> existingSynonyms = fdat.existingSynonyms;
                        if (existingSynonyms.contains(newFieldValue)) {
                            continue;
                        } else {
                            String synonymType = synonymAction.getSynonymType();
                            if (synonymType == null) {
                                synonymType = propTypeName;
                            }
                            boolean isPrimary = false;
                            if (fieldsSet.contains(newFieldValue)) {
                                isPrimary = true;
                            }
                            Item synonym = createSynonym(fdat, synonymType, newFieldValue,
                                                         isPrimary, null);
                            getChadoDBConverter().store(synonym);
                            count++;
                        }

                    }
                }
            }
        }
        LOG.info("created " + count + " synonyms from the featureprop table");
        res.close();
    }

    /**
     * Read the feature, feature_cvterm and cvterm tables, then set fields, create synonyms or
     * create objects based on the cvterms.
     * @param connection
     */
    private void processFeatureCVTermTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getFeatureCVTermResultSet(connection);
        int count = 0;
        Integer previousFeatureId = null;

        // map from reference/collection name to list of Items to store in the reference or
        // collection
        Map<String, List<Item>> dataMap = new HashMap<String, List<Item>>();

        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String cvtermName = res.getString("cvterm_name");
            String cvName = res.getString("cv_name");

            FeatureData fdat = featureMap.get(featureId);

            if (fdat == null) {
                continue;
            }

            if (!featureId.equals(previousFeatureId) && previousFeatureId != null) {
                processCVTermRefCols(previousFeatureId, dataMap);
                dataMap = new HashMap<String, List<Item>>();
            }

            MultiKey key = new MultiKey("cvterm", fdat.interMineType, cvName);

            int taxonId = fdat.organismData.getTaxonId();

            List<ConfigAction> actionList = getConfig(taxonId).get(key);
            if (actionList == null) {
                // no actions configured for this prop
                continue;
            }

            Set<String> fieldsSet = new HashSet<String>();

            for (ConfigAction action: actionList) {
                if (action instanceof SetFieldConfigAction) {
                    SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                    if (setAction.isValidValue(cvtermName)) {
                        String newFieldValue = setAction.processValue(cvtermName);
                        setAttribute(fdat.intermineObjectId, setAction.getFieldName(),
                                     newFieldValue);
                        fieldsSet.add(newFieldValue);
                        if (setAction.getFieldName().equals("primaryIdentifier")) {
                            fdat.setFlag(FeatureData.IDENTIFIER_SET, true);
                        }
                    }
                } else {
                    if (action instanceof CreateSynonymAction) {
                        CreateSynonymAction synonymAction = (CreateSynonymAction) action;
                        if (!synonymAction.isValidValue(cvtermName)) {
                            continue;
                        }
                        String newFieldValue = synonymAction.processValue(cvtermName);
                        Set<String> existingSynonyms = fdat.existingSynonyms;
                        if (existingSynonyms.contains(newFieldValue)) {
                            continue;
                        } else {
                            String synonymType = synonymAction.getSynonymType();
                            boolean isPrimary = false;
                            if (fieldsSet.contains(newFieldValue)) {
                                isPrimary = true;
                            }
                            Item synonym = createSynonym(fdat, synonymType, newFieldValue,
                                                         isPrimary, null);
                            getChadoDBConverter().store(synonym);
                            count++;
                        }

                    } else {
                        if (action instanceof CreateCollectionAction) {
                            CreateCollectionAction cca = (CreateCollectionAction) action;

                            Item item = null;
                            String fieldName = cca.getFieldName();
                            String className = cca.getClassName();
                            if (cca.createSingletons()) {
                                MultiKey singletonKey =
                                    new MultiKey(className, fieldName, cvtermName);
                                item = (Item) singletonMap.get(singletonKey);
                            }
                            if (item == null) {
                                item = getChadoDBConverter().createItem(className);
                                item.setAttribute(fieldName, cvtermName);
                                getChadoDBConverter().store(item);
                                if (cca.createSingletons()) {
                                    singletonMap.put(key, item);
                                }
                            }

                            String referenceName = cca.getReferenceName();
                            List<Item> itemList;
                            if (dataMap.containsKey(referenceName)) {
                                itemList = dataMap.get(referenceName);
                            } else {
                                itemList = new ArrayList<Item>();
                                dataMap.put(referenceName, itemList);
                            }
                            itemList.add(item);
                        }
                    }
                }
            }

            previousFeatureId = featureId;
        }

        if (previousFeatureId != null) {
            processCVTermRefCols(previousFeatureId, dataMap);
        }

        LOG.info("created " + count + " synonyms from the feature_cvterm table");
        res.close();
    }


    /**
     * Given the object id and a map of reference/collection names to Items, store the Items in the
     * reference or collection of the object.
     */
    private void processCVTermRefCols(Integer chadoObjectId, Map<String, List<Item>> dataMap)
        throws ObjectStoreException {

        FeatureData fdat = featureMap.get(chadoObjectId);
        String interMineType = fdat.interMineType;
        ClassDescriptor cd = getModel().getClassDescriptorByName(interMineType);
        for (String referenceName: dataMap.keySet()) {
            FieldDescriptor fd = cd.getFieldDescriptorByName(referenceName);
            if (fd == null) {
                throw new RuntimeException("failed to find " + referenceName + " in "
                                           + interMineType);
            }
            List<Item> itemList = dataMap.get(referenceName);
            Integer intermineObjectId = fdat.getIntermineObjectId();
            if (fd.isReference()) {
                if (itemList.size() > 1) {
                    throw new RuntimeException("found more than one object for reference "
                                               + fd + " in class "
                                               + interMineType + " items: " + itemList);
                } else {
                    Item item = itemList.iterator().next();
                    Reference reference = new Reference();
                    reference.setName(fd.getName());
                    String itemIdentifier = item.getIdentifier();
                    reference.setRefId(itemIdentifier);
                    getChadoDBConverter().store(reference, intermineObjectId);

                    // XXX FIXME TODO: special case for 1-1 relations - we need to set the reverse
                    // reference
                }
            } else {
                ReferenceList referenceList = new ReferenceList();
                referenceList.setName(referenceName);
                for (Item item: itemList) {
                    referenceList.addRefId(item.getIdentifier());
                }
                getChadoDBConverter().store(referenceList, intermineObjectId);
            }
        }
    }

    private void processSynonymTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getSynonymResultSet(connection);
        Set<String> existingAttributes = new HashSet<String>();
        Integer currentFeatureId = null;
        int count = 0;
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String identifier = res.getString("synonym_name");
            String synonymTypeName = res.getString("type_name");
            Boolean isCurrent = res.getBoolean("is_current");

            identifier = fixIdentifier(synonymTypeName, identifier);

            if (currentFeatureId != null && currentFeatureId != featureId) {
                existingAttributes = new HashSet<String>();
            }

            if (featureMap.containsKey(featureId)) {
                FeatureData fdat = featureMap.get(featureId);
                MultiKey key =
                    new MultiKey("synonym", fdat.interMineType, synonymTypeName, isCurrent);
                int taxonId = fdat.organismData.getTaxonId();
                Map<MultiKey, List<ConfigAction>> orgConfig = getConfig(taxonId);
                List<ConfigAction> actionList = orgConfig.get(key);

                if (actionList == null) {
                    // try ignoring isCurrent
                    MultiKey key2 =
                        new MultiKey("synonym", fdat.interMineType, synonymTypeName, null);
                    actionList = orgConfig.get(key2);
                }
                if (actionList == null) {
                    // no actions configured for this synonym
                    continue;
                }

                boolean setField = false;

                for (ConfigAction action: actionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                        if (!existingAttributes.contains(setAction.getFieldName())
                                        && setAction.isValidValue(identifier)) {
                            String newFieldValue = setAction.processValue(identifier);
                            setAttribute(fdat.intermineObjectId, setAction.getFieldName(),
                                         newFieldValue);
                            existingAttributes.add(setAction.getFieldName());
                            setField = true;
                            if (setAction.getFieldName().equals("primaryIdentifier")) {
                                fdat.setFlag(FeatureData.IDENTIFIER_SET, true);
                            }
                        }
                    }
                }

                for (ConfigAction action: actionList) {
                    if (action instanceof CreateSynonymAction) {
                        CreateSynonymAction createSynonymAction = (CreateSynonymAction) action;
                        if (!createSynonymAction.isValidValue(identifier)) {
                            continue;
                        }
                        String newFieldValue = createSynonymAction.processValue(identifier);
                        if (fdat.existingSynonyms.contains(newFieldValue)) {
                            continue;
                        } else {
                            Item synonym =
                                createSynonym(fdat, synonymTypeName, newFieldValue, setField,
                                              null);
                            getChadoDBConverter().store(synonym);
                            count++;
                        }
                    }
                }
            }

            currentFeatureId = featureId;
        }

        LOG.info("created " + count + " synonyms from the synonym table");
        res.close();
    }

    /**
     * Process the identifier and return a "cleaned" version.  Implement in sub-classes to fix
     * data problem.
     * @param type the InterMine type of the feature that this identifier came from
     * @param identifier the identifier
     * @return a cleaned identifier
     */
    protected String fixIdentifier(String type, String identifier) {
        return identifier;
    }

    private void processPubTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getPubResultSet(connection);

        List<String> currentPublicationIds = new ArrayList<String>();
        Integer lastPubFeatureId = null;
        int featureWarnings = 0;
        int count = 0;

        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            if (!featureMap.containsKey(featureId)) {
                if (featureWarnings <= 20) {
                    if (featureWarnings < 20) {
                        LOG.warn("feature " + featureId + " not found in features Map while "
                                 + "processing publications");
                    } else {
                        LOG.warn("further feature id warnings ignored in processPubTable()");
                    }
                    featureWarnings++;
                }
                continue;
            }
            Integer pubMedId = Integer.parseInt(res.getString("pub_db_identifier"));
            if (lastPubFeatureId != null && !featureId.equals(lastPubFeatureId)) {
                makeFeaturePublications(lastPubFeatureId, currentPublicationIds);
                currentPublicationIds = new ArrayList<String>();
            }
            String publicationId = makePublication(pubMedId);
            currentPublicationIds.add(publicationId);
            lastPubFeatureId = featureId;
            count++;
        }

        if (lastPubFeatureId != null) {
            makeFeaturePublications(lastPubFeatureId, currentPublicationIds);
        }
        LOG.info("Created " + count + " publications");
        res.close();
    }

    /**
     * Return the item identifier of the publication Item for the given pubmed id.
     * @param pubMedId the pubmed id
     * @return the publication item id
     * @throws ObjectStoreException if the item can't be stored
     */
    protected String makePublication(Integer pubMedId) throws ObjectStoreException {
        if (publications.containsKey(pubMedId)) {
            return publications.get(pubMedId);
        } else {
            Item publication = getChadoDBConverter().createItem("Publication");
            publication.setAttribute("pubMedId", pubMedId.toString());
            getChadoDBConverter().store(publication); // Stores Publication
            String publicationId = publication.getIdentifier();
            publications.put(pubMedId, publicationId);
            return publicationId;
        }
    }

    /**
     * Set the publications collection of the feature with the given (chado) feature id.
     */
    private void makeFeaturePublications(Integer featureId, List<String> argPublicationIds)
        throws ObjectStoreException {
        FeatureData fdat = featureMap.get(featureId);
        if (fdat == null) {
            throw new RuntimeException("feature " + featureId + " not found in features Map");
        }
        if (argPublicationIds.size() == 0) {
            return;
        }
        List<String> publicationIds = new ArrayList<String>(argPublicationIds);
        ReferenceList referenceList = new ReferenceList();
        referenceList.setName("publications");
        referenceList.setRefIds(publicationIds);
        getChadoDBConverter().store(referenceList, fdat.intermineObjectId);
    }

    /**
     * Return the interesting rows from the features table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getFeatureResultSet(Connection connection)
        throws SQLException {
        String query = "SELECT * FROM " + tempTableName;
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Convert the list of features to a string to be used in a SQL query.
     * @return the list of features as a string (in SQL list format)
     */
    private String getFeaturesString(List<String> featuresList) {
        StringBuffer featureListString = new StringBuffer();
        Iterator<String> i = featuresList.iterator();
        while (i.hasNext()) {
            String item = i.next();
            featureListString.append("'" + item + "'");
            if (i.hasNext()) {
                featureListString.append(", ");
            }
        }
        return featureListString.toString();
    }

    /**
     * Return a comma separated string containing the organism_ids that with with to query from
     * chado.
     */
    private String getOrganismIdsString() {
        return StringUtil.join(getChadoDBConverter().getChadoIdToOrgDataMap().keySet(), ", ");
    }

    /**
     * Create a temporary table containing only the feature_ids of the feature that interest us.
     * The table is used in later queries.  This is a protected method so that it can be overriden
     * for testing.
     * @param connection the Connection
     * @throws SQLException if there is a problem
     */
    protected void createFeatureTempTable(Connection connection) throws SQLException {
        List<String> featuresList = new ArrayList<String>(getFeatures());
        featuresList.addAll(getChromosomeFeatureTypes());
        String featureTypesString = getFeaturesString(featuresList);
        String organismConstraint = getOrganismConstraint();
        String orgConstraintForQuery = "";
        if (!StringUtils.isEmpty(organismConstraint)) {
            orgConstraintForQuery = " AND " + organismConstraint;
        }

        String query =
            "CREATE TEMPORARY TABLE " + tempTableName + " AS"
            + " SELECT feature_id, feature.name, uniquename, cvterm.name as type, seqlen,"
            + "        is_analysis, residues, organism_id"
            + "    FROM feature, cvterm"
            + "    WHERE cvterm.name IN (" + featureTypesString  + ")"
            + orgConstraintForQuery
            + "        AND NOT feature.is_obsolete"
            + "        AND feature.type_id = cvterm.cvterm_id "
            + (getExtraFeatureConstraint() != null
               ? " AND (" + getExtraFeatureConstraint() + ")"
               : "");
        Statement stmt = connection.createStatement();
        LOG.info("executing: " + query);
        stmt.execute(query);
        String idIndexQuery = "CREATE INDEX " + tempTableName + "_feature_index ON "
            + tempTableName + "(feature_id)";
        LOG.info("executing: " + idIndexQuery);
        stmt.execute(idIndexQuery);
        String typeIndexQuery = "CREATE INDEX " + tempTableName + "_type_index ON "
            + tempTableName + "(type)";
        LOG.info("executing: " + typeIndexQuery);
        stmt.execute(typeIndexQuery);
        String analyze = "ANALYZE " + tempTableName;
        LOG.info("executing: " + analyze);
        stmt.execute(analyze);
    }

    /**
     * Return some SQL that can be included in the WHERE part of query that restricts features
     * by organism.  "organism_id" must be selected.
     * @return the SQL
     */
    protected String getOrganismConstraint() {
        String organismIdsString = getOrganismIdsString();
        if (StringUtils.isEmpty(organismIdsString)) {
            return "";
        } else {
            return "organism_id IN (" + organismIdsString + ")";
        }
    }

    /**
     * Return an extra constraint to be used when querying the feature table.  Any feature table
     * column or cvterm table column can be constrained.  The cvterm will match the type_id field
     * in the feature.
     * eg. "uniquename not like 'BAD_ID%'"
     * @return the constraint as SQL or nul if there is no extra constraint.
     */
    protected String getExtraFeatureConstraint() {
        // no default
        return null;
    }

    /**
     * Return an SQL query that finds the feature_ids of all rows from the feature table that we
     * need to process.
     * @return the SQL string
     */
    protected String getFeatureIdQuery() {
        return "SELECT feature_id FROM " + tempTableName;
    }


    private String getChromosomeFeatureIdQuery() {
        return
            "SELECT feature_id FROM feature, cvterm"
            + "  WHERE type_id = cvterm.cvterm_id"
            + "    AND cvterm.name IN (" + getFeaturesString(getChromosomeFeatureTypes()) + ")"
            + (getExtraFeatureConstraint() != null
               ? " AND (" + getExtraFeatureConstraint() + ")"
               : "");
    }

    /**
     * Return the interesting rows from the feature_relationship table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getFeatureRelationshipResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT feature_relationship_id, subject_id, object_id, cvterm.name AS type_name"
            + "  FROM feature_relationship, cvterm"
            + "  WHERE cvterm.cvterm_id = type_id"
            + "      AND subject_id IN (" + getFeatureIdQuery() + ")"
            + "      AND object_id IN (" + getFeatureIdQuery() + ")"
            + "  ORDER BY subject_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the featureloc table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getFeatureLocResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT featureloc_id, feature_id, srcfeature_id, fmin, is_fmin_partial,"
            + "     fmax, is_fmax_partial, strand"
            + "   FROM featureloc"
            + "   WHERE feature_id IN"
            + "         (" + getFeatureIdQuery() + ")"
            + "     AND srcfeature_id IN"
            + "         (" + getChromosomeFeatureIdQuery() + ")"
            + "     AND locgroup = 0";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }


    /**
     * Return the interesting matches from the featureloc and feature tables.
     * feature<->featureloc<->match_feature<->featureloc<->feature
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getMatchLocResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT f1loc.featureloc_id, f1.feature_id, f2.feature_id AS srcfeature_id, f2loc.fmin,"
            + "     false AS is_fmin_partial, f2loc.fmax, false AS is_fmax_partial, f2loc.strand"
            + "   FROM feature match, feature f1, featureloc f1loc, feature f2, featureloc f2loc,"
            + "        cvterm mt"
            + "  WHERE match.feature_id = f1loc.feature_id AND match.feature_id = f2loc.feature_id"
            + "    AND f1loc.srcfeature_id = f1.feature_id AND f2loc.srcfeature_id = f2.feature_id"
            + "    AND match.type_id = mt.cvterm_id AND mt.name IN ('match', 'cDNA_match')"
            + "    AND f1.feature_id <> f2.feature_id"
            + "    AND f1.feature_id IN (" + getFeatureIdQuery() + ")"
            + "    AND f2.feature_id IN (" + getChromosomeFeatureIdQuery() + ")";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the dbxref table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDbxrefResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT feature.feature_id, accession, db.name AS db_name, is_current"
            + "  FROM dbxref, feature_dbxref, feature, db"
            + "  WHERE feature_dbxref.dbxref_id = dbxref.dbxref_id "
            + "    AND feature_dbxref.feature_id = feature.feature_id "
            + "    AND feature.feature_id IN"
            + "        (" + getFeatureIdQuery() + ")"
            + "    AND dbxref.db_id = db.db_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the featureprop table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getFeaturePropResultSet(Connection connection) throws SQLException {
        String query =
            "select feature_id, value, cvterm.name AS type_name FROM featureprop, cvterm"
            + "   WHERE featureprop.type_id = cvterm.cvterm_id"
            + "       AND feature_id IN (" + getFeatureIdQuery() + ")";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the feature_cvterm/cvterm table.  Only returns rows for
     * those features returned by getFeatureIdQuery().
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getFeatureCVTermResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT DISTINCT feature_id, cvterm.cvterm_id, cvterm.name AS cvterm_name,"
            + "              cv.name AS cv_name "
            + "  FROM feature_cvterm, cvterm, cv "
            + " WHERE feature_id IN (" + getFeatureIdQuery() + ")"
            + "   AND cvterm.cvterm_id = feature_cvterm.cvterm_id "
            + "   AND cvterm.cv_id = cv.cv_id "
            + " ORDER BY feature_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the synonym table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getSynonymResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT DISTINCT feature_id, synonym.name AS synonym_name,"
            + "              cvterm.name AS type_name, is_current"
            + "  FROM feature_synonym, synonym, cvterm"
            + "  WHERE feature_synonym.synonym_id = synonym.synonym_id"
            + "     AND synonym.type_id = cvterm.cvterm_id"
            + "     AND feature_id IN (" + getFeatureIdQuery() + ")"
            + "  ORDER BY is_current DESC";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the pub table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getPubResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT DISTINCT feature_pub.feature_id, dbxref.accession as pub_db_identifier"
            + "  FROM feature_pub, dbxref, db, pub, pub_dbxref"
            + "  WHERE feature_pub.pub_id = pub.pub_id"
            + "    AND pub_dbxref.dbxref_id = dbxref.dbxref_id"
            + "    AND dbxref.db_id = db.db_id"
            + "    AND pub.pub_id = pub_dbxref.pub_id"
            + "    AND db.name = 'pubmed'"
            + "    AND feature_id IN (" + getFeatureIdQuery() + ")"
            + "  ORDER BY feature_pub.feature_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Call DataConverter.createSynonym(), store the Item then record in FeatureData that we've
     * created it.
     * @param fdat the FeatureData
     * @param type the synonym type
     * @param identifier the identifier to store in the Synonym
     * @param isPrimary true if the synonym is a primary identifier
     * @param otherEvidence the evidence collection to store in the Synonym
     * @return the new Synonym
     * @throws ObjectStoreException if there is a problem while storing
     */
    protected Item createSynonym(FeatureData fdat, String type, String identifier,
                                 boolean isPrimary, List<Item> otherEvidence)
        throws ObjectStoreException {
        if (fdat.existingSynonyms.contains(identifier)) {
            throw new IllegalArgumentException("feature identifier " + identifier
                                               + " is already a synonym for: "
                                               + fdat.existingSynonyms);
        }
        List<Item> allEvidence = new ArrayList<Item>();
        if (otherEvidence != null) {
            allEvidence.addAll(otherEvidence);
        }
        Item returnItem = getChadoDBConverter().createSynonym(fdat.itemIdentifier, type,
                                                              identifier, isPrimary,
                                                              allEvidence);
        fdat.existingSynonyms.add(identifier);
        return returnItem;
    }

    /**
     * Fetch the populated map of chado feature id to FeatureData objects.
     * @return map of feature details
     */
    protected Map<Integer, FeatureData> getFeatureMap() {
        return this.featureMap;
    }

    /**
     * Fetch the populated map of chromosome-like features.  The keys are the chromosome uniqueName
     * fields and the values are the chado feature_ids.
     * @param organismId the chado organism_id
     * @return map of chromosome details
     */
    protected Map<String, Integer> getChromosomeFeatureMap(Integer organismId) {
        return chromosomeMaps.get(organismId);
    }

    /**
     * Data about one feature from the feature table in chado.  This exists to avoid having lots of
     * Item objects in memory.
     *
     * @author Kim Rutherford
     */
    protected static class FeatureData
    {
        private OrganismData organismData;
        private String uniqueName;
        private String chadoFeatureName;
        // the synonyms that have already been created
        private final Set<String> existingSynonyms
            = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        private String itemIdentifier;
        private String interMineType;
        private Integer intermineObjectId;

        private short flagss = 0;
        static final short EVIDENCE_CREATED = 0;
        static final short IDENTIFIER_SET = 1;
        static final short LENGTH_SET = 2;
        static final short DATASET_SET = 3;

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getIntermineObjectId() {
            return intermineObjectId;
        }

        /**
         * Get the String read from the name column of the feature table.
         * @return the name
         */
        public String getChadoFeatureName() {
            return chadoFeatureName;
        }

        /**
         * Get the String read from the uniquename column of the feature table.
         * @return the uniquename
         */
        public String getChadoFeatureUniqueName() {
            return uniqueName;
        }

        /**
         * Return the InterMine Item identifier for this feature.
         * @return the InterMine Item identifier
         */
        public String getItemIdentifier() {
            return itemIdentifier;
        }

        /**
         * Return the OrganismData object for the organism this feature comes from.
         * @return the OrganismData object
         */
        public OrganismData getOrganismData() {
            return organismData;
        }

        /**
         * Return the InterMine type of this object
         * @return the InterMine type
         */
        public String getInterMineType() {
            return interMineType;
        }

        private int shift(short flag) {
            return (2 << flag);
        }

        /**
         * Get the given flag.
         * @param flag the flag constant eg. LENGTH_SET_BIT
         * @return true if the flag is set
         */
        public boolean getFlag(short flag) {
            return (flagss & shift(flag)) != 0;
        }

        /**
         * Set a flag
         * @param flag the flag constant
         * @param value the new value
         */
        public void setFlag(short flag, boolean value) {
            if (value) {
                flagss |= shift(flag);
            } else {
                flagss &= ~shift(flag);
            }
        }
    }
}
