package org.flymine.biojava2.data;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.annotation.Annotation;
import org.bjv2.identifier.Identifiable;
import org.bjv2.identifier.Identifier;
import org.bjv2.seq.FeatureType;
import org.flymine.model.genomic.BioEntity;


/**
 * Dumb Data Bean for a BioJava 2 FlyMine Feature.
 * This Data Bean will be mapped to FeatureFM by using the BJv2 integration system.
 *
 * @author Markus Brosch
 */
public class FeatureFMData implements Identifiable {

  // =======================================================================
  // Attributes
  // =======================================================================

  private final Identifier identifier;
  private final FeatureType type;
  private final Annotation annotation;
  private final BioEntity bioEntity; //FlyMine BioEntity
  private final String ontologyTerm; //Please change to a proper Type, once Ontologies are available

  // =======================================================================
  // Constructor
  // =======================================================================

  public FeatureFMData(final Identifier pIdentifier, final FeatureType pType,
      final Annotation pAnnotation, final BioEntity pBioEntity, final String pOntologyTerm) {

    this.identifier = pIdentifier;
    this.type = pType;
    this.annotation = pAnnotation;
    this.bioEntity = pBioEntity;
    this.ontologyTerm = pOntologyTerm;
  }

  // =======================================================================
  // Getters
  // =======================================================================

  public FeatureType getType() {
    return type;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public Annotation getAnnotation() {
    return annotation;
  }

  public BioEntity getBioEntity() {
    return bioEntity;
  }

  public String getOntologyTerm() {
    return ontologyTerm;
  }

 // =======================================================================
 // Override Object
 // =======================================================================

  @Override
      public String toString() {
    return "FeatureFMData [type:" + type.getName() + " identifier: " + identifier + " ontology: " +
        ontologyTerm + " bioEntity: " + bioEntity + " annotation: " + annotation + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeatureFMData)) return false;

    final FeatureFMData featureFMData = (FeatureFMData) o;

    if (annotation != null
        ? !annotation.equals(featureFMData.annotation)
        : featureFMData.annotation != null) {
      return false;
    }
    if (bioEntity != null
        ? !bioEntity.equals(featureFMData.bioEntity)
        : featureFMData.bioEntity !=  null) {
      return false;
    }
    if (identifier != null
        ? !identifier.equals(featureFMData.identifier)
        : featureFMData.identifier != null) {
      return false;
    }
    if (ontologyTerm != null
        ? !ontologyTerm.equals(featureFMData.ontologyTerm)
        : featureFMData.ontologyTerm != null) {
      return false;
    }
    if (type != null
        ? !type.equals(featureFMData.type)
        : featureFMData.type != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = (identifier != null ? identifier.hashCode() : 0);
    result = 29 * result + (type != null ? type.hashCode() : 0);
    result = 29 * result + (annotation != null ? annotation.hashCode() : 0);
    result = 29 * result + (bioEntity != null ? bioEntity.hashCode() : 0);
    result = 29 * result + (ontologyTerm != null ? ontologyTerm.hashCode() : 0);
    return result;
  }
}
