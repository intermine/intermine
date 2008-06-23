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
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse biogrid data into items
 * @author Julie Sullivan
 */
public class BioGridConverter extends BioFileConverter
{
    private Map<String, String> pubs = new HashMap<String, String>();
    private static final Logger LOG = Logger.getLogger(BioGridConverter.class);
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static Map<String, String> masterList = new HashMap<String, String>();
    protected IdResolverFactory resolverFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public BioGridConverter(ItemWriter writer, Model model) {
        super(writer, model, "BioGRID", "BioGRID data set");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();
    }

    /**
     * {@inheritDoc}final
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
        private Map<String, String> aliases = new HashMap<String, String>();
        private Map<String, Item> genes = new HashMap<String, Item>();
        private Map<String, String> geneIdsToIdentifiers = new HashMap<String, String>();
        // [experiment name] [experiment holder]
        private Map<String, ExperimentHolder> experimentNames
        = new HashMap<String, ExperimentHolder>();
        // [id][experimentholder]
        private Map<String, ExperimentHolder> experimentIds
        = new HashMap<String, ExperimentHolder>();
        private InteractorHolder interactorHolder;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        private InteractionHolder holder;
        private ExperimentHolder experimentHolder;
        private String geneId;
        private String experimentId;
        private Item organism = null;
        private Set<String> storedItems = new HashSet<String>();
        private String organismTaxonId = null;
        private Set<String> invalidInteractorIds = new HashSet<String>();

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

                experimentId = attrs.getValue("id");

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

            // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if (qName.equals("organism") && stack.peek().equals("interactor")) {

                String taxId = attrs.getValue("ncbiTaxId");
                if (organism == null) {
                    organism = getOrganism(taxId);
                } else {
                    String currentTaxId = organism.getAttribute("taxonId").getValue();
                    if (!taxId.equals(currentTaxId)) {
                        LOG.error("Interaction with different organisms found:  " + taxId
                                  + " and " + currentTaxId);
                        invalidInteractorIds.add(geneId);
                    }
                }

            // <interactorList><interactor id="4">
            } else if (qName.equals("interactor") && stack.peek().equals("interactorList")) {

                geneId = attrs.getValue("id");

            // <interactorList><interactor id="4"><xref><primaryRef db="FLYBASE" id="FBgn0000659"
            } else if ((qName.equals("primaryRef") || qName.equals("secondaryRef"))
                            && stack.peek().equals("xref")
                            && stack.search("interactor") == 2) {

                if (attrs.getValue("db") != null) {
                    String dbRef = attrs.getValue("db");
                    // TODO this should go in a properties file
                    if ((organismTaxonId.equals("7227")
                         && dbRef.equalsIgnoreCase("FLYBASE"))
                                    || (organismTaxonId.equals("6239")
                                    && dbRef.equalsIgnoreCase("WormBase"))
                                                    || (organismTaxonId.equals("4932")
                                                    && dbRef.equalsIgnoreCase("SGD"))) {
                        String identifier = attrs.getValue("id");
                        geneIdsToIdentifiers.put(geneId, identifier);
                        getGene(organismTaxonId, identifier);
                    }
                }

//                <participant id="62692">
//                <interactorRef>62692</interactorRef>
//                <experimentalRoleList><experimentalRole><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("experimentalRole") == 2) {

                attName = "role";

                //<interactionList><interaction>
                //<participantList><participant id="5">
            } else if (qName.equals("interactorRef") && stack.peek().equals("participant")) {

                attName = "participantId";

            //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef")
                       && stack.peek().equals("experimentList")) {

                attName = "experimentRef";
                holder = new InteractionHolder();

            //<interactionList><interaction><interactionType><names><shortLabel
            } else if (qName.equals("shortLabel")
                            && stack.peek().equals("names")
                            && stack.search("interactionType") == 2) {

                attName = "interactionType";

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
//             <experimentList><experimentDescription id="13022">
//            <names><shortLabel>Giot L (2003)</shortLabel>
                if (attName != null
                                && attName.equals("experimentName")
                                && qName.equals("shortLabel")) {

                    String shortLabel = attValue.toString();

                    experimentHolder = checkExperiment(shortLabel);
                    experimentIds.put(experimentId, experimentHolder);
                    experimentHolder.setName(shortLabel);

                //  <experimentList><experimentDescription id="13022"><names><fullName>
                } else if (attName != null
                                    && attName.equals("experimentDescr")
                                    && qName.equals("fullName")) {

                    experimentHolder.setDescription(attValue.toString());

                //<interactionList><interaction>
                //<participantList><participant id="5"><interactorRef>
                } else if (qName.equals("interactorRef")
                                && stack.peek().equals("participant")) {

                    String id = attValue.toString();

                    // we don't have a good ID for human genes so don't store those
                    // interactions
                    if (invalidInteractorIds.contains(id)) {
                        holder.validActors = false;
                    } else {

                        String identifier = geneIdsToIdentifiers.get(id);
                        Item gene = null;
                        if (identifier != null) {
                            gene = getGene(organismTaxonId, identifier);
                        }
                        if (gene != null) {
                            interactorHolder = new InteractorHolder(gene, identifier);
                            holder.addInteractor(interactorHolder);
                            holder.addGene(gene.getIdentifier(), identifier);
                        } else {
                            holder.validActors = false;
                            LOG.error("Gene/protein not found - " + identifier + " ( " + id + ")");
                        }
                    }
//          <participant><interactorRef><experimentalRoleList><experimentalRole><names><shortLabel>
                } else if (attName != null && attName.equals("role")
                                && qName.equals("shortLabel")) {
                    if (interactorHolder != null) {
                                interactorHolder.role = attValue.toString();
                    }
                //<interactionList><interaction><experimentList><experimentRef>
                } else if (qName.equals("experimentRef")
                                && stack.peek().equals("experimentList")) {

                    String experimentRef = attValue.toString();
                    holder.setExperiment(experimentIds.get(experimentRef));

                //<interactionType><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && attName != null
                                && attName.equals("interactionType")) {

                    String type = attValue.toString();
                    holder.type = type;
                    if (type.equalsIgnoreCase("Phenotypic Suppression")
                                    || type.equalsIgnoreCase("Phenotypic Enhancement")) {
                        holder.validExperiment = true;
                    }

                //<interactionList><interaction>
                } else if (qName.equals("interaction")
                                && holder != null
                                && holder.validExperiment
                                && holder.validActors) {

                    /* done processing everything for this interaction */
                    storeAll(holder);
                    holder = null;
                    interactorHolder = null;
                    //experimentHolder = null;

                } else if (attName != null && attName.equals("organismTaxonId")) {
                    String shortLabel = attValue.toString();
                    String[] tokens = shortLabel.split("-");
                    organismTaxonId = tokens[2];
                }
        }

        private void storeAll(InteractionHolder interactionHolder) throws SAXException  {

            Set<InteractorHolder> interactors = interactionHolder.interactors;

            try {

                // loop through genes/interactors in this interaction
                for (Iterator<InteractorHolder> iter = interactors.iterator(); iter.hasNext();) {

                    interactorHolder =  iter.next();
                    Item gene = interactorHolder.gene;
                    String geneRefId = gene.getIdentifier();

                    String interactionName = "";
                    for (String identifier : interactionHolder.geneIdentifiers) {

                        if (!identifier.equals(interactorHolder.identifier)) {
                            interactionName += "_" + identifier;
                        } else {
                            interactionName = identifier + interactionName;
                        }
                    }

                    // build & store interactions - one for each gene
                    Item interaction = createItem("GeneticInteraction");


                    interaction.setAttribute("shortName", interactionName);
                    interaction.setAttribute("type", interactionHolder.type);

                    interaction.setReference("gene", geneRefId);
                    interaction.setReference("experiment",
                           interactionHolder.eh.experiment.getIdentifier());

                    // interactingGenes
                    Set<String> geneIds = interactionHolder.geneIds;
                    geneIds.remove(geneRefId);
                    ReferenceList geneList = new ReferenceList("interactingGenes",
                                                                  new ArrayList<String>());
                    for (Iterator<String> it = geneIds.iterator(); it.hasNext();) {
                        geneList.addRefId(it.next());
                    }
                    interaction.addCollection(geneList);
                    geneIds.add(geneRefId);

                    /* store all interaction-related items */
                    if (!storedItems.contains(gene.getAttribute("primaryIdentifier").getValue())) {
                        gene.setReference("organism", organism.getIdentifier());
                        store(gene);
                        storedItems.add(gene.getAttribute("primaryIdentifier").getValue());
                    }

                    interaction.setAttribute("geneRole", interactorHolder.role);
                    store(interaction);
                }

                ExperimentHolder eh = interactionHolder.eh;
                if (!eh.isStored) {
                    eh.isStored = true;
                    store(eh.experiment);
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

        private Item getGene(String taxonId, String id) {
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
                item.setAttribute("primaryIdentifier", identifier);
                genes.put(identifier, item);
            }
            return item;
        }

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

        private ExperimentHolder checkExperiment(String name) {

            ExperimentHolder eh = experimentNames.get(name);
            if (eh == null) {
                Item exp = createItem("GeneticInteractionExperiment");
                eh = new ExperimentHolder(exp);
                experimentNames.put(name, eh);
            }
            return eh;
        }

//        private String newId(String className) {
//            Integer id = ids.get(className);
//            if (id == null) {
//                id = new Integer(0);
//                ids.put(className, id);
//            }
//            id = new Integer(id.intValue() + 1);
//            ids.put(className, id);
//            return id.toString();
//        }

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
         * Holder object for GeneInteraction.  Holds all information about an interaction until
         * ready to store
         * @author Julie Sullivan
         */
        public class InteractionHolder
        {
            protected String type;
            protected ExperimentHolder eh;
            protected Set<InteractorHolder> interactors = new LinkedHashSet<InteractorHolder>();
            protected Set<String> geneIds = new HashSet<String>();
            protected Set<String> geneIdentifiers = new HashSet<String>();
            protected boolean validExperiment = false;
            protected boolean validActors = true;

            /**
             * @param eh object holding experiment object
             */
            protected void setExperiment(ExperimentHolder eh) {
                this.eh = eh;
            }

            /**
             * @param ih object holding interactor
             */
            protected void addInteractor(InteractorHolder ih) {
                interactors.add(ih);
            }

            /**
             * @param identifier FBgn for a gene - used to create the shortName of the interaction
             * @param id reference to gene item involved in interaction
             */
            protected void addGene(String id, String identifier) {
                geneIds.add(id);
                geneIdentifiers.add(identifier);
            }
        }


        /**
         * Holder object for GeneInteractor. Holds id and identifier for gene until the experiment
         * is verified as a gene interaction and not a protein interaction
         * @author Julie Sullivan
         */
        protected class InteractorHolder
        {
            protected String identifier;    // FBgn
            protected Item gene;
            protected String role;

            /**
             * Constructor
             * @param gene Gene that's part of the interaction
             * @param identifier of the gene
             */
            public InteractorHolder(Item gene, String identifier) {
                this.gene = gene;
                this.identifier = identifier;
            }
        }

        /**
         * Holder object for Experiment.  Holds all information about an experiment until
         * an interaction is verified to have only valid organisms
         * @author Julie Sullivan
         */
        protected class ExperimentHolder
        {
            protected Item experiment;
            protected boolean isStored = false;

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
            }

            /**
            *
            * @param description the full name of the experiments
            */
           protected void setDescription(String description) {
               experiment.setAttribute("description", description);
           }

            /**
             *
             * @param publication publication of this experiment
             */
            protected void setPublication(String publication) {
                experiment.setReference("publication", publication);
            }
        }
    }
}
