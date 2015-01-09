package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

import psidev.psi.mi.jami.commons.MIDataSourceOptionFactory;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.datasource.InteractionStream;
import psidev.psi.mi.jami.factory.MIDataSourceFactory;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.CausalRelationship;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.Confidence;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Entity;
import psidev.psi.mi.jami.model.Experiment;
import psidev.psi.mi.jami.model.FeatureEvidence;
import psidev.psi.mi.jami.model.Interaction;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.model.Interactor;
import psidev.psi.mi.jami.model.Organism;
import psidev.psi.mi.jami.model.Parameter;
import psidev.psi.mi.jami.model.ParameterValue;
import psidev.psi.mi.jami.model.ParticipantEvidence;
import psidev.psi.mi.jami.model.Publication;
import psidev.psi.mi.jami.model.Stoichiometry;
import psidev.psi.mi.jami.model.Xref;


/**
 * Converter to parse complexes. May expand to handle others later.
 *
 * @author Julie Sullivan
 */
public class PsiComplexesConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "Complexes";
    private static final String DATA_SOURCE_NAME = "EBI IntAct";

    private static final String COMPLEX_FUNCTION = "curated-complex";
    private static final String COMPLEX_PROPERTIES = "complex-properties";
    private static final String GENE_ONTOLOGY = "go";

    private static final Logger LOG = Logger.getLogger(PsiComplexesConverter.class);
    private static final String PROP_FILE = "psi-complexes_config.properties";
    private Map<String, String> publications = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Set<String> taxonIds = null;
    private Map<String, String> interactors = new HashMap<String, String>();


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PsiComplexesConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // initialise default factories for reading and writing MITAB/PSI-MI XML files
        PsiJami.initialiseAllFactories();

        // reading MITAB and PSI-MI XML files

        // the option factory for reading files and other datasources
        MIDataSourceOptionFactory optionfactory = MIDataSourceOptionFactory.getInstance();
        // the datasource factory for reading MITAB/PSI-MI XML files and other datasources
        MIDataSourceFactory dataSourceFactory = MIDataSourceFactory.getInstance();


        // get default options for a file. It will identify if the file is MITAB or PSI-MI XML file
        // and then it will load the appropriate options.
        // By default, the datasource will be streaming (only returns an iterator of interactions),
        // and returns a source of Interaction objects.
        // The default options can be overridden using the optionfactory or by manually adding
        // options listed in MitabDataSourceOptions or PsiXmlDataSourceOptions
        Map<String, Object> parsingOptions = optionfactory.getDefaultOptions(reader);

        InteractionStream interactionSource = null;
        try {
            // Get the stream of interactions knowing the default options for this file
            interactionSource = dataSourceFactory.
                    getInteractionSourceWith(parsingOptions);

            // the option factory for reading files and other datasources
            MIWriterOptionFactory optionwriterFactory = MIWriterOptionFactory.getInstance();

            // parse the stream and write as we parse
            // the interactionSource can be null if the file is not recognized or the provided
            // options are not matching any existing/registered datasources
            if (interactionSource != null) {
                Iterator interactionIterator = interactionSource.getInteractionsIterator();

                while (interactionIterator.hasNext()) {
                    Interaction interaction = (Interaction) interactionIterator.next();

                    // most of the interactions will have experimental data attached to them
                    // so they will be of type InteractionEvidence
                    if (interaction instanceof InteractionEvidence) {
                        InteractionEvidence interactionEvidence = (InteractionEvidence) interaction;

                        Item complex = createItem("Complex");

                        String identifier = interactionEvidence.getImexId();
                        if (StringUtils.isNotEmpty(identifier)) {
                            complex.setAttribute("identifier", identifier);
                        }
                        complex.setAttribute("name", interactionEvidence.getShortName());

                        // parse annotations
                        processAnnotations(interactionEvidence, complex);

                        // parse confidences
                        processConfidences(interactionEvidence.getConfidences(), complex);

                        Item detail = createItem("InteractionDetail");

                        // parse experiment
                        processExperiment(interactionEvidence, complex, detail);

                        // type, going to be "physical"
                        processType(interactionEvidence, detail);

                        // parse parameters
                        processParameters(interactionEvidence.getParameters(), detail);

                        // parse participants
                        processParticipants(interactionEvidence, detail, complex);

                        // TODO what are these?
                        // interactionEvidence.getVariableParameterValues();

                        // parse GO terms
                        processXrefs(interactionEvidence, complex);

                        store(detail);
                        store(complex);

                    // modelled interactions are equivalent to abstractInteractions in PSI-MI XML
                    // 3.0. They are returned when the interaction is not an
                    // experimental interaction but a 'modelled' one extracted from any
                    // experimental context
//                    else if (interaction instanceof ModelledInteraction) {
//                        ModelledInteraction modelledInteraction
                    // = (ModelledInteraction) interaction;
//                        // process the modelled interaction
                    } else if (interaction instanceof Complex) {
                        // wait until 3.0
                    }
                }

            }
        } finally {
            // always close the opened interaction stream
            if (interactionSource != null) {
                interactionSource.close();
            }
        }
    }

    private void processParticipants(InteractionEvidence interactionEvidence,
            Item detail, Item complex) throws ObjectStoreException {
        for (ParticipantEvidence evidence : interactionEvidence.getParticipants()) {

            Item interactor = createItem("Interactor");

            // annotations
            parseAnnotations(evidence, interactor);

            // biological role
            setBiologicalRole(evidence, interactor);

            for (CausalRelationship rels : evidence.getCausalRelationships()) {
                CvTerm relationType = rels.getRelationType();
                Entity entity = rels.getTarget();
                Collection<Xref> xrefs = entity.getFeatures();
                // TODO what do I do now?
            }

            processConfidences(evidence.getConfidences(), interactor);

            // experimental prep
            setExperimentalPrep(evidence, interactor);

            // experimental role
            setExperimentalRole(evidence, interactor);

            // parse features
            for (FeatureEvidence feature : evidence.getFeatures()) {
                //feature.getParticipant();
                // TODO what now?
            }

            // participantIdentificationMethods
            processParticipantIdMethods(evidence, interactor);

            processProtein(evidence, interactor);

            processParameters(evidence.getParameters(), interactor);

            // parse stoich
            processStoichiometry(evidence, interactor);

            // not parsing xrefs or aliases

            store(interactor);

            detail.addToCollection("allInteractors", interactor);
            complex.addToCollection("allInteractors", interactor);
        }
    }

    private void processParticipantIdMethods(ParticipantEvidence evidence,
            Item interactor) throws ObjectStoreException {
        for (CvTerm idMethod : evidence.getIdentificationMethods()) {
            interactor.addToCollection("participantIdentificationMethods",
                    getTerm("OntologyTerm", idMethod.getMIIdentifier()));
        }
    }

    private void setExperimentalRole(ParticipantEvidence evidence,
            Item interactor) throws ObjectStoreException {
        CvTerm experimentalRole = evidence.getExperimentalRole();
        interactor.setReference("experimentalRole",
                getTerm("OntologyTerm", experimentalRole.getMIIdentifier()));
    }

    private void setExperimentalPrep(ParticipantEvidence evidence,
            Item interactor) throws ObjectStoreException {
        for (CvTerm prep : evidence.getExperimentalPreparations()) {
            interactor.addToCollection("experimentalPreparations",
                    getTerm("OntologyTerm", prep.getMIIdentifier()));
        }
    }

    private void setBiologicalRole(ParticipantEvidence evidence, Item interactor)
        throws ObjectStoreException {
        CvTerm biologicalRole = evidence.getBiologicalRole();
        interactor.setReference("biologicalRole",
                getTerm("OntologyTerm", biologicalRole.getMIIdentifier()));
    }

    private void parseAnnotations(ParticipantEvidence evidence, Item interactor) {
        StringBuilder annotations = new StringBuilder();
        for (Annotation annotation : evidence.getAnnotations()) {
            annotations.append(annotation.getValue() + " ");
        }
        if (StringUtils.isNotEmpty(annotations.toString())) {
            interactor.setAttribute("annotations", annotations.toString());
        }
    }

    private void processProtein(ParticipantEvidence evidence, Item interactor)
        throws ObjectStoreException {
        Interactor participant = evidence.getInteractor();
        Organism organism = participant.getOrganism();
        Xref xref = participant.getPreferredIdentifier();
        String accession = xref.getId();
        Item protein = createItem("Protein");
        protein.setAttribute("primaryAccession", accession);
        if (organism != null) {
            String organismRefId = getOrganism(String.valueOf(organism.getTaxId()));
            protein.setReference("organism", organismRefId);
        }
        store(protein);
        interactor.setReference("subject", protein);
        interactor.setReference("type", getTerm("OntologyTerm",
                participant.getInteractorType().getMIIdentifier()));
    }

    private void processStoichiometry(ParticipantEvidence evidence, Item interactor)
        throws ObjectStoreException {
        Stoichiometry stoichiometry = evidence.getStoichiometry();
        if (stoichiometry == null) {
            return;
        }
        Item stoichiometryItem = createItem("Stoichiometry");
        stoichiometryItem.setAttribute("min",
                String.valueOf(stoichiometry.getMinValue()));
        stoichiometryItem.setAttribute("max",
                String.valueOf(stoichiometry.getMaxValue()));
        store(stoichiometryItem);
        interactor.setReference("stoichiometry", stoichiometryItem);
    }

    private void processXrefs(InteractionEvidence interactionEvidence,
            Item complex) throws ObjectStoreException {
        for (Xref xref : interactionEvidence.getXrefs()) {
            CvTerm dbTerm = xref.getDatabase();
            String xrefId = xref.getId();
            CvTerm qualifierTerm = xref.getQualifier();
            // String version = xref.getVersion(); -- always null
            if (GENE_ONTOLOGY.equalsIgnoreCase(dbTerm.getShortName())) {
                String goterm = getTerm("GOTerm", xrefId);
                Item goAnnotation = createItem("GOAnnotation");
                if (qualifierTerm != null) {
                    goAnnotation.setAttribute("qualifier",
                            qualifierTerm.getShortName());
                }
                goAnnotation.setReference("ontologyTerm", goterm);
                store(goAnnotation);
                complex.addToCollection("goAnnotation", goAnnotation);
            }
        }
    }

    private void processExperiment(InteractionEvidence interactionEvidence,
            Item complex, Item detail) throws ObjectStoreException {
        Item item = createItem("InteractionExperiment");
        Experiment experiment = interactionEvidence.getExperiment();
        String hostOrganism = experiment.getHostOrganism().getScientificName();
        item.setAttribute("hostOrganism", hostOrganism);
        StringBuffer description = new StringBuffer();
        for (Annotation annotation : experiment.getAnnotations()) {
            description.append(annotation.getValue() + " ");
        }
        item.setAttribute("description", description.toString());
        processConfidences(experiment.getConfidences(), complex);
        CvTerm detectionMethod = experiment.getInteractionDetectionMethod();
        item.addToCollection("interactionDetectionMethods",
                getTerm("OntologyTerm", detectionMethod.getMIIdentifier()));
        Publication publication = experiment.getPublication();
        String pubMedId = publication.getPubmedId();
        item.setReference("publication", getPublication(pubMedId));
        experiment.getVariableParameters();
        store(item);
        detail.setReference("experiment", item);
    }


    private void processConfidences(Collection<Confidence> confidences,
            Item item) throws ObjectStoreException {
        for (Confidence entry : confidences) {
            String value = entry.getValue();
            CvTerm type = entry.getType();
            String refId = getTerm("OntologyTerm", type.getMIIdentifier());
            Item confidence = createItem("Confidence");
            confidence.setAttribute("value", value);
            confidence.setReference("type", refId);
            store(confidence);
            item.addToCollection("confidences", confidence);
        }
    }

    private void processParameters(Collection<Parameter> collection, Item item)
        throws ObjectStoreException {
        for (Parameter parameter : collection) {
            Item entry = createItem("InteractionParameter");
            String typeRefId = getTerm("OntologyTerm", parameter.getType().getMIIdentifier());
            entry.setReference("type", typeRefId);
            entry.setAttribute("uncertainty", String.valueOf(parameter.getUncertainty()));
            entry.setReference("unit", getTerm("OntologyTerm",
                    parameter.getUnit().getMIIdentifier()));
            entry.setReference("value", getParameterValue(parameter));
            store(entry);
            item.addToCollection("parameters", entry);
        }
    }

    private String getParameterValue(Parameter parameter)
        throws ObjectStoreException {
        ParameterValue parameterValue = parameter.getValue();
        Item value = createItem("ParameterValue");
        value.setAttribute("base",
                String.valueOf(parameterValue.getBase()));
        value.setAttribute("exponent",
                String.valueOf(parameterValue.getExponent()));
        value.setAttribute("factor",
                String.valueOf(parameterValue.getFactor()));
        store(value);
        return value.getIdentifier();
    }

    private void processType(InteractionEvidence interactionEvidence,
            Item detail) throws ObjectStoreException {
        CvTerm cvterm = interactionEvidence.getInteractionType();
        String identifier = cvterm.getMIIdentifier();
        String refId = getTerm("InteractionTerm", identifier);
        detail.setReference("relationshipType", refId);
    }

    private void processAnnotations(InteractionEvidence interactionEvidence,
            Item item) {
        StringBuffer complexProperties = new StringBuffer();
        StringBuffer complexFunction = new StringBuffer();

        Collection<Annotation> annotations = interactionEvidence.getAnnotations();
        for (Annotation annotation : annotations) {
            String value = annotation.getValue();
            CvTerm term = annotation.getTopic();
            String termName = term.getShortName();
            if (COMPLEX_PROPERTIES.equals(termName)) {
                complexProperties.append(value + " ");
            } else {
                complexFunction.append(value + " ");
            }
        }
        if (StringUtils.isNotEmpty(complexProperties.toString())) {
            item.setAttribute("properties", complexProperties.toString());
        }
        if (StringUtils.isNotEmpty(complexFunction.toString())) {
            item.setAttribute("function", complexFunction.toString());
        }
    }

    private String getTerm(String termType, String identifier) throws ObjectStoreException {
        String refId = terms.get(identifier);
        if (refId == null) {
            Item ontologyTerm = createItem(termType);
            ontologyTerm.setAttribute("identifier", identifier);
            store(ontologyTerm);
            refId = ontologyTerm.getIdentifier();
            terms.put(identifier, refId);
        }
        return refId;
    }

    private String getPublication(String pubMedId) throws ObjectStoreException {
        String refId = publications.get(pubMedId);
        if (refId == null) {
            Item item = createItem("Publication");
            item.setAttribute("pubMedId", pubMedId);
            store(item);
            refId = item.getIdentifier();
            publications.put(pubMedId, refId);
        }
        return refId;
    }
}

