package org.flymine.biojava2.data;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.annotation.Annotation;
import org.bjv2.annotation.impl.AnnotationImpl;
import org.bjv2.identifier.Identifier;
import org.bjv2.symbol.IllegalSymbolException;
import org.bjv2.symbol.SymbolBuffer;
import org.bjv2.symbol.SymbolBuffers;
import org.bjv2.symbol.impl.CharacterSymbol;
import org.bjv2.symbol.impl.TypedSymbol;
import org.flymine.model.genomic.LocatedSequenceFeature;

/**
 * Dumb Sequence Data Bean. Get's data lazy (only when realy accessed).
 * This Data Bean will be mapped to Sequence by using the BJv2 integration system.
 *
 * @author Markus Brosch
 */
public class SequenceFMData {

  // =======================================================================
  // Attributes
  // =======================================================================

  private final LocatedSequenceFeature lsf;
  private final Identifier identifier;

  private SymbolBuffer<TypedSymbol<CharacterSymbol>> symbolBuffer;
  private Annotation annotation;

  // =======================================================================
  // Constructor
  // =======================================================================

  public SequenceFMData(final Identifier pSequenceIdentifier, final LocatedSequenceFeature pLsf) {
    this.identifier = pSequenceIdentifier;
    this.lsf = pLsf;
  }

  // =======================================================================
  // Getters
  // =======================================================================

  public Identifier getIdentifier() {
    return identifier;
  }

  public SymbolBuffer getSymbolBuffer() {
    if (symbolBuffer == null) {
      try {
        //try to get a proper Sequence
        symbolBuffer = SymbolBuffers.fromText(lsf.getSequence().getResidues());
      } catch (IllegalSymbolException e) {
        e.printStackTrace();
        try {
          //if sequence is invalid, make an empty Sequence
          symbolBuffer = SymbolBuffers.fromText("");
        } catch (IllegalSymbolException ee) {
          throw new RuntimeException("could not create an empty SymbolBuffer");
        }
      }
    }
    return symbolBuffer;
  }

  public Annotation getAnnotation() {
    if (annotation == null) annotation = AnnotationImpl.FACTORY.createAnnotation();
    return annotation;
  }

  // =======================================================================
  // Overrides Object
  // =======================================================================

  @Override
  public String toString() {
    return "SequenceFMData [" + identifier + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SequenceFMData)) return false;

    final SequenceFMData sequenceFMData = (SequenceFMData) o;

    if (identifier != null
        ? !identifier.equals(sequenceFMData.identifier)
        : sequenceFMData.identifier !=
        null) {
      return false;
    }
    if (lsf != null
        ? !lsf.equals(sequenceFMData.lsf)
        : sequenceFMData.lsf != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = (lsf != null ? lsf.hashCode() : 0);
    result = 29 * result + (identifier != null ? identifier.hashCode() : 0);
    return result;
  }

}