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
    identifierKey = "^#=GF AC.*";
    identifierReplacement = "^#=GF AC\\s*";
    nameKey = "^#=GF ID.*";
    nameReplacement = "^#=GF ID\\s*";
    descKey = "^#=GF DE.*";
    descReplacement = "^#=GF DE\\s*";
    
  }
  @Override
  String cleanId(String id) {
    // strip off version number
    return id.replaceAll("\\.\\d+$","");
  }
}
