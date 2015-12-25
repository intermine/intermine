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
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.InputSource;


/**
 * 
 * @author
 */
public class E2p2EnzymesConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "Phytozome-E2P2";
  private static final String DATA_SOURCE_NAME = "Phytozome";
  private Pattern fileMatchPattern;
  private HashMap<String,String> xrefMap;
  private HashMap<String,String> ontMap;
  private String orgId;
  private String ontId;
  private String sourceId;

  protected static final Logger LOG =
      Logger.getLogger(E2p2EnzymesConverter.class);

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public E2p2EnzymesConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    fileMatchPattern = Pattern.compile(".*_(\\d+).pf");
    xrefMap = new HashMap<String,String>();
    ontMap = new HashMap<String,String>();
    orgId = null;
    ontId = null;
    sourceId = null;
  }

  /**
   * 
   *
   * {@inheritDoc}
   */
  public void process(Reader reader) throws Exception {
    String fileName = getCurrentFile().getName();
    Matcher m = fileMatchPattern.matcher(fileName);
    if (m.matches()) {
      String proteomeId = m.group(1);
      LOG.info("Processing file "+fileName+" with proteomeId="+proteomeId);
      Iterator<?> tsvIter;                             
      try {                                            
        tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
      } catch (Exception e) {                                           
        throw new BuildException("cannot parse file: " + getCurrentFile(), e);
      }

      Item o = createItem("Organism");
      o.setAttribute("proteomeId", proteomeId);
      try {
        store(o); 
        orgId = o.getIdentifier();
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing organism: "+e.getMessage());
      }

      // store the ontology and datasource if not done already
      if (ontId == null) {
        Item ont = createItem("Ontology");
        ont.setAttribute("name","ENZYME");
        ont.setAttribute("url","http://enzyme.expasy.org");
        Item source = createItem("DataSource");
        source.setAttribute("name","ENZYME");
        try {
          store(ont);
          store(source);           
        } catch (ObjectStoreException e) {
          throw new BuildException("Trouble storing organism: "+e.getMessage());
        }
        ontId = ont.getIdentifier();
        sourceId = source.getIdentifier();
      }

      int lineNumber = 0;

      // format: 
      // TAG value 
      // Look for tags ID and EC 


      String proteinName = null;
      String proteinId = null;
      while (tsvIter.hasNext()) {
        String[] fields = (String[]) tsvIter.next();
        if (fields.length == 1 && fields[0].equals("//") ) {
          // end of the line for that protein
          proteinName = null;
          proteinId = null;
        } else if (fields.length == 2) {
          if (fields[0].equals("ID") ) {
            proteinName = fields[1];
          } else if (fields[0].equals("EC")) {
            String ec = fields[1];
            proteinId = storePAF(proteinName,ec,proteinId);
          }
        }
        lineNumber++;
      }

      LOG.info("Processed "+lineNumber+" lines");
    }
  } 

  String storePAF(String protName,String ec,String proteinId) {
    // see if we need to register this protein
    if (proteinId == null) {
      // we need to register this protein
      Item prot = createItem("Protein");
      prot.setAttribute("primaryIdentifier",protName);
      prot.setReference("organism",orgId);
      try {
        store(prot);          
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing protein: "+e.getMessage());
      }
      proteinId = prot.getIdentifier();
    }
    if (!ontMap.containsKey(ec) ) {
      Item term = createItem("OntologyTerm");
      term.setAttribute("identifier",ec);
      term.setReference("ontology",ontId);
      Item xref = createItem("CrossReference");
      xref.setAttribute("identifier",ec);
      xref.setReference("source", sourceId);   
      Reference r = new Reference("ontologyTerm",term.getIdentifier());
      ReferenceList refList = new ReferenceList("ontologyTerms");
      refList.addRefId(r.getRefId());
      xref.addCollection(refList);
      try {
        store(term);
        store(xref);
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing ontologyterm/crossreference: "+e.getMessage());
      }
      ontMap.put(ec,term.getIdentifier());
      xrefMap.put(ec,xref.getIdentifier());
    }
    Item paf = createItem("ProteinAnalysisFeature");
    paf.setReference("protein",proteinId);
    paf.setReference("organism",orgId);
    paf.setReference("crossReference",xrefMap.get(ec));
    paf.setAttribute("programname","e2p2");
    paf.setAttribute("primaryIdentifier",protName+":ENZYME:"+ec);
    paf.setAttribute("name",protName+":ENZYME:"+ec);
    try {
      store(paf);    
    } catch (ObjectStoreException e) {
      throw new BuildException("Trouble storing protein analysis feature: "+e.getMessage());
    }
    return proteinId;
  }
}
