package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.ontology.OboParser;
import org.intermine.bio.ontology.OboRelation;
import org.intermine.bio.ontology.OboTerm;
import org.intermine.bio.ontology.OboTermSynonym;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * Convert tree of OboTerms into Items.
 * Sets EMAPA attributes starts_at and ends_at
 * Requires the EMAPA.obo file to be modified
 * "relationship: starts_at TS01"
 * would need to become
 * "starts_at: TS01"
 * this allows the starts_at value to be parsed as tagValue
 * These two sed commands will do this
 * sed -i 's/relationship: ends_at/ends_at:/g' EMAPA.obo
 * sed -i 's/relationship: starts_at/starts_at:/g' EMAPA.obo
 * @author Steve Neuhauser
 * @see OboConverter
 */
public class EmapaOboConverter extends OboConverter implements OboConverterInterface {
private static final Logger LOG = Logger.getLogger(EmapaOboConverter.class);
// child identifier is key to a collection of parent items
private HashMap<String, ArrayList<Item>> parents = new HashMap<String, ArrayList<Item>>();
// child identifier is the key to a map of the child's ancestors each ancestor's key is its identifier
private HashMap<String, HashMap<String,Item>> ancestors = new HashMap<String, HashMap<String,Item>>();
// keep track of visited nodes when recursing
private HashMap<String, String> visited = new HashMap<String, String>();

public EmapaOboConverter(ItemWriter writer, Model model, String dagFilename, String dagName,
                        String url, String termClass) {

	super(writer,model,dagFilename,dagName,url,termClass);
}

    /**
     * Override base class method to calculate stage based ancestors
     * Ancestores need to be calculated after the terms are processed
     * but before the relations are processed so the whole method is overwritten
     */
    protected void storeItems() throws ObjectStoreException {
        long startTime = System.currentTimeMillis();
        store(ontology);
        for (OboTerm term : oboTerms) {
            process(term);
        }

        // caclculate direct parents for each term
        for (OboRelation oboRelation : oboRelations) {
           if (oboRelation.isDirect()) {
               Item p = nameToTerm.get(oboRelation.getParentTermId());
               String cId = nameToTerm.get(oboRelation.getChildTermId()).getAttribute("identifier").getValue();
               if (p != null) { 
	           if (parents.get(cId) != null) {
                      parents.get(cId).add(p);
                   } else {
                      ArrayList<Item> list = new ArrayList<Item>();
                      list.add(p);
                      parents.put(cId,list);
                  }
               }

            }
       }
       // calculate the ancesters for each node  
       for (Item node : nameToTerm.values()) {
              visit(node);       
        }

	
        // the rest of overwritten method storeItems
        for (OboRelation oboRelation : oboRelations) {
            processRelation(oboRelation);
        }
        for (Item termItem: nameToTerm.values()) {
            store(termItem);
        }
        for (Item synItem : synToItem.values()) {
            store(synItem);
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        LOG.info("Ran storeItems, took: " + timeTaken + " ms");
    }

    /**
     * Override base class method to set stage min and max attributes (startsAt, endsAt).
     *
     * @param termId the term id
     * @param item the Item created (see termClass field for type)
     * @param term the source DagTerm
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void configureItem(String termId, Item item, OboTerm term)
        throws ObjectStoreException {

	super.configureItem(termId, item, term);

        // this is the spcific EMAPA stuff 
        if (term.getId()!= null && term.getId().indexOf("EMAPA") != -1) {
           try {
             String starts_at =(String) ((List)term.getTagValues().get("starts_at")).get(0);
             String ends_at =(String) ((List)term.getTagValues().get("ends_at")).get(0);
             item.addAttribute(new Attribute("startsAt", starts_at));
             item.addAttribute(new Attribute("endsAt", ends_at));
           } catch(Exception e){}
        }
    }

    /**
     * Override base class method to skip this relation if it is for a descendant whose stage range
     * does NOT overlap the ancestor's. This is possible in the EMAPA.
     *
     * @param oboRelation a relation record
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void processRelation(OboRelation oboRelation)
    throws ObjectStoreException {
	Item p = nameToTerm.get(oboRelation.getParentTermId());
	Item c = nameToTerm.get(oboRelation.getChildTermId());
        if (p != null && c != null) {
            String cId = c.getAttribute("identifier").getValue();
            String pId = p.getAttribute("identifier").getValue();
	    if (ancestors.containsKey(cId)) {
             if (ancestors.get(cId).containsKey(pId)) {
		super.processRelation(oboRelation);
               }               
            }
          }
    }

    // create a collection of ancestors for each node/term
    // the node's ancestors will have a node specific rage of valid stages
    protected void visit(Item node) {
       String nId = node.getAttribute("identifier").getValue();
       LOG.info("processing node "+nId);
       if (visited.containsKey(nId)) {
           return;
       }
       visited.put(nId,nId);

       addToAncestors(nId,node);

       if (parents.containsKey(nId)) {
           for (Item p : parents.get(nId)) {
               visit(p);
               String pId = p.getAttribute("identifier").getValue();
               for (Item a : ancestors.get(pId).values()) {

                   String aId = a.getAttribute("identifier").getValue();

                   int nStart = Integer.parseInt(node.getAttribute("startsAt").getValue());
                   int nEnd = Integer.parseInt(node.getAttribute("endsAt").getValue()); 
       
                   int aStart = Integer.parseInt(a.getAttribute("startsAt").getValue());
                   int aEnd = Integer.parseInt(a.getAttribute("endsAt").getValue());
        
                   int vStart = Math.max(nStart, aStart);
                   int vEnd = Math.min(nEnd, aEnd);
                  
                   if (vStart<=vEnd) {
                       Item newAncestor = createItem(termClass);
                       newAncestor.setAttribute("identifier",aId);
	               newAncestor.setAttribute("startsAt",vStart+"");
	               newAncestor.setAttribute("endsAt",vEnd+"");      
       
                       addToAncestors(nId,newAncestor);
                       LOG.info("adding "+aId+" as ancestor of "+nId);
                   }
    
              }

           }
      }
    }
  
    // adds an ancestor the the child's map of ancestors
    // creates a new map if one doesn't exist 
    private void addToAncestors(String child, Item ancestor) {

         String aId = ancestor.getAttribute("identifier").getValue();
         if (ancestors.containsKey(child)) {
            if (ancestors.get(child).get(aId) != null) {
                Item existingA = ancestors.get(child).get(aId);
                ancestors.get(child).put(aId, merge(ancestor,existingA));
            }else {
                ancestors.get(child).put(aId,ancestor);
            }
        } else {
            HashMap<String,Item> map = new HashMap<String,Item>();
            map.put(aId,ancestor);
            ancestors.put(child, map);
        }
    }

   // an item is merged if it is being added to a set of ancestors more than once
   // the start and end stages should be the most inclusive range of stages
   private Item merge(Item a, Item b) {
	
      int aStart = Integer.parseInt(a.getAttribute("startsAt").getValue());
      int aEnd = Integer.parseInt(a.getAttribute("endsAt").getValue()); 
       
      int bStart = Integer.parseInt(b.getAttribute("startsAt").getValue());
      int bEnd = Integer.parseInt(b.getAttribute("endsAt").getValue());
        
      int vStart = Math.min(aStart, bStart);
      int vEnd = Math.max(aEnd, bEnd);

      a.setAttribute("startsAt",vStart+"");
      a.setAttribute("endsAt",vEnd+"");
      return a;
   }
}
