package org.flymine.biojava1.bio;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.*;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.ontology.Term;
import org.biojava.utils.AbstractChangeable;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.query.QueryFM;
import org.flymine.biojava1.utils.Config;
import org.flymine.biojava1.utils.FeatureRegistry;
import org.flymine.biojava1.utils.ModelUtilsSoFa;
import org.flymine.model.genomic.BioEntity;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Implementation of a FlyMine (FM) specific BioJava Feature.
 * <ul>
 * <li>A FeatureFM regarding a specific FlyMine BioEntity is only instantiated once (singleton for
 * each BioEntity).
 * <li>A FeatureFM can be instantiated lazy, therefore you have only to provide the underlying
 * Sequence and the concerning BioEntity ID; the FeatureFM data can be retrieved by the feature
 * itself lazily at the time it is needed.
 * <li>A FeatureFM can also be instatiated with a enriched feature template to provide the data for
 * the feature (non lazy).
 * <li>In the case the mode of the application is "MemoryMode" (specified in the config file), all
 * data is kept with a Reference. In the case "NonMemoryMode" the data is kept with a SoftReference
 * to minimize memory requirements.
 * </ul>
 * 
 * @author Markus Brosch
 */
public class FeatureFM extends AbstractChangeable implements StrandedFeature, IFlyMine,
    Serializable {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * logger
   */
  private static Logger _log = Logger.getLogger(FeatureFM.class);

  /**
   * there should only be one Feature for one BioEntity; therefore each Feature exists only once -\>
   * Singelton pattern.
   */
  private static Map _instances = new HashMap();

  /**
   * the sequence this features is located
   */
  private final SequenceFM _seq; //required

  /**
   * the ID of the FlyMine BioEntity which represents this feature
   */
  private final Integer _bioEntityID; //required

  /**
   * the template is held as a SoftReferenz, to free memory if needed; only in non memory modus
   */
  private transient SoftReference _templSoft = new SoftReference(null); //non memory modus

  /**
   * the template is held permanent; only in memory modus
   */
  private StrandedFeature.Template _templPersist; //memory modus

  /**
   * the FlyMine BioEntity which represents this feature is held with a SoftReference
   */
  private transient SoftReference _bioEntity = new SoftReference(null);

  // =======================================================================
  // private constructor & getInstance
  // =======================================================================

  /**
   * private Constructor
   * 
   * @param pSeq
   *        the sequence this feature is located
   * @param pBioEntityID
   *        the FlyMine BioEntity ID which represents this feature - must be an existing ID refering
   *        to an BioEntity - is not checked by the constructor, as millions of features checking if
   *        id exists in DB is too expensive. But be aware, that you can get an
   *        IllegalArgumentException during the work with this feature, if ID is not refering to an
   *        BioEntity or does not exist. Usually you never have to instanciate a Feature by
   *        yourself, you can always instatiate a SequenceFM and query for a specific Feature by
   *        identifier or a proper filter.
   * @param pTemplate
   *        a given template for this feature (faster) already given; can be null, then it is equal
   *        to the simple constucotor
   */
  private FeatureFM(final SequenceFM pSeq, final Integer pBioEntityID,
      final StrandedFeature.Template pTemplate) {
    if (pSeq == null) { throw new NullPointerException("seq must not be null"); }
    if (pBioEntityID == null) { throw new NullPointerException("id must not be null"); }

    _seq = pSeq;
    _bioEntityID = pBioEntityID;
    if (Config.ISMEMORYMODE) {
      _templPersist = pTemplate;
    } else {
      _templSoft = new SoftReference(pTemplate);
    }

    //registry
    FeatureRegistry.registerKey(pBioEntityID);

    //logging
    if (Config.LOG) {
      _log.debug("constructor():");
      _log.debug("\t seq was set; name: " + _seq.getName());
      _log.debug("\t bioEntityID was set; ID: " + pBioEntityID);
      if (Config.ISMEMORYMODE) {
        _log.debug("\t templPersist was set");
      } else {
        _log.debug("\t templSoft was set");
      }
    }
  }

  /**
   * get instance of specific pBioEntityID as FeatureFM
   * 
   * @param pSequence
   *        the sequence this feature is located on
   * @param pBioEntityID
   *        the FlyMine BioEntity ID which represents this feature - must be an existing ID refering
   *        to an BioEntity - is not checked by the constructor, as millions of features checking if
   *        id exists in DB is too expensive. But be aware, that you can get an
   *        IllegalArgumentException during the work with this feature, if ID is not refering to an
   *        BioEntity or does not exist. Usually you never have to instanciate a Feature by
   *        yourself, you can always instatiate a SequenceFM and query for a specific Feature by
   *        identifier or a proper filter.
   * @return an instance of the BioEntity as a FeatureFM
   */
  public static FeatureFM getInstance(final SequenceFM pSequence, final Integer pBioEntityID) {
    return getInstance(pSequence, pBioEntityID, null);
  }

  /**
   * get instance of specific pBioEntityID as FeatureFM
   * 
   * @param pSequence
   *        the sequence this feature is located
   * @param pBioEntityID
   *        the FlyMine BioEntity ID which represents this feature - must be an existing ID refering
   *        to an BioEntity - is not checked by the constructor, as millions of features checking if
   *        id exists in DB is too expensive. But be aware, that you can get an
   *        IllegalArgumentException during the work with this feature, if ID is not refering to an
   *        BioEntity or does not exist. Usually you never have to instanciate a Feature by
   *        yourself, you can always instatiate a SequenceFM and query for a specific Feature by
   *        identifier or a proper filter.
   * @param pTemplate
   *        a given template for this feature (faster); can be null, then it will be queried lazily
   * @return an instance of the BioEntity as a FeatureFM
   */
  public static FeatureFM getInstance(final SequenceFM pSequence, final Integer pBioEntityID,
      final StrandedFeature.Template pTemplate) {

    FeatureFM f = (FeatureFM) (_instances.get(pBioEntityID));
    if (f == null) {
      f = new FeatureFM(pSequence, pBioEntityID, pTemplate);
      _instances.put(pBioEntityID, f);
    }
    return f;
  }

  // =======================================================================
  // Implement IFlyMine interface
  // =======================================================================

  /**
   * @see IFlyMine#getBioEntityID()
   */
  public Integer getBioEntityID() {
    return _bioEntityID;
  }

  /**
   * getting the BioEntity is queried lazily - be aware and use only if you realy need the BioEntity
   * 
   * @see IFlyMine#getBioEntity()
   */
  public BioEntity getBioEntity() {
    if (_bioEntity == null) {
      _bioEntity = new SoftReference(null); //transient
    }
    BioEntity be = (BioEntity) _bioEntity.get();
    if (be != null) { return be; }

    final QueryFM query = QueryFM.getInstance(_seq);
    be = query.getBioEntityByID(_bioEntityID);
    assert (be != null);
    _bioEntity = new SoftReference(be);
    return be;
  }

  // =======================================================================
  // Implements Feature interface
  // =======================================================================

  /**
   * @see Feature#getLocation()
   */
  public Location getLocation() {
    final Location loc = getTemplate().location;
    return new RangeLocation(loc.getMin(), loc.getMax());
  }

  /**
   * @see Feature#setLocation(org.biojava.bio.symbol.Location)
   */
  public void setLocation(final Location pLocation) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the Location; FlyMine is read only");
  }

  /**
   * @see Feature#getType()
   */
  public final String getType() {
    return getTemplate().type;
  }

  /**
   * @see Feature#setType(java.lang.String)
   */
  public void setType(final String pType) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the Type; FlyMine is read only");
  }

  /**
   * @see Feature#getTypeTerm()
   */
  public Term getTypeTerm() {
    return getTemplate().typeTerm;
  }

  /**
   * @see Feature#setTypeTerm(org.biojava.ontology.Term)
   */
  public void setTypeTerm(final Term pTerm) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the TypeTerm; FlyMine is read only");
  }

  /**
   * @see Feature#getSourceTerm()
   */
  public Term getSourceTerm() {
    return getTemplate().sourceTerm;
  }

  /**
   * @see Feature#setSourceTerm(org.biojava.ontology.Term)
   */
  public void setSourceTerm(final Term pTerm) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the SourceTerm; FlyMine is read only");
  }

  /**
   * @see Feature#getSource()
   */
  public String getSource() {
    return getTemplate().source;
  }

  /**
   * @see Feature#setSource(java.lang.String)
   */
  public final void setSource(final String pSource) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the Source; FlyMine is read only");
  }

  /**
   * @see Feature#getSymbols()
   */
  public SymbolList getSymbols() {
    Location loc = getTemplate().location;
    return getSequence().subList(loc.getMin(), loc.getMax());
  }

  /**
   * @see Feature#getParent()
   */
  public FeatureHolder getParent() {
    return getSequence();
  }

  /**
   * @see Feature#getSequence()
   */
  public Sequence getSequence() {
    return _seq;
  }

  /**
   * @see Feature#makeTemplate()
   */
  public Feature.Template makeTemplate() {
    return getTemplate();
  }

  // =======================================================================
  // Implements StrandedFeature interface
  // =======================================================================

  /**
   * @see org.biojava.bio.seq.StrandedFeature#getStrand()
   */
  public Strand getStrand() {
    return getTemplate().strand;
  }

  /**
   * @see org.biojava.bio.seq.StrandedFeature#setStrand(org.biojava.bio.seq.StrandedFeature.Strand)
   */
  public void setStrand(final Strand pStrand) throws ChangeVetoException {
    throw new ChangeVetoException("Can't edit the Source; FlyMine is read only");
  }

  // -----------------------------------------------------------------------
  // private helper of Feature implementation
  // -----------------------------------------------------------------------

  /**
   * get the template of this feature. Either it was provided with the constructor or it is queried
   * from the database, in the case the feature was only created with ID
   * 
   * @return Template of this feature
   */
  private StrandedFeature.Template getTemplate() {
    //try to get the persistent template (memory mode)
    if (_templPersist != null) {
      if (Config.LOG) _log.debug("getTemplate(): return templPersist");
      return _templPersist;
    }

    //try to get the template held by a softReferece (non memory mode)
    if (_templSoft == null) {
      _templSoft = new SoftReference(null); // transient
    }
    StrandedFeature.Template template = (StrandedFeature.Template) _templSoft.get();
    if (template != null) {
      if (Config.LOG) _log.debug("getTemplate(): return templSoft.get()");
      return template;
    }

    //get the template directly from the database
    final QueryFM query = QueryFM.getInstance(this._seq);
    final List id = new ArrayList();
    id.add(_bioEntityID);
    template = (StrandedFeature.Template) (query.getTemplate(id).get(_bioEntityID));
    if (template == null) { throw new IllegalArgumentException(
        "Feature was created with an invalid ID: " + _bioEntityID); }
    if (Config.ISMEMORYMODE) {
      _templPersist = template;
    } else {
      _templSoft = new SoftReference(template);
      assert (_templSoft.get() != null);
    }
    if (Config.LOG) _log.debug("getTemplate(): queryFM ~> getTemplate(ID) -> return template");
    return template;
  }

  // =======================================================================
  // Implements FeatureHolder interface
  // =======================================================================

  /**
   * This implementation does not provide hierarchical features!
   * 
   * @see FeatureHolder#countFeatures()
   * @return 0
   */
  public int countFeatures() {
    return 0;
  }

  /**
   * This implementation does not provide hierarchical features!
   * 
   * @see FeatureHolder#features()
   * @return empty Iterator
   */
  public Iterator features() {
    return new ArrayList().iterator();
  }

  /**
   * @see FeatureFM#filter(FeatureFilter, boolean)
   */
  public FeatureHolder filter(final FeatureFilter pFilter, final boolean pRecurse) {
    return FeatureHolder.EMPTY_FEATURE_HOLDER;
  }

  /**
   * @param pFilter
   *        Your FeatureFilter. You can pass any Filter, but as this implementation does not provide
   *        hierarchical features, you alwas get an empty FeatureHolder
   * @return FeatureHolder.EMPTY_FEATURE_HOLDER;
   */
  public FeatureHolder filter(final FeatureFilter pFilter) {
    return FeatureHolder.EMPTY_FEATURE_HOLDER;
  }

  /**
   * This implementation does not provide hierarchical features! Creation of Features not supported
   * 
   * @see FeatureHolder#createFeature(org.biojava.bio.seq.Feature.Template)
   */
  public final Feature createFeature(final Feature.Template pFeature) throws ChangeVetoException {
    throw new ChangeVetoException("Can't create new features; FlyMine is read only");
  }

  /**
   * * This implementation does not provide hierarchical features! Removal of Features not supported
   * 
   * @see FeatureHolder#removeFeature(org.biojava.bio.seq.Feature)
   */
  public void removeFeature(final Feature pFeature) throws ChangeVetoException {
    throw new ChangeVetoException("Can't remove features; FlyMine is read only");
  }

  /**
   * This implementation does not provide hierarchical features, therefore a Feature never contains
   * Features
   * 
   * @param pFeature
   *        any Feature
   * @return false
   */
  public boolean containsFeature(final Feature pFeature) {
    return false;
  }

  /**
   * @see FeatureHolder#getSchema()
   */
  public FeatureFilter getSchema() {
    return new org.biojava.bio.seq.FeatureFilter.ByParent(new FeatureFilter.ByFeature(this));
  }

  // =======================================================================
  // Implements Annotation interface
  // =======================================================================

  /**
   * get the Annotation of this feature
   * 
   * @return the annotation of this feature
   */
  public Annotation getAnnotation() {
    return getTemplate().annotation;
  }

  // =======================================================================
  // Overwrite Object
  // =======================================================================

  /**
   * @see java.lang.Object#equals(Object)
   */
  public boolean equals(final Object pObject) {
    if (pObject == this) { return true; }
    if (!(pObject instanceof FeatureFM)) { return false; }
    final FeatureFM f = (FeatureFM) pObject;
    if (!f._bioEntityID.equals(_bioEntityID)) { return false; }
    return true;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return _bioEntityID.hashCode();
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return new ToStringBuilder(this).append("bioEntityID", _bioEntityID).toString();
  }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * Navigate the sequence ontology light (SOFA) <br>
   * get partOf/hasA elements of this Feature
   * 
   * @return a Collection of type names which a contained by this feature
   */
  public Collection getHasA() {
    return ModelUtilsSoFa.getInstance().hasA(getTemplate().type);
  }

  /**
   * Navigate the sequence ontology light (SOFA) <br>
   * get inverse partOf/hasA elements of this feature
   * 
   * @return a Collection of type names this feature is contained by
   */
  public Collection getInvHasA() {
    return ModelUtilsSoFa.getInstance().invHasA(getTemplate().type);
  }

  /**
   * an extension to toString(). This dumps all relevant FeatureFM information, but as Features can
   * be instanciated lazily, it could lack in performance; so use carefully!
   * 
   * @return a toString representation of all relevant Feature aspects
   */
  public String toStringDumpAll() {
    StringBuffer identifier = new StringBuffer(" identifier=");
    if (getAnnotation().containsProperty("identifier")) {
      identifier.append(getAnnotation().getProperty("identifier"));
    }
    return new ToStringBuilder(this).append("type", getType()).append(identifier).append(
        " bioEntityID", getBioEntityID()).append(" location", getLocation()).append(" strand",
        getTemplate().strand).append("\tannotation", getAnnotation()).append("\tsource",
        getSource()).append(" sequence", _seq).append(" typeTerm", getTypeTerm()).append(
        " sourceTerm", getSourceTerm()).append(" parent", getParent()).append(" schema",
        getSchema()).toString();
  }
}