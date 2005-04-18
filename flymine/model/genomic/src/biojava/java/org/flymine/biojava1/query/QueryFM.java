package org.flymine.biojava1.query;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.apache.log4j.Logger;
import org.biojava.bio.program.ssbind.AnnotationFactory;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.ontology.OntoTools;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.FeatureHolderFMAnno;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.exceptions.ModelException;
import org.flymine.biojava1.exceptions.ModelExceptionFM;
import org.flymine.biojava1.utils.*;
import org.flymine.model.genomic.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.*;

import java.util.*;

/**
 * This class is used to Query FlyMine; get FeatureFM templates and map some specific BioJava
 * FeatureFilter types to direct FlyMine queries. These specific methods are used by the VisitorFM
 * which traverses through the FeatureFilter tree and map filters to these underlying queries and
 * build one superquery which represents the FeatureFilter.
 * <p>
 * All supported Filters have its own query implementation (BTW: looks as it were a lot of duplicate
 * code, but OQL does not allow me to extract local variables to attributes. And making a private
 * super method with lots parameters ends in complex code which can be avoided. I went for a
 * compromise) <br>
 * ------------------------------------ <br>
 * ok = implemented, ns = not supported <br>
 * ------------------------------------ <br>
 * ok FeatureFilter.And <br>
 * ns FeatureFilter.AnnotationContains <br>
 * ns FeatureFilter.ByAncestor // no hierarchy supported <br>
 * ns FeatureFilter.ByAnnotation <br>
 * ns FeatureFilter.ByAnnotationType <br>
 * ns FeatureFilter.ByChild // no hierarchy supported <br>
 * ok FeatureFilter.ByClass <br>
 * ns FeatureFilter.ByComponentName // Assemblies, e.g. Contigs are simply Features of Sequence <br>
 * ns FeatureFilter.ByDescendant // no hierarchy supported <br>
 * ok FeatureFilter.ByFeature <br>
 * ns FeatureFilter.ByPairwiseScore // No SimilarityPairFeatures <br>
 * ns FeatureFilter.ByParent // no hierarchy supported <br>
 * ns FeatureFilter.BySequenceName // implementation is Chromosome/SequenceFM centric; no need <br>
 * ns FeatureFilter.BySource // all Features have the same source: FlyMine; no need <br>
 * ok FeatureFilter.ByType <br>
 * ok FeatureFilter.ContainedByLocation <br>
 * ns FeatureFilter.FrameFilter // no frames supported by FlyMine/Ensembl; no filter <br>
 * ok FeatureFilter.Not <br>
 * ns FeatureFilter.OnlyChildren // no hierarchy supported <br>
 * ns FeatureFilter.OnlyDescendants // no hierarchy supported <br>
 * ok FeatureFilter.Or <br>
 * ok FeatureFilter.OverlapsLocation <br>
 * ok FeatureFilter.ShadowContainedByLocation <br>
 * ok FeatureFilter.ShadowOverlapsLocation <br>
 * ok FeatureFilter.StrandFilter
 * 
 * @author Markus Brosch
 */
public class QueryFM implements IQueryFM {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * logger
   */
  private static Logger _log = Logger.getLogger(QueryFM.class);

  /**
   * singleton; one instance for each underlying SequenceFM
   */
  private static Map _instances = new HashMap();

  /**
   * the sequence this this QueryFM is supposed to work on
   */
  private final SequenceFM _sequenceFM; //required

  /**
   * the chromosome id this QueryFM is supposed to work on
   */
  private final Integer _chromosomeID;

  /**
   * objectStore used to query FlyMine
   */
  private final ObjectStore _os;

  /**
   * an instance of ModelUtils for the genomic model
   */
  private final ModelUtils _genomicUtils;

  /**
   * an instance of ModelUtils for the sequence ontology light (SOFA) model
   */
  private ModelUtilsSoFa _sofaUtils; //can be NULL !!!

  // -----------------------------------------------------------------------
  // some static constants
  // -----------------------------------------------------------------------

  private static final Class _BEcLASS = BioEntity.class;
  private static final Class _CHROMOSOMEcLASS = Chromosome.class;
  private static final Class _LOCcLASS = Location.class;

  private static final String _BIOENTITY = _BEcLASS.getName();

  //represents some filter types:
  private static final int _CONSTRAINTBYLOCATION = 0;
  private static final int _STRANDFILTER = 1;
  private static final int _OVERLAPSLOCATION = 2;

  // -----------------------------------------------------------------------
  // some private constants which depend on FlyMine -> TODO : config file ???
  // -----------------------------------------------------------------------

  private static final String _OBJECT = "object";
  private static final String _SUBJECT = "subject";
  private static final String _ID = "id";
  private static final String _START = "start";
  private static final String _END = "end";
  private static final String _STRAND = "strand";

  // =======================================================================
  // private constructor & getInstance (Singleton pattern)
  // =======================================================================

  /**
   * Constructor. A QueryFM always acts on 1 (one) specific Chromosome.
   * 
   * @param pSequence
   *        Each QueryFM is working on one specific Chromosome, therefore this parameter passes in
   *        the SequenceFM (represents a Chromosome) of a FlyMine Chromosome
   */
  private QueryFM(final SequenceFM pSequence) {
    if (pSequence == null) throw new NullPointerException("chromsomeID must not be null");

    _sequenceFM = pSequence;
    _chromosomeID = pSequence.getBioEntityID();

    _os = ObjectStoreManager.getInstance().getObjectStore();
    assert (_os != null);

    _genomicUtils = ModelUtils.getInstance((ModelFactory.getGenomicModel()));
    assert (_genomicUtils != null);

    if (Config.LOG) _log.debug("QueryFM instantiated for Chromosome ID " + _chromosomeID);
  }

  /**
   * getInstance method to get an instance of QueryFM for a specific pSequence you want to work on.
   * 
   * @param pSequence
   *        the sequence/chromosome you want to query
   * @return the QueryFM instance
   */
  public static QueryFM getInstance(final SequenceFM pSequence) {
    QueryFM q = (QueryFM) _instances.get(pSequence);
    if (q == null) {
      q = new QueryFM(pSequence);
      _instances.put(pSequence.getBioEntityID(), q);
    }
    return q;
  }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * Method to get StrandedFeature.Templates for each bioEntityID
   * 
   * @param pBioEntityIDs
   *        get all Feature Templates of these BioEntity ID's; <br>
   *        if null, get all Feature Templates of Chromosome!
   * @return Map of the form: <br>
   *         key: Integer ID of BioEntity; value: StrandedFeature.Template
   */
  public Map getTemplate(final Collection pBioEntityIDs) {

    //pBioEntityIDs can be null

    ////// build query:
    ////// Object (Chromosome) <-> Location <-> Subject (any BioEntity)

    final Query q = new Query();
    q.setDistinct(true);

    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    ////Location
    final QueryClass qcLoc = new QueryClass(_LOCcLASS);
    q.addFrom(qcLoc);
    final QueryField start = new QueryField(qcLoc, _START);
    q.addToSelect(start);
    final QueryField end = new QueryField(qcLoc, _END);
    q.addToSelect(end);
    final QueryField strand = new QueryField(qcLoc, _STRAND);
    q.addToSelect(strand);

    ////Object (Chromosome)
    final QueryClass qcObj = new QueryClass(_CHROMOSOMEcLASS);
    q.addFrom(qcObj);
    //constrain location <-> object
    final QueryObjectReference ref1 = new QueryObjectReference(qcLoc, _OBJECT);
    final ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
    cs.addConstraint(cc1);
    //constrain by id
    final QueryField qfObj = new QueryField(qcObj, _ID);
    final QueryValue qv = new QueryValue(_chromosomeID);
    final SimpleConstraint sc = new SimpleConstraint(qfObj, ConstraintOp.EQUALS, qv);
    cs.addConstraint(sc);

    ////Subject (BioEntities)
    final QueryClass qcChild = new QueryClass(_BEcLASS);
    q.addFrom(qcChild);
    q.addToSelect(qcChild);
    //constrain location <-> subject
    final QueryObjectReference ref2 = new QueryObjectReference(qcLoc, _SUBJECT);
    final ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcChild);
    cs.addConstraint(cc2);
    //constrain by id
    if (pBioEntityIDs != null) {
      final QueryField qfSub = new QueryField(qcChild, _ID);
      final BagConstraint bag = new BagConstraint(qfSub, ConstraintOp.IN,
          new TreeSet(pBioEntityIDs));
      cs.addConstraint(bag);
    }

    q.setConstraint(cs);
    if (Config.LOG) _log.info("getTemplate(): query: " + q);
    return getFeatureTemplate(q);
  }

  /**
   * Method to constrain all BioEntities of a given Query to a specific set of IDs
   * 
   * @param pGivenQuery
   *        the given Query to constrain
   * @param pConstrainingIDs
   *        IDs to constrain the BioEntities of the given Query
   * @return new Query which used the old query and constrain this query to the constraining IDs
   */
  public Query constrainQueryByIDs(final Query pGivenQuery, final Collection pConstrainingIDs) {
    if (pGivenQuery == null) throw new NullPointerException("pGivenQuery must not be null");
    if (pConstrainingIDs == null)
        throw new NullPointerException("pConstrainingIDs must not be null");
    if (pConstrainingIDs.size() == 0)
        throw new IllegalArgumentException("pConstrainingIDs are empty");

    final TreeSet ids = new TreeSet(pConstrainingIDs);
    ids.add(_chromosomeID);
    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
    cs.addConstraint(pGivenQuery.getConstraint()); //add old constraint

    try {
      final Set getfrom = pGivenQuery.getFrom();
      for (Iterator it = getfrom.iterator(); it.hasNext();) {
        final QueryClass qcObj = ((QueryClass) it.next());
        final boolean isBioEntity = _genomicUtils.checkSuperClass(_BIOENTITY, qcObj.getType());
        if (isBioEntity) {
          pGivenQuery.addFrom(qcObj);
          final QueryField qfObj = new QueryField(qcObj, _ID);
          final BagConstraint bag = new BagConstraint(qfObj, ConstraintOp.IN, ids);
          cs.addConstraint(bag);
          if (Config.LOG)
              _log.info("constraintQueryByID(): " + pGivenQuery + "\nis now constrained by " + ids);
        }
      }
    } catch (ModelException e) {
      _log.fatal(_BIOENTITY + " does not exist: " + e.getMessage());
      throw new RuntimeException(_BIOENTITY + " does not exist", e); //should never happen
    }
    pGivenQuery.setConstraint(cs);
    return pGivenQuery;
  }

  /**
   * get a BioEntity by it's ID
   * 
   * @param pID
   *        of the BioEntity
   * @return a BioEntity concerning the ID
   */
  public BioEntity getBioEntityByID(final Integer pID) {
    if (pID == null) { throw new NullPointerException("pID must not be null"); }
    try {
      BioEntity be = (BioEntity) _os.getObjectById(pID);
      if (be != null) {
        return be;
      } else {
        throw new IllegalArgumentException("id: " + pID + " is not valid id");
      }
    } catch (ObjectStoreException e) {
      _log.fatal(e);
      throw new RuntimeException(e); //no user error
    } catch (ClassCastException cce) {
      _log.error("id: + " + pID + " is not an ID of a BioEntity: " + cce);
      throw new IllegalArgumentException("id: + " + pID + " is not an ID of a BioEntity");
    }
  }

  /**
   * count how many results are returned by a given Query
   * 
   * @param pGivenQuery
   *        a Query
   * @return the size of ResultsRows for Query q
   */
  public int countResult(final Query pGivenQuery) {
    if (pGivenQuery == null) { throw new NullPointerException("pGivenQuery must not be null"); }
    final Results res = new Results(pGivenQuery, _os, _os.getSequence());
    return res.size();
  }

  /**
   * Get resulting IDs of a given Query. Make sure, the query has only one addToSelect refering to
   * the IDs
   * 
   * @param pGivenQuery
   *        Query with only one (!) "addToSelect" which is the ID of the BioEntity
   * @return all IDs from query
   */
  public Set getIds(final Query pGivenQuery) {
    if (pGivenQuery == null) { throw new NullPointerException("pGivenQuery must not be null"); }
    final Results res = new Results(pGivenQuery, _os, _os.getSequence());
    res.setBatchSize(20000);
    final Set set = new HashSet(new Double(res.size() * 1.3).intValue());
    if (res.size() == 0) { return new HashSet(); }
    for (Iterator it = res.iterator(); it.hasNext();) {
      final ResultsRow row = (ResultsRow) it.next();
      set.add(row.get(0));
    }
    return set;
  }

  /**
   * Helper method for JUnit to test queries
   * 
   * @param pGivenQuery
   *        a given Query
   * @return Results according to the given query.
   */
  public Results testQueries(final Query pGivenQuery) {
    if (pGivenQuery == null) { throw new NullPointerException("pGivenQuery must not be null"); }
    return new Results(pGivenQuery, _os, _os.getSequence());
  }

  /**
   * variable for getChildren (recursion)
   */
  private Set _allChildIDs = null;

  /**
   * method to get hasA/partOf children, according to the sofa model (Sequence Ontology) <br>
   * Example: If we have a pParent of type "Gene" and some IDs of Genes, we get the partOf Features
   * of all these Genes. These could be some Transcripts for example. <br>
   * Not used in the BioJava 1 mapping - for future development
   * 
   * @param pParent
   *        Type of parent
   * @param pParentIDs
   *        IDs related to parent
   * @return IDs of children
   * @throws ModelException
   *         either pParent is not part of model or one of it's children isn't.
   */
  public Set getChildren(final String pParent, final Set pParentIDs) throws ModelException {
    if (pParent == null) { throw new NullPointerException("pParent must not be null"); }
    if (pParentIDs == null) { throw new NullPointerException("pParentIDs must not be null"); }
    if (pParentIDs.size() == 0) { throw new IllegalArgumentException("pParentIDs has no elements"); }
    _allChildIDs = new HashSet();
    return getChildrenHelper(pParent, pParentIDs, false);
  }

  /**
   * get the ancestor/inversePartOf/inverseHasA of a BioEntity; e.g. you HAVE the Transcript and
   * want the inverse has_a relation. Therefore you are looking for a Gene which contains a (hasA)
   * Transcript.
   * 
   * @param pClazz
   *        inverse BioEntity you are looking for (example above: Gene)
   * @param pID
   *        the current BioEntity ID (example above: TranscriptID) of which you want to get ancestor
   * @return a Query representing the ancestor(s)
   */
  public Query getAncestors(final Class pClazz, final Integer pID) {
    if (pClazz == null) throw new NullPointerException("clazz must not be null");
    if (pID == null) throw new NullPointerException("pID must not be null");

    ////// build query:
    ////// Object BioEntity to search <-> Relation <-> Subject known
    final Query q = new Query();
    q.setDistinct(true);

    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    ////Relation
    final QueryClass qcRelation = new QueryClass(Relation.class);
    q.addFrom(qcRelation);

    ////Object
    final QueryClass qcObj = new QueryClass(pClazz);
    q.addFrom(qcObj);
    //constrain by id
    final QueryField qfObj = new QueryField(qcObj, _ID);
    q.addToSelect(qfObj);
    //constrain relation <-> object
    final QueryObjectReference ref1 = new QueryObjectReference(qcRelation, _OBJECT);
    final ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
    cs.addConstraint(cc1);

    ////Subject
    final QueryClass qcSub = new QueryClass(_BEcLASS);
    q.addFrom(qcSub);
    final QueryField qfSub = new QueryField(qcSub, _ID);
    final QueryValue qvSub = new QueryValue(pID);
    final SimpleConstraint sc = new SimpleConstraint(qfSub, ConstraintOp.EQUALS, qvSub);
    cs.addConstraint(sc);
    //constrain relation <-> subject
    final QueryObjectReference ref2 = new QueryObjectReference(qcRelation, _SUBJECT);
    final ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
    cs.addConstraint(cc2);

    q.setConstraint(cs);
    if (Config.LOG) _log.info("byAnchestor(): query: " + q.toString());
    return q;
  }

  // =======================================================================
  // Class specific methods which implement queries for FeatureFilters
  // Used in the non memory mode (filters are mapped to dynamic FlyMine queries)
  // =======================================================================

  /**
   * ok FeatureFilter.And <br>
   * ok FeatureFilter.ByClass <br>
   * ok FeatureFilter.ByFeature <br>
   * ok FeatureFilter.ByType <br>
   * ok FeatureFilter.ContainedByLocation <br>
   * ok FeatureFilter.Not <br>
   * ok FeatureFilter.Or <br>
   * ok FeatureFilter.OverlapsLocation <br>
   * ok FeatureFilter.ShadowContainedByLocation <br>
   * ok FeatureFilter.ShadowOverlapsLocation <br>
   * ok FeatureFilter.StrandFilter
   */

  /**
   * Logical AND operator of two queries.
   * 
   * @param pQuery1
   *        first query
   * @param pQuery2
   *        second query
   * @return Query statement which combines the two child queries by logical AND.
   */
  public Query and(final Query pQuery1, final Query pQuery2) {
    if (pQuery1 == null) throw new NullPointerException("pQuery1 must not null");
    if (pQuery2 == null) throw new NullPointerException("pQuery2 must not null");

    //query
    final Query q = new Query();
    q.setDistinct(true);

    //getting all BioEntities
    final QueryClass qcSub = new QueryClass(_BEcLASS);
    q.addFrom(qcSub);
    final QueryField qfSub = new QueryField(qcSub, _ID);
    q.addToSelect(qfSub);

    //fulfil both subqueries
    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
    final SubqueryConstraint subChild1 = new SubqueryConstraint(qfSub, ConstraintOp.IN, pQuery1);
    cs.addConstraint(subChild1);
    final SubqueryConstraint subChild2 = new SubqueryConstraint(qfSub, ConstraintOp.IN, pQuery2);
    cs.addConstraint(subChild2);
    q.setConstraint(cs);
    if (Config.LOG) _log.info("and(): query: " + q.toString());
    return q;
  }

  /**
   * return Query for all BioEntities of the given clazz type.
   * 
   * @param pClazz
   *        Class for which you want to search (must be a BioEntity class)
   * @return a new Query which queries for all BioEntities of the given Class clazz.
   * @throws ModelException
   *         if clazz is not a BioEntity
   */
  public Query byClass(final Class pClazz) throws ModelException {
    if (pClazz == null) throw new NullPointerException("clazz must not be null");

    //check if it is a BioEntity
    if (!_genomicUtils.checkSuperClass(_BIOENTITY, pClazz)) {
      _log.error(pClazz.getName() + " is not a " + _BIOENTITY);
      throw new ModelExceptionFM(ModelExceptionFM.CLASSNOTABIOENTITY, pClazz.getName()
          + " is not a " + _BIOENTITY);
    }

    ////// build query:
    ////// Object BioEntity <-> Location <-> Subject specific
    final Query q = new Query();
    q.setDistinct(true);

    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    ////Location
    final QueryClass qcLoc = new QueryClass(_LOCcLASS);
    q.addFrom(qcLoc);

    ////Object
    final QueryClass qcObj = new QueryClass(_CHROMOSOMEcLASS);
    q.addFrom(qcObj);
    //constrain by id
    final QueryField qfObj = new QueryField(qcObj, _ID);
    final QueryValue qv = new QueryValue(_chromosomeID);
    final SimpleConstraint sc = new SimpleConstraint(qfObj, ConstraintOp.EQUALS, qv);
    cs.addConstraint(sc);
    //constrain location <-> object
    final QueryObjectReference ref1 = new QueryObjectReference(qcLoc, _OBJECT);
    final ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
    cs.addConstraint(cc1);

    ////Subject
    final QueryClass qcSub = new QueryClass(pClazz);
    q.addFrom(qcSub);
    final QueryField qfSub = new QueryField(qcSub, _ID);
    q.addToSelect(qfSub);
    //constrain location <-> subject
    final QueryObjectReference ref2 = new QueryObjectReference(qcLoc, _SUBJECT);
    final ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
    cs.addConstraint(cc2);

    q.setConstraint(cs);
    if (Config.LOG) _log.info("byClass(): query: " + q.toString());
    return q;
  }

  /**
   * return query for the specified feature
   * 
   * @param pFeature
   *        the given feature
   * @return a new Query which queries for the Feature which was given with pFeature
   */
  public Query byFeature(final Feature pFeature) {
    if (pFeature == null) { throw new NullPointerException("pFeature must not be null"); }

    if (!(pFeature instanceof FeatureFM)) { return new Query(); } //only FeatureFM are used mapping
    final FeatureFM feature = (FeatureFM) pFeature;
    final Integer featureID = feature.getBioEntityID();
    final SequenceFM seq = (SequenceFM) feature.getSequence();
    if (!seq.getBioEntityID().equals(_chromosomeID)) { return new Query(); }

    final Query q = new Query();
    q.setDistinct(true);
    //bioEntity
    final QueryClass qc = new QueryClass(_BEcLASS);
    q.addFrom(qc);
    final QueryField qf = new QueryField(qc, _ID);
    q.addToSelect(qf);
    //constrain to id
    final QueryValue qv = new QueryValue(featureID);
    final SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, qv);
    q.setConstraint(sc);

    if (Config.LOG) _log.info("byFeature(): query: " + q.toString());
    return q;
  }

  /**
   * returns Query for all BioEntities of the given type.
   * 
   * @param pType
   *        Classname (fully qualified or unqualified) to look for. Must be a BioEntity.
   * @return a Query which queries for all BioEntities of the given type/class.
   * @throws ModelException
   *         if pType is not a BioEntity type
   */
  public Query byType(final String pType) throws ModelException {
    if (pType == null) throw new NullPointerException();
    final Class clazz = _genomicUtils.getClassForTypeName(pType);
    final Query q = byClass(clazz);
    if (Config.LOG) _log.info("byType(): query: " + q.toString());
    return q;
  }

  /**
   * returns Query which queries for all BioEntities contained within a location range.
   * 
   * @param pStart
   *        start position
   * @param pEnd
   *        end position
   * @return returns all BioEntities of given range.
   */
  public Query containedByLocation(final Integer pStart, final Integer pEnd) {
    if (pStart == null) throw new NullPointerException("start must not be null");
    if (pEnd == null) throw new NullPointerException("end must not be null");
    final Query q = byLocation(_CONSTRAINTBYLOCATION, pStart, pEnd, null);
    if (Config.LOG) _log.info("containedByLocation(): query: " + q.toString());
    return q;
  }

  /**
   * returns a Query which queries for all BioEntities on a specific strand.
   * 
   * @param pStrand
   *        -1 negative, 0 unknown, 1 positive
   * @return return all BioEntities of specified strand
   */
  public Query strandFilter(final Integer pStrand) {
    if (pStrand == null) throw new NullPointerException("strand must not null");
    int s = pStrand.intValue();
    if (!(s == -1 || s == 0 || s == 1)) { throw new IllegalArgumentException(s
        + " is not a valid strand; must be either 0, -1 or 1"); }

    final Query q = byLocation(_STRANDFILTER, null, null, pStrand);
    if (Config.LOG) _log.info("strandFilter(): query: " + q.toString());
    return q;
  }

  /**
   * Get all BioEntities and NOT elements fulfil query
   * 
   * @param pQuery
   *        a query
   * @return returns all BioEntities which are not in q1.
   */
  public Query not(final Query pQuery) {
    if (pQuery == null) throw new NullPointerException("q1 must not be null");

    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    //query
    final Query q = new Query();
    q.setDistinct(true);

    ////Location
    final QueryClass qcLoc = new QueryClass(_LOCcLASS);
    q.addFrom(qcLoc);

    ////Object
    final QueryClass qcObj = new QueryClass(_CHROMOSOMEcLASS);
    q.addFrom(qcObj);
    //constrain by id
    final QueryField qfObj = new QueryField(qcObj, _ID);
    final QueryValue qv = new QueryValue(_chromosomeID);
    final SimpleConstraint sc = new SimpleConstraint(qfObj, ConstraintOp.EQUALS, qv);
    cs.addConstraint(sc);
    //constrain location <-> object
    final QueryObjectReference ref1 = new QueryObjectReference(qcLoc, _OBJECT);
    final ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
    cs.addConstraint(cc1);

    ////Subject - getting all BioEntities
    final QueryClass qcSub = new QueryClass(_BEcLASS);
    q.addFrom(qcSub);
    final QueryField qfSub = new QueryField(qcSub, _ID);
    q.addToSelect(qfSub);
    //constrain location <-> subject
    final QueryObjectReference ref2 = new QueryObjectReference(qcLoc, _SUBJECT);
    final ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
    cs.addConstraint(cc2);

    //that are not in q1
    final SubqueryConstraint sub = new SubqueryConstraint(qfSub, ConstraintOp.NOT_IN, pQuery);
    cs.addConstraint(sub);
    q.setConstraint(cs);
    if (Config.LOG) _log.info("not(): query: " + q.toString());
    return q;
  }

  /**
   * Logical OR operator for two given Queries. If possible, do not use with two byType or byClass
   * queries or you have to wait for the result quite a while. <b>Use careful and sparingly </b>
   * 
   * @param pQuery1
   *        query1
   * @param pQuery2
   *        query2
   * @return Query statement which combines the two child queries by logical OR.
   */
  public Query or(final Query pQuery1, final Query pQuery2) {
    if (pQuery1 == null) throw new NullPointerException("q1 must not be null");
    if (pQuery2 == null) throw new NullPointerException("q2 must not be null");

    //query that is
    final Query q = new Query();
    q.setDistinct(true);

    //getting all BioEntities
    final QueryClass qcSub = new QueryClass(_BEcLASS);
    q.addFrom(qcSub);
    final QueryField qfSub = new QueryField(qcSub, _ID);
    q.addToSelect(qfSub);

    //or constraint
    final ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
    //query1
    final SubqueryConstraint subChild1 = new SubqueryConstraint(qfSub, ConstraintOp.IN, pQuery1);
    cs.addConstraint(subChild1);
    //query2
    final SubqueryConstraint subChild2 = new SubqueryConstraint(qfSub, ConstraintOp.IN, pQuery2);
    cs.addConstraint(subChild2);

    q.setConstraint(cs);
    if (Config.LOG) _log.info("or(): query: " + q.toString());
    return q;
  }

  /**
   * returns Query which queries for all BioEntities overlapping a location range.
   * 
   * @param pStart
   *        start position
   * @param pEnd
   *        end position
   * @return returns all BioEntities of given overlapped range.
   */
  public Query overlapsLocation(final Integer pStart, final Integer pEnd) {
    if (pStart == null) throw new NullPointerException("start must not be null");
    if (pEnd == null) throw new NullPointerException("end must not be null");

    final Query q = byLocation(_OVERLAPSLOCATION, pStart, pEnd, null);
    if (Config.LOG) _log.info("overlapsLocation(): query: " + q.toString());
    return q;
  }

  /**
   * Same functionality as containedByLocation (we have only contiguous features on chromosome)
   * 
   * @see QueryFM#containedByLocation(Integer, Integer)
   */
  public Query shadowContainedByLocation(final Integer pStart, final Integer pEnd) {
    return containedByLocation(pStart, pEnd);
  }

  /**
   * Same functionality as overlapsLocation (we have only contiguous features on chromosome)
   * 
   * @see QueryFM#overlapsLocation(Integer, Integer)
   */
  public Query shadowOverlapsLocation(final Integer pStart, final Integer pEnd) {
    return overlapsLocation(pStart, pEnd);
  }

  // -----------------------------------------------------------------------
  // private helper
  // -----------------------------------------------------------------------

  /**
   * helper method of getTemplate;
   * 
   * @param pQuery
   *        A query with addToSelect: start, end, strand, id
   * @return Set of IStrandedFeatureFM.Template's
   */
  private Map getFeatureTemplate(final Query pQuery) {

    //// IMPORTANT NOTE for developers ////
    // Do not add _non_ serializable Objects to the templ:FeatureFM.Template
    // RMI can be used with this mapping; all Objects returned in this method must be serializable

    //get all Results of Query q
    final Results res = new Results(pQuery, _os, _os.getSequence());
    res.setBatchSize(25000);

    //store all StrandedFeature.Templates in map: key ID / value Template
    final Map map = new HashMap(new Double(res.size() * 1.3).intValue());

    //Iterate the resultset
    for (final Iterator it = res.iterator(); it.hasNext();) {
      final ResultsRow row = (ResultsRow) it.next();
      final FeatureFM.Template templ = new FeatureFM.Template();

      ////location////
      final int startRes = ((Integer) row.get(0)).intValue();
      final int endRes = ((Integer) row.get(1)).intValue();
      final RangeLocation loc = new RangeLocation(startRes, endRes);
      templ.location = loc;

      ////source////
      templ.source = Config.SOURCE;

      ////sourceTerm////
      templ.sourceTerm = OntoTools.ANY;

      ////type////
      final BioEntity bioEntity = (BioEntity) row.get(3);
      final Integer bioEntityID = bioEntity.getId();
      final Class clazz = bioEntity.getClass();
      templ.type = _genomicUtils.getUnqualifiedClassName(clazz);

      ////typeTerm////
      //not used, as it is not ready yet in BioJava 1.4b
      templ.typeTerm = OntoTools.ANY;

      ////strand////
      final int strandRes = ((Integer) row.get(2)).intValue();
      if (strandRes == 1) {
        templ.strand = StrandedFeature.POSITIVE;
      } else if (strandRes == -1) {
        templ.strand = StrandedFeature.NEGATIVE;
      } else if (strandRes == 0) {
        templ.strand = StrandedFeature.UNKNOWN;
      } else {
        assert (false) : "Strand should be either 0, -1 or 1";
        templ.strand = StrandedFeature.UNKNOWN;
      }
      if (Config.LOG)
          _log.debug(templ.type + " ID:" + bioEntityID
              + " loc, source(Term), type(Term) & strand set");

      ////annotation////
      final Map annotationBundle = new HashMap();

      //identifier
      annotationBundle.put("identifier", bioEntity.getIdentifier());

      //synonyms

      if (Config.SYNONYMS) {
        //proxy collection -> real collection
        //can't use proxy collection as it is not serializable -> make it a real collection
        //developer note: Collections.copy does not work with a proxy collection
        List synonymsProxyCollection = bioEntity.getSynonyms();
        List synonyms = new ArrayList();
        for (Iterator itSynProxCol = synonymsProxyCollection.iterator(); itSynProxCol.hasNext();) {
          Synonym s = (Synonym) itSynProxCol.next();
          synonyms.add(s.getValue());
        }
        annotationBundle.put("synonyms", synonyms);
      }

      if (Config.HASA) {
        if (_sofaUtils == null){
          _sofaUtils = ModelUtilsSoFa.getInstance();
          assert (_sofaUtils != null);
        }
        ///provide has_a/contains relations in annotation bundle:
        //get all has_a of a certain type, e.g. a Gene has_a Transcripts and RegulatoryRegions
        final Collection containsField = _sofaUtils.hasA(templ.type);
        if (containsField != null) {
          for (Iterator iterCF = containsField.iterator(); iterCF.hasNext();) {
            final String containsTypeName = (String) iterCF.next();
            final String containsFieldName = _sofaUtils.typeName2FieldName(containsTypeName);
            if (Config.LOG) _log.debug("Annotation; " + templ.type + " hasA " + containsTypeName);
            final FeatureHolderFMAnno ha = new FeatureHolderFMAnno(_sequenceFM, bioEntity,
                containsTypeName, containsFieldName, false);
            annotationBundle.put("hasA:" + containsTypeName, ha);
          }
        }
      }

      if (Config.INVHASA) {
        if (_sofaUtils == null) {
          _sofaUtils = ModelUtilsSoFa.getInstance();
          assert (_sofaUtils != null);
        }
        ///provide containedBy relations in annotation bundle (inverse of contains):
        //get all containedBy of a certain type, e.g. a Gene has_a Transcripts and
        // RegulatoryRegions
        final Collection containedByField = _sofaUtils.invHasA(templ.type);
        if (containedByField != null) {
          for (Iterator iterCBF = containedByField.iterator(); iterCBF.hasNext();) {
            final String containedByTypeName = (String) iterCBF.next();
            final String containedByFieldName = _sofaUtils.typeName2FieldName(containedByTypeName);
            if (Config.LOG)
                _log.debug("Annotation; " + templ.type + " invHasA " + containedByTypeName);

            final FeatureHolderFMAnno ha = new FeatureHolderFMAnno(_sequenceFM, bioEntity,
                containedByTypeName, containedByFieldName, true);
            annotationBundle.put("invHasA:" + containedByTypeName, ha);
          }
        }
      }

      templ.annotation = AnnotationFactory.makeAnnotation(annotationBundle);
      //templ.annotation = org.biojava.bio.Annotation.EMPTY_ANNOTATION;

      if (Config.LOG) _log.debug("getFeatureTemplate() for " + bioEntity.getId() + " done");
      map.put(bioEntityID, templ);
    }
    if (Config.LOG) _log.info("getFeatureTemplate() done");
    return map;
  }

  /**
   * private helper method
   * 
   * @see QueryFM#getChildren(String, Set) Additional a pRecurse flag to get all children and
   *      subchildren - not used currently;
   */
  private Set getChildrenHelper(final String pParent, final Set pParentIDs, final boolean pRecurse)
      throws ModelException {
    if (_sofaUtils == null) {
      _sofaUtils = ModelUtilsSoFa.getInstance();
      assert (_sofaUtils != null);
    }
    //get direct children of parent and process each of them
    final Collection partOfFields = _sofaUtils.hasA(pParent);
    if (partOfFields == null) { throw new ModelException(ModelException.CLASSNOTINMODEL, pParent
        + " does not exist in model! Make sure you used the unqualified class name!"); }
    for (Iterator it = partOfFields.iterator(); it.hasNext();) {
      final String childTypeName = (String) it.next();
      final String childFieldName = _sofaUtils.typeName2FieldName(childTypeName);
      if (Config.LOG) _log.info("\tchild: " + childTypeName);
      //get ids according to parents of type parent and specific child
      final Set childIDs = query4Children(pParent, pParentIDs, childTypeName, childFieldName);
      _allChildIDs.addAll(childIDs);
      if (pRecurse) {
        getChildrenHelper(childTypeName, childIDs, pRecurse);
      }
    }
    if (Config.LOG) _log.info("getChildrenHelper(): for pParent: " + pParent);
    return _allChildIDs;
  }

  /**
   * private helper method
   * 
   * @see QueryFM#getChildrenHelper(String, Set, boolean)
   */
  private Set query4Children(final String pParent, final Set pParentIDs,
      final String pChildTypeName, final String pChildField) throws ModelException {

    //// Object (parent) according to parentIDs
    //// -> get Collection of a field (childField)

    final Query q = new Query();
    q.setDistinct(true);

    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    //parent
    final Class parentClass = _genomicUtils.getClassForTypeName(pParent);
    final QueryClass qcParent = new QueryClass(parentClass);
    q.addFrom(qcParent);
    //constrain to relevant ids of parents
    final QueryField qfParent = new QueryField(qcParent, _ID);
    final BagConstraint bag = new BagConstraint(qfParent, ConstraintOp.IN, pParentIDs);
    cs.addConstraint(bag);

    //child
    //String className = pChildField.substring(1, pChildField.length() - 1);
    //className = (new Character(pChildField.charAt(0))).toString().toUpperCase() + className;
    final Class childClass = _genomicUtils.getClassForTypeName(pChildTypeName);
    final QueryClass qcChild = new QueryClass(childClass);
    q.addFrom(qcChild);
    final QueryField qfChild = new QueryField(qcChild, _ID);
    q.addToSelect(qfChild);
    //constrain to childField
    final QueryCollectionReference ref = new QueryCollectionReference(qcParent, pChildField);
    final ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcChild);
    cs.addConstraint(cc);

    q.setConstraint(cs);
    if (Config.LOG) _log.info("query4Children(): query: " + q.toString());
    return this.getIds(q);
  }

  /**
   * private helper for any queries which are related to locations or strand
   * 
   * @param pFilterType
   *        any of the specified final types related to the filters
   * @param pStart
   *        start position
   * @param pEnd
   *        end position
   * @param pStrand
   *        strand
   * @return query
   */
  private Query byLocation(final int pFilterType, final Integer pStart, final Integer pEnd,
      final Integer pStrand) {

    ////// build query:
    ////// Object BioEntity <-> Location specific location <-> Subject

    final Query q = new Query();
    q.setDistinct(true);

    final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    ////Location
    final QueryClass qcLoc = new QueryClass(_LOCcLASS);
    q.addFrom(qcLoc);

    switch (pFilterType) {
      case _CONSTRAINTBYLOCATION:
        //Start
        final QueryField qfStart = new QueryField(qcLoc, _START);
        final QueryValue qvStart = new QueryValue(pStart);
        final SimpleConstraint scStart = new SimpleConstraint(qfStart,
            ConstraintOp.GREATER_THAN_EQUALS, qvStart);
        cs.addConstraint(scStart);
        //End
        final QueryField qfEnd = new QueryField(qcLoc, _END);
        final QueryValue qvEnd = new QueryValue(pEnd);
        final SimpleConstraint scEnd = new SimpleConstraint(qfEnd, ConstraintOp.LESS_THAN_EQUALS,
            qvEnd);
        cs.addConstraint(scEnd);
        break;
      case _STRANDFILTER:
        final QueryField qfLoc = new QueryField(qcLoc, _STRAND);
        final QueryValue qvStrand = new QueryValue(pStrand);
        final SimpleConstraint scStrand = new SimpleConstraint(qfLoc, ConstraintOp.EQUALS, qvStrand);
        cs.addConstraint(scStrand);
        break;
      case _OVERLAPSLOCATION:
        /**
         * JavaDoc to keep formatting! <br>
         * <code>
         * .            Variant 1           Variant 2             Variant3         Variante4
         * Sequence:  oSL ----- oEL       oSL ----- oEL        oSL ----- oEL     oSL ----- oEL 
         * Feature :        oS----oE   oS--------------oE    oS----oE              oS----oE 
         * .
         * Constraint: oEL >= oS && oSL <= oE              
         * </code>
         */
        //Startlocation
        final QueryField oSL = new QueryField(qcLoc, _START);
        final QueryValue oS = new QueryValue(pStart);
        //Endlocation
        final QueryField oEL = new QueryField(qcLoc, _END);
        final QueryValue oE = new QueryValue(pEnd);
        //constraints
        final SimpleConstraint sc1 = new SimpleConstraint(oEL, ConstraintOp.GREATER_THAN_EQUALS, oS);
        final SimpleConstraint sc2 = new SimpleConstraint(oSL, ConstraintOp.LESS_THAN_EQUALS, oE);
        cs.addConstraint(sc1);
        cs.addConstraint(sc2);
        break;
      default:
        assert false : "fix parameter to be valid; currently pFilterType: " + pFilterType;
    }

    ////Object
    final QueryClass qcObj = new QueryClass(_CHROMOSOMEcLASS);
    q.addFrom(qcObj);
    final QueryField qfObj = new QueryField(qcObj, _ID);
    //constrain by id
    final QueryValue qv = new QueryValue(_chromosomeID);
    final SimpleConstraint sc = new SimpleConstraint(qfObj, ConstraintOp.EQUALS, qv);
    cs.addConstraint(sc);
    //constrain by objct <-> location
    final QueryObjectReference ref1 = new QueryObjectReference(qcLoc, _OBJECT);
    final ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
    cs.addConstraint(cc1);

    ////Subject
    final QueryClass qcSub = new QueryClass(_BEcLASS);
    q.addFrom(qcSub);
    final QueryField qfSub = new QueryField(qcSub, _ID);
    q.addToSelect(qfSub);
    //constrain by location <-> subject
    final QueryObjectReference ref2 = new QueryObjectReference(qcLoc, _SUBJECT);
    final ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
    cs.addConstraint(cc2);

    //Set constraint
    q.setConstraint(cs);
    if (Config.LOG) _log.info("containedByLocation(): query: " + q.toString());
    return q;
  }

}