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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;

/**
 * DataConverter to parse homophila data file into Items.
 *
 * @author Thomas Riley
 */
public class HomophilaConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(HomophilaConverter.class);

    /* Indexes into tab seperated array. */
    private static final int TRANSLATION_ID = 0;
    private static final int OMIM_ID = 1;
    private static final int PROTEIN_ID = 2;
    private static final int E_VALUE = 3;

    protected Map<String, String> diseaseDescriptions = new HashMap<String, String>();
    protected Map<String, String> proteinToGene = new HashMap<String, String>();
    protected Map<String, Item> proteins = new HashMap<String, Item>();
    protected Map<String, Item> diseases = new HashMap<String, Item>();
    protected Map<String, Item> genes = new HashMap<String, Item>();

    protected int matchCount = 0;
    protected int annotationCount = 0;

    /* Singleton items. */
    protected String orgHuman;
    protected String orgDrosophila;
    protected Item pub1, pub2;

    protected File diseaseFile;
    protected File proteinGeneFile;

    protected IdResolverFactory resolverFactory;
    Set<String> problemProteins = new HashSet<String>();

    /**
     * Construct a new instance of HomophilaConverter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public HomophilaConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, "Homophila", "Homophila data set");

        orgHuman = getOrganism("9606");
        orgDrosophila = getOrganism("7227");

        pub1 = createItem("Publication");
        pub1.addAttribute(new Attribute("pubMedId", "11381037"));
        store(pub1);

        pub2 = createItem("Publication");
        pub2.addAttribute(new Attribute("pubMedId", "11752278"));
        store(pub2);

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("protein");
    }

    /**
     * Read omim ids and descriptions from reader. Input is two columns, tab seperated, first
     * column is OMIM id and second column is a line of description. There may be several lines
     * of description (with the same OMIM id) on adjacent lines.
     *
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     */
    protected void readDiseaseDescriptions(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuffer descBuff = new StringBuffer();
        String omim = "";
        while ((line = br.readLine()) != null) {
            String[] fields = StringUtils.split(line, '\t');
            if (omim.equals(fields[0])) {
                descBuff.append("; ");
                descBuff.append(fields[1]);
                continue;
            }
            if (!StringUtils.isBlank(omim)) {
                // New entry
                diseaseDescriptions.put(omim, descBuff.toString());
            }
            omim = fields[0];
            descBuff = new StringBuffer();
            if (fields.length > 1) {
                descBuff.append(fields[1]);
            }
        }
        diseaseDescriptions.put(omim, descBuff.toString()); // last line
    }

    /**
     * Read mappings from protein id to gene id.
     *
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     */
    protected void readProteinGeneFile(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String line;
        int done = 1;
        while ((line = br.readLine()) != null) {
            String[] fields = StringUtils.split(line, '\t');
            if (fields.length == 2) {
                proteinToGene.put(fields[0], fields[1]);
            } else {
                LOG.warn("line " + done + " in protein_gene.txt contained " + fields.length
                         + " columns");
            }
            done++;
        }
    }

    /**
     * Set the disease description input file. This file just contains OMIM ids and
     * several lines of description for each id.
     *
     * @param diseaseFile disease description input file
     */
    public void setDiseasefile(File diseaseFile) {
        this.diseaseFile = diseaseFile;
    }

    /**
     * Set the protein to gene input file. This file contains two columns, the NP_ protein
     * id followed by the gene id.
     *
     * @param proteinGeneFile protein to gene input file
     */
    public void setProteingenefile(File proteinGeneFile) {
        this.proteinGeneFile = proteinGeneFile;
    }

    /**
     * Reads disease description file first, then reads homophila matches file.
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (diseaseFile == null) {
            throw new NullPointerException("diseaseFile property not set");
        } else if (proteinGeneFile == null) {
            throw new NullPointerException("proteinGeneFile property not set");
        }

        try {
            readDiseaseDescriptions(new FileReader(diseaseFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading disease descriptions", err);
        }

        try {
            readProteinGeneFile(new FileReader(proteinGeneFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading protein to gene file", err);
        }
        BufferedReader br = new BufferedReader(reader);
        br.readLine();
        String line;
        int done = 0;
        while ((line = br.readLine()) != null) {
            String[] array = StringUtils.split(line, '\t');
            newBlastMatch(array);
            if (++done % 10000 == 0) {
                LOG.info("Processed " + done + " homophila matches");
            }
        }
    }

    /**
     * Create new BlastMatch item. Fills in all attributes and references.
     *
     * @param array line of homophila matches file
     * @return BlastMatch Item
     * @throws ObjectStoreException if something goes wrong
     * @throws SAXException if something goes wrong
     */
    protected Item newBlastMatch(String[] array)
        throws ObjectStoreException, SAXException {
        Item drosophilaProtein = findTranslation(array);
        if (drosophilaProtein != null) {
            Item item = createItem("BlastMatch");
            item.setReference("hit", findProtein(array));
            item.setReference("query", drosophilaProtein);
            item.setAttribute("EValue", array[E_VALUE]);
            store(item);
            matchCount++;
            return item;
        }
        return null;
    }

    /**
     * Create new Translation item. Fills in all attributes and references.
     *
     * @param array line of homophila matches file
     * @return Translation Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item findTranslation(String[] array) throws ObjectStoreException {
        String proteinCG = array[TRANSLATION_ID];
        IdResolver resolver = resolverFactory.getIdResolver();
        int resCount = resolver.countResolutions("7227", proteinCG);
        if (resCount != 1) {
            if (!problemProteins.contains(proteinCG)) {
                LOG.info("RESOLVER: failed to resolve protein to one identifier, ignoring protein: "
                        + proteinCG + " count: " + resCount + " FBpp: "
                        + resolver.resolveId("7227", proteinCG));
                problemProteins.add(proteinCG);
            }
            return null;
        }
        String secondaryIdentifier = resolver.resolveId("7227", proteinCG).iterator().next();
        Item translation = proteins.get(secondaryIdentifier);
        if (translation == null) {
            translation = createItem("Protein");
            translation.setAttribute("secondaryIdentifier", secondaryIdentifier);
            translation.setReference("organism", orgDrosophila);
            proteins.put(secondaryIdentifier, translation);
            store(translation);
        }
        return translation;
    }

    /**
     * Create new Protein item. Fills in all attributes and references.
     *
     * @param array line of homophila matches file
     * @return Protein Item
     * @throws ObjectStoreException if something goes wrong
     * @throws SAXException if storing the protein synonym fails
     */
    protected Item findProtein(String[] array)
        throws ObjectStoreException, SAXException {
        Item protein = proteins.get(array[PROTEIN_ID]);
        if (protein == null) {
            protein = createItem("Protein");
            String primaryIdentifier = array[PROTEIN_ID];
            protein.setAttribute("primaryIdentifier", primaryIdentifier);
            protein.setReference("organism", orgHuman);
            Item gene = findGene(array);
            if (gene != null) {
                protein.addToCollection("genes", gene);
            }
            proteins.put(array[PROTEIN_ID], protein);
            store(protein);
        }
        return protein;
    }

    /**
     * Create new Gene item. Fills in all attributes and references.
     *
     * @param array line of homophila matches file
     * @return Gene Item
     * @throws ObjectStoreException if something goes wrong
     * @throws SAXException if storing the protein synonym fails
     */
    protected Item findGene(String[] array)
        throws ObjectStoreException, SAXException {
        String geneId = proteinToGene.get(array[PROTEIN_ID]);
        if (geneId == null) {
            LOG.warn("protein id " + array[PROTEIN_ID] + " doesn't map to a gene id");
            return null;
        }
        Item gene = genes.get(geneId);
        if (gene == null) {
            gene = createItem("Gene");
            genes.put(geneId, gene);
            gene.setAttribute("symbol", geneId);
            gene.setReference("organism", orgHuman);
            gene.addToCollection("omimDiseases", findDisease(array));
            store(gene);
        }
        return gene;
    }

    /**
     * Create new Disease item. Fills in all attributes and references.
     *
     * @param array line of homophila matches file
     * @return Disease Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item findDisease(String[] array) throws ObjectStoreException {
        Item disease = diseases.get(array[OMIM_ID]);
        if (disease == null) {
            disease = createItem("Disease");
            diseases.put(array[OMIM_ID], disease);
            disease.addAttribute(new Attribute("omimId", array[OMIM_ID]));
            String desc = diseaseDescriptions.get(array[OMIM_ID]);
            if (desc == null) {
                LOG.error("no disease description for OMIM " + array[OMIM_ID]);
            } else {
                disease.addAttribute(new Attribute("description", desc));
            }
            store(disease);
        }
        return disease;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        LOG.info("genes.size() == " + genes.size());
        LOG.info("proteins.size() == " + proteins.size());
        LOG.info("diseases.size() == " + diseases.size());
        LOG.info("matches.size() == " + matchCount);
        LOG.info("annotation.size() == " + annotationCount);
        super.close();
    }
}

