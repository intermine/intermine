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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse biogrid data into items
 *
 * Genetic interactions are labeled protein interactions, so we can't store or create any objects
 * until all experiments and interactors are processed.  Holder objects are creating, storing the
 * data processed until the interactions are processed and we know the interactionType
 *
 * EXPERIMENT HOLDER
 * holds the experiment data until the experiment data are processed
 *
 * INTERACTOR HOLDER
 * holds the identifier
 *
 * INTERACTION HOLDER
 * holds all interaction data until the entire entry is processed
 *
 * invalid experiments:
 *  - human interactions use genbank IDs
 *  - dmel gene doesn't resolve
 *  - if any of the participants are invalid, we throw away the interaction
 *
 * @author Julie Sullivan
 */
public class BioGridConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(BioGridConverter.class);
    private static final String PROP_FILE = "biogrid_config.properties";
    protected IdResolverFactory resolverFactory;
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, String> organisms = new HashMap<String, String>();
    private static final Map<String, String> PSI_TERMS = new HashMap<String, String>();
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, Map<String, String>> config = new HashMap<String, Map<String, String>>();
    private Set<String> taxonIds = null;
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();
    private Map<MultiKey, Item> idsToExperiments;
    private Map<String, String> strains = new HashMap<String, String>();
    private Map<MultiKey, Item> interactions = new HashMap<MultiKey, Item>();
    private static final String BAIT = "bait";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public BioGridConverter(ItemWriter writer, Model model) {
        super(writer, model, "BioGRID", "BioGRID interaction data set");
        readConfig();
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    static {
        PSI_TERMS.put("MI:0915", "physical");
        PSI_TERMS.put("MI:0407", "physical");
        PSI_TERMS.put("MI:0403", "physical");
        PSI_TERMS.put("MI:0914", "physical");
        PSI_TERMS.put("MI:0794", "genetic");
        PSI_TERMS.put("MI:0796", "genetic");
        PSI_TERMS.put("MI:0799", "genetic");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        File file = getCurrentFile();
        if (file == null) {
            throw new FileNotFoundException("No valid data files found.");
        }

        if (taxonIds != null || !taxonIds.isEmpty()) {
            if (!isValidOrganism(file.getName())) {
                return;
            }
        }

        BioGridHandler handler = new BioGridHandler();
        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private boolean isValidOrganism(String filename) {
        String organism = filename.substring(17);
        organism = organism.substring(0, organism.indexOf('-'));
        if (!organism.contains("_")) {
            return false;
        }
        String[] bits = organism.split("_");
        if (bits.length == 1) {
            return false;
        }
        OrganismData od = OR.getOrganismDataByGenusSpecies(bits[0], bits[1]);

        if (taxonIds.contains(String.valueOf(od.getTaxonId()))) {
            return true;
        }
        return false;
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

            String taxonId = attributes[0];
            if (config.get(taxonId) == null) {
                Map<String, String> configs = new HashMap<String, String>();
                config.put(taxonId, configs);
            }
            if ("xref".equals(attributes[1])) {
                config.get(taxonId).put(attributes[2], value.toLowerCase());
            } else {
                String attribute = attributes[1];
                if ("strain".equals(attribute)) {
                    strains.put(value, taxonId);
                } else {
                    config.get(taxonId).put(attribute, value);
                }
            }
        }
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setBiogridOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()  {
        if (idsToExperiments != null) {
            for (Item experiment : idsToExperiments.values()) {
                try {
                    store(experiment);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Handles xml file
     */
    class BioGridHandler extends DefaultHandler
    {
        // identifier to [refId|BioGRID_id]
        private Map<String, Participant> participants = new HashMap<String, Participant>();
        // BioGRID_ID to holder - one holder object can have multiple BioGRID_ids (proteins, genes)
        private Map<String, InteractorHolder> interactors = new HashMap<String, InteractorHolder>();
        private Map<String, ExperimentHolder> experimentIDs
            = new HashMap<String, ExperimentHolder>();
        private InteractionHolder holder;
        private ExperimentHolder experimentHolder;
        private InteractorHolder interactorHolder;
        private String participantId = null;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            attName = null;

            /********************************* EXPERIMENT **********************************/
            // <experimentList><experimentDescription>
            if ("experimentDescription".equals(qName)) {
                experimentHolder = getExperimentHolder(attrs.getValue("id"));
            // <entry><source release="2.0.37" releaseDate="2008-01-25"><names><shortLabel>
            // Interactions for BIOGRID-ORGANISM-7227</shortLabel>
            } else if ("shortLabel".equals(qName) && "names".equals(stack.peek())
                            && stack.search("source") == 2) {
                attName = "organismTaxonId";
            //  <experimentList><experimentDescription id="2"><names><shortLabel>
            } else if ("shortLabel".equals(qName) && "names".equals(stack.peek())
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentName";
            //  <experimentList><experimentDescription id="2"><names><fullName>
            } else if ("fullName".equals(qName) && "names".equals(stack.peek())
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentDescr";
            //<experimentList><experimentDescription><bibref><xref><primaryRef>
            } else if ("primaryRef".equals(qName) && "xref".equals(stack.peek())
                            && stack.search("bibref") == 2
                            && stack.search("experimentDescription") == 3) {
                String pubMedId = attrs.getValue("id");
                if (StringUtil.allDigits(pubMedId)) {
                    experimentHolder.setPublication(getPub(pubMedId));
                }
            //<experimentList><experimentDescription><interactionDetectionMethod><xref><primaryRef>
            } else if ("primaryRef".equals(qName) && "xref".equals(stack.peek())
                    && stack.search("interactionDetectionMethod") == 2) {
                String term = attrs.getValue("id");
                experimentHolder.setMethod(getTerm(term));

            /*********************************** GENES ***********************************/

            // <interactorList><interactor id="4">
            } else if ("interactor".equals(qName) && "interactorList".equals(stack.peek())) {
                String interactorId = attrs.getValue("id");
                interactorHolder = new InteractorHolder(interactorId);
                interactors.put(interactorId, interactorHolder);

            // <interactorList><interactor id="4"><xref>
            // <secondaryRef db="SGD"  id="S000006331" secondary="YPR127W"/>
            } else if (("primaryRef".equals(qName) || "secondaryRef".equals(qName))
                            && stack.search("interactor") == 2) {
                String db = attrs.getValue("db");
                if (db != null) {
                    db = db.toLowerCase();
                    interactorHolder.xrefs.put(db, attrs.getValue("id"));
                }

            // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("interactor") == 2) {
                attName = "shortLabel";
           // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if ("organism".equals(qName) && "interactor".equals(stack.peek())) {
                String taxId = attrs.getValue("ncbiTaxId");
                taxId = replaceStrain(taxId);
                if ((taxonIds == null || taxonIds.isEmpty()) || taxonIds.contains(taxId))  {
                    try {
                        interactorHolder.organismRefId = getOrganism(taxId);
                    } catch (ObjectStoreException e) {
                        LOG.error("couldn't store organism:" + taxId);
                        throw new RuntimeException("Could not store organism " + taxId, e);
                    }
                    Map<String, String> identifierConfigs = config.get(taxId);

                    if (identifierConfigs != null) {
                        for (Map.Entry<String, String> entry : identifierConfigs.entrySet()) {
                            boolean validGene = setGene(taxId, interactorHolder, entry.getKey(),
                                    entry.getValue());
                            // try all configs until we get a good one to make it more likely to
                            // find a match
                            if (validGene) {
                                break;
                            }
                        }
                    }
                }
            /*********************************** INTERACTIONS ***********************************/
            // <interaction>
            } else if ("interaction".equals(qName)) {
                holder = new InteractionHolder();
            //<interactionList><interaction><experimentList><experimentRef>
            } else if ("experimentRef".equals(qName) && "experimentList".equals(stack.peek())) {
                attName = "experimentRef";
           //<interactionList><interaction>   <participantList><participant id="68259">
                // <interactorRef>
            } else if ("interactorRef".equals(qName) && "participant".equals(stack.peek())) {
                attName = "participant";
            //<interactionList><interaction><interactionType><xref><primaryRef>
            } else if ("primaryRef".equals(qName) && "xref".equals(stack.peek())
                            && stack.search("interactionType") == 2) {
                String termIdentifier = attrs.getValue("id");
                holder.methodRefId = getTerm(termIdentifier);
                String interactionType = PSI_TERMS.get(termIdentifier);
                if (interactionType == null) {
                    throw new RuntimeException("Bad interaction type:" + termIdentifier);
                }
                holder.interactionType = interactionType;
            // <participant id="62692"><interactorRef>62692</interactorRef>
            // <experimentalRoleList><experimentalRole><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("experimentalRole") == 2) {
                attName = "role";
            // <interactionList><interaction><names><shortLabel>
            } else if ("shortLabel".equals(qName) && stack.search("interaction") == 2) {
                attName = "interactionName";
            }

            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            int st = start;
            int l = length;
            if (attName != null) {
                if (l > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, st, l);
                    attValue.append(s);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);
            stack.pop();

            /********************************* EXPERIMENTS ***********************************/

            // <experimentList><experimentDescription id="13022"><names><shortLabel>
            if (attName != null && "experimentName".equals(attName) && "shortLabel".equals(qName)) {
                String shortName = attValue.toString();
                if (shortName != null) {
                    experimentHolder.shortName = shortName;
                }
            //  <experimentList><experimentDescription id="13022"><names><fullName>
            } else if (attName != null && "experimentDescr".equals(attName)
                            && "fullName".equals(qName)) {
                String descr = attValue.toString();
                if (descr != null) {
                    experimentHolder.setDescription(descr);
                }
            } else if ("experimentDescription".equals(qName)) {
                setExperiment(experimentHolder);

            /********************************* GENES ***********************************/


            // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if (attName != null && "shortLabel".equals(attName)
                            && "shortLabel".equals(qName) && stack.search("interactor") == 2) {

                String shortLabel = attValue.toString();
                if (shortLabel.startsWith("Dmel")) {
                    shortLabel = shortLabel.substring(4);
                    shortLabel = shortLabel.trim();
                }
                interactorHolder.shortLabel = shortLabel;

            /******************* INTERACTIONS ***************************************************/
            //<interactionList><interaction>   <participantList><participant id="68259">
            //<interactorRef>1</interactorRef>
            } else if (attName != null && "participant".equals(attName)
                    && "interactorRef".equals(qName)) {
                participantId = attValue.toString();
                InteractorHolder ih = interactors.get(participantId);
                if (ih == null) {
                    holder.validActors = false;
                    LOG.error("invalid participant ID: " + participantId);
                } else {
                    // TODO make sure this is necessary.  interactor id is reused?
                    ih.role = null;
                    // resolver didn't return valid identifier
                    Participant p = ih.participant;
                    if (p == null) {
                        ih.valid = false;
                        holder.validActors = false;
                    } else {
                        holder.refIds.add(p.refId);
                        holder.identifiers.add(p.identifier);
                        holder.addInteractor(participantId, ih);
                    }
                }
            //<participant><interactorRef>
            //<experimentalRoleList><experimentalRole><names><shortLabel>
            } else if (attName != null && "role".equals(attName) && "shortLabel".equals(qName)
                            && stack.search("experimentalRole") == 2) {
                String role = attValue.toString();
                if (role != null) {
                    InteractorHolder ih = interactors.get(participantId);
                    if (ih == null) {
                        holder.validActors = false;
                    } else {
                        ih.role = role;
                    }
                }
            //<interactionList><interaction><experimentList><experimentRef>
            } else if (attName != null && "experimentRef".equals(attName)
                            && "experimentRef".equals(qName)
                            && "experimentList".equals(stack.peek())) {
                holder.setExperimentHolder(experimentIDs.get(attValue.toString()));
            // <interactionList><interaction><names><shortLabel>
            } else if (attName != null && "interactionName".equals(attName)
                    && "shortLabel".equals(qName)) {
                String name = attValue.toString();
                if (name != null) {
                    holder.name = name;
                }

            //</interaction>
            } else if ("interaction".equals(qName) && holder != null && holder.validActors) {
                try {
                    storeInteraction(holder);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException(" can't store data", e);
                }
                holder = null;
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

        private void storeInteraction(InteractionHolder h) throws ObjectStoreException  {

            // for every gene in interaction store interaction pair
            for (InteractorHolder gene1Interactor: h.ihs.values()) {

                // gene1
                String refId = gene1Interactor.participant.refId;
                Set<InteractorHolder> ihs = new HashSet<InteractorHolder>(h.ihs.values());

                // loop through other genes in this interaction, set as gene2
                for (InteractorHolder gene2Interactor : ihs) {

                    String gene2RefId = gene2Interactor.participant.refId;
                    if (gene2RefId.equals(refId)) {
                        continue;
                    }

                    Item interaction = getInteraction(refId, gene2RefId);
                    Item detail = createItem("InteractionDetail");

                    String role1 = gene1Interactor.role;
                    String role2 = gene2Interactor.role;
                    if (BAIT.equalsIgnoreCase(role1) && BAIT.equalsIgnoreCase(role2)) {
                        // spoke!  not storing bait - bait, only bait - prey
                        continue;
                    }

                    if (gene1Interactor.role != null) {
                        detail.setAttribute("role1", role1);
                    }
                    if (gene2Interactor.role != null) {
                        detail.setAttribute("role2", role2);
                    }
                    detail.setAttribute("type", h.interactionType);
                    detail.setReference("relationshipType", h.methodRefId);
                    detail.setReference("experiment", h.eh.experimentRefId);
                    if (StringUtils.isEmpty(h.name)) {
                        String prettyName = StringUtils.join(h.identifiers, "_");
                        detail.setAttribute("name", "BioGRID:" + prettyName);
                    } else {
                        detail.setAttribute("name", h.name);
                    }
                    detail.setReference("interaction", interaction);
                    store(detail);
                }
            }
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

        private boolean setGene(String taxonId, InteractorHolder ih, String identifierField,
                                String db) throws SAXException {

            IdResolver resolver = resolverFactory.getIdResolver(false);

            String identifier = null;
            String label = identifierField;

            if ("shortLabel".equals(db)) {
                identifier = ih.shortLabel;
            } else {
                identifier = ih.xrefs.get(db);
            }

            if ("7227".equals(taxonId) && resolver != null) {
                identifier = resolveGene(resolver, taxonId, identifier);
                label = "primaryIdentifier";
            }

            // no valid identifiers
            if (identifier == null) {
                ih.valid = false;
                return false;
            }

            ih.participant = storeGene(label, identifier, ih, taxonId);

            ih.valid = true;
            return true;
        }

        private Participant storeGene(String label, String identifier, InteractorHolder ih,
                String taxonId)
            throws SAXException {
            // genes can be in different XML files
            String refId = genes.get(identifier);
            if (refId == null) {
                Item item = createItem("Gene");
                item.setAttribute(label, identifier);
                try {
                    item.setReference("organism", getOrganism(taxonId));
                    Item xref = processBioGridId(ih, item);
                    if (xref != null) {
                        item.addToCollection("crossReferences", xref);
                    }
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                refId = item.getIdentifier();
                genes.put(identifier, refId);
            }
            // participants are specific to an XML file, includes BioGRID id
            Participant p = participants.get(identifier);
            if (p == null) {
                p = new Participant(identifier, ih, refId);
                participants.put(identifier, p);
            } else {
                // we've seen this gene before, discard current holder object - replace with
                // holder object already used
                String interactorId = ih.biogridId;
                interactors.put(interactorId, p.ih);
            }
            return p;
        }

        private Item processBioGridId(InteractorHolder ih, Item item)
            throws ObjectStoreException {
            String biogridID = ih.getBiogridId();
            if (StringUtils.isNotEmpty(biogridID)) {
                Item xref = createItem("CrossReference");
                xref.setAttribute("identifier", biogridID);
                xref.setReference("subject", item);
                store(xref);
                return xref;
            }
            return null;
        }

        /**
         * resolve dmel genes
         * @param taxonId id of organism for this gene
         * @param ih interactor holder
         * @throws ObjectStoreException
         */
        private String resolveGene(IdResolver resolver, String taxonId, String identifier) {
            String id = identifier;
            if ("7227".equals(taxonId) && resolver != null) {
                int resCount = resolver.countResolutions(taxonId, identifier);
                if (resCount != 1) {
                    LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                             + identifier + " count: " + resCount + " FBgn: "
                             + resolver.resolveId(taxonId, identifier));
                    return null;
                }
                id = resolver.resolveId(taxonId, identifier).iterator().next();
            }
            return id;
        }

        private String getOrganism(String taxonId)
            throws ObjectStoreException {
            String refId = organisms.get(taxonId);
            if (refId != null) {
                return refId;
            }
            Item item = createItem("Organism");
            item.setAttribute("taxonId", taxonId);
            organisms.put(taxonId, item.getIdentifier());
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            return item.getIdentifier();
        }

        private String replaceStrain(String id) {
            String mainTaxonId = strains.get(id);
            if (StringUtils.isNotEmpty(mainTaxonId)) {
                return mainTaxonId;
            }
            return id;
        }

        private String getTerm(String identifier)
            throws SAXException {
            String refId = terms.get(identifier);
            if (refId != null) {
                return refId;
            }
            Item item = createItem("InteractionTerm");
            item.setAttribute("identifier", identifier);
            terms.put(identifier, item.getIdentifier());
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return item.getIdentifier();
        }

        private ExperimentHolder getExperimentHolder(String experimentId) {
            ExperimentHolder eh =  experimentIDs.get(experimentId);
            if (eh == null) {
                eh = new ExperimentHolder();
                experimentIDs.put(experimentId, eh);
            }
            return eh;
        }

        /**
         * Experiments are in the worm file multiple times, with the same publication and
         * experiment name.  The only difference is the interactionDetectionMethod.
         * @param eh temporary holder for experiment
         */
        private void setExperiment(ExperimentHolder eh) {
            String pubRefId = eh.pubRefId;
            String name = eh.shortName;
            MultiKey key = new MultiKey(pubRefId, name);
            if (idsToExperiments == null || idsToExperiments.isEmpty()) {
                idsToExperiments = new HashMap<MultiKey, Item>();
            }
            Item exp = idsToExperiments.get(key);
            if (exp == null) {
                exp = createItem("InteractionExperiment");
                exp.addToCollection("interactionDetectionMethods", eh.methodRefId);
                if (eh.description != null && !eh.description.equals("")) {
                    exp.setAttribute("description", eh.description);
                }
                exp.setAttribute("name", name);
                exp.setReference("publication", pubRefId);
                idsToExperiments.put(key, exp);
            }
            eh.experimentRefId = exp.getIdentifier();
        }

        /**
         * Holder object for GeneInteraction.  Holds all information about an interaction until
         * ready to store
         * @author Julie Sullivan
         */
        public class InteractionHolder
        {
            protected ExperimentHolder eh;
            protected Map<String, InteractorHolder> ihs = new HashMap<String, InteractorHolder>();
            protected Set<String> refIds = new HashSet<String>();
            protected Set<String> identifiers = new HashSet<String>();
            protected boolean validActors = true;
            protected String methodRefId;
            protected String interactionType = "physical";
            protected String name;

            /**
             * @param eh object holding experiment object
             */
            protected void setExperimentHolder(ExperimentHolder eh) {
                this.eh = eh;
            }

            /**
             * @param id the participant id used only in the XML
             * @param ih object holding interactor
             */
            protected void addInteractor(String id, InteractorHolder ih) {
                ihs.put(id, ih);
            }

            @Override
            public String toString() {
                return StringUtil.join(identifiers, ",");
            }

            @Override
            public int hashCode() {
                return (methodRefId.hashCode() + 3 * eh.hashCode() + 5 * identifiers.hashCode());
            }
        }

        /**
         * Represents a "participant" in BioGRID XML
         */
        protected class Participant
        {
            protected String identifier;
            protected InteractorHolder ih;
            protected String refId;

            /**
             * Constructor.
             *
             * @param identifier eg. FBgn
             * @param ih holder object for this interacting gene
             * @param refId ID representing stored gene object
             */
            protected Participant(String identifier, InteractorHolder ih, String refId) {
                this.identifier = identifier;
                this.refId = refId;
                this.ih = ih;
            }
        }

        /**
         * Holder object for Interactor.
         */
        protected class InteractorHolder
        {
            // bait or prey
            protected String role;
            // <interactorList><interactor id="1">
            // <interactorRef>1</interactorRef>
            protected String biogridId;
            // refId and identifier, eg. FBgn
            protected Participant participant = null;
            // db to identifier, eg. FlyBase --> FBgn
            protected Map<String, String> xrefs = new HashMap<String, String>();
            // symbol
            protected String shortLabel;
            protected boolean valid = true;
            protected String organismRefId;

            /**
             * Constructor
             * @param id bioGRID id
             */
            public InteractorHolder(String id) {
                this.biogridId = id;
            }

            /**
             * @return ID to use to link to biogrid
             */
            protected String getBiogridId() {
                if (xrefs == null || xrefs.isEmpty()) {
                    return null;
                }
                return xrefs.get("biogrid");
            }

        }


    }

    /**
     * Holder object for Experiment.  Holds all information about an experiment until
     * an interaction is verified to have only valid organisms
     * @author Julie Sullivan
     */
    protected class ExperimentHolder
    {
        protected String experimentRefId;
        protected String shortName;
        protected String description;
        protected String pubRefId;
        protected boolean isStored = false;
        protected String methodRefId;

        /**
         *
         * @param description the full name of the experiments
         */
        protected void setDescription(String description) {
            this.description = description;
        }

        /**
         *
         * @param pubRefId reference to publication item for this experiment
         */
        protected void setPublication(String pubRefId) {
            this.pubRefId = pubRefId;
        }

        /**
         * terms describe the method used int he experiment, eg two hybrid, etc
         * @param methodRefId reference to the term item for this experiment
         */
        protected void setMethod(String methodRefId) {
            this.methodRefId = methodRefId;
        }
    }
}
