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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
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
public class BioGridConverter extends FileConverter
{   
    private static final Logger LOG = Logger.getLogger(BioGridConverter.class);
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static Map<String, String> masterList = new HashMap<String, String>();
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public BioGridConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    // 

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        PsiHandler handler = new PsiHandler(getItemWriter(), masterList);

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
        private ItemFactory itemFactory;
        private Map<String, Integer> ids = new HashMap<String, Integer>();
        private Map<String, String> aliases = new HashMap<String, String>();
        private Map<String, String> pubs = new HashMap<String, String>();
        private Map<String, Item> genes = new HashMap<String, Item>();
        private Map<String, String> geneIdsToIdentifiers = new HashMap<String, String>();
        // [experiment name] [experiment holder]
        private Map<String, ExperimentHolder> experimentNames 
        = new HashMap<String, ExperimentHolder>();  
        // [id][experimentholder]
        private Map<String, ExperimentHolder> experimentIds
        = new HashMap<String, ExperimentHolder>();
        private InteractorHolder interactorHolder;
        private ItemWriter writer;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
        private InteractionHolder holder;
        private ExperimentHolder experimentHolder;
        private String geneId;
        private String experimentId;
        private Item organism = null;
        Set<String> storedItems = new HashSet<String>();
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param masterList a map holding objects that span the XML files (dataset and classId)
         */
        public PsiHandler(ItemWriter writer, Map masterList) {

            itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
            this.writer = writer;
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
                    }
                }
                
            // <interactorList><interactor id="4">
            } else if (qName.equals("interactor") && stack.peek().equals("interactorList")) {
                
                geneId = attrs.getValue("id");     
                
            // <interactorList><interactor id="4"><xref><primaryRef db="FLYBASE" id="FBgn0000659"
            } else if (qName.equals("primaryRef")
                       && stack.peek().equals("xref")
                       && stack.search("interactor") == 2) {

                // TODO can we have a gene with a different participant # but the same identifier?
                String identifier = attrs.getValue("id");
                geneIdsToIdentifiers.put(geneId, identifier);
                genes.put(geneId, getGene(geneId));
                
            //<interactionList><interaction id="1"><names><shortLabel>
            } else if (qName.equals("shortLabel")
                       && stack.peek().equals("names")
                       && stack.search("interaction") == 2) {

                attName = "interactionName";

            //<interactionList><interaction>
            //<participantList><participant id="5"><interactorRef>
            } else if (qName.equals("interactorRef") && stack.peek().equals("participant")) {

                attName = "participantId";

            //<interactionList><interaction><experimentList><experimentRef>
            } else if (qName.equals("experimentRef")
                       && stack.peek().equals("experimentList")) {
                
                attName = "experimentRef";
                
            //<interactionList><interaction><interactionType><names><shortLabel
            } else if (qName.equals("shortLabel")
                            && stack.peek().equals("names")
                            && stack.search("interactionType") == 2) {
                
                attName = "interactionType";
                
            // <entry>
            } else if (qName.equals("entry")) {

                initDatasources();
                
            }

            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }


        /**
         * {@inheritDoc}
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
                    Item gene = getGene(id);
                    if (gene != null) {
                        interactorHolder = new InteractorHolder(gene);
                        holder.addInteractor(interactorHolder);
                        holder.addGene(gene.getIdentifier());
                    } else {
                        holder.isValid = false; // a gene/protein is missing for soem reason
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
                    holder.setExperiment(experimentIds.get(experimentRef));
                    
                //<interactionType><names><shortLabel>
                } else if (qName.equals("shortLabel")
                                && attName != null
                                && attName.equals("interactionType")) {

                    String type = attValue.toString();
                    holder.type = type;
                    if (type.equalsIgnoreCase("Phenotypic Suppression") 
                                    || type.equalsIgnoreCase("Phenotypic Enhancement")) {
                        holder.isValid = true;
                    }
                    
                //<interactionList><interaction>
                } else if (qName.equals("interaction")
                                && holder != null
                                && holder.isValid) {

                    /* done processing everything for this interaction */
                    storeAll(holder);
                    holder = null;
                    interactorHolder = null;
                    //experimentHolder = null;

                }
        }

        private void storeAll(InteractionHolder interactionHolder) throws SAXException  {

            Set interactors = interactionHolder.interactors;
            
            
            try {
                // loop through genes/interactors in this interaction
                for (Iterator iter = interactors.iterator(); iter.hasNext();) {

                    interactorHolder =  (InteractorHolder) iter.next();

                    // build & store interactions - one for each gene
                    Item interaction = createItem("GeneInteraction");
                    Item gene = interactorHolder.gene;
                    String geneRefId = gene.getIdentifier();
                    interaction.setAttribute("shortName", interactionHolder.shortName);
                    interaction.setAttribute("type", interactionHolder.type);
                    
                    interaction.setReference("gene", geneRefId);
                    interaction.setReference("experiment",
                           interactionHolder.experimentHolder.experiment.getIdentifier());

                    // interactingGenes
                    Set<String> geneIds = interactionHolder.geneIds;
                    geneIds.remove(geneRefId);
                    ReferenceList geneList = new ReferenceList("interactingGenes",
                                                                  new ArrayList());
                    for (Iterator it = geneIds.iterator(); it.hasNext();) {
                        geneList.addRefId((String) it.next());
                    }
                    interaction.addCollection(geneList);
                    geneIds.add(geneRefId);

                    // add dataset
                    ReferenceList evidenceColl = new ReferenceList("evidence", new ArrayList());
                    interaction.addCollection(evidenceColl);
                    evidenceColl.addRefId(masterList.get("dataset"));

                    /* store all interaction-related items */
                    if (!storedItems.contains(gene.getIdentifier())) {
                        writer.store(ItemHelper.convert(gene));
                        storedItems.add(gene.getIdentifier());
                    }
                    
                    writer.store(ItemHelper.convert(interaction));
                }

                /* store experiment
                 * TODO we need to not store until all interactions are processed */
                ExperimentHolder eh = interactionHolder.experimentHolder;
                if (!eh.isStored) {
                    eh.isStored = true;
                    writer.store(ItemHelper.convert(eh.experiment));
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
                    writer.store(ItemHelper.convert(pub));
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            return itemId;
        }
        
        private Item getGene(String participantId) {
            Item item = genes.get(participantId);
            if (item == null) {
                String primaryIdentifier = geneIdsToIdentifiers.get(participantId);
                if (primaryIdentifier != null) {
                    item = createItem("Gene");
                    item.setAttribute("primaryIdentifier", primaryIdentifier);
                    genes.put(participantId, item);
                } else {
                    LOG.error("Gene/Protein wasn't in interactor list: #" + participantId);
                }
            }
            return item;
        }

        private Item getOrganism(String taxonId)
        throws SAXException {
            try {
                Item item = createItem("Organism");
                item.setAttribute("taxonId", taxonId);
                writer.store(ItemHelper.convert(item));
                return item;
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private ExperimentHolder checkExperiment(String name) {

            ExperimentHolder eh = experimentNames.get(name);
            if (eh == null) {
                Item exp = createItem("GeneInteractionExperiment");
                eh = new ExperimentHolder(exp);
                experimentNames.put(name, eh);
            }
            return eh;
        }

        private void addToCollection(Item parent, ReferenceList collection, Item newItem) {
            if (collection != null) {
                if (collection.getRefIds().isEmpty()) {
                    parent.addCollection(collection);
                }
                collection.addRefId(newItem.getIdentifier());
            }
        }

        private void initDatasources()
        throws SAXException {
            if (!masterList.containsKey("datasource")) {
                try {

                    Item datasource = createItem("DataSource");
                    datasource.setAttribute("name", "BioGRID");
                    Item dataSet = createItem("DataSet");
                    dataSet.setAttribute("title", "BioGRID data set");
                    dataSet.setReference("dataSource", datasource.getIdentifier());
                    writer.store(ItemHelper.convert(dataSet));
                    masterList.put("dataset", dataSet.getIdentifier());
                    writer.store(ItemHelper.convert(datasource));
                    masterList.put("datasource", datasource.getIdentifier());

                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
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
         * Holder object for GeneInteraction.  Holds all information about an interaction until
         * ready to store
         * @author Julie Sullivan
         */
        public static class InteractionHolder
        {
            protected String shortName, type;
            protected ExperimentHolder experimentHolder;
            protected Set<InteractorHolder> interactors = new LinkedHashSet<InteractorHolder>();
            protected Set<String> geneIds = new HashSet<String>();
            protected boolean isValid = false;
            
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
                this.experimentHolder = experimentHolder;
            }

            /**
             *
             * @param interactorHolder object holding interactor
             */
            protected void addInteractor(InteractorHolder interactorHolder) {
                interactors.add(interactorHolder);
            }

            /**
             *
             * @param geneId gene involved in interaction
             */
            protected void addGene(String geneId) {
                geneIds.add(geneId);
            }
        }


        /**
         * Holder object for GeneInteractor. Holds id and identifier for gene until the experiment
         * is verified as a gene interaction and not a protein interaction
         * @author Julie Sullivan
         */
        protected static class InteractorHolder
        {            
            protected String identifier;    // FBgn
            protected Item gene;

            /**
             * Constructor
             * @param gene Gene that's part of the interaction
             */
            public InteractorHolder(Item gene) {
                this.gene = gene;
            }
        }

        /**
         * Holder object for Experiment.  Holds all information about an experiment until
         * an interaction is verified to have only valid organisms
         * @author Julie Sullivan
         */
        protected static class ExperimentHolder
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
