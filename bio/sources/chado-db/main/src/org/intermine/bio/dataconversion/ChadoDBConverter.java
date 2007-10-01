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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * DataConverter to read from a Chado database into items
 * @author Kim Rutherford
 */
public class ChadoDBConverter extends BioDBConverter
{
    private class FeatureData
    {
        String uniqueName;
        // the synonyms that have already been created
        Set<String> existingSynonyms = new HashSet<String>();
        String itemIdentifier;
        String interMineType;
        public Integer intermineObjectId;
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
        "'gene', 'mRNA', 'transcript', 'CDS', 'intron', 'exon', 'five_prime_untranslated_region', "
        + "'EST', 'cDNA_clone', 'miRNA', 'snRNA', 'ncRNA', 'rRNA', 'ncRNA', 'snoRNA', 'tRNA', "
        + "'five_prime_UTR', 'three_prime_untranslated_region', 'three_prime_UTR', "
        + sequenceFeatureTypesString;
    private String relationshipTypesString = "'partof'";
    private int chadoOrganismId;
    private Model model = Model.getInstanceByName("genomic");

    /**
     * Create a new ChadoDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public ChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
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
     * Set the taxonId to use when creating the Organism Item for the
     * @param taxonId the taxon id
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = Integer.valueOf(taxonId).intValue();
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
        if (taxonId == -1) {
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
        processLocationTable(connection);
        processRelationTable(connection);
        processDbxrefTable(connection);
        processSynonymTable(connection);
        processFeaturePropTable(connection);
    }

    private void processFeatureTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Item dataSet = getDataSetItem(dataSetTitle);
        Item dataSource = getDataSourceItem(dataSourceName);
        Item organismItem = getOrganismItem(taxonId);
        ResultSet res = getFeatureResultSet(connection);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String name = res.getString("name");
            String uniqueName = res.getString("uniquename");
            String type = res.getString("type");
            type = fixFeatureType(type);
            String residues = res.getString("residues");
            int seqlen = 0;
            if (res.getObject("seqlen") != null) {
                seqlen = res.getInt("seqlen");
            }
            List<String> primaryIds = new ArrayList<String>();
            primaryIds.add(uniqueName);
            String interMineType = TypeUtil.javaiseClassName(type);
            Item feature =
                makeFeature(featureId, name, uniqueName, interMineType, residues, seqlen);
            if (feature != null) {
                FeatureData fdat = new FeatureData();
                fdat.itemIdentifier = feature.getIdentifier();
                fdat.uniqueName = uniqueName;
                fdat.interMineType = interMineType;
                feature.setReference("organism", organismItem);
                feature.addToCollection("evidence", dataSet);
                createSynonym(fdat, "identifier", uniqueName, true, dataSet, Collections.EMPTY_LIST,
                              dataSource);
                if (name != null) {
                    createSynonym(fdat, "name", name, false, dataSet, Collections.EMPTY_LIST,
                                  dataSource);
                }
                fdat.intermineObjectId = store(feature);
                features.put(featureId, fdat);
            }
        }
    }

    /**
     * Make and store a new feature
     * @param featureId the chado feature id
     * @param name the name
     * @param uniqueName the uniquename
     * @param clsName the InterMine feature class
     * @param residues the residues (if any)
     * @param seqlen the sequence length (if known)
     * @throws ObjectStoreException if there is a problem while storing
     */
    protected Item makeFeature(Integer featureId, String name, String uniqueName, String clsName,
                               String residues, int seqlen) {

        // XXX FIMXE TODO HACK - this should be configured somewhere
        if (uniqueName.startsWith("FBal")) {
            return null;
        }

        Item feature = createItem(clsName);

        // XXX FIMXE TODO HACK - this should be configured somewhere
        if (feature.hasAttribute("organismDbId")) {
            if (name != null) {
                feature.setAttribute("identifier", name);
            }
            feature.setAttribute("organismDbId", uniqueName);
        } else {
            feature.setAttribute("identifier", uniqueName);
        }
        return feature;
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
                    makeLocation(srcFeatureData.uniqueName, featureData.itemIdentifier,
                                 start, end, strand, taxonId, dataSet);
                } else {
                    throw new RuntimeException("featureId (" + featureId + ") from location "
                                               + featureLocId
                                               + " was not found in the feature table");
                }
            } else {
                throw new RuntimeException("srcfeature_id (" + srcFeatureId + ") from location "
                                           + featureLocId + " was not found in the feature table");
            }
        }
    }

    private void processRelationTable(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getFeatureRelationshipResultSet(connection);
        Integer lastSubjectId = null;
        Map<String, List<String>> collectionData = new HashMap<String, List<String>>();
        int featureWarnings = 0;
        while (res.next()) {
            Integer featureRelationshipId = new Integer(res.getInt("feature_relationship_id"));
            Integer subjectId = new Integer(res.getInt("subject_id"));
            Integer objectId = new Integer(res.getInt("object_id"));

            if (lastSubjectId != null && subjectId != lastSubjectId) {
                processCollectionData(lastSubjectId, collectionData);
                collectionData = new HashMap<String, List<String>>();
            }
            if (features.containsKey(subjectId)) {
                if (features.containsKey(objectId)) {
                    FeatureData objectData = features.get(objectId);
                    List<String> currentList;
                    if (collectionData.containsKey(objectData.interMineType)) {
                        currentList = collectionData.get(objectData.interMineType);
                    } else {
                        currentList = new ArrayList<String>();
                        collectionData.put(objectData.interMineType, currentList);
                    }
                    currentList.add(objectData.itemIdentifier);
                } else {
                    if (featureWarnings <= 20) {
                        if (featureWarnings < 20) {
                            LOG.warn("object_id " + objectId + " from feature_relationship "
                                     + featureRelationshipId + " was not found in the feature table");
                        } else {
                            LOG.warn("further feature_relationship warnings ignored");
                        }
                        featureWarnings++;
                    }
                }
            } else {
                throw new RuntimeException("subject_id " + subjectId + " from feature_relationship "
                                           + featureRelationshipId
                                           + " was not found in the feature table");
            }
            lastSubjectId = subjectId;
        }
        if (lastSubjectId != null) {
            processCollectionData(lastSubjectId, collectionData);
        }
    }

    /**
     * Create collections and references for the Item given by chadoSubjectId.
     */
    private void processCollectionData(Integer chadoSubjectId,
                                       Map<String, List<String>> collectionData)
        throws ObjectStoreException {
        FeatureData subjectData = features.get(chadoSubjectId);
        String subjectInterMineType = subjectData.interMineType;
        Integer intermineItemId = subjectData.intermineObjectId;
        for (Map.Entry<String, List<String>> entry: collectionData.entrySet()) {
            String objectType = entry.getKey();
            List<String> collectionContents = entry.getValue();

            ClassDescriptor cd = model.getClassDescriptorByName(subjectInterMineType);
            List<FieldDescriptor> fds = getReferenceForRelationship(objectType, cd);

            if (fds.size() == 0) {
                throw new RuntimeException("can't find collection for type " + objectType
                                           + " in " + subjectInterMineType);
            }

            for (FieldDescriptor fd: fds) {
                if (fd.isReference()) {
                    if (collectionContents.size() > 1) {
                        throw new RuntimeException("found more than one object for reference "
                                                   + fd + " in class "
                                                   + subjectInterMineType
                                                   + " current subject identifier: "
                                                   + subjectData.uniqueName);
                    } else {
                        if (collectionContents.size() == 1) {
                            Reference reference= new Reference();
                            reference.setName(fd.getName());
                            reference.setRefId(collectionContents.get(0));
                            store(reference, intermineItemId);
                        }
                    }
                } else {
                    ReferenceList referenceList = new ReferenceList();
                    referenceList.setName(fd.getName());
                    referenceList.setRefIds(collectionContents);
                    store(referenceList, intermineItemId);
                }
            }
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
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String accession = res.getString("accession");
            String db_name = res.getString("db_name");
            if (features.containsKey(featureId)) {
                FeatureData fdat = features.get(featureId);
                Set<String> existingSynonyms = fdat.existingSynonyms;
                if (existingSynonyms.contains(accession)) {
                    continue;
                } else {
                    createSynonym(fdat, "identifier", accession, false, dataSet,
                                  Collections.EMPTY_LIST, dataSource);
                }
            }
        }
    }

    private void processFeaturePropTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle);

        ResultSet res = getFeaturePropResultSet(connection);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String identifier = res.getString("value");
            String typeName = res.getString("type_name");
            if (features.containsKey(featureId) && typeName.equals("symbol")) {
                FeatureData fdat = features.get(featureId);
                Set<String> existingSynonyms = fdat.existingSynonyms;
                if (existingSynonyms.contains(identifier)) {
                    continue;
                } else {
                    createSynonym(fdat, typeName, identifier, false, dataSet,
                                  Collections.EMPTY_LIST, dataSource);
                }
            }
        }
    }

    private void processSynonymTable(Connection connection)
        throws SQLException, ObjectStoreException {
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle);

        ResultSet res = getSynonymResultSet(connection);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String identifier = res.getString("synonym_name");
            String typeName = res.getString("type_name");
            if (features.containsKey(featureId)
                && (typeName.equals("symbol") || typeName.equals("fullname"))) {
                FeatureData fdat = features.get(featureId);
                Set<String> existingSynonyms = fdat.existingSynonyms;
                if (existingSynonyms.contains(identifier)) {
                    continue;
                } else {
                    createSynonym(fdat, typeName, identifier, false, dataSet, Collections.EMPTY_LIST,
                                  dataSource);
                }
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
            "SELECT feature_id, feature.name, uniquename, cvterm.name as type, residues, seqlen"
            + "   FROM feature, cvterm"
            + "   WHERE feature.type_id = cvterm.cvterm_id"
            + "        AND cvterm.name IN (" + featureTypesString + ")"
            + "        AND organism_id = " + chadoOrganismId
            + "        AND NOT feature.is_obsolete AND NOT feature.is_analysis";
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
     * Return the interesting rows from the feature_relationship table.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    private ResultSet getFeatureRelationshipResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT feature_relationship_id, subject_id, object_id, cvterm.name AS type_name"
            + "  FROM feature_relationship, cvterm"
            + "  WHERE cvterm.cvterm_id = type_id"
            + "      AND cvterm.name IN (" + relationshipTypesString  + ")"
            + "      AND subject_id IN"
            + "         (SELECT feature_id FROM feature, cvterm"
            + "             WHERE cvterm.name IN (" + featureTypesString + ")"
            + "                 AND organism_id = " + chadoOrganismId
            + "                 AND NOT feature.is_obsolete AND NOT feature.is_analysis"
            + "                 AND feature.type_id = cvterm.cvterm_id )"
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
            + "         (SELECT feature_id FROM feature, cvterm"
            + "             WHERE cvterm.name IN (" + featureTypesString + ")"
            + "                 AND organism_id = " + chadoOrganismId
            + "                 AND NOT feature.is_obsolete AND NOT feature.is_analysis"
            + "                 AND feature.type_id = cvterm.cvterm_id)"
            + "     AND srcfeature_id IN"
            + "         (SELECT feature_id FROM feature, cvterm"
            + "             WHERE cvterm.name IN (" + featureTypesString + ")"
            + "                 AND organism_id = " + chadoOrganismId
            + "                 AND NOT feature.is_obsolete AND NOT feature.is_analysis"
            + "                 AND feature.type_id = cvterm.cvterm_id )";
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
            "SELECT feature.feature_id, accession, db.name AS db_name"
            + "  FROM dbxref, feature_dbxref, feature, db"
            + "  WHERE feature_dbxref.dbxref_id = dbxref.dbxref_id "
            + "    AND feature_dbxref.feature_id = feature.feature_id "
            + "    AND feature.feature_id IN"
            + "        (SELECT feature_id FROM f_type"
            + "           WHERE type IN (" + featureTypesString + ") "
            + "           AND organism_id = " + chadoOrganismId + ")"
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
            + "       AND feature_id IN (SELECT feature_id FROM f_type"
            + "                          WHERE type IN (" + featureTypesString + ")"
            + "                              AND organism_id = " + chadoOrganismId + ")";
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
            "SELECT feature_id, synonym.name AS synonym_name, cvterm.name AS type_name"
            + "  FROM feature_synonym, synonym, cvterm"
            + "  WHERE feature_synonym.synonym_id = synonym.synonym_id"
            + "     AND synonym.type_id = cvterm.cvterm_id"
            + "     AND feature_id IN (select feature_id from f_type"
            + "                          WHERE type IN (" + featureTypesString + ")"
            + "                              AND organism_id = " + chadoOrganismId + ")";
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
        List<Item> allEvidence = new ArrayList<Item>();
        allEvidence.add(dataSet);
        allEvidence.addAll(otherEvidence);
        Item returnItem = createSynonym(fdat.itemIdentifier, type, identifier, isPrimary,
                                        allEvidence, dataSource);
        fdat.existingSynonyms.add(identifier);
        return returnItem;
    }
}
