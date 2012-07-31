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
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.DirectoryConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.util.Util;
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * 
 * @author
 */
public class chebiDataConverter extends DirectoryConverter
{
    //
    private static final String DATASET_TITLE = "CHEBI";
    private static final String DATA_SOURCE_NAME = "CHEBI";

	public IdResolver chemResolver = new IdResolver("chemicals");
	private Map<String, Item> CHEBITermArray = new HashMap<String, Item>();
	private Map<String, Item> chebiChemicalArray = new HashMap<String, Item>();
	private Map<String, Item> primChemicalArray = new HashMap<String, Item>();
	public Item organism;
	public String[] lineVals;
	public String delimeter;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public chebiDataConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    @Override
    public void process(File dataDir) throws Exception {

	delimeter = "\\t";
	organism = createItem("Organism");
	organism.setAttribute("taxonId","9606");
	store(organism);

	File[] fileList=dataDir.listFiles();
	String readString;
	for (File file : fileList)
	{
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			in.readLine(); //first line always junk
			while ((readString = in.readLine()) != null) {
				if ("compounds.tsv".equals(file.getName()))
					parseCompoundLine(readString);
				if ("comments.tsv".equals(file.getName()))
					parseCommentsLine(readString);
				if ("chebiId_inchi.tsv".equals(file.getName()))
					parseInchiLine(readString);
				if ("names.tsv".equals(file.getName()))
					parseNamesLine(readString);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	for ( Map.Entry<String,Item> entry : chebiChemicalArray.entrySet() )
	{
		Item chem=entry.getValue();
		store(chem);
	}

	for ( Map.Entry<String,Item> entry : CHEBITermArray.entrySet() )
	{
		Item term=entry.getValue();
		store(term);
	}

    }


	public void parseCompoundLine(String inLine)
	{
		lineVals = inLine.split(delimeter);
		if (lineVals.length>4)
		{
			Item curChem = getChemical( lineVals[2] );
			Item curTerm = getCHEBITerm( lineVals[2] );
			curChem.setAttribute("systematicName",lineVals[5]);
			curChem.addToCollection("CHEBIAnnotations",curTerm.getIdentifier());
		}
	}

	public void parseCommentsLine(String inLine)
	{
		lineVals = inLine.split(delimeter);
		if (lineVals.length>4 && !lineVals[1].equals(""))
		{
			Item curChem = getChemical(lineVals[1]);
			curChem.setAttribute("comment",lineVals[5]);
		}
	}

	public void parseInchiLine(String inLine)
	{
		lineVals = inLine.split(delimeter);
		if (lineVals.length>1)
		{
			Item curChem = getChemical(lineVals[0]);
			curChem.setAttribute("InChI_Identifier",lineVals[1]);
		}
	}

	public void parseNamesLine(String inLine) throws Exception
	{
		lineVals = inLine.split(delimeter);
		if (lineVals.length>6)
		{
			Item curChem = getChemical(lineVals[1]);
			if (lineVals[2].equals("NAME"))
				curChem.setAttribute("shortName",lineVals[4]);
			else
			{
				Item syn = createItem("ChemicalSynonym");
				syn.setAttribute("value",lineVals[4]);
				curChem.addToCollection("synonyms",syn);
				store(syn);
			}
		}
	}

    public void setChebiResolver(String resolverPath) {
	try {
		BufferedReader in = new BufferedReader(new FileReader(resolverPath));
		in.readLine();
		String readString;	
		while ((readString = in.readLine()) != null) {
			String[] tmp = readString.split("\\t");	
			if (tmp.length>=3 && !tmp[0].equals("") && tmp[1].equals("ChEBI")) 
			{
				chemResolver.addSynonyms("9606",tmp[0],new HashSet(Arrays.asList(new String[] {tmp[2]})));
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	}
    }

		public Item getChemical(String ChebiId)
		{
			if ( "CHE".equals(ChebiId.length()>2 && !ChebiId.substring(0,3)) )
			{
				ChebiId="CHEBI:"+ChebiId;
			}

			try {
				if ( chebiChemicalArray.get(ChebiId) != null)
				{
					return chebiChemicalArray.get(ChebiId); //check for this CHEBI Id in DB
				} else if (chemResolver.countResolutions("9606",ChebiId)>0 && primChemicalArray.get(chemResolver.resolveId("9606",ChebiId).iterator().next()) != null) {
					return primChemicalArray.get(chemResolver.resolveId("9606",ChebiId).iterator().next()); //or return equivalent according to resolution
				} else {
					Item chebiChemical = createItem("Chemical");
					chebiChemical.setAttribute("CHEBIId",ChebiId);
					if (chemResolver.countResolutions("9606",ChebiId)>0)
					{
						chebiChemical.setAttribute("primaryIdentifier",chemResolver.resolveId("9606",ChebiId).iterator().next());
						primChemicalArray.put(chemResolver.resolveId("9606",ChebiId).iterator().next(),chebiChemical);
					}
					chebiChemicalArray.put(ChebiId,chebiChemical);//bit of a fiddle to avoid multiply-associated chemicals
					return chebiChemicalArray.get(ChebiId);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		public Item getCHEBITerm(String termid)
		{
			try {
				if (CHEBITermArray.get(termid) != null)
				{
					return CHEBITermArray.get(termid);
				} else {
					Item CHEBITerm = createItem("CHEBITerm");
					CHEBITerm.setAttribute("identifier",termid);
					CHEBITermArray.put(termid,CHEBITerm);
					return CHEBITermArray.get(termid);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

}
