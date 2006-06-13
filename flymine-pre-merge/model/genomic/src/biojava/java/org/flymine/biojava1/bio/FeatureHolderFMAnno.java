package org.flymine.biojava1.bio;

import org.apache.log4j.Logger;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.StrandedFeature;
import org.flymine.biojava1.exceptions.ModelException;
import org.flymine.biojava1.query.QueryFM;
import org.flymine.biojava1.utils.*;
import org.flymine.model.genomic.BioEntity;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.util.TypeUtil;

import java.io.Serializable;
import java.util.*;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * This FeatureHolder is specificly a FeatureHolder which is used to represent the object relations
 * partOf/inversePartOf of a Feature. BioJava can represent a Feature hierarchy, but it does not
 * allow mulitple parents - but we need that to represent FlyMine BioEntities! An example: a Exon
 * can "belong" to one ore more Transcripts according to the Sequence Ontology which is used by
 * FlyMine. Therefore the default BioJava hierarchy is not used at all, instead you can represent
 * the hierarchy or the object realtions in an Annotation bundle of each Feature. This
 * FeatureHolderAnno is part of an annotation bundle, e.g. we have a Exon and want to provide it's
 * Transcripts, therefore this FeaturHolderAnno provide the Transcripts. If you are not familar with
 * the Sequence Onology hierarchy: http://song.sourceforge.net/so.shtml
 * <p>
 * Two ways to use this FeatureHolder:
 * <ul>
 * <li>Inverse false: You have a FeatureFM and want to know about it's partOf/hasA relations, you
 * simply instanciate this FeatureHolder with the BioEntity (e.g. Transcript) and the hasA element
 * name (e.g. Exon) with inverse flag false.
 * <li>Inverse true: You have a Feature and want to know about it's inverse partOf/hasA relations,
 * you simply instanciate this FeatureHolder with the BioEntity (e.g. Exon) and the hasA element
 * name (e.g. Transcript) with inverse flag false.
 * </ul>
 * 
 * @author Markus Brosch
 */
public class FeatureHolderFMAnno extends FeatureHolderFM implements Serializable {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * logger
   */
  private static Logger _log = Logger.getLogger(FeatureHolderFMAnno.class);

  /**
   * the root BioEntity. This BioEntity is used to get the has_a relations by querying its
   * attributes; e.g. a Gene -\> related Transcripts or RegulatoryRegions. <br>
   * if _inverse flag was set to true, the inverse relation is used, therefore find the ancestors of
   * a Gene.
   */
  private transient BioEntity _bioEntity;

  /**
   * id of BioEntity to retrieve BioEntity as _bioEntity is transient
   */
  private final Integer _bioEntityID;

  /**
   * the attribute type name (unqualified) of the BioEntity; e.g. "RegulatoryRegion"
   */
  private final String _targetTypeName; //required

  /**
   * the attribute field name of the BioEntity; e.g. "regulatoryRegions" (How can _targetTypeName be
   * accessed using fields)
   */
  private final String _targetFieldName; //required

  /**
   * false: annotation represents partOf/hasA relation <br>
   * true: annotation represents inverse partOf/hasA relation
   */
  private final boolean _inverse; //required

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * Constructor
   * 
   * @param pSequence
   *        the underlying SequenceFM
   * @param pBioEntity
   *        the BioEntity the related BioEntities should be found
   * @param pTargetTypeName
   *        the target TypeName of the related BioEntity; e.g. Transcript
   * @param pTargetFieldName
   *        the target fieldName of related BioEntity; e.g. transcripts
   * @param pInverse
   *        false: represents partOf/has_a relation <br>
   *        true: represents inverse partOf/has_a relation
   */
  public FeatureHolderFMAnno(final SequenceFM pSequence, final BioEntity pBioEntity,
      final String pTargetTypeName, final String pTargetFieldName, final boolean pInverse) {

    super(pSequence);
    _isAllFeatures = false;

    if (pSequence == null) { throw new NullPointerException("pSequence must not be null"); }
    if (pBioEntity == null) { throw new NullPointerException("pBioEntity must not be null"); }
    if (pTargetFieldName == null) { throw new NullPointerException("pTargetField must not be null"); }
    if (pTargetTypeName == null) { throw new NullPointerException("pTargetName must not be null"); }
    if (!(ModelUtilsSoFa.getInstance().isPartOfModel(pTargetTypeName))) { throw new IllegalArgumentException(
        pTargetTypeName + " is not part of model"); }

    _bioEntity = pBioEntity;
    _bioEntityID = pBioEntity.getId();
    _targetFieldName = pTargetFieldName;
    _targetTypeName = pTargetTypeName;
    _inverse = pInverse;
  }

  // =======================================================================
  // Overwrites FeatureHolderFM
  // =======================================================================

  /**
   * @see FeatureHolderFM#countFeatures()
   */
  public int countFeatures() {
    if (_features != null) {
      return _features.size();
    } else if (_ids != null) {
      return _ids.size();
    } else {
      queryAndSetIDs();
      assert (_ids != null);
      return _ids.size();
    }
  }

  /**
   * @see FeatureHolderFM#filter(FeatureFilter, boolean)
   */
  public FeatureHolder filter(final FeatureFilter pFilter, final boolean pRecurse) {
    return filter(pFilter);
  }

  /**
   * @see FeatureHolderFM#filter(FeatureFilter)
   */
  public FeatureHolder filter(final FeatureFilter pFilter) {
    queryAndSetIDs();
    if (_ids.size() > 0) {
      return super.filter(pFilter);
    } else {
      return FeatureHolder.EMPTY_FEATURE_HOLDER;
    }
  }

  // -----------------------------------------------------------------------
  // protected & private helpers
  // -----------------------------------------------------------------------

  /**
   * get the Features belonging to this FeatureHolderFM
   * 
   * @return a Collection of Features
   */
  protected Collection getFeatures() {
    //get features if they alredy allocated
    if (_features != null) {
      if (Config.LOG) _log.debug("getFeatures(): _features!=null -> return direct");
      return _features;
    }

    //if ids of features not yet have been allocated
    if (_ids == null) {
      queryAndSetIDs();
    }
    assert (_ids != null);

    //only if there are ids, process ids, else feature is an empty collection
    if (_ids.size() > 0) {
      ids2features();
      assert (_features != null);
      return _features;
    } else {
      return new ArrayList();
    }
  }

  /**
   * query for the BioEntity IDs / Features belonging to this FeatureHolderAnno
   */
  private void queryAndSetIDs() {
    //  has_A ids
    if (_ids == null && _features == null && (!_inverse)) {
      _ids = new HashSet();
      try {
        Collection fieldValues = (Collection) TypeUtil.getFieldValue(getBioEntity(),
            _targetFieldName);
        for (Iterator iterFieldValue = fieldValues.iterator(); iterFieldValue.hasNext();) {
          final BioEntity be = (BioEntity) iterFieldValue.next();
          _ids.add(be.getId());
        }
      } catch (IllegalAccessException e) {
        //should never occur; if so, FlyMine changed model / access to field values
        throw new RuntimeException(e);
      }
      if (Config.LOG) _log.debug("getFeatures(): has_A ids done");
    }

    //inverse has_A ids
    if (_ids == null && _features == null && _inverse) {
      _ids = new HashSet();
      final ModelUtils utils = ModelUtils.getInstance(ModelFactory.getGenomicModel());
      final Class clazz;
      try {
        clazz = utils.getClassForTypeName(_targetTypeName);
        final QueryFM queryFM = QueryFM.getInstance(_sourceSeq);
        final Query q = queryFM.getAncestors(clazz, _bioEntityID);
        final Set ids = queryFM.getIds(q);
        for (Iterator it = ids.iterator(); it.hasNext();) {
          _ids.add(it.next());
        }
      } catch (ModelException e) {
        //_targetTypeName was tested in constructor if it is part of model!
        throw new RuntimeException("class for " + _targetTypeName
            + " not found; programming error;");
      }
      if (Config.LOG) _log.debug("getFeatures(): inverse has_A ids done");
    }
  }

  /**
   * helper method to add features from ids
   */
  private void ids2features() {
    _features = new HashSet();
    final QueryFM queryFM = QueryFM.getInstance(_sourceSeq);

    //process features already instanciated once
    for (Iterator it = FeatureRegistry.getAllocated(_ids).iterator(); it.hasNext();) {
      _features.add(FeatureFM.getInstance(_sourceSeq, (Integer) it.next()));
    }

    //process features still to add
    final Set toProcessIDs = FeatureRegistry.getRestToProcess(_ids);
    if (toProcessIDs.size() == 0) { return; }
    Map templateMap = null;
    if (Config.ISMEMORYMODE) {
      templateMap = queryFM.getTemplate(toProcessIDs);
    }
    for (Iterator it = toProcessIDs.iterator(); it.hasNext();) {
      final Integer id = (Integer) it.next();
      final FeatureFM f;
      if (Config.ISMEMORYMODE) {
        final StrandedFeature.Template templ = (StrandedFeature.Template) templateMap.get(id);
        f = FeatureFM.getInstance(_sourceSeq, id, templ); //full feature
      } else {
        f = FeatureFM.getInstance(_sourceSeq, id); //lazy feature
      }
      _features.add(f);
    }
  }

  /**
   * as _bioEntity is transient, use this getter to get the BioEntity
   * 
   * @return BioEntity concerning _bioEntityID
   */
  private BioEntity getBioEntity() {
    if (_bioEntity != null) {
      return _bioEntity;
    } else {
      try {
        _bioEntity = (BioEntity) ObjectStoreManager.getInstance().getObjectStore().getObjectById(
            _bioEntityID);
      } catch (ObjectStoreException e) {
        throw new RuntimeException(e); //no user error
      }
      return _bioEntity;
    }
  }

}