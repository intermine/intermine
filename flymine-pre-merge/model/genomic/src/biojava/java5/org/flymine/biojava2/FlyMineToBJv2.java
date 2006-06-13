package org.flymine.biojava2;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.bjv2.annotation.Annotation;
import org.bjv2.annotation.impl.AnnotationImpl;
import org.bjv2.identifier.Identifier;
import org.bjv2.integrator.*;
import org.bjv2.integrator.impl.IntegratorContextImpl;
import org.bjv2.seq.*;
import org.bjv2.seq.Sequence;
import org.flymine.biojava2.data.*;
import org.flymine.model.genomic.*;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.bjv2.integrator.DataSources.createDataSource;

//TODO: make add it more general for flymine objects - not only LocatedSequenceFeatures

/**
 * This is an experimental implementation of a FlyMine - BioJava2 org. There are some bits and
 * pieces which are not supported yet (BJv2 alpha), but it directs the way of a simple and flexible
 * org. BioJava2 is definitively first choice compared to BJv1.
 *
 * @author Markus Brosch
 */
public class FlyMineToBJv2 {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * FlyMine ObjectStore
   */
  private final ObjectStore os; //not null

  /**
   * FlyMine Objects which should be mapped to BioJava2
   */
  private final Set<BioEntity> flyMineFeatures; //not null

  /**
   * Keeps track of data which was added to data sources.
   */
  private Set<Identifier> integratedFeature = new HashSet<Identifier>();

  /**
   * DataSource for FeatureFMData
   */
  private DataSource<FeatureFMData> featureDataSource;

  /**
   * DataSource for LocatorFMData
   */
  private DataSource<LocatorFMData> locatorDataSource;

  /**
   * DataSource for AnchorFMData
   */
  private DataSource<AnchorFMData> anchorDataSource;

  /**
   * DataSource for SequenceFMData
   */
  private DataSource<SequenceFMData> sequenceDataSource;

  /**
   * DataSource for RelationFMData
   */
  private DataSource<RelationFMData> relationDataSource;

  /**
   * Integorator for FeatureFM
   */
  private Integrator<FeatureFM> featureIntegrator;

  /**
   * Integrator for Locator
   */
  private Integrator<Locator> locatorIntegrator;

  /**
   * Integrator for Anchor
   */
  private Integrator<Anchor> anchorIntegrator;

  /**
   * Integorator for Sequence
   */
  private Integrator<Sequence> sequenceIntegrator;

  /**
   * Integrator for FeatureRelation
   */
  private Integrator<FeatureRelation> relationIntegrator;

  // =======================================================================
  // Constructor
  // =======================================================================

  public FlyMineToBJv2(final ObjectStore pOS, final Set<BioEntity> pFlyMineFeatures)
      throws IntrospectionException {

    os = pOS;
    flyMineFeatures = pFlyMineFeatures;

    //set up data sources
    featureDataSource = createDataSource(FeatureFMData.class);
    locatorDataSource = createDataSource(LocatorFMData.class);
    anchorDataSource = createDataSource(AnchorFMData.class);
    sequenceDataSource = createDataSource(SequenceFMData.class);
    relationDataSource = createDataSource(RelationFMData.class);

    //fill data sources
    for (BioEntity be : flyMineFeatures) {
      addLocatedSequenceFeature(be);
    }

    //integrate data from data sources
    final IntegratorContext context = new IntegratorContextImpl();

    featureIntegrator = context.getIntegrator(Sequences.FEATURE_DOMAIN, FeatureFM.class);
    featureIntegrator.addDataSource(featureDataSource,
        PropertyMappings.findMapping(FeatureFM.class, FeatureFMData.class));

    locatorIntegrator = context.getIntegrator(Sequences.LOCATOR_DOMAIN, Locator.class);
    locatorIntegrator.addDataSource(locatorDataSource,
        PropertyMappings.findMapping(Locator.class, LocatorFMData.class));

    anchorIntegrator = context.getIntegrator(Sequences.ANCHOR_DOMAIN, Anchor.class);
    anchorIntegrator.addDataSource(anchorDataSource,
        PropertyMappings.findMapping(Anchor.class, AnchorFMData.class));

    sequenceIntegrator = context.getIntegrator(Sequences.SEQUENCE_DOMAIN, Sequence.class);
    sequenceIntegrator.addDataSource(sequenceDataSource,
        PropertyMappings.findMapping(Sequence.class, SequenceFMData.class));

    relationIntegrator = context.getIntegrator(Sequences.RELATION_DOMAIN, FeatureRelation.class);
    relationIntegrator.addDataSource(relationDataSource,
        PropertyMappings.findMapping(FeatureRelation.class, RelationFMData.class));
  }

  // =======================================================================
  // Methods
  // =======================================================================

  public void addLocatedSequenceFeature(BioEntity bioEntity) {
    try {
      // FeatureData
      final IdentifierFM beIdent = new IdentifierFM(bioEntity);

      Set<Class> decompClasses = DynamicUtil.decomposeClass(bioEntity.getClass());
      final Class dClazz = decompClasses.iterator().next();
      final FeatureType fType = FeatureTypes.valueOf(TypeUtil.unqualifiedName(dClazz.getName()));

      final Annotation fAnnotation = AnnotationImpl.FACTORY.createAnnotation();
      //put Synonmys or FlyMine Annotations to fAnnotation if you want ...

      /*TODO: replace ontologyTerm with a proper ontologyTerm from FylMine once it is available.
              The idea is, that you can use this ontologyTerm to hook on the Ontology in BJv2 once
              available and explore the OntologyHierarchy */
      final FeatureFMData fd = new FeatureFMData(beIdent, fType, fAnnotation, bioEntity, "ontologyTerm");
      featureDataSource.add(fd);
      integratedFeature.add(beIdent);

      // Locations & Anchors & Sequences & Relations
      for (Object o : bioEntity.getObjects()) {
        if (o instanceof Location) {
          final Location location = (Location) o;
          final LocatedSequenceFeature seq;

          //SequenceData
          try {
            seq = (LocatedSequenceFeature) location.getObject();
          } catch (ClassCastException e) {
            throw new RuntimeException("Object of a Location must have a LocatedSequenceFeature");
          }
          assert (seq != null);
          final IdentifierFM seqIdent = new IdentifierFM(seq);
          SequenceFMData sd = new SequenceFMData(seqIdent, seq);
          sequenceDataSource.add(sd);

          //LocatorData
          final List<AnchorFMData> anchors = new ArrayList<AnchorFMData>();
          final LocatorFMData ld = new LocatorFMData(beIdent, anchors, null);
          locatorDataSource.add(ld);

          //AnchorData
          final AnchorFMData ad = new AnchorFMData(seqIdent, location);
          anchors.add(ad);
          anchorDataSource.add(ad);
        }

        //Hardcoded RelationData according to FlyMine Relations
        //(for now: Orthologue, Paralogue; Extend if you want...)
        if (o instanceof Orthologue) {
          final Orthologue orthologue = (Orthologue) o;
          final BioEntity subject = orthologue.getObject();
          final IdentifierFM subjIdent = new IdentifierFM(subject);
          final RelationFMData orthRel = new RelationFMData(this, beIdent, subjIdent, "Orthologue");
          relationDataSource.add(orthRel);
        }
        if (o instanceof Paralogue) {
          final Paralogue orthologue = (Paralogue) o;
          final BioEntity subject = orthologue.getObject();
          final IdentifierFM subjIdent = new IdentifierFM(subject);
          final RelationFMData paraRel = new RelationFMData(this, beIdent, subjIdent, "Paralogue");
          relationDataSource.add(paraRel);
        }
        /*TODO: Once Ontologies are integrated to BJv2, "hasA/partOf" and "isA" should be modelled
                as Relations as well! As a hook the ontologyTerm of a Feature is to be used.
                Therefore it is easy to navigate the Ontology-Hierarchy. */
      }
    } catch (OperationNotSupportedException e) {

    }
  }

  /**
   * Provides a way to add FlyMine features to the DataSources. It is used as a kind of lazy data
   * instantiation. So imagine you end up querying an object which wasn't added to the data source
   * already - so as a pre-requirement you use this method to make sure the data related to this
   * identifier is available.<p> Complexity for lookup: O(c) <p> Once BioJava2 is more mature, there
   * should be a convenient way to make lazyLoading available at the DataSource backend - which is
   * more elegant than this work-around.
   *
   * @param pIdentifier
   * @throws ClassCastException
   */
  public void addBioEntityFromID(IdentifierFM pIdentifier) {
    //TODO: Replace this method once lazy loading at the DataSource backend of BJv2 is available
    if (integratedFeature.contains(pIdentifier)) return; //don't add this Feature again...
    try {
      final InterMineObject imo = os.getObjectById(pIdentifier.getFlyMineID());
      final LocatedSequenceFeature lsf = (LocatedSequenceFeature) imo;
      this.addLocatedSequenceFeature(lsf);
    } catch (ObjectStoreException e) { //no user error; programming error!
      throw new RuntimeException(pIdentifier + " refers not to an existing InterMineObject!");
    }
  }

  public Integrator<FeatureFM> getFeatureIntegrator() {
    return featureIntegrator;
  }

  public Integrator<Locator> getLocatorIntegrator() {
    return locatorIntegrator;
  }

  public Integrator<Anchor> getAnchorIntegrator() {
    return anchorIntegrator;
  }

  public Integrator<Sequence> getSequenceIntegrator() {
    return sequenceIntegrator;
  }

  public Integrator<FeatureRelation> getFeatureRelationIntegrator() {
    return relationIntegrator;
  }
}
