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

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.xml.full.Item;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * DataConverter to read from a Chado database into items
 * @author Kim Rutherford
 */
public class ChadoDBConverter extends BioDBConverter
{
    protected static final Logger LOG = Logger.getLogger(ChadoDBConverter.class);

    private String dataSourceName;
    private String dataSetTitle;
    private int taxonId = -1;
    private String genus;
    private String species;
    private int chadoOrganismId;
    private String processors = "";

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
     * Return the data source name set by setDataSourceName().
     * @return the data source name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Set the taxonId to use when creating the Organism Item for the new features.
     * @param taxonId the taxon id
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = Integer.valueOf(taxonId).intValue();
    }

    /**
     * Set the class names of the ChadoSequenceModuleProcessor to run.
     * @param processors a space separated list of the fully-qualified class names of module
     * processors to run
     */
    public void setProcessors(String processors) {
        this.processors = processors;
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
            // no Database when testing and no connection needed
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
        /*
        if (getTaxonIdInt() == -1) {
            throw new IllegalArgumentException("taxonId not set in ChadoDBConverter");
        }
        if (species == null) {
            throw new IllegalArgumentException("species not set in ChadoDBConverter");
        }
        if (genus == null) {
            throw new IllegalArgumentException("genus not set in ChadoDBConverter");
        }
        if (StringUtils.isEmpty(processors)) {
            throw new IllegalArgumentException("processors not set in ChadoDBConverter");
        }
        */
        if (getTaxonIdInt() != -1 && species != null && genus != null) {
            // the organism isn't used by all processors
            chadoOrganismId = getChadoOrganismId(connection);
        }

        String[] bits = processors.trim().split("[ \\t]+");
        for (int i = 0; i < bits.length; i++) {
            String className = bits[i];
            if (!StringUtils.isEmpty(className)) {
                Class<?> cls = Class.forName(className);
                Constructor constructor = cls.getDeclaredConstructor(ChadoDBConverter.class);
                ChadoModuleProcessor processor =
                    (ChadoModuleProcessor) constructor.newInstance(this);
                processor.process(connection);
            }
        }
    }

    /**
     * Return the chado db id (organism_id) for the organism given by the genus and species.
     * @return the organism_id
     */
    public int getChadoOrganismId() {
        return chadoOrganismId;
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
     * Return the DataSet Item created from the dataSetTitle.
     * @return the DataSet Item
     */
    public Item getDataSetItem() {
        return getDataSetItem(dataSetTitle);
    }

    /**
     * Return the DataSource Item created from the dataSourceName.
     * @return the DataSource Item
     */
    public Item getDataSourceItem() {
        return getDataSourceItem(dataSourceName);
    }
}
