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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Feature;
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
    private static final String DATASET_TITLE = "IntAct Complexes";
    private static final String DATA_SOURCE_NAME = "EBI IntAct";
    private static final String COMPLEX_PROPERTIES = "complex-properties";
    private static final String INTERACTION_TYPE = "physical";
    // TODO put this in config file instead
    private static final String PROTEIN = "protein";
    private static final String BINDING_SITE = "binding region";
    private static final String GENE_ONTOLOGY = "go";
    private static final String PUBMED = "pubmed";
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
    public void setPsiOrganisms(String taxonIds) {
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
                MIFileType.psimi_xml, InteractionCategory.complex, null, true, null, reader);

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

                        Xref xref = interactionEvidence.getPreferredIdentifier();
                        if (xref != null && StringUtils.isNotEmpty(xref.getId())) {
                            complex.setAttribute("identifier", xref.getId());
                        }
                        complex.setAttribute("name", interactionEvidence.getShortName());

                        // parse annotations
                        processAnnotations(interactionEvidence, complex);

                        DetailHolder detail = new DetailHolder();

                        // type, e.g. "physical association", "direct interaction"
                        processType(interactionEvidence, detail);

                        // parse participants and interactions
                        processInteractions(interactionEvidence, detail, complex);

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

    private void createInteractions(Map<String, Set<String>> featureToBindingRegions,
            DetailHolder detail, Item complex)
        throws ObjectStoreException {

        for (Map.Entry<String, Set<String>> entry : featureToBindingRegions.entrySet()) {

            String accession = entry.getKey();
            Set<String> bindingSites = entry.getValue();

            for (String bindingRegion : bindingSites) {
                Item interaction = createItem("Interaction");
                interaction.setReference("participant1", interactors.get(accession));
                interaction.setReference("participant2", interactors.get(bindingRegion));
                interaction.setReference("complex", complex);
                store(interaction);

                // TODO add relationship type and all interactors to detail
                Item detailItem = createItem("InteractionDetail");
                detailItem.setAttribute("type", INTERACTION_TYPE);
                detailItem.setReference("interaction", interaction);
                detailItem.setReference("relationshipType", detail.getRelationshipType());
                detailItem.setCollection("allInteractors", detail.getAllInteractors());
                store(detailItem);
            }
        }

    }

    private void processInteractions(Complex interactionEvidence,
            DetailHolder detail, Item complex) throws ObjectStoreException {

        Map<String, Set<String>> featureToBindingRegions = new HashMap<String, Set<String>>();

        for (ModelledParticipant modelledParticipant : interactionEvidence.getParticipants()) {

            Item interactor = createItem("Interactor");

            // annotations
            processAnnotations(modelledParticipant, interactor);

            // biological role
            setBiologicalRole(modelledParticipant, interactor);

            // protein
            ProteinHolder protein = processProtein(modelledParticipant, interactor);

            // not a protein or small molecule, skip for now
            if (protein == null) {
                return;
            }

            Set<String> bindingRegions = new HashSet<String>();

            // interactions
            for (Feature feature : modelledParticipant.getFeatures()) {
                Collection<Feature> linkedFeatures = feature.getLinkedFeatures();
                for (Feature linkedFeature : linkedFeatures) {
                    CvTerm term = linkedFeature.getType();
                    String type = term.getShortName();

                    // only create interactions if we have binding information
                    if (BINDING_SITE.equals(type)) {
                        String accession = linkedFeature.getParticipant()
                                .getInteractor().getPreferredIdentifier().getId();
                        bindingRegions.add(accession);
                    }
                }
            }
            featureToBindingRegions.put(protein.getAccession(), bindingRegions);

            // parse stoich
            processStoichiometry(modelledParticipant, interactor);

            // not parsing xrefs or aliases

            // interactor type - protein or small molecule
            setInteractorType(modelledParticipant, interactor);

            store(interactor);

            detail.addInteractor(interactor.getIdentifier());
            complex.addToCollection("allInteractors", interactor);
        }

        createInteractions(featureToBindingRegions, detail, complex);
    }


    private void setBiologicalRole(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        CvTerm biologicalRole = modelledParticipant.getBiologicalRole();
        interactor.setReference("biologicalRole",
                getTerm("OntologyTerm", biologicalRole.getMIIdentifier()));
    }

    private void setInteractorType(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        CvTerm interactorType = modelledParticipant.getInteractor().getInteractorType();
        interactor.setReference("type",
                getTerm("OntologyTerm", interactorType.getMIIdentifier()));
    }

    private ProteinHolder processProtein(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        Interactor participant = modelledParticipant.getInteractor();

        Xref xref = participant.getPreferredIdentifier();
        String accession = xref.getId();

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
            interactors.put(accession, refId);
        }
        interactor.setReference("participant", refId);
        return new ProteinHolder(accession, refId);
    }

    private void processStoichiometry(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        Stoichiometry stoichiometry = modelledParticipant.getStoichiometry();
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
            } else if (PUBMED.equalsIgnoreCase(dbTerm.getShortName())) {
                Item item = createItem("Publication");
                item.setAttribute("pubMedId", xrefId);
                store(item);
                complex.addToCollection("publications", item);
            }
        }
    }

    private void processType(Complex interactionEvidence,
            DetailHolder detail) throws ObjectStoreException {
        CvTerm cvterm = interactionEvidence.getInteractionType();
        String identifier = cvterm.getMIIdentifier();
        String refId = getTerm("InteractionTerm", identifier);
        detail.setRelationshipType(refId);
    }


    private void processAnnotations(ModelledParticipant modelledParticipant, Item interactor) {
        StringBuilder annotations = new StringBuilder();
        for (Annotation annotation : modelledParticipant.getAnnotations()) {
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

    private class ProteinHolder
    {
        private String refId;
        private String accession;

        protected ProteinHolder(String accession, String refId) {
            this.accession = accession;
            this.refId = refId;
        }

        protected String getAccession() {
            return accession;
        }

        protected String getRefId() {
            return refId;
        }

    }

    // temporary class to hold interaction details
    private class DetailHolder
    {
        protected String relationshipType;
        protected List<String> allInteractors;

        public DetailHolder() {
            allInteractors = new ArrayList<String>();
        }

        protected String getRelationshipType() {
            return relationshipType;
        }

        protected void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }

        protected List<String> getAllInteractors() {
            return allInteractors;
        }

        protected void addInteractor(String interactor) {
            allInteractors.add(interactor);
        }
    }
}
