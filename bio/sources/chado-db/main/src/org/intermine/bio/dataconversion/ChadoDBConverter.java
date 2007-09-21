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

import java.util.HashMap;
import java.util.Map;

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
    private Map<Integer, String> features = new HashMap<Integer, String>();
    private String dataSourceName;
    private String dataSetTitle;
    private int taxonId = -1;
    private String genus;
    private String species;
    private String featureTypesString = "'gene', 'exon', 'transcript'";

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
        
        makeFeatureItems(connection);
    }

    private void makeFeatureItems(Connection connection) throws SQLException, ObjectStoreException {
        Item dataSet = getDataSetItem(dataSetTitle);
        Item dataSource = getDataSourceItem(dataSourceName);
        Item organismItem = getOrganismItem(taxonId);
        int chadoOrganismId = getChadoOrganismId(connection, genus, species);
        ResultSet res = getFeatureResultSet(connection, chadoOrganismId);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String name = res.getString("name");
            String uniqueName = res.getString("uniquename");
            String type = res.getString("type");
            String residues = res.getString("residues");
            int seqlen = res.getInt("seqlen");
            Item feature = makeFeature(featureId, name, uniqueName, type, residues, seqlen);
            feature.setReference("organism", organismItem);
            feature.addToCollection("evidence", dataSet);
            store(feature);
            features.put(featureId, feature.getIdentifier());
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

    /**
     * This is a protected method so that it can be overriden for testing
     */
    protected ResultSet getFeatureResultSet(Connection connection, int chadoOrganismId)
        throws SQLException {
        String query = "select feature_id, name, uniquename, type, residues, seqlen from f_type "
            + "where type in (" + featureTypesString + ")";
        Statement stmt = connection.createStatement();
        
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    protected int getChadoOrganismId(Connection connection, String genus, String species)
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
}
