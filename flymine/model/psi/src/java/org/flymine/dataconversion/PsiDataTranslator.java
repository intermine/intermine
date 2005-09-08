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

/**
 * DataTranslator specific to Protein Interaction data in PSI XML format.
 *
 * @author Richard Smith
 * @author Andrew Varley
 */
public class PsiDataTranslator extends DataTranslator
{
    private Item db, swissProt;
    private Map pubs = new HashMap();

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
    }

    /**
     * @see DataTranslator#getItemIterator
     *
    public Iterator getItemIterator() throws ObjectStoreException {
        ObjectStoreItemPathFollowingImpl os = ((ObjectStoreItemReader) srcItemReader)
            .getObjectStore();
        List iters = new ArrayList();

        Query q = new Query();
        QueryClass qc = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "className"), ConstraintOp.EQUALS,
                    new QueryValue("http://www.flymine.org/model/psi#ExperimentType")));
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(1000);
        res.setNoExplain();
        res.setNoOptimise();
        iters.add(res.iterator());

        q = new Query();
        qc = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "className"), ConstraintOp.EQUALS,
                    new QueryValue("http://www.flymine.org/model/psi#InteractionElementType")));
        res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(1000);
        res.setNoExplain();
        res.setNoOptimise();

        q = new Query();
        qc = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "className"), ConstraintOp.EQUALS,
                    new QueryValue("http://www.flymine.org/model/psi#ProteinInteratorType")));
        res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(1000);
        res.setNoExplain();
        res.setNoOptimise();

        q = new Query();
        qc = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "className"), ConstraintOp.EQUALS,
                    new QueryValue("http://www.flymine.org/model/psi#Source_Entry_EntrySet")));
        res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(1000);
        res.setNoExplain();
        res.setNoOptimise();

        q = new Query();
        qc = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "className"), ConstraintOp.EQUALS,
                    new QueryValue("http://www.flymine.org/model/psi#CvType")));
        res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(1000);
        res.setNoExplain();
        res.setNoOptimise();
        iters.add(res.iterator());

        return new CombinedIterator(iters);
    }/*

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

    private Item createProteinInteraction(Item intElType, Collection result)
        throws ObjectStoreException {
        Item interaction = createItem("ProteinInteraction");
        Item participants = getReference(intElType, "participantList");
        for (Iterator i = getCollection(participants, "proteinParticipants"); i.hasNext();) {
            Item participant = (Item) i.next();
            if (getReference(participant, "featureList") != null) {
                createProteinRegion(participant, result);
            }
            String role = participant.getAttribute("role").getValue();
            interaction.addReference(new Reference(role, participant
                                                   .getReference("proteinInteractorRef")
                                                   .getRefId()));
            if (participant.getAttribute("isTaggedProtein") != null) {
                interaction.addAttribute(new Attribute(role + "IsTagged", participant
                                                       .getAttribute("isTaggedProtein")
                                                       .getValue()));
            }
            if (participant.getAttribute("isOverexpressedProtein") != null) {
                interaction.addAttribute(new Attribute(role + "IsOverexpressed", participant
                                                       .getAttribute("isOverexpressedProtein")
                                                       .getValue()));
            }
        }
        // object = prey, subject = bait
        interaction.addReference(new Reference("object",
                                               interaction.getReference("prey").getRefId()));
        interaction.addReference(new Reference("subject",
                                               interaction.getReference("bait").getRefId()));
        return interaction;
    }

    private void createProteinRegion(Item participant, Collection result)
        throws ObjectStoreException {
        Item featureList = getReference(participant, "featureList");
        Item feature = (Item) getCollection(featureList, "features").next();
        Item featureDescription = getReference(feature, "featureDescription");
        Item xref = getReference(featureDescription, "xref");
        Item primaryRef = getReference(xref, "primaryRef");
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
            result.add(tgtProteinRegion);
            result.add(tgtLocation);
            result.add(tgtTerm);
            result.add(tgtAnnotation);
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
                                String pubmedId = idAttr.getValue();
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
     * @see DataTranslatorTask#execute
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
