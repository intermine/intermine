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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DataConverter to read from a Chado database into items
 * @author Kim Rutherford
 */
public class ChadoDBConverter extends BioDBConverter
{
    private class FeatureData {
        String uniqueName;
        // the synonyms that have already been created
        Set<String> existingSynonyms = new HashSet<String>();
        String itemIdentifier;
        String type;
    }
    
    private Map<Integer, FeatureData> features = new HashMap<Integer, FeatureData>();
    private String dataSourceName;
    private String dataSetTitle;
    private int taxonId = -1;
    private String genus;
    private String species;
    private String featureTypesString = "'gene', 'exon', 'transcript'";
    private int chadoOrganismId;

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
        processRelationTable(connection);
        processDbxrefTable(connection);
        processSynonymTable(connection);
        processFeaturePropTable(connection);
    }

    private void processFeatureTable(Connection connection) throws SQLException, ObjectStoreException {
        Item dataSet = getDataSetItem(dataSetTitle);
        Item organismItem = getOrganismItem(taxonId);        
        ResultSet res = getFeatureResultSet(connection);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String name = res.getString("name");
            String uniqueName = res.getString("uniquename");
            String type = res.getString("type");
            String residues = res.getString("residues");
            int seqlen = 0;
            if (res.getObject("seqlen") != null) {
                seqlen = res.getInt("seqlen");
            }
            List<String> primaryIds = new ArrayList<String>();
            primaryIds.add(uniqueName);
            Item feature = makeFeature(featureId, name, uniqueName, type, residues, seqlen);
            if (feature != null) {
                FeatureData fdat = new FeatureData();
                fdat.itemIdentifier = feature.getIdentifier();
                fdat.uniqueName = uniqueName;
                fdat.type = type;
                feature.setReference("organism", organismItem);
                feature.addToCollection("evidence", dataSet);
                store(feature);
                features.put(featureId, fdat);
            }
        }
    }

    /**
     * Make and store a new feature 
     * @param featureId the chado feature id
     * @param name the name
     * @param uniqueName the uniquename
     * @param type the feature type from the cvterm table 
     * @param residues the residues (if any)
     * @param seqlen the sequence length (if known)
     * @throws ObjectStoreException if there is a problem while storing
     */
    protected Item makeFeature(Integer featureId, String name, String uniqueName, String type,
                               String residues, int seqlen) {
        
        // XXX FIMXE TODO HACK - this should be configured somehow
        if (uniqueName.startsWith("FBal")) {
            return null; 
        }
        
        String clsName = TypeUtil.javaiseClassName(type);
        Item feature = createItem(clsName);
        if (name != null) {
            feature.setAttribute("identifier", name);
        }
        feature.setAttribute("organismDbId", uniqueName);
   
/*
                createSynonym(feature, "identifier", accession, true, dataSet, dataSource);
            getItemWriter().store(ItemHelper.convert(accSynonym));
            feature.setAttribute("curated", "false");
            feature.setReference("organism", organismItem);
            feature.addToCollection("evidence", dataSet);
            Item cloneSynonym =
                createSynonym(feature, "identifier", cloneId, false, dataSet, dataSource);
            getItemWriter().store(ItemHelper.convert(cloneSynonym));
        }
       */
        return feature;
    }

    private void processRelationTable(Connection connection) {
        
    }

    private void processDbxrefTable(Connection connection) throws SQLException, ObjectStoreException {
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle);

        ResultSet res = getDbxrefResultSet(connection);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String accession = res.getString("accession");
            boolean isPrimary;
            if (features.containsKey(featureId)) {
                FeatureData fdat = features.get(featureId);
                Set<String> existingSynonyms = fdat.existingSynonyms;
                if (existingSynonyms.contains(accession)) {
                    return;
                } else {
                    if (fdat.uniqueName.equals(accession)) {
                        isPrimary = true;
                    } else {
                        isPrimary = false;
                    }
                    Item synonym = createSynonym(fdat.itemIdentifier, "identifier", accession,
                                                 isPrimary, dataSet, dataSource);
                    store(synonym);
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
            boolean isPrimary;
            if (features.containsKey(featureId) && typeName.equals("symbol")) {
                FeatureData fdat = features.get(featureId);
                Set<String> existingSynonyms = fdat.existingSynonyms;
                if (existingSynonyms.contains(identifier)) {
                    return;
                } else {
                    if (fdat.uniqueName.equals(identifier)) {
                        isPrimary = true;
                    } else {
                        isPrimary = false;
                    }
                    Item synonym = createSynonym(fdat.itemIdentifier, "identifier", identifier,
                                                 isPrimary, dataSet, dataSource);
                    store(synonym);
                }
            }
        }
    }

    private void processSynonymTable(Connection connection) throws SQLException {
            
    }

    /**
     * Return the interesting rows from the features table. 
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     */
    protected ResultSet getFeatureResultSet(Connection connection)
        throws SQLException {
        String query = "select feature_id, name, uniquename, type, residues, seqlen from f_type "
            + "where type in (" + featureTypesString + ") and organism_id = " + chadoOrganismId;
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
     * Return the interesting rows from the dbxref table. 
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     */
    protected ResultSet getDbxrefResultSet(Connection connection) throws SQLException {
        String query =
            "select feature.feature_id, accession from dbxref, feature_dbxref, feature "
            + " where feature_dbxref.dbxref_id = dbxref.dbxref_id "
            + "   and feature_dbxref.feature_id = feature.feature_id "
            + "   and feature.feature_id in"
            + "        (select feature_id from f_type" 
            + "           where type in (" + featureTypesString + ") "
            + "           and organism_id = " + chadoOrganismId + ")";
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the interesting rows from the featureprop table. 
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     */
    protected ResultSet getFeaturePropResultSet(Connection connection) throws SQLException {

        String query =
            "select feature_id, value, cvterm.name as type_name from featureprop, cvterm"
            + "   where featureprop.type_id = cvterm.cvterm_id"
            + "       and feature_id in (select feature_id from f_type"
            + "                          where type in (" + featureTypesString + ")"
            + "                              and organism_id = " + chadoOrganismId + ")";
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
}
