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
public class PfamTermsConverter extends OntologyTermsFileConverter {

  /**
   * @param writer
   * @param model
   */
  public PfamTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "#=GF AC";
    nameKey = "#=GF ID";
    descKey = "#=GF DE";
    
  }
  @Override
  String cleanId(String id) {
    // strip off version number
    return id.replaceAll("\\.\\d+$","");
  }
}
