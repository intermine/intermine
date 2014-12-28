package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class InparanoidHomologsConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Phytozome Homologs";
    private static final String DATA_SOURCE_NAME = "Phytozome v10";
    private static final Logger LOG =
        Logger.getLogger(InparanoidHomologsConverter.class);
    private HashMap<Integer,String> organismMap = new HashMap<Integer,String>();
    private HashMap<String,String> geneMap = new HashMap<String,String>();
    Pattern filePattern;
    int orthoRegistered;
    int paraRegistered;
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public InparanoidHomologsConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        filePattern = Pattern.compile("table\\.(\\d+)\\.fa-(\\d+)\\.fa");
        orthoRegistered = 0;
        paraRegistered = 0;
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
      File theFile = getCurrentFile();

      Matcher fileMatcher = filePattern.matcher(theFile.getName());
      if( !fileMatcher.matches()) {
        LOG.info("Ignoring file " + theFile.getName() + ". Does not match pattern.");
      } else {
        LOG.info("Processing file "+theFile.getName());
        Integer[] proteomeId = new Integer[2];
        try {
          proteomeId[0] = Integer.parseInt(fileMatcher.group(1));
          proteomeId[1] = Integer.parseInt(fileMatcher.group(2));
        } catch (NumberFormatException e) {
          throw new BuildException("Cannot parse proteome ids from string "+theFile.getName());
        }

        // register both as needed.
        for ( Integer proteome: proteomeId ) {
          if (!organismMap.containsKey(proteome)) {
            Item o = createItem("Organism");
            o.setAttribute("proteomeId", proteome.toString());
            try {
              store(o);
            } catch (ObjectStoreException e) {
              throw new BuildException("Trouble storing organism: "+e.getMessage());
            }
            organismMap.put(proteome, o.getIdentifier());
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
          String id = fields[0];
          String genes1 = fields[2];
          String genes2 = fields[3];
          
          orthoRegistered += registerPairs(id,genes1,proteomeId[0],genes2,proteomeId[1]);
          orthoRegistered += registerPairs(id,genes2,proteomeId[1],genes1,proteomeId[0]);
          paraRegistered += registerPairs(id,genes1,proteomeId[0],genes1,proteomeId[0]);
          paraRegistered += registerPairs(id,genes2,proteomeId[1],genes2,proteomeId[1]);
          lineNumber++;

          if ( (lineNumber%5000)==0 ) {
            LOG.info("Processed "+lineNumber+" lines and registered "+orthoRegistered+" orthologs and "+paraRegistered+" paralogs...");
          }
        }
      }
      LOG.info("Registered "+orthoRegistered+" orthologs and "+paraRegistered+" paralogs.");
    }
      private int registerPairs(String id,String g1,Integer p1,String g2,Integer p2) {
        return registerPairs(id,g1,p1.toString(),g2,p2.toString());
      }
      private int registerPairs(String id,String g1,String p1,String g2,String p2) {
        String[] fields1 = g1.split(" ");
        String[] fields2 = g2.split(" ");
        int registered = 0;

        for( int i1=0; i1 < fields1.length; i1+=2) {
          for( int i2=0; i2 < fields2.length; i2+=2 ) {
            // register all pairs with genes1 and genes2
            // everything should be an integer. Skip this record if not
            try {
              Integer.parseInt(id);
              Integer.parseInt(fields1[i1]);
              Integer.parseInt(fields2[i2]);

              if (p1.equals(p2) && i1==i2) {
                continue;
              } else {
                Item o = createItem("Homolog");
                o.setReference("organism1",organismMap.get(Integer.parseInt(p1)));
                o.setReference("organism2", organismMap.get(Integer.parseInt(p2)));
                String gene1 = "PAC:"+fields1[i1];
                String gene2 = "PAC:"+fields2[i2];
                o.setReference("gene1",getGene(gene1,p1));
                o.setReference("gene2",getGene(gene2,p2));
                o.setAttribute("groupName",p1.toString()+"_"+p2.toString()+"_"+id);
                o.setAttribute("method", "inParanoid");
                try {
                  store(o);
                } catch (ObjectStoreException e) {
                  throw new BuildException("There was a problem storing homolog: "+e.getMessage());
                }
                registered++;
              }
            } catch (NumberFormatException e) {}
          }
        }
        return registered;
    }
      
      private String getGene(String geneName, String proteomeId) {
        if (!geneMap.containsKey(geneName)) {
          Item g = createItem("Gene");
          g.setAttribute("secondaryIdentifier",geneName);
          g.setReference("organism", organismMap.get(Integer.parseInt(proteomeId)));
          try {
            store(g);
          } catch (ObjectStoreException e) {
            throw new BuildException("There was a problem storing gene: "+e.getMessage());
          }
          geneMap.put(geneName,g.getIdentifier());
        }
        return geneMap.get(geneName);
      }
    }