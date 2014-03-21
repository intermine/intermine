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
public class ProfileTermsConverter extends OntologyTermsFileConverter {
  
  /**
   * @param writer
   * @param model
   */
  public ProfileTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "AC";
    nameKey = "ID";
    descKey = "DE";
  }
  // @Override
  String cleanId(String identifier) {
    // remove final semicolor. It's not a sentence!
    return identifier.replace("\\;$", "");
  }
  // @Override
  String cleanDescription(String desc) {
    // remove final period. It's not a sentence!
    return desc.replace("\\.$", "");
  }
  // @Override
  String cleanName(String name) {
    // this consists of a name ":" and PATTERN or MATRIX.
    String[] fields = name.split(";");
    name = fields[0].trim();
    if (fields[1] != null) {
      recordNamespace = fields[1].trim().replace("\\.$", "");
    }
    return name;
  }
}
