package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2018 FlyMine
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
public class ISAConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ISAConverter.class);
    private Set<String> taxonIds;
    private Map<String, Item> pathways = new HashMap<String, Item>();
    private Map<String, Item> proteins = new HashMap<String, Item>();
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ISAConverter(ItemWriter writer, Model model) {
        super(writer, model, "ISA", "ISA data");
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setISAOrganisms(String taxonIds) {
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


    private void justTesting() {USE_JAVA_ARRAY_FOR_JSON_ARRAY
        Map<String,String> myMap = new HashMap<String, String>();
        int ord = 0;
    for(Map.Entry<String, String> entry: myMap.entrySet()) {

    //    System.out.println("[" + ord++ + "] " + entry.getKey() + " : " + entry.getValue());
        System.out.println("[" + ord++ + "]");
        System.out.println(entry.getKey());
        System.out.println((entry));
    }
    }


    private void justTesting2() {
        Map<String,List<String>> myMap = new HashMap<String, List<String>>();
        int ord = 0;
        for(Map.Entry<String, List<String>> entry: myMap.entrySet()) {

            ord++;
//    System.out.println("[" + ord++ + "] " + entry.getKey() + " : " + entry.getValue());

            System.out.println("[" + ord + "]");
            System.out.println(entry.getKey());

            if (ord != 6) {
                System.out.println(entry);
                System.out.println(entry.getValue());

                if (entry.getValue().startsWith("[")) {
                    System.out.println(entry.getValue().get(0));
                }

            } else {
                System.out.println("...............\n");
            }





            //    System.out.println("[" + ord++ + "] " + entry.getKey() + " : " + entry.getValue());
            System.out.println("[" + ord++ + "]");
            System.out.println(entry.getKey());
            System.out.println((entry));
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
