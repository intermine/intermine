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
import java.util.Arrays;
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
public class PsiConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(PsiConverter.class);
    private Map<String, String> organisms = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Object> experimentNames = new HashMap<String, Object>();
    private Map<String, String> terms = new HashMap<String, String>();
    private String termId = null;
    private Map<String, Item> genes = new  HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PsiConverter(ItemWriter writer, Model model) {
        super(writer, model, "IntAct", "IntAct data set");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();

        try {
            termId = getTerm("MI:0117");
        } catch (SAXException e) {
            throw new RuntimeException("ack");
        }
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
            try {
                getOrganism(taxId);
            } catch (SAXException e) {
                throw new RuntimeException("ack");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        PsiHandler handler = new PsiHandler();

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
    class PsiHandler extends DefaultHandler
    {
        private Map<String, ExperimentHolder> experimentIds
            = new HashMap<String, ExperimentHolder>();
        private InteractionHolder holder = null;
        private ExperimentHolder experimentHolder = null;
        private InteractorHolder interactorHolder = null;
        private Item gene = null, comment = null;
        private String experimentId = null, interactorId = null;
        private Map<String, String> identifiers = new HashMap<String, String>();

        private Map<String, Item> validGenes = new HashMap<String, Item>();
        private String regionName = null;

        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;

        /**
         * Constructor
         */
        public PsiHandler() {
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
                    String pub = getPub(pubMedId);
                    experimentHolder.setPublication(pub);
                }
                //<experimentList><experimentDescription><attributeList><attribute>
            } else if (qName.equals("attribute") && stack.peek().equals("attributeList")
                            && stack.search("experimentDescription") == 2) {
                String name = attrs.getValue("name");
                if (experimentHolder.experiment != null && name != null) {
                    comment = createItem("Comment");
                    comment.setAttribute("type", name);
                    attName = "experimentAttribute";
                } else {
                    LOG.info("Can't create comment, bad experiment.");
                }
                // <hostOrganismList><hostOrganism ncbiTaxId="9534"><names><fullName>
            } else if (qName.equals("hostOrganism")) {
                attName = "hostOrganism";
                String hostOrganism = attrs.getValue("ncbiTaxId");
                if (hostOrganism != null && !hostOrganism.equals("-1")) {
                    String refId = getOrganism(hostOrganism);
                    experimentHolder.setHostOrganism(refId);
                }
                //<interactionDetectionMethod><xref><primaryRef>
            } else if (qName.equals("primaryRef") && stack.peek().equals("xref")
                            && stack.search("interactionDetectionMethod") == 2) {
                String termItemId = getTerm(attrs.getValue("id"));
                experimentHolder.setMethod("interactionDetectionMethod", termItemId);
                //<participantIdentificationMethod><xref> <primaryRef>
            } else if (qName.equals("primaryRef") && stack.peek().equals("xref")
                            && stack.search("participantIdentificationMethod") == 2) {
                String termItemId = getTerm(attrs.getValue("id"));
                experimentHolder.setMethod("participantIdentificationMethod", termItemId);
                // <interactorList><interactor id="4">
            } else if (qName.equals("interactor") && stack.peek().equals("interactorList")) {
                interactorId = attrs.getValue("id");
                // <interactorList><interactor id="4"><names><fullName>F15C11.2</fullName>
            } else if ((qName.equals("fullName") || qName.equals("shortLabel"))
                            && stack.search("interactor") == 2) {
                attName = qName;
                // <interactorList><interactor id="4"><organism ncbiTaxId="7227">
            } else if (qName.equals("organism") && stack.peek().equals("interactor")) {
                String taxId = attrs.getValue("ncbiTaxId");
                if (organisms.containsKey(taxId)) {
                    try {
                        gene = getGene(taxId);
                    } catch (ObjectStoreException e) {
//
                    }

                    if (!validGenes.containsKey(interactorId)) {
                        validGenes.put(interactorId, gene);
                    }
                }
                identifiers = new HashMap();
                // <interactorList><interactor id="4"><names>
                // <alias type="locus name" typeAc="MI:0301">HSC82</alias>
            } else if (qName.equals("alias") && stack.peek().equals("names")
                            && stack.search("interactor") == 2) {
                attName = attrs.getValue("type");
                // <interactorList><interactor id="4"><sequence>
            } else if (qName.equals("sequence") && stack.peek().equals("interactor")) {
                attName = "sequence";
                //<interactionList><interaction id="1"><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("interaction") == 2) {
                attName = "interactionName";
                //<interaction><confidenceList><confidence><unit><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.peek().equals("names")
                            && stack.search("confidence") == 3) {
                attName = "confidenceUnit";
                //<interactionList><interaction><confidenceList><confidence><value>
            } else if (qName.equals("value") && stack.peek().equals("confidence")) {
                attName = "confidence";
                //<interactionList><interaction>
                //<participantList><participant id="5"><interactorRef>
            } else if (qName.equals("interactorRef")
                            && stack.peek().equals("participant")) {
                attName = "participantId";
                // <participantList><participant id="5"><experimentalRole><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("experimentalRole") == 2) {
                attName = "proteinRole";
                //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef") && stack.peek().equals("experimentList")) {
                attName = "experimentRef";
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
            //<interactorType><xref><primaryRef db="psi-mi" dbAc="MI:0488" id="MI:0326"
            } else if (qName.equals("primaryRef") && stack.search("interactionType") == 2) {
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

            // <experimentList><experimentDescription><attributeList><attribute/>
            // <attribute name="publication-year">2006</attribute>
            if (attName != null && attName.equals("experimentAttribute")
                            && qName.equals("attribute")) {
                String s = attValue.toString();
                if (comment != null && s != null) {
                    if (!s.equals("")) {
                        comment.setAttribute("text", s);
                    }
                    try {
                        store(comment);
                    } catch (ObjectStoreException e) {
                        // TODO only store for valid experiments
                    }
                    comment = null;
                } else {
                    LOG.info("Experiment " + experimentHolder.name + " has a bad comment");
                }
            // <experimentList><experimentDescription><names><shortLabel>
            } else if (attName != null && attName.equals("experimentName")
                            && qName.equals("shortLabel")) {
                String shortLabel = attValue.toString();
                if (shortLabel != null) {
                    experimentHolder = getExperiment(shortLabel);
                    experimentIds.put(experimentId, experimentHolder);
                    experimentHolder.setName(shortLabel);
                } else {
                    LOG.error("Experiment " + experimentId + " doesn't have a shortLabel");
                }

            // <experimentList><experimentDescription><names><fullName>
            } else if (attName != null && attName.equals("experimentDescr")
                            && qName.equals("fullName")) {
                String descr = attValue.toString();
                if (descr != null) {
                    experimentHolder.setDescription(descr);
                }
                // <hostOrganismList><hostOrganism ncbiTaxId="9534"><names><fullName>
            } else if (attName != null && attName.equals("hostOrganism")
                            && qName.equals("fullName")) {
                String hostOrganism = attValue.toString();
                if (hostOrganism != null && !hostOrganism.equals("-1")) {
                    experimentHolder.setHostOrganism(hostOrganism);
                }
            // <interactorList><interactor id="4"><names><fullName>
            } else if ((qName.equals("fullName") || qName.equals("shortLabel"))
                            && stack.search("interactor") == 2) {
                String name = attValue.toString();
                if (name != null) {
                    identifiers.put(qName, name);
                }
            // <interactorList><interactor id="4">
            } else if (qName.equals("alias")) {
                String identifier = attValue.toString();
                if (identifier != null && !identifier.equals("")) {
                    if (identifier.startsWith("Dmel_")) {
                        identifier = identifier.substring(5);
                    }
                    if (identifier.startsWith("cg")) {
                        identifier = "CG" + identifier.substring(2);
                    }
                    identifiers.put(attName, identifier);
                }

                // <interactorList><interactor id="4"></interactor>
            } else if (gene != null && qName.equals("interactor")) {
                gene = null;
            //<interactionList><interaction>
            //<participantList><participant id="5"><interactorRef>
            } else if (qName.equals("interactorRef") && stack.peek().equals("participant")) {
                String id = attValue.toString();
                if (validGenes.get(id) != null) {
                    Item interactor = validGenes.get(id);
                    String geneRefId = interactor.getIdentifier();
                    interactorHolder = new InteractorHolder(geneRefId);
                    String ident = null;
                    if (interactor.getAttribute("secondaryIdentifier") != null) {
                        ident = interactor.getAttribute("secondaryIdentifier").getValue();
                    }
                    if ((ident == null || ident.equals(""))
                                    && interactor.getAttribute("symbol") != null) {
                        ident = interactor.getAttribute("symbol").getValue();
                    }
                    if ((ident == null || ident.equals(""))
                                    && interactor.getAttribute("primaryIdentifier") != null) {
                        ident = interactor.getAttribute("primaryIdentifier").getValue();
                    }
                    if (ident != null) {
                        interactorHolder.identifier = ident;
                        holder.addInteractor(interactorHolder);
                        holder.addProtein(geneRefId);
                        holder.addIdentifer(ident);
                    } else {
                        holder.isValid = false;
                    }
                } else {
                    holder.isValid = false;
                }
            // <interactionList><interaction><names><shortLabel>
            } else if (qName.equals("shortLabel") && attName != null
                            && attName.equals("interactionName")) {
                holder = new InteractionHolder(attValue.toString());
                //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef") && stack.peek().equals("experimentList")) {
                String experimentRef = attValue.toString();
                if (experimentIds.get(experimentRef) != null) {
                    holder.setExperiment(experimentIds.get(experimentRef));
                } else {
                    LOG.error("Bad experiment:  [" + experimentRef + "] of "
                              + experimentIds.size() + " experiments");
                }
            //<interaction><confidenceList><confidence><unit><names><shortLabel>
            } else if (qName.equals("shortLabel") && attName != null
                            && attName.equals("confidenceUnit") && holder != null) {
                String shortLabel = attValue.toString();
                if (shortLabel != null) {
                    holder.confidenceUnit = shortLabel;
                }
            //<interactionList><interaction><confidenceList><confidence><value>
            } else if (qName.equals("value") && attName != null && attName.equals("confidence")
                            && holder != null) {
                if (holder.confidenceUnit.equals("author-confidence")) {
                    String value = attValue.toString();
                    holder.setConfidence(value);
                }
            // <interactionList><interaction><participantList><participant id="5">
            // <experimentalRole><names><shortLabel>
            } else if (qName.equals("shortLabel") && stack.search("experimentalRole") == 2
                            && interactorHolder != null) {
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

                // build & store interactions - one for each gene
                Item interaction = createItem("Interaction");
                String geneRefId = interactorHolder.geneRefId;
                String identifier = interactorHolder.identifier;
                String shortName = interactionHolder.shortName;
                interaction.setAttribute("shortName", shortName);
                interaction.setAttribute("role", interactorHolder.role);

                String name = buildName(interactionHolder.primaryIdentifiers, identifier);
                interaction.setAttribute("name", name);

                if (interactionHolder.confidence != null) {
                    interaction.setAttribute("confidence", interactionHolder.confidence.toString());
                }
                if (interactionHolder.confidenceText != null) {
                    interaction.setAttribute("confidenceText", interactionHolder.confidenceText);
                }
                interaction.setReference("gene", geneRefId);
                interaction.setReference("type", interactionHolder.termRefId);
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

                    String regionIdentifier = shortName + "_" + identifier;

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

        private String buildName(List<String> primaryIdentifiers, String identifier) {
            String name = "IntAct:" + identifier;
            Iterator it = primaryIdentifiers.iterator();
            while (it.hasNext()) {
                String primaryIdentifier = (String) it.next();
                if (!primaryIdentifier.equals(identifier)) {
                    name = name + "_" + primaryIdentifier;
                }
            }
            return name;
        }

        private Item getGene(String taxonId) throws ObjectStoreException {
            String identifier = null;
            String label = "secondaryIdentifier";
            identifier = identifiers.get("orf name");
            if (identifier == null) {
                identifier = identifiers.get("gene name");
                label = "symbol";
            }

            if (identifier == null) {
                identifier = identifiers.get("fullName");
                label = "secondaryIdentifier";
            }

            //<names> <shortLabel>mir-13b</shortLabel> </names>
            if (identifier == null) {
                identifier = identifiers.get("shortLabel");
                label = "symbol";
            }

            if (identifier == null) {
                throw new RuntimeException(" ~~ org:" + taxonId + " interactor " + interactorId);
            }

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
                label = "primaryIdentifier";
            }

            Item item = genes.get(identifier);
            if (item == null) {
                item = createItem("Gene");
                item.setAttribute(label, identifier);
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
            private String shortName;
            private ExperimentHolder eh;
            private Double confidence;
            private String confidenceText;
            private String confidenceUnit;
            private Set<InteractorHolder> interactors = new LinkedHashSet<InteractorHolder>();
            private boolean isValid = true;
            private Set<String> geneIds = new HashSet<String>();
            private List<String> primaryIdentifiers = new ArrayList<String>();
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
                if (Character.isDigit(confidence.charAt(0))) {
                    this.confidence = new Double(confidence);
                } else {
                    confidenceText = (confidenceText != null
                                    ? confidenceText + confidence : confidence);
                }
            }

            /**
             * @param ih object holding interactor
             */
            protected void addInteractor(InteractorHolder ih) {
                interactors.add(ih);
            }

            /**
             * @param identifier primaryIdentifier for an interactor in this interaction
             */
            protected void addIdentifer(String identifier) {
                primaryIdentifiers.add(identifier);
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
             * @param refId id representing a term object
             */
            protected void setType(String refId) {
                termRefId = refId;
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

            protected String name, description;
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
            * @param description description of experiment
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
