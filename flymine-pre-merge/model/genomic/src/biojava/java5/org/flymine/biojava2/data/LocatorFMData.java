package org.flymine.biojava2.data;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.annotation.Annotation;
import org.bjv2.identifier.Identifier;

import java.util.List;

/**
 * Dumb Locator Data Bean.
 * This Data Bean will be mapped to Locator by using the BJv2 integration system.
 *
 * @author Markus Brosch
 */
public class LocatorFMData {

  // =======================================================================
  // Attributes
  // =======================================================================

  private final Identifier feature_identifier;
  private final List<AnchorFMData> anchors;
  private final Annotation annotation;

  // =======================================================================
  // Constructor
  // =======================================================================

  public LocatorFMData(final Identifier pFeature_identifier, final List<AnchorFMData> pAnchors,
      Annotation annotation) {

    this.feature_identifier = pFeature_identifier;
    this.anchors = pAnchors;
    this.annotation = annotation;
  }

  // =======================================================================
  // Getters
  // =======================================================================

  public Identifier getFeature_identifier() {
    return feature_identifier;
  }

  public List<AnchorFMData> getAnchors() {
    return anchors;
  }

  public Annotation getAnnotation() {
    return annotation;
  }

  // =======================================================================
  // Override Object
  // =======================================================================

  @Override
  public String toString() {
    return "Locator [feature_identifier: " + feature_identifier +
        " anchors: " + anchors + " annotations: " + annotation + "]";
  }

  @Override
  public boolean equals(final Object pObject) {
    if (this == pObject) {
      return true;
    }
    if (!(pObject instanceof LocatorFMData)) {
      return false;
    }

    final LocatorFMData locatorData = (LocatorFMData) pObject;

    if (anchors != null
        ? !anchors.equals(locatorData.anchors)
        : locatorData.anchors != null) {
      return false;
    }
    if (annotation != null
        ? !annotation.equals(locatorData.annotation)
        : locatorData.annotation != null) {
      return false;
    }
    if (feature_identifier != null
        ? !feature_identifier.equals(locatorData.feature_identifier)
        : locatorData.feature_identifier != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = (feature_identifier != null ? feature_identifier.hashCode() : 0);
    result = 29 * result + (anchors != null ? anchors.hashCode() : 0);
    result = 29 * result + (annotation != null ? annotation.hashCode() : 0);
    return result;
  }
}
