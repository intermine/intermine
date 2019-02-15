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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.isa.Investigation;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author sc
 */
public class ISAConverter extends BioFileConverter {
    private static final Logger LOG = Logger.getLogger(ISAConverter.class);
    private Map<String, Map> people;
    private Map<String, Map> publications;
    private Map<String, Map> comments;  // add field for ref?
    private Map<String, Map> sdd;       // studyDesignDescriptors
    private Map<String, Map> osr;
    private Map<String, Map> protocols;
    private Map<String, Map> protpars;  // protocol.parameters



    private Set<String> taxonIds;
    private Map<String, Item> pathways = new HashMap<>();
    private Map<String, Item> proteins = new HashMap<>();
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();

    /**
     * Constructor
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model  the Model
     */
    public ISAConverter(ItemWriter writer, Model model) {
        super(writer, model, "ISA", "ISA data");
    }

    /*
     *    json structure:
     *    ---------------
     *    investigation
     *       people
     *       publications
     *       comments
     *       ontologySourceReferences
     *       studies
     *           publications
     *           people
     *           studyDesignDescriptors
     *           protocols
     *               protocolTypes
     *               parameters
     *                   parameterName
     *           etc TODO
     *
     */

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {


        File file = getFiles();
   // private Map<String, Map> protocols;

        JsonNode root = new ObjectMapper().readTree(file);

        //otherAccess(root);

        JsonNode osr = root.path("ontologySourceReferences");

        LOG.warn("OSR type is ... " + osr.getNodeType().toString());

        mapOSR(osr);

        // do the storing

        JsonNode study = root.path("studies");
        for (JsonNode node : study) {
            String identifier = node.path("identifier").asText();
            String title = node.path("title").asText();
            String description = node.path("description").asText();
            String filename = node.path("filename").asText();
            String subDate = node.path("submissiondate").asText();
            String pubDate = node.path("publicreleasedate").asText();

            LOG.warn("STUDY " + identifier);
            LOG.warn(title + " -- " + filename + " | " + subDate);


            JsonNode protocol = node.path("protocols");
            for (JsonNode n2 : protocol) {
                String protName = n2.path("name").asText();
                String pDescr = n2.path("description").asText();

                Integer protPar = n2.path("parameters").size();

                LOG.warn("PROT " + protName + " pars: " + protPar);
                LOG.warn("PROT " + pDescr);

                JsonNode ppar = n2.path("parameters");
                LOG.warn("PPAR " + ppar.getNodeType().toString());

                for (JsonNode pnode : ppar) {
                    LOG.warn("QQ");
                    String id = pnode.path("@id").asText();
                    String annotationValue = pnode.path("parameterName").get("annotationValue").asText();
                    //String annotationValue = pnode.path("annotationValue").asText();
                    String termAccession = pnode.path("termAccession").asText();
                    //String termSource = pnode.path("termSource").asText();

                    LOG.info("STUDY DESDES " + annotationValue + "|" + termAccession + "|" + id);
                }


            }
        }

        LOG.warn("-----" + study.path("identifier").asText());



        //createInvestigationWithPojo(file);


    }

    private void mapOSR(JsonNode osr) {
        for (JsonNode node : osr) {
            String name = node.path("name").asText();
            String description = node.path("description").asText();
            String filename = node.path("file").asText();
            String version = node.path("version").asText();

            LOG.warn("OSR " + name);
            LOG.warn(description + " -- " + filename + " | " + version);
        }
    }

    private void mapOSRg(JsonNode osr) {
        osr.getNodeType().toString();
        //Iterator ontology = osr.elements()
        /*
        for ( Iterator element : osr.elements()) {

            String name = node.path("name").asText();
            String description = node.path("description").asText();
            String filename = node.path("file").asText();
            String version = node.path("version").asText();

            LOG.warn("OSR " + name);
            LOG.warn(description + " -- " + filename + " | " + version);
        }
        */
    }




    private void otherAccess(JsonNode root) {
        String invIdentifier = root.get("identifier").textValue();
        LOG.warn("INV ID " + invIdentifier);

        String osrName = root.get("ontologySourceReferences").get(1).get("name").textValue();
        LOG.warn("OSR name " + osrName);
    }

    private File getFiles() throws FileNotFoundException {
        File file = getCurrentFile();
        if (file == null) {
            throw new FileNotFoundException("No valid data files found.");
        }

        LOG.info("ISA: Reading " + file.getName());
        return file;
    }

    private void createInvestigationWithPojo(File file) throws java.io.IOException, ObjectStoreException {

        // check if useful...
//        Investigation isaInv = new Investigation();
//        Study isaStudy = new Study();
//        OntologySourceReference isaOSR = new OntologySourceReference();


        // item creation here using pojos
        ObjectMapper mapper = new ObjectMapper();
        Investigation isaInv1 = mapper.readValue(file, Investigation.class);
//        Study isaStudy = mapper.readValue(file, Study.class);

        LOG.warn("investigation " + isaInv1.identifier);
        //String prettyStaff1 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(isaInv);
        //LOG.info(prettyStaff1);

        Item inv = createItem("Investigation");
        if (!StringUtils.isEmpty(isaInv1.identifier)) {
            inv.setAttribute("identifier", isaInv1.identifier);
        }
        if (!StringUtils.isEmpty(isaInv1.description)) {
            inv.setAttribute("description", isaInv1.description);
        }
        store(inv);
    }


    private void justTesting() { //USE_JAVA_ARRAY_FOR_JSON_ARRAY
        Map<String, String> myMap = new HashMap<String, String>();
        int ord = 0;
        for (Map.Entry<String, String> entry : myMap.entrySet()) {

            //    System.out.println("[" + ord++ + "] " + entry.getKey() + " : " + entry.getValue());
            System.out.println("[" + ord++ + "]");
            System.out.println(entry.getKey());
            System.out.println((entry));
        }
    }

    //
    // OLDIES
    //

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
