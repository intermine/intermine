/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author jcarlson
 *
 */
public class OntologyTermsXMLConverter extends OntologyTermsFileConverter {

  
  /**
   * @param writer
   * @param model
   */
  public OntologyTermsXMLConverter(ItemWriter writer, Model model)  throws SAXException {
    super(writer, model);
 
  }

  /**
   * {@inheritDoc}
   */
  public void process(Reader reader) throws Exception {
    
    if( (srcDataFile != null) && (!getCurrentFile().getName().equals(srcDataFile)) ) {
      //LOG.info("Ignoring file " + theFile.getName() + ". File name is set and this is not it.");
    } else {
      // register and store the ontology.
      if (ontology == null) {
        ontology = createItem("Ontology");
        ontology.addAttribute(new Attribute("name",dataSetTitle));
        setIfNotNull(ontology,"url",dataSetURL);
        store(ontology);
      }
      InterProHandler handler = new InterProHandler(getItemWriter());
      try {
        SAXParser.parse(new InputSource(reader), handler);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

private class InterProHandler extends DefaultHandler
{
  private Item ontologyTerm = null;
  private String identifier = null;
  private StringBuffer description = null;
  private Stack<String> stack = new Stack<String>();
  private String attName = null;
  private StringBuffer attValue = null;
  private Map<String,String> parentMap = new HashMap<String,String>();
  private Map<String,Item> itemMap = new HashMap<String,Item>();

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param mapMaster the Map of maps
   */
  public InterProHandler(ItemWriter writer) {
      // Nothing to do
  }

  /**
   * {@inheritDoc}
   */
  public void startElement(String uri, String localName, String qName, Attributes attrs)
      throws SAXException {

    // descriptions span multiple lines
    // so don't reset temp var when processing descriptions
    if (attName != null && !attName.equals("description")) {
      attName = null;
    }

    // <interpro id="IPR000002" type="Domain" short_name="Fizzy" protein_count="256">
    if (qName.equals("interpro")) {
      identifier = attrs.getValue("id");
      ontologyTerm = createItem("OntologyTerm");
      ontologyTerm.setAttribute("identifier",identifier);
      ontologyTerm.setReference("ontology", ontology.getIdentifier());
      itemMap.put(identifier,ontologyTerm);
      // <interpro><name>
    } else if (qName.equals("name") && stack.peek().equals("interpro")) {
      attName = "name";
      //nameLine = attrs.getValue("name");
      //ontologyTerm.setAttribute("name",nameLine);
      // <interpro><abstract>
    } else if (qName.equals("abstract") && stack.peek().equals("interpro")) {
      attName = "description";
      description = new StringBuffer();
      //descriptionLine =attrs.getValue("abstract");
      //ontologyTerm.setAttribute("description",descriptionLine); 
      } else if (qName.equals("rel_ref") && stack.peek().equals("parent_list")) {
      parentMap.put(ontologyTerm.getAttribute("identifier").getValue(), attrs.getValue("ipr_ref"));
      }
    super.startElement(uri, localName, qName, attrs);
    stack.push(qName);
    attValue = new StringBuffer();
  }

  /**
   * {@inheritDoc}
   */
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
      super.endElement(uri, localName, qName);

      stack.pop();
      // finished processing file, store all domains
      if (qName.equals("interprodb")) {
        finalProcessing();
      // <interpro><name>
      } else if (qName.equals("name") && stack.peek().equals("interpro")
                      && attName != null) {
          String name = attValue.toString();
          ontologyTerm.setAttribute("name", name);
      // <interpro><abstract>
      } else if (qName.equals("abstract") && stack.peek().equals("interpro")) {
          ontologyTerm.setAttribute("description", description.toString());
          attName = null;
      }
  }

  void finalProcessing() throws SAXException {

    Set<Item> relations = new HashSet<Item>();
    for( String child : parentMap.keySet()) {
      if (itemMap.get(parentMap.get(child))==null) {
        // there may be some parent_list that refer to obsolete(?) terms. Skip
        //LOG.info("No id for parent of child " + child +":"+ parentMap.get(child));
        continue;
      }
      Item relation = createItem("OntologyRelation");
      relation.setReference("parentTerm", itemMap.get(parentMap.get(child)).getIdentifier());
      relation.setReference("childTerm", itemMap.get(child).getIdentifier());
      relation.setAttribute("relationship", "part_of");
      relation.setAttribute("direct", "true");
      relation.setAttribute("redundant", "false");
      // Set the reverse reference
      itemMap.get(itemMap.get(child).getAttribute("identifier").getValue()).addToCollection("relations", relation.getIdentifier());
      itemMap.get(itemMap.get(parentMap.get(child)).getAttribute("identifier").getValue()).addToCollection("relations", relation.getIdentifier());
      relations.add(relation);
      // walk the parentage and add indirect parents
      // this is probably acyclic. But verify
      Set<String> beenThereDoneThat = new HashSet<String>();
      String ancestor = parentMap.get(child);
      while (!beenThereDoneThat.contains(ancestor)) {
        beenThereDoneThat.add(ancestor);
        ancestor = parentMap.get(ancestor);
        if (ancestor == null || itemMap.get(ancestor)==null) break;
        Item indirectRelation = createItem("OntologyRelation");
        indirectRelation.setReference("parentTerm", itemMap.get(ancestor).getIdentifier());
        indirectRelation.setReference("childTerm", itemMap.get(child).getIdentifier());
        indirectRelation.setAttribute("relationship", "part_of");
        indirectRelation.setAttribute("direct", "false");
        indirectRelation.setAttribute("redundant", "false");
        itemMap.get(child).addToCollection("relations", indirectRelation.getIdentifier());
        itemMap.get(ancestor).addToCollection("relations", indirectRelation.getIdentifier());
        relations.add(indirectRelation);
      }
    }


    // now store the items
    for (String itemName : itemMap.keySet()) {
      try {
        store(itemMap.get(itemName));
      } catch (ObjectStoreException e) {
        throw new SAXException(e);
      }
    }
    for(Item relation: relations) {
      try {
        store(relation);
      } catch (ObjectStoreException e) {
        throw new SAXException(e);
      }
    }
  }
  /**
   * {@inheritDoc}
   */
  public void characters(char[] ch, int start, int length) {
      int st = start;
      int l = length;
      if (attName != null) {

          // DefaultHandler may call this method more than once for a single
          // attribute content -> hold text & create attribute in endElement
          while (l > 0) {
              boolean whitespace = false;
              switch(ch[st]) {
                  case ' ':
                  case '\r':
                  case '\n':
                  case '\t':
                      whitespace = true;
                      break;
                  default:
                      break;
              }
              if (!whitespace) {
                  break;
              }
              ++st;
              --l;
          }

          if (l > 0) {
              StringBuffer s = new StringBuffer();
              s.append(ch, st, l);
              attValue.append(s);
              if (attName.equals("description")) {
                  description.append(s);
              }
          }
      }
  }
}
}
