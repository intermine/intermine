package org.flymine.biojava1.server;

import org.biojava.utils.cache.CacheMap;
import org.biojava.utils.cache.WeakCacheMap;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.utils.TextProgressBar;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * This class represents a repository for Sequences. If we run out of memory, old Sequences are
 * released and has to be newly instantiated by the next accession (weak references).
 * 
 * @author Markus Brosch
 */
public class SequenceRepository {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * Keep all sequences in memory as long memory is available. If we run out of memory, release old
   * Sequences.
   */
  private static CacheMap _seqDB = new WeakCacheMap();

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * method to get a SequenceFM including all it's features.
   * 
   * @param pOrganism
   *        organism
   * @param pIdentifier
   *        the identifier
   * @return SequenceFM according to the parameters; either from cache or newly generated.
   */
  public static SequenceFM getSequenceFM(final String pOrganism, final String pIdentifier) {
    String id = pOrganism + pIdentifier;
    SequenceFM seq = (SequenceFM) _seqDB.get(id);
    if (seq == null) {
      seq = SequenceFM.getInstance(pOrganism, pIdentifier);
      assert (seq != null);
      System.out.println("\nNew SequenceFM bound to server: Organism " + pOrganism
          + " / Chromosome: " + pIdentifier);
      TextProgressBar p = new TextProgressBar();
      p.start();
      seq.features();
      p.stop();
      _seqDB.put(id, seq);
    }
    return seq;
  }
}