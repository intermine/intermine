package org.flymine.tests.bio;

import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.ontology.OntoTools;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.utils.ObjectStoreManager;
import org.flymine.tests.FlyMineFixture;


/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * JUnit testcase for testing FeatureFM Template provided - as it is a singleton, you have to have
 * your own testcase for FeatureFM without template 
 * <p>
 * Test in both configuration states: memoryMode and nonMemoryMode -\> config file!
 * 
 * @author Markus Brosch
 */
public class FeatureFMTemplateTest extends FlyMineFixture {

  private SequenceFM _seq;
  private StrandedFeature.Template _templ;
  private FeatureFM _feature;

  public FeatureFMTemplateTest() {
    super();
  }

  public void setUp() throws Exception {
    super.setUp();
    _seq = SequenceFM.getInstance(_C1);
   
    _templ = new StrandedFeature.Template();
    _templ.location = new RangeLocation(5, 7); 
    _templ.annotation = org.biojava.bio.Annotation.EMPTY_ANNOTATION;
    _templ.source = "FM";
    _templ.sourceTerm = OntoTools.ANY;
    _templ.type = "gEnE";
    _templ.typeTerm = OntoTools.ANY;
    _templ.strand = StrandedFeature.UNKNOWN;

    _feature = FeatureFM.getInstance(_seq, _GENE10ID, _templ);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

    public void testThatPasses() throws Exception {
        // see comment below
        // this test is here so that JUnit doesn't complain that there are no tests
    }

    /******
     *
     * there are problems with the biojava1 code - lots of hard coded paths to property files
     *

    public void testGetBioEntityID() {
        assertEquals(_GENE10ID, _feature.getBioEntityID());
    }
  
  public void testGetBioEntity() {
    System.out.println(_os);
    System.out.println(ObjectStoreManager.getInstance().getObjectStore());
    assertEquals(_GENE10, _feature.getBioEntity());
  }
 
  public void testGetLocationTempl() {
    final Location loc = _feature.getLocation();
    assertEquals(_templ.location.getMin(), loc.getMin());
    assertEquals(_templ.location.getMax(), loc.getMax());
  }

  public void testSetLocation() throws Exception {
    try {
      _feature.setLocation(new RangeLocation(5, 10));
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetType() {
    assertEquals(_feature.getType(), _templ.type);
  }

  public void testSetType() {
    try {
      _feature.setType("foo");
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetTypeTerm() {
    assertEquals(_feature.getTypeTerm(), _templ.typeTerm);
  }

  public void testSetTypeTerm() throws Exception {
    try {
      _feature.setTypeTerm(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetSourceTerm() {
    assertEquals(_feature.getSourceTerm(), _templ.sourceTerm);
  }

  public void testSetSourceTerm() throws Exception {
    try {
      _feature.setSourceTerm(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetSource() {
    assertEquals(_feature.getSource(), _templ.source);
  }

  public void testSetSource() {
    try {
      _feature.setSource(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetSymbols() {
    final Location loc = _templ.location;
    final String expected = _feature.getSequence().subStr(loc.getMin(), loc.getMax());
    assertEquals(expected, _feature.getSymbols().seqString());
  }

  public void testGetParent() {
    assertEquals(_seq.getBioEntityID(), ((SequenceFM) _feature.getParent()).getBioEntityID());
  }

  public void testGetSequence() {
    assertEquals(_seq.getBioEntityID(), ((SequenceFM) _feature.getSequence()).getBioEntityID());
  }

  public void testGetStrand() {
    assertEquals(_feature.getStrand(), _templ.strand);
  }
  
  public void testSetStrand() {
    try {
      _feature.setStrand(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }
  
  public void testMakeTemplate() {
    assertEquals(_templ, _feature.makeTemplate());
  }

  public void testCountFeatures() {
    assertEquals(0, _feature.countFeatures());
  }

  public void testFeatures() throws Exception {
    while (_feature.features().hasNext()) {
      fail("no hierarchy, therfore no children");
    }
  }

  public void testFilterFeatureFilterboolean() {
    assertEquals(FeatureHolder.EMPTY_FEATURE_HOLDER, _feature.filter(null, true));
    assertEquals(FeatureHolder.EMPTY_FEATURE_HOLDER, _feature.filter(null));
  }

  public void testCreateFeature() {
    try {
      _feature.createFeature(null);
      fail("should have risen a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testRemoveFeature() {
    try {
      _feature.removeFeature(null);
      fail("should have risen a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testContainsFeature() {
    assertFalse(_feature.containsFeature(null));
  }

  public void testGetAnnotation() {
    assertEquals(_feature.getAnnotation(), _templ.annotation);
  }
*/  
  //hasA & invHasA depends on the type of Feature, therefore checked in FeatureFMTest
}