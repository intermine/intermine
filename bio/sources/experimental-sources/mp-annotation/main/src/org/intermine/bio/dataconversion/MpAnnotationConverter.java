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
 * 
 * @author
 */
public class MpAnnotationConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "MP-annotation";
    private static final String DATA_SOURCE_NAME = "MP-annotation";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public MpAnnotationConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
	MPHandler handler = new MPHandler(getItemWriter());
	try {
		handler.parseTabbedFile(reader);
	} catch (Exception e) {
		throw new RuntimeException(e);
	}
    }

	public class MPHandler
	{
		private ItemWriter writer;
		private BufferedReader in;
		private Map<String, String> MPTermArray = new HashMap<String, String>();
		public IdResolver geneResolver = new IdResolver("gene");
		//public IdResolver mouseGeneResolver = new IdResolver("gene");
		public Set<String> existingGenes = new HashSet<String>();

		public MPHandler(ItemWriter writer) {
			this.writer=writer;
		}

		public void parseTabbedFile(Reader reader) throws IOException, ObjectStoreException, SAXException {
			in = new BufferedReader(reader);
			String delimeter = "\\t";
			String readString;
		
			try {
				BufferedReader in = new BufferedReader(new FileReader("/shared/data/HGNC/HomoSapiens/6_07_2009.dat"));
				in.readLine();
				while ((readString = in.readLine()) != null) {
					String[] tmp = readString.split(delimeter);		
					if (tmp.length>=9) 
					{
						geneResolver.addSynonyms("9606",tmp[8],new HashSet(Arrays.asList(new String[] {tmp[1]}))); //Just the approved symbol this time
					}
				}
				in = new BufferedReader(new FileReader("/shared/data/MGI/Mouse.txt"));
				in.readLine();
				while ((readString = in.readLine()) != null) {
					String[] tmp = readString.split(delimeter);		
					if (tmp.length>=3 && !tmp[0].equals("")) 
					{
						geneResolver.addSynonyms("10060",tmp[2],new HashSet(Arrays.asList(new String[] {tmp[0]}))); //Just the approved symbol this time
						System.out.println("Now "+tmp[0]+" resolves to "+geneResolver.resolveId("10060",tmp[0]));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			Item organism = createItem("Organism");
			organism.setAttribute("taxonId","9606");
			store(organism);

			Item mouseOrganism = createItem("Organism");
			mouseOrganism.setAttribute("taxonId","10090");
			store(mouseOrganism);

			while ((readString = in.readLine()) != null) {
				String[] lineVals = readString.split(delimeter);

				Set<String> refSet = new HashSet<String>();

				if (lineVals.length>4)
				{
					String[] mpVals = lineVals[4].substring(2).split(" ");	
					for (int i=0; i<mpVals.length; i++)
					{
						refSet.add(getMPTerm(mpVals[i]));
					}
				}

				if (lineVals[3].length()>2) lineVals[3]=lineVals[3].substring(2);

				if (geneResolver.countResolutions("9606",lineVals[0])>0 && !existingGenes.contains(geneResolver.resolveId("9606",lineVals[0]).iterator().next())) {
					existingGenes.add(geneResolver.resolveId("9606",lineVals[0]).iterator().next());
					Item gene = createItem("Gene");
					gene.setAttribute("primaryIdentifier",geneResolver.resolveId("9606",lineVals[0]).iterator().next());
					gene.setCollection("MPAnnotations",new ArrayList(refSet));
					store(gene);
				}

				System.out.println("'"+lineVals[3]+"' - '"+geneResolver.resolveId("10060",lineVals[3])+"'");
					
				if (geneResolver.countResolutions("10060",lineVals[3])>0 && !existingGenes.contains(geneResolver.resolveId("10060",lineVals[3]).iterator().next())) {
					System.out.println("Adding mouse gene "+geneResolver.resolveId("10060",lineVals[3]).iterator().next());
					existingGenes.add(geneResolver.resolveId("10060",lineVals[3]).iterator().next());
					Item mouseGene = createItem("Gene");
					mouseGene.setReference("organism",mouseOrganism.getIdentifier());
					mouseGene.setAttribute("primaryIdentifier",geneResolver.resolveId("10060",lineVals[3]).iterator().next());
					mouseGene.setAttribute("MGIId",lineVals[3]);
					mouseGene.setCollection("MPAnnotations",new ArrayList(refSet));
					store(mouseGene);
				}

			}
		}

		public String getMPTerm(String termid)
		{
			try {
				if (MPTermArray.get(termid) != null)
				{
					return MPTermArray.get(termid);
				} else {
					Item mpterm = createItem("MPTerm");
					mpterm.setAttribute("identifier",termid);
					store(mpterm);
					MPTermArray.put(termid,mpterm.getIdentifier());
					return MPTermArray.get(termid);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

	}
}






