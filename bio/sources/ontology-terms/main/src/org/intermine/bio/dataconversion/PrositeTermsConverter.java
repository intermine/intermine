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
public class PrositeTermsConverter extends OntologyTermsFileConverter {

  /**
   * @param writer
   * @param model
   */
  public PrositeTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "^AC.*";
    nameKey = "^ID.*; PATTERN.";
    descKey = "^DE.*";
    identifierReplacement = "^AC\\s*";
    nameReplacement = "^ID\\s*";
    descReplacement = "^DE\\s*";
    namespaceReplacement = null;
  }
  // @Override
  String cleanId(String identifier) {
    // remove final semicolor. It's not a sentence!
    return identifier.replaceAll(";$", "");
  }
  // @Override
  String cleanDescription(String desc) {
    // remove final period. It's not a sentence!
    return desc.replaceAll("\\.$", "");
  }
  // @Override
  String cleanName(String name) {
    // this consists of a name ":" and PATTERN or MATRIX.
    if (name==null) return null;
    String[] fields = name.split(";");
    if (fields.length < 1) return null;
    name = fields[0].trim();
    return name;
  }
}