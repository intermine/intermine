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
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    static {
        PSI_TERMS.put("Biochemical Activity", "biochemical");
        PSI_TERMS.put("Co-localization", "colocalization");
        PSI_TERMS.put("Co-purification", "copurification");
        PSI_TERMS.put("Far Western", "far western blotting");
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
        private Map<String, String> genes = new HashMap();
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

            // <interactorList><interactor id="4"><xref>
            // <secondaryRef db="SGD"  id="S000006331" secondary="YPR127W"/>
            } else if ((qName.equals("primaryRef") || qName.equals("secondaryRef"))
                            && stack.search("interactor") == 2) {
                String db = attrs.getValue("db");
                if (db != null && (db.equalsIgnoreCase("sgd") || db.equalsIgnoreCase("flybase")
                                || db.equalsIgnoreCase("wormbase"))) {
                    interactorHolder.primaryIdentifier = attrs.getValue("id");
                }

              // <interactorList><interactor id="4"><names><shortLabel>YFL039C</shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("interactor") == 2) {

                attName = "secondaryIdentifier";

             // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if (qName.equals("organism") && stack.peek().equals("interactor")) {
                String taxId = attrs.getValue("ncbiTaxId");
                try {
                    interactorHolder.organismRefId = getOrganism(taxId);
                    setGene(taxId, interactorHolder);
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
                // TODO make sure this is necessary.  interactor id is reused?
                ih.role = null;
                // resolver didn't return valid identifier
                if (ih.refId == null) {
                    ih.valid = false;
                    holder.validActors = false;
                } else {
                    holder.refIds.add(ih.refId);
                    holder.identifiers.add(ih.identifier);
                    holder.addInteractor(participantId, ih);
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
                String term = attValue.toString();
                if (term != null) {
                    experimentHolder.setMethod(getTerm(term));
                }

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
            } else if (attName != null && attName.equals("interactionType")
                            && qName.equals("shortLabel")) {
                String term = attValue.toString();
                holder.methodRefId = getTerm(term);
                if (term.equalsIgnoreCase("phenotypic enhancement")
                                || term.equalsIgnoreCase("phenotypic suppression")) {
                    holder.interactionType = "genetic";
                }
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
                interaction.setAttribute("interactionType", h.interactionType);
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
         * create/store gene (if new)
         * @param taxonId id of organism for this gene
         * @param ih interactor holder
         * @throws ObjectStoreException
         */
        private void setGene(String taxonId, InteractorHolder ih)
        throws ObjectStoreException {

            IdResolver resolver = resolverFactory.getIdResolver(false);

            // try primaryIdentifier
            String label = "primaryIdentifier";
            String identifier = resolveGene(resolver, taxonId, ih.primaryIdentifier);

            // try again
            if (identifier == null) {
                if (!taxonId.equals("7227")) {  // resolver returns primaryIdentifier
                    label = "secondaryIdentifier";
                }
                identifier = resolveGene(resolver, taxonId, ih.secondaryIdentifier);
            }

            // no valid identifiers
            if (identifier == null) {
                ih.valid = false;
                LOG.error("could not resolve bioentity == " + identifier + ", participantId: "
                          + ih.biogridId);
                return;
            }

            String refId = genes.get(identifier);
            if (refId == null) {
                Item item = createItem("Gene");
                item.setAttribute(label, identifier);
                item.setReference("organism", getOrganism(taxonId));
                store(item);
                refId = item.getIdentifier();
                genes.put(identifier, refId);
            }

            ih.identifier = identifier;
            ih.refId = refId;

            return;
        }


        /**
         * resolve dmel genes
         * @param taxonId id of organism for this gene
         * @param ih interactor holder
         * @throws ObjectStoreException
         */
        private String resolveGene(IdResolver resolver, String taxonId, String identifier) {
            String id = identifier;
            if (taxonId.equals("7227") && resolver != null) {
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

        private String getTerm(String name)
        throws SAXException {
            String term = name;
            if (PSI_TERMS.get(term) != null) {
                term = PSI_TERMS.get(term);
            } else {
                term = term.toLowerCase();
            }
            String refId = terms.get(term);
            if (refId != null) {
                return refId;
            }

            Item item = createItem("InteractionTerm");
            item.setAttribute("name", term);
            terms.put(term, item.getIdentifier());
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return item.getIdentifier();
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
            protected String interactionType = "physical";

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
            protected String primaryIdentifier;
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
