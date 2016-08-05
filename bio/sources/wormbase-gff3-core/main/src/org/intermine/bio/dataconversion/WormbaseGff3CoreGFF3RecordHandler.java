package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import wormbase.model.parser.WMDebug;

/**
 * A converter/retriever for the WormbaseGff3Core dataset via GFF files.
 */

public class WormbaseGff3CoreGFF3RecordHandler extends GFF3RecordHandler
{

	WMDebug wmd;
    /**
     * Create a new WormbaseGff3CoreGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public WormbaseGff3CoreGFF3RecordHandler (Model model) {
        super(model);
        wmd = new WMDebug();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        // This method is called for every line of GFF3 file(s) being read.  Features and their
        // locations are already created but not stored so you can make changes here.  Attributes
        // are from the last column of the file are available in a map with the attribute name as
        // the key.   For example:
        //
        //     Item feature = getFeature();
        //     String symbol = record.getAttributes().get("symbol");
        //     feature.setAttrinte("symbol", symbol);
        //
        // Any new Items created can be stored by calling addItem().  For example:
        // 
        //     String geneIdentifier = record.getAttributes().get("gene");
        //     gene = converter.createItem("Gene");
        //     gene.setAttribute("primaryIdentifier", geneIdentifier);
        //     addItem(gene);
        //
        // You should make sure that new Items you create are unique, i.e. by storing in a map by
        // some identifier. 

    	Item feature = getFeature();
    	
    	String PID = record.getId();
    	
    	if( PID == null ) return;
    	
    	// Store unprefixed symbol
    	feature.setAttribute("symbol", stripTypePrefix(PID));
    	
    	if(key2refID.containsKey(PID)){
    		return;
    	}else{
    		// So keyAdded(PID) will return true
    		key2refID.put(PID, feature.getIdentifier());
    	}
    	
    	// Convert record type if available
    	String recordType = record.getType();
    	if( typeMap != null && typeMap.containsKey(recordType) ){
    		feature.setClassName(typeMap.get(recordType));
    	}
    	
    	if( 		feature.getClassName().equals("Transcript")	){
    		processTranscript(record, feature);
    	}else if(	feature.getClassName().equals("CDS")		){
    		processCDS(record, feature);
    	}
    	
    	
    	
//    	WMDebug.debug("WormbaseGff3CoreGFF3RecordHandler.process() called"); // TODO DEBUG
//    	System.out.println("JDJDJD:: WormbaseGff3CoreGFF3RecordHandler.process() :\t"+record.toString());
    }
    
    /**
     * Strips "Gene:" off of string.
     * @param rawName
     * @return
     */
    // TODO temp fixes all over 
    public String stripTypePrefix(String rawName){
    	if( rawName.contains(":")){
    		return rawName.substring(rawName.indexOf(':')+1);
    	}else{
    		return rawName;
    	}
    }
    
    public void processGene(GFF3Record record, Item feature){
    	// empty right now
    }
    
    public void processTranscript(GFF3Record record, Item feature){
    	// Set gene parent
    	String parentGeneSeqName = stripTypePrefix( record.getAttributes().get("Parent").get(0) );
    	String genePID = mapThisID(parentGeneSeqName);
    	Item gene;
    	String geneID;
    	if(!keyAdded(genePID)){
	    	gene = converter.createItem("Gene");
	    	gene.setAttribute("primaryIdentifier", genePID);
	    	addItem(gene, genePID);
	    	geneID = gene.getIdentifier();
	    	
    	}else{
    		geneID = key2refID.get(genePID);
    	}
    	
    	feature.setReference("gene", geneID);
    }
    
    public void processCDS(GFF3Record record, Item feature){
    	
    	if( record.getAttributes().get("Parent") != null ){
	    	// Set transcript parent
	    	List<String> parentTranscriptNames = record.getAttributes().get("Parent");
	    	Item transcript;
	    	String transcriptID;
	    	for( int i=0; i<parentTranscriptNames.size(); i++){
	    		String parentTranscriptName = parentTranscriptNames.get(i);
		    	if(!keyAdded(parentTranscriptName)){
			    	transcript = converter.createItem("Transcript");
			    	transcript.setAttribute("primaryIdentifier", parentTranscriptName);
			    	addItem(transcript, parentTranscriptName);
			    	transcriptID = transcript.getIdentifier();
		    	}else{
		    		transcriptID = key2refID.get(parentTranscriptName);
		    	}
		    	
		    	feature.addToCollection("transcripts", transcriptID);
	    	}
    	}
    }


}

