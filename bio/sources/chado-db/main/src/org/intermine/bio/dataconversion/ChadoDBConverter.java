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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.util.StringUtil;

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

    private final List<ChadoProcessor> completedProcessors = new ArrayList<ChadoProcessor>();

    private Connection connection;


    /**
     * Create a new ChadoDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     * @throws SQLException if we fail to get a database connection

     */
    public ChadoDBConverter(Database database, Model tgtModel, ItemWriter writer)
        throws SQLException {
        super(database, tgtModel, writer, null, null);
        organismRepository = OrganismRepository.getOrganismRepository();
        if (getDatabase() == null) {
            // no Database when testing and no connection needed
            connection = null;
        } else {
            connection = getDatabase().getConnection();
        }
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
            }
            organismsToProcess.add(od);
        }
    }

    /**
     * Set the class names of the ChadoProcessors to run.
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
     * Get the connection to use when processing.
     * @return the Connection, or null while testing
     */
    protected Connection getConnection() {
        return connection;
    }

    /**
     * Process the data from the Database and write to the ItemWriter.
     * {@inheritDoc}
     */
    @Override
    public void process() throws Exception {

        if (StringUtils.isEmpty(processors)) {
            throw new IllegalArgumentException("processors not set in ChadoDBConverter");
        }

        Map<OrganismData, Integer> tempChadoOrgMap = getChadoOrganismIds(getConnection());

        for (OrganismData od: organismsToProcess) {
            Integer chadoId = tempChadoOrgMap.get(od);
            if (chadoId == null) {
                throw new RuntimeException("Organism " + od
                                           + " not found in the chado organism table");
            }
            chadoToOrgData.put(chadoId, od);
        }

        if (chadoToOrgData.size() == 0) {
            throw new RuntimeException("can't find any known organisms in the organism table");
        }

        String[] bits = processors.trim().split("[ \\t]+");
        for (int i = 0; i < bits.length; i++) {
            String className = bits[i];
            if (!StringUtils.isEmpty(className)) {
                Class<?> cls = Class.forName(className);
                Constructor<?> constructor = cls.getDeclaredConstructor(ChadoDBConverter.class);
                ChadoProcessor currentProcessor = (ChadoProcessor) constructor.newInstance(this);
                currentProcessor.process(getConnection());
                getCompletedProcessors().add(currentProcessor);
            }
        }
    }

    /**
     * Return a map from chado organism id to OrganismData for the organisms in the organism table
     * in chado.  This is a protected method so that it can be overriden for testing
     * @param conn the db connection
     * @param organismsToProcess2
     * @return a Map from abbreviation to chado organism_id
     * @throws SQLException if the is a database problem
     */
    protected Map<OrganismData, Integer> getChadoOrganismIds(Connection conn)
        throws SQLException {
        String query = "select organism_id, abbreviation, genus, species from organism";
        LOG.info("executing: " + query);
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);

        Map<OrganismData, Integer> retMap = new HashMap<OrganismData, Integer>();

        OrganismRepository or = OrganismRepository.getOrganismRepository();

        while (res.next()) {
            int organismId = res.getInt("organism_id");
            String abbreviation = res.getString("abbreviation");
            String genus = res.getString("genus");
            String species = res.getString("species");

            OrganismData od = null;

            if (genus != null && species != null) {
                od = or.getOrganismDataByGenusSpecies(genus, species);
            }

            if (od == null) {
                if (abbreviation != null) {
                    od = or.getOrganismDataByAbbreviation(abbreviation);
                }
            }

            if (od == null) {
                LOG.warn("can't find OrganismData for species: " + species
                         + " genus: " + genus + " abbreviation: " + abbreviation);
            }

            retMap.put(od, new Integer(organismId));
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
     * Look at the list of completed processors and return the processor of the given type.  If
     * there is none or more than one, throw a RuntimeException
     * @param cls the class
     * @return the ChadoProcessor
     */
    public ChadoProcessor findProcessor(Class<? extends ChadoProcessor> cls) {
        ChadoProcessor returnProcessor = null;

        for (ChadoProcessor processor: getCompletedProcessors()) {
            if (cls.isAssignableFrom(processor.getClass())) {
                if (returnProcessor == null) {
                    returnProcessor = processor;
                } else {
                    throw new RuntimeException("Completed processors list contains two objects of "
                                               + "type: " + cls.getName());
                }
            }
        }

        if (returnProcessor == null) {
            throw new RuntimeException("Can't find `" + cls.getName() + "` before `"
                                       + this.getClass().getName()
                                       + "` in the list of completed processors - must run "
                                       + cls.getName() + " first.");
        }
        return returnProcessor;
    }
    /**
     * Default implementation that makes a data set title based on the data source name.
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        OrganismData od = organismRepository.getOrganismDataByTaxon(new Integer(taxonId));
        if (od != null) {
            return getDataSourceName() + " data set for " + od.getGenus() + " " + od.getSpecies();
        }
        return getDataSourceName() + " data set";
    }

    /**
     * @return the completedProcessors
     */
    public List<ChadoProcessor> getCompletedProcessors() {
        return completedProcessors;
    }
}
