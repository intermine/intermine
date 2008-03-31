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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import java.io.Reader;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse pride data into items
 * @author Dominik Grimm and Michael Menden
 */
public class PrideConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final Logger LOG = Logger.getLogger(PrideConverter.class);
    private Map<String, Map> mapMaster = new HashMap<String, Map>();  // map of maps
    
    //the following maps should avoid that not unnecessary objects will be created
    private Map<String, String> mapOrganism = new HashMap<String, String>();
    private Map<String, String> mapPublication = new HashMap<String, String>();
    private Map<String, String> mapGOTerm = new HashMap<String, String>();
    private Map<String, String> mapProtein = new HashMap<String, String>();
    private Map<String, String> mapPSIMod = new HashMap<String, String>();
    private Map<String, String> mapCellType = new HashMap<String, String>();
    private Map<String, String> mapMedSubject = new HashMap<String, String>();
    private Map<String, String> mapDisease = new HashMap<String, String>();
    private Map<String, String> mapTissue = new HashMap<String, String>();
    private Map<String, String> mapProject = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PrideConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }


    // master map of all maps used across XML files
    private void mapMaps() {
        mapMaster.put("mapOrganism",mapOrganism);
        mapMaster.put("mapPublication", mapPublication);
        mapMaster.put("mapGOTerm",mapGOTerm);
        mapMaster.put("mapProtein", mapProtein);
        mapMaster.put("mapPSIMod", mapPSIMod);
        mapMaster.put("mapCellType", mapCellType);
        mapMaster.put("mapMedSubject", mapMedSubject);
        mapMaster.put("mapDisease", mapDisease);
        mapMaster.put("mapTissue", mapTissue);
        mapMaster.put("mapProject", mapProject);
    }


    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        mapMaps();
        PrideHandler handler = new PrideHandler(getItemWriter(), mapMaster);
        LOG.error("in PrideConverter");
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
    private class PrideHandler extends DefaultHandler
    {
        //private data fields
        private ItemFactory itemFactory;
        private ItemWriter writer;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;
     
      //private maps
        private Map<String, String> mapOrganism;
        private Map<String, String> mapPublication;
        private Map<String, String> mapGOTerm;
        private Map<String, String> mapProtein;
        private Map<String, String> mapPSIMod;
        private Map<String, String> mapCellType;
        private Map<String, String> mapMedSubject;
        private Map<String, String> mapDisease;
        private Map<String, String> mapTissue;
        private Map<String, String> mapProject;
        
        //private Items
        private Item itemOrganism = null;
        private Item itemPublication = null;
        private Item itemGOTerm = null;
        private Item itemProtein = null;
        private Item itemPSIMod = null;
        private Item itemCellType = null;
        private Item itemMedSubject = null;
        private Item itemDisease = null;
        private Item itemTissue = null;
        private Item itemProteinIdentification = null;
        private Item itemPrideProject = null;
        private Item itemPrideExperiment = null;
        private Item itemPeptide = null;
        private Item itemPeptideModification = null;
        
        //private reference strings
    	private String[] 	proteinAccessionId 	= null;
    	private String[]	proteinIdentifierId = null;
    	
    	//private bool
    	private boolean		SWISSPROT = false;
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param mapMaster master map of all maps used across XML files
         */
        public PrideHandler(ItemWriter writer, Map mapMaster) {

            itemFactory 		= new ItemFactory(Model.getInstanceByName("genomic"));
            this.writer 		= writer;
            this.mapOrganism    = (Map) mapMaster.get("mapOrganism");
            this.mapGOTerm      = (Map) mapMaster.get("mapGOTerm");
            this.mapPublication = (Map) mapMaster.get("mapPublication");
            this.mapProtein     = (Map) mapMaster.get("mapProtein");
            this.mapPSIMod	    = (Map) mapMaster.get("mapPSIMod");
            this.mapCellType    = (Map) mapMaster.get("mapCellType");
            this.mapMedSubject	= (Map) mapMaster.get("mapMedSubject");
            this.mapDisease     = (Map) mapMaster.get("mapDisease");
            this.mapTissue	    = (Map) mapMaster.get("mapTissue");
            this.mapProject		= (Map) mapMaster.get("mapProject");
        }


        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
        	attName = null;
         	/**
        	 * if only attName is set, the the attValue is between the tags and must store in endElement().
        	 * if the value is included in the tag, you have to set the attribut now.
        	 * Objects (=items) have to be builded here.
        	 * 
			 */
        	
        	/**
        	 * PrideExperiment start
        	 */
            // <ExperimentCollection><Experiment>
            if (qName.equals("Experiment")) {
            	itemPrideExperiment = createItem("PrideExperiment");
            } 
            // <ExperimentCollection><Experiment><ExperimentAccession>
            else if (qName.equals("ExperimentAccession")) {
            	attName = "accessionId";
            }
            // <ExperimentCollection><Experiment><Title>
            else if (qName.equals("Title")){
            	attName = "title";
            }
            // <ExperimentCollection><Experiment><ShortLabel>
            else if (qName.equals("ShortLabel")){
            	attName = "shortLabel";
            }
            // <ExperimentCollection><Experiment><Protocol><ProtocolName>
            else if (qName.equals("ProtocolName") 
            		 && stack.peek().equals("Protocol")){
            	attName = "protocolName";
            }
            /**
             * PrideProject
             */
            //<ExperimentCollection><Experiment><additional><cvParam>
            else if (qName.equals("cvParam") 
            		 && stack.peek().equals("additional") 
            		 && attrs.getValue("name").equals("Project")) {
            	
            	String refId;
            	
            	if(attrs.getValue("value") != null) {
            		if(!attrs.getValue("value").toString().equals("")) {
            			if(mapProject.get(attrs.getValue("value").toString()) == null) {
            				itemPrideProject = createItem("PrideProject");
            				
                      		itemPrideProject.setAttribute("title",attrs.getValue("value").toString());
                        
            				refId = itemPrideProject.getIdentifier();
            				mapProject.put(attrs.getValue("value").toString(), refId);

            				try {
            					writer.store(ItemHelper.convert(itemPrideProject));
                				itemPrideProject = null;
            				} catch (ObjectStoreException e) {
            					throw new SAXException(e);
            				} 
            			}
            			else {
            				refId = mapProject.get(attrs.getValue("value").toString());
            			}
            			itemPrideExperiment.addReference(new Reference("prideProject", refId));
            		}
            	}
            }
            
            /**
             * ProteinIdentification
             */
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification>
            else if(qName.equals("GelFreeIdentification")
            		|| qName.equals("TwoDimensionalIdentification")) {
            	itemProteinIdentification = createItem("ProteinIdentification");
            }
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><Score>
            else if(qName.equals("Score")
            		&& (stack.peek().equals("GelFreeIdentification") 
            			|| stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "score";
            }
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><Threshold>
            else if(qName.equals("Threshold")
            		&& (stack.peek().equals("GelFreeIdentification") 
            			|| stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "threshold";
            }
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SearchEngine>
            else if(qName.equals("SearchEngine")
            		&& (stack.peek().equals("GelFreeIdentification") 
            			|| stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "searchEngine";
            }
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SpliceIsoform>
            else if(qName.equals("SpliceIsoform")
            		&& (stack.peek().equals("GelFreeIdentification") 
            			|| stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "spliceIsoform";
            }
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SpectrumReference>
            else if(qName.equals("SpectrumReference")
            		&& (stack.peek().equals("GelFreeIdentification") 
            			|| stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "spectrumReference";
            }
            // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SequenceCoverage>
            else if(qName.equals("SequenceCoverage")
            		&& (stack.peek().equals("GelFreeIdentification") 
            			|| stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "sequenceCoverage";
            }
            // <ExperimentCollection><Experiment><TwoDimensionalIdentification><MolecularWeight>
            else if(qName.equals("MolecularWeight")
            		&& (stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "molecularWeight";
            }
            // <ExperimentCollection><Experiment><TwoDimensionalIdentification><pI>
            else if(qName.equals("pI")
            		&& (stack.peek().equals("TwoDimensionalIdentification"))) {
            	attName = "pI";
            }
            // <ExperimentCollection><Experiment><TwoDimensionalIdentification><Gel><GelLink>
            else if(qName.equals("GelLink")
            		&& stack.peek().equals("Gel") ) {
            	attName = "gelLink";
            }
            // <ExperimentCollection><Experiment><TwoDimensionalIdentification><GelLocation><XCoordinate>
            else if(qName.equals("XCoordinate")
            		&& stack.peek().equals("GelLocation") ) {
            	attName = "gelXCoordinate";
            }
            // <ExperimentCollection><Experiment><TwoDimensionalIdentification><GelLocation><YCoordinate>
            else if(qName.equals("YCoordinate")
            		&& stack.peek().equals("GelLocation") ) {
            	attName = "gelYCoordinate";
            }
            
            /**
            * protein class
            */
           //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><additional><cvParam>
           else if (qName.equals("cvParam") 
           		 && stack.peek().equals("additional")
           		 && attrs.getValue("name").toString().equals("Automatic allocation")) {
           	   	//start SWISSPROT identification
        	   PrideExpression exp = new PrideExpression(attrs.getValue("value").toString());
        	   
        	   proteinAccessionId = new String[exp.accessionCounter];
        	   proteinIdentifierId = new String[exp.identifierCounter];
       		   //store accessionIds
       		   if(exp.findSwissport()) {
       			   SWISSPROT = true;
       			   proteinAccessionId = exp.getAccession();	  
       			   proteinIdentifierId = exp.getIdentifier();
       		   }
       		   	
           }
           
            /**
             * peptide class
             */
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
           	else if (qName.equals("PeptideItem") 
           		 && (stack.peek().equals("GelFreeIdentification")
                   	 || stack.peek().equals("TwoDimensionalIdentification"))) {
           	   itemPeptide = createItem("Peptide");
           	}
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
            else if (qName.equals("Sequence") 
            		 && (stack.peek().equals("PeptideItem"))) {
            	   attName = "sequence";
            }
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Start>
            else if (qName.equals("Start") 
            		 && (stack.peek().equals("PeptideItem"))) {
            	   attName = "start";
            }
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><End>
            else if (qName.equals("End") 
            		 && (stack.peek().equals("PeptideItem"))) {
            	   attName = "end";
            }
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><SpectrumReference>
            else if (qName.equals("SpectrumReference") 
            		 && (stack.peek().equals("PeptideItem"))) {
            	   attName = "spectrumReference";
            }
            
            /**
             * PeptideModification class
             */
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem>
           	else if (qName.equals("ModificationItem") 
           		 && stack.peek().equals("PeptideItem")) {
           	   itemPeptideModification = createItem("PeptideModification");
           	}
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem><ModLocation>
           	else if (qName.equals("ModLocation") 
           		 && stack.peek().equals("ModificationItem")) {
           		attName = "location";
           	}
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem><ModAccession>
           	else if (qName.equals("ModAccession") 
           		 && stack.peek().equals("ModificationItem")) {
           		attName = "accessionId";
           	}            
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem><ModDatabase>
           	else if (qName.equals("ModDatabase") 
           		 && stack.peek().equals("ModificationItem")) {
           		attName = "modDB";
           	}            
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem><ModDatabaseVersion>
           	else if (qName.equals("ModDatabaseVersion") 
           		 && stack.peek().equals("ModificationItem")) {
           		attName = "modDBVersion";
           	}            
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem><ModMonoDelta>
           	else if (qName.equals("ModMonoDelta") 
           		 && stack.peek().equals("ModificationItem")) {
           		attName = "modMonoDelta";
           	}            
            //<ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem><ModAvgDelta>
           	else if (qName.equals("ModAvgDelta") 
           		 && stack.peek().equals("ModificationItem")) {
           		attName = "modAvgDelta";
           	}        
            
            /**
             * Organism class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("NEWT")){
            	
            	String accId;
    
            	//is the organism available?
            	if(mapOrganism.get(attrs.getValue("accession")) == null){
            		// put onto hashMap taxonId (=key) and identifier (=value)
            		itemOrganism = createItem("Organism");
                	// store in itemOrganism the taxonId
                	itemOrganism.setAttribute("taxonId",attrs.getValue("accession"));
                	
                	accId = itemOrganism.getIdentifier();
                	
            		mapOrganism.put(attrs.getValue("accession"), accId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemOrganism));
            			itemOrganism = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		accId = mapOrganism.get(attrs.getValue("accession"));
        		}
	
            	//set reference
            	itemPrideExperiment.addReference(new Reference("organism", accId));
            		
            	
            }
            
            /**
             * Publication class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("PubMed")){
            
            	String refId;
            	
            	//is the organism available?
            	if(mapPublication.get(attrs.getValue("accession")) == null){
            		// put onto hashMap taxonId (=key) and identifier (=value)
                	itemPublication = createItem("Publication");
                	// store in itemOrganism the taxonId
                	itemPublication.setAttribute("pubMedId",attrs.getValue("accession").toString());
                	refId = itemPublication.getIdentifier();
            		mapPublication.put(attrs.getValue("accession"), refId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemPublication));
            			itemPublication = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		refId = mapPublication.get(attrs.getValue("accession"));
        		}
            	//set reference
            	itemPrideExperiment.addReference(new Reference("publication", refId));
            }
            
            /**
             * GOTerm class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("GO")){
            	

            	String refId;
            	//is the organism available?
            	if(mapGOTerm.get(attrs.getValue("name")) == null){
            		// put onto hashMap taxonId (=key) and identifier (=value)
                	itemGOTerm = createItem("GOTerm");
                	itemGOTerm.setAttribute("name",attrs.getValue("name").toString());
                	refId = itemGOTerm.getIdentifier();
            		mapGOTerm.put(attrs.getValue("name"), refId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemGOTerm));
                		itemGOTerm = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		refId = mapGOTerm.get(attrs.getValue("name"));
        		}
	
            	//set reference
            	itemPrideExperiment.addReference(new Reference("goTerm", refId));
            }
            
            /**
             * PSIMod class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("PSI-MOD")){
            	
            	String refId;
            	//is the organism available?
            	if(mapPSIMod.get(attrs.getValue("name")) == null){
            		itemPSIMod = createItem("PSIMod");
                	itemPSIMod.setAttribute("name",attrs.getValue("name").toString());
                	refId = itemPSIMod.getIdentifier();
            		// put onto hashMap taxonId (=key) and identifier (=value)
            		mapPSIMod.put(attrs.getValue("name"), refId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemPSIMod));
            			itemPSIMod = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		refId = mapPSIMod.get(attrs.getValue("name"));
        		}
	
            	//set reference
            	itemPrideExperiment.addReference(new Reference("psiMod", refId));
        		
            }
            
            /**
             * CellType class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("CL")){
            	

            	String refId;
            	//is the organism available?
            	if(mapCellType.get(attrs.getValue("name")) == null){
            		
                	itemCellType = createItem("CellType");
                	itemCellType.setAttribute("name",attrs.getValue("name").toString());
            		refId = itemCellType.getIdentifier();
                	// put onto hashMap taxonId (=key) and identifier (=value)
            		mapCellType.put(attrs.getValue("name"), refId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemCellType));
            			itemCellType = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		refId = mapCellType.get(attrs.getValue("name"));
        		}
	
            	//set reference
            	itemPrideExperiment.addReference(new Reference("cellType", refId));
        		
            }
            
            /**
             * Disease class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("DOID")){
            
            	String refId;
            	//is the organism available?
            	if(mapDisease.get(attrs.getValue("name")) == null){
                	itemDisease = createItem("PrideDisease");
                	itemDisease.setAttribute("name",attrs.getValue("name").toString());
                	refId = itemDisease.getIdentifier();
            		// put onto hashMap taxonId (=key) and identifier (=value)
            		mapDisease.put(attrs.getValue("name"), refId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemDisease));
                		itemDisease = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		refId = mapDisease.get(attrs.getValue("name"));
        		}
	
            	//set reference
            	itemPrideExperiment.addReference(new Reference("disease", refId));
        		

            }
            
            /**
             * Tissue class
             */
            //<ExperimentCollection><Experiment><mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam")
            		 && stack.peek().equals("sampleDescription")
            		 && attrs.getValue("cvLabel").equals("BTO")){
            	
            	String refId;
            	
            	//is the organism available?
            	if(mapTissue.get(attrs.getValue("name")) == null){
                	itemTissue = createItem("Tissue");
                	itemTissue.setAttribute("name",attrs.getValue("name").toString());
                	refId = itemTissue.getIdentifier();
            		// put onto hashMap taxonId (=key) and identifier (=value)
            		mapTissue.put(attrs.getValue("name"), refId);
            		
            		try {
            			//store as object in file
            			writer.store(ItemHelper.convert(itemTissue));
                		itemTissue = null;
            		} catch (ObjectStoreException e) {
            			throw new SAXException(e);
            		}
            	}
            	else{
            		//store in item the right identiefier
            		refId = mapTissue.get(attrs.getValue("name"));
        		}
	
            	//set reference
            	itemPrideExperiment.addReference(new Reference("tissue", refId));
        		
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

        	try {
                stack.pop();
                
                /**
            	 * PrideExperiment start
            	 */
                //<ExperimentCollection><Experiment><ExperimentAccession>
                if(qName.equals("ExperimentAccession")) {
                	itemPrideExperiment.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><Title>
                else if(qName.equals("Title")){
                	itemPrideExperiment.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><ShortLabel>
                else if(qName.equals("ShortLabel")){
                	itemPrideExperiment.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><Protocol><ProtocolName>
                else if(qName.equals("ProtocolName") 
                		&& stack.peek().equals("Protocol")){
                	itemPrideExperiment.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                else if(qName.equals("Experiment")) {
              
                	
                	
                	writer.store(ItemHelper.convert(itemPrideExperiment));

                	itemPrideExperiment = null;
                	
                }
                /**
            	 * ProteinIndentification
            	 */
                // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><Score>
                else if(qName.equals("Score")
                		&& (stack.peek().equals("GelFreeIdentification") 
                    			|| stack.peek().equals("TwoDimensionalIdentification"))) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><Threshold>
                else if(qName.equals("Threshold")
                		&& (stack.peek().equals("GelFreeIdentification") 
                    			|| stack.peek().equals("TwoDimensionalIdentification"))) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                }                 
                // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SearchEngine>
                else if(qName.equals("SearchEngine")
                		&& (stack.peek().equals("GelFreeIdentification") 
                    			|| stack.peek().equals("TwoDimensionalIdentification"))) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SpliceIsoform>
                else if(qName.equals("SpliceIsoform")
                		&& (stack.peek().equals("GelFreeIdentification") 
                    			|| stack.peek().equals("TwoDimensionalIdentification"))) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SpectrumReference>
                else if(qName.equals("SpectrumReference")
                		&& (stack.peek().equals("GelFreeIdentification") 
                    			|| stack.peek().equals("TwoDimensionalIdentification"))) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><GelFreeIdentification || TwoDimensionalIdentification><SequenceCoverage>
                else if(qName.equals("SequenceCoverage")
                		&& (stack.peek().equals("GelFreeIdentification") 
                    			|| stack.peek().equals("TwoDimensionalIdentification"))) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><MolecularWeight>
                else if(qName.equals("MolecularWeight")
                		&& stack.peek().equals("TwoDimensionalIdentification")) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><pI>
                else if(qName.equals("pI")
                		&& stack.peek().equals("TwoDimensionalIdentification")) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><Gel><GelLink>
                else if(qName.equals("GelLink")
                		&& stack.peek().equals("Gel")) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><GelLocation><XCoordinate>
                else if(qName.equals("XCoordinate")
                		&& stack.peek().equals("GelLocation")) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><GelLocation><YCoordinate>
                else if(qName.equals("YCoordinate")
                		&& stack.peek().equals("GelLocation")) {
                	itemProteinIdentification.setAttribute(attName, attValue.toString());
                } 
                
               /**
                * Peptide class
                */
                 //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("Sequence") 
               		     && stack.peek().equals("PeptideItem")) {
                	// store in itemOrganism the taxonId
                	itemPeptide.setAttribute(attName,attValue.toString());
                }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Start>
                else if (qName.equals("Start") 
               		     && stack.peek().equals("PeptideItem")) {
                	// store in itemOrganism the taxonId
                	itemPeptide.setAttribute(attName,attValue.toString());
                }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><End>
                else if (qName.equals("End") 
               		     && stack.peek().equals("PeptideItem")) {
                	// store in itemOrganism the taxonId
                	itemPeptide.setAttribute(attName,attValue.toString());
                }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><SpectrumReference>
                else if (qName.equals("SpectrumReference") 
               		     && stack.peek().equals("PeptideItem")) {
                	// store in itemOrganism the taxonId
                	itemPeptide.setAttribute(attName,attValue.toString());
                }
                else if (qName.equals("PeptideItem")){
                	writer.store(ItemHelper.convert(itemPeptide));
                	itemPeptide = null;
                }
                
                /**
                 * PeptideModification class
                 */
                  //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModLocation") 
                		     && stack.peek().equals("ModificationItem")) {
                 	// store in itemOrganism the taxonId
                	 itemPeptideModification.setAttribute(attName,attValue.toString());
                 }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModAccession") 
                		     && stack.peek().equals("ModificationItem")) {
                 	// store in itemOrganism the taxonId
                	 itemPeptideModification.setAttribute(attName,attValue.toString());
                 }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModDatabase") 
                		     && stack.peek().equals("ModificationItem")) {
                 	// store in itemOrganism the taxonId
                	 itemPeptideModification.setAttribute(attName,attValue.toString());
                 }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModDatabaseVersion") 
                		     && stack.peek().equals("ModificationItem")) {
                 	// store in itemOrganism the taxonId
                	 itemPeptideModification.setAttribute(attName,attValue.toString());
                 }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModMonoDelta") 
                		     && stack.peek().equals("ModificationItem")) {
                 	// store in itemOrganism the taxonId
                	 itemPeptideModification.setAttribute(attName,attValue.toString());
                 }
                //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModAvgDelta") 
                		     && stack.peek().equals("ModificationItem")) {
                 	// store in itemOrganism the taxonId
                	 itemPeptideModification.setAttribute(attName,attValue.toString());
                 }
                 else if (qName.equals("ModificationItem")) {
                	 // store in itemOrganism the taxonId
                	writer.store(ItemHelper.convert(itemPeptideModification));
                	itemPeptideModification = null;
                 }
                
               /**
                * Identification closing tag. Time to store all of the items
                */
               //<ExperimentCollection><Experiment>GelFreeIdentification || TwoDimensionalIdentification>
               else if(qName.equals("GelFreeIdentification")
               		   || qName.equals("TwoDimensionalIdentification")) {
               	
            	   //Store Protein accessionId and identifier
            	   if(SWISSPROT) {
            		   String refId = null;
            		   //store several numbers of accessionIds and identifiers
            		   for(int i=0; i < proteinAccessionId.length; i++) {
            			   //create new Item if no protein with the same accessionId exists
            			   if(mapProtein.get(proteinAccessionId[i]) == null) {
            				   itemProtein = createItem("Protein");
            				   itemProtein.setAttribute("primaryAccession", proteinAccessionId[i]);
            				   if(i < proteinIdentifierId.length)
            					   itemProtein.setAttribute("primaryIdentifier", proteinIdentifierId[i]);
            				   refId = itemProtein.getIdentifier();
            				   mapProtein.put(proteinAccessionId[i], refId); 
            				   writer.store(ItemHelper.convert(itemProtein));
            				   itemProtein = null;
            			   } else{
            				   refId = mapProtein.get(proteinAccessionId[i]);
            			   }
            			   //set reference
            			   itemProteinIdentification.addReference(new Reference("protein",refId));
            		   }
            		   SWISSPROT = false;
            	   }

                 	//store ProteinIdentification
                   	itemProteinIdentification.addReference(new Reference("prideExperiment", itemPrideExperiment.getIdentifier()));
                   	writer.store(ItemHelper.convert(itemProteinIdentification));
                   	
                  	itemProteinIdentification = null;
               }
            
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
           
        }
        

        protected Item createItem(String className) {
            return PrideConverter.this.createItem(className);
        }
    }
}
