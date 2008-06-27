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

import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 *
 * @author Julie Sullivan
 */
public class Drosophila2ProbeConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(Drosophila2ProbeConverter.class);

    protected Item dataSource, dataSet, org;
    protected Map<String, Item> bioMap = new HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private static final String TAXON_ID = "7227";

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
        lineIter.next();    // skip header
        boolean hasDataset = false;

        while (lineIter.hasNext()) {

            String[] line = lineIter.next();

            if (!hasDataset)  {
                dataSet = createItem("DataSet");
                dataSet.setReference("dataSource", dataSource.getIdentifier());
                dataSet.setAttribute("title", "Affymetrix array: " + line[1]);
                store(dataSet);
                hasDataset = true;
            }

            List<Item> delayedItems = new ArrayList<Item>();
            Item probeSet = createProbeSet(line[0], delayedItems);

            String seqType = line[4];

            if (seqType.equalsIgnoreCase("control sequence")) {
                // TODO add a description and flag
                // probeSet.setAttribute("description", line[4]);
                probeSet.setAttribute("isControl", "true");
            }

            String fbgns = line[7];
            if (!fbgns.equals("---")) {
                String transcriptIdentifier = line[6].trim();
                if (transcriptIdentifier.startsWith("CG")
                    || transcriptIdentifier.startsWith("a_merged")
                    || transcriptIdentifier.startsWith("a_predict")) {

                    if (transcriptIdentifier.startsWith("CG")) {
                        Item transcript = createBioEntity("Transcript", transcriptIdentifier);
                        probeSet.setReference("transcript", transcript.getIdentifier());
                    }


                    String[] genes = fbgns.split(" ");
                    ReferenceList geneColl = new ReferenceList("genes", new ArrayList<String>());
                    for (String identifier : genes) {
                        if (identifier.trim().startsWith("FB:")) {
                            identifier = identifier.substring(3);
                        }
                        if (identifier.trim().startsWith("FBgn")) {
                            Item gene = createBioEntity("Gene", identifier);
                            if (gene != null) {
                                geneColl.addRefId(gene.getIdentifier());
                            }
                        }
                    }
                    probeSet.addCollection(geneColl);
                }
            }
            store(probeSet);
            for (Item item : delayedItems) {
                store(item);
            }

        }
    }

    private Item createBioEntity(String clsName, String id)
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
        Item bio = bioMap.get(identifier);
        if (bio == null) {
            bio = createItem(clsName);
            bio.setReference("organism", org.getIdentifier());

            if (clsName.equals("Gene")) {
                bio.setAttribute("primaryIdentifier", identifier);
            } else {
                bio.setAttribute("secondaryIdentifier", identifier);
            }
            bioMap.put(identifier, bio);
            store(bio);
        }
        return bio;
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
    private Item createProbeSet(String probeSetId, List<Item> delayedItems) {
        Item probeSet = createItem("ProbeSet");
        probeSet.setAttribute("primaryIdentifier", probeSetId);
        probeSet.setAttribute("name", probeSetId);
        probeSet.setReference("organism", org.getIdentifier());
        probeSet.setCollection("dataSets",
            new ArrayList(Collections.singleton(dataSet.getIdentifier())));

        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", probeSetId);
        synonym.setReference("source", dataSource.getIdentifier());
        synonym.setReference("subject", probeSet.getIdentifier());
        delayedItems.add(synonym);

        return probeSet;
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
