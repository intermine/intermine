package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/*
 * - is it correct that for non 180454 genes identifier is not set?
 * - is it correct that for non 180454 genes organism is not set
 * WBGene00044949 X ?
 */

/*
 * Inspired by ncbi_pubmed_xml.pl perl script. Basic optimization is
 * in the fact that data are loaded and processed incrementally by organism.
 * Memory can be saved by just saving referencesId to publications instead
 * of publications.
 *
 */
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
 *  Configuration file must be configured in this way:
 *  &lt;source name="pubmed-gene" type="pubmed-gene"&gt;
 *      &lt;property name="src.data.dir" location="/shared/data/pubmed/current"/&gt;
 *      &lt;property name="src.data.dir.includes" value="gene2pubmed"/&gt;
 *      &lt;property name="infoFile" location="/shared/data/pubmed/current/gene_info"/&gt;
 *   &lt;/source&gt;
 *    => Convertor  is run only once with reader set to references file == pubmed2gene
 *
 * @author Jakub Kulaviak
 */
public class PubMedGeneConverter extends FileConverter
{
    private String referencesFileName;
    private File infoFile;
    private Set<Integer> organismsToProcess = new HashSet<Integer>();
    private Map<Integer, String> publications = new HashMap<Integer, String>();
    private String datasetRefId;
    private String datasourceRefId;
    protected IdResolverFactory resolverFactory;
    /**
     * @param writer item writer
     * @param model model
     * @throws ObjectStoreException if can't store the datasource/dataset
     */
    public PubMedGeneConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        // process only these organisms
        // S.cerevisiae
        organismsToProcess.add(new Integer(4932));
        // C.elegans
        organismsToProcess.add(new Integer(6239));
        // D.melanogaster
        organismsToProcess.add(new Integer(7227));
        // A.gambiae str PEST
        organismsToProcess.add(new Integer(180454));

        Item datasource = createItem("DataSource");
        datasource.setAttribute("name", "NCBI");
        store(datasource);
        datasourceRefId = datasource.getIdentifier();

        Item dataset = createItem("DataSet");
        dataset.setAttribute("title", "PubMed to gene mapping");
        dataset.setReference("dataSource", datasourceRefId);
        store(dataset);
        datasetRefId = dataset.getIdentifier();
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * Process references file and gene information file. Implementation based on the fact
     * that both files are sorted by the organism id. This is checked and if it is not true
     * an exception is thrown.
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
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
                if (organismsToProcess.contains(organismId)) {
                    Map<Integer, List<String>> geneToPub = convertAndStorePubs(ref.getReferences());
                    Item organism = createOrganism(organismId);
                    geneConverter.processGenes(geneToPub, organismId, organism);
                }
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

    private Item createOrganism(Integer organismId) {
        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", organismId.toString());
        try {
            store(organism);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("storing organism failed", e);
        }
        return organism;
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

    /**
     * @return organisms that only should be processed
     */
    public Set<Integer> getOrganismsToProcess() {
        return organismsToProcess;
    }

    /**
     * @param organismsToProcess organisms that only should be processed
     */
    public void setOrganismsToProcess(Set<Integer> organismsToProcess) {
        this.organismsToProcess = organismsToProcess;
    }
}

