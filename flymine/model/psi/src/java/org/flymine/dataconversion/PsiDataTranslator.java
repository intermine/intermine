package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.apache.log4j.Logger;

import java.util.*;

/**
 * DataTranslator specific to Protein Interaction data in PSI XML format.
 *
 * @author Richard Smith
 * @author Andrew Varley
 * @author Peter Mclaren - Modifications to cater for complex interactions
 *
 * TODO: Test with NULL Prey or Bait items (interactions with nothing specific about them...)
 */
public class PsiDataTranslator extends DataTranslator
{
    private Item db, swissProt;
    private Map pubs = new HashMap();

    //private Map experimentIdToFeatureDescriptionSets = new HashMap();
    //private Map experimentIdToExperiment = new HashMap();

    protected static final Logger LOG = Logger.getLogger(PsiDataTranslator.class);

    /**
     * @see DataTranslator#DataTranslator
     */
    public PsiDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                             Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {
        swissProt = createItem("DataSource");
        swissProt.addAttribute(new Attribute("name", "Swiss-Prot"));
        tgtItemWriter.store(ItemHelper.convert(swissProt));

        db = createItem("DataSet");

        super.translate(tgtItemWriter);

        //setExperimentToFeatureDescripionMappings();
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                Item tgtItem = (Item) i.next();
                if ("ExperimentType".equals(className)) {
                    Item pub = getPub(srcItem);
                    if (pub != null) {
                        tgtItem.addReference(new Reference("publication", pub.getIdentifier()));
                        result.add(pub);
                    }
                    Item attributeList = getReference(srcItem, "attributeList");
                    if (attributeList != null) {
                        for (Iterator j = getCollection(attributeList, "attributes");
                             j.hasNext();) {
                            Item attribute = (Item) j.next();
                            Item comment = createItem("Comment");
                            comment.addAttribute(new Attribute("type",
                                                               attribute.getAttribute("name")
                                                               .getValue()));
                            comment.addAttribute(new Attribute("text",
                                                               attribute.getAttribute("attribute")
                                                               .getValue()));
                            comment.addReference(new Reference("source",
                                                               db.getIdentifier()));
                            result.add(comment);
                            addToCollection(tgtItem, "comments", comment);
                        }
                    }

                    //experimentIdToExperiment.put(srcItem.getIdentifier(), tgtItem);

                } else if ("InteractionElementType".equals(className)) {
                    addReferencedItem(tgtItem, db, "source", false, "", false);

                    Item exptType = (Item) getCollection(getReference(srcItem, "experimentList"),
                                                         "experimentRefs").next();
                    addReferencedItem(tgtItem, exptType, "analysis", false, "", false);

                    // set confidence from attributeList
                    if (srcItem.getReference("attributeList") != null) {
                        for (Iterator j = getCollection(getReference(srcItem, "attributeList"),
                                                        "attributes"); j.hasNext();) {
                            Item attribute = (Item) j.next();
                            String value = attribute.getAttribute("attribute").getValue().trim();
                            String name = attribute.getAttribute("name").getValue().trim();
                            if (Character.isDigit(value.charAt(0))
                                && name.equals("author-confidence")) {
                                tgtItem.addAttribute(new Attribute("confidence", value));
                            } else if (name.equals("author-confidence")) {
                                //If we have some text instead of a numerical value...
                                tgtItem.addAttribute(new Attribute("confidenceDesc", value));
                            }
                        }
                    }
                    Item interaction = createProteinInteraction(srcItem, result);
                    addReferencedItem(tgtItem, interaction, "relations", true, "evidence", true);
                    result.add(interaction);
                } else if ("ProteinInteractorType".equals(className)) {
                    Item xref = getReference(srcItem, "xref");
                    Item dbXref = getReference(xref, "primaryRef");
                    if (dbXref.getAttribute("db").getValue().equals("uniprot")) {
                        String value = dbXref.getAttribute("id").getValue();
                        tgtItem.addAttribute(new Attribute("primaryAccession", value));
                        Item synonym = createItem("Synonym");
                        addReferencedItem(synonym, swissProt, "source", false, "", false);
                        synonym.addAttribute(new Attribute("value", value));
                        synonym.addAttribute(new Attribute("type", "accession"));
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        result.add(synonym);
                    }
                    if (srcItem.getAttribute("sequence") != null) {
                        Item seq = createItem("Sequence");
                        seq.addAttribute(new Attribute("residues", srcItem.getAttribute("sequence")
                                                       .getValue()));
                        tgtItem.addReference(new Reference("sequence", seq.getIdentifier()));
                        result.add(seq);
                    }
                } else if ("Source_Entry_EntrySet".equals(className)) {
                    tgtItem.setIdentifier(db.getIdentifier());
                    tgtItem.addAttribute(new Attribute("title", getReference(srcItem, "names")
                                                       .getAttribute("shortLabel").getValue()));
                } else if ("CvType".equals(className)) {
                    Item xref = getReference(srcItem, "xref");
                    Item primaryRef = getReference(xref, "primaryRef");
                    if (primaryRef.getAttribute("db").getValue().equalsIgnoreCase("psi-mi")) {
                        tgtItem.addAttribute(new Attribute("identifier",
                                                           primaryRef.getAttribute("id")
                                                           .getValue()));
                    }
                }
                result.add(tgtItem);
            }
        }
        return result;
    }

    private Item createProteinInteraction(Item srcInteractionElementItem, Collection result)
        throws ObjectStoreException {
        Item interaction = createItem("ProteinInteraction");
        Item participants = getReference(srcInteractionElementItem, "participantList");

        ArrayList preyRefIdsList = new ArrayList();

        for (Iterator i = getCollection(participants, "proteinParticipants"); i.hasNext();) {
            Item participant = (Item) i.next();
            if (getReference(participant, "featureList") != null) {
                createProteinRegion(interaction, participant, result);
            }
            String role = participant.getAttribute("role").getValue();

            if (role.equalsIgnoreCase("prey")) {
                preyRefIdsList.add(participant.getReference("proteinInteractorRef").getRefId());
            } else {
                interaction.addReference(
                        new Reference(role,
                                participant.getReference("proteinInteractorRef").getRefId()));
            }
        }

        Reference namesRef = srcInteractionElementItem.getReference("names");
        org.intermine.model.fulldata.Item namesItem =
                this.srcItemReader.getItemById(namesRef.getRefId());

        boolean shortLabelFound = false;

        for (Iterator nameIt = namesItem.getAttributes().iterator();
            nameIt.hasNext() && !shortLabelFound;) {

            org.intermine.model.fulldata.Attribute nextNameAttr
                    = (org.intermine.model.fulldata.Attribute) nameIt.next();
            if ("shortLabel".equalsIgnoreCase(nextNameAttr.getName())) {
                interaction.setAttribute("shortName", nextNameAttr.getValue());
                LOG.info("INTERACTION.SHORTNAME WAS SET AS:" + nextNameAttr.getValue());
                shortLabelFound = true;
            }
        }

        if (preyRefIdsList.size() == 1) {
            Object nextPrey = preyRefIdsList.iterator().next();
            LOG.info("PREY ITEM:" + nextPrey.toString());
            interaction.addReference(new Reference("prey", nextPrey.toString()));

        } else if (preyRefIdsList.size() > 1) {
            interaction.addToCollection("complex", interaction.getReference("bait").getRefId());
            LOG.info("ADDING BAIT ITEM TO A COMPLEX:"
                    + interaction.getReference("bait").getRefId());
        } else {
            LOG.warn("SKIPPING PREY/COMPLEX REFERENCE CREATION IN A PROTEININTERACTION ITEM!");
        }


        return interaction;
    }

    private void createProteinRegion(Item interaction, Item participant, Collection result)
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
                {db.getIdentifier()})));
            Item tgtTerm = createItem("ProteinInteractionTerm");
            tgtTerm.addAttribute(new Attribute("identifier",
                                               primaryRef.getAttribute("id")
                                               .getValue()));
            Item tgtAnnotation = createItem("Annotation");
            tgtAnnotation.addReference(new Reference("property", tgtTerm.getIdentifier()));
            tgtAnnotation.addReference(new Reference("subject", tgtProteinRegion.getIdentifier()));
            tgtAnnotation.addCollection(new ReferenceList("evidence", Arrays.asList(new Object[]
                {db.getIdentifier()})));

            //Item tgtFeatureDesc = createItem("FeatureDescription");
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

            //NOTE: This is done automatically on loading due to the reverse references...
            //interaction.addToCollection("interactingRegions",
            //        tgtProteinInteractionRegion.getIdentifier());

            result.add(tgtProteinRegion);
            result.add(tgtLocation);
            result.add(tgtTerm);
            result.add(tgtAnnotation);
            result.add(tgtProteinInteractionRegion);
            result.add(psiDagTerm);
        } else {
            LOG.info("Skipping creating a ProteinRegion as the psi term MI:0117 was not found!");
        }
    }

    /**
     * Helper method to fetch the relevant feature descriptor set for the given experiment id.
     * If this is a new experiment then return a new set and keep a reference of it.
     *
     * @ param experimentIdRef PSI internal Id string of the experiment we want.
     * @ return a Set - which may be empty - of feature descriptors for the given experiment id.
     * *  /
    private Set getFeatureDescSetByExpId(String experimentIdRef){

        LOG.info("getFeatureDescSetByExpId called!");

        if (! experimentIdToFeatureDescriptionSets.containsKey(experimentIdRef)){

            experimentIdToFeatureDescriptionSets.put(experimentIdRef, new HashSet());
        }

        return (Set) experimentIdToFeatureDescriptionSets.get(experimentIdRef);
    }

    private void setExperimentToFeatureDescripionMappings(){

        if (experimentIdToExperiment.keySet().size()
                != experimentIdToFeatureDescriptionSets.keySet().size()){
            LOG.warn("POSSIBLE DISCREPANCY IN SOURCE FILE FOR EXPERIMENT INFORMATION!");
        }

        for (Iterator exKeyIt = experimentIdToExperiment.keySet().iterator(); exKeyIt.hasNext();){

            Object nextExpKey = exKeyIt.next();

            Item expTgtItem = (Item) experimentIdToExperiment.get(nextExpKey);
            Set featDescSet = (Set) experimentIdToFeatureDescriptionSets.get(nextExpKey);

            if(featDescSet == null){
                LOG.warn("NOTE: featureDescriptions not set for:" + expTgtItem.getClassName()
                        + " id:" + expTgtItem.getIdentifier());
            }
            else{
                expTgtItem.setCollection("featureDescriptions", new ArrayList(featDescSet));
            }
        }


        logNamedCollection("exIdToEx keys", experimentIdToExperiment.keySet());
        logNamedCollection("exIdToEx vals", experimentIdToExperiment.values());
        logNamedCollection("exIdToFD keys", experimentIdToFeatureDescriptionSets.keySet());
        logNamedCollection("exIdToFD vals", experimentIdToFeatureDescriptionSets.values());
    }

    private void logNamedCollection(String name, Collection values){

        LOG.info("NAME:" + name + " COLLECTION SIZE:" + values.size());

        for(Iterator valIt = values.iterator(); valIt.hasNext();){

            LOG.info(" NEXT_VAL:" + valIt.next().toString());
        }
    }
    */

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
     * @see org.flymine.task.DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();

        Set descSet = new HashSet();
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("ExperimentType.attributeList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("attributeList",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc);
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                "ExperimentType.attributeList.attributes");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("attributes",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        desc = new ItemPrefetchDescriptor("ExperimentType.bibref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("bibref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc);
        desc2 = new ItemPrefetchDescriptor("ExperimentType.bibref.xref");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        ItemPrefetchDescriptor desc3 = new
            ItemPrefetchDescriptor("ExperimentType.bibref.xref.primaryRef");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        paths.put("http://www.flymine.org/model/psi#ExperimentType", descSet);

        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("InteractionElementType.experimentList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("experimentList",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc);
        desc2 = new ItemPrefetchDescriptor("InteractionElementType.experimentList.experimentRefs");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("experimentRefs",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        desc = new ItemPrefetchDescriptor("InteractionElementType.attributeList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("attributeList",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc);
        desc2 = new ItemPrefetchDescriptor("InteractionElementType.attributeList.attributes");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("attributes",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        desc = new ItemPrefetchDescriptor("InteractionElementType.participantList");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("participantList",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc);
        desc2 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("proteinParticipants",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        desc3 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("featureList",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        ItemPrefetchDescriptor desc4 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("features",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3.addPath(desc4);
        ItemPrefetchDescriptor desc5 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription");
        desc5.addConstraint(new ItemPrefetchConstraintDynamic("featureDescription",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc4.addPath(desc5);
        ItemPrefetchDescriptor desc6 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription.xref");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc5.addPath(desc6);
        ItemPrefetchDescriptor desc7 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".featureDescription.xref.primaryRef");
        desc7.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc6.addPath(desc7);
        desc5 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".location");
        desc5.addConstraint(new ItemPrefetchConstraintDynamic("location",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc4.addPath(desc5);
        desc6 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".location.begin");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("begin",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc5.addPath(desc6);
        desc6 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.featureList.features"
                + ".location.end");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("end",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc5.addPath(desc6);
        desc3 = new ItemPrefetchDescriptor(
                "InteractionElementType.participantList.proteinParticipants.proteinInteractorRef");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("proteinInteractorRef",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        paths.put("http://www.flymine.org/model/psi#InteractionElementType", descSet);

        desc = new ItemPrefetchDescriptor("ProteinInteractorType.xref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("ProteinInteractorType.xref.primaryRef");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        paths.put("http://www.flymine.org/model/psi#ProteinInteractorType",
                  Collections.singleton(desc));

        desc = new ItemPrefetchDescriptor("Source_Entry_EntrySet.names");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/psi#Source_Entry_EntrySet",
                  Collections.singleton(desc));

        desc = new ItemPrefetchDescriptor("CvType.xref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("CvType.xref.primaryRef");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("primaryRef",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        paths.put("http://www.flymine.org/model/psi#CvType", Collections.singleton(desc));

        return paths;
    }    
}
