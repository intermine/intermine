package org.flymine.biojava2;

import org.bjv2.gql.FilterException;
import org.bjv2.gql.Filters;
import org.bjv2.integrator.Integrator;
import org.bjv2.integrator.IntrospectionException;
import org.bjv2.seq.Anchor;
import org.bjv2.seq.FeatureRelation;
import org.bjv2.seq.Locator;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.RepeatRegion;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A UseCase class to show how the org can be used
 *
 * @author MarkusBrosch
 */
public class UseCase {

  public static void main(String[] args) throws ObjectStoreException, IntrospectionException {

    //Setting up a connection to FlyMine by using the ObjectStore
    final String objectstore = "os.production";
    ObjectStore os;
    try {
      os = ObjectStoreFactory.getObjectStore(objectstore);
    } catch (Exception e) {
      throw new RuntimeException("getting ObjectStore failed");
    }

    //Some hardcoded Features from FlyMine
    RepeatRegion rr1 = (RepeatRegion) os.getObjectById(5349301);
    RepeatRegion rr2 = (RepeatRegion) os.getObjectById(5349314);
    Gene g0 = (Gene) os.getObjectById(5124867);
    Gene g1 = (Gene) os.getObjectById(5124871);
    Protein p0 = (Protein) os.getObjectById(5124886);

    if (rr1==null || rr2==null || g0==null || g1==null || p0==null) {
      throw new RuntimeException("some of the hardcoded ID's may be expired ;-) Replace with whatever you like ...");
    }

    //All Features can be passed in to FlyMineToBJv2
    Set<BioEntity> bioEntities = new HashSet<BioEntity>();
    bioEntities.add(rr1);
    bioEntities.add(rr2);
    bioEntities.add(g0);
    bioEntities.add(g1);
    bioEntities.add(p0);

    FlyMineToBJv2 test = new FlyMineToBJv2(os, bioEntities);

    //print out Feature properties
    dump(test);
  }

  private static void dump(FlyMineToBJv2 pTest) {
    StringBuilder sb = new StringBuilder();

    //FlyMineToBJv2 has several Integrators to get data from, defined by Generics.
    Integrator<FeatureFM> featureIntegrator = pTest.getFeatureIntegrator();
    try {
      for (FeatureFM f : featureIntegrator.filter(Filters.<FeatureFM>acceptAll())) {

        sb.append("\n" + f.toString());
        sb.append("\n\nfeature - identifier:" + f.getIdentifier() + "\t");
        sb.append("\n\ttype:" + f.getType().getName());
        sb.append("\n\tontologyTerm:" + f.getOntologyTerm());
        sb.append("\n\tbioEntity:" + f.getBioEntity());

        sb.append("\n\trelations:" + f.getRelations());
        for (FeatureRelation rel : f.getRelations()) {
          sb.append("\n\t\trelation: source: " + rel.getSource());
          sb.append("\n\t\trelation: target: " + rel.getTarget());
        }

        Set<Locator> locators = f.getLocators();
        for (Locator loc : locators) {
          sb.append("\n\t\tlocator - " + loc.toString());

          List<Anchor> anchors = loc.getAnchors();
          for (Anchor anch : anchors) {
            sb.append("\n\t\t\tanchor - min:" + anch.getMin());
            sb.append("\t\tmax:" + anch.getMax());
            sb.append("\t\tstrand:" + anch.getStrand());
            sb.append("\t\tsequence:" + anch.getSequence() + "\t");
            //sb.append("\n\t\t" + anch.getSequence().getSymbolBuffer().length());
          }
        }
      }
    } catch (FilterException e) {
      e.printStackTrace(); //just for demonstration here ...
    }
    System.out.println(sb);
  }
}
