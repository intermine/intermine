package org.intermine.webservice.server.complexes;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.ols.CachedOlsOntologyTermFetcher;
import psidev.psi.mi.jami.datasource.InteractionWriter;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.json.InteractionViewerJson;
import psidev.psi.mi.jami.json.MIJsonOptionFactory;
import psidev.psi.mi.jami.json.MIJsonType;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.InteractionCategory;
import psidev.psi.mi.jami.model.Xref;
import psidev.psi.mi.jami.model.impl.DefaultComplex;
import psidev.psi.mi.jami.model.impl.DefaultCvTerm;
import psidev.psi.mi.jami.model.impl.DefaultInteractor;
import psidev.psi.mi.jami.model.impl.DefaultModelledFeature;
import psidev.psi.mi.jami.model.impl.DefaultModelledParticipant;
import psidev.psi.mi.jami.model.impl.DefaultOrganism;
import psidev.psi.mi.jami.model.impl.DefaultPosition;
import psidev.psi.mi.jami.model.impl.DefaultRange;
import psidev.psi.mi.jami.model.impl.DefaultStoichiometry;
import psidev.psi.mi.jami.model.impl.DefaultXref;

/**
 * Web service that produces JSON required by the complex viewer.
 *
 * @author julie
 */
public class ExportService extends JSONService
{
//    private static final Logger LOG = Logger.getLogger(ExportService.class);
    private static final String FORMAT_PARAMETER = "format";
    private static final String DEFAULT_FORMAT = "JSON";
    private static final String EBI = "intact";
    // private static final String BINDING_SITE = "binding region";
    private static final Map<String, String> MOLECULE_TYPES = new HashMap<String, String>();
    private Map<String, DefaultInteractor> interactors = new HashMap<String, DefaultInteractor>();
    private Map<String, DefaultModelledParticipant> participants
        = new HashMap<String, DefaultModelledParticipant>();
    private Map<String, FeatureHolder> features = new HashMap<String, FeatureHolder>();
    private static final String BINDING_SITE = "binding region";

    /**
     * Default constructor.
     * @param im The InterMine state object.
     */
    public ExportService(InterMineAPI im) {
        super(im);
    }

    // TODO this is stupid and dumb to hardcode this. Replace with a webservice call.
    static {
        MOLECULE_TYPES.put("protein", "MI:0326");
        MOLECULE_TYPES.put("small molecule", "MI:0328");
        MOLECULE_TYPES.put("ribonucleic acid", "MI:0320");
        MOLECULE_TYPES.put("transfer rna", "MI:0325");
        MOLECULE_TYPES.put("double stranded deoxyribonucleic acid", "MI:0681");
        MOLECULE_TYPES.put("small nucleolar rna", "MI:0609");
    }

    @Override
    protected void execute() throws Exception {

        // format is optional
        String format = request.getParameter(FORMAT_PARAMETER);
        if (StringUtils.isEmpty(format)) {
            format = DEFAULT_FORMAT;
        }

        // EBI complex identifier
        String identifier = getComplexIdentifier();

        // get complex from InterMine database, transform to JAMI complex
        DefaultComplex complex = getComplex(identifier);

        // initialise all existing json writers
        InteractionViewerJson.initialiseAllMIJsonWriters();

        // get jsob option factory
        MIJsonOptionFactory optionFactory = MIJsonOptionFactory.getInstance();

        // get json writer for complexes from factory
        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();
        InteractionWriter writer = null;

        try {
            // you will try to use a cachedOlsClient if available, otherwise, you don't sort
            // your features using ols
            writer = writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(
                    getRawOutput(), InteractionCategory.modelled, null, MIJsonType.n_ary_only,
                    new CachedOlsOntologyTermFetcher(), null));
        } catch (BridgeFailedException e) {
            writer = writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(
                    getRawOutput(), InteractionCategory.modelled, null, MIJsonType.n_ary_only,
                    null, null));
        }

        try {
            writer.start();
            writer.write(complex);
            writer.end();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private String getComplexIdentifier() {
        String identifier = StringUtils.substring(request.getPathInfo(), 1);
        if (StringUtils.isBlank(identifier)) {
            throw new BadRequestException("No identifier provided");
        }
        return identifier;
    }

    /**
     * @param identifier complex identifier, e.g. EBI-123
     * @return complex of interest
     * @throws ObjectStoreException if something goes wrong
     */
    protected DefaultComplex getComplex(String identifier) throws ObjectStoreException {

        // construct query
        PathQuery q = getQuery(identifier);

        // execute query
        ExportResultsIterator results = im.getPathQueryExecutor().execute(q);

        // identifier
        Xref complexXref = new DefaultXref(new DefaultCvTerm(EBI), identifier);

        // create the complex
        DefaultComplex complex = new DefaultComplex(identifier, complexXref);

        // loop through query results
        // proteins will span different rows if there are interacting regions
        while (results.hasNext()) {
            List<ResultElement> row = results.next();

            String name = (String) row.get(0).getField();
            String systematicName = (String) row.get(1).getField();
            String properties = (String) row.get(2).getField();
            // String function = (String) row.get(3).getField();
            String primaryIdentifier = (String) row.get(4).getField();
            Integer stoichiometry = (Integer) row.get(5).getField();
            if (stoichiometry == null) {
                stoichiometry = 1;
            }
            String biologicalRole = (String) row.get(7).getField();
            // e.g. protein, SmallMolecule
            String moleculeType = (String) row.get(8).getField();

            // set complex attributes
            complex.setFullName(name);
            complex.setSystematicName(systematicName);
            complex.setPhysicalProperties(properties);

            // interactor type
            CvTerm type = getInteractorType(moleculeType);

            // organism
            DefaultOrganism organism = null;
            if (row.get(6) != null && row.get(6).getField() != null) {
                Integer taxonId = (Integer) row.get(6).getField();
                organism = new DefaultOrganism(taxonId);
            }

            // cv term
            CvTerm db = new DefaultCvTerm("uniprotkb");

            // identifier
            Xref xref = new DefaultXref(db, primaryIdentifier);

            // interactor
            DefaultInteractor interactor
                = updateInteractor(primaryIdentifier, type, organism, xref);

            // participant
            DefaultModelledParticipant participant = getParticipant(complex, primaryIdentifier,
                    interactor, biologicalRole, stoichiometry);

            // interactions -- not all complexes will have them!
            if (row.get(9) != null && row.get(9).getField() != null) {
                // same as protein above
                //String featureIdentifier = (String) row.get(9).getField();
                String locatedOn = (String) row.get(10).getField();
                Integer start = (Integer) row.get(11).getField();
                Integer end = (Integer) row.get(12).getField();

                // range
                DefaultPosition startPosition = new DefaultPosition(new Long(start));
                DefaultPosition endPosition = new DefaultPosition(new Long(end));

                DefaultRange range = new DefaultRange(startPosition, endPosition);

                DefaultCvTerm cvterm = new DefaultCvTerm(BINDING_SITE);

                // feature
                DefaultModelledFeature feature = getFeature(primaryIdentifier, participant,
                        locatedOn);

                Xref bindingXref = new DefaultXref(db, locatedOn);

                // main interactor
                DefaultInteractor bindingInteractor = getInteractor(locatedOn, bindingXref);

                // binding participant
                DefaultModelledParticipant bindingParticipant = getParticipant(complex, locatedOn,
                    bindingInteractor, null, null);

                // binding feature
                DefaultModelledFeature bindingFeature = getFeature(locatedOn,
                        bindingParticipant, primaryIdentifier);

                bindingFeature.getRanges().add(range);

                feature.getLinkedFeatures().add(bindingFeature);

                // associate linked features with interactor
                participant.getFeatures().add(feature);
            }
        }
        return complex;
    }

    private DefaultInteractor updateInteractor(String primaryIdentifier, CvTerm type,
            DefaultOrganism organism, Xref xref) {
        DefaultInteractor interactor = getInteractor(primaryIdentifier, xref);
        interactor.setInteractorType(type);
        interactor.setOrganism(organism);

        return interactor;
    }

    private DefaultInteractor getInteractor(String primaryIdentifier, Xref xref) {
        DefaultInteractor interactor = interactors.get(primaryIdentifier);
        if (interactor == null) {
            interactor = new DefaultInteractor(primaryIdentifier, xref);
            interactors.put(primaryIdentifier, interactor);
        }
        return interactor;
    }

    private DefaultModelledParticipant getParticipant(DefaultComplex complex,
            String primaryIdentifier, DefaultInteractor interactor, String biologicalRole,
            Integer stoichiometry) {
        DefaultModelledParticipant participant = participants.get(primaryIdentifier);
        if (participant == null) {
            participant = new DefaultModelledParticipant(interactor);
            participants.put(primaryIdentifier, participant);
            complex.addParticipant(participant);
        }
        // we may or may not have the information when it's processed
        if (biologicalRole != null) {
            participant.setBiologicalRole(new DefaultCvTerm(biologicalRole));
            participant.setStoichiometry(new DefaultStoichiometry(stoichiometry));
        }
        return participant;
    }

    private DefaultModelledFeature getFeature(String primaryIdentifier,
            DefaultModelledParticipant participant, String otherIdentifier) {
        FeatureHolder holder = features.get(primaryIdentifier);
        DefaultModelledFeature feature = null;
        if (holder == null) {
            // new feature
            feature = new DefaultModelledFeature(participant, primaryIdentifier, primaryIdentifier);

            participant.addFeature(feature);

            // new holder
            holder = new FeatureHolder(primaryIdentifier, participant);

            // store this linked feature for reuse when we process the other side of this
            // interaction
            holder.addLinkedFeature(otherIdentifier, feature);

            // store for later
            features.put(primaryIdentifier, holder);
        } else {
            // see if we have this relationship already
            feature = holder.getLinkedFeature(otherIdentifier);
            // we might have the protein but we have not seen the other interactor before
            if (feature == null) {
                feature = new DefaultModelledFeature(participant, primaryIdentifier,
                        primaryIdentifier);
                holder.addLinkedFeature(otherIdentifier, feature);
                participant.addFeature(feature);
            }
        }
        return feature;
    }

    private DefaultCvTerm getInteractorType(String moleculeType) {
        String identifier = MOLECULE_TYPES.get(moleculeType);
        DefaultCvTerm cvTerm = new DefaultCvTerm(moleculeType, identifier);
        return cvTerm;
    }

    private PathQuery getQuery(String identifier) throws ObjectStoreException {
        PathQuery query = new PathQuery(model);
        query.addViews("Complex.name",
                "Complex.systematicName",
                "Complex.properties",
                "Complex.function",
                "Complex.allInteractors.participant.primaryIdentifier",
                "Complex.allInteractors.stoichiometry",
                "Complex.allInteractors.participant.organism.taxonId",
                "Complex.allInteractors.biologicalRole",
                "Complex.allInteractors.type",
                "Complex.allInteractors.interactions.details.interactingRegions.locations.feature."
                + "primaryIdentifier",
                "Complex.allInteractors.interactions.details.interactingRegions.locations."
                + "locatedOn.primaryIdentifier",
                "Complex.allInteractors.interactions.details.interactingRegions.locations.start",
                "Complex.allInteractors.interactions.details.interactingRegions.locations.end");
        query.setOuterJoinStatus("Complex.allInteractors.interactions", OuterJoinStatus.OUTER);
        query.setOuterJoinStatus("Complex.allInteractors.participant.organism",
            OuterJoinStatus.OUTER);
        query.addConstraint(Constraints.eq("Complex.identifier", identifier));
        query.addOrderBy("Complex.allInteractors.participant.primaryIdentifier",
                OrderDirection.ASC);
        return query;
    }

    /**
     * hold the feature / linked feature relationships here
     * @author julie
     */
    protected class FeatureHolder
    {
        // primary of the protein that has the feature
        protected String primaryIdentifier = null;
        // map from the primary identifier of the protein that has a linked feature
        // to the linked feature
        protected Map<String, DefaultModelledFeature> binderToFeatures
            = new HashMap<String, DefaultModelledFeature>();

        protected DefaultModelledParticipant participant = null;

        /**
         * @param primaryIdentifier primary identifier for the protein
         * @param participant participant for this protein
         */
        protected FeatureHolder(String primaryIdentifier, DefaultModelledParticipant participant) {
            this.primaryIdentifier = primaryIdentifier;
            this.participant = participant;
        }

        /**
         * @param identifier primary identifier for the protein that resident protein is
         * interacting with
         * @param linkedFeature feature object created for this relationship
         */
        protected void addLinkedFeature(String identifier, DefaultModelledFeature linkedFeature) {
            binderToFeatures.put(identifier, linkedFeature);
        }

        /**
         * @param identifier primary identifier for other protein in this interaction
         * @return the feature associated with this binding protein
         */
        protected DefaultModelledFeature getLinkedFeature(String identifier) {
            return binderToFeatures.get(identifier);
        }

        /**
         * @return participant for this feature
         */
        protected DefaultModelledParticipant getParticipant() {
            return participant;
        }
    }
}
