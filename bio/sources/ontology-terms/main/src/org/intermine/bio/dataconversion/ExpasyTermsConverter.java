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
    identifierReplacement = "^ID\\s*";
    nameReplacement = "^AN\\s*";
    descReplacement = "^DE\\s*";
  }
  //@Override
  String cleanDescription(String desc) {
    if (desc==null) return null;
    return desc.trim().replaceAll("\\.$", "");
  }
  //@Override
  String cleanName(String name) {
    if (name==null) return null;
    return name.trim().replaceAll("\\.$", "");
  }
}
