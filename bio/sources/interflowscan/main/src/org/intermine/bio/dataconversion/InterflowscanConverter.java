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
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * 
 * @author
 */
public class InterflowscanConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "InterFlowScan Results";
  private static final String DATA_SOURCE_NAME = "InterFlowScan";

  private Integer proteomeId = null;
  private String srcDataFile = null;
  private String srcDataDir = null;
  private String orgId = null;

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public InterflowscanConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
  }

  /**
   * {@inheritDoc}
   */
  public void process(Reader reader) throws Exception {
   
    if( (srcDataFile != null) && (!getCurrentFile().getName().equals(srcDataFile)) ) {
      //LOG.info("Ignoring file " + theFile.getName() + ". File name is set and this is not it.");
    } else {
      // register the organism
      if (proteomeId == null) {
        throw new BuildException("Proteome Id is not set.");
      }
      Item org = createItem("Organism");
      org.setAttribute("proteomeId",proteomeId.toString());
      store(org);
      orgId = org.getIdentifier();

      InterFlowScanHandler handler = new InterFlowScanHandler(getItemWriter());
      try {
        SAXParser.parse(new InputSource(reader), handler);
      } catch (Exception e) {
        e.printStackTrace();
        throw new BuildException(e);
      }
    }
  }
  public void setSrcDataFile(String file) {
    srcDataFile = file;
  }
  public void setSrcDataDir(String dir) {
    srcDataDir = dir;
  }
  public void setProteomeId(String proteomeString) {
    try {
      proteomeId = new Integer(proteomeString);
    } catch (NumberFormatException e) {
      throw new BuildException("proteome id "+proteomeString+" cannot be parsed as an integer.");
    }
  }
  
  private class InterFlowScanHandler extends DefaultHandler
  {
    private String proteinId = null;
    private String proteinName = null;
    private String programName = null;
    private String dbName = null;
    private Item crossReference = null;
    private Item proteinDomain = null;
    private String hitAcc = null;
    private String eValue = null;
    private String score = null;
    private ArrayList<String> goXref = new ArrayList<String>();
    // data sources we register
    private HashMap<String,String> sourceMap = new HashMap<String,String>();
    // ontologies we register
    private HashMap<String,String> ontologyMap = new HashMap<String,String>();
    // protein domains
    private HashMap<String,String> domainMap = new HashMap<String,String>();
    // go terms
    private HashMap<String,String> goTermMap = new HashMap<String,String>();
    // hits. Indexed by db, then by identifier
    private HashMap<String,HashMap<String,String>> xrefMap = new HashMap<String,HashMap<String,String>>();
    private HashMap<String,HashMap<String,String>> oTermMap = new HashMap<String,HashMap<String,String>>();
 
    // we should see proteins grouped together. But just in case...
    private HashMap<String,String> protMap = new HashMap<String,String>();
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param mapMaster the Map of maps
     */
    public InterFlowScanHandler(ItemWriter writer) {
      // Nothing to do except register the GO ontology
      Item ontology = createItem("Ontology");
      ontology.setAttribute("name","GO");
      ontologyMap.put("GO",tryToStore(ontology));
      oTermMap.put("GO",new HashMap<String,String>());
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {

      if (qName.equals("release")) {
      } else if (qName.equals("dbinfo")) {
        // we're counting on these getting processed first.
        String dbName = attrs.getValue("dbname");
        String version = attrs.getValue("version");
        registerDbName(dbName,version);
        oTermMap.put(dbName,new HashMap<String,String>());
        xrefMap.put(dbName,new HashMap<String,String>());
      } else if (qName.equals("protein")) {
        // start of a new <protein> null out things.
        proteinId = null;
        proteinName = null;
        programName = null;
        dbName = null;
        hitAcc = null;
        crossReference = null;
        proteinDomain = null;
        eValue = null;
        score = null;
        goXref = new ArrayList<String>();
      } else if (qName.equals("xref")) {
        // this is the protein name
        proteinName = attrs.getValue("id");
        if (!protMap.containsKey(proteinName)) {
          Item prot = createItem("Protein");
          prot.setAttribute("primaryIdentifier",proteinName);
          prot.setReference("organism",orgId);
          protMap.put(proteinName,tryToStore(prot));
        }
        proteinId = protMap.get(proteinName);
      } else if (qName.endsWith("-match") ) {
        programName = qName.replace("-match","");
      } else if (qName.equals("signature") ) {
        hitAcc = attrs.getValue("ac");
        dbName = attrs.getValue("dbname");
        
        // e-value and score may be at this level
        if (attrs.getValue("e-value") != null && attrs.getValue("e-value").trim().length() > 0) {
          eValue = attrs.getValue("e-value");
        }
        if (attrs.getValue("score") != null && attrs.getValue("score").trim().length() > 0) {
          score = attrs.getValue("score");
        }
        
        // special case handling: strip off .NN from PFAM
        if (dbName.equals("PFAM") ) hitAcc = hitAcc.replaceAll("\\.\\d+$","");
        
        // make the item for the ontology term and store it.
        if (!oTermMap.get(dbName).containsKey(hitAcc) ) {
          Item hit = createItem("OntologyTerm");
          hit.setAttribute("identifier",hitAcc);
          if (attrs.getValue("name") != null && attrs.getValue("name").trim().length() > 0) {
            hit.setAttribute("name",attrs.getValue("name").trim());
          }
          if (attrs.getValue("desc") != null && attrs.getValue("desc").trim().length() > 0) {
            hit.setAttribute("description",attrs.getValue("desc").trim());
          }
          hit.setReference("ontology", ontologyMap.get(dbName));
          oTermMap.get(dbName).put(hitAcc,tryToStore(hit));
        }
        // we're going to make the item for the cross reference,
        // link it to the ontology term, but do not store it just yet.
        if (!xrefMap.get(dbName).containsKey(hitAcc) ) {
          crossReference = createItem("CrossReference");
          crossReference.setAttribute("identifier",hitAcc);
          if (!sourceMap.containsKey(dbName) || sourceMap.get(dbName)==null ) {
            throw new BuildException("database "+dbName+" was not part of the dbinfo header in xml.");
          }
          crossReference.setReference("source", sourceMap.get(dbName));   
          Reference r = new Reference("ontologyTerm",oTermMap.get(dbName).get(hitAcc));
          ReferenceList refList = new ReferenceList("ontologyTerms");
          refList.addRefId(r.getRefId());
          crossReference.addCollection(refList);
        }
      } else if (qName.equals("entry") ) {
        // this is the subject for the crossreference. But only need to do this
        // if we're processing this crossref on this loop.
        if (crossReference != null) {
          if (!domainMap.containsKey(attrs.getValue("id") ) ) {
            // register this protein domain
            proteinDomain = createItem("ProteinDomain");
            proteinDomain.setAttribute("primaryIdentifier",attrs.getValue("id"));
            proteinDomain.setAttribute("name",attrs.getValue("name"));
            domainMap.put(attrs.getValue("id"),tryToStore(proteinDomain));
          }
          crossReference.setReference("subject",domainMap.get(attrs.getValue("id")));
        }
      } else if (qName.equals("go-xref") ) { 
        if (proteinDomain != null ) {
          if (!oTermMap.get("GO").containsKey(attrs.getValue("id")) ) {
            // register this GO term
            Item goTerm = createItem("GOTerm");
            goTerm.setAttribute("identifier",attrs.getValue("id"));
            goTerm.setReference("ontology",ontologyMap.get("GO"));
            if (attrs.getValue("category") != null ) goTerm.setAttribute("namespace",attrs.getValue("category"));
            if (attrs.getValue("name") != null ) goTerm.setAttribute("name",attrs.getValue("name"));
            if (attrs.getValue("desc") != null ) goTerm.setAttribute("description",attrs.getValue("desc"));
            oTermMap.get("GO").put(attrs.getValue("id"),tryToStore(goTerm));
          }
          Item goAnnot = createItem("GOAnnotation");
          goAnnot.setReference("subject",proteinDomain);
          goAnnot.setReference("ontologyTerm",oTermMap.get("GO").get(attrs.getValue("id")));
          String goAnnotRef = tryToStore(goAnnot);
        }
      } else if (qName.equals("location") ) {
        // this is the main event.
        // e-value and score may be at this level
        if (attrs.getValue("e-value") != null && attrs.getValue("e-value").trim().length() > 0) {
          eValue = attrs.getValue("e-value");
        }
        if (attrs.getValue("score") != null && attrs.getValue("score").trim().length() > 0) {
          score = attrs.getValue("score");
        }
        Item paf = createItem("ProteinAnalysisFeature");
        paf.setReference("protein",proteinId);
        paf.setReference("organism",orgId);
        paf.setReference("crossReference",xrefMap.get(dbName).get(hitAcc));
        paf.setAttribute("programname",programName);
        if (eValue != null) paf.setAttribute("significance",eValue);
        if (score != null) paf.setAttribute("normscore",score);
        String theStart = attrs.getValue("start");
        String theEnd = attrs.getValue("end");
        paf.setAttribute("primaryIdentifier",proteinName+":"+dbName+":"+hitAcc+":"+theStart+"-"+theEnd);
        paf.setAttribute("name",proteinName+":"+dbName+":"+hitAcc+":"+theStart+"-"+theEnd);
        String pafRef = tryToStore(paf);
        Item loc = createItem("Location");
        loc.setReference("locatedOn",proteinId);
        loc.setAttribute("start",theStart);
        loc.setAttribute("end",theEnd);
        loc.setReference("feature",pafRef);
        tryToStore(loc);
      }
      super.startElement(uri, localName, qName, attrs);
    }
  

    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      super.endElement(uri, localName, qName);

      if (qName.equals("protein-matches")) {
      } else if (qName.equals("release") ) {
      } else if (qName.endsWith("-match") ) {
      } else if (qName.equals("signature") ) {
        if (crossReference != null ) {
          xrefMap.get(dbName).put(hitAcc,tryToStore(crossReference));
          crossReference = null;
        }
      }
      
    }
    
    void registerDbName(String name, String version) {
      Item source = createItem("DataSource");
      source.setAttribute("name",name);
      if (version != null ) source.setAttribute("description",version);
      sourceMap.put(name,tryToStore(source));

      Item ontology = createItem("Ontology");
      ontology.setAttribute("name",name);
      ontologyMap.put(name,tryToStore(ontology));
          
    }
    
    String tryToStore(Item i) {
      // a silly convenience to store and return the identifier string.
      try {
        store(i);
      } catch (ObjectStoreException e) {
        throw new BuildException("Cannot store item: "+e.getMessage());
      }
      return i.getIdentifier();
    }
  }
    
  
  
}
