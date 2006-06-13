package org.flymine.biojava1.query;

import org.flymine.biojava1.utils.ObjectStoreManager;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

/**
 * Queries FlyMine for a specific Chromosome, given the organism and identifier
 * 
 * @author Markus Brosch
 */
public final class QueryChromosome {

  /**
   * private constructor - utility class
   */
  private QueryChromosome() { }

  /**
   * This method returns a Chromosome
   * 
   * @param pOrganism
   *        the organism
   * @param pIdentifier
   *        the identifier of the Chromosome
   * @return a Chromosome of the specified organism and the specified identifier; <br>
   *         null, if either pOrganism does not exist or pIdentifier does not exist
   */
  public static Chromosome getChromosome(final String pOrganism, final String pIdentifier) {
    if (pOrganism == null) throw new NullPointerException("pOrganism must not be null");
    if (pIdentifier == null) throw new NullPointerException("pIdentifier must not be null");

    final Query q = new Query();
    q.setDistinct(true);
    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    //constrain to identifier
    final QueryClass qcC = new QueryClass(Chromosome.class);
    final QueryField qfI = new QueryField(qcC, "identifier");
    q.addFrom(qcC);
    q.addToSelect(qcC);
    final QueryValue qvI = new QueryValue(pIdentifier);
    final SimpleConstraint sc2 = new SimpleConstraint(qfI, ConstraintOp.EQUALS, qvI);
    cs.addConstraint(sc2);

    //constrain to organisem
    final QueryClass qcO = new QueryClass(Organism.class);
    final QueryField qfON = new QueryField(qcO, "name");
    q.addFrom(qcO);
    final QueryValue qvO = new QueryValue(pOrganism);
    final SimpleConstraint sc1 = new SimpleConstraint(qfON, ConstraintOp.EQUALS, qvO);
    cs.addConstraint(sc1);

    //constrain chromosome -> organism
    final QueryObjectReference ref = new QueryObjectReference(qcC, "organism");
    final ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcO);
    cs.addConstraint(cc);

    //Set constraint
    q.setConstraint(cs);

    final ObjectStore os = ObjectStoreManager.getInstance().getObjectStore();
    final Results res = new Results(q, os, os.getSequence());
    try {
      final ResultsRow row = (ResultsRow) res.get(0);
      return (Chromosome) row.get(0);
    } catch (IndexOutOfBoundsException e) {
      return null;
    } catch (RuntimeException rte) {
      throw new RuntimeException("Is connection to database established?", rte);
    }
  }

  public static void main(String[] args) {
    Chromosome c = QueryChromosome.getChromosome("Drosophila melanogaster", "4");
    System.out.println(c);
  }
}