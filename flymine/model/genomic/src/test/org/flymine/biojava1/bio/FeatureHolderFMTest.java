package org.flymine.tests.bio;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.FeatureHolderFM;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.tests.FlyMineFixture;


/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * JUnit testcase fore testing FeatureHolderFM
 * <p>
 * Test in both configuration states: memoryMode and nonMemoryMode -\> config file!
 * 
 * @author Markus Brosch
 */
public class FeatureHolderFMTest extends FlyMineFixture {

  private SequenceFM _seq;
  private FeatureHolderFM _fhAllFeatures;
  private FeatureHolderFM _fhExon50Transcript20;

  private Set _fhPartialElements;

  public FeatureHolderFMTest() {
    super();
  }

  public void setUp() throws Exception {
    super.setUp();

    //representing all features of C1
    _seq = SequenceFM.getInstance(_C1);
    _fhAllFeatures = new FeatureHolderFM(_seq);

    //representing Exon50 and Transcript20
    _fhPartialElements = new HashSet();
    _fhPartialElements.add(_EXON50ID);
    _fhPartialElements.add(_TRANSCRIPT20ID);
    _fhExon50Transcript20 = new FeatureHolderFM(_seq, _fhPartialElements);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCountFeatures() {
    assertEquals(_C1BioEntities.size(), _fhAllFeatures.countFeatures());
    assertEquals(_fhPartialElements.size(), _fhExon50Transcript20.countFeatures());
  }

  public void testFeatures1() {
    //testCountFeatures() must be successfull to ensure this test works as expected
    //full
    for (Iterator it = _fhAllFeatures.features(); it.hasNext();) {
      final FeatureFM f = (FeatureFM) it.next();
      if (!_C1BioEntities.contains(f.getBioEntity())) {
        fail(f.getBioEntity() + " not contained in C1BioEntities");
      }
    }
  }

  public void testFeatures2() {
    //partial
    _fhExon50Transcript20.features();
    for (Iterator it = _fhExon50Transcript20.features(); it.hasNext();) {
      final FeatureFM f = (FeatureFM) it.next();
      if (!_fhPartialElements.contains(f.getBioEntity().getId())) {
        fail(f.getBioEntity().toString());
      }
    }
  }

  public void testFilterFeatureFilter() {
    //simple filter
    final FeatureFilter geneFilter = new FeatureFilter.ByType("Gene");

    final FeatureHolder fh = _fhAllFeatures.filter(geneFilter);
    for (Iterator it = fh.features(); it.hasNext();) {
      assertEquals(_GENE10, ((FeatureFM) it.next()).getBioEntity());
    }
  }

  public void testFilterFeatureFilterKomplex() {
    //combined filters
    final Location loc = new RangeLocation(7, 13);
    final FeatureFilter locationFilter = new FeatureFilter.ContainedByLocation(loc);
    final FeatureFilter exonFilter = new FeatureFilter.ByType("Exon");
    final FeatureFilter geneANDLocation = new FeatureFilter.And(exonFilter, locationFilter);
    final FeatureHolder fh = _fhAllFeatures.filter(geneANDLocation);
    for (Iterator it = fh.features(); it.hasNext();) {
      assertEquals(_EXON50, ((FeatureFM) it.next()).getBioEntity());
    }
  }

  public void testCreateFeature() throws BioException {
    try {
      _fhAllFeatures.createFeature(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testRemoveFeature() throws BioException {
    try {
      _fhAllFeatures.removeFeature(null);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testContainsFeaturesMem() {
    //_seq.features(); //to have real features available in ID2FeatureFM
    final FeatureFM fExist = FeatureFM.getInstance(_seq, _GENE10ID);
    final FeatureFM fNotExist = FeatureFM.getInstance(_seq, _CSE1ID);
    assertTrue(_fhAllFeatures.containsFeature(fExist));
    assertFalse(_fhAllFeatures.containsFeature(fNotExist));
  }
}