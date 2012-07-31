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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * DataConverter to load flat file linking BDGP clones to Flybase genes.
 * @author Wenyan Ji
 */
public class ImageCloneConverter extends CDNACloneConverter
{
//    protected static final Logger LOG = Logger.getLogger(ImageCloneConverter.class);

    protected Map<String, Item> geneMap = new HashMap<String, Item>();
    protected Map<String, Item> cloneMap = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public ImageCloneConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model, "RZPD", "RZPD uniprot data set");

        organism = createItem("Organism");
        organism.setAttribute("abbreviation", "HS");
        store(organism);
    }

    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
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

            String[] geneId = array[16].trim().split(",");
            String cloneId = array[5].trim();
            // if clone points to more than one gene, or points to a gene without identifier
            // don't create gene
            if (geneId.length == 1 && geneId[0].length() > 1 && StringUtil.allDigits(cloneId)) {
                Item gene = createGene("Gene", geneId[0], organism.getIdentifier(),
                                       getItemWriter());
                createClone("CDNAClone", cloneId, organism.getIdentifier(),
                            gene.getIdentifier(), getItemWriter());
            }
        }
    }

    /**
     * @param clsName = target class name
     * @param id = identifier
     * @param ordId = ref id for organism
     * @param writer = itemWriter write item to objectstore
     * @return item
     * @throws exception if anything goes wrong when writing items to objectstore
     */
    private Item createGene(String clsName, String id, String orgId, ItemWriter writer)
        throws Exception {
        Item gene = geneMap.get(id);
        if (gene == null) {
            gene = createItem(clsName);
            gene.setAttribute("primaryIdentifier", id);
            gene.setReference("organism", orgId);
            geneMap.put(id, gene);
            store(gene);
        }
        return gene;
    }

    /**
     * @param clsName = target class name
     * @param id = identifier
     * @param ordId = ref id for organism
     * @param geneId = ref id for gene item
     * @param dbId = ref id for db item
     * @param writer = itemWriter write item to objectstore
     * @throws exception if anything goes wrong when writing items to objectstore
     */
    private void createClone(String clsName, String id, String orgId, String geneId,
            ItemWriter writer)
        throws Exception {
        Item clone = cloneMap.get(id);
        if (clone == null) {
            clone = createItem(clsName);
            clone.setAttribute("primaryIdentifier", id);
            clone.setReference("organism", orgId);
            clone.setReference("gene", geneId);
            cloneMap.put(id, clone);
            store(clone);
        }
    }
}
