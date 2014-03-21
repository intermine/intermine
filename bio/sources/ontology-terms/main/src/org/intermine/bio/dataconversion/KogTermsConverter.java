/**
 * 
 */
package org.intermine.bio.dataconversion;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * @author jcarlson
 *
 */
public class KogTermsConverter extends OntologyTermsFileConverter {
  /**
   * @param writer
   * @param model
   */
  public KogTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "[";
    nameKey = null;
    descKey = null;
    endOfRecord = null;
  }
  void parseIdentifier(String line){
    // a representative line:
    // [OR] KOG0001 Ubiquitin and ubiquitin-like proteins
    // split on each spaces.
    String[] fields = line.split(" ");
    recordNamespace = fields[0].substring(1,fields[0].length()-1);
    identifierLine = fields[1];
    descriptionLine = StringUtils.join(fields," ",2,fields.length);
    try {
    storeRecord();
    } catch (Exception e) { }
  }
}
