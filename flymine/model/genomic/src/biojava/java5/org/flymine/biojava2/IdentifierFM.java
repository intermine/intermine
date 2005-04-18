package org.flymine.biojava2;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.identifier.Identifier;
import org.flymine.model.genomic.BioEntity;

/**
 * An BJv2 Identifier for FlyMine Objects
 *
 * @author Markus Brosch
 */
public class IdentifierFM implements Identifier {

  // =======================================================================
  // Attributes
  // =======================================================================

  public static final String URN = "org:flymine:";
  private Integer id;

  // =======================================================================
  // Constructor
  // =======================================================================

  public IdentifierFM(BioEntity pFlyMineBioEntity) {
    id = pFlyMineBioEntity.getId();
  }

  // =======================================================================
  // Getters
  // =======================================================================

  public Integer getFlyMineID() {
    return id;
  }

  // =======================================================================
  // Override Object
  // =======================================================================
  
  @Override
      public String toString() {
    return URN + id.toString();
  }

  @Override
      public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof IdentifierFM)) return false;

    final IdentifierFM identifierFM = (IdentifierFM) o;

    if (!URN.equals(identifierFM.URN)) return false;
    if (!id.equals(identifierFM.id)) return false;

    return true;
  }

  @Override
      public int hashCode() {
    int result;
    result = URN.hashCode();
    result = 29 * result + id.hashCode();
    return result;
  }
}
