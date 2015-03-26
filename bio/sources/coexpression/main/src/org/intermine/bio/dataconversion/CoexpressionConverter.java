package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;


/**
 * 
 * @author
 */
public class CoexpressionConverter extends BioFileConverter
{
  //                                                                     
  private static final String DATASET_TITLE = "RNA-seq Expression Data";              
  private static final String DATA_SOURCE_NAME = "RNA-seq";                           

  private static final Logger LOG =
      Logger.getLogger(CoexpressionConverter.class);
  // for now, this can only process files of 1 organism
  private Integer proteomeId = null;
  private Item organism;
  // bioentities we record data about
  private HashMap<String,Item> geneMap = new HashMap<String, Item>();
  private HashMap<String,ReferenceList> collections = new HashMap<String,ReferenceList>();
  private HashMap<String,Integer> geneIdMap = new HashMap<String,Integer>();
  private Double bucketSize = .05;
  private Integer bucketNumber = 3;
  private HashMap<String,ArrayList<Pair>> jsonBuckets = new HashMap<String,ArrayList<Pair>>();
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CoexpressionConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        throw new BuildException("We should be using direct data loader");
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void process(Reader reader) throws Exception {
      File theFile = getCurrentFile();   
      if (!theFile.getName().equals("coexpression.tsv")) {
        return;
      }
      LOG.info("Processing file "+theFile.getName()+"...");
      
      

      if (organism==null) {
        // we need to register the organism
        if (proteomeId != null ) {
          organism = createItem("Organism");
          organism.setAttribute("proteomeId", proteomeId.toString());
          try {
            store(organism);
          } catch (ObjectStoreException e) {
            throw new RuntimeException("failed to store organism with proteomeId: "
                + proteomeId, e);
          }
        } else {
          throw new BuildException("No proteomeId specified.");
        }
      }

      Iterator<?> tsvIter;                             
      try {                                            
        tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
      } catch (Exception e) {                                           
        throw new BuildException("cannot parse file: " + getCurrentFile(), e);
      }

      int lineNumber = 0;                                        

      while (tsvIter.hasNext()) {
        String[] fields = (String[]) tsvIter.next();
        lineNumber++;
        if ( (lineNumber%1000000) == 0 ) {
          LOG.info("Processed "+lineNumber+" lines...");
        }
        if (fields.length==3 && !fields[0].equals(fields[1]) && 
            (Double.parseDouble(fields[2]) >= 1. - bucketSize*bucketNumber) ) {
          if (!geneMap.containsKey(fields[0]) ) {
            Item i = createItem("Gene");
            i.setAttribute("primaryIdentifier", fields[0]);
            i.setReference("organism", organism);
            geneIdMap.put(fields[0],store(i));
            geneMap.put(fields[0], i);
            jsonBuckets.put(i.getIdentifier(), new ArrayList<Pair>());
          }
          if (!geneMap.containsKey(fields[1]) ) {
            Item i = createItem("Gene");
            i.setAttribute("primaryIdentifier", fields[1]);
            i.setReference("organism", organism);
            geneIdMap.put(fields[1],store(i));
            geneMap.put(fields[1], i);
            jsonBuckets.put(i.getIdentifier(), new ArrayList<Pair>());
          }

          jsonBuckets.get(geneMap.get(fields[1]).getIdentifier()).add(new Pair(fields[0], Double.parseDouble(fields[2])));
          jsonBuckets.get(geneMap.get(fields[0]).getIdentifier()).add(new Pair(fields[1], Double.parseDouble(fields[2])));

          Item i = createItem("Coexpression");
          i.setReference("gene", geneMap.get(fields[0]));
          i.setReference("coexpressedGene",geneMap.get(fields[1]));
          i.setAttribute("correlation", fields[2]);
          store(i);

          Reference r1 = new Reference("coexpressions",i.getIdentifier());
          if (!collections.containsKey(fields[0]) ) collections.put(fields[0], new ReferenceList("coexpressions"));
          collections.get(fields[0]).addRefId(r1.getRefId());

          Item j = createItem("Coexpression");
          j.setReference("gene", geneMap.get(fields[1]));
          j.setReference("coexpressedGene",geneMap.get(fields[0]));
          j.setAttribute("correlation", fields[2]);
          store(j);

          Reference r2 = new Reference("coexpressions",j.getIdentifier());
          if (!collections.containsKey(fields[1]) ) collections.put(fields[1], new ReferenceList("coexpressions"));
          collections.get(fields[1]).addRefId(r2.getRefId());
        }
      }
      LOG.info("Processed "+lineNumber+" lines. Now storing collections...");
      
      // now store the collections
      for(String key : collections.keySet()) {
        store(collections.get(key),geneIdMap.get(key));
      }

      // and store the JSON
      for(String geneId: jsonBuckets.keySet()) {
        ArrayList<Pair> coexpressionList = jsonBuckets.get(geneId);
        StringBuffer thisJSON = null;
        Collections.sort(coexpressionList);
        Double thisBucket = 10.; // anything bigger than 1.
        for(Pair p : coexpressionList) {

          while (p.getValue() < thisBucket) {
            if (thisJSON != null && !thisJSON.toString().isEmpty()) {
              Item i = createItem("CoexpressionJSON");
              i.setReference("gene",geneId);
              i.setAttribute("lowRange", thisBucket.toString());
              i.setAttribute("highRange", Double.toString(thisBucket + bucketSize));
              i.setAttribute("JSON","{" + thisJSON.toString() +"}");
              store(i);
            }
            thisJSON = new StringBuffer();
            thisBucket -= bucketSize;
          }

          if (!thisJSON.toString().isEmpty() ) thisJSON.append(",");
          thisJSON.append("\""+p.getKey()+"\":\""+p.getValue()+"\"");
        }
        if (thisJSON != null && !thisJSON.toString().isEmpty()) {
          Item i = createItem("CoexpressionJSON");
          i.setReference("gene",geneId);
          i.setAttribute("lowRange", thisBucket.toString());
          i.setAttribute("highRange", Double.toString(thisBucket + bucketSize));
          i.setAttribute("JSON","{" + thisJSON.toString() +"}");
          store(i);
        }

      }
    }

public class Pair implements Comparable {
  private String left;
  private Double right;
  public Pair(String left,Double right) {
    this.left = left;
    this.right = right;
  }
  public String getKey() { return left; }
  public Double getValue() { return right; }
  public int compareTo(Object b) {
    return -right.compareTo(((Pair)b).getValue());
  }
}
  
    public void setProteomeId(String organism) {
      try {
        proteomeId = Integer.valueOf(organism);
      } catch (NumberFormatException e) {
        throw new BuildException("Cannot find integer proteome id for: " + organism);
      }
    }
    
    public void setBucketSize(String size) {
      try {
        bucketSize = Double.valueOf(size);
      } catch (NumberFormatException e) {
        throw new BuildException("Cannot find double-valued bucket size for: "+size);
      }
    }

    public void setBucketNumber(String number) {
      try {
        bucketNumber = Integer.valueOf(number);
      } catch (NumberFormatException e) {
        throw new BuildException("Cannot find integer-valued bucket number for: "+number);
      }
    }
  }