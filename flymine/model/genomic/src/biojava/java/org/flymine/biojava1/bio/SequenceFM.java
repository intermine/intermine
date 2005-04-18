package org.flymine.biojava1.bio;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.apache.log4j.Logger;
import org.biojava.bio.Annotatable;
import org.biojava.bio.Annotation;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.*;
import org.biojava.bio.seq.Feature.Template;
import org.biojava.bio.symbol.*;
import org.biojava.utils.AbstractChangeable;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.query.QueryChromosome;
import org.flymine.biojava1.utils.Config;
import org.flymine.biojava1.utils.ObjectStoreManager;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a FlyMine (FM) specific BioJava Sequence <br>
 * 
 * @author Markus Brosch
 */
public class SequenceFM extends AbstractChangeable implements Sequence, IFlyMine, Serializable {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * logger
   */
  private static Logger _log = Logger.getLogger(SequenceFM.class);

  /**
   * as a Sequence is read only for FlyMine and represent one specific Chromosome, only one instance
   * for one Chromosome should exist -> singelton
   */
  private static Map _instances = new HashMap();

  /**
   * the Chromosome which represents this sequence<br>
   * never access directly, always use (Chromosome)getBioEntity() as it is transient
   */
  private final transient Chromosome _chromosome; //never access directly
  /**
   * as the Chromosome is transient this ID is kept as a reference to that chromosome
   */
  private final Integer _chromosomeID; //requiereds

  /**
   * urn represents the URN, like urn:flymine/internalID:someID
   */
  private final String _urn; //required

  /**
   * name represents the name of the Sequence
   */
  private final String _name; //required

  /**
   * annotation of that sequence
   */
  private final Annotation _annotation; //required

  /**
   * keep residues with a soft reference (memory issue)<br>
   * never access directly, always use getResidues() as it is transient
   */
  private transient SoftReference _residues = new SoftReference(null); //never access directly

  /**
   * Fetureholder of this sequence
   */
  private FeatureHolderFM _featureHolder = null;

  // =======================================================================
  // private constructor and getInstance
  // =======================================================================

  /**
   * private constructor
   * 
   * @param pChromosome
   *        is the corresponding FlyMine Chromosome <br>
   */
  private SequenceFM(final Chromosome pChromosome) {

    //set chromosome
    if (pChromosome != null) {
      _chromosome = pChromosome;
    } else {
      throw new NullPointerException("chromosomID must not be null");
    }

    _chromosomeID = _chromosome.getId();

    //set URN -> hardcoded
    Integer chromosomeID = _chromosome.getId();
    _urn = "urn:flymine/internalID:" + chromosomeID;

    //set name
    _name = _chromosome.getClass().getName() + "_ID:" + chromosomeID;

    //set Annotation
    _annotation = Annotation.EMPTY_ANNOTATION;

    if (Config.LOG) {
      _log.debug("constructor():");
      _log.debug("\t chromosome was set; ID:" + chromosomeID);
      _log.debug("\t urn was set to " + _urn);
      _log.debug("\t name was set to " + _name);
      _log.debug("\t annotation was set to " + _annotation);
    }
  }

  /**
   * getInstance of Sequence according to Chromosome/BioEntity ID
   * 
   * @param pOrganism
   *        is the full organism name of the chromosome <br>
   *        if null then pChromosome must not be null
   * @param pIdentifier
   *        the identifier of the Chromosome, e.g. 4, 2L, ... <br>
   *        if null then pChromosome must not be null
   * @return instance of Sequence according to Chromosome/BioEntity ID
   */
  public static SequenceFM getInstance(final String pOrganism, final String pIdentifier) {
    return getInstance(null, pOrganism, pIdentifier);
  }

  /**
   * getInstance of Sequence according to Chromosome/BioEntity ID
   * 
   * @param pChromosome
   *        is the corresponding FlyMine Chromosome, must not be null
   * @return instance of Sequence according to Chromosome/BioEntity ID
   */
  public static SequenceFM getInstance(final Chromosome pChromosome) {
    return getInstance(pChromosome, null, null);
  }

  /**
   * getInstance of Sequence according to Chromosome/BioEntity ID
   * 
   * @param pChromosome
   *        is the corresponding FlyMine Chromosome, must not be null
   * @param pOrganism
   *        is the full organism name of the chromosome <br>
   *        if null then pChromosome must not be null
   * @param pIdentifier
   *        the identifier of the Chromosome, e.g. 4, 2L, ... <br>
   *        if null then pChromosome must not be null
   * @return instance of Sequence according to Chromosome/BioEntity ID
   */
  private static SequenceFM getInstance(Chromosome pChromosome, final String pOrganism,
      final String pIdentifier) {

    final Integer id;
    if (pChromosome == null && pOrganism != null && pIdentifier != null) {
      pChromosome = QueryChromosome.getChromosome(pOrganism, pIdentifier);
      if (pChromosome == null) { throw new NullPointerException("either " + pOrganism
          + " does not exist or " + pIdentifier + " is wrong - no Chromosome found"); }
      id = pChromosome.getId();
    } else if (pChromosome != null) {
      id = pChromosome.getId();
    } else {
      throw new NullPointerException("parameters null; not allowed");
    }

    assert (id != null);

    SequenceFM seq = (SequenceFM) _instances.get(id);
    if (seq == null) {
      seq = new SequenceFM(pChromosome);
      _instances.put(id, seq);
    }
    return seq;
  }

  // =======================================================================
  // Methods
  // =======================================================================

  /**
   * Usually a Sequence takes care about it's features.
   * But if YOU want to provide the Feautures, then you have the choice.
   * Only provided Features in FeatureHolder will be taken into account.
   * @param pfh your FretureHolderFM you want to set for this Sequence.
   */
  public void setFeatureHolderFM(final FeatureHolderFM pfh) {
    _featureHolder = pfh;
  }

  // =======================================================================
  // Implements IFlyMine interface
  // =======================================================================

  /**
   * @see IFlyMine#getBioEntityID()
   */
  public Integer getBioEntityID() {
    return _chromosomeID;
  }

  /**
   * getter of Chromosome; always use getters for non primitiv typed attributes This SequenceFM
   * represents a FlyMine Chromosome
   * 
   * @return the Chromosome which represents this SequenceFM
   */
  public BioEntity getBioEntity() {
    if (_chromosome == null) {
      try {
        ObjectStore os = ObjectStoreManager.getInstance().getObjectStore();
        return (Chromosome) os.getObjectById(_chromosomeID);
      } catch (ObjectStoreException e) {
        throw new RuntimeException(e);
      }
    }
    return _chromosome;
  }

  // =======================================================================
  // Implements SymbolList interface
  // =======================================================================

  /**
   * @see SymbolList#getAlphabet()
   */
  public Alphabet getAlphabet() {
    return AlphabetManager.alphabetForName("DNA");
  }

  /**
   * @see SymbolList#iterator()
   */
  public Iterator iterator() {
    return getResidues().iterator();
  }

  /**
   * @see SymbolList#length()
   */
  public int length() {
    return ((Chromosome) getBioEntity()).getLength().intValue();
  }

  /**
   * @see SymbolList#seqString()
   */
  public String seqString() {
    return getResidues().seqString();
  }

  /**
   * @see SymbolList#subStr(int, int)
   */
  public String subStr(final int pStart, final int pEnd) {
    return getResidues().subStr(pStart, pEnd);
  }

  /**
   * @see SymbolList#subList(int, int)
   */
  public SymbolList subList(final int pStart, final int pEnd) {
    return getResidues().subList(pStart, pEnd);
  }

  /**
   * @see SymbolList#symbolAt(int)
   */
  public Symbol symbolAt(final int pIndex) {
    return getResidues().symbolAt(pIndex);
  }

  /**
   * @see SymbolList#toList()
   */
  public List toList() {
    return getResidues().toList();
  }

  /**
   * @see SymbolList#edit(org.biojava.bio.symbol.Edit)
   */
  public void edit(final Edit pEdit) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the underlying SymbolList");
  }

  // -----------------------------------------------------------------------
  // private helpers for implementation of SymbolList
  // -----------------------------------------------------------------------

  /**
   * @return the residues of the SequenceFM. As these residues can be quite big, they are kept in a
   *         SoftReference.
   */
  private SymbolList getResidues() {
    SymbolList res;
    if (_residues != null) { //_residues is transient
      res = (SymbolList) _residues.get();
      if (res != null) {
        if (Config.LOG) _log.info("getResidues(): return, as residues are already allocated");
        return res;
      }
    }
    try {
      res = DNATools.createDNA(((Chromosome) getBioEntity()).getSequence().getResidues());
      assert (res != null || res.length() == 0) : "residues of chromosome are null or empty";
      _residues = new SoftReference(res);
      if (Config.LOG) _log.info("getResidues(): residues were retrieved and set");
    } catch (IllegalSymbolException e) {
      _log.fatal("getResidues(): problem processing the FlyMine chromosome sequence");
      throw new RuntimeException("problem processing the FlyMine chromosome sequence", e);
    }
    return res;
  }

  // =======================================================================
  // Implements Sequence interface
  // =======================================================================

  /**
   * @see Sequence#getURN()
   */
  public String getURN() {
    return _urn;
  }

  /**
   * @see Sequence#getName()
   */
  public String getName() {
    return _name;
  }

  // =======================================================================
  // Implements FeatureHolder interface
  // =======================================================================

  /**
   * @see org.biojava.bio.seq.FeatureHolder#countFeatures()
   */
  public int countFeatures() {
    return getFeatureHolderFM().countFeatures();
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#features()
   */
  public Iterator features() {
    return getFeatureHolderFM().features();
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter, boolean)
   * @deprecated
   */
  public FeatureHolder filter(final FeatureFilter pFilter, final boolean pRecurse) {
    return getFeatureHolderFM().filter(pFilter);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter)
   */
  public FeatureHolder filter(final FeatureFilter pFilter) {
    return getFeatureHolderFM().filter(pFilter);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#createFeature(org.biojava.bio.seq.Feature.Template)
   */
  public Feature createFeature(final Template pTemplate) throws BioException, ChangeVetoException {
    return getFeatureHolderFM().createFeature(pTemplate);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#removeFeature(org.biojava.bio.seq.Feature)
   */
  public final void removeFeature(final Feature pFeature) throws ChangeVetoException, BioException {
    getFeatureHolderFM().removeFeature(pFeature);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#containsFeature(org.biojava.bio.seq.Feature)
   */
  public boolean containsFeature(final Feature pFeature) {
    return getFeatureHolderFM().containsFeature(pFeature);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#getSchema()
   */
  public FeatureFilter getSchema() {
    return getFeatureHolderFM().getSchema();
  }

  // -----------------------------------------------------------------------
  // private helper
  // -----------------------------------------------------------------------

  /**
   * @return return the FeatureHolder which represents the FH of this sequence
   */
  private FeatureHolderFM getFeatureHolderFM() {
    if (_featureHolder == null) {
      if (Config.LOG) _log.info("getFeatureHolderFM(): new FeatureHolderFM was set");
      _featureHolder = new FeatureHolderFM(this);
    }
    return _featureHolder;
  }

  // =======================================================================
  // Implements Annotatable interface
  // =======================================================================

  /**
   * @see Annotatable#getAnnotation()
   */
  public Annotation getAnnotation() {
    return _annotation;
  }

}