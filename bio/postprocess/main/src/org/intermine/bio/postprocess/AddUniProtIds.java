/**
 * 
 */
package org.intermine.bio.postprocess;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
  private HashMap<Integer,HashMap<String,ArrayList<String>>> seqToUniProtId =
      new HashMap<Integer,HashMap<String,ArrayList<String>>>();
  
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
    String acc = null;
    Integer taxonId = null;
    StringBuffer seq = new StringBuffer();
    boolean readingSequence = false;
    boolean nonTerminal = false;
    Pattern taxonIdPattern = Pattern.compile("OX\\s+NCBI_TaxID=(\\d+);");
    Pattern accessionPattern = Pattern.compile("AC\\s+(\\w+);.*");
    Pattern nonTerminalPattern = Pattern.compile(".*\\s+NON_TER*");
    try {
      while ( (line = in.readLine()) != null) {
        if (readingSequence) {
          if (line.startsWith("//")) {
            readingSequence = false;
            if (!nonTerminal) {
              seq.append("*");
            }
            String theSeq = seq.toString().replaceAll(" ", "");
            if (acc != null && taxonId != null && seq.length()>0 ) {
              if (!seqToUniProtId.containsKey(taxonId)) {
                seqToUniProtId.put(taxonId, new HashMap<String,ArrayList<String>>());
              }
              if (seqToUniProtId.get(taxonId).containsKey(theSeq)) {
                LOG.warn("There is a repeat for "+acc+" in organism " + taxonId +
                         " with " +
                 seqToUniProtId.get(taxonId).get(theSeq));
                nRepeats++;
              } else {
                seqToUniProtId.get(taxonId).put(theSeq, new ArrayList<String>());
              }
              seqToUniProtId.get(taxonId).get(theSeq).add(acc);
              seq = new StringBuffer();
              acc = null;
              taxonId = null;
              nonTerminal = false;
              ctr++;
              if (ctr%10000 == 0) {
                LOG.info("Read "+ctr+" proteins from file...");
              }
            }
          } else {
            seq.append(line);
          }
        } else if ( line.startsWith("SQ")) {
          readingSequence = true;
        } else if ( line.startsWith("FT")) {
          Matcher m = nonTerminalPattern.matcher(line);
          if (m.matches()) {
            nonTerminal = true;
          }
        } else if ( line.startsWith("AC")) {
          Matcher m = accessionPattern.matcher(line);
          if (m.matches()) {
            //m.reset();
            acc = m.group(1);
          }
        } else if ( line.startsWith("OX")) {
          Matcher m = taxonIdPattern.matcher(line);
          if (m.matches()) {
            //m.reset();
            taxonId = new Integer(m.group(1));
          }
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
    String peps = seq.getResidues().toString().replaceAll("\\*","");
    if (seqToUniProtId.containsKey(taxonId) &&
        seqToUniProtId.get(taxonId).containsKey(peps)) {
      ArrayList<String> allAcc = seqToUniProtId.get(taxonId).get(peps);
      if (allAcc.size() > 1) {
        LOG.warn("Peptide for "+prot.getPrimaryIdentifier()+" has multiple uniprot accs.");
      } else {
        prot.setPrimaryAccession(allAcc.get(0));
        try {
          osw.store(prot);
        } catch (ObjectStoreException e) {
          throw new BuildException("Problem when trying to store UniProt accession.");
        }
        return 1;
      }
    }
    
    return 0;
    
  }

}
