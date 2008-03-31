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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RegularExpression scanner
 * @author Dominik Grimm
 */

class PrideExpression{
	
	//private fields
	private Pattern accession;
	private Pattern swissport;
	private Pattern identifier;
	private Matcher matcher;
	private String  text;
	
	//public field: contains how many accessions and identifiers were in the String array
	public int		accessionCounter;
	public int		identifierCounter;
	
	//initialise parser
	public PrideExpression(String text) {
		this.text = text;
		//set regular expression for accessionId
		accession = Pattern.compile("\\w*\\p{Lu}\\w+\\.\\d\\w*");
		//set regular expression for identifier
		identifier = Pattern.compile("\\w*\\p{Lu}\\w+\\_\\w*");
		//set regular expression for SWISSPORT
		swissport = Pattern.compile("^S\\w+[P]\\w+[T]\\:\\w*");
		
		accessionCounter = 0;
		identifierCounter = 0;
		
		if(findSwissport()) {
			matcher = accession.matcher(text);
			//counter...how much ids are found
			while(matcher.find())
				accessionCounter++;
			matcher = identifier.matcher(text);
			while(matcher.find())
				identifierCounter++;
		}
	}
	
	//if SWISSPORT is in the text this method will return true
	public boolean findSwissport() {
		//set matcher to pattern text
		matcher = swissport.matcher(text);
		//find SWISSPROT pattern
		if(matcher.find())
			return true;
		
		return false;
	}
	
	//get the Accession ids from the text
	public String[] getAccession() {
		//set matcher to pattern accession
		matcher = accession.matcher(text);

		String[] ids = new String[accessionCounter];

		//find ids and store it
		for(int i=0; i < ids.length; i++) {
			matcher.find();
			ids[i] = text.substring(matcher.start(), matcher.end());
		}	
		return ids;
	}
	
	public String[] getIdentifier() {
		//set matcher to pattern identifier
		matcher = identifier.matcher(text);

		String[] ids = new String[identifierCounter];

		//find ids and store it
		for(int i=0; i < ids.length; i++) {
			matcher.find();
			ids[i] = text.substring(matcher.start(), matcher.end());
		}	
		return ids;
	}
	
}