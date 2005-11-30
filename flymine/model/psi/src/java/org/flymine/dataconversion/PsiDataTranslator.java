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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
        dataSource = createItem("DataSource");
        dataSource.addAttribute(new Attribute("name", "IntAct"));
        tgtItemWriter.store(ItemHelper.convert(dataSource));

        super.translate(tgtItemWriter);

        Iterator iter = dataSetMap.values().iterator();

        while (iter.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) iter.next()));
        }
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
                    Item dataSetItem =
                        getDataSetFromNamesType(getReference(srcItem, "names"));
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
                                                               dataSetItem.getIdentifier()));
                            result.add(comment);
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
                    if (srcItem.getReference("attributeList") != null) {
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
                    Item interaction = createProteinInteraction(srcItem, tgtItem, result, dataSet);

                    interaction.addReference(new Reference("experiment", exptType.getIdentifier()));

                    interaction.addCollection(new ReferenceList("evidence",
                            Arrays.asList(new Object[] {dataSource.getIdentifier()})));

                    addReferencedItem(tgtItem, interaction, "relations", true, "evidence", true);
                    result.add(interaction);
                } else if ("ProteinInteractorType".equals(className)) {
                    Item xref = getReference(srcItem, "xref");
                    Item dbXref = getReference(xref, "primaryRef");
                    if (dbXref.getAttribute("db").getValue().equals("uniprot")) {
                        String value = dbXref.getAttribute("id").getValue();
                        tgtItem.addAttribute(new Attribute("primaryAccession", value));
                        Item synonym = createItem("Synonym");
                        addReferencedItem(synonym, dataSource, "source", false, "", false);
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

                dataSetItem.addAttribute(new Attribute("description", fullNameAttr.getValue()));
            } else {
                LOG.debug("NO FULLNAME ATTR FOUND FOR THIS SHORTNAME:" + shortName);
            }
            dataSetItem.setReference("dataSource", dataSource);

            dataSetMap.put(shortName, dataSetItem);
        }

        return dataSetItem;
    }

    private Item createProteinInteraction(
            Item srcInteractionElementItem, Item tgtExperimentalResult, Collection result,
            Item dataSetItem)
        throws ObjectStoreException {
        Item interaction = createItem("ProteinInteraction");

        Item participants = getReference(srcInteractionElementItem, "participantList");

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

            Reference proteinRef =
                    new Reference("protein",
                            participant.getReference("proteinInteractorRef").getRefId());
            interactor.setReference("protein", proteinRef.getRefId());
            interactor.setReference("interaction", interaction.getIdentifier());
            interaction.addToCollection("interactors", interactor);
            result.add(interactor);
        }

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
            LOG.debug("An 'itemWithSomeNames' has no 'names'!" + itemWithSomeNames.getIdentifier());
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
