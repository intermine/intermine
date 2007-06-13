package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ReferenceList;

import java.io.Reader;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse psi data into items
 * @author Julie Sullivan
 */
public class PsiConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final Logger LOG = Logger.getLogger(PsiConverter.class);
    private Map<String, Integer> ids = new HashMap<String, Integer>();
    private Map<String, String> aliases = new HashMap<String, String>();
    private Map<String, Map> mapMaster = new HashMap<String, Map>();  // map of maps
    private Map<String, String> organisms = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private LinkedHashMap<String, String> proteinAccessions = new LinkedHashMap<String, String>();
    private Map<String, Object> experimentNames = new HashMap<String, Object>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, String> masterList = new HashMap<String, String>();
    //private int nextClsId;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public PsiConverter(ItemWriter writer) {
        super(writer);
    }


    /**
     * A space separated list of of NCBI taxonomy ids for which we want to retrieve
     * interactions.
     * @param orgStr a list of taxon ids
     */
    public void setOrganisms(String orgStr) {
        List<String> orgArray = Arrays.asList(orgStr.split("\\s"));
        for (Iterator iter = orgArray.iterator(); iter.hasNext();) {
            String taxId = (String) iter.next();
            organisms.put(taxId, null);
        }
    }


    // master map of all maps used across XML files
    private void mapMaps() {
        mapMaster.put("ids", ids);
        mapMaster.put("aliases", aliases);
        mapMaster.put("organisms", organisms);
        mapMaster.put("publications", pubs);
        mapMaster.put("experimentNames", experimentNames);
        mapMaster.put("proteinAccessions", proteinAccessions);
        mapMaster.put("terms", terms);
        mapMaster.put("masterList", masterList);
    }


    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {

        mapMaps();
        PsiHandler handler = new PsiHandler(getItemWriter(), mapMaster);

        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles xml file
     */
    static class PsiHandler extends DefaultHandler
    {
        //private int nextClsId = 0;
        private ItemFactory itemFactory;
        private Map<String, Integer> ids;
        private Map<String, String> aliases;
        private Map<String, String> pubs;
        private Map<String, String> validProteins = new HashMap<String, String>();
        private Map<String, String> proteinAccessions = new HashMap<String, String>();
        private Map<String, String> organisms = new HashMap<String, String>();
        private Map<String, String> terms = new HashMap<String, String>();
        private Map<String, String> masterList = new HashMap<String, String>();
        // [experiment name] [experiment holder]
        private Map<String, ExperimentHolder> experimentNames;  // keeps names until experiment is
                                                                // stored
        // [id][experimentholder]1
        private Map<String, ExperimentHolder> experimentIds     // keeps names for this file only
                                                        = new HashMap<String, ExperimentHolder>();
        private InteractorHolder interactorHolder;
        private ItemWriter writer;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        private InteractionHolder holder;
        private ExperimentHolder experimentHolder;
        private Item protein, sequence;
        private Item comment;
        //private Item dataset;
        private String datasourceItemId;
        private ReferenceList commentCollection;
        private String proteinId; // id used in xml to refer to protein
        private Set<Item> synonyms;
        private String psiDagTermItemId;
        private String experimentId;

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param orgs List of organisms for which we want to retrieve interactions.
         * @param psiDagTerm dag term for all interactions, always binding site
         * @param datasource datasource for these XML files, always intact
         * @param mapMaster master map of all maps used across XML files
         */
        public PsiHandler(ItemWriter writer, Map mapMaster) {

            itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
            this.writer = writer;
            this.ids = (Map) mapMaster.get("ids");
            this.aliases = (Map) mapMaster.get("aliases");
            this.organisms = (Map) mapMaster.get("organisms");
            this.pubs = (Map) mapMaster.get("publications");
            this.proteinAccessions = (Map) mapMaster.get("proteinAccessions");
            this.experimentNames = (Map) mapMaster.get("experimentNames");
            this.terms = (Map) mapMaster.get("terms");
            this.masterList = (Map) mapMaster.get("masterList");
            if (masterList.size() > 1) {
                this.psiDagTermItemId = masterList.get("psiDagTerm");
                this.datasourceItemId = masterList.get("datasource");
            }
            //nextClsId = mastproteinAccessionserList.get("nextClsId");
            validProteins = new HashMap<String, String>();
        }


        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
            attName = null;

                 // <experimentList><experimentDescription>
                 if (qName.equals("experimentDescription")) {

                    // this experiment may have already been created
                    // we won't know until the name is processed
                    try {
                        experimentId = attrs.getValue("id");
                    } catch (Exception e) {
                        throw new SAXException(e);
                    }

                //  <experimentList><experimentDescription id="2"><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.peek().equals("names")
                                && stack.search("experimentDescription") == 2) {

                    /* experiment.experimentName = shortName */
                    attName = "experimentName";

                //  <experimentList><experimentDescription id="2"><names><fullName>
                } else if (qName.equals("fullName")
                                && stack.peek().equals("names")
                                && stack.search("experimentDescription") == 2) {

                    /* dataset.description = fullName */
                    attName = "experimentFullName";

                //<experimentList><experimentDescription><bibref><xref><primaryRef>
                } else if (qName.equals("primaryRef")
                                && stack.peek().equals("xref")
                                && stack.search("bibref") == 2
                                && stack.search("experimentDescription") == 3) {

                    String pubMedId = attrs.getValue("id");
                    if (StringUtil.allDigits(pubMedId)) {
                        String pub = getPub(pubMedId);
                        experimentHolder.setPublication(pub);
                    }

                //<experimentList><experimentDescription><attributeList><attribute>
                } else if (qName.equals("attribute")
                                && stack.peek().equals("attributeList")
                                && stack.search("experimentDescription") == 2) {

                    String name = attrs.getValue("name");
                    if (experimentHolder.experiment != null && name != null) {
                        comment = createItem("Comment");
                        setComment();
                        comment.setAttribute("type", name);
//                        String title = experimentHolder.name;
//                        Item item = getInfoSource(title);
//                        comment.setReference("source", item.getIdentifier());
                        attName = "experimentAttribute";
                    } else {
                        LOG.info("Can't create comment, bad experiment.");
                    }

                // <hostOrganismList><hostOrganism ncbiTaxId="9534"><names><fullName>
                } else if (qName.equals("fullName")
                                && stack.peek().equals("names")
                                && stack.search("hostOrganism") == 3) {

                    attName = "hostOrganismName";

                //<interactionDetectionMethod><xref><primaryRef>
                } else if (qName.equals("primaryRef")
                                && stack.peek().equals("xref")
                                && stack.search("interactionDetectionMethod") == 2) {

                    String id = attrs.getValue("id");
                    String termItemId = getTerm(id);
                    experimentHolder.setMethod("interactionDetectionMethod", termItemId);


                //<participantIdentificationMethod><xref> <primaryRef>
                } else if (qName.equals("primaryRef")
                                && stack.peek().equals("xref")
                                && stack.search("participantIdentificationMethod") == 2) {
                    String id = attrs.getValue("id");
                    String termItemId = getTerm(id);
                    experimentHolder.setMethod("participantIdentificationMethod", termItemId);


                // <interactorList><interactor id="4">
                } else if (qName.equals("interactor")
                                && stack.peek().equals("interactorList")) {

                    proteinId = attrs.getValue("id");

                // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
                } else if (qName.equals("organism")
                                && stack.peek().equals("interactor")) {

                    /* if organism is valid, put the protein in our list for later reference */
                    String taxId = attrs.getValue("ncbiTaxId");
                    if (organisms.containsKey(taxId)) {
                        if (protein != null) {
                            protein.setReference("organism", organisms.get(taxId));
                        }
                        if (!validProteins.containsKey(proteinId)) {
                            validProteins.put(proteinId, protein.getIdentifier());
                        }
                
                    }

                // <interactorList><interactor id="4"><xref><primaryRef>
                } else if (qName.equals("primaryRef")
                                && stack.peek().equals("xref")
                                && stack.search("interactor") == 2) {

                    String db = attrs.getValue("db");
                    String id = attrs.getValue("id");
                    synonyms = new HashSet<Item>();
                    if (db != null) {
                        String primaryAccession = null;
                        if (db.startsWith("uniprot")) {
                            // accessions like P14734-1 are isoform identifiers, remove the '-n'
                            // to get back to main protein id
                            if (id.indexOf("-") > 0) {
                                id = id.substring(0, id.indexOf("-"));
                            }
                            primaryAccession = id;
                        } else if (db.equals("intact")) {
                            primaryAccession = "IntAct:" + id;
                        } else {
                            primaryAccession = id;
                        }
                        
                        protein = createItem("Protein");
                        protein.setAttribute("primaryAccession", primaryAccession);
                        String proteinIdentifier = proteinAccessions.get(primaryAccession);
                        if (proteinIdentifier == null) {
                            // we have seen this before so set the correct identifier, this won't
                            // be stored this time.
                            proteinIdentifier = protein.getIdentifier();
                        } else {
                            protein.setIdentifier(proteinIdentifier);
                        }
                        
                        // TODO this should maybe create a data source for each db
                        
                        Item synonym = createItem("Synonym");
                        synonym.setAttribute("id", id);
                        synonym.setAttribute("type", db.startsWith("uniprot") 
                                             ? "accession" : "identifier");
                        synonym.setReference("source", datasourceItemId);
                        synonym.setReference("subject", proteinIdentifier);
                        synonyms.add(synonym);
                        
                        // create an extra synonym for proteins that have an IntAct identifier
                        if (db.equals("intact")) {
                            synonym = createItem("Synonym");
                            synonym.setAttribute("id", id);
                            synonym.setAttribute("type", "identifier");
                            synonym.setReference("source", datasourceItemId);
                            synonym.setReference("subject", proteinIdentifier);
                            synonyms.add(synonym);
                        }
                    }

                // <interactorList><interactor id="4"><sequence>
                } else if (qName.equals("sequence")
                                && stack.peek().equals("interactor")) {

                    attName = "sequence";

                //<interactionList><interaction id="1"><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.peek().equals("names")
                                && stack.search("interaction") == 2) {

                    attName = "interactionName";

                //<interaction><confidenceList><confidence><unit><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.peek().equals("names")
                                && stack.search("confidence") == 3) {

                    attName = "confidenceUnit";

                //<interactionList><interaction><confidenceList><confidence><value>
                } else if (qName.equals("value")
                                && stack.peek().equals("confidence")) {

                    attName = "confidence";

                //<interactionList><interaction>
                //<participantList><participant id="5"><interactorRef>
                } else if (qName.equals("interactorRef")
                                && stack.peek().equals("participant")) {

                    attName = "participantId";

                // <participantList><participant id="5"><biologicalRole><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("biologicalRole") == 2) {

                    attName = "biologicalRole";

                // <participantList><participant id="5"><experimentalRole><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("experimentalRole") == 2) {

                    attName = "experimentalRole";

                //<interactionList><interaction><experimentList><experimentRef>
                } else if (qName.equals("experimentRef")
                                && stack.peek().equals("experimentList")) {
                    attName = "experimentRef";

                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><startStatus><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("startStatus") == 2) {

                    attName = "startStatus";

                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><endStatus><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("endStatus") == 2) {

                    attName = "endStatus";

                // <participantList><participant id="6919"><featureList><feature id="6920">
                // <featureType><xref><primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0117"
                } else if (qName.equals("primaryRef")
                                && stack.search("featureType") == 2
                                && attrs.getValue("id").equals("MI:0117")
                                && interactorHolder != null) {

                    interactorHolder.isRegionFeature = true;

                    // create interacting region
                    Item interactionRegion = createItem("ProteinInteractionRegion");
                    interactionRegion.setReference("protein", interactorHolder.proteinId);
                    interactionRegion.setReference("ontologyTerm", psiDagTermItemId);

                    // create new location object (start and end are coming later)
                    Item location = createItem("Location");
                    location.setReference("object", interactorHolder.proteinId);
                    location.setReference("subject", interactionRegion.getIdentifier());
                    
                    interactionRegion.setReference("location", location);
                    
                    // add location and region to interaction object
                    interactorHolder.setInteractionRegion(interactionRegion);
                    //interactorHolder.interactingRegion = interactingRegion;
                    interactorHolder.location = location;

                    holder.addRegion(interactionRegion.getIdentifier());

                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><begin position="470"/>
                } else if (qName.equals("begin")
                                && stack.peek().equals("featureRange")
                                && interactorHolder != null
                                && interactorHolder.isRegionFeature) {

                    interactorHolder.location.setAttribute("start", attrs.getValue("position"));

                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><end position="470"/>
                } else if (qName.equals("end")
                                && stack.peek().equals("featureRange")
                                && interactorHolder != null
                                && interactorHolder.isRegionFeature) {
                    interactorHolder.location.setAttribute("end", attrs.getValue("position"));
                // <entry>
                } else if (qName.equals("entry")) {
                    /* stuff done only once */
                     if (masterList.size() <= 1) {
                        init();
                    }
                }

            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void characters(char[] ch, int start, int length) {

            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (length > 0) {
                    boolean whitespace = false;
                    switch(ch[start]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        whitespace = true;
                        break;
                    default:
                        break;
                    }
                    if (!whitespace) {
                        break;
                    }
                    ++start;
                    --length;
                }

                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    attValue.append(s);
                }
            }
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
        throws SAXException {

            super.endElement(uri, localName, qName);

            stack.pop();
            try {
                // <experimentList><experimentDescription><attributeList><attribute/>
                // <attribute name="publication-year">2006</attribute>
                if (attName != null
                                && attName.equals("experimentAttribute")
                                && qName.equals("attribute")) {

                    String s = attValue.toString();
                    if (comment != null && s != null) {
                        //TODO store these only when valid experiment
                        comment.setAttribute("text", s);
                        writer.store(ItemHelper.convert(comment));
                        comment = null;
                    } else {
                        LOG.info("Experiment " + experimentHolder.name
                                 + " has a bad comment");
                    }

                // <experimentList><experimentDescription><names><shortLabel>
                } else if (attName != null
                                && attName.equals("experimentName")
                                && qName.equals("shortLabel")) {

                    /* experiment.experimentName = shortLabel */
                    String shortLabel = attValue.toString();
                    if (shortLabel != null) {

                        // you can have an experiment spread across several xml files
                        experimentHolder = checkExperiment(shortLabel);
                        experimentIds.put(experimentId, experimentHolder);
                        experimentHolder.setName(shortLabel);
                        Item dataset = createItem("DataSet");
                        dataset.setAttribute("title", shortLabel);
                        experimentHolder.setDataSet(dataset);
                    } else {
                        LOG.error("Experiment " + experimentId + " doesn't have a shortLabel");
                    }

                    // <experimentList><experimentDescription><names><fullName>
                } else if (attName != null
                                && attName.equals("experimentFullName")
                                && qName.equals("fullName")) {

                    String fullName = attValue.toString();
                    if (fullName != null & experimentHolder.getDataSet() != null) {
                        experimentHolder.getDataSet().setAttribute("description", fullName);
                        experimentHolder.getDataSet().setReference("dataSource", datasourceItemId);
                    } else {
                        LOG.info("Experiment doesn't have a fullName");
                    }

                    // <hostOrganismList><hostOrganism ncbiTaxId="9534"><names><fullName>
                } else if (attName != null
                                && attName.equals("hostOrganismName")
                                && qName.equals("fullName")) {

                    /* experiment.hostOrganism = fullName */
                    String fullName = attValue.toString();
                    if (fullName != null) {
                        experimentHolder.setHostOrganism(fullName);
                    } else {
                        LOG.info("Experiment " + experimentHolder.name
                                 + " doesn't have a host organism name");
                    }

                    // <interactorList><interactor id="4"><sequence>
                } else if (attName != null
                                && attName.equals("sequence")
                                && qName.equals("sequence")
                                && stack.peek().equals("interactor")) {

                    sequence = createItem("Sequence");
                    String srcResidues = attValue.toString();
                    sequence.setAttribute("residues", srcResidues);
                    sequence.setAttribute("length", "" + srcResidues.length());
                    protein.setReference("sequence", sequence);
                // <interactorList><interactor id="4">
                } else if (qName.equals("interactor")) {

                    String accession = protein.getAttribute("primaryAccession").getValue();
                    if (!proteinAccessions.containsKey(accession)) {
                        writer.store(ItemHelper.convert(protein));
                        proteinAccessions.put(protein.getAttribute("primaryAccession").getValue(),
                                              protein.getIdentifier());
                        for (Object ob : synonyms) {
                            writer.store(ItemHelper.convert((Item) ob));
                        }
                        if (sequence != null) {
                            writer.store(ItemHelper.convert(sequence));
                        }
                    }
                    protein = null;
                    proteinId = null;
                    synonyms = null;
                    sequence = null;

                    //<interactionList><interaction>
                    //<participantList><participant id="5"><interactorRef>
                } else if (qName.equals("interactorRef")
                                && stack.peek().equals("participant")) {

                    // get protein from our list, using the id as the key
                    String id = attValue.toString();
                    if (validProteins.get(id) != null) {
                        String proteinRefId = validProteins.get(id);
                        interactorHolder = new InteractorHolder(proteinRefId);
                        holder.addInteractor(interactorHolder);
                        holder.addProtein(proteinRefId);
                    } else {
                        holder.isValid = false;
                    }

                    // <interactionList><interaction><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && attName != null
                                && attName.equals("interactionName")) {

                    String shortLabel = attValue.toString();
                    holder = new InteractionHolder(shortLabel);

                    //<interactionList><interaction><experimentList><experimentRef>
                } else if (qName.equals("experimentRef")
                                && stack.peek().equals("experimentList")) {

                    String experimentRef = attValue.toString();
                    if (experimentIds.get(experimentRef) != null) {
                        holder.setExperiment(experimentIds.get(experimentRef));
                    } else {
                        LOG.error("Bad experiment:  [" + experimentRef + "] of "
                                  + experimentIds.size() + " experiments");
                    }

                    //<interaction><confidenceList><confidence><unit><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && attName != null
                                && attName.equals("confidenceUnit")
                                && holder != null) {

                    String shortLabel = attValue.toString();
                    if (shortLabel != null) {
                        holder.confidenceUnit = shortLabel;
                    }

                    //<interactionList><interaction><confidenceList><confidence><value>
                } else if (qName.equals("value")
                                && attName != null
                                && attName.equals("confidence")
                                && holder != null) {

                    if (holder.confidenceUnit.equals("author-confidence")) {
                        String value = attValue.toString();
                        holder.setConfidence(value);
                    }

                    // <interactionList><interaction><participantList><participant id="5">
                    // <biologicalRole><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("biologicalRole") == 2
                                && interactorHolder != null) {

                    interactorHolder.biologicalRole = attValue.toString();

                    // <interactionList><interaction><participantList><participant id="5">
                    // <biologicalRole><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("experimentalRole") == 2
                                && interactorHolder != null) {

                    interactorHolder.experimentalRole = attValue.toString();

                    // <participantList><participant id="6919"><featureList><feature id="6920">
                    //    <featureRangeList><featureRange><startStatus><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("startStatus") == 2
                                && interactorHolder != null
                                && interactorHolder.isRegionFeature) {

                    interactorHolder.startStatus = attValue.toString();

                    // <participantList><participant id="6919"><featureList><feature id="6920">
                    //    <featureRangeList><featureRange><endStatus><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && stack.search("endStatus") == 2
                                && interactorHolder != null
                                && interactorHolder.isRegionFeature) {

                    interactorHolder.endStatus = attValue.toString();

                //<interactionList><interaction>
                } else if (qName.equals("interaction")
                                && holder != null) {

                    /* done processing everything for this interaction */
                    if (holder.isValid) {
                        storeAll(holder);
                        holder = null;
                        interactorHolder = null;
                        //experimentHolder = null;
                    }
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private void storeAll(InteractionHolder interactionHolder) throws SAXException  {

            Set proteinInteractors = interactionHolder.interactors;

            try {
                // loop through proteins/interactors in this interaction
                for (Iterator iter = proteinInteractors.iterator(); iter.hasNext();) {

                    interactorHolder =  (InteractorHolder) iter.next();

                    // build & store interactions - one for each protein
                    Item interaction = createItem("ProteinInteraction");
                    String proteinRefId = interactorHolder.proteinId;
                    interaction.setAttribute("shortName", interactionHolder.shortName);
                    interaction.setAttribute("biologicalRole",
                                             interactorHolder.biologicalRole);
                    interaction.setAttribute("experimentalRole",
                                             interactorHolder.experimentalRole);
                    if (interactionHolder.confidence != null) {
                        interaction.setAttribute("confidence",
                                             interactionHolder.confidence.toString());
                    }
                    if (interactionHolder.confidenceText != null) {
                        interaction.setAttribute("confidenceText",
                                             interactionHolder.confidenceText);
                    }
                    interaction.setReference("protein", proteinRefId);
                    interaction.setReference("experiment",
                           interactionHolder.experimentHolder.experiment.getIdentifier());

                    interaction.addCollection(interactionHolder.getInteractingRegions());
                    // get all proteins for this interaction
                    Set<String> proteinIds = interactionHolder.proteinIds;

                    // remove this protein from the list.
                    proteinIds.remove(proteinRefId);
                    // make new collection
                    ReferenceList proteinList = new ReferenceList("interactingProteins",
                                                                  new ArrayList());
                    // add all other proteins to collection
                    for (Iterator it = proteinIds.iterator(); it.hasNext();) {
                        proteinList.addRefId((String) it.next());
                    }
                    // add reference
                    interaction.addCollection(proteinList);
                    proteinIds.add(proteinRefId);

                    /* store all protein interaction-related items */
                    writer.store(ItemHelper.convert(interaction));
                    if (interactorHolder.getInteractionRegion() != null) {
                        writer.store(ItemHelper.convert(interactorHolder.getInteractionRegion()));
                        writer.store(ItemHelper.convert(interactorHolder.location));
                    }
                }

                /* store all experiment-related items */
                ExperimentHolder eh = interactionHolder.experimentHolder;
                // TODO is this experiment going to have extra items to store, the 2nd time it
                // gets processed?  In the other XML files?
                if (!eh.isStored) {
                    eh.isStored = true;
                    writer.store(ItemHelper.convert(eh.experiment));
                    // TODO how can dataset be null?
                    if (eh.dataset != null) {
                        writer.store(ItemHelper.convert(eh.dataset));
                    }
                    //TODO store comments here instead
                    //for (Object o : eh.comments) {
                    //    writer.store(ItemHelper.convert((Item) o));
                    //}
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }

        private String getPub(String pubMedId)
        throws SAXException {
            String itemId = pubs.get(pubMedId);
            if (itemId == null) {
                try {
                    Item pub = createItem("Publication");
                    pub.setAttribute("pubMedId", pubMedId);
                    itemId = pub.getIdentifier();
                    pubs.put(pubMedId, itemId);
                    // TODO could have orphan pubs!
                    writer.store(ItemHelper.convert(pub));
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            return itemId;
        }

        private String getTerm(String identifier)
        throws SAXException {

            String itemId = terms.get(identifier);
            if (itemId == null) {
                try {
                    Item term = createItem("ProteinInteractionTerm");
                    term.setAttribute("identifier", identifier);
                    itemId = term.getIdentifier();
                    terms.put(identifier, itemId);
                    // TODO store sensibly
                    writer.store(ItemHelper.convert(term));
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            return itemId;
        }


        private ExperimentHolder checkExperiment(String name) {

            ExperimentHolder eh = experimentNames.get(name);
            if (eh == null) {
                Item exp = createItem("ProteinInteractionExperiment");
                eh = new ExperimentHolder(exp);
                commentCollection = new ReferenceList("comments", new ArrayList());
                experimentNames.put(name, eh);
            }
            return eh;
        }

        private void setComment() {
            experimentHolder.comments.add(comment);
            addToCollection(experimentHolder.experiment, commentCollection, comment);
        }

        private void addToCollection(Item parent, ReferenceList collection, Item newItem) {
            //TODO how can collection be null?
            if (collection != null) {
            if (collection.getRefIds().isEmpty()) {
                parent.addCollection(collection);
            }
            collection.addRefId(newItem.getIdentifier());
            }
        }


        private void init()
        throws SAXException {

                try {
                    // TODO why doesn't this call getTerm()???
                    Item psiDagTerm = createItem("OntologyTerm");
                    psiDagTermItemId = psiDagTerm.getIdentifier();
                    psiDagTerm.setAttribute("identifier", "MI:0117");
                    writer.store(ItemHelper.convert(psiDagTerm));
                    masterList.put("psiDagTerm", psiDagTermItemId);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                initOrganisms();
                getDatasource();

        }

        private void initOrganisms()
        throws SAXException {
            try {
                for (Iterator iter = organisms.keySet().iterator(); iter.hasNext();) {
                    String taxId = (String) iter.next();
                    Item organism = createItem("Organism");
                    organism.setAttribute("taxonId", taxId);
                    writer.store(ItemHelper.convert(organism));
                    organisms.put(taxId, organism.getIdentifier());
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }


        private void getDatasource()
        throws SAXException {

                Item datasource = createItem("DataSource");
                datasourceItemId = datasource.getIdentifier();
                try {
                    datasource.addAttribute(new Attribute("name", "IntAct"));
                    writer.store(ItemHelper.convert(datasource));
                    masterList.put("datasource", datasourceItemId);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }

        }

        /**
         * Convenience method for creating a new Item
         * @param className the name of the class
         * @return a new Item
         */
        protected Item createItem(String className) {
            return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                        GENOMIC_NS + className, "");
        }


        private String newId(String className) {



            Integer id = ids.get(className);
            if (id == null) {
                id = new Integer(0);
                ids.put(className, id);

            }

            id = new Integer(id.intValue() + 1);
            ids.put(className, id);
            return id.toString();
        }

        /**
         * Uniquely alias a className
         * @param className the class name
         * @return the alias
         */
        protected String alias(String className) {
            String alias = aliases.get(className);
            
            if (alias != null) {
                return alias;
            }
            String s = "0";
            if (masterList.get("nextClsId") != null) {
                s = masterList.get("nextClsId");
            }
            int i = Integer.parseInt(s);
            i++;
            String nextIndex = "" + i;
            masterList.put("nextClsId", nextIndex);
            aliases.put(className, nextIndex);
            return nextIndex;
        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about an interaction until
         * it's verified that all organisms are in the list given.
         * @author Julie Sullivan
         */
        public static class InteractionHolder
        {
            private String shortName;
            private ExperimentHolder experimentHolder;
            private Double confidence;
            private String confidenceText;
            private String confidenceUnit;
            private Set<InteractorHolder> interactors = new HashSet<InteractorHolder>();
            private boolean isValid = true;
            private Set<String> proteinIds = new HashSet<String>();
            private ReferenceList interactingRegions =
                new ReferenceList("interactingRegions", new ArrayList());

            /**
             * Constructor
             * @param shortName name of this interaction
             */
            public InteractionHolder(String shortName) {
                this.shortName = shortName;
            }

            protected void setExperiment(ExperimentHolder experimentHolder) {
                this.experimentHolder = experimentHolder;
            }

            protected void setConfidence(String confidence) {
                if (Character.isDigit(confidence.charAt(0))) {
                    this.confidence = new Double(confidence);
                } else {
                    // if confidencetext has a value, concatenate
                    confidenceText = (confidenceText != null
                                    ? confidenceText + confidence : confidence);
                }
            }

            protected void addInteractor(InteractorHolder interactorHolder) {
                interactors.add(interactorHolder);
            }

            protected void addProtein(String proteinId) {
                proteinIds.add(proteinId);
            }

            protected void addRegion(String id) {
                interactingRegions.addRefId(id);
            }

            protected ReferenceList getInteractingRegions() {
                return interactingRegions;
            }
        }



        /**
         * Holder object for ProteinInteraction.  Holds all information about an protein in an
         * interaction until it's verified that all organisms are in the list given.
         * @author Julie Sullivan
         */
        public static class InteractorHolder
        {
            private String proteinId;   // protein.getIdentifier()
            private String experimentalRole;
            private String biologicalRole;
            private Item interactionRegion;
            private Item location;
            private String startStatus;
            private String endStatus;

            /* we only want to process the binding site feature.  this flag is FALSE until
             *
             * <participantList><participant id="6919"><featureList>
             * <feature id="6920"><featureType><xref>
             * <primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0117"
             * id="MI:0117" (the id for binding site)
             *
             * then the flag is set to TRUE until </feature>
             */
            private boolean isRegionFeature;

            /**
             * Constructor
             * @param proteinId Protein that's part of the interaction
             */
            public InteractorHolder(String proteinId) {
                this.proteinId = proteinId;
            }

            protected void setInteractionRegion(Item interactionRegion) {
                this.interactionRegion = interactionRegion;
            }

            protected Item getInteractionRegion() {
                return interactionRegion;
            }
        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about an experiment until
         * an interaction is verified to have only valid organisms
         * @author Julie Sullivan
         */
        public static class ExperimentHolder
        {

            protected String name;
            protected Item experiment;
            //private String publication;
            //private Item interactionDetectionMethod;
            //private Item participantIdentificationMethod;
            protected Item dataset;
            protected HashSet comments = new HashSet(); // items to be stored
            // whether or not this item has been stored in the db yet
            protected boolean isStored = false; 

            /**
             * Constructor
             * @param experiment
             */
            public ExperimentHolder(Item experiment) {
                this.experiment = experiment;
            }

            protected void setName(String name) {
                experiment.setAttribute("name", name);
                this.name = name;
            }
            
            protected void setPublication(String publication) {
                //this.publication = publication;
                experiment.setReference("publication", publication);
            }

            protected void setMethod(String whichMethod, String termItemId) {
                experiment.setReference(whichMethod, termItemId);
            }

            protected void setHostOrganism(String fullName) {
                experiment.setAttribute("hostOrganism", fullName);
            }

            protected void setDataSet(Item dataset) {
                this.dataset = dataset;
            }

            protected Item getDataSet() {
                return dataset;
            }
        }
    }
}
