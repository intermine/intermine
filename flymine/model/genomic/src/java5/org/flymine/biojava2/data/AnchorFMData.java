package org.flymine.biojava2.data;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.identifier.Identifier;
import org.bjv2.seq.Strand;
import org.flymine.model.genomic.Location;

/**
 * Dumb Anchor Data Bean. Get's data lazy (only when realy accessed).
 * This Data Bean will be mapped to Anchor by using the BJv2 integration system.
 *
 * @author Markus Brosch
 */
public class AnchorFMData {

  // =======================================================================
  // Attributes
  // =======================================================================

  private final Location location;
  private final Identifier sequence_identifier;
  private final int DEFAULT = -9999;

  private int min = DEFAULT;
  private int max = DEFAULT;
  private Strand strand = null;

  // =======================================================================
  // Constructor
  // =======================================================================

  public AnchorFMData(final Identifier pSequenceIdentifier, final Location pLocation) {
    this.sequence_identifier = pSequenceIdentifier;
    this.location = pLocation;
  }

 // =======================================================================
 // Getters
 // =======================================================================

  public Identifier getSequence_identifier() {
    return sequence_identifier;
  }

  public int getMin() {
    if (min == DEFAULT) min = location.getStart();
    return min;
  }

  public int getMax() {
    if (max == DEFAULT) max = location.getEnd();
    return max;
  }

  public Strand getStrand() {
    if (strand == null) {
      final int intStrand = location.getStrand();
      switch (intStrand) {
        case 1:
          strand = Strand.POSITIVE;
          break;
        case -1:
          strand = Strand.NEGATIVE;
          break;
        case 0:
          strand = Strand.UNKNOWN;
          break;
        default:
          strand = Strand.UNKNOWN; //TODO
      }
    }
    return strand;
  }

  // =======================================================================
  // Override Object
  // =======================================================================

  @Override
  public String toString() {
    return "AnchorData [sequence_identifier: " + sequence_identifier +
        " min: " + getMin() + " max: " + getMax() + " strand: " + getStrand() + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AnchorFMData)) return false;

    final AnchorFMData anchorFMData = (AnchorFMData) o;

    if (location != null
        ? !location.equals(anchorFMData.location)
        : anchorFMData.location != null) return false;
    if (sequence_identifier != null
        ? !sequence_identifier.equals(anchorFMData.sequence_identifier)
        : anchorFMData.sequence_identifier != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = (location != null ? location.hashCode() : 0);
    result = 29 * result + (sequence_identifier != null ? sequence_identifier.hashCode() : 0);
    return result;
  }
}
