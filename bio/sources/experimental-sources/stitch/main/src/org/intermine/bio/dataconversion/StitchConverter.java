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
 * @author: Alex Wray
 */
public class StitchConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "Stitch";
    private static final String DATA_SOURCE_NAME = "Stitch";

    public StitchConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    public void process(Reader reader) throws Exception {
	stitchHandler handler = new stitchHandler(getItemWriter());
	try {
		handler.parseTabbedFile(reader,1,".v1.0.tsv"); //protein-chemical
		handler.parseTabbedFile(reader,0,".v1.0.tsv"); //chemical-chemical
	} catch (Exception e) {
		throw new RuntimeException(e);
	}
    }


	public class stitchHandler
	{
		private ItemWriter writer;
		private BufferedReader in;
		public IdResolver proteinResolver = new IdResolver("protein");
		String readString;
		private Map<String, String> allItemArray = new HashMap<String, String>();

		public stitchHandler(ItemWriter writer) {
			this.writer=writer;
		}

		public void parseTabbedFile(Reader reader,int hasProteins,String suffix) throws IOException, ObjectStoreException, SAXException {

			String filename="/shared/data/stitch/"+((hasProteins==1)?"protein":"chemical")+"_chemical.links"+suffix;

			if (hasProteins==1)
			{
				//need to set up resolver...
				in = new BufferedReader(reader);
				in.readLine();
				while ((readString=in.readLine())!=null)
				{
					String tmp[]=readString.split("\\t");
					{
						if (tmp.length>1 && !tmp[1].equals(""))
						{
							proteinResolver.addSynonyms("9606",tmp[1],new HashSet(Arrays.asList(new String[] {tmp[0]})));
							//System.out.println("Now "+tmp[0]+" resolves to "+proteinResolver.resolveId("9606",tmp[0]));
						}
					}
				}
			}

			in=new BufferedReader(new FileReader(filename));
			in.readLine();
			while ((readString=in.readLine())!=null)
			{
				String[] tmp = readString.split("\\t");

				String itema=getItem(tmp[0],0);
				String itemb=getItem(tmp[1],hasProteins);
				if (!"".equals(itemb) && tmp.length==3)
				{
					if (allItemArray.get(itema+"_"+itemb)!=null || allItemArray.get(itemb+"_"+itema)!=null)
					{
					}
					else {
						Item linkItem=createItem(hasProteins==1?"ProteinChemicalLink":"ChemicalChemicalLink");
						linkItem.setAttribute("linkId",(tmp[0]+"-"+tmp[1]));
						if (hasProteins==1)
						{
							linkItem.setAttribute("combined_score",tmp[2]);
							linkItem.setCollection("proteins",Arrays.asList(new String[] {itemb}));
							linkItem.setCollection("chemicals",Arrays.asList(new String[] {itema}));
						}
						else {
							linkItem.setAttribute("textmining",tmp[2]);
							linkItem.setCollection("chemicals",Arrays.asList(new String[] {itema,itemb}));
						}
						store(linkItem);
						allItemArray.put(itema+"_"+itemb,"");
					}
				}
			}
		}

		public String getItem(String itemId, int isProtein)
		{
			try {
				if (isProtein==1 && !itemId.split("\\.")[0].equals("9606")) return "";
				if (isProtein==1) 
				{
					if (itemId.split("\\.").length==2 && proteinResolver.countResolutions("9606",itemId.split("\\.")[1])>0)
					{
						itemId=proteinResolver.resolveId("9606",itemId.split("\\.")[1]).iterator().next();
					}
					else return "";
				}
				//if (isProtein==1) System.out.println(itemId);
				if (allItemArray.get(itemId) != null)
				{
					return allItemArray.get(itemId);
				} else {
					Item item = createItem((isProtein==1)?"Protein":"Chemical");
					item.setAttribute("primaryIdentifier",itemId);
					store(item);
					allItemArray.put(itemId,item.getIdentifier());
					return item.getIdentifier();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}













