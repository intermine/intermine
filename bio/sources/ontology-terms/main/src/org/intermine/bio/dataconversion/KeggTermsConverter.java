/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * @author jcarlson
 *
 */
public class KeggTermsConverter extends OntologyTermsFileConverter {
  
  private HashMap<String,String> beenThereDoneThat = new HashMap<String,String>();
  private ArrayList<String> ancestry;
  /**
   * @param writer
   * @param model
   */
  public KeggTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    // Look for lines that start with 'A' - 'D'. We enter a record for every one.
    // the parent is the last one with the earlier letter.
    identifierKey = "^[ABCD].*";
    nameKey = null;
    descKey = null;
    endOfRecord = null;
  }
  void parseIdentifier(String line){
    // a representative lines:
    //A id1  Metabolism
    //B
    //B id2 Overview
    //C    01200 Carbon metabolism [PATH:ko01200]
    //D      K01623  ALDO; fructose-bisphosphate aldolase, class I [EC:4.1.2.13]


    char level = line.charAt(0);
    // the first field separated by space, then everything else.
    String[] lineFields = line.substring(1).trim().split("\\s+",2);
    if(lineFields.length < 1) return;
    
    identifierLine = cleanId(lineFields[0]);
    
    if (identifierLine.isEmpty()) return;

    if (level == 'A') {
      ancestry = new ArrayList<String>();
      ancestry.add(identifierLine);
    } else {
      if( ancestry.size() < (level-'A') ) {
        throw new BuildException("There is an inconsistent parent-child relationship.");
      }
      ancestry.add(level-'A',identifierLine);
      for(int goingUp = level-'A'-1; goingUp >= 0; goingUp--) {
        parentChildren.add(new ParentChild(ancestry.get(goingUp),identifierLine,
            (goingUp==(level-'A'-1)?true:false)));
      }
    }
    if (lineFields.length > 1 ) {
      String[] fields = lineFields[1].split("[;\\[]");
      // there are multiple names.
      nameLine = fields[0].trim();
      if(fields.length > 1  && !fields[1].endsWith("]")) {
        descriptionLine = fields[1].trim();
      }
    } else {
      // A and B levels do not have a name. Only a (long) identifier.
      // copy this into the name
      nameLine = identifierLine;
    }
    // identifiers occur in multiple sections. We're just going to insert
    // them uniquely
    if (!beenThereDoneThat.containsKey(identifierLine)) {
      beenThereDoneThat.put(identifierLine, nameLine);
      try {
        storeRecord();
      } catch (Exception e) { }
    } else {
      // null out record
      identifierLine = null;
      nameLine = null;
      descriptionLine = null;
    }
  }
  String cleanId(String a) {
    return a.trim().replaceAll("<b>", "").replaceAll("</b>","");
  }
}
