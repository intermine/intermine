/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * @author jcarlson
 *
 */
public class KeggTermsConverter extends OntologyTermsFileConverter {
  
  private HashMap<String,String> beenThereDoneThat = new HashMap<String,String>();
  /**
   * @param writer
   * @param model
   */
  public KeggTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    // Look for lines that start with 'D'. We enter a record for every one.
    identifierKey = "D";
    nameKey = null;
    descKey = null;
    endOfRecord = null;
  }
  void parseIdentifier(String line){
    // a representative line:
    // D      K01623  ALDO; fructose-bisphosphate aldolase, class I [EC:4.1.2.13]
    identifierLine = line.substring(7,13);
    String[] fields = line.substring(15).split("[;\\[]");
    // there are multiple names.
    nameLine = fields[0].trim();
    descriptionLine = fields[1].trim();
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
}
