package org.flymine.biojava2.data;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.flymine.biojava2.FlyMineToBJv2;
import org.flymine.biojava2.IdentifierFM;
import org.flymine.biojava2.FlyMineToBJv2;
import org.flymine.biojava2.IdentifierFM;
import org.bjv2.identifier.Identifier;

/**
 * Dumb Relation Data Bean. This Data Bean will be mapped to Relation by using the BJv2 integration
 * system. If this relation refers to objects which are not loaded, they are retrieved from FlyMine
 * before accessed (lazy loading mechanism).
 *
 * @author Markus Brosch
 */
public class RelationFMData {

  // =======================================================================
  // Attributes
  // =======================================================================

  private FlyMineToBJv2 ref2DataIntegration;
  private IdentifierFM source_identifier;
  private IdentifierFM target_identifier;
  private String type;

  // =======================================================================
  // Constructor
  // =======================================================================

  public RelationFMData(FlyMineToBJv2 ref, IdentifierFM source, IdentifierFM target, String type) {
    this.ref2DataIntegration = ref;
    this.source_identifier = source;
    this.target_identifier = target;
    this.type = type;
  }

  // =======================================================================
  // Getters
  // =======================================================================

  public String getType() {
    return type;
  }

  public Identifier getTarget_identifier() {
    ref2DataIntegration.addBioEntityFromID(target_identifier);
    return target_identifier;
  }

  public Identifier getSource_identifier() {
    ref2DataIntegration.addBioEntityFromID(source_identifier);
    return source_identifier;
  }

  // =======================================================================
  // Overrides Object
  // =======================================================================

  @Override
      public String toString() {
    return "source:" + source_identifier + " target:" + target_identifier + " type: " + type;
  }

  @Override
      public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RelationFMData)) return false;

    final RelationFMData relationData = (RelationFMData) o;

    if (ref2DataIntegration != null
        ? !ref2DataIntegration.equals(relationData.ref2DataIntegration)
        : relationData.ref2DataIntegration != null) {
      return false;
    }
    if (type != null
        ? !type.equals(relationData.type)
        : relationData.type != null) {
      return false;
    }

    //source-source
    boolean ss = true;
    if (source_identifier != null
        ? !source_identifier.equals(relationData.source_identifier)
        : relationData.source_identifier != null) {
      ss = false;
    }
    boolean tt = true;
    //target-target
    if (target_identifier != null
        ? !target_identifier.equals(relationData.target_identifier)
        : relationData.target_identifier != null) {
      tt = false;
    }
    if (tt == ss == true) { return true; }
    else {
      boolean st = true;
      //source-target (bidirectional)
      if (source_identifier != null
          ? !source_identifier.equals(relationData.target_identifier)
          : relationData.source_identifier != null) {
        st = false;
      }
      boolean ts = true;
      //target-source (bidirectional)
      if (target_identifier != null
          ? !target_identifier.equals(relationData.source_identifier)
          : relationData.target_identifier != null) {
        ts = false;
      }
      return (st == true && ts == true);
    }
  }

  @Override
      public int hashCode() {
    int result;
    result = (ref2DataIntegration != null ? ref2DataIntegration.hashCode() : 0);

    final int sourceHash;
    if (source_identifier != null) {
      sourceHash = source_identifier.hashCode();
    } else {
      sourceHash = 0;
    }

    final int targetHash;
    if (source_identifier != null) {
      targetHash = target_identifier.hashCode();
    } else {
      targetHash = 0;
    }

    if (sourceHash < targetHash) {
      result = 29 * result + sourceHash;
      result = 29 * result + targetHash;
    } else {
      result = 29 * result + targetHash;
      result = 29 * result + sourceHash;
    }

    result = 29 * result + (type != null ? type.hashCode() : 0);
    return result;
  }
}
