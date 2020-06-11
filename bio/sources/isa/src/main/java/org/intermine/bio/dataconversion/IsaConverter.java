package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.ArrayList;
import java.util.Set;


/**
 * @author sc
 */
public class IsaConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(IsaConverter.class);
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();

    private static final String SOURCE = "source";
    //private static final String SAMPLE = "sample";

    private static final List<String> MATERIALS =
            Collections.unmodifiableList(Arrays.asList("sources", "samples"));


    private Map<String, Map> people;
    private Map<String, Map> publications;
    private Map<String, Map> comments;  // add field for ref?

    private Map<String, Item> protocols = new HashMap<>();
    private Map<String, Item> protocolParameters = new HashMap<>();
    // prot.id - list of param ids
    private Map<String, List<String>> protocolParameterList = new HashMap<>();
    // prot.id - list of input ids (sources/study data)
    private Map<String, List<String>> protocolIn = new HashMap<>();
    // prot.id - list of output ids (samples/sd)
    private Map<String, List<String>> protocolOut = new HashMap<>();

    private Map<String, String> sdItemId = new HashMap<>();

    private Set<String> taxonIds;

    private Map<String, Item> proteins = new HashMap<>(); // not used

    private Map<String, Item> factors = new HashMap<>();
    //private Map<String, List<String>> factorRefs = new HashMap<>();

    private Reference investigationReference;
    private Reference studyReference;


    /**
     * Constructor
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model  the Model
     */
    public IsaConverter(ItemWriter writer, Model model) {
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
     *///            getCharacteristicCategories(study);


    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        // TODO: decide if to use blunt ids or not

        //File file = getFiles();

        JsonNode root = new ObjectMapper().readTree(reader);
        //otherAccess(root);

        processInvestigation(root);

        JsonNode ontologyNode = root.path("ontologySourceReferences");
        getOntologies(ontologyNode);  // TODO: make a map to use for referencing terms?

        JsonNode studyNode = root.path("studies");
        for (JsonNode study : studyNode) {

            clearMaps();

            String identifier = study.path("identifier").asText();
            String title = study.path("title").asText();
            String description = study.path("description").asText();
            String filename = study.path("filename").asText();
            String subDate = study.path("submissionDate").asText();
            String pubDate = study.path("publicReleaseDate").asText();


            LOG.info("STUDY " + identifier);
            //LOG.warn(title + " -- " + filename + " | " + subDate);

            Item studyItem = createStudy("Study", identifier, title, description, pubDate, subDate);
            // store and add ref to investigation
            store(investigationReference, store(studyItem));
            // get study reference, used for collections attached to study
            studyReference = getReference("study", studyItem);

            getDesignDescriptors(study);

            getProcess(study); //run before materials/protocols! (TBC)

            getFactors(study, "factors");
            getFactors(study, "characteristicCategories");

            getProtocols(study);
            getMaterials(study);
            getAssays(study);

            storeProtocols();

            //TODO
//            getPublications(study); investigation?
//            getPeople(study); inv?
//            getUnitCategories(study);
//            getComments(study);
            //getProcess
        }

        //createInvestigationWithPojo(file);
    }

    private void clearMaps() {
        protocols.clear();
        protocolParameters.clear();
        protocolParameterList.clear();
        protocolIn.clear();
        protocolOut.clear();
        sdItemId.clear();
        factors.clear();
    }


    private void processInvestigation(JsonNode investigation) throws ObjectStoreException {

        String identifier = investigation.path("identifier").asText();
        String title = investigation.path("title").asText();
        String description = investigation.path("description").asText();
        String pubDate = investigation.path("publicReleaseDate").asText();
        String subDate = investigation.path("submissionDate").asText();

        Item investigationItem = createStudy("Investigation", identifier, title,
                description, pubDate, subDate);
        investigationReference = getReference("investigation", investigationItem);

        Integer investigationOID = store(investigationItem);
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

                LOG.info("PPAR " + pnid + ": " + annotationValue
                        + "|" + termAccession + "|" + termSource);

                createProtocolParameter(pid, annotationValue);
                addToMap(protocolParameterList, id, pid);
            }
        }
    }


    private void getProcess(JsonNode study) throws ObjectStoreException {
        JsonNode processNode = study.path("processSequence");
        for (JsonNode process : processNode) {

            String id = blunt(process.path("@id").asText());
            String name = process.path("name").asText();
            String performer = process.path("performer").asText();
            String date = process.path("date").asText();

            // get also protocolType (term)..
            String pid = blunt(process.path("executesProtocol").get("@id").textValue());

            JsonNode inputNode = process.path("inputs");
            for (JsonNode in : inputNode) {
                String inpid = blunt(in.path("@id").asText());
                addToMap(protocolIn, pid, inpid);
            }

            JsonNode outputNode = process.path("outputs");
            for (JsonNode out : outputNode) {
                String outpid = blunt(out.path("@id").asText());
                addToMap(protocolOut, pid, outpid);
            }

            // TODO? add parameterValues
        }
        LOG.debug("SP: " + protocolIn.toString() + "->" + protocolOut.toString());
    }


    private void getFactors(JsonNode study, String path) throws ObjectStoreException {
        JsonNode factorNode = study.path(path);
        for (JsonNode factor : factorNode) {
            String nodeType;

            if ("factors".equalsIgnoreCase(path)) {
                nodeType = "factor";
            } else {
                nodeType = "characteristic";
            }

            String id = blunt(factor.path("@id").asText());
            String name;

            Term term = new Term(factor.path(nodeType.concat("Type"))).invoke();
            String termId = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            if ("factor".equalsIgnoreCase(nodeType)) {
                name = factor.path("factorName").asText();
            } else {
                name = annotationValue;
            }

            LOG.info("FACTOR " + nodeType + ": " + name + "|" + annotationValue);
            Item factorItem = createFactor(id, "", name, nodeType, annotationValue,
                    "", termAccession);
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

            Item sddItem = createStudyData("study", "descriptor", annotationValue, "");
            Integer sddoid = store(sddItem);
            store(studyReference, sddoid);
        }
    }


    private void getMaterials(JsonNode study) throws ObjectStoreException {
        // processes sources and samples
        for (String mat : MATERIALS) {
            JsonNode sourceNode = study.path("materials").get(mat);
            for (JsonNode source : sourceNode) {
                //LOG.warn("IN " + mat.toUpperCase() + "...");
                getMaterials(source, mat);
            }
        }
    }

    private void getAssays(JsonNode study) throws ObjectStoreException {
        JsonNode assayNode = study.path("assays");
        for (JsonNode assay : assayNode) {

            // for the moment (using MTBLS45)
            // creating a studydata with
            //
            // type = "file"
            // name = filename
            // value = cc.annotationValue (e.g. label)
            // technology = technologyPlatform
            // measurament = mt.annotationValue
            //
            // TODO: add ontology ref (measurament)

            String fileName = assay.path("filename").asText();
            String technologyPlatform = assay.path("technologyPlatform").asText();

            LOG.warn("ASSAY " + fileName + " on: " + technologyPlatform);
            String mtValue = null;
            // GET characteristicCategories
            JsonNode characteristicCategoriesNode = assay.get("characteristicCategories");

            // rm loop?
            for (JsonNode inode : characteristicCategoriesNode) {
                String characteristicCategoryId = inode.path("@id").asText(); // prob not useful

                Term term = new Term(inode.path("characteristicType")).invoke();
                String id = term.getId();
                mtValue = term.getAnnotationValue();
                String termAccession = term.getTermAccession();
                String termSource = term.getTermSource();

                LOG.info("CHAR " + id + ": " + mtValue + "|" + termAccession + "|" + termSource);
            }

            // GET measurementType
            JsonNode measurementTypeNode = assay.get("measurementType");

            Term term = new Term(measurementTypeNode).invoke();
            String id = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("MT " + id + ": " + annotationValue + "|" + termAccession + "|" + termSource);


            Item sdItem = createStudyData("file", fileName, mtValue, annotationValue,
                    technologyPlatform);
            Reference sdRef = getReference("studyData", sdItem);
            store(studyReference, store(sdItem));


            // GET datafiles
            JsonNode dataFilesNode = assay.get("dataFiles");

            for (JsonNode inode : dataFilesNode) {

                DataFile file = new DataFile(inode).invoke();
                String fileId = file.getId();
                String name = file.getName();
                String type = file.getType();

                LOG.info("FILE " + fileId);

                Item item = createDataFile(type, name);
                item.addReference(sdRef);
                store(studyReference, store(item));
            }
        }
    }


    private void getMaterials(JsonNode source, String types) throws ObjectStoreException {
        String name = source.path("name").asText();
        String id = blunt(source.path("@id").asText()); // -> e.g. sample/140116434162296

        // remove the s (plural -> singular..)
        String type = types.substring(0, types.length() - 1);

        String innerLevel;
        if (type.equalsIgnoreCase(SOURCE)) {
            innerLevel = "characteristics";
        } else {
            innerLevel = "factorValues";
        }

        getSampleFactors(source.path(innerLevel), name, id, type); // characteristics for SOURCES
        Integer cSize = source.path(innerLevel).size();
        LOG.warn(type.toUpperCase() + " " + id + ": " + name + " with " + cSize + " " + innerLevel);

        // empty for sample! is it general?
        JsonNode characteristicNode = source.path("characteristics");
        for (JsonNode characteristic : characteristicNode) {
            String categoryId = characteristic.path("category").get("@id").asText();

            JsonNode value = characteristic.path("value");
            Term term = new Term(value).invoke();
            String charId = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            LOG.info("CHAR " + categoryId + ": " + charId + "|" + annotationValue + "|"
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


    private void getSampleFactors(JsonNode source, String sampleName, String id, String type)
            throws ObjectStoreException {
        //
        // storing these as study data, with reference to factor
        //
        boolean found = false;
        for (JsonNode characteristic : source) {
            String categoryId = blunt(characteristic.path("category").get("@id").asText());

            JsonNode value = characteristic.path("value");
            Term term = new Term(value).invoke();
            String charId = term.getId();
            String annotationValue = term.getAnnotationValue();
            String termAccession = term.getTermAccession();
            String termSource = term.getTermSource();

            // alt: check if in factors, add if not, get item_id and add ref to createSD
            // name should come from the study factor?

            Item item = createStudyData(type, sampleName, annotationValue, "");
            Reference factorRef = getReference("factor", factors.get(categoryId));
            item.addReference(factorRef);

            // add to map of id-item id for refs
            sdItemId.put(id, item.getIdentifier());

            store(studyReference, store(item));
            found = true;
        }
        if (!found) {
            store(studyReference, store(createStudyData(type, sampleName, "", "")));
        }
    }


    private void getOntologies(JsonNode osr) throws ObjectStoreException {
        for (JsonNode node : osr) {
            String name = node.path("name").asText();
            String description = node.path("description").asText();
            String filename = node.path("file").asText();
            String version = node.path("version").asText();

            store(createOntology(description, filename, name, version));

            LOG.info("OSR " + name);
        }
    }


    private File getFiles() throws FileNotFoundException {
        File file = getCurrentFile();
        if (file == null) {
            throw new FileNotFoundException("No valid data files found.");
        }
        LOG.info("====================================================================");
        LOG.info("        ISA: Reading " + file.getName());
        LOG.info("====================================================================");
        return file;
    }


    /**
     * {@inheritDoc}
     */
    public void storeProtocols() throws ObjectStoreException {
        // to move with protocols probably, in line with other cases
        for (Map.Entry<String, Item> entry : protocols.entrySet()) {

            // store protocols
            Integer protocoloid = store(entry.getValue());
            store(studyReference, protocoloid);

            String pid = entry.getKey();
            LOG.warn("STORING " + pid + " (" + protocoloid + ")");
            // store protocol parameters
            List<String> pparid = protocolParameterList.get(pid);
            // sometime no parameters (TO CHECK)
            if (pparid != null) {
                for (String ppid : pparid) {

                    LOG.warn("STORE par " + ppid);
                    Reference reference = new Reference();
                    reference.setName("protocol");
                    reference.setRefId(entry.getValue().getIdentifier());

                    Integer ppoid = store(protocolParameters.get(ppid));
                    store(reference, ppoid);
                }
            }
            // store references to inputs
            Iterator<String> pin = protocolIn.keySet().iterator();
            while (pin.hasNext()) {
                String thisProtId = pin.next();
                List<String> ins = protocolIn.get(thisProtId);

                ReferenceList inputCollection = new ReferenceList();
                inputCollection.setName("inputs");
                for (String in : ins) {
                    inputCollection.addRefId(sdItemId.get(in));
                }
                store(inputCollection, protocoloid);
            }

            // store references to outputs
            Iterator<String> pout = protocolOut.keySet().iterator();
            while (pout.hasNext()) {
                String thisProtId = pout.next();
                List<String> outs = protocolOut.get(thisProtId);

                //LOG.info("OUTS are " + outs.size());
                ReferenceList outputCollection = new ReferenceList();
                outputCollection.setName("outputs");
                for (String out : outs) {
                    //LOG.info("REF " + out + " -> " + sdItemId.get(out));
                    if (sdItemId.get(out) == null) {
                        // not sure why this should be, TOCHECK
                        continue;
                    }
                    outputCollection.addRefId(sdItemId.get(out));
                }
                store(outputCollection, protocoloid);
            }
        }

//        for (Item item : protocols.values()) {
//            Integer protocoloid = store(item);
//
//        }
    }


//    long bT = System.currentTimeMillis();     // to monitor time spent in the process
//        LOG.info("TIME setting submission-protocol references: "
//                + (System.currentTimeMillis() - bT) + " ms");


    private Item createStudy(String type, String id, String title, String description,
                             String subDate, String pubDate)
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
            setDate(subDate, item);
        }
        if (!pubDate.isEmpty()) {
            setDate(pubDate, item);
        }
        return item;
    }

    private void setDate(String date, Item item) {
        if (date.contains("/")) {
            item.setAttribute("submissionDate", reformDate(date));
        } else {
            item.setAttribute("submissionDate", date);
        }
    }

    private String reformDate(String subDate) {
        // to change from dd/mm/yyyy to yyyy-mm-dd
        // which is understood by integration.
        DateFormat readFormat = new SimpleDateFormat("dd/mm/yyyy");
        DateFormat writeFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = readFormat.parse(subDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String formattedDate = "";
        if (date != null) {
            formattedDate = writeFormat.format(date);
        }
        return formattedDate;
    }


    private Item createFactor(String fid, String cid, String name, String type, String value,
                              String unit, String accession)
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


    private Item createDataFile(String type, String name)
            throws ObjectStoreException {

        Item item = createItem("DataFile");
        item.setAttribute("type", type);

        if (!name.isEmpty()) {
            item.setAttribute("name", name);
        }
        return item;
    }


    private Item createStudyData(String type, String name, String value, String unit)
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

    private Item createStudyData(String type, String name, String value, String measurement,
                                 String technology)
            throws ObjectStoreException {

        Item item = createItem("StudyData");
        item.setAttribute("name", name);

        if (!type.isEmpty()) {
            item.setAttribute("type", type);
        }
        if (!value.isEmpty()) {
            item.setAttribute("value", value);
        }
        if (!measurement.isEmpty()) {
            item.setAttribute("measurement", measurement);
        }
        if (!technology.isEmpty()) {
            item.setAttribute("technology", technology);
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
        if (!url.isEmpty()) {
            item.setAttribute("url", url);
        }
        item.setAttribute("shortName", shortName);
        item.setAttribute("version", version);

        return item;
    }


    private Item createProtocol(String id, String name, String description, String uri,
                                String version)
            throws ObjectStoreException {
        Item item = protocols.get(id);
        if (item == null) {
            item = createItem("Protocol");
            item.setAttribute("name", name);
            if (!description.isEmpty()) {
                item.setAttribute("description", description);
            }
            if (!uri.isEmpty()) {
                item.setAttribute("URI", uri);
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


    private class Term
    {
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

    private class DataFile
    {
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
}
