/**
 * 
 */
package org.intermine.bio.dataconversion;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;

/**
 * @author jcarlson
 *
 */
public class PrintsTermsConverter extends OntologyTermsFileConverter {

  /**
   * @param writer
   * @param model
   */
  public PrintsTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    // prints.dat does not have an end of record indicator. But it looks
    // like there are always 3 entries and the description comes last.
    identifierKey = "^gx;.*";
    nameKey = "^gc;.*";
    descKey = "^gt;.*";
    endOfRecord = null;
  }
  void parseDescription(String line) {
    // parse normally. then store
    super.parseDescription(line);
      try {
        storeRecord();
      } catch (Exception e) {}
    }
}
