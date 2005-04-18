package org.flymine.tests.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.ontology.OntoTools;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.exceptions.ModelException;
import org.flymine.biojava1.exceptions.ModelExceptionFM;
import org.flymine.biojava1.query.QueryFM;
import org.flymine.biojava1.utils.Config;
import org.flymine.biojava1.utils.ModelFactory;
import org.flymine.biojava1.utils.ModelUtils;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Gene;
import org.flymine.tests.FlyMineFixture;
import org.intermine.objectstore.query.Query;

/**
 * JUnit TestCase for QueryFM
 * 
 * @author Markus Brosch
 */
public class QueryFMTest extends FlyMineFixture {

  protected ModelUtils _genomicUtils;
  private SequenceFM _seqC1;
  private SequenceFM _seqC2;

  private QueryFM _qfm;
  private QueryFM _qfm2;

  public void setUp() throws Exception {
    super.setUp();
    _genomicUtils = ModelUtils.getInstance(ModelFactory.getGenomicModel());
    _seqC1 = SequenceFM.getInstance(_C1);
    _seqC2 = SequenceFM.getInstance(_C2);
    _qfm = QueryFM.getInstance(_seqC1);
    _qfm2 = QueryFM.getInstance(_seqC2);
  }

  public void testGetTemplate() {
    Collection ids = new ArrayList();
    ids.add(_GENE10ID);
    ids.add(_EXON50ID);
    ids = Collections.unmodifiableCollection(ids);
    final Map map = _qfm.getTemplate(ids);
    assertEquals(ids.size(), map.size());
    final StrandedFeature.Template templ = (StrandedFeature.Template) map.get(_GENE10ID);
    final Location loc = new RangeLocation(_LOcCHROMOSOME1GENE10.getStart().intValue(),
        _LOcCHROMOSOME1GENE10.getEnd().intValue());
    assertEquals(loc, templ.location);
    assertEquals(Config.SOURCE, templ.source);
    assertEquals(OntoTools.ANY, templ.sourceTerm);
    assertEquals(StrandedFeature.POSITIVE, templ.strand);
    assertEquals(_GENEsTRING, templ.type);
    assertEquals(OntoTools.ANY, templ.typeTerm);
    //Annotation not tested, as result depends on config file configuration
  }

  public void testConstrainQueryByIDs() throws ModelException {
    Set expectedIDs = new TreeSet();
    expectedIDs.add(_EXON51ID);
    expectedIDs.add(_EXON50ID);
    expectedIDs = Collections.unmodifiableSet(expectedIDs);
    Query q = _qfm.byType(_EXONsTRING);
    q = _qfm.constrainQueryByIDs(q, expectedIDs);
    final TreeSet idsActual = new TreeSet(_qfm.getIds(q));
    assertEquals(expectedIDs, idsActual);
  }

  public void testGetBioEntityByID() {
    assertEquals(_GENE10, _qfm.getBioEntityByID(_GENE10ID));
    try {
      assertEquals(null, _qfm.getBioEntityByID(new Integer(12345)));
      fail("should raise an IllegalArgumentException, as 12345 does not exist as an ID");
    } catch (IllegalArgumentException e) {}
  }

  public void testCountResult() throws ModelException {
    final Query q = _qfm.byClass(BioEntity.class);
    assertEquals(_C1BioEntities.size(), _qfm.countResult(q));
  }

  //public void testGetIds() //indirectly tested by other test methods

  public void testGetChildren() throws ModelException {
    //test exception
    Set expectedTranscriptIDs = new TreeSet();
    expectedTranscriptIDs.add(_TRANSCRIPT20ID);
    expectedTranscriptIDs.add(_TRANSCRIPT21ID);
    expectedTranscriptIDs = Collections.unmodifiableSet(expectedTranscriptIDs);
    try {
      _qfm.getChildren("Foo", expectedTranscriptIDs);
      fail("should have risen a ModelException as Foo is not a type of the model");
    } catch (ModelException e) {}

    //test1 - gene -> transcripts
    Set geneIds = new HashSet();
    geneIds.add(_GENE10ID);
    Set idsActual = _qfm.getChildren(_GENEsTRING, geneIds);
    assertEquals(expectedTranscriptIDs, new TreeSet(idsActual));

    //test2 - transcript -> exons
    idsActual = _qfm.getChildren(_TRANSCRIPTsTRING, expectedTranscriptIDs);
    Set exons = new HashSet(_TRANSCRIPT20.getExons());
    exons.addAll(_TRANSCRIPT21.getExons());
    assertEquals(exons.size(), idsActual.size());
    for (Iterator it = exons.iterator(); it.hasNext();) {
      BioEntity b = (BioEntity) it.next();
      assertTrue(idsActual.contains(b.getId()));
    }
  }

  public void testGetAncestors() {
    Set expected = new TreeSet();
    expected.add(_GENE10ID);
    Query q = _qfm.getAncestors(Gene.class, _TRANSCRIPT20ID);
    assertEquals(expected, new TreeSet(_qfm.getIds(q)));
    //don't test other ancestors. To test them, you have to ensure the RELATIONS are set
    //between those entities. In FlyMineTextFixture relation is only def. between Gene-Transcript!
  }

  public void testByClass() throws ModelException {
    //exception test
    try {
      _qfm.byClass(Integer.class);
      fail("should have risen a ModelException as Integer is not part of model");
    } catch (ModelException e) {}
    //test
    Set expected = new TreeSet();
    expected.add(_GENE10ID);
    final Query q = _qfm.byClass(Gene.class);
    assertEquals(expected, new TreeSet(_qfm.getIds(q)));
  }

  public void testByFeature() throws ModelExceptionFM {
    final FeatureFM f = FeatureFM.getInstance(_seqC1, _GENE10ID);

    Set expected = new TreeSet();
    expected.add(_GENE10ID);

    final Query q = _qfm.byFeature(f);
    assertEquals(expected, _qfm.getIds(q));
  }

  public void testByType() throws ModelException {
    //exception test
    try {
      _qfm.byType("Integer");
      fail("should have risen a ModelException as Integer is not part of model");
    } catch (ModelException e) {}
    //test
    Set expected = new TreeSet();
    expected.add(_GENE10ID);
    final Query q = _qfm.byType(_GENEsTRING);
    assertEquals(expected, new TreeSet(_qfm.getIds(q)));
  }

  public void testContainedByLocation() {
    Set expectedIDs = new TreeSet();
    expectedIDs.add(_EXON50ID);
    expectedIDs.add(_EXON51ID);
    expectedIDs.add(_EXON53ID);
    expectedIDs = Collections.unmodifiableSet(expectedIDs);

    final Query q = _qfm.containedByLocation(new Integer(0), new Integer(15));
    Set actualIDs = _qfm.getIds(q);
    assertEquals(expectedIDs, new TreeSet(actualIDs));
  }

  public void testStrandFilter() {
    Set expectedIDs = new TreeSet();
    Query q = _qfm.strandFilter(_NEGATIVE);
    assertEquals(expectedIDs, _qfm.getIds(q));

    q = _qfm2.strandFilter(_UNKNOWN);
    expectedIDs.add(_CSE2ID);
    assertEquals(expectedIDs, new TreeSet(_qfm2.getIds(q)));

    q = _qfm2.strandFilter(_NEGATIVE);
    expectedIDs.remove(_CSE2ID);
    expectedIDs.add(_CSE1ID);
    assertEquals(expectedIDs, new TreeSet(_qfm2.getIds(q)));
  }

  public void testOverlappingLocation() {
    //test1
    Set expectedIDs = new TreeSet();
    expectedIDs.add(_GENE10ID);
    expectedIDs.add(_TRANSCRIPT20ID);
    expectedIDs.add(_TRANSCRIPT21ID);
    expectedIDs.add(_EXON50ID);
    expectedIDs.add(_EXON51ID);
    expectedIDs.add(_EXON52ID);
    expectedIDs.add(_EXON53ID);
    expectedIDs = Collections.unmodifiableSet(expectedIDs);
    Query q = _qfm.overlapsLocation(new Integer(10), new Integer(15));
    assertEquals(expectedIDs, new TreeSet(_qfm.getIds(q)));

    //test2
    expectedIDs = new TreeSet();
    q = _qfm.overlapsLocation(new Integer(1), new Integer(2));
    assertEquals(expectedIDs, new TreeSet(_qfm.getIds(q)));
  }

  public void testAnd() throws ModelException {
    Set expected = new TreeSet();
    expected.add(_EXON51ID);
    final Query q1 = _qfm.byType(_EXONsTRING);
    final Query q2 = _qfm.containedByLocation(new Integer(3), new Integer(10));
    final Query q1q2 = _qfm.and(q1, q2);
    Set idsActual = _qfm.getIds(q1q2);
    assertEquals(expected, new TreeSet(_qfm.getIds(q1q2)));
  }

  public void testNot() throws ModelException {
    Set expectedIDs = new TreeSet();
    expectedIDs.add(_GENE10ID);
    expectedIDs.add(_TRANSCRIPT20ID);
    expectedIDs.add(_TRANSCRIPT21ID);
    expectedIDs = Collections.unmodifiableSet(expectedIDs);

    final Query q1 = _qfm.not(_qfm.byType(_EXONsTRING));
    assertEquals(expectedIDs, new TreeSet(_qfm.getIds(q1)));
  }

  public void testOr() {
    Set expectedIDs = new TreeSet();
    expectedIDs.add(_EXON51ID);
    expectedIDs.add(_EXON52ID);
    expectedIDs = Collections.unmodifiableSet(expectedIDs);

    final Query q1 = _qfm.containedByLocation(new Integer(0), new Integer(10));
    final Query q2 = _qfm.containedByLocation(new Integer(15), new Integer(20));
    final Query q1q2 = _qfm.or(q1, q2);

    assertEquals(expectedIDs, new TreeSet(_qfm.getIds(q1q2)));
  }
}