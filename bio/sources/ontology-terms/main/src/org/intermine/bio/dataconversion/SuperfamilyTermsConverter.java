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
    identifierKey = "ACC";
    nameKey = null;
    descKey = "DESC";
  }
  //@Override
}
