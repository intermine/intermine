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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.log4j.Logger;

/**
 * DataConverter to read from a Chado database into items
 * @author Kim Rutherford
 */
public class ChadoDBConverter extends BioDBConverter
{
    private static class FeatureData
    {
        String uniqueName;
        // the synonyms that have already been created
        Set<String> existingSynonyms = new HashSet<String>();
        String itemIdentifier;
        String interMineType;
        Integer intermineObjectId;
        short flags = 0;
        static final short EVIDENCE_CREATED_BIT = 0;
        static final short EVIDENCE_CREATED = 1 << EVIDENCE_CREATED_BIT;
    }

    protected static final Logger LOG = Logger.getLogger(ChadoDBConverter.class);

    private Map<Integer, FeatureData> features = new HashMap<Integer, FeatureData>();
    private String dataSourceName;
    private String dataSetTitle;
    private int taxonId = -1;
    private String genus;
    private String species;
    private String sequenceFeatureTypesString = "'chromosome', 'chromosome_arm'";
    private String featureTypesString =
        "'gene', 'mRNA', 'transcript', 'CDS', 'intron', 'exon', "
        + "'regulatory_region', 'enhancer', "
        // ignore for now:        + "'EST', 'cDNA_clone', "
        + "'miRNA', 'snRNA', 'ncRNA', 'rRNA', 'ncRNA', 'snoRNA', 'tRNA', "
        + "'chromosome_band', 'transposable_element_insertion_site', "
        + "'chromosome_structure_variation', 'protein', 'point_mutation', "
        + "'five_prime_untranslated_region', "
        + "'five_prime_UTR', 'three_prime_untranslated_region', 'three_prime_UTR', "
        + sequenceFeatureTypesString;

    private int chadoOrganismId;
    private Model model = Model.getInstanceByName("genomic");
    private MultiKeyMap config = null;

    private static final List<String> PARTOF_RELATIONS = Arrays.asList("partof", "part_of");

    private static final List<Item> EMPTY_ITEM_LIST = Collections.emptyList();

    /**
     * A class that represents an action while processing synonyms, dbxrefs, etc.
     * @author Kim Rutherford
     */
    protected static class ConfigAction
    {
        protected ConfigAction() {
            // empty
        }
    }

    /**
     * An action that sets an attribute in a new Item.
     */
    protected static class SetFieldConfigAction extends ConfigAction
    {
        private String fieldName;
        SetFieldConfigAction() {
            fieldName = null;
        }
        SetFieldConfigAction(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    /**
     *  An action that sets a Synonym.
     */
    protected static class CreateSynonymAction extends ConfigAction
    {
        private String synonymType;

        // make a synonym and use the type from chado ("symbol", "identifier" etc.) as the Synonym
        // type
        CreateSynonymAction() {
            synonymType = null;
        }

        // make a synonym and use given type as the Synonym type
        CreateSynonymAction(String synonymType) {
            this.synonymType = synonymType;
        }
    }

    private static class DoNothingAction extends ConfigAction
    {
        // do nothing for this data
    }

    /**
     * An action that make a synonym.
     */
    protected static final ConfigAction CREATE_SYNONYM_ACTION = new CreateSynonymAction();

    protected static final ConfigAction DO_NOTHING_ACTION = new DoNothingAction();

    /**
     * Create a new ChadoDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public ChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
    }

    @SuppressWarnings("unchecked")
    protected Map<MultiKey, List<ConfigAction>> getConfig() {
        if (config == null) {
            config = new MultiKeyMap();
        }
        return config;
    }

    /**
     * Set the name of the DataSet Item to create for this converter.
     * @param title the title
     */
    public void setDataSetTitle(String title) {
        this.dataSetTitle = title;
    }

    /**
     * Set the name of the DataSource Item to create for this converter.
     * @param name the name
     */
    public void setDataSourceName(String name) {
        this.dataSourceName = name;
    }

    /**
     * Set the taxonId to use when creating the Organism Item for the new features.
     * @param taxonId the taxon id
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = Integer.valueOf(taxonId).intValue();
    }

    /**
     * Get the taxonId to use when creating the Organism Item for the
     * @return the taxon id
     */
    public int getTaxonIdInt() {
        return taxonId;
    }

    /**
     * The genus to use when querying for features.
     * @param genus the genus
     */
    public void setGenus(String genus) {
        this.genus = genus;
    }

    /**
     * The species to use when querying for features.
     * @param species the species
     */
    public void setSpecies(String species) {
        this.species = species;
    }

    /**
     * Process the data from the Database and write to the ItemWriter.
     * {@inheritDoc}
     */
    @Override
    public void process() throws Exception {
        Connection connection;
        if (getDatabase() == null) {
            // no Database when testing and no connectio needed
            connection = null;
        } else {
            connection = getDatabase().getConnection();
        }

        if (dataSetTitle == null) {
            throw new IllegalArgumentException("dataSetTitle not set in ChadoDBConverter");
        }
        if (dataSourceName == null) {
            throw new IllegalArgumentException("dataSourceName not set in ChadoDBConverter");
        }
        if (getTaxonIdInt() == -1) {
            throw new IllegalArgumentException("taxonId not set in ChadoDBConverter");
        }
        if (species == null) {
            throw new IllegalArgumentException("species not set in ChadoDBConverter");
        }
        if (genus == null) {
            throw new IllegalArgumentException("genus not set in ChadoDBConverter");
        }
        chadoOrganismId = getChadoOrganismId(connection);
        processFeatureTable(connection);
        processPubTable(connection);
        processLocationTable(connection);
        processRelationTable(connection);
        processDbxrefTable(connection);
        processSynonymTable(connection);
        processFeaturePropTable(connection);
        addMissingDataEvidence();
    }

    private void processFeatureTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Item dataSet = getDataSetItem(dataSetTitle); // Stores DataSet
        Item dataSource = getDataSourceItem(dataSourceName); // Stores DataSource
        Item organismItem = getOrganismItem(getTaxonIdInt()); // Stores Organism
        ResultSet res = getFeatureResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String name = res.getString("name");
            String uniqueName = res.getString("uniquename");
            String type = res.getString("type");
            int seqlen = 0;
            if (res.getObject("seqlen") != null) {
                seqlen = res.getInt("seqlen");
            }
            List<String> primaryIds = new ArrayList<String>();
            primaryIds.add(uniqueName);
            String interMineType = TypeUtil.javaiseClassName(fixFeatureType(type));
            uniqueName = fixIdentifier(interMineType, uniqueName);
            Item feature =  makeFeature(featureId, type, interMineType, name, uniqueName, seqlen);
            if (feature != null) {
                FeatureData fdat = new FeatureData();
                fdat.itemIdentifier = feature.getIdentifier();
                fdat.uniqueName = uniqueName;
                fdat.interMineType = XmlUtil.getFragmentFromURI(feature.getClassName());
                feature.setReference("organism", organismItem);
                MultiKey nameKey =
                    new MultiKey("feature", fdat.interMineType, dataSourceName, "name");
                List<ConfigAction> nameActionList = getConfig().get(nameKey);
                MultiKey uniqueNameKey =
                    new MultiKey("feature", fdat.interMineType, dataSourceName, "uniquename");
                List<ConfigAction> uniqueNameActionList = getConfig().get(uniqueNameKey);

                if (name != null) {
                    if (nameActionList == null || nameActionList.size() == 0) {
                        if (feature.checkAttribute("symbol")) {
                            feature.setAttribute("symbol", name);
                        } else {
                            if (feature.checkAttribute("symbol")) {
                                feature.setAttribute("symbol", name);
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
                                feature.setAttribute(attrAction.fieldName, name);
                            }
                        }
                    }
                }

                if (uniqueNameActionList == null || uniqueNameActionList.size() == 0) {
                    feature.setAttribute("identifier", uniqueName);
                } else {
                    for (ConfigAction action: uniqueNameActionList) {
                        if (action instanceof SetFieldConfigAction) {
                            SetFieldConfigAction attrAction = (SetFieldConfigAction) action;
                            feature.setAttribute(attrAction.fieldName, uniqueName);
                        }
                    }
                }

                // don't set the evidence collection - that's done by processPubTable()
                fdat.intermineObjectId = store(feature); // Stores Feature

                // always create a synonym for the uniquename
                createSynonym(fdat, "identifier", uniqueName, true, dataSet, EMPTY_ITEM_LIST,
                              dataSource); // Stores Synonym

                if (name != null) {
                    if (nameActionList == null || nameActionList.size() == 0
                        || nameActionList.contains(CREATE_SYNONYM_ACTION)) {
                        name = fixIdentifier(interMineType, name);
                        if (!fdat.existingSynonyms.contains(name)) {
                            createSynonym(fdat, "name", name, false, dataSet, EMPTY_ITEM_LIST,
                                          dataSource); // Stores Synonym
                        }
                    }
                }
                features.put(featureId, fdat);
                count++;
            }
        }
        LOG.info("created " + count + " features");
        res.close();
    }

    /**
     * Make and store a new feature
     * @param featureId the chado feature id
     * @param chadoFeatureType the chado feature type (a SO term)
     * @param interMineType the InterMine type of the feature
     * @param name the name
     * @param uniqueName the uniquename
     * @param seqlen the sequence length (if known)
     */
    protected Item makeFeature(Integer featureId, String chadoFeatureType, String interMineType,
                               String name, String uniqueName,
                               int seqlen) {
        return createItem(interMineType);
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

    private void processLocationTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Item dataSet = getDataSetItem(dataSetTitle);

        ResultSet res = getFeatureLocResultSet(connection);
        int count = 0;
        int featureWarnings = 0;
        while (res.next()) {
            Integer featureLocId = new Integer(res.getInt("featureloc_id"));
            Integer featureId = new Integer(res.getInt("feature_id"));
            Integer srcFeatureId = new Integer(res.getInt("srcfeature_id"));
            int start = res.getInt("fmin") + 1;
            int end = res.getInt("fmax");
            int strand = res.getInt("strand");
            if (features.containsKey(srcFeatureId)) {
                FeatureData srcFeatureData = features.get(srcFeatureId);
                if (features.containsKey(featureId)) {
                    FeatureData featureData = features.get(featureId);
                    makeLocation(srcFeatureData.itemIdentifier, featureData.itemIdentifier,
                                 start, end, strand, getTaxonIdInt(), dataSet); // Stores Location
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

    private void processRelationTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getFeatureRelationshipResultSet(connection);
        Integer lastSubjectId = null;

        // Map from relation type to Map from object type to FeatureData
        Map<String, Map<String, List<FeatureData>>> relTypeMap =
            new HashMap<String, Map<String, List<FeatureData>>>();
        int featureWarnings = 0;
        int count = 0;
        int collectionTotal = 0;
        while (res.next()) {
            Integer featRelationshipId = new Integer(res.getInt("feature_relationship_id"));
            Integer subjectId = new Integer(res.getInt("subject_id"));
            Integer objectId = new Integer(res.getInt("object_id"));
            String relationTypeName = res.getString("type_name");

            if (lastSubjectId != null && subjectId != lastSubjectId) {
                processCollectionData(lastSubjectId, relTypeMap); // Stores stuff
                collectionTotal += relTypeMap.size();
                relTypeMap = new HashMap<String, Map<String, List<FeatureData>>>();
            }
            if (features.containsKey(subjectId)) {
                if (features.containsKey(objectId)) {
                    FeatureData objectFeatureData = features.get(objectId);
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
            processCollectionData(lastSubjectId, relTypeMap); // Stores stuff
            collectionTotal += relTypeMap.size();
        }
        LOG.info("processed " + count + " relations");
        LOG.info("total collection elements created: " + collectionTotal);
        res.close();
    }

    /**
     * Create collections and references for the Item given by chadoSubjectId.
     */
    private void processCollectionData(Integer chadoSubjectId,
                                       Map<String, Map<String, List<FeatureData>>> relTypeMap)
        throws ObjectStoreException {
        FeatureData subjectData = features.get(chadoSubjectId);
        if (subjectData == null) {
            LOG.warn("unknown feature " + chadoSubjectId + " passed to processCollectionData - "
                     + "ignoring");
            return;
        }

        // map from collection name to list of item ids
        Map<String, List<String>> collectionsToStore = new HashMap<String, List<String>>();

        String subjectInterMineType = subjectData.interMineType;
        Integer intermineItemId = subjectData.intermineObjectId;
        for (Map.Entry<String, Map<String, List<FeatureData>>> entry: relTypeMap.entrySet()) {
            String relationType = entry.getKey();
            Map<String, List<FeatureData>> objectClassFeatureDataMap = entry.getValue();

            Set<Entry<String, List<FeatureData>>> mapEntries = objectClassFeatureDataMap.entrySet();
            for (Map.Entry<String, List<FeatureData>> featureDataMap: mapEntries) {
                String objectClass = featureDataMap.getKey();
                List<FeatureData> featureDataCollection = featureDataMap.getValue();
                ClassDescriptor cd = model.getClassDescriptorByName(subjectInterMineType);
                List<FieldDescriptor> fds = null;

                FeatureData subjectFeatureData = features.get(chadoSubjectId);
                // key example: ("relationship", "Translation", "producedby", "MRNA")
                MultiKey key = new MultiKey("relationship", subjectFeatureData.interMineType,
                                            relationType, objectClass);
                List<ConfigAction> actionList = getConfig().get(key);

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
                            String fieldName = setAction.fieldName;
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
                                store(reference, intermineItemId); // Stores Reference for Feature

                                // special case for 1-1 relations - we need to set the reverse
                                // reference
                                ReferenceDescriptor rd = (ReferenceDescriptor) fd;
                                ReferenceDescriptor reverseRD = rd.getReverseReferenceDescriptor();
                                if (reverseRD != null && !reverseRD.isCollection()) {
                                    Reference revReference = new Reference();
                                    revReference.setName(reverseRD.getName());
                                    revReference.setRefId(subjectData.itemIdentifier);
                                    store(revReference, referencedFeatureData.intermineObjectId);
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
            referenceList.setName(entry.getKey());
            referenceList.setRefIds(entry.getValue());
            store(referenceList, intermineItemId); // Stores ReferenceList for Feature
        }
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
            Set<String> parentClasses = ClassDescriptor.findSuperClassNames(model, objectType);
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
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle);

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

            if (features.containsKey(featureId)) {
                FeatureData fdat = features.get(featureId);
                accession  = fixIdentifier(fdat.interMineType, accession);
                MultiKey key = new MultiKey("dbxref", fdat.interMineType, dbName, isCurrent);
                List<ConfigAction> actionList = getConfig().get(key);

                if (actionList == null) {
                    // try ignoring isCurrent
                    MultiKey key2 = new MultiKey("dbxref", fdat.interMineType, dbName, null);
                    actionList = getConfig().get(key2);
                }

                if (actionList == null) {
                    // no actions configured for this synonym
                    continue;
                }
                for (ConfigAction action: actionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                        if (!existingAttributes.contains(setAction.fieldName)) {
                            setAttribute(fdat, setAction.fieldName, accession); // Stores
                                                                         // Attribute for Feature
                            existingAttributes.add(setAction.fieldName);
                        }
                    } else {
                        if (action instanceof CreateSynonymAction) {
                            if (fdat.existingSynonyms.contains(accession)) {
                                continue;
                            } else {
                                createSynonym(fdat, "identifier", accession, false, dataSet,
                                              EMPTY_ITEM_LIST, dataSource); // Stores Synonym
                                count++;
                            }
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
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle);

        ResultSet res = getFeaturePropResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String identifier = res.getString("value");
            String propTypeName = res.getString("type_name");

            if (features.containsKey(featureId)) {
                FeatureData fdat = features.get(featureId);
                MultiKey key = new MultiKey("prop", fdat.interMineType, propTypeName);
                List<ConfigAction> actionList = getConfig().get(key);
                if (actionList == null) {
                    // no actions configured for this prop
                    continue;
                }

                for (ConfigAction action: actionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                        setAttribute(fdat, setAction.fieldName, identifier); // Stores
                                                                        // Attribute for Feature
                    } else {
                        if (action instanceof CreateSynonymAction) {
                            CreateSynonymAction synonymAction = (CreateSynonymAction) action;
                            Set<String> existingSynonyms = fdat.existingSynonyms;
                            if (existingSynonyms.contains(identifier)) {
                                continue;
                            } else {
                                String synonymType = synonymAction.synonymType;
                                if (synonymType == null) {
                                    synonymType = propTypeName;
                                }
                                createSynonym(fdat, synonymType, identifier, false, dataSet,
                                              EMPTY_ITEM_LIST, dataSource); // Stores Synonym
                                count++;
                            }
                        }
                    }
                }
            }
        }
        LOG.info("created " + count + " synonyms from the featureprop table");
        res.close();
    }

    private void processSynonymTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle);

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

            if (features.containsKey(featureId)) {
                FeatureData fdat = features.get(featureId);
                MultiKey key =
                    new MultiKey("synonym", fdat.interMineType, synonymTypeName, isCurrent);
                List<ConfigAction> actionList = getConfig().get(key);

                if (actionList == null) {
                    // try ignoring isCurrent
                    MultiKey key2 =
                        new MultiKey("synonym", fdat.interMineType, synonymTypeName, null);
                    actionList = getConfig().get(key2);
                }
                if (actionList == null) {
                    // no actions configured for this synonym
                    continue;
                }
                for (ConfigAction action: actionList) {
                    if (action instanceof SetFieldConfigAction) {
                        SetFieldConfigAction setAction = (SetFieldConfigAction) action;
                        if (!existingAttributes.contains(setAction.fieldName)) {
                            setAttribute(fdat, setAction.fieldName, identifier); // Stores
                                                                        // Attribute for Feature
                            existingAttributes.add(setAction.fieldName);
                        }
                    } else {
                        if (action instanceof CreateSynonymAction) {
                            if (fdat.existingSynonyms.contains(identifier)) {
                                continue;
                            } else {
                                createSynonym(fdat, synonymTypeName, identifier, false, dataSet,
                                              EMPTY_ITEM_LIST, dataSource); // Stores Synonym
                                count++;
                            }
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
     * @param the (SO) type of the feature that this identifier came from
     * @param identifier the identifier
     * @return a cleaned identifier
     */
    protected String fixIdentifier(String type, String identifier) {
        /*
         * default implementation should be: return identifier
         */
        // XXX FIXME TODO - for wormbase - move to WormBaseDBConverter
        if (identifier.startsWith(type + ":")) {
            return identifier.substring(type.length() + 1);
        } else {
            return identifier;
        }
    }

    /**
     * Set an attribute in an Item by creating an Attribute object and storing it.
     * @param fdat the data about the feature
     * @param attributeName the attribute name
     * @param value the value to set
     */
    private void setAttribute(FeatureData fdat, String attributeName, String value)
        throws ObjectStoreException {
        Attribute att = new Attribute();
        att.setName(attributeName);
        att.setValue(value);
        store(att, fdat.intermineObjectId);
    }

    private void processPubTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getPubResultSet(connection);

        List<String> currentEvidenceIds = new ArrayList<String>();
        Integer lastPubFeatureId = null;
        int featureWarnings = 0;
        int count = 0;

        Map<String, String> pubs = new HashMap<String, String>();

        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            if (!features.containsKey(featureId)) {
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
            String pubMedId = res.getString("pub_db_identifier");
            if (lastPubFeatureId != null && !featureId.equals(lastPubFeatureId)) {
                makeFeaturePublications(lastPubFeatureId, currentEvidenceIds);
                makeFeatureEvidence(lastPubFeatureId, currentEvidenceIds); // Stores ReferenceList
                currentEvidenceIds = new ArrayList<String>();
            }
            String publicationId;
            if (pubs.containsKey(pubMedId)) {
                publicationId = pubs.get(pubMedId);
            } else {
                Item publication = createItem("Publication");
                publication.setAttribute("pubMedId", pubMedId);
                store(publication); // Stores Publication
                publicationId = publication.getIdentifier();
                pubs.put(pubMedId, publicationId);
            }
            currentEvidenceIds.add(publicationId);
            lastPubFeatureId = featureId;
            count++;
        }

        if (lastPubFeatureId != null) {
            makeFeaturePublications(lastPubFeatureId, currentEvidenceIds);
            makeFeatureEvidence(lastPubFeatureId, currentEvidenceIds);
        }
        LOG.info("Created " + count + " publications");
        res.close();
    }

    /**
     * Set the publications collection of the feature with the given (chado) feature id.
     */
    private void makeFeaturePublications(Integer featureId, List<String> argPublicationIds)
        throws ObjectStoreException {
        FeatureData fdat = features.get(featureId);
        if (fdat == null) {
            throw new RuntimeException("feature " + featureId + " not found in features Map");
        }
        if (!fdat.interMineType.equals("Gene")) {
            // only Gene has a publications collection
            return;
        }
        List<String> publicationIds = new ArrayList<String>(argPublicationIds);
        ReferenceList referenceList = new ReferenceList();
        referenceList.setName("publications");
        referenceList.setRefIds(publicationIds);
        store(referenceList, fdat.intermineObjectId);
    }

    /**
     * Set the evidence collection of the feature with the given (chado) feature id.
     */
    private void makeFeatureEvidence(Integer featureId, List<String> argEvidenceIds)
        throws ObjectStoreException {
        FeatureData fdat = features.get(featureId);
        if (fdat == null) {
            throw new RuntimeException("feature " + featureId + " not found in features Map");
        }
        List<String> evidenceIds = new ArrayList<String>(argEvidenceIds);
        Item dataSet = getDataSetItem(dataSetTitle);
        evidenceIds.add(0, dataSet.getIdentifier());

        ReferenceList referenceList = new ReferenceList();
        referenceList.setName("evidence");

        referenceList.setRefIds(evidenceIds);
        store(referenceList, fdat.intermineObjectId);

        fdat.flags |= FeatureData.EVIDENCE_CREATED;
    }

    /**
     * For those features in the features Map that don't yet have a evidence collection, create one
     * containing the DataSet.  We know if a feature doesn't have an evidence collection if it
     * doesn't have it's EVIDENCE_CREATED flag set.
     */
    private void addMissingDataEvidence() throws ObjectStoreException {
        List<String> emptyList = Collections.emptyList();
        for (Map.Entry<Integer, FeatureData> entry: features.entrySet()) {
            Integer featureId = entry.getKey();
            FeatureData featureData = entry.getValue();
            if ((featureData.flags & FeatureData.EVIDENCE_CREATED) == 0) {
                makeFeatureEvidence(featureId, emptyList);
            }
        }
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
        String query =
            "SELECT feature_id, feature.name, uniquename, cvterm.name as type, seqlen, is_analysis"
            + "   FROM feature, cvterm"
            + "   WHERE feature.type_id = cvterm.cvterm_id"
            + "        AND cvterm.name IN (" + featureTypesString + ")"
            + "        AND organism_id = " + chadoOrganismId
            + "        AND NOT feature.is_obsolete";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the chado organism id for the given genus/species.  This is a protected method so
     * that it can be overriden for testing
     * @param connection the db connection
     * @return the internal id (organism_id from the organism table)
     * @throws SQLException if the is a database problem
     */
    protected int getChadoOrganismId(Connection connection)
        throws SQLException {
        String query = "select organism_id from organism where genus = "
            + DatabaseUtil.objectToString(genus) + " and species = "
            + DatabaseUtil.objectToString(species);
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        if (res.next()) {
            return res.getInt(1);
        } else {
            throw new RuntimeException("no rows returned when querying organism table for genus \""
                                       + genus + "\" and species \"" + species + "\"");
        }
    }

    /**
     * Return a SQL query string the gets all non-obsolete interesting features.
     */
    private String getFeatureIdQuery() {
        return
          " SELECT feature_id FROM feature, cvterm"
        + "             WHERE cvterm.name IN (" + featureTypesString + ")"
        + "                 AND organism_id = " + chadoOrganismId
        + "                 AND NOT feature.is_obsolete"
        + "                 AND feature.type_id = cvterm.cvterm_id";
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
            + "         (" + getFeatureIdQuery() + ")"
            + "     AND locgroup = 0";
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
            + "     AND feature_id IN (" + getFeatureIdQuery() + ")";
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
     * Call super.createSynonym(), store the Item then record in fdat that we've created it.
     */
    private Item createSynonym(FeatureData fdat, String type, String identifier,
                               boolean isPrimary, Item dataSet, List<Item> otherEvidence,
                               Item dataSource)
        throws ObjectStoreException {
        if (fdat.existingSynonyms.contains(identifier)) {
            throw new IllegalArgumentException("feature identifier " + identifier
                                               + " is already a synonym for: "
                                               + fdat.existingSynonyms);
        }
        List<Item> allEvidence = new ArrayList<Item>();
        allEvidence.add(dataSet);
        allEvidence.addAll(otherEvidence);
        Item returnItem = createSynonym(fdat.itemIdentifier, type, identifier, isPrimary,
                                        allEvidence, dataSource);
        fdat.existingSynonyms.add(identifier);
        return returnItem;
    }
}
