package org.flymine.tests.bio;

import java.util.Iterator;
import java.util.TreeSet;

import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.FeatureHolderFMAnno;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.tests.FlyMineFixture;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * @author Markus Brosch
 */
public class FeatureHolderFMAnnoTest extends FlyMineFixture {

  SequenceFM _seq;
  
  /**
   * FeatureHolder Annotation for a Gene to get/describe the partOf relation for a Transcript 
   */
  FeatureHolderFMAnno _geneTranscripts;
  
  /**
   * FeatureHolder Annotation for a Transcript to get the inverse partOf relation to a Gene; The 
   * only Relation made by FlyMineFixture is between Gene-Transcript! So do not test inverse
   * partOf relations between other Feature types.
   */
  FeatureHolderFMAnno _transcriptGene;
  
  public void setUp() throws Exception {
    super.setUp();
    _seq = SequenceFM.getInstance(_C1);
    _geneTranscripts = new FeatureHolderFMAnno(_seq, _GENE10, "Transcript", "transcripts", false);
    _transcriptGene = new FeatureHolderFMAnno(_seq, _TRANSCRIPT20, "Gene", "genes", true); 
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCountFeatures() {
    assertEquals(2, _geneTranscripts.countFeatures());
    assertEquals(1, _transcriptGene.countFeatures());
  }
  
  public void testFeatures() {
    //this test is only valid, if testCountFeatures() was passed
    //this test implicit tests getFeatures()
    final TreeSet expected = new TreeSet();
    FeatureFM f = FeatureFM.getInstance(_seq, _TRANSCRIPT20ID);
    expected.add(f.getBioEntityID());
    FeatureFM f2 = FeatureFM.getInstance(_seq, _TRANSCRIPT21ID);
    expected.add(f2.getBioEntityID());
    
    final TreeSet actual = new TreeSet();
    for (Iterator it = _geneTranscripts.features(); it.hasNext();) {
      actual.add(((FeatureFM)it.next()).getBioEntityID());      
    }
    
    assertEquals(expected, actual);
  }

  public void testFilterFeatureFilter() {
    final FeatureFilter gene = new FeatureFilter.ByType("Gene");
    FeatureHolder fh = _transcriptGene.filter(gene);
    for (Iterator it = fh.features(); it.hasNext(); ) {
      final FeatureFM f = (FeatureFM)it.next();
      assertEquals(_GENE10, f.getBioEntity());
    }  
    
    FeatureFilter exon = new FeatureFilter.ByType("Exon");
    fh = _transcriptGene.filter(exon);
    assertEquals(0, fh.countFeatures());    
  }
}