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

public class MageTimeCourseMasFileConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(MageTimeCourseMasFileConverter.class);
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final String PROBEPREFIX = "Affymetrix:CompositeSequence:MG_U74Av2:";
    private static final String PROBEURL = "https://www.affymetrix.com/LinkServlet?probeset=";

    protected ItemFactory itemFactory;
    protected Map config = new HashMap();
    protected Item dataSource, dataSet, organismMM, experiment, sample1;
    protected String expName = "FDCP";
    protected Map assayMap = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     * @throws IOException if fail to read config file
     */
    public MageTimeCourseMasFileConverter(ItemWriter writer)
        throws ObjectStoreException, MetaDataException, IOException {
        super(writer);

        readConfig();
        LOG.info("config " + config);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "The Weatherall Institute of Molecular Medicine, "
                                + "Oxford University");
        dataSource.setAttribute("url", "http://www.imm.ox.ac.uk/");
        writer.store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        dataSet.setAttribute("title", "FDCP");
        dataSet.setAttribute("description",
                             "Molecular Signatures of Self-Renewal, Differentiation, "
                             + "and Lineage Choice in Multipotential Hemopoietic Progenitor "
                             + "Cells In Vitro");

        dataSet.setAttribute("url",
                        "http://www.imm.ox.ac.uk/pages/research/molecular_haematology/tariq.htm");
        writer.store(ItemHelper.convert(dataSet));

        organismMM = createItem("Organism");
        organismMM.setAttribute("abbreviation", "MM");
        writer.store(ItemHelper.convert(organismMM));

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
            writer.store(ItemHelper.convert(pub));
            experiment.setReference("publication", pub.getIdentifier());
        }
        writer.store(ItemHelper.convert(experiment));

        Item sample1 = createItem("Sample");
        sample1.setReference("organism", organismMM.getIdentifier());
        String materialType = null;
        materialType = getConfig(expName, "materialIdType");
        if (materialType != null) {
            sample1.setAttribute("materialType",  getConfig(expName, "materialIdType"));
        }
        String characteric = null;
        characteric = getConfig(expName, "primaryCharacteristic");
        if (characteric != null) {
            sample1.setAttribute("primaryCharacteristicType", "TargetedCellType");
            sample1.setAttribute("primaryCharacteristic",
                                 getConfig(expName, "primaryCharacteristic"));
        }
        writer.store(ItemHelper.convert(sample1));
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

            double aveAPCall = 0.0;
            String probeId = array[0].trim();
            String apcall = array[1];
            String replicates = array[2];
            String type = array[3];
            String timePoint = array[5];
            String timeUnit = array[6];
            aveAPCall = Integer.parseInt(apcall) / Integer.parseInt(replicates);

            Item probe = createProbe("CompositeSequence", PROBEPREFIX, probeId,
                                     organismMM.getIdentifier(), dataSource.getIdentifier(),
                                     dataSet.getIdentifier(), writer);

            String name = timePoint.concat(" ").concat(timeUnit);
            Item assay = getAssay(name, writer);

            Item result = createItem("MicroArrayResult");
            result.setAttribute("type", type);
            result.setAttribute("scale", "n/a");
            result.setAttribute("isControl", "false");
            if (aveAPCall > 0.5) {
                result.setAttribute("flag", "present");
            } else {
                result.setAttribute("flag", "absent");
            }
            result.setAttribute("value", apcall.concat("/").concat(replicates));
            result.setReference("experiment", experiment.getIdentifier());
            result.addCollection(new ReferenceList("assays",
                                 new ArrayList(Collections.singleton(assay.getIdentifier()))));
            writer.store(ItemHelper.convert(result));

            probe.addCollection(new ReferenceList("results",
                                new ArrayList(Collections.singleton(result.getIdentifier()))));

            writer.store(ItemHelper.convert(probe));

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
        probe.setAttribute("url", PROBEURL + id);
        probe.setReference("organism", orgId);
        probe.addCollection(new ReferenceList("evidence",
                            new ArrayList(Collections.singleton(datasetId))));

        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", PROBEPREFIX + id);
        synonym.setReference("source", datasourceId);
        synonym.setReference("subject", probe.getIdentifier());
        writer.store(ItemHelper.convert(synonym));

        return probe;
    }

    private Item getAssay(String name, ItemWriter writer) throws Exception {
        Item assay = new Item();
        if (assayMap.containsKey(name)) {
            assay = (Item) assayMap.get(name);
        } else {
            assay = createItem("MicroArrayAssay");
            assay.setAttribute("sample1", getConfig(expName, "sample1"));
            assay.setAttribute("sample2", getConfig(expName, "sample2"));
            assay.setAttribute("name", name);
            assay.setReference("experiment", experiment.getIdentifier());
            if (sample1 != null) {
                assay.addCollection(new ReferenceList("samples",
                                new ArrayList(Collections.singleton(sample1.getIdentifier()))));
            }
            assayMap.put(name, assay);
            writer.store(ItemHelper.convert(assay));
        }
        return assay;
    }

    /**
     * Read in a properties file with additional information about experiments.  Key is
     * the MAGE:Experiment.name, values are for e.g. a longer name and primary characteristic
     * type of samples.
     * @throws IOException if file not found
     */
    protected void readConfig() throws IOException {
        // create a map from experiment name to a map of config values
        String propertiesFileName = "mage_config.properties";
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
