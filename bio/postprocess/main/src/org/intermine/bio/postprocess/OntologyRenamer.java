package org.intermine.bio.postprocess;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.Ontology;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

public class OntologyRenamer {
  private static final Logger LOG = Logger.getLogger(TransferGOAnnotations.class);
  protected ObjectStore os;
  protected ObjectStoreWriter osw = null;
  public  OntologyRenamer(ObjectStoreWriter osw) {    
    this.osw = osw;
    this.os = osw.getObjectStore();    
  }
  
  /**
   * Copy all GO annotations from the Protein objects to the corresponding Gene(s)
   * @throws ObjectStoreException if anything goes wrong
   */
  public void execute() throws ObjectStoreException {

    osw.beginTransaction();
    Pattern splitter = Pattern.compile("([^._]+)[._].*");

    Iterator<?> resIter = findOntologies();
    while (resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      Ontology thisOntology = (Ontology) rr.get(0);

      Matcher m = splitter.matcher(thisOntology.getName());

      // store if different
      if (m.matches()) {
        String newName = thisOntology.getName().substring(m.start(1),m.end(1));
        LOG.info("Renaming "+thisOntology.getName()+ " to " + newName);
        thisOntology.setName(newName);
        osw.store(thisOntology);
      }
    }
    osw.commitTransaction();
  }

  private Iterator<?> findOntologies()
      throws ObjectStoreException {
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcOntology = new QueryClass(Ontology.class);
      q.addFrom(qcOntology);
      q.addToSelect(qcOntology);
      

      ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = os.execute(q, 5000, true, true, true);
      return res.iterator();
  }
}

