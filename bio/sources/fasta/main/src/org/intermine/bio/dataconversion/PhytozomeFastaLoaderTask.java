package org.intermine.bio.dataconversion;

import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.PendingClob;
import org.apache.tools.ant.BuildException;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;

public class PhytozomeFastaLoaderTask extends FastaLoaderTask {

  /* this is just a tweak for phytozome's structure of things.
   * proteome id is the key. And this was an emergency where I needed
   * to load 1 proteome. Hence the hard coding of this number.
   */
  private Integer proteomeId = new Integer(296);

  public void setProtoemeId(String proteomeId) {
    try {
      this.proteomeId = Integer.parseInt(proteomeId);
    } catch (NumberFormatException e) {
      throw new BuildException("Cannot parse integer from "+proteomeId);
    }
  }
  /**
   * The class name to use for objects created during load.  Generally this is
   * "org.intermine.model.bio.LocatedSequenceFeature" or "org.intermine.model.bio.Protein"
   * @param className the class name
   */
  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * Get and store() the Organism object to reference when creating new objects.
   * @param bioJavaSequence the biojava sequence to be parsed
   * @throws ObjectStoreException if there is a problem
   * @return the new Organism
   */
  /*protected Organism getOrganism(Sequence bioJavaSequence) throws ObjectStoreException {
    if (org == null) {
      org = getDirectDataLoader().createObject(Organism.class);
      org.setProteomeId(new Integer(proteomeId));
      getDirectDataLoader().store(org);
    }
    return org;
  }*/


  /**
   * Create a Sequence and an object of type className for the given BioJava Sequence.
   * @param organism the Organism to reference from new objects
   * @param bioJavaSequence the Sequence object
   * @throws ObjectStoreException if store() fails
   */
  private void processSequence(Organism organism, Sequence bioJavaSequence)
      throws ObjectStoreException {
    // some fasta files are not filtered - they contain sequences from organisms not
    // specified in project.xml
    if (organism == null) {
      throw new BuildException("Organism was not set.");
    }
    org.intermine.model.bio.Sequence seqObject = getDirectDataLoader().createObject(
        org.intermine.model.bio.Sequence.class);

    String sequence = bioJavaSequence.seqString();
    String md5checksum = Util.getMd5checksum(sequence);
    seqObject.setResidues(new PendingClob(sequence));
    seqObject.setLength(bioJavaSequence.length());
    seqObject.setMd5checksum(md5checksum);
    Class<? extends InterMineObject> imClass;
    Class<?> c;
    try {
      c = Class.forName(className);
      if (InterMineObject.class.isAssignableFrom(c)) {
        imClass = (Class<? extends InterMineObject>) c;
      } else {
        throw new RuntimeException("Feature className must be a valid class in the model"
            + " that inherits from InterMineObject, but was: " + className);
      }
    } catch (ClassNotFoundException e1) {
      throw new RuntimeException("unknown class: " + className
          + " while creating new Sequence object");
    }
    BioEntity imo = (BioEntity) getDirectDataLoader().createObject(imClass);

    String attributeValue = getIdentifier(bioJavaSequence);

    try {
      imo.setFieldValue(classAttribute, attributeValue);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error setting: " + className + "."
          + classAttribute + " to: " + attributeValue
          + ". Does the attribute exist?");
    }
    try {
      imo.setFieldValue("sequence", seqObject);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error setting: " + className + ".sequence to: "
          + attributeValue + ". Does the attribute exist?");
    }
    imo.setOrganism(organism);
    try {
      imo.setFieldValue("length", new Integer(seqObject.getLength()));
    } catch (Exception e) {
      throw new IllegalArgumentException("Error setting: " + className + ".length to: "
          + seqObject.getLength() + ". Does the attribute exist?");
    }

    try {
      imo.setFieldValue("md5checksum", md5checksum);
    } catch (Exception e) {
      // Ignore - we don't care if the field doesn't exist.
    }
  }
}
