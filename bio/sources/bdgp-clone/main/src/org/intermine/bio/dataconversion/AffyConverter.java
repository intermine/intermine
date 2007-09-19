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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to load flat file linking BDGP clones to Flybase genes.
 * @author Wenyan Ji
 */
public class AffyConverter extends CDNACloneConverter
{
    protected static final Logger LOG = Logger.getLogger(AffyConverter.class);

    protected Item dataSource;
    protected Item dataSet;
    protected Item organism;
    protected Map geneMap = new HashMap();
    protected Map probeMap = new HashMap();
    private static final String PROBEPREFIX = "Affymetrix:CompositeSequence:HG-U133A:";
    private static final String PROBEURL = "https://www.affymetrix.com/LinkServlet?probeset=";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public AffyConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Affymetrix GeneChip");
        store(dataSource);

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "Affymetrix HG-U133A annotation data set");
        store(dataSet);

        organism = createItem("Organism");
        organism.setAttribute("abbreviation", "HS");
        store(organism);
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
            //String geneSymbol= array[14];//this is hugo identifier
            String geneEnsembl = array[17];
            //don't create probe if no ensembl id is given in the file
            if (geneEnsembl.startsWith("ENSG")) {               
                Item probe = createProbe("CompositeSequence", probeId.trim(), 
                                 organism.getIdentifier(), dataSource.getIdentifier(), 
                                 dataSet.getIdentifier(), getItemWriter());
                StringTokenizer st = new StringTokenizer(geneEnsembl, "///");
                ReferenceList rf = new ReferenceList("genes");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    Item gene = createGene("Gene", organism.getIdentifier(), token.trim(),
                                           getItemWriter());
                    rf.addRefId(gene.getIdentifier());
                }
                probe.addCollection(rf);
                store(probe);
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
            store(gene);
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
        store(synonym);

        return probe;
    }
}
