package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
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
import org.intermine.xml.full.Item;

/**
 * DataConverter to load flat file linking BDGP clones to Flybase genes.
 * @author Richard Smith
 */
public class BDGPCloneConverter extends CDNACloneConverter
{
//    protected static final Logger LOG = Logger.getLogger(BDGPCloneConverter.class);
    private Map<String, Item> genes = new HashMap<String, Item>();
    private static final String TAXON_FLY = "7227";
    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public BDGPCloneConverter(ItemWriter writer, Model model)
        throws ObjectStoreException,
               MetaDataException {
        super(writer, model, "BDGP", "BDGP cDNA clone data set");

        organism = createItem("Organism");
        organism.setAttribute("taxonId", TAXON_FLY);
        store(organism);
    }


    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }

        BufferedReader br = new BufferedReader(reader);
        //intentionally throw away first line
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t", -1); //keep trailing empty Strings
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Item gene = getGene(array[0]);
            String[] cloneIds = array[3].split(";");
            for (int i = 0; i < cloneIds.length; i++) {
                Item clone = createBioEntity("CDNAClone", cloneIds[i], "secondaryIdentifier",
                                             organism.getIdentifier());
                if (gene != null) {
                    clone.setReference("gene", gene.getIdentifier());
                }
                store(clone);
            }
        }
    }

    private Item getGene(String identifier) throws ObjectStoreException {
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return null;
        }
        int resCount = rslv.countResolutions(TAXON_FLY, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + identifier + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(TAXON_FLY, identifier));
            return null;
        }
        String primaryIdentifier = rslv.resolveId(TAXON_FLY, identifier).iterator().next();
        if (genes.containsKey(primaryIdentifier)) {
            return genes.get(primaryIdentifier);
        }
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", primaryIdentifier);
        gene.setReference("organism", organism);
        genes.put(primaryIdentifier, gene);
        store(gene);
        return gene;
    }
}
