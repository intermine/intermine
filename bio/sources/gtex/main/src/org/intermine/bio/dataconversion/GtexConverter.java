package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Julie Sullivan
 */
public class GtexConverter extends BioDirectoryConverter
{
    //
    private static final String DATASET_TITLE = "GTex data set";
    private static final String DATA_SOURCE_NAME = "GTex";
    private Map<String, Item> genes = new HashMap<String, Item>();
    private static final String TAXON_ID = "9606";
    protected IdResolver rslv;
    private static final Logger LOG = Logger.getLogger(GtexConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public GtexConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    @Override
    public void process(File dataDir) throws Exception {

        if (rslv == null) {
            rslv = IdResolverService.getIdResolverByOrganism(TAXON_ID);
        }

        List<File> files = readFilesInDir(dataDir);
        for (File f : files) {
            String fileName = f.getName();
            if (fileName.contains("gene_median_rpkm")) {
                processExpression(new FileReader(f));
            } else if (fileName.contains("signif_snpgene")) {
                processSNPs(new FileReader(f), fileName);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        for (Item gene : genes.values()) {
            try {
                store(gene);
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<File> readFilesInDir(File dir) {
        List<File> files = new ArrayList<File>();
        for (File file : dir.listFiles()) {
            files.add(file);
        }
        return files;
    }

//    private void processGeneFile(Reader reader) throws IOException, ObjectStoreException {
//        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
//        lineIter.next(); // move past header
//        while (lineIter.hasNext()) {
//            String[] line = (String[]) lineIter.next();
//            if (line.length < 28) {
//                continue;
//            }
//            String geneIdentifier = line[0];
//            Item gene = getGene(geneIdentifier);
//        }
//    }

    private void processSNPs(Reader reader, String filename)
        throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        lineIter.next(); // move past header
        String tissue = parseFilename(filename);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (line.length != 11) {
                continue;
            }
            String snpIdentifier = line[0];
            String geneIdentifier = line[1];
            String tssDistance = line[2];
            String pValue = line[3];

            Item gene = getGene(geneIdentifier);
            if (gene == null) {
                continue;
            }
            String snp = getSNP(snpIdentifier, gene, tissue, tssDistance, pValue);
            gene.addToCollection("SNPs", snp);
        }
    }

    // Nerve_Tibial_Analysis.v6p.egenes.txt
    private String parseFilename(String filename) {
        String[] bits = filename.split("\\.");
        String tissue = bits[0];
        return tissue.replace("_", " ");
    }

    private void processExpression(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        String[] headers = null;

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // process header
            if ("Name".equals(line[0])) {
                headers = line;
            }
            // keep going until we find the column heading
            if (headers == null) {
                continue;
            }
            String geneIdentifier = line[0];
            Item gene = getGene(geneIdentifier);
            if (gene == null) {
                continue;
            }

            // skip first two columns, name and description
            for (int i = 2; i < line.length; i++) {
                String columnName = headers[i];
                Item item = createItem("RNASeqResult");
                item.setReference("gene", gene);
                item.setAttribute("tissue", columnName);
                item.setAttribute("expressionScore", line[i]);
                store(item);
                gene.addToCollection("rnaSeqResults", item);
            }
        }
    }

    private Item getGene(String ensemblIdentifier) throws ObjectStoreException {

        String primaryIdentifier = resolveGene(ensemblIdentifier);
        // could not resolve
        if (primaryIdentifier == null) {
            return null;
        }

        Item item = genes.get(primaryIdentifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setReference("organism", getOrganism(TAXON_ID));
            genes.put(primaryIdentifier, item);
        }
        return item;
    }

    private String getSNP(String primaryIdentifier, Item gene, String tissue,
        String tssDistance, String pValue)
        throws ObjectStoreException {
        Item item = createItem("SNP");
        item.setAttribute("primaryIdentifier", primaryIdentifier);
        item.setAttribute("tissue", tissue);
        item.setAttribute("tssDistance", tssDistance);
        item.setAttribute("pValue", pValue);
        item.setReference("organism", getOrganism(TAXON_ID));
        item.setReference("gene", gene);
        store(item);
        return item.getIdentifier();
    }

    /**
     * resolve old human symbol
     * @param taxonId id of organism for this gene
     * @param ih interactor holder
     * @throws ObjectStoreException
     */
    private String resolveGene(String identifier) {
        String id = identifier;
        // ENSG00000225880.4
        String[] bits = id.split("\\.");
        String ensemblIdentifier = bits[0];
        if (rslv != null && rslv.hasTaxon(TAXON_ID)) {
            int resCount = rslv.countResolutions(TAXON_ID, ensemblIdentifier);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + ensemblIdentifier + " count: " + resCount + " Human identifier: "
                         + rslv.resolveId(TAXON_ID, ensemblIdentifier));
                return null;
            }
            id = rslv.resolveId(TAXON_ID, ensemblIdentifier).iterator().next();
        }
        return id;
    }
}
