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
public class Gene3DTermsConverter extends OntologyTermsFileConverter {

  /**
   * @param writer
   * @param model
   */
  public Gene3DTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "NAME";
    nameKey = null;
    descKey = null;
    namespaceKey = null;
  }

}
