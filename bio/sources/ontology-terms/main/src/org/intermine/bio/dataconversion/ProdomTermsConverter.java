/**
 * 
 */
package org.intermine.bio.dataconversion;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;

import java.util.HashMap;

/**
 * @author jcarlson
 *
 */
public class ProdomTermsConverter extends OntologyTermsFileConverter {

  
  HashMap<String,String> seenIt = new HashMap<String,String>();
  /**
   * @param writer
   * @param model
   */
  public ProdomTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = ">";
    nameKey = null;
    descKey =null;
    endOfRecord = null;
  }
  @Override
  void parseIdentifier(String line) {
    // split on space
    String[] fields = line.split("\\|");
    // identifier is the word after the > delimited by #
    String[] subFields = fields[0].split("#");
    identifierLine = subFields[1].trim();
    if (seenIt.containsKey(identifierLine)) {
      return;
    }
    descriptionLine = fields[3].trim().replaceAll("^\\(\\d+\\) *","");
    seenIt.put(identifierLine,descriptionLine);
    try {
      // identifierLine is null'ed after the store. So subsequent things are tossed.
      storeRecord();
    } catch (Exception e) {}
  }
}
