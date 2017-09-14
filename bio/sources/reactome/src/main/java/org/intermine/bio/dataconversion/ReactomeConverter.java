package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */



import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 *
 *
 * @author Julie Sullivan
 */
public class ReactomeConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ReactomeConverter.class);
    private Set<String> taxonIds;
    private Map<String, Item> pathways = new HashMap<String, Item>();
    private Map<String, Item> proteins = new HashMap<String, Item>();
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ReactomeConverter(ItemWriter writer, Model model) {
        super(writer, model, "Reactome", "Reactome pathways data set");
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setReactomeOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        if (taxonIds == null || taxonIds.isEmpty()) {
            throw new IllegalArgumentException("No organism data provided for reactome");
        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            if (line.length != 6) {
                throw new RuntimeException("Invalid line length " + line.length);
            }

            String accession = line[0];
            String pathwayIdentifier = line[1];
            String uri = line[2];
            String pathwayName = line[3];
            String evidenceCode = line[4];
            String organismName = line[5];

            String taxonId = getTaxonId(organismName);
            if (taxonId == null) {
                // invalid organism
                continue;
            }
            Item pathway = getPathway(pathwayIdentifier, pathwayName);

            Item protein = getProtein(accession, taxonId);
            protein.addToCollection("pathways", pathway);

            pathway.addToCollection("proteins", protein);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        for (Item item : proteins.values()) {
            store(item);
        }
        for (Item item : pathways.values()) {
            store(item);
        }
    }

    private String getTaxonId(String organismName) {
        String[] bits = organismName.split(" ");
        if (bits.length != 2) {
            LOG.warn("Couldn't parse the organism name " + organismName);
            return null;
        }
        OrganismData od = OR.getOrganismDataByGenusSpecies(bits[0], bits[1]);
        if (od == null) {
            LOG.warn("Couldn't parse the organism name " + organismName);
            return null;
        }
        String taxonId = String.valueOf(od.getTaxonId());
        if (!taxonIds.contains(taxonId)) {
            return null;
        }
        return taxonId;
    }


    private Item getPathway(String pathwayId, String pathwayName) throws ObjectStoreException {
        Item item = pathways.get(pathwayId);
        if (item == null) {
            item = createItem("Pathway");
            item.setAttribute("identifier", pathwayId);
            item.setAttribute("name", pathwayName);
            pathways.put(pathwayId, item);
        }
        return item;
    }

    private Item getProtein(String accession, String taxonId)
        throws ObjectStoreException {
        Item item = proteins.get(accession);
        if (item == null) {
            item = createItem("Protein");
            item.setAttribute("primaryAccession", accession);
            item.setReference("organism", getOrganism(taxonId));
            proteins.put(accession, item);
        }
        return item;
    }
}
