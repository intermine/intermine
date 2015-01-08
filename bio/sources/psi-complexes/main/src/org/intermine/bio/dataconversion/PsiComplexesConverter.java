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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
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
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.Confidence;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Experiment;
import psidev.psi.mi.jami.model.Interaction;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.model.Parameter;
import psidev.psi.mi.jami.model.ParameterValue;
import psidev.psi.mi.jami.model.ParticipantEvidence;


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

    private static final Logger LOG = Logger.getLogger(PsiComplexesConverter.class);
    private static final String PROP_FILE = "psi-complexes_config.properties";
    private Map<String, String> pubs = new HashMap<String, String>();
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
    public void setIntactOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }

    /**
     *
     *
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

                        Item interactionItem = createItem("Complex");

                        String identifier = interactionEvidence.getImexId();
                        if (StringUtils.isNotEmpty(identifier)) {
                            interactionItem.setAttribute("identifier", identifier);
                        }
                        interactionItem.setAttribute("name", interactionEvidence.getShortName());

                        // parse annotations
                        processAnnotations(interactionEvidence, interactionItem);

                        // parse confidences
                        for (Confidence confidence : interactionEvidence.getConfidences()) {
                            String value = confidence.getValue();
                        }

                        Experiment experiment = interactionEvidence.getExperiment();
                        Item detail = createItem("InteractionDetail");

                        // type, going to be "physical"
                        processType(interactionEvidence, detail);

                        // parse parameters
                        processParameters(interactionEvidence, detail);

                        // parse participants
                        for (ParticipantEvidence evidence : interactionEvidence.getParticipants()) {
                            evidence.getAliases();
                            evidence.getAnnotations();
                            evidence.getBiologicalRole();
                            evidence.getCausalRelationships();
                            evidence.getConfidences();
                            evidence.getExperimentalPreparations();
                            evidence.getExperimentalRole();
                            evidence.getFeatures();
                            evidence.getIdentificationMethods();
                            evidence.getInteractor();
                            evidence.getParameters();
                            evidence.getStoichiometry();
                            evidence.getXrefs();
                        }

                        interactionEvidence.getVariableParameterValues();

                        interactionEvidence.getXrefs();

                        store(detail);
                        store(interactionItem);

                    // modelled interactions are equivalent to abstractInteractions in PSI-MI XML
                    // 3.0. They are returned when the interaction is not an
                    // experimental interaction but a 'modelled' one extracted from any
                    // experimental context
//                    else if (interaction instanceof ModelledInteraction) {
//                        ModelledInteraction modelledInteraction
                    // = (ModelledInteraction) interaction;
//                        // process the modelled interaction
                    } else if (interaction instanceof Complex) {
                        Complex complex = (Complex) interaction;

                        Item item = createItem("Complex");
                        item.setAttribute("name", complex.getRecommendedName());
                        item.setAttribute("systemicName", complex.getSystematicName());
                        item.setAttribute("properties", complex.getPhysicalProperties());
                        store(item);
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

    private void processParameters(InteractionEvidence interactionEvidence,
            Item detail) throws ObjectStoreException {
        for (Parameter parameter : interactionEvidence.getParameters()) {
            Item parameterItem = createItem("InteractionParameter");

            String typeRefId = getTerm("OntologyTerm", parameter.getType().getMIIdentifier());
            parameterItem.setReference("type", typeRefId);

            parameterItem.setAttribute("uncertainty",
                    String.valueOf(parameter.getUncertainty()));

            parameterItem.setReference("unit", getUnit(parameter));

            parameterItem.setReference("value", getParameterValue(parameter));

            store(parameterItem);

            detail.addToCollection("parameters", parameterItem);
        }
    }

    // TODO need OBO file
    private String getUnit(Parameter parameter) throws ObjectStoreException {
        Item unitTerm = createItem("OntologyTerm");
        unitTerm.setIdentifier(parameter.getUnit().getMIIdentifier());
        store(unitTerm);
        return unitTerm.getIdentifier();
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
        item.setAttribute("properties", complexProperties.toString());
        item.setAttribute("function", complexFunction.toString());
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
}
