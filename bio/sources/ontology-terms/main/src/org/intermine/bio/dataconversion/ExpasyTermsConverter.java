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
public class ExpasyTermsConverter extends OntologyTermsFileConverter {
 
  /**
   * @param writer
   * @param model
   */
  public ExpasyTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "^ID.*";
    nameKey = "^AN.*";
    descKey = "^DE.*";
  }
  //@Override
  String cleanDescription(String desc) {
    return desc.replace("\\.$", "");
  }
}
