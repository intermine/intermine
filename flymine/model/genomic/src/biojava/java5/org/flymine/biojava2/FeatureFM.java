package org.flymine.biojava2;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.integrator.*;
import org.bjv2.seq.Feature;
import org.bjv2.seq.FeatureRelation;
import org.flymine.model.genomic.BioEntity;

import java.util.Set;

/**
 * This Feature defines a FlyMine BioEntity Feature. This Interface is used for instantiating
 * proxy objects of Feature.
 *
 * @author Markus Brosch
 */
@PrimaryKey({"identifier"})
@Identifier("urn:biojava.org:integratorContext/sequence:features")
@Properties({@Property(property = "locators",
              plan = MergeStrategy.MERGE,
              query = "locators <- feature == this",
              identifier = "urn:biojava.org:integratorContext/sequence:locators"),
             @Property(property = "relations",
              plan = MergeStrategy.MERGE,
              query = "relations <- OR(source, target) == this",
              identifier = "urn:biojava.org:integratorContext/sequence:relations"),
             @Property(property = "annotation",
              plan = MergeStrategy.MERGE),
             @Property(property = "identifier",
              plan = MergeStrategy.FIRST_VALUE),
             @Property(property = "type",
              plan = MergeStrategy.FIRST_VALUE),
             @Property(property = "bioEntity",
              plan = MergeStrategy.FIRST_VALUE),
             @Property(property = "ontologyTerm",
              plan = MergeStrategy.FIRST_VALUE)
            })
public interface FeatureFM extends Feature {

  public BioEntity getBioEntity();

  /**
   * The underlying idea of this ontology term is the following:<br>
   * once BJv2 has ontology org and reasoning integrated, we can use this String (or with
   * whatever it will be replaced) as a hook. UseCase scenario: Let's say our ontology term
   * is a "transcript" and the underlying ontology is the SequenceOntology (ligth). Then we can
   * easily work out the relation of a transcript. E.g. we know a Transcript HAS Exons. This
   * information can be used to get the actual Exons from the Transcript Object. BJv2 Relations
   * can represent the conntections.
   * @return
   */
  public String getOntologyTerm();

  public Set<FeatureRelation> getRelations();
}
