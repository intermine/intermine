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
import org.intermine.xml.full.Reference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.*;

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

    private Map<String, Item> protocols = new HashMap<>();
    private Map<String, Item> protocolParameters = new HashMap<>();
    private Map<String, List<String>> protocolParameterList = new HashMap<>();

    private Map<String, Map> protpars;  // protocol.parameters
    private Set<String> taxonIds;
    private Map<String, Item> pathways = new HashMap<>();
    private Map<String, Item> proteins = new HashMap<>();

    private Integer investigationOID;

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

        JsonNode root = new ObjectMapper().readTree(file);
        //otherAccess(root);

        getInvestigation(root);

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
            String subDate = study.path("submissionDate").asText();
            String pubDate = study.path("publicReleaseDate").asText();

            LOG.warn("STUDY " + identifier);
            LOG.warn(title + " -- " + filename + " | " + subDate);

            getProtocols(study);
            getMaterials(study);
            getAssays(study);
        }

        LOG.warn("-----");

        //createInvestigationWithPojo(file);

    }


    private void getInvestigation(JsonNode investigation) throws ObjectStoreException {

            String identifier = investigation.path("identifier").asText();
            String title = investigation.path("title").asText();
            String description = investigation.path("description").asText();
            String pubDate = investigation.path("publicReleaseDate").asText();
            String subDate = investigation.path("submissionDate").asText();

            Item investigationItem = createInvestigation(identifier, title, description, pubDate, subDate);

            investigationOID = store(investigationItem);
        }

            private void getProtocols(JsonNode study) throws ObjectStoreException {
        JsonNode protocolNode = study.path("protocols");
        for (JsonNode protocol : protocolNode) {

            String id = blunt(protocol.path("@id").asText());
            String name = protocol.path("name").asText();
            String description = protocol.path("description").asText();
            String uri = protocol.path("uri").asText();
            String version = protocol.path("version").asText();

            // get also protocolType (term)..
            createProtocol(id, name, description, uri, version);
            LOG.warn("PROT " + name + " pars: " + protocol.path("parameters").size());

            JsonNode parameterNode = protocol.path("parameters");

            for (JsonNode parameter : parameterNode) {
                String pid = blunt(parameter.path("@id").asText());

                Term term = new Term(parameter.path("parameterName")).invoke();
                String pnid = term.getId();
                String annotationValue = term.getAnnotationValue();
                String termAccession = term.getTermAccession();
                String termSource = term.getTermSource();

                LOG.info("PPAR " + pnid + ": " + annotationValue + "|" + termAccession + "|" + termSource);

                createProtocolParameter(pid, annotationValue);
                addToMap(protocolParameterList, id, pid);
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


    private File getFiles() throws FileNotFoundException {
        File file = getCurrentFile();
        if (file == null) {
            throw new FileNotFoundException("No valid data files found.");
        }

        LOG.info("ISA: Reading " + file.getName());
        return file;
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        for (Map.Entry<String, Item> entry : protocols.entrySet()) {

            Integer protocoloid = store(entry.getValue());

            String pid = entry.getKey();

            LOG.warn("STORING " + pid + " (" + protocoloid);

            List<String> pparid = protocolParameterList.get(pid);

            for (String ppid : pparid) {

                LOG.warn("STORE par " + ppid);
                Reference reference = new Reference();
                reference.setName("protocol");
                reference.setRefId(entry.getValue().getIdentifier());

                Integer ppoid = store(protocolParameters.get(ppid));
                store(reference, ppoid);
            }
        }

//        for (Item item : protocols.values()) {
//            Integer protocoloid = store(item);
//
//        }


    }


    private Item createInvestigation(String id, String title, String description, String subDate, String pubDate)
            throws ObjectStoreException {

            Item item = createItem("Investigation");
            item.setAttribute("identifier", id);

        if (!title.isEmpty()) {
            item.setAttribute("title", title);
        }
            if (!description.isEmpty()) {
                item.setAttribute("description", description);
            }
            if (!subDate.isEmpty()) {
                item.setAttribute("submissionDate", subDate);
            }
            if (!pubDate.isEmpty()) {
                item.setAttribute("version", pubDate);
            }
        return item;
    }



    private Item createProtocol(String id, String name, String description, String uri, String version)
            throws ObjectStoreException {
        Item item = protocols.get(id);
        if (item == null) {
            item = createItem("Protocol");
            item.setAttribute("name", name);
            if (!description.isEmpty()) {
                item.setAttribute("description", description);
            }
            if (!uri.isEmpty()) {
                item.setAttribute("uri", uri);
            }
            if (!version.isEmpty()) {
                item.setAttribute("version", version);
            }
            protocols.put(id, item);
        }
        return item;
    }


    private Item createProtocolParameter(String id, String name)
            throws ObjectStoreException {
        Item item = protocolParameters.get(id);
        if (item == null) {
            item = createItem("ProtocolParameter");
            if (!name.isEmpty()) {
                item.setAttribute("name", name);
            }
            protocolParameters.put(id, item);
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

    private String blunt(String in) {
        return in.replaceAll("#", "");
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


    /**
     * adds an element to a list which is the value of a map
     *
     * @param m     the map (<String, List<String>>)
     * @param key   the key for the map
     * @param value the list
     */
    private static void addToMap(Map<String, List<String>> m, String key, String value) {

        List<String> ids = new ArrayList<String>();

        if (m.containsKey(key)) {
            ids = m.get(key);
        }
        if (!ids.contains(value)) {
            ids.add(value);
            m.put(key, ids);
        }
    }


    //
    // OLDIES/ATTEMPTS
    //

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


}
