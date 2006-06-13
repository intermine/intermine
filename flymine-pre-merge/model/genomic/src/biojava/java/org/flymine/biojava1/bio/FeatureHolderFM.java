package org.flymine.biojava1.bio;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.apache.log4j.Logger;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.*;
import org.biojava.utils.walker.Walker;
import org.biojava.utils.walker.WalkerFactory;
import org.flymine.biojava1.exceptions.ModelException;
import org.flymine.biojava1.query.QueryFM;
import org.flymine.biojava1.query.VisitorFM;
import org.flymine.biojava1.utils.Config;
import org.flymine.biojava1.utils.FeatureRegistry;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.intermine.objectstore.query.Query;

import java.io.Serializable;
import java.util.*;

/**
 * Implementation of a FlyMine (FM) specific BioJava FeatureHolder. <br>
 * A FeatureHolderFM can be instantiated by only providing a SequencFM, then this FeatureHolderFM
 * represents all the Features of SequenceFM. The FeatureHolderFM can also be instantiated with a
 * Collection of IDs regarding BioEnities. Therefore this FeatureHolder reflects a subset of
 * Features (IDs) of the Sequence.
 * 
 * @author Markus Brosch
 */
public class FeatureHolderFM extends AbstractFeatureHolder implements FeatureHolder, Serializable {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * logger
   */
  private static Logger _log = Logger.getLogger(FeatureHolderFM.class);

  /**
   * the source/root SequenceFM the features of this FeatureHolder are located
   */
  protected final SequenceFM _sourceSeq; //required

  /**
   * chromosome ID which represents the ID of _sourceSeq; can be directly accessed from sourceSeq,
   * but it is called many times, therefore introduced as a attribute
   */
  protected final Integer _chromosomeID;

  /**
   * if this FeatureHolder holds _all_ features of a SequenceFM/Chromosome, this flag ist true; <br>
   * if this FeatureHolder is a subset of features of a chromosome, this flag is set to false
   */
  protected boolean _isAllFeatures;

  /**
   * ID's of Features (BioEntities) represented by this FeatureHolder
   */
  protected Collection _ids;

  /**
   * Set of FeatureFM Objects which are held by this FeatureHolder
   */
  protected Collection _features;

  // =======================================================================
  // Constructors
  // =======================================================================

  /**
   * Constructor for a FeatureHolder acting as a FeatureHolder for a whole Sequence
   * 
   * @param pSourceSeq
   *        represents the source / root Sequence (FlyMine Chromosome) the Features of this
   *        FeatureHolder are located on. All operations are based on this underlying sequence.
   *        Therefore, _all_ Features of this sequence are taken into account. If you want to build
   *        a subset of features, use the extended constructor and pass in the ids of these features
   */
  public FeatureHolderFM(final SequenceFM pSourceSeq) {
    if (pSourceSeq == null) { throw new NullPointerException("SequenceFM must not be null"); }
    _sourceSeq = pSourceSeq;
    _chromosomeID = pSourceSeq.getBioEntityID();
    _isAllFeatures = true;
  }

  /**
   * Constructor for a FeatureHolder representing a subset of Features of a specific
   * SequenceFM/Chromosome defined by a Collection of BioEntity IDs
   * 
   * @param pSourceSeq
   *        represents the source / root Sequence (FlyMine Chromosome) of the features.
   * @param pIds
   *        the IDs of Features (FlyMine BioEntities) this FeatureHolder holds; <br>
   *        If IDs are provided, all operations and methods only work on this subset of features
   *        according to the ID set.
   */
  public FeatureHolderFM(final SequenceFM pSourceSeq, final Collection pIds) {
    if (pSourceSeq == null) { throw new NullPointerException("pSourceSeq must not be null"); }
    if (pIds == null) { throw new NullPointerException("pIds must not be null"); }

    _sourceSeq = pSourceSeq;
    _chromosomeID = pSourceSeq.getBioEntityID();
    _ids = pIds;
    _isAllFeatures = false;
  }

  /**
   * Constructor for a FeatureHolder representing a subset of Features of a specific
   * SequenceFM/Chromosome defined by a Collection of BioEntities
   * 
   * @param pChromosome
   *        the chromosome the Features/BioEntities are located on
   * @param pBioEntities
   *        the FlyMine BioEntities this FeatureHolder should represent
   */
  public FeatureHolderFM(final Chromosome pChromosome, final Collection pBioEntities) {
    if (pChromosome == null) { throw new NullPointerException("pChromosome must not be null"); }
    if (pBioEntities == null) { throw new NullPointerException("pBioEntities must not be null"); }

    _sourceSeq = SequenceFM.getInstance(pChromosome);
    _chromosomeID = pChromosome.getId();
    _isAllFeatures = false;
    _ids = new ArrayList();
    for (Iterator it = pBioEntities.iterator(); it.hasNext();) {
      BioEntity be = (BioEntity) it.next();
      _ids.add(be.getId());
    }
  }

  // =======================================================================
  // Implements FeatureHolder interface
  // =======================================================================

  /**
   * Count how many Features (BioEntities) are contained on this Sequence (Chromosome)
   * 
   * @return a positive integer or zero, equal to the number of features contained
   */
  public int countFeatures() {
    if (_ids != null) {
      if (Config.LOG) _log.info("countFeatures(): ids.size();");
      return _ids.size();
    }
    if (_features != null) {
      if (Config.LOG) _log.info("countFeatures(): features.size();");
      return _features.size();
    }
    final QueryFM q = QueryFM.getInstance(_sourceSeq);
    try {
      if (Config.LOG) _log.info("countFeatures(): q.countResult(q.byClass(BioEntity.class));");
      return q.countResult(q.byClass(BioEntity.class));
    } catch (ModelException e) {
      _log.fatal("ModelException; Not a BioEntity! + " + e.getMessage());
      throw new RuntimeException("Not a BioEntity", e);
    }
  }

  /**
   * @see FeatureHolder#features()
   */
  public Iterator features() {
    return getFeatures().iterator();
  }

  /**
   * @param pFilter
   *        Your FeatureFilter. Please check out the documentation! Not all featureFilters are
   *        implemented.
   * @param pRecurse
   *        either, no effect, as method is deprecated
   * @return FeatureFM Objects in a FeatureHolder which fulfil the filter pFilter
   * @deprecated
   * @see FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter, boolean)
   */
  public FeatureHolder filter(final FeatureFilter pFilter, final boolean pRecurse) {
    return filter(pFilter, false);
  }

  /**
   * @param pFilter
   *        Your FeatureFilter. Please check out the documentation! Not all featureFilters are
   *        implemented.
   * @return FeatureFM Objects in a FeatureHolder which fulfil the filter pFilter
   * @see FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter)
   */
  public FeatureHolder filter(FeatureFilter pFilter) {
    if (Config.LOG) _log.info("filter(filter):");

    //check if features of featureFilter tree are disjoint
    final FeatureFilter childF = new FeatureFilter.Not(FeatureFilter.top_level);
    if (FilterUtils.areDisjoint(pFilter, childF)) { return FeatureHolder.EMPTY_FEATURE_HOLDER; }

    //optimize filter
    pFilter = FilterUtils.optimize(pFilter);

    //IF in memory processing
    if (Config.ISMEMORYMODE) {
      if (Config.LOG) _log.info("\t isMemoryMode ~> super.filter(filter, false);");
      //note: this.features() is called in super.filter()
      return super.filter(pFilter, false);
    }

    //IF on the fly processing / non memory mode: query dynamically:
    final QueryFM q = QueryFM.getInstance(_sourceSeq);
    //get a visitor & walk the filter tree
    final VisitorFM visitor = new VisitorFM(q);
    final WalkerFactory wf = WalkerFactory.getInstance();
    if (Config.LOG) _log.info("\t !isMemoryMode ~> walker / visitor used");
    try {
      final Walker walker = wf.getWalker(visitor);
      walker.walk(pFilter, visitor);
      //get the generated dynamic Query & return FH with resulting IDs
      Query query = (Query) walker.getValue();
      if (Config.LOG) _log.info("\t walker.getValue() -> query -> \n" + query.toString());
      assert (query != null) : "something went wrong by walking the feature filter tree";
      if (_ids != null && !_isAllFeatures) {
        query = q.constrainQueryByIDs(query, _ids); //constrain by given IDs
        if (Config.LOG) _log.info("\t constrained query: \n" + query.toString());
      }
      final Collection resultIDs = q.getIds(query);
      if (Config.LOG) _log.info("\t return new FeatureHolderFM(_sourceSeq, resulIds of query)");
      return new FeatureHolderFM(_sourceSeq, resultIDs);
    } catch (BioException e) {
      //should never happen; no user error -> programming error
      _log.fatal("Problem getting the walker from WalkerFactory" + e.getMessage());
      throw new RuntimeException("Problem getting the walker from WalkerFactory", e);
    } catch (UnsupportedOperationException e) {
      _log.warn("on of the FeatureFilters in your filter tree is not supported! Return null");
      return FeatureHolder.EMPTY_FEATURE_HOLDER;
    }
  }

  /**
   * @see FeatureHolder#containsFeature(org.biojava.bio.seq.Feature)
   */
  public boolean containsFeature(final Feature pFeature) {
    if (pFeature instanceof FeatureFM) { return getFeatures().contains(pFeature); }
    return false; //FeatureHolderFM can only hold instances of FeatureFM
  }

  /**
   * @see FeatureHolder#getSchema()
   */
  public FeatureFilter getSchema() {
    return FeatureFilter.all;
  }

  // =======================================================================
  // Methods
  // =======================================================================

  /**
   * printing all Features
   * @return a String with all Features
   */
  public String toStringAllFeatures() {
    StringBuffer result = new StringBuffer(this.toString());
    for (Iterator it = features(); it.hasNext();) {
      result.append("\t" + it.next());
    }
    return result.toString();
  }

  /**
   * @return Underlying Sequence of this FeautreHolder
   */
  public SequenceFM getSequence() {
    return _sourceSeq;
  }

  // -----------------------------------------------------------------------
  // private / protected helpers for implementation of FeatureHolder
  // -----------------------------------------------------------------------

  /**
   * method to get features of this featureHolder. If features are already allocated, simply return.
   * Else: depending on the memory mode, create features and return. In memoryMode, the whole
   * Feature object is instantiated, in non memory mode, it is lazily instantiated by only passing
   * in the id (the Feature itself then can retrive it's data - once it is needed)
   * 
   * @return an ArrayList of features
   */
  protected Collection getFeatures() {
    if (Config.LOG) _log.info("getFeatures()");
    if (_features == null) {
      _features = new ArrayList();

      if (Config.ISMEMORYMODE) {
        ////memory mode - template features
        if (Config.LOG) _log.info("\t isMemoryMode");
        templateFeatures();
      } else {
        ////non memory mode - lazy features
        if (Config.LOG) _log.info("\t isNotMemoryMode");
        lazyFeatures();
      }
    }
    assert (_features != null);
    return _features;
  }

  /**
   * helper of getFeatures(); adds all features with fully loaded templates (memory mode)
   */
  protected void templateFeatures() {
    if (_isAllFeatures) {
      if (Config.LOG) _log.info("\t isAllFeatures");
      ////if this featureHolder has ALL features of the underlying chromosome
      templateFeaturesAll();
    } else {
      if (Config.LOG) _log.info("\t isNOTallFeaures");
      ////this featureHolder keeps only a subset of features of the underlying chromosome
      templateFeaturesPartial();
    } //else
  }

  /**
   * helper of templateFeatures(QueryFM q); adds all features of the underlying sequence
   */
  private void templateFeaturesAll() {
    if (Config.LOG) _log.info("\t get map, iterate and add new Features to _features");
    final QueryFM queryFM = QueryFM.getInstance(_sourceSeq);
    //get ALL feature templates of this sequence
    final Map map = (queryFM.getTemplate(null));
    for (Iterator it = map.keySet().iterator(); it.hasNext();) {
      final Integer id = (Integer) it.next();
      final StrandedFeature.Template templ = (StrandedFeature.Template) map.get(id);
      _features.add(FeatureFM.getInstance(_sourceSeq, id, templ));
    }
  }

  /**
   * helper of templateFeatures(QueryFM q); adds only features of provided _ids during construction
   * of this FeatureHolder - therefore it represents a subset of features of the underlying sequence
   */
  private void templateFeaturesPartial() {
    ////check if some FeatureFM are already instanciated by using the FeatureRegistry and add them
    for (Iterator it = FeatureRegistry.getAllocated(_ids).iterator(); it.hasNext();) {
      Integer aID = (Integer) it.next();
      _features.add(FeatureFM.getInstance(_sourceSeq, aID));
    }

    ////if not instantiated
    //chop ids to bunches of 10000's - avoid too complex sql query
    if (Config.LOG) _log.info("\t get.Templates of given rest IDs");
    final QueryFM queryFM = QueryFM.getInstance(_sourceSeq);
    final Map templateMap = new HashMap();
    final int packageSize = 10000;
    final Set restIDs = FeatureRegistry.getRestToProcess(_ids);
    final Iterator iterateIds = restIDs.iterator();
    //iterate in bunches of 'packageSize' over restIDs and get the feature templates
    for (int i = 0; i < restIDs.size(); i += packageSize) {
      for (int j = i; j < (i + packageSize); j++) {
        final Set bunchOfIds = new HashSet();
        if (iterateIds.hasNext()) {
          bunchOfIds.add(iterateIds.next());
        } else {
          break;
        }
        templateMap.putAll(queryFM.getTemplate(bunchOfIds));
      } //inner for
      if (!iterateIds.hasNext()) {
        break;
      }
    } //outer for
    //iterate the templates and generate the related features
    if (Config.LOG) _log.info("\t iterate map [ID-Template] and add !lazy features to features");
    for (Iterator it = templateMap.keySet().iterator(); it.hasNext();) {
      final Integer id = (Integer) it.next();
      final StrandedFeature.Template templ = (StrandedFeature.Template) templateMap.get(id);
      _features.add(FeatureFM.getInstance(_sourceSeq, id, templ));
    }
  }

  /**
   * helper of getFeatures(); adds all features as lazy features
   */
  private void lazyFeatures() {
    ////lazyFeaturesAll
    if (_ids == null) {
      if (Config.LOG) _log.info("\t ids == null ~> query for all ids of sequence features");
      final QueryFM queryFM = QueryFM.getInstance(_sourceSeq);
      try {
        _ids = queryFM.getIds(queryFM.byClass(BioEntity.class));
      } catch (ModelException e) {
        _log.fatal("Not a BioEntity " + e.getMessage());
        throw new RuntimeException("Not a BioEntity", e);
      }
    }
    ////lazyFeaturesAll && lazyFeaturesPartial
    assert (_ids != null);
    if (Config.LOG) _log.info("\t iterate over ids and add new lazy Features to features");
    for (Iterator it = _ids.iterator(); it.hasNext();) {
      final Integer id = (Integer) it.next();
      _features.add(FeatureFM.getInstance(_sourceSeq, id));
    }
  }

}