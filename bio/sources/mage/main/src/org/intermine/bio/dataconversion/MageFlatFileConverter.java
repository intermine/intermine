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

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.xml.full.ReferenceList;

import org.apache.log4j.Logger;

/**
 * DataConverter to load flat file linking probeSet with their microArrayResults.
 * @author Wenyan Ji
 */

public class MageFlatFileConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(MageFlatFileConverter.class);
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final String PROBEPREFIX = "Affymetrix:CompositeSequence:Mouse430:";

    protected ItemFactory itemFactory;
    protected Map config = new HashMap();
    protected Item dataSource, dataSet, organismMM, experiment;
    protected String expName = "E-SMDB-3450";
    private String propertiesFileName = "mage_config.properties";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     * @throws IOException if fail to read config file
     */
    public MageFlatFileConverter(ItemWriter writer)
        throws ObjectStoreException, MetaDataException, IOException {
        super(writer);
    
        init(writer);
    }
    
    protected MageFlatFileConverter(ItemWriter writer, String propertiesFileName)
    throws ObjectStoreException, MetaDataException, IOException {
        super(writer);
        this.propertiesFileName = propertiesFileName;
        init(writer);
    }   
    
    private void init(ItemWriter writer) throws IOException, ObjectStoreException {
        readConfig();
        LOG.info("config " + config);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Proceedings of the National Academy of Sciences USA");
        dataSource.setAttribute("url", "http://www.pnas.org/");
        getItemWriter().store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        dataSet.setAttribute("title", "E-SMDB-3450");
        dataSet.setAttribute("description", 
                "Rossi et al, 2005: Compare blood stem cells from young vs old mice");     
        
        dataSet.setAttribute("url", 
        "http://www.pnas.org/content/vol0/issue2005/images/data/0503280102/DC1/03280Table4.xls");
        getItemWriter().store(ItemHelper.convert(dataSet));

        organismMM = createItem("Organism");
        organismMM.setAttribute("abbreviation", "MM");
        getItemWriter().store(ItemHelper.convert(organismMM));

        experiment = createItem("MicroArrayExperiment");
        experiment.setAttribute("identifier", expName);
        String experimentName = null;
        experimentName = getConfig(expName, "experimentName");
        if (experimentName != null) {
            experiment.setAttribute("name", experimentName);
        }
        String description = null; 
        description = getConfig(expName, "description");
        if (description != null) {
            experiment.setAttribute("description", description);
        }
        String pmid = getConfig(expName, "pmid");
        if (pmid != null && !pmid.equals("")) {
            Item pub = getPublication(pmid.trim()); 
            getItemWriter().store(ItemHelper.convert(pub));
            experiment.setReference("publication", pub.getIdentifier());
        }
        getItemWriter().store(ItemHelper.convert(experiment));

    }


    /**
     * Read each line from flat file.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {

        BufferedReader br = new BufferedReader(reader);
        //intentionally throw away first line
        String line = br.readLine();

        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t", -1); //keep trailing empty Strings

            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }

           
            String probeId = array[0].trim();
            String foldChange = array[1];

            Item probe = createProbe("ProbeSet", PROBEPREFIX, probeId,
                                     organismMM.getIdentifier(), dataSource.getIdentifier(), 
                                     dataSet.getIdentifier(), getItemWriter());
            Item result = createItem("MicroArrayResult");
            result.setAttribute("type", "Fold Change");
            result.setAttribute("scale", "linear");
            result.setAttribute("isControl", "false");
            result.setAttribute("value", foldChange);
            result.setReference("experiment", experiment.getIdentifier());
            getItemWriter().store(ItemHelper.convert(result));
            probe.addCollection(new ReferenceList("results", 
                                new ArrayList(Collections.singleton(result.getIdentifier()))));

            getItemWriter().store(ItemHelper.convert(probe));            
            
        }
    }


    /**
     * @param clsName = target class name
     * @param id = identifier
     * @param ordId = ref id for organism
     * @param datasourceId = ref id for datasource item
     * @param datasetId = ref id for dataset item
     * @param writer = itemWriter write item to objectstore
     * @return item
     * @throws exception if anything goes wrong when writing items to objectstore
     */
     private Item createProbe(String clsName, String probePre, String id, String orgId, 
                              String datasourceId, String datasetId, ItemWriter writer)
        throws Exception {
        Item probe = createItem(clsName);
        probe.setAttribute("identifier", probePre + id);
        probe.setAttribute("name", id);
        //probe.setAttribute("url", PROBEURL + id);
        probe.setReference("organism", orgId);
        probe.addCollection(new ReferenceList("evidence",
                            new ArrayList(Collections.singleton(datasetId))));

        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", PROBEPREFIX + id);
        synonym.setReference("source", datasourceId);
        synonym.setReference("subject", probe.getIdentifier());
        getItemWriter().store(ItemHelper.convert(synonym));

        return probe;
    }


    /**
     * Read in a properties file with additional information about experiments.  Key is
     * the MAGE:Experiment.name, values are for e.g. a longer name and primary characteristic
     * type of samples.
     * @throws IOException if file not found
     */
    protected void readConfig() throws IOException {
        // create a map from experiment name to a map of config values
        InputStream is =
            MageFlatFileConverter.class.getClassLoader().getResourceAsStream(propertiesFileName);

        if (is == null) {
            throw new IllegalArgumentException("Cannot find " + propertiesFileName
                                               + " in the class path");
        }

        Properties properties = new Properties();
        properties.load(is);

        Iterator iter = properties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String exptName = key.substring(0, key.indexOf("."));
            String propName = key.substring(key.indexOf(".") + 1);
            
            addToMap(config, exptName, propName, value);            
        }
    }

    /**
     * Add an entry to nester map of the form:
     * config = [group, [key, value]]
     * @param config the outer map
     * @param group key for outer map
     * @param key key to inner map
     * @param value value for inner map
     */
    protected void addToMap(Map config, String group, String key, String value) {
        Map exptConfig = (Map) config.get(group);
        if (exptConfig == null) {
            exptConfig = new HashMap();
            config.put(group, exptConfig);
        }
        exptConfig.put(key, value);
    }


    private String getConfig(String exptName, String propName) {
        String value = null;
        Map exptConfig = (Map) config.get(exptName);
        if (exptConfig != null) {
            value = (String) exptConfig.get(propName);
        } else {
            LOG.warn("No config details found for experiment: " + exptName);
        }
        return value;
    }

    /**
     * @param pmid pubmed id read from config
     * @return publication item
     */
    private Item getPublication(String pmid) {
        Item pub = createItem("Publication");
        pub.setAttribute("pubMedId", pmid);        
        return pub;
    }


    /**
     * @param clsName = target class name
     * @return item created by itemFactory
     */
    protected Item createItem(String clsName) {
        return itemFactory.makeItemForClass(GENOMIC_NS + clsName);
    }   
    
}
