package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.Reader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
 * @author Alex Wray
 */
public class HuGEConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "HuGE";
    private static final String DATA_SOURCE_NAME = "HuGE";

    /**
     * Constructor
     */
    public HuGEConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    public void process(Reader reader) throws Exception {
	HuGEHandler handler = new HuGEHandler(getItemWriter());
	try {
		handler.parseTabbedFile(reader);
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	}
    }

	
	public class HuGEHandler
	{
		private ItemWriter writer;
		private BufferedReader in;
		private List<String> values = new Vector<String>(); //this will store the values for each line
		private List<List<String>> valueRows = new Vector<List<String>>(); //this will store all the lines parsed into a list of vectors
		private Map<String, Item> genes = new HashMap<String, Item>();
		private Set<String> synonyms = new HashSet();
		public IdResolver geneResolver = new IdResolver("gene");

		private Map<String, String> geneMap = new HashMap<String, String>();
		private Map<String, geneHolder> geneHolderMap = new HashMap<String, geneHolder>();
		private Map<String, diseaseHolder> diseaseMap = new HashMap<String, diseaseHolder>();
		private Map<String, String> publicationMap = new HashMap<String, String>();

		String genePrimaryIdentifier;
		String publicationIdentifier;
		String diseaseIdentifier;
		String geneRefId;
		String publicationRefId;
	
		public HuGEHandler(ItemWriter writer) {
		    this.writer=writer;
		}

        private void setSynonym(String subjectRefId, String type, String value)
        throws SAXException {
            String key = subjectRefId + type + value;
            if (!synonyms.contains(key)) {
                Item synonym = createItem("Synonym");
                synonym.setAttribute("type", type);
                synonym.setAttribute("value", value);
                synonym.setReference("subject", subjectRefId);
                synonyms.add(key);
                try {
                    store(synonym);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
	        }
	    }

		public void parseTabbedFile(Reader reader) throws IOException, ObjectStoreException, SAXException {
			in = new BufferedReader(reader);
			String delimeter = "\\t";
			String readString;
			String primaryID;
	

			try {
			    String delimiter = "\\t";
			    BufferedReader in = new BufferedReader(new FileReader("/shared/data/HGNC/HomoSapiens/6_07_2009.dat"));
			    in.readLine();
			    while ((readString = in.readLine()) != null) {
				String[] tmp = readString.split(delimiter);
		
				if (tmp.length>=9) 
				{
				    geneResolver.addSynonyms("9606",tmp[8],new HashSet(Arrays.asList(new String[] {tmp[1]}))); //Just the approved symbol this time
				    System.out.println(tmp[1]+" now resolves to "+geneResolver.resolveId("9606",tmp[1]).iterator().next());
				}
			    }
			} catch (Exception e) {
			    e.printStackTrace();
			    throw new RuntimeException(e);
			}



			Item organism = createItem("Organism");
			organism.setAttribute("taxonId","9606");
			store(organism);
	
			in.readLine(); //First line is junk
	
			while ((readString = in.readLine()) != null) {
	
				String[] tmp = readString.split(delimeter);

				//[0]:GeneSymbol [1]:Disease [2]:PubMedID [3]:Title [4]:Publication [5]:Authors

System.out.println("Pre: "+tmp.length+"; "+tmp[0]+"; Resolutions: "+geneResolver.countResolutions("9606",tmp[0]));

				if (geneResolver.countResolutions("9606",tmp[0])>0) {

					genePrimaryIdentifier=geneResolver.resolveId("9606",tmp[0]).iterator().next();
					publicationIdentifier=tmp[2];
					diseaseIdentifier=tmp[1];

					geneHolder genH;	
	
					if (geneMap.get(genePrimaryIdentifier) == null) {
						Item gene = createItem("Gene");
						genH=new geneHolder(genePrimaryIdentifier,gene,organism.getIdentifier());
						geneRefId = gene.getIdentifier();
						geneMap.put(genePrimaryIdentifier,geneRefId);
						geneHolderMap.put(genePrimaryIdentifier,genH);

						//gene.setAttribute("primaryIdentifier",genePrimaryIdentifier);
						//gene.setReference("organism",organism.getIdentifier());
						//store(gene);
					}
					else {
						geneRefId=geneMap.get(genePrimaryIdentifier);
						genH=geneHolderMap.get(genePrimaryIdentifier);
					}			
	
					if (publicationMap.get(publicationIdentifier) == null) {
						Item publication = createItem("Publication");
						publication.setAttribute("firstAuthor",tmp[5]);
						publication.setAttribute("journal",tmp[4]);
						publication.setAttribute("title",tmp[3]);
						publication.setAttribute("pubMedId",tmp[2]);
						publicationRefId = publication.getIdentifier();
						store(publication);
						publicationMap.put(publicationIdentifier,publicationRefId);
					}
					else {
						publicationRefId=publicationMap.get(publicationIdentifier);
					}	

					diseaseHolder disease;

					if (diseaseMap.get(diseaseIdentifier) == null) {
						disease = new diseaseHolder(diseaseIdentifier);
						diseaseMap.put(diseaseIdentifier,disease);
					}
					else {
						disease=diseaseMap.get(diseaseIdentifier);
					}

					genH.publicationSet.add(publicationRefId);
					disease.geneSet.add(geneRefId);
					disease.publicationSet.add(publicationRefId);
				}
			}

			for ( Map.Entry<String,geneHolder> entry : geneHolderMap.entrySet() )
			{
				Item gene=entry.getValue().item;
				gene.setAttribute("primaryIdentifier",entry.getValue().id);
				gene.setReference("organism",entry.getValue().organismRef);
				gene.setCollection("publications",new ArrayList(entry.getValue().publicationSet));
				store(gene);
			}

			for ( Map.Entry<String,diseaseHolder> entry : diseaseMap.entrySet() )
			{
				Item disease = createItem("Disease");
				Item synonym = createItem("Synonym");
				disease.setAttribute("diseaseId",entry.getValue().id);
				disease.setCollection("associatedGenes",getAssociatedGenes(entry.getValue()));
				disease.setCollection("associatedPublications",getAssociatedPublications(entry.getValue()));
				store(disease);
				setSynonym(disease.getIdentifier(),"identifier",entry.getValue().id);
			}
		}



	        private ArrayList<String> getAssociatedGenes(diseaseHolder holder) {
	            ArrayList<String> genes = new ArrayList(holder.geneSet);
	            return genes;
	        }
	
	        private ArrayList<String> getAssociatedPublications(diseaseHolder holder) {
	            ArrayList<String> publications = new ArrayList(holder.publicationSet);
	            return publications;
	        }
	
		public class diseaseHolder
		{
	            public Set<String> geneSet = new HashSet<String>();
	            public Set<String> publicationSet = new HashSet<String>();
		    String id;
	
		    public diseaseHolder(String id) {
			this.id = id;
		    }
		}

		public class geneHolder
		{
		    Item item;
		    String organismRef;
	            public Set<String> publicationSet = new HashSet<String>();
		    String id;
		    public geneHolder(String primaryIdentifier,Item item,String organismRef) {
			this.id = primaryIdentifier;
			this.item = item;
			this.organismRef = organismRef;
		    }
		}


	}
}











