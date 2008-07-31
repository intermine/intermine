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
 * DataConverter to parse psi data into items
 * @author Julie Sullivan
 */
public class PimConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(PimConverter.class);
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Object> experimentNames = new HashMap<String, Object>();
    private Map<String, String> organisms = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();
    private String termId = null, organismId = null;
    private Map<String, Item> genes = new  HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private static final String TAXONID = "7227";
    private static final String EXPERIMENT = "Hgx PIM";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PimConverter(ItemWriter writer, Model model) {
        super(writer, model, "Pim", "Pim dataset");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();

        try {
            termId = getTerm("MI:0117");
            organismId = getOrganism(TAXONID);
        } catch (SAXException e) {
            throw new RuntimeException("ack");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        PimHandler handler = new PimHandler();

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
    class PimHandler extends DefaultHandler
    {
        private Map<String, ExperimentHolder> experimentIds
            = new HashMap<String, ExperimentHolder>();
        private InteractionHolder holder = null;
        private ExperimentHolder experimentHolder = null;
        private InteractorHolder interactorHolder = null;
        private String experimentId = null, interactorId = null;

        private Map<String, Item> validGenes = new HashMap<String, Item>();
        private String regionName = null;

        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;

        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
            attName = null;

            //<experimentList> <experimentDescription id="hgx_experiment">
            if (qName.equals("experimentDescription")) {
                experimentId = attrs.getValue("id");
            //  <experimentList><experimentDescription id="2"><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("experimentDescription") == 2) {
                attName = "experimentName";
            //<experimentList><experimentDescription><bibref><xref><primaryRef>
            } else if (qName.equals("primaryRef") && stack.peek().equals("xref")
                            && stack.search("bibref") == 2
                            && stack.search("experimentDescription") == 3) {
                String pubMedId = attrs.getValue("id");
                if (StringUtil.allDigits(pubMedId)) {
                    String pub = getPub(pubMedId);
                    experimentHolder.setPublication(pub);
                }

            // <hostOrganismList><hostOrganism ncbiTaxId="9534">
            } else if (qName.equals("hostOrganism") && stack.peek().equals("names")) {
                String hostOrganism = attrs.getValue("ncbiTaxId");
                if (hostOrganism != null) {
                    String refId = getOrganism(hostOrganism);
                    experimentHolder.setHostOrganism(refId);
                } else {
                    LOG.info("Experiment " + experimentHolder.name
                             + " doesn't have a host organism");
                }

            //<interactionDetectionMethod><xref><primaryRef>
            } else if (qName.equals("primaryRef") && stack.peek().equals("xref")
                            && stack.search("interactionDetectionMethod") == 2) {
                String termItemId = getTerm(attrs.getValue("id"));
                experimentHolder.setMethod("interactionDetectionMethod", termItemId);

                //TODO text?
                //<participantIdentificationMethod><xref> <primaryRef>
//            } else if (qName.equals("primaryRef") && stack.peek().equals("xref")
//                            && stack.search("participantIdentificationMethod") == 2) {
//                String termItemId = getTerm(attrs.getValue("id"));
//                experimentHolder.setMethod("participantIdentificationMethod", termItemId);


                // ------------- interactors ------------ //

            // <interactorList> <proteinInteractor id="cds-1">
            } else if (qName.equals("proteinInteractor") && stack.peek().equals("interactorList")) {
                interactorId = attrs.getValue("id");

            // <proteinInteractor id="cds-1289"><xref>
            // <secondaryRef db="FlyBase V3" id="FBgn0032414" />
            } else if (qName.equals("secondaryRef") && stack.peek().equals("xref")
                            && stack.search("proteinInteractor") == 2) {
                String db = attrs.getValue("db");
                if (db != null && db.startsWith("FlyBase")) {
                    String id = attrs.getValue("id");
                    Item gene = null;
                    try {
                        gene = getGene(id);
                    } catch (ObjectStoreException e) {
                        //throw new RuntimeException("id " + id);
                    }
                    if (gene != null && !validGenes.containsKey(interactorId)) {
                        validGenes.put(interactorId, gene);
                    }
                }

                // ----------- interaction --------------------- //

           //<interactionList><interaction>
           //<participantList><proteinParticipant><proteinInteractorRef ref="cds-1" />
            } else if (qName.equals("proteinInteractorRef")
                            && stack.peek().equals("proteinParticipant")) {

                    String id = attrs.getValue("ref");
                    if (validGenes.get(id) != null) {
                        Item interactor = validGenes.get(id);
                        String geneRefId = interactor.getIdentifier();
                        interactorHolder = new InteractorHolder(geneRefId);
                        String ident = null;
                        if (interactor.getAttribute("primaryIdentifier") != null) {
                            ident = interactor.getAttribute("primaryIdentifier").getValue();
                        }
                        if (ident != null) {
                            interactorHolder.identifier = ident;
                            holder.addInteractor(interactorHolder);
                            holder.addProtein(geneRefId);
                        } else {
                            holder.isValid = false;
                        }
                    } else {
                        holder.isValid = false;
                    }


            // <participantList><participant id="5"><experimentalRole><names><shortLabel>
            } else if (qName.equals("role")) {
                attName = "role";
            //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef") && stack.peek().equals("experimentList")) {
                String experimentRef = attrs.getValue("ref");
                holder = new InteractionHolder(experimentIds.get(experimentRef));
            // <participantList><participant id="6919"><featureList><feature id="6920">
            //    <featureRangeList><featureRange><startStatus><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("startStatus") == 2) {
                attName = "startStatus";
                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><endStatus><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("endStatus") == 2) {
                attName = "endStatus";
                // <featureList><feature id="24"><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("feature") == 2) {
                attName = "regionName";
                // <participantList><participant id="6919"><featureList><feature id="6920">
                // <featureType><xref><primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0117"
            } else if (qName.equals("primaryRef") && stack.search("featureType") == 2
                            && attrs.getValue("id").equals("MI:0117") && interactorHolder != null) {
                interactorHolder.isRegionFeature = true;
                // create interacting region
                Item interactionRegion = createItem("InteractionRegion");
                interactionRegion.setAttribute("name", regionName);
                interactionRegion.setReference("gene", interactorHolder.geneRefId);
                interactionRegion.setReference("ontologyTerm", termId);

                // create new location object (start and end are coming later)
                Item location = createItem("Location");
                location.setReference("object", interactorHolder.geneRefId);
                location.setReference("subject", interactionRegion.getIdentifier());

                interactionRegion.setReference("location", location);

                // add location and region to interaction object
                interactorHolder.interactionRegion = interactionRegion;
                interactorHolder.location = location;

                holder.addRegion(interactionRegion.getIdentifier());

                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><begin position="470"/>
            } else if (qName.equals("begin")
                            && stack.peek().equals("featureRange")
                            && interactorHolder != null
                            && interactorHolder.isRegionFeature) {
                interactorHolder.setStart(attrs.getValue("position"));
                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><end position="470"/>
            } else if (qName.equals("end")
                            && stack.peek().equals("featureRange")
                            && interactorHolder != null
                            && interactorHolder.isRegionFeature) {
                interactorHolder.setEnd(attrs.getValue("position"));
                //<confidence unit="Hybrigenics PBS(r)" value="D" />
            } else if (qName.equals("confidence") && holder != null) {
                holder.confidenceUnit = attrs.getValue("unit");
                holder.setConfidence(attrs.getValue("value"));
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
            // <experimentList><experimentDescription><names><shortLabel>
            if (attName != null && attName.equals("experimentName")
                            && qName.equals("shortLabel")) {
                String shortLabel = attValue.toString();
                if (shortLabel != null) {
                    experimentHolder = getExperiment(shortLabel);
                    experimentIds.put(experimentId, experimentHolder);
                    experimentHolder.setName(shortLabel);
                } else {
                    LOG.error("Experiment " + experimentId + " doesn't have a shortLabel");
                }

                // ---  interactions --- ///

            // <interactionList><interaction><participantList><participant id="5">
            // <experimentalRole><names><shortLabel>
            } else if (qName.equals("role") && interactorHolder != null) {
                interactorHolder.role = attValue.toString();
            // <participantList><participant id="6919"><featureList><feature id="6920">
            //    <featureRangeList><featureRange><startStatus><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("startStatus") == 2
                            && interactorHolder != null && interactorHolder.isRegionFeature) {
                interactorHolder.startStatus = attValue.toString();
                // <participantList><participant id="6919"><featureList><feature id="6920">
                //    <featureRangeList><featureRange><endStatus><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("endStatus") == 2
                            && interactorHolder != null && interactorHolder.isRegionFeature) {
                interactorHolder.endStatus = attValue.toString();
                //     <featureList><feature id="24"><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("feature") == 2
                            && attName != null && attName.equals("regionName")) {
                regionName  = attValue.toString();
                //<interactionList><interaction>
            } else if (qName.equals("interaction") && holder != null) {
                if (holder.isValid) {
                    storeAll(holder);
                    holder = null;
                    interactorHolder = null;
                }
            }
        }

        private void storeAll(InteractionHolder interactionHolder) throws SAXException  {
            Set interactors = interactionHolder.interactors;
            // loop through proteins/interactors in this interaction
            for (Iterator iter = interactors.iterator(); iter.hasNext();) {

                interactorHolder =  (InteractorHolder) iter.next();

                // build & store interactions - one for each protein
                Item interaction = createItem("Interaction");
                String geneRefId = interactorHolder.geneRefId;
                String identifier = interactorHolder.identifier;
                //String shortName = interactionHolder.shortName;
                interaction.setAttribute("shortName", EXPERIMENT);
                interaction.setAttribute("role", interactorHolder.role);

                if (interactionHolder.confidence != null) {
                    interaction.setAttribute("confidence",
                                             interactionHolder.confidence.toString());
                }
                if (interactionHolder.confidenceText != null) {
                    interaction.setAttribute("confidenceText",
                                             interactionHolder.confidenceText);
                }
                interaction.setReference("gene", geneRefId);
                interaction.setReference("experiment",
                                         interactionHolder.eh.experiment.getIdentifier());

                // interactingProteins
                List<String> geneIds = new ArrayList(interactionHolder.geneIds);
                geneIds.remove(geneRefId);
                interaction.setCollection("interactingGenes", geneIds);

                // interactingRegions
                Set<String> regionIds = interactionHolder.regionIds;
                if (!regionIds.isEmpty()) {
                    interaction.setCollection("interactingRegions", new ArrayList(regionIds));
                }

                /* store all protein interaction-related items */
                try {
                    store(interaction);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }

                if (interactorHolder.interactionRegion != null) {
                    Item region = interactorHolder.interactionRegion;
                    if (interactorHolder.startStatus != null) {
                        region.setAttribute("startStatus", interactorHolder.startStatus);
                    }
                    if (interactorHolder.endStatus != null) {
                        region.setAttribute("endStatus", interactorHolder.endStatus);
                    }
                    region.setReference("interaction", interaction);

                    String regionIdentifier = EXPERIMENT + "_" + identifier;

                    if (interactorHolder.start != null && !interactorHolder.start.equals("0")) {
                        regionIdentifier += ":" + interactorHolder.start;
                        regionIdentifier += "-" + interactorHolder.end;
                    }
                    region.setAttribute("primaryIdentifier", regionIdentifier);
                    try {
                        store(region);
                        store(interactorHolder.location);
                    } catch (ObjectStoreException e) {
                        //
                    }
                }

            }

            /* store all experiment-related items */
            ExperimentHolder eh = interactionHolder.eh;
            // TODO is this experiment going to have extra items to store, the 2nd time it
            // gets processed?  In the other XML files?
            if (!eh.isStored) {
                eh.isStored = true;
                try {
                    store(eh.experiment);
                } catch (ObjectStoreException e) {
                    //
                }
                //TODO store comments here instead
                //for (Object o : eh.comments) {
                //    writer.store(ItemHelper.convert((Item) o));
                //}
            }
        }

        private Item getGene(String fbgn) throws ObjectStoreException {
            String identifier = fbgn;

            // for Drosophila attempt to update to a current gene identifier
            IdResolver resolver = resolverFactory.getIdResolver(false);
            if (resolver != null) {
                int resCount = resolver.countResolutions(TAXONID, identifier);
                if (resCount != 1) {
                    LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                             + identifier + " count: " + resCount + " FBgn: "
                             + resolver.resolveId(TAXONID, identifier));
                    return null;
                }
                identifier = resolver.resolveId(TAXONID, identifier).iterator().next();
            }

            Item item = genes.get(identifier);
            if (item == null) {
                item = createItem("Gene");
                item.setAttribute("primaryIdentifier", identifier);
                item.setReference("organism", organismId);
                genes.put(identifier, item);
                store(item);
            }
            return item;
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

        private ExperimentHolder getExperiment(String name) {
            ExperimentHolder eh = (ExperimentHolder) experimentNames.get(name);
            if (eh == null) {
                eh = new ExperimentHolder(createItem("InteractionExperiment"));
                experimentNames.put(name, eh);
            }
            return eh;
        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about an interaction until
         * it's verified that all organisms are in the list given.
         * @author Julie Sullivan
         */
        public class InteractionHolder
        {
            private ExperimentHolder eh;
            private Double confidence;
            private String confidenceText;
            private String confidenceUnit;
            private Set<InteractorHolder> interactors = new LinkedHashSet<InteractorHolder>();
            private boolean isValid = true;
            private Set<String> geneIds = new HashSet<String>();
            private Set<String> regionIds = new HashSet<String>();

            /**
             * Constructor
             * @param shortName name of this interaction
             */
            public InteractionHolder(ExperimentHolder eh) {
                this.eh = eh;
            }

            /**
             *
             * @param confidence confidence score for interaction
             */
            protected void setConfidence(String confidence) {
                if (Character.isDigit(confidence.charAt(0))) {
                    this.confidence = new Double(confidence);
                } else {
                    // if confidencetext has a value, concatenate
                    confidenceText = (confidenceText != null
                                    ? confidenceText + confidence : confidence);
                }
            }

            /**
             *
             * @param ih object holding interactor
             */
            protected void addInteractor(InteractorHolder ih) {
                interactors.add(ih);
            }

            /**
             * @param geneId protein involved in interaction
             */
            protected void addProtein(String geneId) {
                geneIds.add(geneId);
            }

            /**
             *
             * @param regionId Id of ProteinInteractionRegion object
             */
            protected void addRegion(String regionId) {
                regionIds.add(regionId);
            }
        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about a gene in an
         * interaction until it's verified that all organisms are in the list given.
         * @author Julie Sullivan
         */
        public class InteractorHolder
        {
            private String geneRefId;   // protein.getIdentifier()
            private String role;
            private Item interactionRegion; // for storage later
            private Item location;          // for storage later
            private String startStatus, start;
            private String endStatus, end;
            protected String identifier;

            /* we only want to process the binding site feature.  this flag is FALSE until
             *
             * <participantList><participant id="6919"><featureList>
             * <feature id="6920"><featureType><xref>
             * <primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0117"
             * id="MI:0117" (the id for binding site)
             *
             * then the flag is set to TRUE until </feature>
             */
            // TODO this isn't enforced, I think
            private boolean isRegionFeature;

            /**
             * Constructor
             * @param geneId Protein that's part of the interaction
             */
            public InteractorHolder(String geneId) {
                this.geneRefId = geneId;
            }

            /**
             * @param start start position of region
             */
            protected void setStart(String start) {
                location.setAttribute("start", start);
                this.start = start;
            }

            /**
             * @param end the end position of the region
             */
            protected void setEnd(String end) {
                location.setAttribute("end", end);
                this.end = end;
            }

        }

        /**
         * Holder object for ProteinInteraction.  Holds all information about an experiment until
         * an interaction is verified to have only valid organisms
         * @author Julie Sullivan
         */
        public class ExperimentHolder
        {

            protected String name;
            protected Item experiment;
            protected HashSet comments = new HashSet();
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
                this.name = name;
            }

            /**
             *
             * @param publication publication of this experiment
             */
            protected void setPublication(String publication) {
                experiment.setReference("publication", publication);
            }

            /**
             *
             * @param whichMethod method
             * @param termItemId termID
             */
            protected void setMethod(String whichMethod, String termItemId) {
                experiment.setReference(whichMethod, termItemId);
            }

            /**
             *
             * @param fullName name of organism
             */
            protected void setHostOrganism(String fullName) {
                experiment.setAttribute("hostOrganism", fullName);
            }
        }
    }

    /**
     * create and store protein interaction terms
     * @param identifier identifier for interaction term
     * @return id representing term object
     * @throws SAXException if term can't be stored
     */
    protected String getTerm(String identifier) throws SAXException {
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


    private String getOrganism(String taxId) throws SAXException {
        String refId = organisms.get(taxId);
        if (refId != null) {
            return refId;
        }

        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", taxId);
        try {
            store(organism);
        } catch (ObjectStoreException e) {
            throw new SAXException(e);
        }
        refId = organism.getIdentifier();
        organisms.put(taxId, refId);
        return refId;
    }
}
