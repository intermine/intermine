/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.*;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * @author jcarlson
 *
 */
public class PirTermsConverter extends OntologyTermsFileConverter {

  private static final Logger LOG = Logger.getLogger(PirTermsConverter.class);
  protected HashMap<String,String> idMap = new HashMap<String,String>();
  protected HashMap<String,String> parentMap = new HashMap<String,String>();
  protected Pattern parentPattern = Pattern.compile("\\[Parent=(\\w+)\\]$");
  /**
   * @param writer
   * @param model
   */
  public PirTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "^>.*";
    identifierReplacement = "^>\\s*";
    nameKey = null;
    descKey =null;
    endOfRecord = null;
  }
  void parseIdentifier(String line) {
    // split on space
    String[] fields = line.split(" +");
    // identifier is the word after the >
    identifierLine = fields[0].replaceAll(identifierReplacement,"").trim();
    // skip the curation status field. And parent field.
    StringBuffer d = new StringBuffer(line.length());
    for(int i=2;i<fields.length;i++) {
      // look for "[Parent=\w+]" field
      Matcher m = parentPattern.matcher(fields[i]);
      if (! m.matches()) {
        if ( i > 2) d.append(" ");
        d.append(fields[i]);
      } else {
        m.reset();
        while( m.find() ) {
          LOG.info("Found Parent tag in " + m.group(1) + ": " + m.group(1));
          parentMap.put(identifierLine, m.group(1));
        }
      }
    }
    nameLine = d.toString();
    idMap.put(identifierLine,createOntologyTerm().getIdentifier());
  }
  
  /*
   * Store the parent-child relationships.
   * @see org.intermine.bio.dataconversion.OntologyTermsFileConverter#finalProcessing()
   */
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
      //termMap.get(idMap.get(child)).addToCollection("relations", relation.getIdentifier());
      //termMap.get(idMap.get(parentMap.get(child))).addToCollection("relations", relation.getIdentifier());
      termMap.get(child).addToCollection("relations", relation.getIdentifier());
      termMap.get(parentMap.get(child)).addToCollection("relations", relation.getIdentifier());
      relations.add(relation);
      // walk the parentage and add indirect parents
      // this is probably acyclic. But verify
      Set<String> beenThereDoneThat = new HashSet<String>();
      String ancestor = parentMap.get(child);
      while (!beenThereDoneThat.contains(ancestor)) {
        beenThereDoneThat.add(ancestor);
        ancestor = parentMap.get(ancestor);
        if (ancestor == null || idMap.get(ancestor)==null) break;
        Item indirectRelation = createItem("OntologyRelation");
        indirectRelation.setReference("parentTerm", idMap.get(ancestor));
        indirectRelation.setReference("childTerm", idMap.get(child));
        indirectRelation.setAttribute("relationship", "part_of");
        indirectRelation.setAttribute("direct", "false");
        indirectRelation.setAttribute("redundant", "false");
        termMap.get(child).addToCollection("relations", indirectRelation.getIdentifier());
        termMap.get(ancestor).addToCollection("relations", indirectRelation.getIdentifier());
        relations.add(indirectRelation);
      }
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
