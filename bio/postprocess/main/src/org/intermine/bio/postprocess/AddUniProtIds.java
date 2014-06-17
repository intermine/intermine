/**
 * 
 */
package org.intermine.bio.postprocess;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.BioQueries;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.Synonym;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
/**
 * @author jcarlson
 *
 */
public class AddUniProtIds {
  private ObjectStoreWriter osw = null;
  private ObjectStore os;
  private DataSet dataSet;
  private DataSource dataSource;
  private HashMap<Integer,Integer> orgIdToTaxonId = new HashMap<Integer,Integer>();
  private HashMap<Integer,Integer> taxonIdToOrgId = new HashMap<Integer,Integer>();
  // the hash of what we read from the uniprot file. First key is taxonId, Second is md5sum
  private HashMap<Integer,HashMap<String,ArrayList<UniProtInfo>>> seqToUniProtId =
      new HashMap<Integer,HashMap<String,ArrayList<UniProtInfo>>>();
  
  private static final Logger LOG = Logger.getLogger(AddUniProtIds.class);

  public AddUniProtIds(ObjectStoreWriter osw) {
    this.osw = osw;
    this.os = osw.getObjectStore();
    dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
    dataSource.setName("UniProtKB");
    try {
      dataSource = (DataSource) os.getObjectByExample(dataSource,
          Collections.singleton("name"));
    } catch (ObjectStoreException e) {
      throw new RuntimeException(
          "unable to fetch PhytoMine DataSource object", e);
    }
    populateOrgMap();
    populateUniProtDB("/projectb/sandbox/plant/phytomine/uniprot/uniprot_trembl_plants.dat");
  }
  private void populateUniProtDB(String theFile) {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(theFile));
    } catch (FileNotFoundException e) {
      throw new BuildException("Problem: UniProt file not found.");
    }
    String line;
    int ctr = 0;
    int nRepeats = 0;
    boolean readingSequence = false;
    Pattern taxonIdPattern = Pattern.compile("OX\\s+NCBI_TaxID=(\\d+);");
    Pattern accessionPattern = Pattern.compile("AC\\s+(\\w+);.*");
    Pattern nonTerminalFivePrimePattern = Pattern.compile("FT\\s+NON_TER\\s+1\\s+1");
    Pattern nonTerminalPattern = Pattern.compile("FT\\s+NON_TER");
    Pattern geneNamePattern = Pattern.compile("GN\\s+(\\w.*)");
    UniProtInfo uPI = new UniProtInfo();
    try {
      while ( (line = in.readLine()) != null) {
        if (readingSequence) {
          // make this condition first! The peptide sequence may have
          // keys in the front. (but after spaces, I expect.)
          if (line.startsWith("//")) {
            processLastRecord(uPI);
            // null out the fields that indicate we are processing
            uPI = new UniProtInfo();
            readingSequence = false;
          } else {
            uPI.appendSeq(line);
          }
        } else if ( line.startsWith("OX")) {
          // this is the taxon id. Only process it if this is a known taxon
          Matcher m = taxonIdPattern.matcher(line);
          if (m.matches()) {
            Integer taxonId = new Integer(m.group(1));
            if (!taxonIdToOrgId.containsKey(taxonId)) {
              advanceToNextRecord(in);
              // be certain this all fields are un-set
              uPI = new UniProtInfo();
            } else {
              uPI.setTaxon(taxonId);
            }
          } 
        } else if ( line.startsWith("FT")) {
          // first, check for FT NON_TER 1 1. Then more general FT NON_TER (bigger int)
          if (nonTerminalFivePrimePattern.matcher(line).matches()) {
            uPI.setFivePrimePartial(true);
          } else if (nonTerminalPattern.matcher(line).matches()) {
            uPI.setThreePrimePartial(true);
          }
        } else if ( line.startsWith("AC")) {
          Matcher m = accessionPattern.matcher(line);
          if (m.matches()) {
            uPI.setAccession(m.group(1));
          }
        }  else if ( line.startsWith("GN")) {
          Matcher m = geneNamePattern.matcher(line);
          if (m.matches()) {
            uPI.addGeneNames(m.group(1));
          }
        } else if ( line.startsWith("SQ")) {
          readingSequence = true;
        }
      }
    } catch (NumberFormatException e) {
      try {
        in.close();
      } catch (IOException e1) {
        throw new BuildException("Problem when trying to close UniProt file.");
      }
      throw new BuildException("Problem when trying extract taxonId number.");
    } catch (IOException e) {
      throw new BuildException("Problem when trying to read UniProt file.");
    }
    try {
      in.close();
      LOG.info("Read "+ctr+" proteins from file with "+nRepeats+" repeats.");
    } catch (IOException e) {
      throw new BuildException("Problem when trying to close UniProt file.");
    }
    // final record? there should not be one.
    processLastRecord(uPI);

  }
  private void populateOrgMap() {
    Query q = new Query();
    q.setDistinct(true);
    QueryClass orgObj;
    try {
      orgObj = new QueryClass(Class.forName("org.intermine.model.bio.Organism"));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    q.addFrom(orgObj);
    q.addToSelect(orgObj);
    Results res = os.execute(q, 1000, true, true, true);
    Iterator<?> resIter = res.iterator();
    int ctr = 0;
    while (resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      Organism org = (Organism) rr.get(0);
      orgIdToTaxonId.put(org.getId(),org.getTaxonId());
      taxonIdToOrgId.put(org.getTaxonId(),org.getId());
      ctr++;
    }
    LOG.info("Populated org map with "+ctr+" values.");
  }
  
  public void execute() throws ObjectStoreException {
    Results results = BioQueries.findObjects(os,
        Protein.class, false, true, 1000);

    dataSet = (DataSet) DynamicUtil.createObject(Collections
        .singleton(DataSet.class));
    dataSet.setName("UniProtKB/TrEMBL");
    dataSet.setVersion("" + new Date()); // current time and date
    dataSet.setUrl("http://www.uniprot.org");
    dataSet.setDataSource(dataSource);

    Iterator<?> resIter = results.iterator();

    int count = 0;
    int added = 0;

    osw.beginTransaction();
    while (resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      Protein prot = (Protein) rr.get(0);
      Sequence seq = (Sequence) rr.get(1);
      added += lookUpAndStoreUniProtId(prot,seq);
      if ((count % 1000) == 0) {
        LOG.info("Added UniProt Ids for " + added + " of " + count + " proteins...");
      }
      count++;
    }
    osw.store(dataSet);
    LOG.info("Added UniProt Ids for " + added + " of " + count + " proteins.");
    osw.commitTransaction();
    
    
  }
  private int lookUpAndStoreUniProtId(Protein prot, Sequence seq) {

    LOG.debug("Protein "+prot.getPrimaryIdentifier()+" has sequence "+seq.getResidues());
    Integer taxonId = orgIdToTaxonId.get(prot.getOrganism().getId());
    String peps = seq.getResidues().toString().toUpperCase();
    String md5 = calcMD5(peps);
    if (seqToUniProtId.containsKey(taxonId) &&
        seqToUniProtId.get(taxonId).containsKey(md5)) {
      
      ArrayList<UniProtInfo> allAcc = seqToUniProtId.get(taxonId).get(md5);
      if (allAcc.size() > 1) {
        LOG.warn("MD5 for peptide for "+prot.getPrimaryIdentifier()+" has multiple uniprot accs.");
      }
      for( UniProtInfo uPI: allAcc) {
        if (uPI.getSequence().equals(peps)) {
          if ( prot.getPrimaryIdentifier().equalsIgnoreCase(uPI.getAccession())) {
            LOG.info("Adding "+uPI.getAccession()+" to "+prot.getPrimaryIdentifier()+" based on accession.");
            prot.setPrimaryAccession(uPI.getAccession());
            try {
              osw.store(prot);
            } catch (ObjectStoreException e) {
              throw new BuildException("Problem when trying to store UniProt accession.");
            }
            return 1;
          }
          if ( uPI.matchesName(prot.getPrimaryIdentifier()) ) {
            LOG.info("Adding "+uPI.getAccession()+" to "+prot.getPrimaryIdentifier()+" based on protein name.");
            prot.setPrimaryAccession(uPI.getAccession());
            try {
              osw.store(prot);
            } catch (ObjectStoreException e) {
              throw new BuildException("Problem when trying to store UniProt accession.");
            }
            return 1;
          }
          // does the protein have synonyms?
          for( Synonym syn : prot.getSynonyms() ) {
            if ( uPI.matchesName(syn.getValue()) ) {
              LOG.info("Adding "+uPI.getAccession()+" to "+prot.getPrimaryIdentifier()+" based on protein synonym.");
              prot.setPrimaryAccession(uPI.getAccession());
              try {
                osw.store(prot);
              } catch (ObjectStoreException e) {
                throw new BuildException("Problem when trying to store UniProt accession.");
              }
              return 1;
            }
          }
          for( Gene gene : prot.getGenes() ) {
            if ( uPI.matchesName(gene.getPrimaryIdentifier()) ) {
              LOG.info("Adding "+uPI.getAccession()+" to "+prot.getPrimaryIdentifier()+" based on gene name.");
              prot.setPrimaryAccession(uPI.getAccession());
              try {
                osw.store(prot);
              } catch (ObjectStoreException e) {
                throw new BuildException("Problem when trying to store UniProt accession.");
              }
              return 1;
            }
            // and try gene synonyms
            for( Synonym syn : gene.getSynonyms() ) {
              if ( uPI.matchesName(syn.getValue()) ) {
                LOG.info("Adding "+uPI.getAccession()+" to "+prot.getPrimaryIdentifier()+" based on gene synonym.");
                prot.setPrimaryAccession(uPI.getAccession());
                try {
                  osw.store(prot);
                } catch (ObjectStoreException e) {
                  throw new BuildException("Problem when trying to store UniProt accession.");
                }
                return 1;
              }
            }
          }
        }
      }
    }
    
    return 0;
    
  }
  
  private void advanceToNextRecord(BufferedReader in) {
    String line;
    try {
      while ( (line = in.readLine()) != null) {
        if ( line.startsWith("//")) {
          return;
        }
      }
    } catch (IOException e) {
      throw new BuildException("Problem when trying to read UniProt file.");
    }
    return;
  }
  private void processLastRecord(UniProtInfo uPI) {
    // process if not null
    if (uPI == null || uPI.getAccession() == null || 
        uPI.getTaxon() == null || uPI.getSequence() == null) {
      return;
    } else {
      if (!uPI.getThreePrimePartial() && !uPI.getSequence().endsWith("*")) {
        uPI.appendSeq("*");
      }
      if (!seqToUniProtId.containsKey(uPI.getTaxon())) {
        seqToUniProtId.put(uPI.getTaxon(), new HashMap<String,ArrayList<UniProtInfo>>());
      }
      if (!seqToUniProtId.get(uPI.getTaxon()).containsKey(uPI.getMD5())) {
        seqToUniProtId.get(uPI.getTaxon()).put(uPI.getMD5(), new ArrayList<UniProtInfo>());
      }
      seqToUniProtId.get(uPI.getTaxon()).get(uPI.getMD5()).add(uPI);
    }
  }
  
  public static String calcMD5(String s) {
    byte[] b;
    MessageDigest md;
    try {
      b = s.getBytes("UTF-8");
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new BuildException("NoSuchAlgorithm Exception?");
    } catch (UnsupportedEncodingException e) {
      throw new BuildException("Unsupported Encoding Exception?");
    }
    return (new BigInteger(1,md.digest(b))).toString(16);
  }
  
public class UniProtInfo {
  String accession = null;;
  StringBuffer residues = null;
  String md5 = null;
  boolean fivePrimePartial;
  boolean threePrimePartial;
  Integer taxon;
  ArrayList<String> geneNames;
  
  UniProtInfo(String acc,String s,boolean f, boolean t, String name) {
    accession = new String(acc);
    residues = new StringBuffer(s);
    fivePrimePartial = false;
    threePrimePartial = false;
    geneNames = new ArrayList<String>();
    geneNames.add(new String(name));
    md5 = new String(getMD5());
  }
  UniProtInfo(String acc,String s) {
    accession = new String(acc);
    residues = new StringBuffer(s);
    md5 = new String(getMD5());
    geneNames = new ArrayList<String>();
    fivePrimePartial = false;
    threePrimePartial = false;
  }
  UniProtInfo(String acc) {
    accession = new String(acc);
    residues = new StringBuffer();
    geneNames = new ArrayList<String>();
    fivePrimePartial = false;
    threePrimePartial = false;
  }
  UniProtInfo() {
    fivePrimePartial = false;
    threePrimePartial = false;
    residues = new StringBuffer();
    geneNames = new ArrayList<String>();
  }
  String getMD5() {
    if (md5 == null) {
      setMD5();
    }
    return md5;
  }
  void setAccession(String a) { accession = new String(a);}
  void setTaxon(Integer i) { taxon = new Integer(i);}
  void setFivePrimePartial(boolean b) { fivePrimePartial = b;}
  void setThreePrimePartial(boolean b) { threePrimePartial = b;}
  String getAccession() { return accession; }
  Integer getTaxon() { return taxon; }
  boolean getFivePrimePartial() { return fivePrimePartial;}
  boolean getThreePrimePartial() { return threePrimePartial;}
  
  String getSequence() { return residues.toString().replaceAll(" ","").toUpperCase(); }
  
  private void setMD5() {
    md5 = AddUniProtIds.calcMD5(getSequence());
  }
  
  void setSequence(String s) {
    residues = new StringBuffer(s);
    setMD5();
  }
  void addGeneNames(String names) {
    // names is an unparsed string of the form tag=value;
    // we're going to toss on everything with a value
    for( String p : names.split(";") ) {
      String[] bits = p.trim().split("=");
      if (bits.length==2) {
        geneNames.add(bits[1].trim());
      }
    }
  }
  void appendSeq(String s) {
    residues.append(s);
    setMD5();
  }
  
  boolean matchesName(String match) {
    for(String name : geneNames) {
      if (name.equalsIgnoreCase(match)) return true;
    }
    return false;
  }
  
 
}

}
