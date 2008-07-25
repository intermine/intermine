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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
 * holds the experiment data until the interactions are processed.
 * interactions.interactionType tells us if this is a genetic or protein interaction, and thus
 * which type of experiment object to create
 *
 * INTERACTOR HOLDER
 * holds the identifier
 * could be gene or protein
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
        //private Map<String, Item> proteins = new HashMap<String, Item>();
        // [participant id] [identifier or accession]
        private Map<String, InteractorHolder> ids = new HashMap<String, InteractorHolder>();
        // [id][experimentholder]
        private Map<String, ExperimentHolder> experimentIds
                                              = new HashMap<String, ExperimentHolder>();
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        private InteractionHolder holder;
        private ExperimentHolder experimentHolder;
        private InteractorHolder interactorHolder;
        private String interactorId;
        private Item organism = null;
        private String organismTaxonId = null, currentParticipantId = null;
        //private Set<String> invalidInteractorIds = new HashSet<String>();

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

            // <experimentList><experimentDescription>
            if (qName.equals("experimentDescription")) {

                experimentHolder = getExperimentHolder(attrs.getValue("id"));

            // <entry><source release="2.0.37" releaseDate="2008-01-25"><names><shortLabel>
            // Interactions for BIOGRID-ORGANISM-7227</shortLabel>
            } else if (qName.equals("shortLabel")
                            && stack.peek().equals("names")
                            && stack.search("source") == 2) {

                attName = "organismTaxonId";

                //  <experimentList><experimentDescription id="2"><names><shortLabel>
            } else if (qName.equals("shortLabel")
                            && stack.peek().equals("names")
                            && stack.search("experimentDescription") == 2) {

                attName = "experimentName";

                //  <experimentList><experimentDescription id="2"><names><fullName>
            } else if (qName.equals("fullName")
                            && stack.peek().equals("names")
                            && stack.search("experimentDescription") == 2) {

                attName = "experimentDescr";

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

            //<experimentList><experimentDescription><interactionDetectionMethod><names><shortLabel>
            } else if (qName.equals("shortLabel")
                            && stack.peek().equals("names")
                            && stack.search("interactionDetectionMethod") == 2) {

                attName = "interactionDetectionMethod";

                // <interactorList><interactor id="4">
            } else if (qName.equals("interactor") && stack.peek().equals("interactorList")) {

                interactorId = attrs.getValue("id");
                interactorHolder = new InteractorHolder(interactorId);
                ids.put(interactorId, interactorHolder);

                // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("interactor") == 2) {

                attName = "secondaryIdentifier";

            // <interactorList><interactor id="4"><xref><primaryRef db="FLYBASE" id="FBgn0000659"
            } else if ((qName.equals("primaryRef") || qName.equals("secondaryRef"))
                            && stack.peek().equals("xref")
                            && stack.search("interactor") == 2) {
                // TODO we aren't using these at the moment
//                if (attrs.getValue("db") != null) {
//                    String dbRef = attrs.getValue("db");
//                    String identifier = attrs.getValue("id");
//                    // store all identifiers as we don't know if this is a gene or a protein, yet
//                    if (dbRef.equalsIgnoreCase("FLYBASE")
//                     || dbRef.equalsIgnoreCase("WormBase")
//                     || dbRef.equalsIgnoreCase("SGD")) {
//                        interactorHolder.primaryIdentifiers.add(identifier);
//                    } else if (dbRef.equalsIgnoreCase("UNIPROTKB")) {
//                        interactorHolder.accessions.add(identifier);
//                    }
//                }

                // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if (qName.equals("organism") && stack.peek().equals("interactor")) {

                String taxId = attrs.getValue("ncbiTaxId");
                if (organism == null) {
                    organism = getOrganism(taxId);

//                } else {
//                    String currentTaxId = organism.getAttribute("taxonId").getValue();
//                    if (!taxId.equals(currentTaxId)) {
//                        LOG.error("Interaction with different organisms found:  " + taxId
//                                  + " and " + currentTaxId);
//                        invalidInteractorIds.add(interactorId);
//                    }
                }
                interactorId = null;
            //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef") && stack.peek().equals("experimentList")) {
                attName = "experimentRef";
                holder = new InteractionHolder();
           //<interactionList><interaction>   <participantList><participant id="68259">
            } else if (qName.equals("participant") && stack.peek().equals("participantList")) {
                currentParticipantId = attrs.getValue("id");
                holder.addInteractor(ids.get(currentParticipantId));
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

            // <experimentList><experimentDescription id="13022"><names><shortLabel>
            if (attName != null && attName.equals("experimentName")
                            && qName.equals("shortLabel")) {
                experimentHolder.shortName = attValue.toString();
            //  <experimentList><experimentDescription id="13022"><names><fullName>
            } else if (attName != null && attName.equals("experimentDescr")
                            && qName.equals("fullName")) {
                experimentHolder.setDescription(attValue.toString());
            //<experimentList><experimentDescription>
            //<interactionDetectionMethod><names><shortLabel>
            } else if (attName != null && attName.equals("interactionDetectionMethod")
                            && qName.equals("shortLabel")) {
                experimentHolder.setMethod(getTerm(attValue.toString()));
            // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if (attName != null && attName.equals("secondaryIdentifier")
                            && qName.equals("shortLabel") && stack.search("interactor") == 2) {

                interactorHolder.secondaryIdentifier = attValue.toString();

            //<participant><interactorRef>
            //<experimentalRoleList><experimentalRole><names><shortLabel>
            } else if (attName != null && attName.equals("role") && qName.equals("shortLabel")
                            && stack.search("experimentalRole") == 2) {
                holder.interactors.get(currentParticipantId).role = attValue.toString();
            //<interactionList><interaction><experimentList><experimentRef>
            } else if (attName != null && attName.equals("experimentRef")
                            && qName.equals("experimentRef")
                            && stack.peek().equals("experimentList")) {
                holder.setExperimentHolder(experimentIds.get(attValue.toString()));
            //<interactionType><names><shortLabel>
            } else if (attName != null && attName.equals("interactionType")) {
                String type = attValue.toString();
                holder.methodRefId = getTerm(type);
//                holder.type = type;
//                if (type.equalsIgnoreCase("Phenotypic Suppression")
//                                || type.equalsIgnoreCase("Phenotypic Enhancement")) {
//                    holder.isGeneticInteraction = true;
//                }
                // we now know what kind of interaction this is, so we can set our identifiers and
                // build the correct objects
                try {
                    setInteractions(holder);
                } catch (ObjectStoreException e) {
                    // TODO something clever
                }

            //<interactionList><interaction>
            } else if (qName.equals("interaction") && holder != null && holder.validActors) {
                try {
                    storeExperiment(holder.eh);
                } catch (ObjectStoreException e) {
                    // TODO something clever here
                }
                storeInteraction(holder);
                holder = null;
            } else if (attName != null && attName.equals("organismTaxonId")) {
                String shortLabel = attValue.toString();
                String[] tokens = shortLabel.split("-");
                organismTaxonId = tokens[2];
            }
        }

        private void storeInteraction(InteractionHolder h) throws SAXException  {

            LinkedHashSet<InteractorHolder> interactors = (LinkedHashSet) h.interactors;

            try {
                Iterator<InteractorHolder> iter = interactors.iterator();
                while (iter.hasNext()) {
                    InteractorHolder ih = iter.next();
                    String refId = ih.refId;

                    Item interaction = null;
                    interaction = createItem("Interaction");
                    if (ih.role != null) {
                        interaction.setAttribute("role", ih.role);
                    }

//                    if (ih.type.equals("Gene")) {
                        interaction.setReference("gene", refId);
                        interaction.setCollection("interactingGenes",
                                                  getInteractingObjects(h, refId));
                        interaction.setReference("type", h.methodRefId);
//                    } else {
//                        interaction.setReference("protein", refId);
//                        interaction.setCollection("interactingProteins",
//                                                  getInteractingObjects(h, refId));
//                    }

                    String interactionName = "";
                    for (String identifier : h.identifiers) {
                        if (!identifier.equals(ih.identifier)) {
                            interactionName += "_" + identifier;
                        } else {
                            interactionName = identifier + interactionName;
                        }
                    }

                    interaction.setReference("experiment", h.eh.experimentRefId);
                    interaction.setAttribute("shortName", interactionName);

                    store(interaction);
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
                    store(pub);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            return itemId;
        }

        private Item getGene(String taxonId, String id, boolean isPrimaryIdentifier)
        throws ObjectStoreException {
            String identifier = id;
            // for Drosophila attempt to update to a current gene identifier
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
                String identifierLabel = "primaryIdentifier";
                if (!taxonId.equals("7227") && !isPrimaryIdentifier) {
                    identifierLabel = "secondaryIdentifier";
                }
                item.setAttribute(identifierLabel, identifier);
                item.setReference("organism", organism.getIdentifier());
                store(item);
                genes.put(identifier, item);
            }
            return item;
        }
// TODO save protein info too?
//        private Item getProtein(String identifier) throws ObjectStoreException {
//            Item item = proteins.get(identifier);
//            if (item == null) {
//                item = createItem("Protein");
//                item.setAttribute("primaryAccession", identifier);
//                item.setReference("organism", organism.getIdentifier());
//                store(item);
//                proteins.put(identifier, item);
//            }
//            return item;
//        }

//        private String getBioEntity(boolean isPrimaryIdentifier, String identifier) {
//            Item bioentity = null;
//            try {
//                if (isPrimaryIdentifier) {
//                    bioentity = getGene(organismTaxonId, identifier, true);
//                } else {
//                    bioentity = getProtein(identifier);
//                }
//                if (bioentity != null) {
//                    return bioentity.getIdentifier();
//                }
//            } catch (ObjectStoreException e) {
//                throw new RuntimeException("error while storing: " + identifier, e);
//            }
//            LOG.error("couldn't match identifier with a partipantid " + identifier);
//            return null;
//        }

        private Item getOrganism(String taxonId)
        throws SAXException {
            try {
                Item item = createItem("Organism");
                item.setAttribute("taxonId", taxonId);
                store(item);
                return item;
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private String getTerm(String name)
        throws SAXException {
            String refId = terms.get(name);
            if (refId != null) {
                return refId;
            }
            try {
                Item item = createItem("ProteinInteractionTerm");
                item.setAttribute("name", name);
                terms.put(name, item.getIdentifier());
                store(item);
                return item.getIdentifier();
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private ExperimentHolder getExperimentHolder(String experimentId) {
            ExperimentHolder eh =  experimentIds.get(experimentId);
            if (eh == null) {
                eh = new ExperimentHolder(experimentId);
                experimentIds.put(experimentId, eh);
            }
            return eh;
        }

        private void storeExperiment(ExperimentHolder eh)
        throws ObjectStoreException {
            if (!eh.isStored) {
                Item exp = null;
                exp = createItem("InteractionExperiment");
                exp.setReference("interactionDetectionMethod", eh.methodRefId);
                exp.setAttribute("name", eh.shortName);
                exp.setReference("publication", eh.pubRefId);
                store(exp);
                eh.isStored = true;
                eh.experimentRefId = exp.getIdentifier();
            }
        }

        private ArrayList<String> getInteractingObjects(InteractionHolder interactionHolder,
                                                        String refId) {
            ArrayList<String> interactorIds = new ArrayList(interactionHolder.refIds);
            interactorIds.remove(refId);
            return interactorIds;
        }

        /**
         * we now know what kind of interaction this is, so we can fetch our identifiers from
         * the correct maps.
         *
         * we add the identifiers to a list just for convenience. this list will later be used
         * to build the indentifier for the entire interaction.
         */
        private void setInteractions(InteractionHolder h) throws ObjectStoreException {

            for (InteractorHolder ih : h.interactors.values()) {
                String refId = null;
                Item item = getGene(organismTaxonId, ih.secondaryIdentifier, false);
                if (item != null) {
                    refId = item.getIdentifier();
                    ih.refId = refId;
                    h.refIds.add(refId);
                    ih.identifier = ih.secondaryIdentifier;
                    h.identifiers.add(ih.secondaryIdentifier);
                } else {
                    h.validActors = false;
                    String msg = "could not resolve bioentity == "
                    + ih.secondaryIdentifier + ", participantId: " + ih.participantId;
                    LOG.error(msg);
                }
            }
        }

        /**
         * Holder object for GeneInteraction.  Holds all information about an interaction until
         * ready to store
         * @author Julie Sullivan
         */
        public class InteractionHolder
        {
            protected ExperimentHolder eh;
            protected Map<String, InteractorHolder> interactors
                                                        = new HashMap<String, InteractorHolder>();
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
             * @param ih object holding interactor
             */
            protected void addInteractor(InteractorHolder ih) {
                interactors.put(ih.participantId, ih);
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
            protected String participantId;
            protected String identifier;    // same as secondaryIdentifier right now
            protected String refId;
            protected List<String> accessions = new ArrayList<String>(); // obsolete?
            protected List<String> primaryIdentifiers = new ArrayList<String>(); // obsolete?
            protected String secondaryIdentifier;


            /**
             * Constructor
             * @param participantId bioGRID id
             */
            public InteractorHolder(String participantId) {
                this.participantId = participantId;
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
