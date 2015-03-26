/**
 * 
 */
package org.intermine.bio.postprocess;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.BioQueries;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinAnalysisFeature;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.DynamicUtil;
/**
 * @author jcarlson
 *
 */
public class FeatureNamer {
  private ObjectStoreWriter osw = null;
  private ObjectStore os;
  private DataSet dataSet;
  private DataSource dataSource;
  private static final Logger LOG = Logger.getLogger(FeatureNamer.class);

  public FeatureNamer(ObjectStoreWriter osw) {
    this.osw = osw;
    this.os = osw.getObjectStore();
  }

  
  public void execute() throws ObjectStoreException {
    
    Query q = new Query();
    QueryClass qcBioentity = new QueryClass(BioEntity.class);
    q.addFrom(qcBioentity);
    q.addToSelect(qcBioentity);
    
    QueryField qcNameField = new QueryField(qcBioentity,"name");

    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    cs.addConstraint(new SimpleConstraint(qcNameField,ConstraintOp.IS_NULL));

    q.setConstraint(cs);

    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Results results = os.execute(q, 500000, true, true, true);

    Iterator<?> resIter = results.iterator();

    int count = 0;
    osw.beginTransaction();
    while (resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      BioEntity feat = (BioEntity) rr.get(0);
      if (feat.getPrimaryIdentifier()!=null && !feat.getPrimaryIdentifier().isEmpty() ) {
        feat.setName(feat.getPrimaryIdentifier());
        osw.store(feat);
      } else {
        LOG.warn("Bioentity "+feat.getId()+" has no primary identifier.");
      }
      if ((count % 10000) == 0) {
        LOG.info("Named " + count + " bioentities...");
      }
      count++;
    }
    osw.commitTransaction();
    
    
  }
  
}

