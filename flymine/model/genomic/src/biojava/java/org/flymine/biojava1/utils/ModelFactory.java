package org.flymine.biojava1.utils;

import com.hp.hpl.jena.ontology.OntModel;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.ontology.Dag2Owl;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.Owl2InterMine;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provide the FlyMine genomic model and the Sequence Ontology Light (SOFA) model. Both
 * as InterMine models.
 *
 * @author Markus Brosch
 */
public final class ModelFactory {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * represents the Sequence Ontology light version (SOFA) as a intermine model, describes partOf
   * relations, e.g. Gene-\>{Transcript,RegulatoryRegion}
   */
  private static Model _SOFAMODEL;

  /**
   * represents the genomic model of FlyMine
   */
  private static Model _GENOMICMODEL;

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * private Constructor
   */
  private ModelFactory() { }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * get the genomic flymine model
   *
   * @return genomic model
   */
  public static Model getGenomicModel() {
    if (_GENOMICMODEL == null) {
      try {
        _GENOMICMODEL = Model.getInstanceByName(Config.FLYMINE_GENOMICMODELNAME);
      } catch (MetaDataException e) {
        throw new RuntimeException("config error: problems getting an instance of "
            + Config.FLYMINE_GENOMICMODELNAME + " model", e);
      }
    }

    return _GENOMICMODEL;
  }

  /**
   * get the SOFA model
   *
   * @return SOFA model
   */
  public static Model getSofaModel() {
    if (_SOFAMODEL == null) {
      ////SOFAMODEL////
      //dag file -> owl model
      DagParser dagParser = new DagParser();
      FileReader in;
      try {
        in = new FileReader(Config.SOFA_FILEPATH);
      } catch (FileNotFoundException e) {
        throw new RuntimeException("sofa ontology file not found", e);
      }
      Set dagTerms;
      try {
        dagTerms = dagParser.processForClassHeirarchy(in);
      } catch (Exception e1) {
        throw new RuntimeException("sofa ontology file could not be parsed correctly", e1);
      }
      Dag2Owl dag2Owl = new Dag2Owl(Config.SOFA_NAMESPACE, false);
      dag2Owl.process(new HashSet(dagTerms));
      OntModel ontModel = dag2Owl.getOntModel();
      //owl model -> intermine model
      Owl2InterMine owl2Intermine = new Owl2InterMine("sofa", Config.SOFA_PACKAGE);
      try {
        _SOFAMODEL = owl2Intermine.process(ontModel, Config.SOFA_NAMESPACE);
      } catch (Exception e) {
        throw new RuntimeException("processing error: owl model -> intermine model", e);
      }
      assert (_SOFAMODEL != null) : "postcondition: model should not be null";
    }

    return _SOFAMODEL;
  }
}