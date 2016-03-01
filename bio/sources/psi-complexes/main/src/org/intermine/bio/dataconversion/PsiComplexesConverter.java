package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
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
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Feature;
import psidev.psi.mi.jami.model.Interaction;
import psidev.psi.mi.jami.model.InteractionCategory;
import psidev.psi.mi.jami.model.Interactor;
import psidev.psi.mi.jami.model.ModelledParticipant;
import psidev.psi.mi.jami.model.Organism;
import psidev.psi.mi.jami.model.Position;
import psidev.psi.mi.jami.model.Range;
import psidev.psi.mi.jami.model.Stoichiometry;
import psidev.psi.mi.jami.model.Xref;
import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;


/**
 * Converter to parse complexes. May expand to handle others later.
 *
 * @author Julie Sullivan
 */
public class PsiComplexesConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(PsiComplexesConverter.class);
    private static final String DATASET_TITLE = "IntAct Complexes";
    private static final String DATA_SOURCE_NAME = "EBI IntAct";
    private static final String COMPLEX_PROPERTIES = "complex-properties";
    private static final String INTERACTION_TYPE = "physical";
    private static final String DEFAULT_INTERACTOR_TYPE = "BioEntity";
    // TODO put this in config file instead
    private static final String PROTEIN = "MI:0326";
    private static final String SMALL_MOLECULE = "MI:0328";
    private static final String BINDING_SITE = "binding region";
    private static final String DIRECT_BINDING = "direct binding";
    private static final String GENE_ONTOLOGY = "go";
    private static final String PUBMED = "pubmed";
    private static final String EBI = "intact";
    private String xrefDatabase;
    private static final String COMPLEX_NAME = "complex recommended name";
    // TODO types (protein and small molecules are processed now) are hardcoded.
    // maybe put this in config file? Or check model to see if type is legal?
    private static final Map<String, String> INTERACTOR_TYPES = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();
    // accession to stored object ID
    private Map<String, String> interactors = new HashMap<String, String>();
    private Map<String, String> publications = new HashMap<String, String>();

    // See #1168
    static {
        INTERACTOR_TYPES.put("MI:0326", "Protein");
        INTERACTOR_TYPES.put("MI:0328", "SmallMolecule");
        INTERACTOR_TYPES.put("MI:0320", "RNA");
        INTERACTOR_TYPES.put("MI:0609", "SnoRNA");
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
     * Sets the database, e.g. SGD, from which we should get the identifier
     *
     * @param identifierSource the data source for the identifier
     */
    public void setComplexesSource(String identifierSource) {
        xrefDatabase = identifierSource;
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

                        processIdentifiers(interactionEvidence, complex);

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

    private void processIdentifiers(Complex interactionEvidence, Item complex) {
        String identifier = getComplexIdentifier(interactionEvidence);
        if (StringUtils.isNotEmpty(identifier)) {
            complex.setAttribute("identifier", identifier);
        }
        String systematicName = interactionEvidence.getSystematicName();
        if (StringUtils.isNotEmpty(systematicName)) {
            complex.setAttribute("systematicName", systematicName);
        }
        Collection<Alias> aliases = interactionEvidence.getAliases();
        for (Alias alias : aliases) {
            CvTerm type = alias.getType();
            if (COMPLEX_NAME.equals(type.getShortName())) {
                complex.setAttribute("name", alias.getName());
            }
        }
    }

    private void processInteractions(Complex interactionEvidence,
            DetailHolder detail, Item complex) throws ObjectStoreException {
        for (ModelledParticipant modelledParticipant : interactionEvidence.getParticipants()) {
            Item interactor = createItem("Interactor");

            // annotations
            processAnnotations(modelledParticipant, interactor);

            // biological role
            setBiologicalRole(modelledParticipant, interactor);

            // protein
            String refId = processProtein(modelledParticipant.getInteractor());

            // not a protein or small molecule, skip for now
            if (refId == null) {
                return;
            }
            interactor.setReference("participant", refId);

            // interactions and regions
            for (Feature feature : modelledParticipant.getFeatures()) {
                Collection<Feature> linkedFeatures = feature.getLinkedFeatures();
                for (Feature linkedFeature : linkedFeatures) {
                    CvTerm term = linkedFeature.getType();
                    String type = term.getShortName();
                    // only create interactions if we have binding information
                    if (BINDING_SITE.equals(type) || DIRECT_BINDING.equals(type)) {
                        String binderRefId = processProtein(linkedFeature.getParticipant()
                                .getInteractor());

                        Item interaction = createItem("Interaction");
                        interaction.setReference("participant1", refId);
                        interaction.setReference("participant2", binderRefId);
                        interaction.setReference("complex", complex);
                        store(interaction);
                        interactor.addToCollection("interactions", interaction);

                        Item detailItem = createItem("InteractionDetail");
                        detailItem.setAttribute("type", INTERACTION_TYPE);
                        detailItem.setAttribute("relationshipType", detail.getRelationshipType());
                        detailItem.setReference("interaction", interaction);
                        detailItem.setCollection("allInteractors", detail.getAllInteractors());

                        processRegions(linkedFeature.getRanges(), detailItem, refId, binderRefId);
                        store(detailItem);
                    }
                }
            }

            // parse stoich
            processStoichiometry(modelledParticipant, interactor);

            // not parsing xrefs or aliases

            // interactor type - protein or small molecule
            setInteractorType(modelledParticipant, interactor);

            store(interactor);

            detail.addInteractor(interactor.getIdentifier());
            complex.addToCollection("allInteractors", interactor);
        }
    }

    private void processRegions(Collection<Range> ranges, Item detail, String feature,
        String locatedOn)
        throws ObjectStoreException {
        for (Range range : ranges) {
            Item location = createItem("Location");
            Position startPosition = range.getStart();
            Position endPosition = range.getEnd();
            Long start = startPosition.getStart();
            Long end = endPosition.getStart();
            location.setAttribute("start", String.valueOf(start));
            location.setAttribute("end", String.valueOf(end));
            location.setReference("locatedOn", locatedOn);
            location.setReference("feature", feature);
            store(location);
            Item region = createItem("InteractionRegion");
            region.addToCollection("locations", location);
            region.setReference("interaction", detail);
            store(region);
        }
    }


    private void setBiologicalRole(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        CvTerm biologicalRole = modelledParticipant.getBiologicalRole();
        String termName = biologicalRole.getFullName();
        interactor.setAttribute("biologicalRole", termName);
    }

    private void setInteractorType(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        CvTerm interactorType = modelledParticipant.getInteractor().getInteractorType();
        String termName = interactorType.getFullName();
        interactor.setAttribute("type", termName);
    }

    private String getComplexIdentifier(Complex complex) {
        Collection<Xref> xrefs = complex.getIdentifiers();
        for (Xref xref : xrefs) {
            CvTerm cvTerm = xref.getDatabase();
            if (EBI.equalsIgnoreCase(cvTerm.getShortName())) {
                return xref.getId();
            }
        }
        return null;
    }

    private String processProtein(Interactor participant)
        throws ObjectStoreException {

        String accession, primaryIdentifier = null;
        for (Xref xref : participant.getXrefs()) {
            if (xrefDatabase.equalsIgnoreCase(xref.getDatabase().getShortName())) {
                primaryIdentifier = xref.getId();
            }
        }
        Xref xref = participant.getPreferredIdentifier();
        String originalAccession = xref.getId();
        boolean createSynonym = false;
        // Chop off the PRO ontology, we aren't using it yet
        // P00424-PRO0000006097, P00425-PRO0000006098, P00427-PRO_0000006108
        if (originalAccession.contains("-")) {
            accession = originalAccession.substring(0, originalAccession.indexOf("-"));
            createSynonym = true;
        } else {
            accession = originalAccession;
        }
        if (StringUtils.isEmpty(primaryIdentifier)) {
            // if no SGD identifier, eg. for small molecules, accession and identifier are equal
            primaryIdentifier = accession;
        }
        String refId = interactors.get(primaryIdentifier);
        if (refId == null) {
            String typeTermIdentifier = participant.getInteractorType().getMIIdentifier();
            String interactorType = INTERACTOR_TYPES.get(typeTermIdentifier);
            if (interactorType == null) {
                // see #1168
                LOG.error("Unknown interactor type: " + typeTermIdentifier);
                interactorType = DEFAULT_INTERACTOR_TYPE;
            }
            Item protein = createItem(interactorType);
            protein.setAttribute("primaryIdentifier", primaryIdentifier);
            if (PROTEIN.equals(typeTermIdentifier)) {
                protein.setAttribute("primaryAccession", accession);
            } else if (SMALL_MOLECULE.equals(typeTermIdentifier)) {
                String smallMolecule = getChebiName(primaryIdentifier);
                if (StringUtils.isNotEmpty(smallMolecule)) {
                    protein.setAttribute("name", smallMolecule);
                }
            }
            Organism organism = participant.getOrganism();
            if (organism != null) {
                String organismRefId = getOrganism(String.valueOf(organism.getTaxId()));
                protein.setReference("organism", organismRefId);
            }
            store(protein);
            refId = protein.getIdentifier();
            interactors.put(primaryIdentifier, refId);
        }
        if (createSynonym) {
            createSynonym(refId, originalAccession, true);
            String proIdentifier = originalAccession.substring(originalAccession.indexOf("-") + 1);
            createSynonym(refId, proIdentifier, true);
        }
        return refId;
    }

    private String getChebiName(String identifier) {
        try {
            ChebiWebServiceClient client = new ChebiWebServiceClient();
            Entity entity = client.getCompleteEntity(identifier);
            return entity.getChebiAsciiName();
        } catch (ChebiWebServiceFault_Exception e) {
            LOG.warn(e.getMessage());
        }
        return null;
    }

    private void processStoichiometry(ModelledParticipant modelledParticipant, Item interactor)
        throws ObjectStoreException {
        Stoichiometry stoichiometry = modelledParticipant.getStoichiometry();
        if (stoichiometry == null) {
            return;
        }
        interactor.setAttribute("stoichiometry", String.valueOf(stoichiometry.getMaxValue()));
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
                    goAnnotation.setAttribute("qualifier", qualifierTerm.getShortName());
                }
                goAnnotation.setReference("ontologyTerm", goterm);
                store(goAnnotation);
                complex.addToCollection("goAnnotation", goAnnotation);
            } else if (PUBMED.equalsIgnoreCase(dbTerm.getShortName())) {
                complex.addToCollection("publications", getPublication(xrefId));
            }
        }
    }

    private void processType(Complex interactionEvidence,
            DetailHolder detail) throws ObjectStoreException {
        CvTerm cvterm = interactionEvidence.getInteractionType();
        String termName = cvterm.getFullName();
        detail.setRelationshipType(termName);
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

    // temporary class to hold interaction details
    private class DetailHolder
    {
        protected String relationshipType;
        protected List<String> allInteractors;

        protected DetailHolder() {
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
