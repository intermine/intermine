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
public class SuperfamilyTermsConverter extends OntologyTermsFileConverter {

  /**
   * @param writer
   * @param model
   */
  public SuperfamilyTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "^ACC\\s.*";
    identifierReplacement = "^ACC\\s*";
    nameKey = "^NAME\\s.*";
    nameReplacement = "^NAME\\s*";
    descKey = "^DESC\\s.*";
    descReplacement = "^DESC\\s*";
  }
  //@Override
  String cleanId(String id) {
    // If we see digits, prepend 'SSF'
    return id.startsWith("SSF")?id:"SSF"+id;
  }
}
