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
    private Map<String, String> organisms = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();
    private String termId = null, organismId = null;
    private Map<String, Item> genes = new  HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private static final String TAXONID = "7227";
    private static final String EXPERIMENT = "formstecher-2005-1";
    private static final String PUBMEDID = "15710747";
    private String experimentId = null, interactionTermId;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PimConverter(ItemWriter writer, Model model) {
        super(writer, model, "PIMRider", "PIMRider data set");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();

        try {
            termId = getTerm("MI:0117");
            interactionTermId = getTerm("MI:0218");
            organismId = getOrganism(TAXONID);
            experimentId = getExperiment();
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
        private InteractionHolder holder = new InteractionHolder();
        private InteractorHolder interactorHolder = null;
        private String interactorId = null;

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

                // ------------- interactors ------------ //

            // <interactorList> <proteinInteractor id="cds-1">
            if (qName.equals("proteinInteractor") && stack.peek().equals("interactorList")) {
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
                            holder.addIdentifier(ident);
                        } else {
                            holder.isValid = false;
                        }
                    } else {
                        holder.isValid = false;
                    }


            // <participantList><participant id="5"><experimentalRole><names><shortLabel>
            } else if (qName.equals("role")) {
                attName = "role";

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

                // ---  interactions --- ///

            // <interactionList><interaction><participantList><participant id="5">
            // <experimentalRole><names><shortLabel>
            if (qName.equals("role") && interactorHolder != null) {

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
                    holder = new InteractionHolder();
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

                String interactionName = buildName(interactionHolder.identifiers, identifier);
                interaction.setAttribute("name", interactionName);

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
                interaction.setReference("experiment", experimentId);
                interaction.setReference("type", interactionTermId);

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
        }

        private String buildName(List<String> primaryIdentifiers, String identifier) {
            String name = "PIMRider:" + identifier;
            Iterator it = primaryIdentifiers.iterator();
            while (it.hasNext()) {
                String primaryIdentifier = (String) it.next();
                if (!primaryIdentifier.equals(identifier)) {
                    name = name + "_" + primaryIdentifier;
                }
            }
            return name;
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


        /**
         * Holder object for ProteinInteraction.  Holds all information about an interaction until
         * it's verified that all organisms are in the list given.
         * @author Julie Sullivan
         */
        public class InteractionHolder
        {
            private Double confidence;
            private String confidenceText;
            protected String confidenceUnit;
            private Set<InteractorHolder> interactors = new LinkedHashSet<InteractorHolder>();
            private boolean isValid = true;
            private Set<String> geneIds = new HashSet<String>();
            private Set<String> regionIds = new HashSet<String>();
            private List<String> identifiers = new ArrayList<String>();

            /**
             * Constructor
             * @param shortName name of this interaction
             */
            public InteractionHolder() {
                //nothing to do
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
             * @param regionId Id of ProteinInteractionRegion object
             */
            protected void addRegion(String regionId) {
                regionIds.add(regionId);
            }

            /**
            * @param identifier for a gene in this interaction
            */
           protected void addIdentifier(String identifier) {
               identifiers.add(identifier);
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


    private String getExperiment()
    throws SAXException {
        String itemId;
            try {
                Item item = createItem("InteractionExperiment");
                item.setAttribute("name", EXPERIMENT);
                item.setReference("publication", getPub(PUBMEDID));
                itemId = item.getIdentifier();
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        return itemId;
    }
}
