package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;


import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.ItemWriter;

import org.apache.log4j.Logger;

/**
 * DataConverter to load flat file linking CompositeSequence to Ensembl genes.
 * @author Wenyan Ji
 */
public class AffyConverter extends CDNACloneConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected static final Logger LOG = Logger.getLogger(AffyConverter.class);

    protected Item dataSource;
    protected Item dataSet1, dataSet2, dataSet3;
    protected Item organism, organismMM;
    protected ItemFactory itemFactory;
    protected Map geneMap = new HashMap();
    private static final String PROBEPREFIX = "Affymetrix:CompositeSequence:";
    private static final String PROBEURL = "https://www.affymetrix.com/LinkServlet?probeset=";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public AffyConverter(ItemWriter writer)
        throws ObjectStoreException, MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Affymetrix GeneChip");
        writer.store(ItemHelper.convert(dataSource));

        dataSet1 = createItem("DataSet");
        dataSet1.setAttribute("title", "Affymetrix Human Genome U133A Array");
        writer.store(ItemHelper.convert(dataSet1));

        dataSet2 = createItem("DataSet");
        dataSet2.setAttribute("title", "Affymetrix Human Genome U95Av2 Array");
        writer.store(ItemHelper.convert(dataSet2));
        
        dataSet3 = createItem("DataSet");
        dataSet3.setAttribute("title", "Affymetrix Mouse Genome 430 2.0 Array");
        writer.store(ItemHelper.convert(dataSet3));

        organism = createItem("Organism");
        organism.setAttribute("abbreviation", "HS");
        writer.store(ItemHelper.convert(organism));

        organismMM = createItem("Organism");
        organismMM.setAttribute("abbreviation", "MM");
        writer.store(ItemHelper.convert(organismMM));

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
            String[] array = line.split("\",\"", -1); //keep trailing empty Strings

            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }

           
            String probeId = array[0].substring(1);
            String probeIdentifier = null;
            String dataSetId = null;
            String orgId = null;

            String chipInfo = array[1];
            if (chipInfo.equals("Human Genome U133A Array")) {
                probeIdentifier = probeId.concat("HG-U133A:");
                dataSetId = dataSet1.getIdentifier();
                orgId = organism.getIdentifier();
            } else if (chipInfo.equals("Human Genome U95Av2 Array")) {
                probeIdentifier = probeId.concat("HG-U95Av2:");
                dataSetId = dataSet2.getIdentifier();
                orgId = organism.getIdentifier();
            } else if (chipInfo.equals("Mouse Genome 430 2.0 Array")) {
                probeIdentifier = probeId.concat("Mouse430:");
                dataSetId = dataSet3.getIdentifier();
                orgId = organismMM.getIdentifier();

            }
                        
            String geneEnsembl = array[17];
            //don't create probe if no ensembl id is given in the file
            if (geneEnsembl.startsWith("ENSG") || geneEnsembl.startsWith("ENSMUSG")) {               
                Item probe = createProbe("CompositeSequence", probeIdentifier, 
                             orgId, dataSource.getIdentifier(), dataSetId, writer);
                StringTokenizer st = new StringTokenizer(geneEnsembl, "///");
                ReferenceList rf = new ReferenceList("genes");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    Item gene = createGene("Gene", orgId, token.trim(), writer);
                    rf.addRefId(gene.getIdentifier());
                }
                probe.addCollection(rf);
                writer.store(ItemHelper.convert(probe));
            }
        }

    }

    /**
     * @param clsName = target class name
     * @param ordId = ref id for organism
     * @param geneEnsembl = ensembl identifier used for gene organismDbId
     * @param writer = itemWriter write item to objectstore
     * @return item
     * @throws exception if anything goes wrong when writing items to objectstore
     */
    private Item createGene(String clsName, String orgId, String geneEnsembl, ItemWriter writer)
        throws Exception {
        Item gene = (Item) geneMap.get(geneEnsembl);
        if (gene == null) {
            gene = createItem(clsName);
            gene.setReference("organism", orgId);
            gene.setAttribute("organismDbId", geneEnsembl);
            geneMap.put(geneEnsembl, gene);
            writer.store(ItemHelper.convert(gene));
        }
        return gene;
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
     private Item createProbe(String clsName, String id, String orgId, 
                              String datasourceId, String datasetId, ItemWriter writer)
        throws Exception {
        Item probe = createItem(clsName);
        probe.setAttribute("identifier", PROBEPREFIX + id);
        probe.setAttribute("name", id);
        probe.setAttribute("url", PROBEURL + id);
        probe.setReference("organism", orgId);
        //      probe.addCollection(new ReferenceList("genes", geneId));
        probe.addCollection(new ReferenceList("evidence",
                            new ArrayList(Collections.singleton(datasetId))));
        //writer.store(ItemHelper.convert(probe));

        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", PROBEPREFIX + id);
        synonym.setReference("source", datasourceId);
        synonym.setReference("subject", probe.getIdentifier());
        writer.store(ItemHelper.convert(synonym));

        return probe;
    }
}
