/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * @author jcarlson
 *
 */
public class PantherTermsConverter extends OntologyTermsFileConverter {
  
  private static final Logger LOG = Logger.getLogger(PantherTermsConverter.class);
  protected HashMap<String,String> idMap = new HashMap<String,String>();
  protected HashMap<String,String> parentMap = new HashMap<String,String>();
  /**
   * @param writer
   * @param model
   */
  public PantherTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    // we're going to process each line
    identifierKey = null;
    nameKey = null;
    descKey = null;
    endOfRecord = null;
  }
  @Override
  boolean parseLine(String line) {
    String[] fields = line.split("\\t");
    if (fields.length >= 2) {
      String p1 = fields[0].trim().replace(".mod","");
      identifierLine = p1.trim().replace(".mag","");
      descriptionLine = fields[1].trim();
      if (identifierLine.matches(".*\\.SF\\d+$")) {
        parentMap.put(identifierLine, identifierLine.replaceAll("\\.SF\\d+$", ""));
      }
      idMap.put(identifierLine, createOntologyTerm().getIdentifier());
    }
    return true;
  }
  @Override
  public void finalProcessing() {
    Set<Item> relations = new HashSet<Item>();
    for( String child : parentMap.keySet()) {
      if (idMap.get(parentMap.get(child))==null) {
        // there are some Parent= tags that refer to obsolete(?) terms. Skip
        LOG.info("No id for parent of child " + child +":"+ parentMap.get(child));
        continue;
      }
      Item relation = createItem("OntologyRelation");
      relation.setReference("parentTerm", idMap.get(parentMap.get(child)));
      relation.setReference("childTerm", idMap.get(child));
      relation.setAttribute("relationship", "part_of");
      relation.setAttribute("direct", "true");
      relation.setAttribute("redundant", "false");
      // Set the reverse reference
      termMap.get(idMap.get(child)).addToCollection("relations", relation.getIdentifier());
      termMap.get(idMap.get(parentMap.get(child))).addToCollection("relations", relation.getIdentifier());
      relations.add(relation);
    }
    // now store the items
    for(String item : termMap.keySet()) {
      try {
        store(termMap.get(item));
      } catch (Exception e) {}
    }
    for(Item relation: relations) {
      try {
        store(relation);
      } catch (Exception e) {}
    }
  }
}
