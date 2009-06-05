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
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * @author Julie Sullivan
 */
public class TreefamConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "TreeFam dataset";
    private static final String DATA_SOURCE_NAME = "TreeFam";
    private static final Logger LOG = Logger.getLogger(TreefamConverter.class);
    private Set<String> taxonIds = new HashSet();
    protected File geneFile;
    private Map<String, Item> genes = new HashMap();
    private Set<String> synonyms = new HashSet();
    private Map<String, String> organisms = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public TreefamConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Sets the list of taxonIds that should be processed
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setTreefamOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    /**
     * Read genes file
     *
     * Col0 = internal id (eg, 12345)
     * Col4 = identifier (eg, Fbgn)
     * Col8 = taxid
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     * @throws ObjectStoreException if the objects cannot be stored to the database
     */
    public void readGenes(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String bits[] = lineIter.next();
            if (bits.length < 9) {
                throw new RuntimeException("bad data file, couldn't process:" + bits);
            }
            String id = bits[0];
            String identifier = bits[4];
            String taxonId = bits[8];
            System.out.println("taxonId:" + taxonId);
            try {
                new Integer(taxonId);
            } catch (NumberFormatException e) {
                continue;
            }
            System.out.println("taxonId:" + taxonId);
            if (!taxonIds.isEmpty() && !taxonIds.contains(taxonId)) {
                System.out.println("taxonId:" + taxonId);
                // don't create gene object if gene isn't from an organism of interest
                continue;
            }
            genes.put(id, createGene(identifier, taxonId));
        }
    }

    /**
     * Set the gene input file.
     *
     * @param geneFile gene input file
     */
    public void setGeneFile(File geneFile) {
        this.geneFile = geneFile;
    }

    /**
     * Process the text file
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {

        if (geneFile == null) {
            throw new NullPointerException("geneFile property not set");
        }

        try {
            readGenes(new FileReader(geneFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading geneFile", err);
        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String bits[] = lineIter.next();
            if (bits.length < 4) {
                continue;
            }
            String gene1identifier = bits[0];
            String gene2identifier = bits[1];
            //String taxonId = bits[2];
            String bootstrap = bits[3];

            Item gene1 = genes.get(gene1identifier);
            if (gene1 != null) {
                Item gene2 = genes.get(gene2identifier);
                if (gene2 != null) {
                    processHomologues(gene1, gene2, bootstrap);
                    processHomologues(gene2, gene1, bootstrap);
                }
            }
        }

        for (Item item : genes.values()) {
            try {
                store(item);
                createSynonym(item.getIdentifier(), "identifier",
                              item.getAttribute("primaryIdentifier").getValue());
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }

    private void processHomologues(Item gene1, Item gene2, String bootstrap)
    throws ObjectStoreException {
        Item homologue = createItem("Homologue");
        homologue.setAttribute("bootstrapScore", bootstrap);
        homologue.setReference("gene", gene1);
        homologue.setReference("homologue", gene2);
        String type = "orthologue";
        String refId = gene1.getReference("organism").getRefId();
        if (gene2.getReference("organism").getRefId().equals(refId)) {
            type = "inParalogue";
        }
        homologue.setAttribute("type", type);
        gene1.addToCollection("homologues", homologue);
        try {
            store(homologue);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }

    // assumes genes are unique
    private Item createGene(String identifier, String taxonId)
    throws ObjectStoreException {
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", identifier);
        gene.setReference("organism", getOrganism(taxonId));
        return gene;
    }

    private void createSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
        String key = subjectId + type + value;
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (!synonyms.contains(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            synonyms.add(key);
            try {
                store(syn);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }

    private String getOrganism(String taxonId)
    throws ObjectStoreException {
        String refId = organisms.get(taxonId);
        if (refId == null) {
            Item item = createItem("Organism");
            item.setAttribute("taxonId", taxonId);
            refId = item.getIdentifier();
            organisms.put(taxonId, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
        return refId;
    }
}
