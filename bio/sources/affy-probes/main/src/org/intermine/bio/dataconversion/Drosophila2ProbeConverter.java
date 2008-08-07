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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 *
 * @author Julie Sullivan
 */
public class Drosophila2ProbeConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(Drosophila2ProbeConverter.class);

    protected Item dataSource, dataSet, org;
    protected Map<String, Item> bioentities = new HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private static final String TAXON_ID = "7227";
    private Map<String, Item> synonyms = new HashMap<String, Item>();


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the data model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public Drosophila2ProbeConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Affymetrix");
        store(dataSource);

        org = createItem("Organism");
        org.setAttribute("taxonId", TAXON_ID);
        store(org);

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();
    }


    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        Iterator<String[]> lineIter = FormattedTextParser.parseCsvDelimitedReader(reader);

        // process header
        String[] line = lineIter.next();
        //Affy drosophila 2
        //Affy drosgenome1

        dataSet = createItem("DataSet");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        dataSet.setAttribute("title", "Affymetrix array: " + line[2]);
        store(dataSet);
        Map<String, Item> probes = new HashMap<String, Item>();
        List<Item> delayedItems = new ArrayList<Item>();

        while (lineIter.hasNext()) {
            line = lineIter.next();

            String fbgn = line[0];
//            String transcriptIdentifier = line[1];
            String probesetIdentifier = line[2];

            Item probeSet = createProbeSet(probesetIdentifier, probes, delayedItems);
//            Item transcript = createBioEntity("Transcript", transcriptIdentifier, delayedItems);
//            probeSet.setReference("transcript", transcript.getIdentifier());

            Item gene = createBioEntity("Gene", fbgn, delayedItems);
            if (gene != null) {
                probeSet.addToCollection("genes", gene);
            }
        }
        for (Item item : probes.values()) {
            store(item);
        }
        for (Item item : delayedItems) {
            store(item);
        }
    }

    private Item createBioEntity(String clsName, String id, List<Item> delayedItems)
    throws ObjectStoreException {
        String identifier = id;
        if (clsName.equals("Gene")) {
            IdResolver resolver = resolverFactory.getIdResolver();
            int resCount = resolver.countResolutions(TAXON_ID, identifier);
             if (resCount != 1) {
                 LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                          + identifier + " count: " + resCount + " FBgn: "
                          + resolver.resolveId(TAXON_ID, identifier));
                 return null;
             }
             identifier = resolver.resolveId(TAXON_ID, identifier).iterator().next();
        }
        Item bioentity = bioentities.get(identifier);
        if (bioentity == null) {
            bioentity = createItem(clsName);
            bioentity.setReference("organism", org.getIdentifier());

            if (clsName.equals("Gene")) {
                bioentity.setAttribute("primaryIdentifier", identifier);
            } else {
                bioentity.setAttribute("secondaryIdentifier", identifier);
            }
            bioentity.setCollection("dataSets",
                               new ArrayList(Collections.singleton(dataSet.getIdentifier())));
            bioentities.put(identifier, bioentity);
            store(bioentity);
            createSynonym(bioentity.getIdentifier(), "identifier", identifier, delayedItems);
        }
        return bioentity;
    }

    /**
     * @param clsName target class name
     * @param id identifier
     * @param ordId ref id for organism
     * @param datasourceId ref id for datasource item
     * @param datasetId ref id for dataset item
     * @param writer itemWriter write item to objectstore
     * @return item
     * @throws exception if anything goes wrong when writing items to objectstore
     */
    private Item createProbeSet(String probeSetId, Map<String, Item> probes,
                                List<Item> delayedItems) {
        if (probes.get(probeSetId) != null) {
            return probes.get(probeSetId);
        }

        Item probeSet = createItem("ProbeSet");
        probeSet.setAttribute("primaryIdentifier", probeSetId);
        probeSet.setAttribute("name", probeSetId);
        probeSet.setReference("organism", org.getIdentifier());
        probeSet.setCollection("dataSets",
            new ArrayList(Collections.singleton(dataSet.getIdentifier())));
        createSynonym(probeSet.getIdentifier(), "identifier", probeSetId, delayedItems);
        probes.put(probeSetId, probeSet);
        return probeSet;
    }

    private Item createSynonym(String subjectId, String type, String value,
                               List<Item> delayedItems) {
        String key = subjectId + type + value;
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (!synonyms.containsKey(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            syn.setReference("source", dataSource.getIdentifier());
            synonyms.put(key, syn);
            delayedItems.add(syn);
            return syn;
        }
        return null;
    }



    // not used

//    private Item createChromosome(String chrId) throws ObjectStoreException {
//        Item chr = (Item) chrMap.get(chrId);
//        if (chr == null) {
//            chr = createItem("Chromosome");
//            String primaryIdentifier = null;
//            // convert 'arm_2L' -> '2L'
//            if (chrId.contains("_")) {
//                String[] s = chrId.split("_");
//                primaryIdentifier = s[1];
//            } else {
//                primaryIdentifier = chrId;
//            }
//            chr.setAttribute("primaryIdentifier", primaryIdentifier);
//            chr.setReference("organism", org.getIdentifier());
//            chrMap.put(chrId, chr);
//            store(chr);
//        }
//        return chr;
//    }
}
/**
-PROBE => $probe,
-MISMATCHCOUNT => 0,
-SLICE => $chr_1_slice,
-START => 1_000_000,
-END => 1_000_024,
-STRAND => -1,
**/

