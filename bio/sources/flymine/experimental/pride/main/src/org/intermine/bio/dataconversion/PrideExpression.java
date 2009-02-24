package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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
 * @author Dominik Grimm and Michael Menden
 */

class PrideExpression 
{
       
       //private fields
       private Pattern accession;
       private Pattern swissport;
       private Pattern identifier;
       private Matcher matcher;
       private String  text;
       
       //private field: contains how many accessions and identifiers were in the String array
       private int              accessionCounter;
       private int              identifierCounter;
       
       /**
        * Constructor
        * @param text text
        */
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
              
              if (findSwissport()) {
                     matcher = accession.matcher(text);
                     //counter...how much ids are found
                     while (matcher.find()) {
                            accessionCounter++;
                     }
                     matcher = identifier.matcher(text);
                     while (matcher.find()) {
                            identifierCounter++;
                     }
              }
       }
       
       /**
        * find Swissprot
        * @return true if SWISSPROT is found in the text
        */
       //if SWISSPORT is in the text this method will return true
       public boolean findSwissport() {
              //set matcher to pattern text
              matcher = swissport.matcher(text);
              //find SWISSPROT pattern
              if (matcher.find()) {
                     return true;
              }
              
              return false;
       }
       
       /**
        * getAccession
        * @return String-Array of accessionIds
        */
       //get the Accession ids from the text
       public String[] getAccession() {
              //set matcher to pattern accession
              matcher = accession.matcher(text);

              String[] ids = new String[accessionCounter];

              //find ids and store it
              for (int i = 0; i < ids.length; i++) {
                     matcher.find();
                     ids[i] = text.substring(matcher.start(), matcher.end() - 2);
              }       
              return ids;
       }
       
       /**
        * getIdentifier
        * @return String-Array of identifiers
        */
       public String[] getIdentifier() {
              //set matcher to pattern identifier
              matcher = identifier.matcher(text);

              String[] ids = new String[identifierCounter];

              //find ids and store it
              for (int i = 0; i < ids.length; i++) {
                     matcher.find();
                     ids[i] = text.substring(matcher.start(), matcher.end());
              }       
              return ids;
       }

       /**
        * getAccessionCounter
        * @return numbers of accessionIds
        */
       public int getAccessionCounter() {
           return accessionCounter;
       }

       /**
        * getIdentifierCounter
        * @return numbers of identifierIds
        */
       public int getIdentifierCounter() {
           return identifierCounter;
       }
       
}
