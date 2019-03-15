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

    private Map<String, Item> proteins = new HashMap<>();

    private Map<String, Item> factors = new HashMap<>();
    private Map<String, List<String>> factorRefs = new HashMap<>();

    private Integer investigationOID;
    private Integer studyOID;
    private Item studyItem;
    private Reference studyReference;


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

        processInvestigation(root);

        JsonNode ontologyNode = root.path("ontologySourceReferences");
        getOntologies(ontologyNode);  // TODO: make a map to use for referencing terms?

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

            studyItem = createStudy("Study", identifier, title, description, pubDate, subDate);
            studyOID = store(studyItem);
//            studyReference = getStudyReference(studyItem);
            studyReference = getReference("study", studyItem);

            getFactors(study);
            getDesignDescriptors(study);

            getProtocols(study);
            getMaterials(study);
            getAssays(study);

            //TODO
//            getPublications(study);
//            getPeople(study);
//            getCharacteristicCategories(study);
//            getUnitCategories(study);
//            getComments(study);

        }

        LOG.warn("----- parsing over -----------");

        //createInvestigationWithPojo(file);
    }


    private void processInvestigation(JsonNode investigation) throws ObjectStoreException {

        String identifier = investigation.path("identifier").asText();
        String title = investigation.path("title").asText();
        String description = investigation.path("description").asText();
        String pubDate = investigation.path("publicReleaseDate").asText();
        String subDate = investigation.path("submissionDate").asText();

        Item investigationItem = createStudy("Investigation", identifier, title, description, pubDate, subDate);
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
            LOG.warn("PROT " + name + " #pars: " + protocol.path("parameters").size());

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


    private void getFactors(JsonNode study) throws ObjectStoreException {
        JsonNode factorNode = study.path("factors");
        for (JsonNode factor : factorNode) {

            String id = blunt(factor.path("@id").asText());
            String name = factor.path("factorName").asText();

            Term term = new Term(factor.path("factorType")).invoke();
            String termId = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.warn("FACTOR study " + name + ": " + annotationValue);

            Item factorItem = createFactor(id, "", annotationValue, name, annotationValue, "", termAccession);
            Integer oid = store(factorItem);
            store(studyReference, oid);
        }
    }

    // generic
    private Reference getReference(String name, Item item) {
        Reference reference = new Reference();
        reference.setName(name);
        reference.setRefId(item.getIdentifier());
        return reference;
    }

    private void getDesignDescriptors(JsonNode study) throws ObjectStoreException {
        JsonNode sddNode = study.path("studyDesignDescriptors");
        for (JsonNode sdd : sddNode) {

            // storing in studyData
            // TODO: - decide if to add collection to study
            //       - decide what to do of the ontology ref

            Term term = new Term(sdd).invoke();
            String termId = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.warn("SDD: " + annotationValue);

            Item sddItem = createStudyData("descriptor", annotationValue, "", "study");
            Integer sddoid = store(sddItem);
            store(studyReference, sddoid);
        }
    }


    private void getMaterials(JsonNode study) throws ObjectStoreException {
        JsonNode sourceNode = study.path("materials").get("sources");
        for (JsonNode source : sourceNode) {
            LOG.warn("IN Sources...");
            getSource(source);
        }

        JsonNode sampleNode = study.path("materials").get("samples");
        for (JsonNode sample : sampleNode) {
            LOG.warn("IN Samples...");
            getSample(sample);
        }
    }

    private void getAssays(JsonNode study) {
        JsonNode assayNode = study.path("assays");
        for (JsonNode assay : assayNode) {
            String fileName = assay.path("filename").asText();
            String technologyPlatform = assay.path("technologyPlatform").asText();

            LOG.warn("ASSAY " + fileName + " on: " + technologyPlatform);

            // GET characteristicCategories
            JsonNode characteristicCategoriesNode = assay.get("characteristicCategories");

            for (JsonNode inode : characteristicCategoriesNode) {
                String characteristicCategoryId = inode.path("@id").asText(); // prob not useful

                Term term = new Term(inode.path("characteristicType")).invoke();
                String id = term.getId();
                String annotationValue = term.getAnnotationValue();
                String termAccession = term.getTermAccession();
                String termSource = term.getTermSource();

                LOG.info("CHAR " + id + ": " + annotationValue + "|" + termAccession + "|" + termSource);
            }

            // GET measurementType
            JsonNode measurementTypeNode = assay.get("measurementType");

            Term term = new Term(measurementTypeNode).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("MT " + id + ": " + annotationValue + "|" + termAccession + "|" + termSource);


            // GET datafiles
            JsonNode dataFilesNode = assay.get("dataFiles");

            for (JsonNode inode : dataFilesNode) {

                DataFile file = new DataFile(inode).invoke();
                String fileId = file.getId();
                String name = file.getName();
                String type = file.getType();

                LOG.info("FILE " + fileId + ": " + type + "|" + name);
            }

        }
    }


    private void getSource(JsonNode source) throws ObjectStoreException {
        String sourceName = source.path("name").asText();
        String sourceId = source.path("@id").asText();

        storeSource(sourceName);

        Integer cSize = source.path("characteristics").size();
        LOG.warn("SOURCE " + sourceId + ": " + sourceName + " with " + cSize + " characteristics");

        JsonNode characteristicNode = source.path("characteristics");
        for (JsonNode characteristic : characteristicNode) { // TODO: que pasa?
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


    private void getSample(JsonNode source) throws ObjectStoreException {
        String name = source.path("name").asText();
        String sourceId = source.path("@id").asText();

        Integer cSize = source.path("characteristics").size();
        LOG.warn("SAMPLE " + sourceId + ": " + name + " with " + cSize + " characteristics");

        getSampleFactors(source.path("factorValues"), name);

        store (studyReference, store(createStudyData (name, "","", "sample")));

        // empty for sample! is it general?
        JsonNode characteristicNode = source.path("characteristics");
        for (JsonNode characteristic : characteristicNode) {
            String categoryId = characteristic.path("category").get("@id").asText();

            JsonNode value = characteristic.path("value");
            Term term = new Term(value).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("SAMPLECHAR " + categoryId + ": " + id + "|" + annotationValue + "|"
                    + termAccession + "|" + termSource);


        }
    }


    private void storeSource(String sourceName) throws ObjectStoreException {
        Item sdItem = createStudyData(sourceName);

        LOG.warn("STORING SOURCE " + sourceName);
        Integer sdoid = store(sdItem);
        store(studyReference, sdoid);
    }

    private void storeSample(String sourceName) throws ObjectStoreException {
        Item sdItem = createStudyData(sourceName);

        Integer sdoid = store(sdItem);
        store(studyReference, sdoid);
    }


    private void getSampleFactors(JsonNode source, String sampleName) throws ObjectStoreException {
        //
        // storing these as study data, with reference to factor
        //
        for (JsonNode characteristic : source) {
            String categoryId = blunt(characteristic.path("category").get("@id").asText());

            JsonNode value = characteristic.path("value");
            Term term = new Term(value).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            // check if in factors, add if not, get item_id and add ref to createSD
            //Item studyFactor = createFactor(id, categoryId, annotationValue, "sample", "", "", "");

            //Item sampleFactor = createStudyData(id, categoryId, annotationValue, "sample", "", "", "");

            LOG.info("CHAR " + categoryId + ": " + "id" + annotationValue + "|"
                    + termAccession + "|" + termSource);
            LOG.warn("FLIST " + factorRefs.toString() + "|" + factors.size());

            // name should come from the study factor

            Item item = createStudyData(sampleName, annotationValue,"","factor");
            LOG.warn("FV CAT ID " + categoryId);
            LOG.warn("F ITEM " + factors.get(categoryId).getIdentifier());
            Reference factorRef = getReference("factor", factors.get(categoryId));
            item.addReference(factorRef);

//            Reference factorRef = getReference("factor", studyFactor);
//            item.addReference(factorRef);

            LOG.warn("REFF " + factorRef.getRefId());
 //           Integer foid = store(item);
            store(studyReference, store(item));
        }
    }


    private void getOntologies(JsonNode osr) throws ObjectStoreException {
        for (JsonNode node : osr) {
            String name = node.path("name").asText();
            String description = node.path("description").asText();
            String filename = node.path("file").asText();
            String version = node.path("version").asText();

            store(createOntology(description, filename, name, version));

            LOG.warn("OSR " + name);
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


    private Item createStudy(String type, String id, String title, String description, String subDate, String pubDate)
            throws ObjectStoreException {

        Item item = createItem(type);
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
            item.setAttribute("publicReleaseDate", pubDate);
        }
        return item;
    }



    private Item createFactor(String fid, String cid, String name, String type, String value, String unit, String accession)
            throws ObjectStoreException {
        Item item = factors.get(fid);
        if (item == null) {
            item = createItem("Factor");
            item.setAttribute("name", name);
            if (!type.isEmpty()) {
                item.setAttribute("type", type);
            }
            if (!value.isEmpty()) {
                item.setAttribute("value", value);
            }
            if (!unit.isEmpty()) {
                item.setAttribute("unit", unit);
            }
            if (!accession.isEmpty()) {
                item.setAttribute("accession", accession);
            }
            factors.put(fid, item);
        }
        return item;
    }

// torm
    private Item createFactor(String name, String value, String unit, String type)
            throws ObjectStoreException {

        Item item = createItem("Factor");
        item.setAttribute("name", name);

        if (!value.isEmpty()) {
            item.setAttribute("value", value);
        }
        // gives NPE ! change contract?
//        if (!unit.isEmpty()) {
//            item.setAttribute("unit", unit);
//        }
//        if (!type.isEmpty()) {
//            item.setAttribute("type", type);
//        }
        return item;
    }

    private Item createStudyData(String name, String value, String unit, String type)
            throws ObjectStoreException {

        Item item = createItem("StudyData");
        item.setAttribute("name", name);

        if (!value.isEmpty()) {
            item.setAttribute("value", value);
        }
        // gives NPE if i pass null.. change contract?
        if (!unit.isEmpty()) {
            item.setAttribute("unit", unit);
        }
        if (!type.isEmpty()) {
            item.setAttribute("type", type);
        }
        return item;
    }

    private Item createStudyData(String source)
            throws ObjectStoreException {

        Item item = createItem("StudyData");
        item.setAttribute("source", source);

        return item;
    }

    private Item createOntology(String name, String url, String shortName, String version)
            throws ObjectStoreException {

        Item item = createItem("Ontology");
        item.setAttribute("name", name);
        item.setAttribute("url", url);
        item.setAttribute("shortName", shortName);
        item.setAttribute("version", version);

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


    private String blunt(String in) {
        return in.replaceAll("#", "");
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




}
