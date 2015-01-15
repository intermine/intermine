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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

import psidev.psi.mi.jami.commons.MIDataSourceOptionFactory;
import psidev.psi.mi.jami.commons.MIFileType;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.datasource.InteractionStream;
import psidev.psi.mi.jami.factory.MIDataSourceFactory;
import psidev.psi.mi.jami.model.Alias;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.CausalRelationship;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Entity;
import psidev.psi.mi.jami.model.Interaction;
import psidev.psi.mi.jami.model.InteractionCategory;
import psidev.psi.mi.jami.model.Interactor;
import psidev.psi.mi.jami.model.ModelledParticipant;
import psidev.psi.mi.jami.model.Organism;
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
    private static final String COMPLEX_PROPERTIES = "complex-properties";
    private static final String INTERACTION_DETAIL = "InteractionDetail";
    private static final String INTERACTION_TYPE = "physical";
    // TODO put this in config file instead
    private static final String PROTEIN = "protein";
    private static final String GENE_ONTOLOGY = "go";
    // TODO types (protein and small molecules are processed now) are hardcoded.
    // maybe put this in config file? Or check model to see if type is legal?
    private static final Map<String, String> INTERACTOR_TYPES = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Set<String> taxonIds = null;
    private Map<String, String> interactors = new HashMap<String, String>();

    static {
        INTERACTOR_TYPES.put("MI:0326", "Protein");
        INTERACTOR_TYPES.put("MI:0328", "SmallMolecule");
    }


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

        Map<String, Object> parsingOptions = MIDataSourceOptionFactory.getInstance().getOptions(
                MIFileType.psimi_xml, InteractionCategory.complex, null, false, null, reader);

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
                    Interaction entry = (Interaction) interactionIterator.next();

                    // most of the interactions will have experimental data attached to them
                    // so they will be of type InteractionEvidence
                    if (entry instanceof Complex) {

                        Complex interactionEvidence = (Complex) entry;

                        Item complex = createItem("Complex");

                        String identifier = null; // get from xrefs
                        if (StringUtils.isNotEmpty(identifier)) {
                            complex.setAttribute("identifier", identifier);
                        }
                        complex.setAttribute("name", interactionEvidence.getShortName());

                        // parse annotations
                        processAnnotations(interactionEvidence, complex);

                        Item detail = createItem(INTERACTION_DETAIL);
                        detail.setAttribute("type", INTERACTION_TYPE);

                        // type, e.g. "physical association", "direct interaction"
                        processType(interactionEvidence, detail);

                        // parse participants
                        Set<String> proteins
                            = processParticipants(interactionEvidence, detail, complex);

                        // create interactions
                        createInteractions(proteins, detail, complex);

                        // parse GO terms
                        processXrefs(interactionEvidence, complex);

                        store(complex);
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

    private void createInteractions(Set<String> proteins, Item detail, Item complex)
        throws ObjectStoreException {
        if (!proteins.isEmpty()) {
            String interactor = proteins.iterator().next();
            proteins.remove(interactor);

            for (String protein : proteins) {
                Item interaction = createItem("Interaction");
                interaction.setReference("participant1", interactor);
                interaction.setReference("participant2", protein);
                Item detail1 = copyItem(detail, INTERACTION_DETAIL);
                detail1.setReference("interaction", interaction);
                store(detail1);
                interaction.setReference("complex", complex);
                store(interaction);

                interaction = createItem("Interaction");
                interaction.setReference("participant1", protein);
                interaction.setReference("participant2", interactor);
                Item detail2 = copyItem(detail, INTERACTION_DETAIL);
                detail2.setReference("interaction", interaction);
                store(detail2);
                complex.addToCollection("interactions", interaction);
                interaction.setReference("complex", complex);
                store(interaction);
            }
            createInteractions(proteins, detail, complex);
        }

    }

    private Set<String> processParticipants(Complex interactionEvidence,
            Item detail, Item complex) throws ObjectStoreException {

        Set<String> results = new HashSet<String>();

        for (ModelledParticipant evidence : interactionEvidence.getParticipants()) {

            Item interactor = createItem("Interactor");

            // annotations
            processAnnotations(evidence, interactor);

            // biological role
            setBiologicalRole(evidence, interactor);

            for (CausalRelationship rels : evidence.getCausalRelationships()) {
                CvTerm relationType = rels.getRelationType();
                Entity entity = rels.getTarget();
                Collection<Xref> xrefs = entity.getFeatures();
                // TODO what do I do now?
            }

            // protein
            String refId = processProtein(evidence, interactor);
            results.add(refId);

            // parse stoich
            processStoichiometry(evidence, interactor);

            // not parsing xrefs or aliases

            store(interactor);

            detail.addToCollection("allInteractors", interactor);
            complex.addToCollection("allInteractors", interactor);
        }

        return results;
    }


    private void setBiologicalRole(ModelledParticipant evidence, Item interactor)
        throws ObjectStoreException {
        CvTerm biologicalRole = evidence.getBiologicalRole();
        interactor.setReference("biologicalRole",
                getTerm("OntologyTerm", biologicalRole.getMIIdentifier()));
    }


    private String processProtein(ModelledParticipant evidence, Item interactor)
        throws ObjectStoreException {
        Interactor participant = evidence.getInteractor();

        Collection<Alias> xrefs = participant.getAliases();

        String accession = null;

        for (Alias alias : xrefs) {
            accession = alias.getName();
            System.out.println(accession);
        }

        System.out.println(" **** " + accession);

        String refId = interactors.get(accession);
        if (refId == null) {
            String typeTermIdentifier = participant.getInteractorType().getMIIdentifier();
            String interactorType = INTERACTOR_TYPES.get(typeTermIdentifier);
            if (interactorType == null) {
                // we don't know how to handle non-protein, non-small molecules
                return null;
            }
            Item protein = createItem(interactorType);
            if (PROTEIN.equals(typeTermIdentifier)) {
                protein.setAttribute("primaryAccession", accession);
            } else {
                protein.setAttribute("primaryIdentifier", accession);
            }
            Organism organism = participant.getOrganism();
            if (organism != null) {
                String organismRefId = getOrganism(String.valueOf(organism.getTaxId()));
                protein.setReference("organism", organismRefId);
            }
            store(protein);
            refId = protein.getIdentifier();
        }
        interactor.setReference("participant", refId);
        return refId;
    }

    private void processStoichiometry(ModelledParticipant evidence, Item interactor)
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

    private void processXrefs(Complex interactionEvidence,
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

    private void processType(Complex interactionEvidence,
            Item detail) throws ObjectStoreException {
        CvTerm cvterm = interactionEvidence.getInteractionType();
        String identifier = cvterm.getMIIdentifier();
        String refId = getTerm("InteractionTerm", identifier);
        detail.setReference("relationshipType", refId);
    }


    private void processAnnotations(ModelledParticipant evidence, Item interactor) {
        StringBuilder annotations = new StringBuilder();
        for (Annotation annotation : evidence.getAnnotations()) {
            annotations.append(annotation.getValue() + " ");
        }
        if (StringUtils.isNotEmpty(annotations.toString())) {
            interactor.setAttribute("annotations", annotations.toString());
        }
    }

    private void processAnnotations(Complex interactionEvidence, Item item) {
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
}
