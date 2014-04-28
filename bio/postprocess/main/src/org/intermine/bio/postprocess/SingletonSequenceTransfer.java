package org.intermine.bio.postprocess;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.BioQueries;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.GeneFlankingRegion;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinFamily;
import org.intermine.model.bio.Sequence;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.Item;

public class SingletonSequenceTransfer {

  private static final Logger LOG = Logger.getLogger(SingletonSequenceTransfer.class);
  ObjectStoreWriter osw;
  ObjectStore os;
  DataSource dataSource;
  
  public SingletonSequenceTransfer(ObjectStoreWriter osw) {
    this.osw = osw;
    this.os = osw.getObjectStore();
    dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
    dataSource.setName("Phytozome");
    try {
      dataSource = (DataSource) os.getObjectByExample(dataSource,
          Collections.singleton("name"));
    } catch (ObjectStoreException e) {
      throw new RuntimeException(
          "unable to fetch PhytoMine DataSource object", e);
    }
  }

  public void execute() throws ObjectStoreException {
    Iterator<?> resIter = getClusters();

    int count = 0;

    osw.beginTransaction();
    while (resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      ProteinFamily family = (ProteinFamily) rr.get(0);
      if (family.getMsa() == null && family.getConsensus() == null) {
        LOG.debug("Looking at family "+ family.getClusterName() + " with msa="+family.getMsa()+" and consensus "+family.getConsensus());
        String residues = getSequenceForFamily(family);
        if (residues != null) {
          LOG.debug("Creating sequence.");  
          MessageDigest md;
          try {
            md = MessageDigest.getInstance("MD5");
          } catch (NoSuchAlgorithmException e) {
            throw new BuildException("No such algorithm for md5?");
          }
          Sequence sequence = (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
          sequence.setResidues(new PendingClob(residues));
          md.update(residues.getBytes(),0,residues.length());
          String md5sum = new BigInteger(1,md.digest()).toString(16);
          sequence.setMd5checksum(md5sum);
          Integer len = new Integer(residues.length());
          sequence.setLength(len);
          try {
            osw.store(sequence);
            family.setConsensus(sequence);
            osw.store(family);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing sequence." + e.getMessage());
          }
        }
        count++;
        if ( (count%10000 == 0)) {
          LOG.info("Processed "+count+" sequences...");
        }
      }
    }

    LOG.info("Processed "+count+" families.");

    osw.commitTransaction();
  }
  
  private String getSequenceForFamily(ProteinFamily fam)  throws ObjectStoreException {
    /*Query q = new Query();

    q.setDistinct(true);

    QueryClass qcProtein = new QueryClass(Protein.class);
    q.addFrom(qcProtein);
    q.addToSelect(qcProtein);

    QueryClass qcGOAnnotation = new QueryClass(GOAnnotation.class);
    q.addFrom(qcGOAnnotation);

    QueryClass qcGoTerm = new QueryClass(GOTerm.class);
    q.addFrom(qcGoTerm);
    q.addToSelect(qcGoTerm);

    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    QueryObjectCollection goAnnotRef = new QueryObjectReference(qcGOAnnotation, "subject");
    cs.addConstraint(new ContainsConstraint(goAnnotRef, ConstraintOp.CONTAINS, qcGene));

    QueryObjectReference goTermRef = new QueryObjectReference(qcGOAnnotation, "ontologyTerm");
    cs.addConstraint(new ContainsConstraint(goTermRef, ConstraintOp.CONTAINS, qcGoTerm));

    q.setConstraint(cs);

    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Iterator<?>  res = os.execute(q, 5000, true, true, true).iterator();*/
    
    Collection<?> collection = null;
    try {
      collection = (Collection<?>) fam.getFieldValue("protein");
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (collection.size() > 1) {
      LOG.warn("Did not expect to have multiple proteins.");
      return null;
    }
    Protein p = (Protein)(collection.toArray()[0]);
    String s = p.getSequence().getResidues().toString();
    if (s.endsWith("*")) {
      return s.substring(0,s.length()-1);
    } else {
      return s;
    }

  }

  private Iterator<?> getClusters()
      throws ObjectStoreException {
    Query q = new Query();

    q.setDistinct(true);

    QueryClass qcFamily = new QueryClass(ProteinFamily.class);
    q.addFrom(qcFamily);
    q.addToSelect(qcFamily);


    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Results res = os.execute(q, 5000, true, true, true);
    return res.iterator();
  }
}
