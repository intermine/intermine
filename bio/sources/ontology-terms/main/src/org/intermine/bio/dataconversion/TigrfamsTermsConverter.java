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
public class TigrfamsTermsConverter extends OntologyTermsFileConverter {
 
  /**
   * @param writer
   * @param model
   */
  public TigrfamsTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "ACC";
    nameKey = "NAME";
    descKey = "DESC";
  }

}
