package org.flymine.biojava1.app.useCase;

import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.FeatureHolderFM;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.RepeatRegion;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.biojava.bio.seq.io.SeqIOTools;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.IOException;

/**
 * @author Markus Brosch
 */
public class FM2BJ {
  public static void main(String[] args) throws ObjectStoreException, IOException {

    //Setting up a connection to FlyMine by using the ObjectStore
    final String objectstore = "os.production";
    ObjectStore os;
    try {
      os = ObjectStoreFactory.getObjectStore(objectstore);
    } catch (Exception e) {
      throw new RuntimeException("getting ObjectStore failed");
    }

    //Some HARDCODED Features from FlyMin - just for demonstration - ID's change with new releases!
    Chromosome c = (Chromosome) os.getObjectById(new Integer(5335731));
    //Elements of Chromosomve IV
    Set set = new HashSet();
    RepeatRegion rr1 = (RepeatRegion) os.getObjectById(new Integer(5375138));
    set.add(rr1);
    RepeatRegion rr2 = (RepeatRegion) os.getObjectById(new Integer(5375144));
    set.add(rr2);
    Gene g1 = (Gene) os.getObjectById(new Integer(5124867));
    set.add(g1);
    Gene g2 = (Gene) os.getObjectById(new Integer(5124871));
    set.add(g2);

    if (c == null || rr1 == null || rr2 == null || g1 == null || g2 == null) {
      throw new RuntimeException("some of the hardcoded ID's may be expired ;-) Replace with whatever you like ...");
    }

    //////////////////////////
    // FlyMine to BioJava 1 //
    /////////////////////////

    //FeatureHolder

    FeatureHolderFM fh = new FeatureHolderFM(c, set);
    System.out.println("fh.toString() = " + fh.toString());
    System.out.println("fh.countFeatures() = " + fh.countFeatures());

    Iterator features = fh.features();
    while (features.hasNext()) {
      FeatureFM f = (FeatureFM)features.next();
      System.out.println("f = " + f);
    }

    //Sequence

    SequenceFM seq = fh.getSequence();
    //getting ALL Features on this sequence will load missing features and retuns ALL Features of
    //this Sequence (Chromosome)
    Iterator allFeaturesOnSequence = seq.features();
    System.out.println("seq.countFeatures() = " + seq.countFeatures());

    //If you ONLY want YOUR Features of FeatureHolder on the Sequence, do the following:
    seq.setFeatureHolderFM(fh);
    System.out.println("seq.countFeatures() = " + seq.countFeatures());
    SeqIOTools.writeEmbl(System.out,  seq);
  }
}
