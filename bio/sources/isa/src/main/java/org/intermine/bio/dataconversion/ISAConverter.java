package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2019 FlyMine
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
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();
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

        JsonNode studyNode = root.path("studies");
        for (JsonNode study : studyNode) {
            String identifier = study.path("identifier").asText();
            String title = study.path("title").asText();
            String description = study.path("description").asText();
            String filename = study.path("filename").asText();
            String subDate = study.path("submissiondate").asText();
            String pubDate = study.path("publicreleasedate").asText();

            LOG.warn("STUDY " + identifier);
            LOG.warn(title + " -- " + filename + " | " + subDate);

            getProtocols(study);
            getMaterials(study);
            getAssays(study);
        }

        LOG.warn("-----");


        //createInvestigationWithPojo(file);


    }

    private void getProtocols(JsonNode study) {
        JsonNode protocolNode = study.path("protocols");
        for (JsonNode protocol : protocolNode) {
            String protName = protocol.path("name").asText();
            String pDescr = protocol.path("description").asText();

            Integer protPar = protocol.path("parameters").size();

            LOG.warn("PROT " + protName + " pars: " + protPar);
            LOG.warn("PROT " + pDescr);

            JsonNode parameterNode = protocol.path("parameters");

            for (JsonNode parameter : parameterNode) {
                String id = parameter.path("@id").asText();
                String annotationValue = parameter.path("parameterName").get("annotationValue").asText();
                //String annotationValue = pnode.path("annotationValue").asText();
                String termAccession = parameter.path("termAccession").asText();
                //String termSource = pnode.path("termSource").asText();

                LOG.info("PPar " + id + ": " + annotationValue + "|" + termAccession);
            }
        }
    }


    private void getMaterials(JsonNode study) {
        LOG.warn("IN MATERIALS...");
        JsonNode sourceNode = study.path("materials").get("sources");
        for (JsonNode source : sourceNode) {
            LOG.warn("IN Sources...");
            getSource(source);
        }

        JsonNode sampleNode = study.path("materials").get("samples");
        for (JsonNode sample : sampleNode) {
            LOG.warn("IN Samples...");
            getSource(sample);
            getFactorValues(sample.path("factorValues"));
        }
    }

    private void getAssays(JsonNode study) {
        JsonNode assayNode = study.path("assays");
        for (JsonNode assay : assayNode) {
            String fileName = assay.path("fileName").asText();
            String technologyPlatform = assay.path("technologyPlatform").asText();

            LOG.warn("ASSAY " + fileName + " on: " + technologyPlatform);

            // GET characteristicCategories
            JsonNode characteristicCategoriesNode = assay.get("characteristicCategories");
//            LOG.warn("characteristicCategories node type is "
//                    + characteristicCategoriesNode.getNodeType().toString()
//                    + " and size " + characteristicCategoriesNode.size());

            for (JsonNode inode : characteristicCategoriesNode) {
                LOG.warn("IN characteristicCategory ...");
                String characteristicCategoryId = inode.path("@id").asText(); // prob not useful

                LOG.warn("--characteristicType: " + inode.path("characteristicType").size());

                Term term = new Term(inode.path("characteristicType")).invoke();
                String id = term.getId();
                String annotationValue = term.getAnnotationValue();
                String termAccession = term.getTermAccession();
                String termSource = term.getTermSource();

                LOG.info("CHAR " + id + ": " + annotationValue + "|" + termAccession + "|" + termSource);
            }

            // GET measurementType
            JsonNode measurementTypeNode = assay.get("measurementType");
            LOG.warn("MT node type is " + measurementTypeNode.getNodeType().toString()
                    + " and size " + measurementTypeNode.size());

            Term term = new Term(measurementTypeNode).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("CHAR " + id + ": " + annotationValue + "|" + termAccession + "|" + termSource);


            // GET datafiles
            JsonNode dataFilesNode = assay.get("dataFiles");
            LOG.warn("DF node type is " + dataFilesNode.getNodeType().toString()
                    + " and size " + dataFilesNode.size());

            for (JsonNode inode : dataFilesNode) {

                DataFile file = new DataFile(inode).invoke();
                String fileId = file.getId();
                String name = file.getName();
                String type = file.getType();

                LOG.info("FILE " + fileId + ": " + type + "|" + name);
            }



        }
    }




    private void getSource(JsonNode source) { //TODO rename to more generic
        String sourceName = source.path("name").asText();
        String sourceId = source.path("@id").asText();

        Integer cSize = source.path("characteristics").size();

        LOG.warn("SOURCE " + sourceId + ": " + sourceName + " with " + cSize + " characteristics");

        JsonNode characteristicNode = source.path("characteristics");

        for (JsonNode characteristic : characteristicNode) {
            String categoryId = characteristic.path("category").get("@id").asText();

            JsonNode value = characteristic.path("value");
            Term term = new Term(value).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("CHAR " + categoryId + ": " + id + "|" + annotationValue + "|"
                    + termAccession + "|" + termSource);
        }
    }

    private void getFactorValues(JsonNode source) { //TODO rename to more generic

        for (JsonNode characteristic : source) {
            String categoryId = characteristic.path("category").get("@id").asText();

            JsonNode value = characteristic.path("value");
            Term term = new Term(value).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("CHAR " + categoryId + ": " + "id" + annotationValue + "|"
                    + termAccession + "|" + termSource);
        }
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


    private void getTerm(JsonNode source) { //TODO rename to more generic
        //    String sourceName = source.path("name").asText();
        //    String sourceId = source.path("@id").asText();

        Term term = new Term(source).invoke();
        String annotationValue = term.getAnnotationValue();
        String termAccession = term.getTermAccession();
        String termSource = term.getTermSource();

        LOG.info("CHAR: " + annotationValue + "|" + termAccession + "|" + termSource);

    }


    private class Term {
        private JsonNode node;
        private String id;
        private String annotationValue;
        private String termAccession;
        private String termSource;

        public Term(JsonNode node) {
            this.node = node;
        }

        public String getId() {
            return id;
        }

        public String getAnnotationValue() {
            return annotationValue;
        }

        public String getTermAccession() {
            return termAccession;
        }

        public String getTermSource() {
            return termSource;
        }

        public Term invoke() {
            id = node.path("@id").asText();
            annotationValue = node.path("annotationValue").asText();
            termAccession = node.path("termAccession").asText();
            termSource = node.path("termSource").asText();
            return this;
        }
    }

    private class DataFile {
        private JsonNode node;
        private String id;
        private String name;
        private String type;

        public DataFile(JsonNode node) {
            this.node = node;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public DataFile invoke() {
            id = node.path("@id").asText();
            name = node.path("name").asText();
            type = node.path("type").asText();
            return this;
        }
    }


}
