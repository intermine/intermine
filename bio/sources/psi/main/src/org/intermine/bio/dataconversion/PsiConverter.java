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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.util.Util;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse psi data into items
 *
 * Experiments and interactions appear in different files, so we have to keep all experiments
 * until all interactions are processed.
 *
 * @author Julie Sullivan
 */
public class PsiConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(PsiConverter.class);
    private static final String PROP_FILE = "psi-intact_config.properties";
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Object> experimentNames = new HashMap<String, Object>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, String> regions = new HashMap<String, String>();
    private String termId = null;
    protected IdResolverFactory flyResolverFactory;
    protected IdResolverFactory humanResolverFactory;
    private static final String INTERACTION_TYPE = "physical";
    private static final String ENSEMBL = "ensembl";
    private Map<String, String[]> config = new HashMap<String, String[]>();
    private Set<String> taxonIds = null;
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<MultiKey, Item> interactions = new HashMap<MultiKey, Item>();
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();
    private static final String ALIAS_TYPE = "gene name";
    private static final String SPOKE_MODEL = "prey";   // don't store if all roles prey
    private static final String DEFAULT_IDENTIFIER = "symbol";
    private static final String DEFAULT_DATASOURCE = "";
    private static final String BINDING_SITE = "MI:0117";
    private static final Set<String> INTERESTING_COMMENTS = new HashSet<String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PsiConverter(ItemWriter writer, Model model) {
        super(writer, model, "IntAct", "IntAct interactions data set");
        readConfig();
        // only construct factory here so can be replaced by mock factory in tests
        flyResolverFactory = new FlyBaseIdResolverFactory("gene");
        humanResolverFactory = new EnsemblIdResolverFactory();
        try {
            termId = getTerm(BINDING_SITE);
        } catch (SAXException e) {
            throw new RuntimeException("couldn't save ontology term");
        }
    }

    static {
        INTERESTING_COMMENTS.add("exp-modification");
        INTERESTING_COMMENTS.add("curation depth");
        INTERESTING_COMMENTS.add("library used");
        INTERESTING_COMMENTS.add("data-processing");
        INTERESTING_COMMENTS.add("comment");
        INTERESTING_COMMENTS.add("caution");
        INTERESTING_COMMENTS.add("last-imex assigned");
        INTERESTING_COMMENTS.add("imex-range assigned");
        INTERESTING_COMMENTS.add("imex-range requested");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        PsiHandler handler = new PsiHandler();
        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry: props.entrySet()) {

            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();

            String[] attributes = key.split("\\.");
            if (attributes.length == 0) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                                           + key);
            }
            String organism = attributes[0];

            if (config.get(organism) == null) {
                String[] configs = new String[2];
                configs[0] = DEFAULT_IDENTIFIER;
                configs[1] = DEFAULT_DATASOURCE;
                config.put(organism, configs);
            }
            if ("identifier".equals(attributes[1])) {
                config.get(organism)[0] = value;
            } else if ("datasource".equals(attributes[1])) {
                config.get(organism)[1] = value.toLowerCase();
            } else {
                String msg = "Problem processing properties '" + PROP_FILE + "' on line " + key
                    + ".  This line has not been processed.";
                LOG.error(msg);
            }
        }
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setIntactOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }

    /**
     * Handles xml file
     */
    class PsiHandler extends DefaultHandler
    {
        private Map<String, ExperimentHolder> experimentIds
            = new HashMap<String, ExperimentHolder>();
        // current interaction being processed
        private InteractionHolder holder = null;
        // current experiment being processed
        private ExperimentHolder experimentHolder = null;
        // current gene being processed
        private InteractorHolder interactorHolder = null;
        private Item comment = null;
        private String experimentId = null, interactorId = null;
        private String regionName = null;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        // intactId to temporary holding object
        private Map<String, InteractorHolder> intactIdToHolder
            = new HashMap<String, InteractorHolder>();
        // per gene - list of identifiers
        private Map<String, Set<String>> geneIdentifiers = new HashMap<String, Set<String>>();


        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            attName = null;

            // ------------- experiments ------------------------- //

            // <experimentList><experimentDescription>
            if ("experimentDescription".equals(qName)) {
                experimentId = attrs.getValue("id");
            //  <experimentList><experimentDescription id="2"><names><shortLabel>
            } else if ("shortLabel".equals(qName) && "names".equals(stack.peek())
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentName";
            //  <experimentList><experimentDescription id="2"><names><fullName>
            } else if ("fullName".equals(qName) && "names".equals(stack.peek())
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentDescr";
            //<experimentList><experimentDescription><bibref><xref><primaryRef>
            } else if ("primaryRef".equals(qName) &&  "xref".equals(stack.peek())
                            && stack.search("bibref") == 2
                            && stack.search("experimentDescription") == 3) {
                experimentHolder.setPublication(attrs.getValue("id"));
            //<experimentList><experimentDescription><attributeList><attribute>
            } else if ("attribute".equals(qName) && "attributeList".equals(stack.peek())
                            && stack.search("experimentDescription") == 2) {
                String name = attrs.getValue("name");
                if (experimentHolder.experiment != null && name != null
                        && INTERESTING_COMMENTS.contains(name)) {
                    comment = createItem("Comment");
                    comment.setAttribute("type", name);
                    attName = "experimentAttribute";
                }
            // <hostOrganismList><hostOrganism ncbiTaxId="9534"><names><fullName>
            } else if ("hostOrganism".equals(qName)) {
                attName = "hostOrganism";
            //<interactionDetectionMethod><xref><primaryRef>
            } else if ("primaryRef".equals(qName) && "xref".equals(stack.peek())
                            && stack.search("interactionDetectionMethod") == 2) {
                String termItemId = getTerm(attrs.getValue("id"));
                experimentHolder.setMethod("interactionDetectionMethods", termItemId);
            //<participantIdentificationMethod><xref> <primaryRef>
            } else if ("primaryRef".equals(qName) && "xref".equals(stack.peek())
                            && stack.search("participantIdentificationMethod") == 2) {
                String termItemId = getTerm(attrs.getValue("id"));
                experimentHolder.setMethod("participantIdentificationMethods", termItemId);


            // --------------- interactors -------------------- //

            // <interactorList><interactor id="4">
            } else if ("interactor".equals(qName) && "interactorList".equals(stack.peek())) {
                interactorId = attrs.getValue("id");
            // <interactorList><interactor id="4"><names><fullName>F15C11.2</fullName>
            } else if (("fullName".equals(qName) || "shortLabel".equals(qName))
                            && stack.search("interactor") == 2) {
                attName = qName;
            // <interactorList><interactor id="4"><xref>
            // <secondaryRef db="sgd" dbAc="MI:0484" id="S000006331" secondary="YPR127W"/>
            } else if (("primaryRef".equals(qName) || "secondaryRef".equals(qName))
                            && stack.search("interactor") == 2 && attrs.getValue("db") != null) {
                Util.addToSetMap(geneIdentifiers, attrs.getValue("db").toLowerCase(),
                        attrs.getValue("id"));
            // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if ("organism".equals(qName) && "interactor".equals(stack.peek())) {
                String taxId = attrs.getValue("ncbiTaxId");
                if ((taxonIds == null || taxonIds.isEmpty()) || taxonIds.contains(taxId))  {
                    try {
                        processGene(taxId, interactorId);
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException("failed storing gene");
                    }
                }
                // reset list of identifiers, this list is only per gene
                geneIdentifiers = new HashMap<String, Set<String>>();
            // <interactorList><interactor id="4"><names>
            // <alias type="locus name" typeAc="MI:0301">HSC82</alias>
            } else if ("alias".equals(qName) && "names".equals(stack.peek())
                            && stack.search("interactor") == 2) {
                String type = attrs.getValue("type");
                if (ALIAS_TYPE.equals(type)) {
                    attName = type;
                }
            // <interactorList><interactor id="4"><sequence>
            } else if ("sequence".equals(qName) && "interactor".equals(stack.peek())) {
                attName = "sequence";

            // ---------------- interactions ---------------- //

            //<interactionList><interaction id="1"><names><shortLabel>
            } else if ("shortLabel".equals(qName) && "names".equals(stack.peek())
                            && stack.search("interaction") == 2) {
                attName = "interactionName";
            //<interaction><confidenceList><confidence><unit><names><shortLabel>
            } else if ("shortLabel".equals(qName) && "names".equals(stack.peek())
                            && stack.search("confidence") == 3) {
                attName = "confidenceUnit";
                //<interactionList><interaction><confidenceList><confidence><value>
            } else if ("value".equals(qName) && "confidence".equals(stack.peek())) {
                attName = "confidence";
                //<interactionList><interaction>
                //<participantList><participant id="5"><interactorRef>
            } else if ("interactorRef".equals(qName) && "participant".equals(stack.peek())) {
                attName = "participantId";
                // <participantList><participant id="5"><experimentalRole><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("experimentalRole") == 2) {
                attName = "proteinRole";
                //<interactionList><interaction><experimentList><experimentRef>
            } else if ("experimentRef".equals(qName) && "experimentList".equals(stack.peek())) {
                attName = "experimentRef";
                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><startStatus><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("startStatus") == 2) {
                attName = "startStatus";
                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><endStatus><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("endStatus") == 2) {
                attName = "endStatus";
                // <featureList><feature id="24"><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("feature") == 2) {
                attName = "regionName";
            // <participantList><participant id="6919"><featureList><feature id="6920">
            // <featureType><xref><primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0117"
            } else if ("primaryRef".equals(qName) && stack.search("featureType") == 2
                            && BINDING_SITE.equals(attrs.getValue("id"))
                            && interactorHolder != null) {
                interactorHolder.isRegionFeature = true;
                interactorHolder.regionName1 = regionName;
            // <participantList><participant id="6919"><featureList><feature id="6920">
            //    <featureRangeList><featureRange><begin position="470"/>
            } else if ("begin".equals(qName) && "featureRange".equals(stack.peek())
                            && interactorHolder != null && interactorHolder.isRegionFeature) {
                interactorHolder.setStart(attrs.getValue("position"));
            // <participantList><participant id="6919"><featureList><feature id="6920">
            //    <featureRangeList><featureRange><end position="470"/>
            } else if ("end".equals(qName) && "featureRange".equals(stack.peek())
                            && interactorHolder != null && interactorHolder.isRegionFeature) {
                interactorHolder.setEnd(attrs.getValue("position"));
            //<interactorType><xref><primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0326"
            } else if ("primaryRef".equals(qName) && stack.search("interactionType") == 2) {
                String term = attrs.getValue("id");
                if (term != null) {
                    holder.setType(getTerm(term));
                }
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);
            stack.pop();

            // ----------------------- experiment ------------------------------ //

            // <experimentList><experimentDescription><attributeList><attribute/>
            // <attribute name="publication-year">2006</attribute>
            if (attName != null && "experimentAttribute".equals(attName)
                            && "attribute".equals(qName)) {
                String s = attValue.toString();
                if (comment != null && StringUtils.isNotEmpty(s)) {
                    processComment(s);
                    comment = null;
                }
            // <experimentList><experimentDescription><names><shortLabel>
            } else if (attName != null && "experimentName".equals(attName)
                            && "shortLabel".equals(qName)) {
                String shortLabel = attValue.toString();
                if (StringUtils.isNotEmpty(shortLabel)) {
                    experimentHolder = getExperiment(shortLabel);
                    experimentIds.put(experimentId, experimentHolder);
                    experimentHolder.setName(shortLabel);
                } else {
                    LOG.error("Experiment " + experimentId + " doesn't have a shortLabel");
                }

            // <experimentList><experimentDescription><names><fullName>
            } else if (attName != null && "experimentDescr".equals(attName)
                            && "fullName".equals(qName)) {
                String descr = attValue.toString();
                if (StringUtils.isNotEmpty(descr)) {
                    experimentHolder.setDescription(descr);
                }
            // <hostOrganismList><hostOrganism ncbiTaxId="9534"><names><fullName>
            } else if (attName != null && "hostOrganism".equals(attName)
                            && "fullName".equals(qName)) {
                // organism must be a string because several entries are in vivo or in vitro, with
                // a taxonid of -1 or -2
                String hostOrganism = attValue.toString();
                if (StringUtils.isNotEmpty(hostOrganism)) {
                    experimentHolder.setHostOrganism(hostOrganism);
                }


             // ----------------- interactors ----------------------- //

            // <interactorList><interactor id="4"><names><fullName>
            } else if (("fullName".equals(qName) || "shortLabel".equals(qName))
                            && stack.search("interactor") == 2) {
                String name = attValue.toString();
                if (StringUtils.isNotEmpty(name)) {
                    Util.addToSetMap(geneIdentifiers, qName, name);
                }
            // <interactorList><interactor id="4"><names><alias>
            } else if ("alias".equals(qName)) {
                String identifier = attValue.toString();
                if (StringUtils.isNotEmpty(identifier)) {
                    Util.addToSetMap(geneIdentifiers, attName, formatString(identifier));
                }

            //  ----------------- interactions ----------------------- //

            //<interactionList><interaction><participantList><participant id="5"><interactorRef>
            } else if ("interactorRef".equals(qName) && "participant".equals(stack.peek())) {
                String id = attValue.toString();
                interactorHolder = intactIdToHolder.get(id);
                if (interactorHolder != null) {
                    holder.addInteractor(interactorHolder);
                } else {
                    holder.isValid = false;
                }
            // <interactionList><interaction><names><shortLabel>
            } else if ("shortLabel".equals(qName) && attName != null
                            && "interactionName".equals(attName)) {
                holder = new InteractionHolder(attValue.toString());
            //<interactionList><interaction><experimentList><experimentRef>
            } else if ("experimentRef".equals(qName) && "experimentList".equals(stack.peek())) {
                String experimentRef = attValue.toString();
                if (experimentIds.get(experimentRef) != null) {
                    holder.setExperiment(experimentIds.get(experimentRef));
                } else {
                    LOG.error("Bad experiment:  [" + experimentRef + "] of "
                              + experimentIds.size() + " experiments");
                }
            //<interaction><confidenceList><confidence><unit><names><shortLabel>
            } else if ("shortLabel".equals(qName) && attName != null
                            && "confidenceUnit".equals(attName) && holder != null) {
                String shortLabel = attValue.toString();
                if (StringUtils.isNotEmpty(shortLabel)) {
                    holder.confidenceUnit = shortLabel;
                }
            //<interactionList><interaction><confidenceList><confidence><value>
            } else if ("value".equals(qName) && attName != null && "confidence".equals(attName)
                            && holder != null) {
                if (holder.confidenceUnit.equals("author-confidence")) {
                    holder.setConfidence(attValue.toString());
                }
            // <interactionList><interaction><participantList><participant id="5">
            // <experimentalRole><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("experimentalRole") == 2
                            && interactorHolder != null) {
                interactorHolder.role = attValue.toString();
            // <participantList><participant id="6919"><featureList><feature id="6920">
            //    <featureRangeList><featureRange><startStatus><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("startStatus") == 2
                            && interactorHolder != null && interactorHolder.isRegionFeature) {
                interactorHolder.startStatus = attValue.toString();
                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><endStatus><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("endStatus") == 2
                            && interactorHolder != null && interactorHolder.isRegionFeature) {
                interactorHolder.endStatus = attValue.toString();
                //     <featureList><feature id="24"><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("feature") == 2
                            && attName != null && "regionName".equals(attName)) {
                regionName  = attValue.toString();
                //<interactionList><interaction>
            } else if ("interaction".equals(qName) && holder != null) {
                if (holder.isValid) {
                    try {
                        storeAll(holder);
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e);
                    }
                    holder = null;
                    interactorHolder = null;
                }
            }
        }

        private Item getInteraction(String refId, String gene2RefId) throws ObjectStoreException {
            MultiKey key = new MultiKey(refId, gene2RefId);
            Item interaction = interactions.get(key);
            if (interaction == null) {
                interaction = createItem("Interaction");
                interaction.setReference("gene1", refId);
                interaction.setReference("gene2", gene2RefId);
                interactions.put(key, interaction);
                store(interaction);
            }
            return interaction;
        }

        private void storeAll(InteractionHolder h) throws ObjectStoreException {

            // for every gene in interaction store interaction pair
            for (InteractorHolder gene1Interactor: h.interactors) {

                ReferenceList allInteractors = getAllRefIds(h.interactors);

                Set<InteractorHolder> gene2Interactors
                    = new HashSet<InteractorHolder>(h.interactors);
                gene2Interactors.remove(gene1Interactor);

                // protein interactions may have more than one gene ID (not usually though)
                for (String gene1RefId : gene1Interactor.geneRefIds) {
                    storeDetails(h, gene1Interactor, gene2Interactors, gene1RefId, allInteractors);
                }

                /* store all experiment-related items */
                ExperimentHolder eh = h.eh;
                if (!eh.isStored) {
                    if (eh.comments != null && !eh.comments.isEmpty()) {
                        eh.experiment.setCollection("comments", eh.comments);
                    }
                    store(eh.experiment);
                    eh.isStored = true;
                }
            }
        }

        // get all the gene ref IDs for an interaction
        private ReferenceList getAllRefIds(Set<InteractorHolder> allIds) {
            ReferenceList allInteractors = new ReferenceList("allInteractors");
            for (InteractorHolder ih : allIds) {
                // only multiple IDs for proteins
                for (String refId : ih.geneRefIds) {
                    allInteractors.addRefId(refId);
                }
            }
            return allInteractors;
        }

        private void storeDetails(InteractionHolder h, InteractorHolder gene1Interactor,
                Set<InteractorHolder> gene2Interactors, String gene1RefId,
                ReferenceList allInteractors)
            throws ObjectStoreException {

            // for each interaction pair, store details
            for (InteractorHolder gene2Interactor : gene2Interactors) {

                // interactor (if protein) may have 2 genes
                for (String gene2RefId : gene2Interactor.geneRefIds) {
                    String role1 = gene1Interactor.role;
                    String role2 = gene2Interactor.role;
                    if (SPOKE_MODEL.equalsIgnoreCase(role1)
                            && SPOKE_MODEL.equalsIgnoreCase(role2)) {
                        // spoke!  not storing prey - prey, only bait - prey
                        continue;
                    }
                    Item interaction = getInteraction(gene1RefId, gene2RefId);
                    Item interactionDetail =  createItem("InteractionDetail");
                    String shortName = h.shortName;
                    interactionDetail.setAttribute("name", shortName);
                    interactionDetail.setAttribute("role1", role1);
                    interactionDetail.setAttribute("role2", role2);
                    interactionDetail.setAttribute("type", INTERACTION_TYPE);
                    if (h.confidence != null) {
                        interactionDetail.setAttribute("confidence", h.confidence.toString());
                    }
                    if (h.confidenceText != null) {
                        interactionDetail.setAttribute("confidenceText", h.confidenceText);
                    }
                    interactionDetail.setReference("relationshipType", h.termRefId);
                    interactionDetail.setReference("experiment", h.eh.experiment.getIdentifier());
                    interactionDetail.setReference("interaction", interaction);
                    processRegions(h, interactionDetail, gene1Interactor, shortName, gene1RefId);
                    interactionDetail.addCollection(allInteractors);
                    store(interactionDetail);
                }
            }
        }

        private void processRegions(InteractionHolder interactionHolder,
                Item interactionDetail, InteractorHolder ih, String shortName, String geneRefId)
            throws ObjectStoreException {
            if (ih.isRegionFeature()) {
                String refId = getRegion(ih, interactionDetail.getIdentifier(), shortName,
                        geneRefId);
                interactionDetail.addToCollection("interactingRegions", refId);
            }
        }

        private boolean locationValid(InteractorHolder ih) {
            boolean isValid = false;
            String start = ih.start;
            String end = ih.end;
            if (start != null && end != null && (!"0".equals(start) || !"0".equals(end))
                            && !start.equals(end)) {
                /*
                 * Per kmr's instructions, or else the bioseg postprocess will fail.
                 *  -- Start needs to be 1 if it is zero
                 *  -- start/end should be switched if start > end.
                */

                int c;
                try {
                    Integer a = new Integer(start);
                    Integer b = new Integer(end);
                    c = a.compareTo(b);
                } catch (NumberFormatException e) {
                    return false;
                }

                if (c == 0) {
                    return false;
                } else if (c > 0) {
                    String tmp = start;
                    start = end;
                    end = tmp;
                }

                if ("0".equals(start)) {
                    start = "1";
                }

                ih.start = start;
                ih.end = end;
                isValid = true;
            } else {
                isValid = false;
            }
            return isValid;
        }

        private void processGene(String taxonId, String intactId)
            throws ObjectStoreException, SAXException {

            if (config.get(taxonId) == null) {
                LOG.error("gene not processed.  configuration not found for taxonId: " + taxonId);
                return;
            }

            String field = config.get(taxonId)[0];
            // if empty, use gene name
            String datasource = config.get(taxonId)[1];
            Set<String> identifiers = geneIdentifiers.get(datasource);
            Set<String> refIds = new HashSet<String>();
            StringBuilder sb = null;

            if (identifiers == null || identifiers.isEmpty()) {
                LOG.error("gene not processed.  no valid identifiers found for " + datasource);
                return;
            }

            for (String identifier : identifiers) {
                // validate ensembl, look up dmel
                String newIdentifier = resolveGeneIdentifier(taxonId, datasource, identifier);
                if (StringUtils.isNotEmpty(newIdentifier)) {
                    String refId = storeGene(field, newIdentifier, taxonId);
                    refIds.add(refId);
                    if (sb == null) {
                        sb = new StringBuilder();
                        sb.append(newIdentifier);
                    } else {
                        sb.append("_" + newIdentifier);
                    }
                }
            }
            if (sb == null) {
                return;
            }
            InteractorHolder ih = intactIdToHolder.get(intactId);
            if (ih == null) {
                ih = new InteractorHolder(refIds);
                ih.identifier = sb.toString();
                intactIdToHolder.put(intactId, ih);
            } else {
                throw new RuntimeException("interactor ID found twice in same file: " + intactId);
            }
        }

        private String resolveGeneIdentifier(String taxonId, String datasource, String id) {
            if ("7227".equals(taxonId)) {
                IdResolver resolver = flyResolverFactory.getIdResolver(false);
                if (resolver != null) {
                    String identifier = id;
                    int resCount = resolver.countResolutions(taxonId, identifier);
                    if (resCount != 1) {
                        LOG.info("RESOLVER: failed to resolve gene to one identifier, "
                                + "ignoring gene: " + identifier + " count: " + resCount + " FBgn: "
                                + resolver.resolveId(taxonId, identifier));
                        return null;
                    }
                    identifier = resolver.resolveId(taxonId, identifier).iterator().next();
                    return identifier;
                }
            }
            if (ENSEMBL.equals(datasource)) {
                Integer taxonInt = null;
                try {
                    taxonInt = Integer.valueOf(taxonId);
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("invalid taxon ID " + taxonId);
                }
                OrganismData od = OR.getOrganismDataByTaxon(taxonInt);
                if (od == null) {
                    throw new RuntimeException("Add taxon ID " + taxonId
                            + " to organism repository");
                }
                final String ensemblPrefix = od.getEnsemblPrefix();
                if (ensemblPrefix == null) {
                    throw new RuntimeException("Add ensemblPrefix for " + taxonId
                            + " to organism repository");
                }
                if (id.startsWith(ensemblPrefix)) {
                    if ("9606".equals(taxonId)) {
                        IdResolver resolver = humanResolverFactory.getIdResolver(false);
                        if (resolver != null) {
                            String identifier = id;
                            int resCount = resolver.countResolutions(taxonId, identifier);
                            if (resCount != 1) {
                                LOG.info("RESOLVER: failed to resolve gene to one identifier, "
                                        + "ignoring gene: " + identifier + " count: " + resCount
                                        + " results: " + resolver.resolveId(taxonId, identifier));
                                return null;
                            }
                            identifier = resolver.resolveId(taxonId, identifier).iterator().next();
                            return identifier;
                        }
                    }
                    return id;
                } else {
                    LOG.info("gene for taxon ID had invalid ensembl identifier:" + id
                            + ", was expecting prefix of " + ensemblPrefix);
                    return null;
                }
            }
            // everyone not using the resolver should have an identifier
            if (id == null) {
                LOG.error("no identifier found for organism:" + taxonId + " interactor "
                        + interactorId);
                return null;
            }
            return id;
        }

        private String storeGene(String field, String identifier, String taxonId)
            throws SAXException, ObjectStoreException {
            String itemId = genes.get(identifier);
            if (itemId == null) {
                Item item = createItem("Gene");
                item.setAttribute(field, identifier);
                item.setReference("organism", getOrganism(taxonId));
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                itemId = item.getIdentifier();
                genes.put(identifier, itemId);
            }
            return itemId;
        }

        private String getPub(String pubMedId)
            throws SAXException {
            String itemId = pubs.get(pubMedId);
            if (itemId == null) {
                Item pub = createItem("Publication");
                pub.setAttribute("pubMedId", pubMedId);
                itemId = pub.getIdentifier();
                pubs.put(pubMedId, itemId);
                try {
                    store(pub);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            return itemId;
        }

        private ExperimentHolder getExperiment(String name) {
            ExperimentHolder eh = (ExperimentHolder) experimentNames.get(name);
            if (eh == null) {
                eh = new ExperimentHolder(createItem("InteractionExperiment"));
                experimentNames.put(name, eh);
            }
            return eh;
        }

        private String getRegion(InteractorHolder ih, String interactionRefId,
                                 String interactionName, String geneRefId)
            throws ObjectStoreException {
            String refId = regions.get(ih.regionName1);
            if (refId == null) {
                Item region = createItem("InteractionRegion");
                refId = region.getIdentifier();
                region.setReference("gene", geneRefId);
                region.setReference("ontologyTerm", termId);
                if (ih.startStatus != null) {
                    region.setAttribute("startStatus", ih.startStatus);
                }
                if (ih.endStatus != null) {
                    region.setAttribute("endStatus", ih.endStatus);
                }
                region.setReference("interaction", interactionRefId);
                regions.put(regionName, refId);
                if (locationValid(ih)) {
                    Item location = createItem("Location");
                    location.setAttribute("start", ih.start);
                    location.setAttribute("end", ih.end);
                    location.setReference("locatedOn", geneRefId);
                    location.setReference("feature", refId);
                    region.setReference("location", location);
                    store(location);
                }
                store(region);
            }
            return refId;
        }

        private void processComment(String s) {
            comment.setAttribute("description", s);
            try {
                store(comment);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("Couldn't store comment: ", e);
            }
            experimentHolder.comments.add(comment.getIdentifier());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            int st = start;
            int l = length;

            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (l > 0) {
                    boolean whitespace = false;
                    switch(ch[st]) {
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
                    ++st;
                    --l;
                }

                if (l > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, st, l);
                    attValue.append(s);
                }
            }
        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about an interaction until
         * it's verified that all organisms are in the list given.
         * @author Julie Sullivan
         */
        protected class InteractionHolder
        {
            private String shortName;
            private ExperimentHolder eh;
            private Double confidence;
            private String confidenceText;
            String confidenceUnit;
            private Set<InteractorHolder> interactors = new LinkedHashSet<InteractorHolder>();
            boolean isValid = true;
            private Set<String> regionIds = new HashSet<String>();
            private String termRefId;

            /**
             * Constructor
             * @param shortName name of this interaction
             */
            public InteractionHolder(String shortName) {
                this.shortName = shortName;
            }

            /**
             *
             * @param experimentHolder object holding experiment object
             */
            protected void setExperiment(ExperimentHolder experimentHolder) {
                this.eh = experimentHolder;
            }

            /**
             *
             * @param confidence confidence score for interaction
             */
            protected void setConfidence(String confidence) {
                try {
                    this.confidence = new Double(confidence);
                } catch (NumberFormatException e) {
                    confidenceText = (confidenceText != null
                                    ? confidenceText + ' ' + confidence : confidence);
                }
            }

            /**
             * @param ih object holding interactor
             */
            protected void addInteractor(InteractorHolder ih) {
                interactors.add(ih);
            }

            /**
             * @param regionId Id of ProteinInteractionRegion object
             */
            protected void addRegion(String regionId) {
                regionIds.add(regionId);
            }

            /**
             * @param refId id representing a term object
             */
            protected void setType(String refId) {
                termRefId = refId;
            }
        }

        /**
         * Holder object for Interaction.  Holds all information about a gene in an
         * interaction until it's verified that all organisms are in the list given.
         */
        protected class InteractorHolder
        {
            // collection because some proteins have multiple gene IDs. can't find an example
            // though
            private Set<String> geneRefIds = new HashSet<String>();
            private String role;
            private String regionName1; // for storage later
            private String startStatus, start;
            private String endStatus, end;
            private String identifier;

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
             * @param refIds list of IDs representing gene objects for this interactor
             */
            public InteractorHolder(Set<String> refIds) {
                setGeneRefIds(refIds);
            }

            private void setGeneRefIds(Set<String> geneRefIds) {
                this.geneRefIds = geneRefIds;
            }

            /**
             * @return list of IDs that represent the gene objects for this interactor
             */
            protected Set<String> getGeneRefIds() {
                return geneRefIds;
            }

            /**
             * @param start start position of region
             */
            protected void setStart(String start) {
                this.start = start;
            }

            /**
             * @param end the end position of the region
             */
            protected void setEnd(String end) {
                this.end = end;
            }

            /**
             * @return the endStatus
             */
            protected String getEndStatus() {
                return endStatus;
            }

            /**
             * @param endStatus the endStatus to set
             */
            protected void setEndStatus(String endStatus) {
                this.endStatus = endStatus;
            }

            /**
             * @return the identifier
             */
            protected String getIdentifier() {
                return identifier;
            }

            /**
             * @param identifier the identifier to set
             */
            protected void setIdentifier(String identifier) {
                this.identifier = identifier;
            }

            /**
             * @return the isRegionFeature
             */
            protected boolean isRegionFeature() {
                return isRegionFeature;
            }

            /**
             * @param isRegionFeature the isRegionFeature to set
             */
            protected void setRegionFeature(boolean isRegionFeature) {
                this.isRegionFeature = isRegionFeature;
            }

            /**
             * @return the role
             */
            protected String getRole() {
                return role;
            }

            /**
             * @param role the role to set
             */
            protected void setRole(String role) {
                this.role = role;
            }

            /**
             * @return the startStatus
             */
            protected String getStartStatus() {
                return startStatus;
            }

            /**
             * @param startStatus the startStatus to set
             */
            protected void setStartStatus(String startStatus) {
                this.startStatus = startStatus;
            }

            /**
             * @return the end
             */
            protected String getEnd() {
                return end;
            }

            /**
             * @return the start
             */
            protected String getStart() {
                return start;
            }

        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about an experiment until
         * an interaction is verified to have only valid organisms
         * @author Julie Sullivan
         */
        protected class ExperimentHolder
        {

            @SuppressWarnings("unused")
            private String name, description;
            private Item experiment;
            private List<String> comments = new ArrayList<String>();
            private boolean isStored = false;

            /**
             * Constructor
             * @param experiment experiment where this interaction was observed
             */
            public ExperimentHolder(Item experiment) {
                this.experiment = experiment;
            }

            /**
             *
             * @param name name of experiment
             */
            protected void setName(String name) {
                experiment.setAttribute("name", name);
                this.name = name;
            }

            /**
             * @param description description of experiment
             */
            protected void setDescription(String description) {
                experiment.setAttribute("description", description);
            }

            /**
             * @param pubMedId of this experiment
             * @throws SAXException if publication can't be stored
             */
            protected void setPublication(String pubMedId)
                throws SAXException {
                if (StringUtil.allDigits(pubMedId)) {
                    String pubRefId = getPub(pubMedId);
                    experiment.setReference("publication", pubRefId);
                }
            }

            /**
             * @param collectionname method
             * @param termItemId termID
             */
            protected void setMethod(String collectionname, String termItemId) {
                experiment.addToCollection(collectionname, termItemId);
            }

            /**
             *
             * @param ref name of organism
             */
            protected void setHostOrganism(String ref) {
                experiment.setAttribute("hostOrganism", ref);
            }
        }
    }

    /**
     * create and store protein interaction terms
     * @param identifier identifier for interaction term
     * @return id representing term object
     * @throws SAXException if term can't be stored
     */
    private String getTerm(String identifier) throws SAXException {
        String itemId = terms.get(identifier);
        if (itemId == null) {
            try {
                Item term = createItem("InteractionTerm");
                term.setAttribute("identifier", identifier);
                itemId = term.getIdentifier();
                terms.put(identifier, itemId);
                store(term);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return itemId;
    }

    private String formatString(String ident) {
        String identifier = ident;
        if (identifier.startsWith("Dmel_")) {
            identifier = identifier.substring(5);
        }
        if (identifier.startsWith("cg")) {
            identifier = "CG" + identifier.substring(2);
        }
        return identifier;
    }
}
