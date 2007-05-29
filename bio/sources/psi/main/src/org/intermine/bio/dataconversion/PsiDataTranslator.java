package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.InterMineException;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * DataTranslator specific to Protein Interaction data in PSI XML format.
 *
 * @author Richard Smith
 * @author Andrew Varley
 * @author Peter Mclaren - Modifications to cater for complex interactions
 */


public class PsiDataTranslator extends DataTranslator
{
    private Item dataSource;
    private Map pubs = new HashMap();
    private Map dataSetMap = new HashMap();
    private Set organisms = new HashSet();
    private Set exptsToStore = new HashSet();
    private Map exptMap = new HashMap();

    protected static final Logger LOG = Logger.getLogger(PsiDataTranslator.class);

    /**
     * @see DataTranslator#DataTranslator(ItemReader, Properties, Model, Model)
     */
    public PsiDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                             Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
    }

    /**
     * A space separated list of of NCBI taxonomy ids for which we want to retrieve
     * interactions.
     * @param orgStr a list of taxon ids
     */
    public void setOrganisms(String orgStr) {
        organisms.addAll(Arrays.asList(orgStr.split("\\s")));
    }

    /**
     * @see DataTranslator#translate(ItemWriter)
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        if (organisms.isEmpty()) {
            throw new IllegalArgumentException("Must specify a list of oganisms to accept"
                                               + " interactions from.");
        }

        dataSource = createItem("DataSource");
        dataSource.addAttribute(new Attribute("name", "IntAct"));
        tgtItemWriter.store(ItemHelper.convert(dataSource));

        super.translate(tgtItemWriter);

        Iterator iter = dataSetMap.values().iterator();

        while (iter.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) iter.next()));
        }

        iter = exptsToStore.iterator();
        while (iter.hasNext()) {
            tgtItemWriter.storeAll(ItemHelper
                         .convertToFullDataItems((List) exptMap.get(iter.next())));
        }
    }

    /**
     * @see DataTranslator#translateItem(Item srcItem)
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        boolean storeTgtItem = true;
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                Item tgtItem = (Item) i.next();
                if ("ExperimentType".equals(className)) {
                    storeTgtItem = false;
                    // TODO we need to check that there is anything to store in this experiment,
                    // we only want those that contain interactions for selected organisms

                    // put experiment and associated data in a exptMap, exptsToStore will
                    // define identifiers of experiments that should be stored.

                    List exptData = new ArrayList();
                    exptMap.put(tgtItem.getIdentifier(), exptData);
                    exptData.add(tgtItem);
                    Item dataSetItem =
                        getDataSetFromNamesType(getReference(srcItem, "names"));
                    Item pub = getPub(srcItem);
                    if (pub != null) {
                        tgtItem.addReference(new Reference("publication", pub.getIdentifier()));
                        //result.add(pub);
                        exptData.add(pub);
                    }
                    Item attributeList = getReference(srcItem, "attributeList");
                    if (attributeList != null) {
                        for (Iterator j = getCollection(attributeList, "attributes");
                             j.hasNext();) {
                            Item attribute = (Item) j.next();
                            Item comment = createItem("Comment");
                            if (attribute.hasAttribute("name")) {
                                comment.addAttribute(new Attribute("type",
                                        attribute.getAttribute("name").getValue()));
                            }
                            if (attribute.hasAttribute("attribute")) {
                                comment.addAttribute(new Attribute("text",
                                        attribute.getAttribute("attribute").getValue()));
                            }

                            comment.addReference(new Reference("source",
                                     dataSetItem.getIdentifier()));
                            //result.add(comment);
                            exptData.add(comment);
                            addToCollection(tgtItem, "comments", comment);
                        }
                    }

                    Item hostOrganismItem = getReference(srcItem, "hostOrganism");
                    if (hostOrganismItem != null) {
                        String hostOrgFullName  = findNameInNamesList(hostOrganismItem, "fullName");
                        if (hostOrgFullName != null) {
                            tgtItem.setAttribute("hostOrganism", hostOrgFullName);
                            LOG.debug("PIE.hostOrganism:" + hostOrgFullName);
                        } else {
                            LOG.warn("NO FULLNAME FOUND FOR THIS HOST_ORGANISM!");
                        }
                    } else {
                        LOG.warn("NO HOST_ORGANISM FOUND FOR SRC_ITEM:" + srcItem.getIdentifier());
                    }

                } else if ("InteractionElementType".equals(className)) {
                    storeTgtItem = false;
                    Item exptType = (Item) getCollection(getReference(srcItem, "experimentList"),
                                                        "experimentRefs").next();
                    addReferencedItem(tgtItem, exptType, "analysis", false, "", false);

                    // get: tgt.experimentList.experimentRefs[0].names.shortLabel
                    // and  tgt.experimentList.experimentRefs[0].names.fullName
                    Iterator experimentRefsIter =
                        getCollection(getReference(srcItem, "experimentList"), "experimentRefs");
                    Item namesType = getReference((Item) experimentRefsIter.next(), "names");
                    Item dataSet = getDataSetFromNamesType(namesType);
                    tgtItem.setReference("source", dataSet);
                    // set confidence from attributeList
                    //TODO: This is a deprecated representation of the Confidence data.
                    if (srcItem.hasReference("attributeList")) {
                        for (Iterator j = getCollection(getReference(srcItem, "attributeList"),
                                                        "attributes"); j.hasNext();) {
                            Item attrItem = (Item) j.next();
                            Attribute valueAttr = attrItem.getAttribute("attribute");
                            Attribute nameAttr = attrItem.getAttribute("name");
                            if (valueAttr != null && nameAttr != null) {
                                String value = valueAttr.getValue().trim();
                                String name = nameAttr.getValue().trim();
                                if (Character.isDigit(value.charAt(0))
                                    && name.equals("author-confidence")) {
                                    tgtItem.addAttribute(new Attribute("confidence", value));
                                } else if (name.equals("author-confidence")) {
                                    //If we have some text instead of a numerical value...
                                    tgtItem.addAttribute(new Attribute("confidenceDesc", value));
                                }
                            } else {
                                LOG.debug("Skipped an Attribute - looking for 'author-confidence'");
                            }
                        }
                    }
                    //NEW WAY OF REPRESENTING CONFIDENCE SCORES.
                    if (srcItem.hasReference("confidence")) {
                        Item confItem = getReference(srcItem, "confidence");
                        if (confItem.hasAttribute("unit") && confItem.hasAttribute("value")) {

                            Attribute unit  = confItem.getAttribute("unit");
                            Attribute value = confItem.getAttribute("value");

                            if (Character.isDigit(value.getValue().charAt(0))
                                && unit.getValue().equals("author-confidence")) {
                                tgtItem.addAttribute(
                                        new Attribute("confidence", value.getValue()));

                            } else if (unit.getValue().equals("author-confidence")) {
                                //If we have some text instead of a numerical value...
                                tgtItem.addAttribute(
                                        new Attribute("confidenceDesc", value.getValue()));
                            }
                        } else {
                            LOG.debug("Found a new confidence item without both a unit & a value!");
                        }
                    }

                    // if createProteinInteraction returns null it means it has been rejected
                    Item interaction = createProteinInteraction(srcItem, tgtItem, result, dataSet);
                    if (interaction != null) {
                        storeTgtItem = true;
                        interaction.addReference(new Reference("experiment",
                                                               exptType.getIdentifier()));

                        interaction.addCollection(new ReferenceList("evidence",
                            Arrays.asList(new Object[] {dataSource.getIdentifier()})));

                        addReferencedItem(tgtItem, interaction, "relations", true,
                                          "evidence", true);
                        result.add(interaction);

                        // store this experiment
                        exptsToStore.add(exptType.getIdentifier());
                    }
                } else if ("ProteinInteractorType".equals(className)) {
                    // only store if this protein is for an accepted organism
                    Item organism = getReference(srcItem, "organism");
                    String taxId = organism.getAttribute("ncbiTaxId").getValue();

                    // TODO interactions are only stored if they contain at least two interactors
                    // from accepted organisms.  Proteins are only stored if they are from an
                    // an accpted organism.  In the case that an interaction is between one
                    // accepted and one not accepted then an 'orphan' protein could be stored.
                    // This is not too serious but a little untidy.

                    if (organisms.contains(taxId)) {
                        Item xref = getReference(srcItem, "xref");
                        Item primDbXref = (xref != null ? getReference(xref, "primaryRef") : null);
                        Attribute dbAttr = (primDbXref != null
                                            ? primDbXref.getAttribute("db") : null);
                        String dbAttrStr = (dbAttr != null ? dbAttr.getValue() : "");
                        if ("uniprotkb".equals(dbAttrStr) || "uniprot".equals(dbAttrStr)) {
                            String value = primDbXref.getAttribute("id").getValue();
                            // accessions like P14734-1 are isoform identifiers, remove the '-n'
                            // to get back to main protein id
                            if (value.indexOf("-") > 0) {
                                value = value.substring(0, value.indexOf("-"));
                            }
                            tgtItem.addAttribute(new Attribute("primaryAccession", value));
                            Item synonym = createItem("Synonym");
                            addReferencedItem(synonym, dataSource, "source", false, "", false);
                            synonym.addAttribute(new Attribute("value", value));
                            synonym.addAttribute(new Attribute("type", "accession"));
                            addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                            result.add(synonym);
                        } else {
                            //Since there's no usable Uniprot id, just use the Intact internal id.
                            if (xref.hasCollection("secondaryRefs")) {
                                Iterator secondaryDbXrefIt = getCollection(xref, "secondaryRefs");

                                boolean foundIntact = false;
                                String intactId = null;
                                while (secondaryDbXrefIt.hasNext() && !foundIntact) {

                                    Item nextDbXref = (Item) secondaryDbXrefIt.next();
                                    Attribute nextDbAttr = nextDbXref.getAttribute("db");
                                    if (nextDbAttr.getValue().equalsIgnoreCase("intact")) {

                                        Attribute idAttr = nextDbXref.getAttribute("id");
                                        intactId = idAttr.getValue();
                                        foundIntact = true;
                                    }
                                }

                                if (foundIntact && intactId != null) {
                                    tgtItem.addAttribute(new Attribute("primaryAccession",
                                            "IntAct:" + intactId));
                                    Item synonym = createItem("Synonym");
                                    addReferencedItem(synonym, dataSource, "source",
                                                      false, "", false);
                                    synonym.addAttribute(new Attribute("value",
                                                         "IntAct:" + intactId));
                                    synonym.addAttribute(new Attribute("type",
                                                         "identifier"));
                                    addReferencedItem(tgtItem, synonym, "synonyms",
                                                      true, "subject", false);
                                    result.add(synonym);
                                    Item idSynonym = createItem("Synonym");
                                    addReferencedItem(idSynonym, dataSource, "source",
                                                      false, "", false);
                                    idSynonym.addAttribute(new Attribute("value", intactId));
                                    idSynonym.addAttribute(new Attribute("type", "identifier"));
                                    addReferencedItem(
                                       tgtItem, idSynonym, "synonyms", true, "subject", false);
                                    result.add(idSynonym);

                                } else {
                                    throw new RuntimeException("Can't set a suitable "
                                       + "primaryAccession for this ProteinInteractor");
                                }
                            }
                        }

                        if (srcItem.getAttribute("sequence") != null) {
                            Item seq = createItem("Sequence");
                            String srcResidues = srcItem.getAttribute("sequence").getValue();
                            seq.setAttribute("residues", srcResidues);
                            seq.setAttribute("length", "" + srcResidues.length());
                            tgtItem.addReference(new Reference("sequence", seq.getIdentifier()));
                            result.add(seq);
                        }
                    } else {
                        storeTgtItem = false;
                    }
                } else if ("CvType".equals(className)) {
                    Item xref = getReference(srcItem, "xref");
                    Item primaryRef = getReference(xref, "primaryRef");
                    if (primaryRef.getAttribute("db").getValue().equalsIgnoreCase("psi-mi")) {
                        tgtItem.addAttribute(new Attribute("identifier",
                                primaryRef.getAttribute("id").getValue()));
                    }
                } else if ("Organism_ProteinInteractorType".equals(className)) {
                    String taxonId = srcItem.getAttribute("ncbiTaxId").getValue();
                    if (!organisms.contains(taxonId)) {
                        storeTgtItem = false;
                    }
                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        }
        return result;
    }

    private Item getDataSetFromNamesType(Item namesTypeItem) {
        String shortName = namesTypeItem.getAttribute("shortLabel").getValue();

        Attribute fullNameAttr = namesTypeItem.getAttribute("fullName");

        Item dataSetItem;

        if (dataSetMap.containsKey(shortName)) {
            dataSetItem = (Item) dataSetMap.get(shortName);
        } else {
            dataSetItem = createItem("DataSet");
            dataSetItem.addAttribute(new Attribute("title", shortName));
            if (fullNameAttr != null) {
                dataSetItem.addAttribute(new Attribute(
                            "description", fullNameAttr.getValue()));
            } else {
                LOG.debug("NO FULLNAME ATTR FOUND FOR THIS SHORTNAME:" + shortName);
            }
            dataSetItem.setReference("dataSource", dataSource);
            dataSetMap.put(shortName, dataSetItem);
        }

        return dataSetItem;
    }
    /**
     * @param srcInteractionElementItem = InteractionElementType

    */
    private Item createProteinInteraction(
            Item srcInteractionElementItem, Item tgtExperimentalResult, Collection result,
            Item dataSetItem)
        throws ObjectStoreException {

        // we only want to create an interaction between proteins of the same organism
        // and that are for organisms we are interested in.
        Item interaction = createItem("ProteinInteraction");

        Item participants = getReference(srcInteractionElementItem, "participantList");
        List interactionOrganisms = new ArrayList();
        Set interactors = new HashSet();

        for (Iterator i = getCollection(participants, "proteinParticipants"); i.hasNext();) {
            Item participant = (Item) i.next();

            if (getReference(participant, "featureList") != null) {
                createProteinRegion(interaction, participant, result, dataSetItem);
            }

            Attribute itemRole = participant.getAttribute("role");
            String role;

            if (itemRole != null) {
                role = itemRole.getValue();
            } else {
                role = "unspecifed";
            }

            Item interactor = createItem("ProteinInteractor");
            interactor.setAttribute("role", role);

            // check the protein organism
            Item protein = getReference(participant, "proteinInteractorRef");
            Item organism = getReference(protein, "organism");
            String taxonId = organism.getAttribute("ncbiTaxId").getValue();
            if (organisms.contains(taxonId)) {
                interactionOrganisms.add(taxonId);
                Reference proteinRef = new Reference("protein",
                          participant.getReference("proteinInteractorRef").getRefId());
                interactor.setReference("protein", proteinRef.getRefId());
                interactor.setReference("interaction", interaction.getIdentifier());
                interaction.addToCollection("interactors", interactor);
                interactors.add(interactor);
            }

            //result.add(interactor);
        }

        // more than protein from accepted organisms -> keep
        if (interactionOrganisms.size() > 1) {
            result.addAll(interactors);
            String iShortName = findNameInNamesList(srcInteractionElementItem, "shortLabel");
            if (iShortName != null) {
                interaction.setAttribute("shortName", iShortName);
                LOG.debug("INTERACTION.SHORTNAME WAS SET AS:" + iShortName);
            } else {
                LOG.debug("No names reference for srcItem "
                          + srcInteractionElementItem.getIdentifier());
            }

            //<confidence unit="author-confidence" value="D"/>
            Item conf = getReference(srcInteractionElementItem, "confidence");

            if (conf != null) {
                LOG.debug("CONFIDENCE TAG FOUND IN INTERACTION:"
                          + (iShortName != null ? iShortName : interaction.getIdentifier()));

                tgtExperimentalResult.addAttribute(
                    new Attribute("confidenceUnit", conf.getAttribute("unit").getValue()));

                tgtExperimentalResult.addAttribute(
                    new Attribute("confidenceValue", conf.getAttribute("value").getValue()));
            } else {
                LOG.debug("NO CONFIDENCE TAG FOUND IN INTERACTION:"
                          + (iShortName != null ? iShortName : interaction.getIdentifier()));
            }

            return interaction;
        }

        // make sure this interaction has at least two interacting proteins from an
        // accepted organism
//         String keepInteraction = false;
//         Iteractor orgIter = oranisms.iterator();
//         while (orgIter.hasNext()) {
//             String nextOrg = orgIter.next();
//             if (interactionOrgs.contains(nextOrg)) {
//                 if (((Integer) interactionOrgs.get(nextOrg)).parseInt() > 1) {
//                     keepInteraction = true;
//                 }
//             }
//         }

        // we can store this interaction

        // experiment can be stored

        // we don't want to store this interaction
        return null;
    }

    /**
     * Iterate over the names list in the supplied source item
     * (which can come from many parts of the data file) in order to
     * find the supplied name type - eg fullName or shortLabel etc etc
     *
     * @return The located name as a String or as a null.
     * */
    private String findNameInNamesList(Item itemWithSomeNames, String nameType)
            throws ObjectStoreException {

        if (itemWithSomeNames == null) {
            LOG.debug("An 'itemWithSomeNames' is null!");
            return null;
        }

        Reference namesRef = itemWithSomeNames.getReference("names");

        if (namesRef == null) {
            LOG.debug("An 'itemWithSomeNames' has no 'names'!"
                      + itemWithSomeNames.getIdentifier());
            return null;
        }

        org.intermine.model.fulldata.Item namesItem =
                this.srcItemReader.getItemById(namesRef.getRefId());

        boolean shortLabelFound = false;
        String iShortName = null;

        for (Iterator nameIt = namesItem.getAttributes().iterator();
             nameIt.hasNext() && !shortLabelFound;) {

            org.intermine.model.fulldata.Attribute nextNameAttr
                    = (org.intermine.model.fulldata.Attribute) nameIt.next();
            if (nameType.equalsIgnoreCase(nextNameAttr.getName())) {
                iShortName = nextNameAttr.getValue();
                shortLabelFound = true;
            }
        }
        return iShortName;
    }

    private void createProteinRegion(Item interaction, Item participant, Collection result,
                                     Item dataSetItem)
        throws ObjectStoreException {
        Item featureList = getReference(participant, "featureList");
        Item feature = (Item) getCollection(featureList, "features").next();
        Item featureDescription = getReference(feature, "featureDescription");
        Item xref = getReference(featureDescription, "xref");
        Item primaryRef = getReference(xref, "primaryRef");

        //MI:0117 = 'Binding Site'
        if ("MI:0117".equals(primaryRef.getAttribute("id").getValue())) {
            Item location = getReference(feature, "location");
            Item tgtProteinRegion = createItem("ProteinRegion");
            tgtProteinRegion.addReference(new Reference("protein", participant
                                                        .getReference("proteinInteractorRef")
                                                        .getRefId()));
            Item tgtLocation = createItem("Location");
            tgtLocation.addAttribute(new Attribute("start",
                                                   getReference(location, "begin")
                                                   .getAttribute("position").getValue()));
            tgtLocation.addAttribute(new Attribute("end", getReference(location, "end")
                                                   .getAttribute("position").getValue()));
            tgtLocation.addReference(new Reference("object", participant
                                                   .getReference("proteinInteractorRef")
                                                   .getRefId()));
            tgtLocation.addReference(new Reference("subject", tgtProteinRegion.getIdentifier()));

            tgtLocation.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {dataSetItem.getIdentifier()})));
            Item tgtTerm = createItem("ProteinInteractionTerm");
            tgtTerm.addAttribute(new Attribute("identifier",
                                               primaryRef.getAttribute("id")
                                               .getValue()));
            Item tgtAnnotation = createItem("Annotation");
            tgtAnnotation.addReference(new Reference("property", tgtTerm.getIdentifier()));
            tgtAnnotation.addReference(new Reference("subject", tgtProteinRegion.getIdentifier()));
            tgtAnnotation.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {dataSetItem.getIdentifier()})));

            Item tgtProteinInteractionRegion = createItem("ProteinInteractionRegion");

            tgtProteinInteractionRegion.addAttribute(
                    new Attribute("name",
                            getReference(featureDescription,
                                    "names").getAttribute("shortLabel").getValue()));

            Item psiDagTerm = createItem("OntologyTerm");
            psiDagTerm.setAttribute("identifier", "MI:0117");

            tgtProteinInteractionRegion.addReference(
                    new Reference("ontologyTerm", psiDagTerm.getIdentifier()));

            tgtProteinInteractionRegion.addReference(
                    new Reference("protein",
                            participant.getReference("proteinInteractorRef").getRefId()));

            tgtProteinInteractionRegion.addReference(
                    new Reference("location", tgtLocation.getIdentifier()));

            tgtProteinInteractionRegion.addReference(
                    new Reference("interaction", interaction.getIdentifier()));

            interaction.addToCollection("interactingRegions", tgtProteinInteractionRegion);

            result.add(tgtProteinRegion);
            result.add(tgtLocation);
            result.add(tgtTerm);
            result.add(tgtAnnotation);
            result.add(tgtProteinInteractionRegion);
            result.add(psiDagTerm);
        } else {
            LOG.debug("Skipping creating a ProteinRegion as the psi term MI:0117 was not found!");
        }
    }


    // Return the publication for a given experiment, creating it if necessary
    // Note that experiments known not to have a publication are stored in the map with a null pub
    private Item getPub(Item exptType) throws ObjectStoreException {
        Item pub = null;
        String exptId = exptType.getIdentifier();
        if (pubs.containsKey(exptId)) {
            pub = (Item) pubs.get(exptId);
        } else {
            Item bibRefType = getReference(exptType, "bibref");
            if (bibRefType != null) {
                Item xRefType = getReference(bibRefType, "xref");
                if (xRefType != null) {
                    Item dbReferenceType = getReference(xRefType, "primaryRef");
                    if (dbReferenceType != null) {
                        Attribute dbAttr = dbReferenceType.getAttribute("db");
                        if (dbAttr != null && dbAttr.getValue().equalsIgnoreCase("pubmed")) {
                            Attribute idAttr = dbReferenceType.getAttribute("id");
                            if (idAttr != null) {
                                pub = createItem("Publication");
                                pub.addAttribute(new Attribute("pubMedId", idAttr.getValue()));
                            }
                        }
                    }
                }
            }
            pubs.put(exptId, pub);
        }
        return pub;
    }

    /**
     * @see org.intermine.bio.task.DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        String identifier = ObjectStoreItemPathFollowingImpl.IDENTIFIER;
        Set descSet = new HashSet();

        //ExperimentType
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("ExperimentType.names");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    identifier));
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("ExperimentType.bibref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("bibref",
                    identifier));
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor("ExperimentType.bibref.xref");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    identifier));
        ItemPrefetchDescriptor desc3 = new
            ItemPrefetchDescriptor("ExperimentType.bibref.xref.primaryRef");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    identifier));
        desc2.addPath(desc3);
        desc.addPath(desc2);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("ExperimentType.attributeList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("attributeList",
                    identifier));
        desc2 = new ItemPrefetchDescriptor(
                "ExperimentType.attributeList.attributes");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("attributes",
                    identifier));
        desc.addPath(desc2);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("ExperimentType.hostOrganism");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("hostOrganism",
                    identifier));
        ItemPrefetchDescriptor desc1 = new ItemPrefetchDescriptor(
                    "ExperimentType.hostOrganism.fullName");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("fullName",
                    identifier));
        desc2 = new ItemPrefetchDescriptor(
                    "ExperimentType.hostOrganism.fullName.names");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    identifier));
        desc1.addPath(desc2);
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/psi#ExperimentType", descSet);

        //InteractionElementType
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("InteractionElementType.experimentList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("experimentList",
                    identifier));
        desc2 = new ItemPrefetchDescriptor(
                    "InteractionElementType.experimentList.experimentRefs");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("experimentRefs",
                    identifier));
        desc3 = new ItemPrefetchDescriptor(
                    "InteractionElementType.experimentList.experimentRefs.names");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    identifier));
        desc2.addPath(desc3);
        desc.addPath(desc2);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("InteractionElementType.attributeList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("attributeList",
                    identifier));
        desc2 = new ItemPrefetchDescriptor(
                    "InteractionElementType.attributeList.attributes");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("attributes",
                    identifier));
        desc.addPath(desc2);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("InteractionElementType.participantList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("participantList",
                    identifier));
        desc1 = new ItemPrefetchDescriptor(
                    "InteractionElementType.participantList.proteinParticipants");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("proteinParticipants",
                    identifier));
        desc3 = new ItemPrefetchDescriptor("InteractionElementType.participantList"
                     + ".proteinParticipants.proteinInteractorRef");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("proteinInteractorRef",
                    identifier));
        ItemPrefetchDescriptor desc4 = new ItemPrefetchDescriptor(
                    "InteractionElementType.participantList.proteinParticipants"
                     + ".proteinInteractorRef.organism");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("organism",
                    identifier));
        desc3.addPath(desc4);
        desc1.addPath(desc3);
        desc.addPath(desc1);

        desc2 = new ItemPrefetchDescriptor(
                    "InteractionElementType.participantList.proteinParticipants.featureList");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("featureList",
                    identifier));
        desc4 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("features",
                    identifier));
        ItemPrefetchDescriptor desc5 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription");
        desc5.addConstraint(new ItemPrefetchConstraintDynamic("featureDescription",
                    identifier));
        ItemPrefetchDescriptor desc6 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription.xref");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    identifier));

        ItemPrefetchDescriptor desc7 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription.xref.primaryRef");
        desc7.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    identifier));
        desc6.addPath(desc7);
        desc5.addPath(desc6);
        desc4.addPath(desc5);
        desc6 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription.names");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    identifier));
        desc5.addPath(desc6);
        desc5 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".location");
        desc5.addConstraint(new ItemPrefetchConstraintDynamic("location",
                    identifier));
        desc6 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".location.begin");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("begin",
                    identifier));
        desc7 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".location.end");
        desc7.addConstraint(new ItemPrefetchConstraintDynamic("end",
                    identifier));
        desc5.addPath(desc7);
        desc5.addPath(desc6);
        desc4.addPath(desc5);
        desc2.addPath(desc4);
        desc1.addPath(desc2);
        desc.addPath(desc1);

        desc3 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.proteinInteractorRef");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("proteinInteractorRef",
                identifier));
        desc2.addPath(desc3);
        desc.addPath(desc2);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("InteractionElementType.shortLabel");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("shortLabel",
                    identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("InteractionElementType.confidence");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("confidence",
                    identifier));
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/psi#InteractionElementType", descSet);

        //ProteinInteractorType
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("ProteinInteractorType.organism");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("organism",
                    identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("ProteinInteractorType.xref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    identifier));
        desc2 = new ItemPrefetchDescriptor("ProteinInteractorType.xref.primaryRef");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    identifier));
        desc.addPath(desc2);
        desc3 = new ItemPrefetchDescriptor("ProteinInteractorType.xref.secondaryRefs");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("secondaryRefs",
                    identifier));
        desc.addPath(desc3);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/psi#ProteinInteractorType",
                  descSet);

        //Source_Entry_EntrySet
        desc = new ItemPrefetchDescriptor("Source_Entry_EntrySet.names");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    identifier));
        paths.put("http://www.intermine.org/model/psi#Source_Entry_EntrySet",
                  Collections.singleton(desc));

        //CvType
        desc = new ItemPrefetchDescriptor("CvType.xref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    identifier));
        desc2 = new ItemPrefetchDescriptor("CvType.xref.primaryRef");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    identifier));
        desc.addPath(desc2);
        paths.put("http://www.intermine.org/model/psi#CvType", Collections.singleton(desc));

        return paths;
    }
}
