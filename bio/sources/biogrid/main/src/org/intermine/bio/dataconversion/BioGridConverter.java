package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
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
    protected IdResolverFactory resolverFactory;
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, String> organisms = new HashMap<String, String>();
    private static final Map<String, String> PSI_TERMS = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public BioGridConverter(ItemWriter writer, Model model) {
        super(writer, model, "BioGRID", "BioGRID interaction data set");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();
    }

    static {
        PSI_TERMS.put("Biochemical Activity", "biochemical");
        PSI_TERMS.put("Co-localization", "colocalization");
        PSI_TERMS.put("Co-purification", "copurification");
        PSI_TERMS.put("Far Western", "far western blotting");
        PSI_TERMS.put("FRET", "FRET");
        PSI_TERMS.put("PCA", "protein complementation assay");
        PSI_TERMS.put("Synthetic Lethality", "synthetic lethal");
        PSI_TERMS.put("Two-hybrid", "two hybrid");
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        BioGridHandler handler = new BioGridHandler();
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
    class BioGridHandler extends DefaultHandler
    {
        private Map<String, Item> genes = new HashMap<String, Item>();
        private Map<String, InteractorHolder> interactors = new HashMap<String, InteractorHolder>();
        private Map<String, ExperimentHolder> experiments = new HashMap<String, ExperimentHolder>();
        private InteractionHolder holder;
        private ExperimentHolder experimentHolder;
        private InteractorHolder interactorHolder;
        private String participantId = null;

        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;

        /**
         * Constructor
         */
        public BioGridHandler() {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
            attName = null;

            /********************************* EXPERIMENT **********************************/

            // <experimentList><experimentDescription>
            if (qName.equals("experimentDescription")) {
                experimentHolder = getExperimentHolder(attrs.getValue("id"));
            // <entry><source release="2.0.37" releaseDate="2008-01-25"><names><shortLabel>
            // Interactions for BIOGRID-ORGANISM-7227</shortLabel>
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("source") == 2) {
                attName = "organismTaxonId";
            //  <experimentList><experimentDescription id="2"><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentName";
            //  <experimentList><experimentDescription id="2"><names><fullName>
            } else if (qName.equals("fullName") && stack.peek().equals("names")
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentDescr";
            //<experimentList><experimentDescription><bibref><xref><primaryRef>
            } else if (qName.equals("primaryRef") && stack.peek().equals("xref")
                            && stack.search("bibref") == 2
                            && stack.search("experimentDescription") == 3) {
                String pubMedId = attrs.getValue("id");
                if (StringUtil.allDigits(pubMedId)) {
                    experimentHolder.setPublication(getPub(pubMedId));
                }
            //<experimentList><experimentDescription><interactionDetectionMethod><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("interactionDetectionMethod") == 2) {
                attName = "interactionDetectionMethod";

            /*********************************** GENES ***********************************/

            // <interactorList><interactor id="4">
            } else if (qName.equals("interactor") && stack.peek().equals("interactorList")) {
                String interactorId = attrs.getValue("id");
                interactorHolder = new InteractorHolder(interactorId);
                interactors.put(interactorId, interactorHolder);
            // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("interactor") == 2) {

                attName = "secondaryIdentifier";

             // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if (qName.equals("organism") && stack.peek().equals("interactor")) {
                String taxId = attrs.getValue("ncbiTaxId");
                try {
                    interactorHolder.organismRefId = getOrganism(taxId);
                    // now that we know which organism it is, we know which identifier to use
                    setIdentifier(taxId);
                } catch (ObjectStoreException e) {
                    LOG.error("couldn't store organism:" + taxId);
                }

            /*********************************** INTERACTIONS ***********************************/

            //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef") && stack.peek().equals("experimentList")) {
                attName = "experimentRef";
                holder = new InteractionHolder();
           //<interactionList><interaction>   <participantList><participant id="68259">
            } else if (qName.equals("participant") && stack.peek().equals("participantList")) {
                participantId = attrs.getValue("id");
                InteractorHolder ih = interactors.get(participantId);
                ih.role = null;
                holder.addInteractor(participantId, ih);
                holder.identifiers.add(ih.identifier);
                holder.refIds.add(ih.refId);
                if (ih.refId == null) {
                    LOG.error("~~ bad participant:" + participantId + " - didn't have an organism");
                    ih.valid = false;
                }
                if (!ih.valid) {
                    holder.validActors = false;
                }
            //<interactionList><interaction><interactionType><names><shortLabel
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("interactionType") == 2) {
                attName = "interactionType";
            // <participant id="62692"><interactorRef>62692</interactorRef>
            // <experimentalRoleList><experimentalRole><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("experimentalRole") == 2) {
                attName = "role";
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }

        /**
         * {@inheritDoc}
         */
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
         * {@inheritDoc}
         */
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            super.endElement(uri, localName, qName);
            stack.pop();

            /********************************* EXPERIMENTS ***********************************/

            // <experimentList><experimentDescription id="13022"><names><shortLabel>
            if (attName != null && attName.equals("experimentName")
                            && qName.equals("shortLabel")) {
                String shortName = attValue.toString();
                if (shortName != null) {
                    experimentHolder.shortName = shortName;
                }
            //  <experimentList><experimentDescription id="13022"><names><fullName>
            } else if (attName != null && attName.equals("experimentDescr")
                            && qName.equals("fullName")) {
                String descr = attValue.toString();
                if (descr != null) {
                    experimentHolder.setDescription(descr);
                }
            //<experimentList><experimentDescription>
            //<interactionDetectionMethod><names><shortLabel>
            } else if (attName != null && attName.equals("interactionDetectionMethod")
                            && qName.equals("shortLabel")) {

                experimentHolder.setMethod(getTerm(attValue.toString()));

            } else if (qName.equals("experimentDescription")) {
                try {
                    storeExperiment(experimentHolder);
                } catch (ObjectStoreException e) {
                    LOG.error("couldn't store experiment");
                }

            /********************************* GENES ***********************************/

            // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if (attName != null && attName.equals("secondaryIdentifier")
                            && qName.equals("shortLabel") && stack.search("interactor") == 2) {

                String secondaryIdentifier = attValue.toString();
                if (secondaryIdentifier.startsWith("Dmel")) {
                    secondaryIdentifier = secondaryIdentifier.substring(4);
                    secondaryIdentifier = secondaryIdentifier.trim();
                }
                interactorHolder.secondaryIdentifier = secondaryIdentifier;

            /******************* INTERACTIONS ***************************************************/

            //<participant><interactorRef>
            //<experimentalRoleList><experimentalRole><names><shortLabel>
            } else if (attName != null && attName.equals("role") && qName.equals("shortLabel")
                            && stack.search("experimentalRole") == 2) {
                String role = attValue.toString();
                if (role != null) {
                    interactors.get(participantId).role = role;
                }
            //<interactionList><interaction><experimentList><experimentRef>
            } else if (attName != null && attName.equals("experimentRef")
                            && qName.equals("experimentRef")
                            && stack.peek().equals("experimentList")) {
                holder.setExperimentHolder(experiments.get(attValue.toString()));
            //<interactionType><names><shortLabel>
            } else if (attName != null && attName.equals("interactionType")) {
                holder.methodRefId = getTerm(attValue.toString());
            //</interaction>
            } else if (qName.equals("interaction") && holder != null && holder.validActors) {
                storeInteraction(holder);
                holder = null;
            }
        }

        private void storeInteraction(InteractionHolder h) throws SAXException  {
            for (InteractorHolder ih: h.ihs.values()) {
                String refId = ih.refId;
                Item interaction = null;
                interaction = createItem("Interaction");
                if (ih.role != null) {
                    interaction.setAttribute("role", ih.role);
                }
                interaction.setReference("gene", refId);
                interaction.setCollection("interactingGenes", getInteractingObjects(h, refId));
                interaction.setReference("type", h.methodRefId);
                interaction.setReference("experiment", h.eh.experimentRefId);
                String interactionName = "";
                for (String identifier : h.identifiers) {
                    if (!identifier.equals(ih.identifier)) {
                        interactionName += "_" + identifier;
                    } else {
                        interactionName = "BioGRID:" + identifier + interactionName;
                    }
                }
                interaction.setAttribute("name", interactionName);
                interaction.setReference("experiment", h.eh.experimentRefId);

                try {
                    store(interaction);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
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
                    store(pub);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            return itemId;
        }

        /**
         * 1. create/store gene (if new)
         * 2. set identifier attribute
         *    - will be primaryIdentifier for dmel and secondaryIdentifier for everyone else
         *      because the idResolver returns primaryIdentifier
         * @param taxonId taxonomyId of organism for this gene.  may be different from organism
         * set at beginning of file
         */
        private void setIdentifier(String taxonId) {
            Item item = null;
            String secondaryIdentifier = interactorHolder.secondaryIdentifier;
            try {
                item = getGene(taxonId, secondaryIdentifier);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("couldn't store gene: " + secondaryIdentifier);
            }

            if (item != null) {
                interactorHolder.refId = item.getIdentifier();
                String identifier = null;
                if (item.getAttribute("secondaryIdentifier") != null) {
                    identifier = item.getAttribute("secondaryIdentifier").getValue();
                }
                if (identifier == null) {
                    identifier = item.getAttribute("primaryIdentifier").getValue();
                }
                interactorHolder.identifier = identifier;
            } else {
                interactorHolder.valid = false;
                LOG.error("could not resolve bioentity == " + secondaryIdentifier
                          + ", participantId: " + interactorHolder.biogridId);
            }
        }

        /**
         * create/store gene (if new)
         * @param taxonId id of organism for this gene
         * @param id identifier
         * @return gene item
         * @throws ObjectStoreException
         */
        private Item getGene(String taxonId, String id)
        throws ObjectStoreException {
            String identifier = id;
            IdResolver resolver = resolverFactory.getIdResolver(false);
            if (taxonId.equals("7227") && resolver != null) {
                int resCount = resolver.countResolutions(taxonId, identifier);
                if (resCount != 1) {
                    LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                             + identifier + " count: " + resCount + " FBgn: "
                             + resolver.resolveId(taxonId, identifier));
                    return null;
                }
                identifier = resolver.resolveId(taxonId, identifier).iterator().next();
            }
            Item item = genes.get(identifier);
            if (item == null) {
                item = createItem("Gene");
                String identifierLabel = (!taxonId.equals("7227")
                                ? "secondaryIdentifier" : "primaryIdentifier");
                item.setAttribute(identifierLabel, identifier);
                item.setReference("organism", getOrganism(taxonId));
                store(item);
                genes.put(identifier, item);
            }
            return item;
        }

        private String getOrganism(String taxonId)
        throws ObjectStoreException {
            String refId = organisms.get(taxonId);
            if (refId != null) {
                return refId;
            }
            try {
                Item item = createItem("Organism");
                item.setAttribute("taxonId", taxonId);
                store(item);
                organisms.put(taxonId, item.getIdentifier());
                return item.getIdentifier();
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

        }

        private String getTerm(String name)
        throws SAXException {
            String term = name;
            if (PSI_TERMS.get(term) != null) {
                term = PSI_TERMS.get(term);
            } else {
                term = term.toLowerCase().replace("-", " ");
            }
            String refId = terms.get(term);
            if (refId != null) {
                return refId;
            }
            try {
                Item item = createItem("InteractionTerm");
                item.setAttribute("name", term);
                terms.put(term, item.getIdentifier());
                store(item);
                return item.getIdentifier();
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private ExperimentHolder getExperimentHolder(String experimentId) {
            ExperimentHolder eh =  experiments.get(experimentId);
            if (eh == null) {
                eh = new ExperimentHolder(experimentId);
                experiments.put(experimentId, eh);
            }
            return eh;
        }

        private void storeExperiment(ExperimentHolder eh)
        throws ObjectStoreException {
            Item exp = createItem("InteractionExperiment");
            exp.setReference("interactionDetectionMethod", eh.methodRefId);
            exp.setAttribute("name", eh.shortName);
            exp.setReference("publication", eh.pubRefId);
            store(exp);
            eh.experimentRefId = exp.getIdentifier();
        }

        private ArrayList<String> getInteractingObjects(InteractionHolder interactionHolder,
                                                        String refId) {
            ArrayList<String> interactorIds = new ArrayList(interactionHolder.refIds);
            interactorIds.remove(refId);
            return interactorIds;
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
        }

        /**
         * Holder object for GeneInteractor. Holds id and identifier for gene until the experiment
         * is verified as a gene interaction and not a protein interaction
         * @author Julie Sullivan
         */
        protected class InteractorHolder
        {
            protected String role;
            protected String biogridId;
            protected String identifier;
            protected String refId;
            protected String secondaryIdentifier;
            protected boolean valid = true;
            protected String organismRefId;

            /**
             * Constructor
             * @param id bioGRID id
             */
            public InteractorHolder(String id) {
                this.biogridId = id;
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
            protected String id;

            /**
             * Constructor
             * @param id experiment id
             */
            public ExperimentHolder(String id) {
                this.id = id;
            }

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
}
