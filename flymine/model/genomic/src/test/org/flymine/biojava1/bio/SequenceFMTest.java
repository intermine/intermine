package org.flymine.tests.bio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biojava.bio.Annotation;
import org.biojava.bio.BioException;
import org.biojava.bio.program.ssbind.AnnotationFactory;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.AlphabetManager;
import org.biojava.bio.symbol.Edit;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.exceptions.ModelExceptionFM;
import org.flymine.tests.FlyMineFixture;


/**
 * JUnit testcase for SequenceFM
 * <p>
 * Test in both configuration states: memoryMode and nonMemoryMode -\> config file!
 * 
 * @author Markus Brosch
 */
public class SequenceFMTest extends FlyMineFixture {

  private SequenceFM _seq;
  private String _res;

  public void setUp() throws Exception {
    super.setUp();
    _seq = SequenceFM.getInstance(_C1);
    _res = _C1.getSequence().getResidues();
  }

    public void testThatPasses() throws Exception {
        // see comment below
        // this test is here so that JUnit doesn't complain that there are no tests
    }

    /******
     *
     * there are problems with the biojava1 code - lots of hard coded paths to property files
     *
  public void testGetAlphabet() {
    Alphabet expected = AlphabetManager.alphabetForName("DNA");
    assertEquals(expected, _seq.getAlphabet());
  }

  public void testIterator() throws IllegalSymbolException {
    final Iterator expected = DNATools.createDNA(_res).iterator();
    final List expectedList = new ArrayList();
    while (expected.hasNext()) {
      expectedList.add(expected.next());
    }
    final Iterator actual = _seq.iterator();
    final List actualList = new ArrayList();
    while (actual.hasNext()) {
      actualList.add(actual.next());
    }
    assertEquals(expectedList, actualList);
  }

  public void testLength() {
    final int expected = _C1.getLength().intValue();
    assertEquals(expected, _seq.length());
  }

  public void testSeqString() {
    assertEquals(_res, _seq.seqString());
  }

  public void testSubStr() {
    final String expected = _res.substring(0, 6);
    assertEquals(expected, _seq.subStr(1, 6));
  }

  public void testSubList() throws IllegalSymbolException {
    final String substr = _res.substring(0, 6);
    final SymbolList expected = DNATools.createDNA(substr);
    assertEquals(expected, _seq.subList(1, 6));
  }

  public void testSymbolAt() {
    assertEquals("guanine", _seq.symbolAt(1).getName());
  }

  public void testToList() throws IllegalSymbolException {
    final SymbolList sl = DNATools.createDNA(_res);
    final List expected = sl.toList();
    assertEquals(expected, _seq.toList());
  }

  public void testEdit() throws IllegalSymbolException {
    try {
      Edit edit = new Edit(1, 1, DNATools.createDNA("a"));
      _seq.edit(edit);
      fail("should raise a ChangeVetoException");
    } catch (ChangeVetoException e) {}
  }

  public void testGetBioEntityID() {
    assertEquals(_C1ID, _seq.getBioEntityID());
  }

  public void testCountFeatures() {
    //general test of featureHolder
    assertEquals(_C1BioEntities.size(), _seq.countFeatures());
  }

  // Refer to FeatureHolderFMTest:
  // - public void testFeatures()
  // - public void testFilter()
  // - public void testContainsFeature()

  public void testCreateFeature() throws BioException, ChangeVetoException {
    try {
      final Feature.Template templ = new Feature.Template();
      _seq.createFeature(templ);
      fail("should raise an UnsupportedOperationException");
    } catch (ChangeVetoException e) {}
  }

  public void testRemoveFeature() throws ChangeVetoException, BioException {
    try {
      for (Iterator it = _seq.features(); it.hasNext();) {
        _seq.removeFeature((FeatureFM) it.next());
      }
      fail("should raise an UnsupportedOperationException");
    } catch (ChangeVetoException e) {}
  }
*/
}