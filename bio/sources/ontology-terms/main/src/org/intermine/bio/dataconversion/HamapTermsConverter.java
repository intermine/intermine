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
public class HamapTermsConverter extends OntologyTermsFileConverter {

  /**
   * @param writer
   * @param model
   */
  public HamapTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "AC";
    nameKey = "ID";
    descKey = "DE";
  }
  @Override
  public String cleanId(String id) {
    // strip off the final ;
    return id.replaceAll(";$","");
  }
  @Override
  public String cleanName(String name) {
    // strip off the ; and everything after it.
    return name.replaceAll(";\\.*$","");
  }
  @Override
  public String cleanDescription(String desc) {
    // loose the period
    return desc.replaceAll("\\.$", "");
  }
}
