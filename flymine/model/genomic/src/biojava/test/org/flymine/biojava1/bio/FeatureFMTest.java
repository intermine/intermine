package org.flymine.tests.bio;

import java.util.TreeSet;

import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.ontology.OntoTools;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.utils.Config;
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
public class FeatureFMTest extends FlyMineFixture {

  private SequenceFM _seq;
  private FeatureFM _feature;

  public FeatureFMTest() {
    super();
  }

  public void setUp() throws Exception {
    super.setUp();
    _seq = SequenceFM.getInstance(_C1);
    _feature = FeatureFM.getInstance(_seq, _TRANSCRIPT20ID);
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

  public void testGetInstanceFail() {
    final FeatureFM f = FeatureFM.getInstance(_seq, new Integer(123456));
    try {
      f.getBioEntity();
      fail("should raise IllegalArgumentException as ID does not exist for a BioEntity");
    } catch (IllegalArgumentException e) {}
  }

  public void testGetBioEntityID() {
    assertEquals(_TRANSCRIPT20ID, _feature.getBioEntityID());
  }

  public void testGetBioEntity() {
    _feature.getBioEntity();
    assertEquals(_TRANSCRIPT20, _feature.getBioEntity());
  }

  public void testGetLocation() {
    final Location loc = _feature.getLocation();
    assertEquals(_LOcCHROMOSOME1TRANSCRIPT20.getStart().intValue(), loc.getMin());
    assertEquals(_LOcCHROMOSOME1TRANSCRIPT20.getEnd().intValue(), loc.getMax());
  }

  public void testSetLocation() throws Exception {
    try {
      _feature.setLocation(new RangeLocation(5, 10));
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetType() {
    assertEquals(_feature.getType(), "Transcript");
  }

  public void testSetType() {
    try {
      _feature.setType("foo");
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetTypeTerm() {
    assertEquals(_feature.getTypeTerm(), OntoTools.ANY);
  }

  public void testSetTypeTerm() throws Exception {
    try {
      _feature.setTypeTerm(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetSourceTerm() {
    assertEquals(_feature.getSourceTerm(), OntoTools.ANY);
  }

  public void testSetSourceTerm() throws Exception {
    try {
      _feature.setSourceTerm(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetSource() {
    assertEquals(_feature.getSource(), Config.SOURCE);
  }

  public void testSetSource() {
    try {
      _feature.setSource(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetSymbols() {
    final Location loc = _feature.getLocation();
    final String expected = _feature.getSequence().subStr(loc.getMin(), loc.getMax());
    assertEquals(expected, _feature.getSymbols().seqString());
  }

  public void testGetParent() {
    assertEquals(_seq, _feature.getParent());
  }

  public void testGetSequence() {
    assertEquals(_seq, _feature.getSequence());
  }

  public void testGetStrand() {
    assertEquals(_feature.getStrand(), StrandedFeature.POSITIVE);
  }

  public void testSetStrand() {
    try {
      _feature.setStrand(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
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

  //getAnnotation; only tested in FeatureFMTemplateTest mode, as it is testable there
 
  public void testHasA() {
    final TreeSet expected = new TreeSet();
    expected.add("Exon");
    assertEquals(expected, new TreeSet(_feature.getHasA()));
  }
  
  public void testInvHasA() {
    final TreeSet expected = new TreeSet();
    expected.add("Gene");
    expected.add("TransposableElementGene");
    assertEquals(expected, new TreeSet(_feature.getInvHasA()));
    
  }
*/  
}