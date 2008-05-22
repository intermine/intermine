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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.util.StringUtil;

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

    // a Map from chado organism_id to taxonId
    private final Map<Integer, OrganismData> chadoToOrgData = new HashMap<Integer, OrganismData>();
    private String processors = "";

    private final Set<OrganismData> organismsToProcess = new HashSet<OrganismData>();

    private final OrganismRepository organismRepository;

    /**
     * Create a new ChadoDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public ChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
        organismRepository = OrganismRepository.getOrganismRepository();
    }

    /**
     * Set the taxon ids to use when creating the Organism Item for the new features.  Only features
     * from chado with these organisms will be processed.
     * @param organisms a space separated list of the organism abbreviations or taxon ids to look
     * up in the organism table eg. "Dmel Dpse"
     */
    public void setOrganisms(String organisms) {
        String[] bits = StringUtil.split(organisms, " ");
        //for (int i = 0; i < bits.length; i++) {
        for (String organismIdString: bits) {
            OrganismData od = null;
            try {
                Integer taxonId = Integer.valueOf(organismIdString);
                od = organismRepository.getOrganismDataByTaxon(taxonId);
            } catch (NumberFormatException e) {
                od = organismRepository.getOrganismDataByAbbreviation(organismIdString);
            }
            if (od == null) {
                throw new RuntimeException("can't find organism for: " + organismIdString);
            } else {
                organismsToProcess.add(od);
            }
        }
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
     * Return a map from chado organism_id to OrganismData object for all the organisms that we
     * are processing
     * @return the Map
     */
    public Map<Integer, OrganismData> getChadoIdToOrgDataMap() {
        return chadoToOrgData;
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

        if (StringUtils.isEmpty(processors)) {
            throw new IllegalArgumentException("processors not set in ChadoDBConverter");
        }

        List<String> orgAbbrevationsToProcess = new ArrayList<String>();
        for (OrganismData od: organismsToProcess) {
            orgAbbrevationsToProcess.add(od.getAbbreviation());
        }

        if (orgAbbrevationsToProcess.size() > 0) {
            Map<String, Integer> abbrevChadoIdMap =
                getChadoOrganismIds(connection, orgAbbrevationsToProcess);

            for (Map.Entry<String, Integer> entry: abbrevChadoIdMap.entrySet()) {
                String abbreviation = entry.getKey();
                Integer chadoOrganismId = entry.getValue();

                OrganismData orgData =
                    organismRepository.getOrganismDataByAbbreviation(abbreviation);
                chadoToOrgData.put(chadoOrganismId, orgData);
            }
        }

        String[] bits = processors.trim().split("[ \\t]+");
        for (int i = 0; i < bits.length; i++) {
            String className = bits[i];
            if (!StringUtils.isEmpty(className)) {
                Class<?> cls = Class.forName(className);
                Constructor<?> constructor = cls.getDeclaredConstructor(ChadoDBConverter.class);
                ChadoProcessor currentProcessor = (ChadoProcessor) constructor.newInstance(this);
                currentProcessor.process(connection);
            }
        }
    }

    /**
     * Return the chado organism id for the given organism abbreviations.  This is a protected
     * method so that it can be overriden for testing
     * @param connection the db connection
     * @param abbreviations a space separated list of the organism abbreviations to look up in the
     * organism table eg. "Dmel Dpse"
     * @return a Map from abbreviation to chado organism_id
     * @throws SQLException if the is a database problem
     */
    protected Map<String, Integer> getChadoOrganismIds(Connection connection,
                                                       List<String> abbreviations)
        throws SQLException {
        StringBuffer abbrevBuffer = new StringBuffer();
        for (int i = 0; i < abbreviations.size(); i++) {
            if (i != 0) {
                abbrevBuffer.append(", ");
            }
            abbrevBuffer.append("'").append(abbreviations.get(i)).append("'");
        }
        String query = "select organism_id, abbreviation from organism where abbreviation IN ("
            + abbrevBuffer.toString() + ")";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);

        Map<String, Integer> retMap = new HashMap<String, Integer>();

        while (res.next()) {
            int organismId = res.getInt("organism_id");
            String abbreviation = res.getString("abbreviation");
            retMap.put(abbreviation, new Integer(organismId));
        }

        return retMap;
    }

    /**
     * Return the OrganismData objects for the organisms listed in the source configuration.
     * @return the organismsToProcess
     */
    public Set<OrganismData> getOrganismsToProcess() {
        return organismsToProcess;
    }

    /**
     * Default implementation that makes a data set title based on the data source name.
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        OrganismData od = organismRepository.getOrganismDataByTaxon(taxonId);
        if (od != null) {
            return getDataSourceName() + " data set for " + od.getGenus() + " " + od.getSpecies();
        } else {
            return getDataSourceName() + " data set";
        }
    }
}
