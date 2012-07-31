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
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
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
 * HGNC parser
 *
 * @author Alex Wray
 */
public class HGNCConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "HGNC";
    private static final String DATA_SOURCE_NAME = "http://www.genenames.org/";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HGNCConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
	HGNCHumanHandler handler = new HGNCHumanHandler(getItemWriter());
        try {
    	    handler.parseTabbedFile(reader);
        } catch (Exception e) {
    	    e.printStackTrace();
    	    throw new RuntimeException(e);
        }
    }

    private class HGNCHumanHandler
    {
		private ItemWriter writer;
		private BufferedReader in;
	    private List<String> values = new Vector<String>(); //this will store the values for each line
	    private List<List<String>> valueRows = new Vector<List<String>>(); //this will store all the lines parsed into a list of vectors
	    private Map<String, Item> genes = new HashMap<String, Item>();
		private Set<String> synonyms = new HashSet();
		private Set<String> publications = new HashSet();
	
		public HGNCHumanHandler(ItemWriter writer) {
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
		    String delimiter = "\\t";
		    String readString;
		    String primaryID;
		    
		    int goodCount=0; //correct (unique) lines
		    int badCount=0; //collisions
		    int unique;
	
		    Item organism = createItem("Organism");
		    organism.setAttribute("taxonId","9606");
		    store(organism);
		    
		    in.readLine(); //First line is junk
	
		    while ((readString = in.readLine()) != null) {
		    	
		    	String[] tmp = readString.split(delimiter); //current line
	
		    	if (tmp.length>=16 && !tmp[15].equals("")) //we need an Ensembl ID to map!
		    	{
		    		unique=1;
		    		for (int i=values.size()-1; i>=0 && unique==1; i--) if (tmp[15].equals(values.get(i))) unique=0;
		    		if (unique==1)
		    		{
		    			goodCount++; //keep a track
		    			values.add(tmp[15]); //add to list of unique Ensembl IDs
		    			
		    			//create gene:
		  	  	    	Item gene = createItem("Gene");
		  	  	    	gene.setAttribute("primaryIdentifier", tmp[15]); //HGNC_ID
		  	  	    	gene.setReference("organism",organism);
		    			
		    			//Handle ID=2; name:
		    			gene.setAttribute("name",tmp[2]);
		    			


		    			//debug:
		    			//System.out.println(tmp[15]+" "+goodCount+" "+badCount);
		    			
		    			//now, store synonyms:
						for (int i=0; i<tmp.length; i++) {
							if (i==5 || i==6 || i==7 || i==8) //Aliases, Name Aliases, Specialist IDs
							{
								if (i==8)
								{
							        	Set<String> pubs = new HashSet<String>();
									String[] tmp2 = tmp[i].split(", ");
									for (int j=0; j<tmp2.length; j++) 
									{
										if (!tmp2[j].equals("") && !publications.contains(tmp2[j]))
										{
											Item publication = createItem("Publication");
											publication.setAttribute("pubMedId",tmp2[j]);
											store(publication);
											publications.add(tmp2[j]);
											pubs.add(publication.getIdentifier());
										}
									}
									gene.setCollection("publications",new ArrayList(pubs));
								}
								if (i==5 || i==6 || i==7) 
								{
									String[] tmp2 = tmp[i].split(", ");
									for (int j=0; j<tmp2.length; j++) if (!tmp2[j].equals("")) setSynonym(gene.getIdentifier(),"identifier",tmp2[j]);
								}
							}
							else //just add to Synonym list
							{
								if (!tmp[1].equals("")) gene.setAttribute("symbol",tmp[1]);
								if (!tmp[i].equals("")) setSynonym(gene.getIdentifier(),"identifier",tmp[i]);
							}	
						}

	    				store(gene); //store the gene
		    			}
		    		else badCount++;
				}
			}
	
		    /*
	
			//HGNC has very occasional mistakes (incorrect ensembl IDs); check to make sure this doesn't affect us!
	
			if ("".equals(values.size()>=9 && !values.get(8))) //check our current gene has a valid ensembl ID
			{
			    //System.out.println(i + "/" + valueRows.size()+"; "+goodCount+" v "+badCount);
			    int unique=1;
			    String curPrim=values.get(8);
			    for (int k=i-1; k>=0; k--)
			    {
				if (valueRows.get(k).size()>=9 && valueRows.get(k).get(8).equals(curPrim)) unique=0; //check each in turn...
			    }
		    	    if (unique==1) 
			    {
				goodCount++;
				store(gene); //yay, unique - continue...
			    }
			    else badCount++;
			}
		    }
		    System.out.println(goodCount + " unique IDs incorporate; " + badCount + " duplicates discarded.");
		    */
		}
    }
}
