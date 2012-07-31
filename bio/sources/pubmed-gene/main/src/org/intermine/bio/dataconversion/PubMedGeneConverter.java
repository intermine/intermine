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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.bio.util.BioUtil;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 * DataConverter creating items from PubMed data files. Data files contain
 * information about references between publications and genes and some
 * information about genes.
 *
 * Normally takes data from /shared/data/pubmed/gene2pubmed and
 * /shared/data/pubmed/gene_info (the current files were downloaded from
 *  ftp://ftp.ncbi.nlm.nih.gov/gene/DATA) and then writes the xml file for linking the
 *  ncbi gene ID to one or more pubmed IDs.
 *
 * @author Jakub Kulaviak
 */
public class PubMedGeneConverter extends BioFileConverter
{
    private String referencesFileName;
    private File infoFile;
    private Set<String> taxonIds = new HashSet<String>();
    private Map<Integer, String> publications = new HashMap<Integer, String>();
    private String datasetRefId;
    private String datasourceRefId;
    protected IdResolverFactory resolverFactory;
    private Map<Integer, String> organisms = new HashMap<Integer, String>();

    /**
     * @param writer item writer
     * @param model model
     * @throws ObjectStoreException if can't store the datasource/dataset
     */
    public PubMedGeneConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);

        Item datasource = createItem("DataSource");
        datasource.setAttribute("name", "NCBI");
        store(datasource);
        datasourceRefId = datasource.getIdentifier();

        Item dataset = createItem("DataSet");
        dataset.setAttribute("name", "PubMed to gene mapping");
        dataset.setReference("dataSource", datasourceRefId);
        store(dataset);
        datasetRefId = dataset.getIdentifier();
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * Sets the list of taxonIds that should be imported
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setPubmedOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
    }

    /**
     * Process references file and gene information file. Implementation based on the fact
     * that both files are sorted by the organism id. This is checked and if it is not true
     * an exception is thrown.
     * {@inheritDoc}
     */
    public void process(Reader reader)
        throws Exception {

        if (getInfoFile() == null) {
            throw new RuntimeException("PubMedGeneConverter: Property infoFile is not set.");
        }
        Reader infoReader = null;
        try {
            ReferencesFileProcessor proc = new ReferencesFileProcessor(reader);
            infoReader = new FileReader(getInfoFile());
            GenesFileProcessor geneConverter = new GenesFileProcessor(infoReader, this,
                                                                      datasetRefId,
                                                                      resolverFactory);
            Iterator<PubMedReference> it = proc.getReferencesIterator();
            /* Uses ReferencesFileProcessor to obtain iterator over data in
             * references file. In each iteration data for one organism is
             * obtained from references file and is processed by GenesFileProcessor.
             * Genes are saved by GenesFileProcessor, organisms and publications
             * are saved by this convertor.
             * Procedure is based on the fact that both files are sorted by organism id.
             */
            while (it.hasNext()) {
                PubMedReference ref = it.next();
                Integer organismId = ref.getOrganism();
                if (!taxonIds.isEmpty() && !taxonIds.contains(organismId.toString())) {
                    continue;
                }
                Map<Integer, List<String>> geneToPub = convertAndStorePubs(ref.getReferences());
                geneConverter.processGenes(geneToPub, organismId, createOrganism(organismId),
                        taxonIds);
            }
        } catch (ReferencesProcessorException ex) {
            throw new RuntimeException("Conversion failed. File: " + getReferencesFileName(), ex);
        } catch (GenesProcessorException ex) {
            throw new RuntimeException("Conversion failed. File: " + getInfoFile(), ex);
        } catch (Throwable ex) {
            throw new RuntimeException("Conversion failed.", ex);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (infoReader != null) {
                infoReader.close();
            }
        }
    }

    private Map<Integer, List<String>> convertAndStorePubs(
            Map<Integer, List<Integer>> geneToPub) throws ObjectStoreException {
        Map<Integer, List<String>> ret = new HashMap<Integer, List<String>>();
        for (Integer geneId : geneToPub.keySet()) {
            List<Integer> pubs = geneToPub.get(geneId);
            List<String> list = new ArrayList<String>();
            for (Integer pubId : pubs) {
                String writerPubId = publications.get(pubId);
                if (writerPubId == null) {
                    Item pub = createPublication(pubId);
                    writerPubId = pub.getIdentifier();
                    publications.put(pubId, writerPubId);
                    store(pub);
                }
                list.add(writerPubId);
            }
            ret.put(geneId, list);
        }
        return ret;
    }

    private Item createPublication(Integer pubId) {
        Item pub = createItem("Publication");
        pub.setAttribute("pubMedId", pubId.toString());
        return pub;
    }

    /**
     * @return file name of file with references between gene ids and publication ids
     */
    public String getReferencesFileName() {
        return referencesFileName;
    }

    /**
     * @param fileName file name
     * {@link #getReferencesFileName()}
     */
    public void setReferencesFileName(String fileName) {
        this.referencesFileName = fileName;
    }

    /**
     * @return file with information about genes
     */
    public File getInfoFile() {
        return infoFile;
    }

    /**
     * @param infoFile file
     * {@link #getInfoFile()}
     */
    public void setInfoFile(File infoFile) {
        this.infoFile = infoFile;
    }

    private String createOrganism(Integer organismId) {
        Integer taxonId = BioUtil.replaceStrain(organismId);
        String refId = organisms.get(taxonId);
        if (refId != null) {
            return refId;
        }
        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", taxonId.toString());
        try {
            store(organism);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("storing organism failed", e);
        }
        return organism.getIdentifier();
    }
}

