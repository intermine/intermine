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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.FieldNameAndValue;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;

import org.apache.log4j.Logger;

/**
 * DataTranslator specific to uniprot.
 *
 * @author Matthew Wakeling
 */
public class UniprotDataTranslator extends DataTranslator
{
    private static final Logger LOG = Logger.getLogger(UniprotDataTranslator.class);

    private Item database = null;
    private String drosophilaId = null;
    private String caenorhabditisId = null;
    private Map pubMedIdToPublicationId = new HashMap();
    private Map geneIdentifierToId = new HashMap();
    private int idSequence = 0;
    private int pubLinkCount = 0;
    private int drosophilaCount = 0;
    private int caenorhabditisCount = 0;

    private static final String SRC_NS = "http://www.flymine.org/model#";

    /**
     * @see DataTranslator
     */
    public UniprotDataTranslator(ItemReader srcItemReader, String tgtNs) {
        //super(srcItemReader, model, tgtNs);
        this.tgtNs = tgtNs;
        this.srcItemReader = srcItemReader;
    }

    /**
     * @see DataTranslator#getItemIterator
     */
     public Iterator getItemIterator() throws ObjectStoreException {
         Query q = new Query();
         q.setDistinct(false);
         QueryClass qc1 = new QueryClass(org.intermine.model.fulldata.Item.class);
         q.addFrom(qc1);
         q.addToSelect(qc1);
         SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "className"),
                                   ConstraintOp.EQUALS, new QueryValue(SRC_NS + "EntryType"));
         q.setConstraint(sc1);
         return ((ObjectStoreItemReader) srcItemReader).itemIterator(q);
     }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        database = createItem("Database");
        database.addAttribute(new Attribute("title", "UniProt"));
        tgtItemWriter.store(ItemHelper.convert(database));

        super.translate(tgtItemWriter);
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        // This Item should be an EntryType - should only have proteins associated with one organism
        if ((SRC_NS + "EntryType").equals(srcItem.getClassName())) {
            // First things first: find out the taxonid of the organism of this entry.
            int taxonId = 0;
            List organisms = getItemsInCollection(srcItem.getCollection("organisms"));
            Iterator organismIter = organisms.iterator();
            while (organismIter.hasNext()) {
                // Drosophila melanogaster = 7227
                // Caenorhabditis elegans 6239
                // Anopheles gambiae = 7165
                Item organism = (Item) organismIter.next();
                Item dbReference = ItemHelper.convert(srcItemReader.getItemById(organism
                            .getReference("dbReference").getRefId()));
                String type = getAttributeValue(dbReference, "type");
                if ("NCBI Taxonomy".equals(type)) {
                    String taxonString = getAttributeValue(dbReference, "id");
                    if ("7227".equals(taxonString)
                        || "6239".equals(taxonString)
                        || "7165".equals(taxonString)) {
                        if (taxonId != 0) {
                            throw new IllegalStateException("Attempting to set taxon id to "
                                                            + taxonString + " when it is already " 
                                                            + taxonId);
                        }
                        taxonId = Integer.parseInt(taxonString);
                    }
                }
            }

            // should remove hard coding of organism types in translator
            if (taxonId != 0) {
                // We have a recognised organism
                Set retval = new HashSet();

                // set up protein, same for all organisms
                Item protein = createItem(tgtNs + "Protein", "");
                String proteinName = getAttributeValue(srcItem, "name");
                protein.addAttribute(new Attribute("identifier", proteinName));
                retval.add(createSynonym(protein.getIdentifier(), "identifier", proteinName));
                List srcAccessions = getItemsInCollection(srcItem.getCollection("accessions"));
                if (srcAccessions.isEmpty()) {
                    LOG.info("Entry " + proteinName + " does not have any accessions");
                } else {
                    Item srcPrimaryAccession = (Item) srcAccessions.get(0);
                    protein.addAttribute(new Attribute("primaryAccession", getAttributeValue(
                                    srcPrimaryAccession, "accession")));
                    // all accessions should be Synonyms
                    Iterator srcAccIter = srcAccessions.iterator();
                    while (srcAccIter.hasNext()) {
                        Item srcAccession = (Item) srcAccIter.next();
                        String srcAccessionString = getAttributeValue(srcAccession, "accession");
                        retval.add(createSynonym(protein.getIdentifier(), "accession",
                                                 srcAccessionString));
                    }
                }
                // add UniProt Database to evidence collection
                ReferenceList evidence = new ReferenceList("evidence", new ArrayList());
                evidence.addRefId(database.getIdentifier());
                protein.addCollection(evidence);

                // collection of Comments
                List srcComments = getItemsInCollection(srcItem.getCollection("comments"));
                Iterator srcComIter = srcComments.iterator();
                while (srcComIter.hasNext()) {
                    Item srcComment = (Item) srcComIter.next();
                    String srcCommentType = getAttributeValue(srcComment, "type");
                    String srcCommentText = getAttributeValue(srcComment, "text");
                    if ((srcCommentType != null) && (srcCommentText != null)) {
                        Item comment = createItem(tgtNs + "Comment", "");
                        comment.addAttribute(new Attribute("type", srcCommentType));
                        comment.addAttribute(new Attribute("text", srcCommentText));
                        comment.addReference(new Reference("source", database.getIdentifier()));
                        retval.add(comment);
                    }
                }
                Item srcProtein = ItemHelper.convert(srcItemReader.getItemById(srcItem
                            .getReference("protein").getRefId()));
                if (srcProtein != null) {
                    List srcProteinNames = getItemsInCollection(srcProtein.getCollection("names"));
                    Iterator srcProtNameIter = srcProteinNames.iterator();
                    while (srcProtNameIter.hasNext()) {
                        Item srcProteinName = (Item) srcProtNameIter.next();
                        String srcProteinNameString = getAttributeValue(srcProteinName, "name");
                        String srcProteinNameEvidence = getAttributeValue(srcProteinName,
                                "evidence");
                        if (srcProteinNameEvidence != null) {
                            srcProteinNameString += " (Evidence " + srcProteinNameEvidence + ")";
                        }
                        retval.add(createSynonym(protein.getIdentifier(), "name",
                                                 srcProteinNameString));
                    }
                }
                Reference geneOrganismReference = null;
                if (taxonId == 7227) {
                    if (drosophilaId == null) {
                        Item drosophila = createItem(tgtNs + "Organism", "");
                        drosophila.addAttribute(new Attribute("taxonId", "7227"));
                        retval.add(drosophila);
                        drosophilaId = drosophila.getIdentifier();
                    }
                    protein.addReference(new Reference("organism", drosophilaId));
                    geneOrganismReference = new Reference("organism", drosophilaId);
                    drosophilaCount++;
                    if (drosophilaCount % 100 == 0) {
                        LOG.info("Processed " + drosophilaCount + " Drosophila proteins");
                    }
                } else if (taxonId == 6239) {
                    if (caenorhabditisId == null) {
                        Item caenorhabditis = createItem(tgtNs + "Organism", "");
                        caenorhabditis.addAttribute(new Attribute("taxonId", "6239"));
                        retval.add(caenorhabditis);
                        caenorhabditisId = caenorhabditis.getIdentifier();
                    }
                    protein.addReference(new Reference("organism", caenorhabditisId));
                    geneOrganismReference = new Reference("organism", caenorhabditisId);
                    caenorhabditisCount++;
                    if (caenorhabditisCount % 100 == 0) {
                        LOG.info("Processed " + caenorhabditisCount + " Caenorhabditis proteins");
                    }
                }
                List srcReferences = getItemsInCollection(srcItem.getCollection("references"));
                ReferenceList publications = null;
                Iterator srcRefIter = srcReferences.iterator();
                while (srcRefIter.hasNext()) {
                    Item srcReference = (Item) srcRefIter.next();
                    Item srcCitation = ItemHelper.convert(srcItemReader.getItemById(srcReference
                                .getReference("citation").getRefId()));
                    List srcDbReferences = getItemsInCollection(srcCitation
                            .getCollection("dbReferences"));
                    Iterator srcDbRefIter = srcDbReferences.iterator();
                    while (srcDbRefIter.hasNext()) {
                        Item srcDbReference = (Item) srcDbRefIter.next();
                        String type = getAttributeValue(srcDbReference, "type");
                        if ("PubMed".equals(type)) {
                            String pubMedString = new String(getAttributeValue(srcDbReference,
                                        "id"));
                            String publicationId = (String) pubMedIdToPublicationId
                                .get(pubMedString);
                            pubLinkCount++;
                            if (publicationId == null) {
                                Item publication = createItem("" + "Publication", "");
                                publication.addAttribute(new Attribute("pubMedId", pubMedString));
                                retval.add(publication);
                                publicationId = publication.getIdentifier();
                                pubMedIdToPublicationId.put(pubMedString,
                                        publicationId);
                                if (pubMedIdToPublicationId.size() % 100 == 0) {
                                    LOG.info("Processed " + pubMedIdToPublicationId.size()
                                            + " publications, with " + pubLinkCount
                                            + " references to publications");
                                }
                            }
                            if (publications == null) {
                                publications = new ReferenceList("publications", new ArrayList());
                                protein.addCollection(publications);
                            }
                            publications.addRefId(publicationId);
                        }
                    }
                }

                List srcGenes = getItemsInCollection(srcItem.getCollection("genes"));
                ReferenceList geneCollection = null;
                Iterator srcGeneIter = srcGenes.iterator();
                while (srcGeneIter.hasNext()) {
                    Item srcGene = (Item) srcGeneIter.next();
                    String geneIdentifier = null;
                    List srcGeneNames = getItemsInCollection(srcGene.getCollection("names"));
                    String primaryGeneName = null;
                    String tmpGeneName = null;
                    Set geneNames = new HashSet();
                    {
                        Iterator srcGeneNameIter = srcGeneNames.iterator();
                        while (srcGeneNameIter.hasNext()) {
                            Item srcGeneName = (Item) srcGeneNameIter.next();
                            if ("primary".equals(getAttributeValue(srcGeneName, "type"))) {
                                primaryGeneName = new String(getAttributeValue(srcGeneName,
                                            "name"));
                            }
                            geneNames.add(new String(getAttributeValue(srcGeneName, "name")));
                        }
                        if ((primaryGeneName == null) && (!srcGeneNames.isEmpty())) {
                            LOG.error("no primaryGeneName found for protein: " + proteinName);
                            // tmpGeneName just used for logging
                            tmpGeneName = new String(getAttributeValue((Item) srcGeneNames
                                        .get(0), "name"));
                        } else {
                            tmpGeneName = primaryGeneName;
                        }
                    }
                    String geneOrganismDbId = null;
                    if (taxonId == 7227) {
                        Iterator srcGeneNameIter = srcGeneNames.iterator();
                        while (srcGeneNameIter.hasNext()) {
                            Item srcGeneName = (Item) srcGeneNameIter.next();
                            if ("ORF".equals(getAttributeValue(srcGeneName, "type"))) {
                                geneIdentifier = new String(getAttributeValue(srcGeneName, "name"));
                            }
                        }
                        geneOrganismDbId = getDbReferenceValue(srcItem, "FlyBase", geneNames);
                    } else if (taxonId == 6239) {
                        // C elagans
                        // Gene.name = primaryGeneName
                        // Gene.identifier = name with type ORF
                        // Gene.organismDbId = dbReference with type = WormBase corresponding
                        // to geneIdentifier
                        // if identifier
                        Iterator srcGeneNameIter = srcGeneNames.iterator();
                        while (srcGeneNameIter.hasNext()) {
                            Item srcGeneName = (Item) srcGeneNameIter.next();
                            if ("ORF".equals(getAttributeValue(srcGeneName, "type"))) {
                                geneIdentifier = new String(getAttributeValue(srcGeneName, "name"));
                            }
                        }

                        geneOrganismDbId = getDbReferenceValue(srcItem, "WormBase", geneNames);

                    }
                    LOG.info("name: " + primaryGeneName + "\tidentifier: "
                             + geneIdentifier + "\torgansismDbId: " + geneOrganismDbId);
                    if (geneOrganismDbId == null) {
                        LOG.warn("Could not find organismDbId for " + taxonId + " "
                                 + (geneIdentifier == null ? "" : "(no geneIdentifier)")
                                 + " for gene with name " + tmpGeneName
                                 + " for protein with name " + proteinName);
                    }
                    if (geneIdentifier == null) {
                        LOG.warn("Could not find geneIdentifier for " + taxonId + " "
                                 + (geneOrganismDbId == null ? "" : "(no geneOrganismDbId)")
                                 + " gene with name " + tmpGeneName + " for protein with name "
                                 + proteinName);
                    }
                    if (geneIdentifier != null) {
                        // We have a gene id.
                        String geneId = (String) geneIdentifierToId.get(geneIdentifier);
                        if (geneId == null) {
                            Item gene = createItem(tgtNs + "Gene", "");
                            gene.addAttribute(new Attribute("identifier", geneIdentifier));
                            if (primaryGeneName != null) {
                                gene.addAttribute(new Attribute("name", primaryGeneName));
                            }
                            if (geneOrganismDbId != null) {
                                gene.addAttribute(new Attribute("organismDbId", geneOrganismDbId));
                            } else {
                                LOG.error("geneOrganismDbId was null" + geneIdentifier);
                            }
                            gene.addReference(geneOrganismReference);

                            // add UniProt Database to evidence collection
                            ReferenceList evidence1 = new ReferenceList("evidence",
                                                                        new ArrayList());
                            evidence1.addRefId(database.getIdentifier());
                            protein.addCollection(evidence1);

                            ReferenceList geneSynonyms = new ReferenceList("synonyms",
                                    new ArrayList());
                            String primaryName = null;
                            Iterator srcGeneNameIter = srcGeneNames.iterator();
                            while (srcGeneNameIter.hasNext()) {
                                Item srcGeneName = (Item) srcGeneNameIter.next();
                                String type = getAttributeValue(srcGeneName, "type");
                                String name = getAttributeValue(srcGeneName, "name");
                                retval.add(createSynonym(gene.getIdentifier(), type, name));
                            }
                            gene.addCollection(geneSynonyms);
                            geneId = gene.getIdentifier();
                            geneIdentifierToId.put(geneIdentifier, geneId);
                            retval.add(gene);
                        }
                        if (geneCollection == null) {
                            geneCollection = new ReferenceList("genes", new ArrayList());
                            protein.addCollection(geneCollection);
                        }
                        geneCollection.addRefId(geneId);
                    } else {
                        LOG.error("geneIdentifier was null " + taxonId + " for protein "
                                  + proteinName);
                    }
                }
                retval.add(protein);
                return retval;
            }
        }
        return Collections.EMPTY_SET;
    }


    private Item createSynonym(String subjectId, String type, String value) {
        Item synonym = createItem(tgtNs + "Synonym", "");
        synonym.addReference(new Reference("subject", subjectId));
        synonym.addAttribute(new Attribute("type", type));
        synonym.addAttribute(new Attribute("value", value));
        synonym.addReference(new Reference("source", database.getIdentifier()));
        return synonym;
    }

    protected String getDbReferenceValue(Item srcItem, String dbName, Set geneNames)
        throws ObjectStoreException {
        // Drosophila - take the gene id from the FlyBase dbReference that has a
        // property type = "gene designation" value = the primary gene name
        String geneIdentifier = null;
        List srcDbRefs = getItemsInCollection(srcItem.getCollection("dbReferences"), "type",
                                              dbName);
        Iterator srcDbRefIter = srcDbRefs.iterator();
        while (srcDbRefIter.hasNext()) {
            Item srcDbReference = (Item) srcDbRefIter.next();
            String type = getAttributeValue(srcDbReference, "type");
            if (dbName.equals(type)) {
                // uncomment to set geneIdentifier if there is no propertys collection present (?)
                //if ((srcDbRefs.size() == 1) && (srcGenes.size() == 1)) {
                //    geneIdentifier = new String(getAttributeValue(srcDbReference,"id"));
                //} else {
                List srcProperties = getItemsInCollection(srcDbReference
                                                          .getCollection("propertys"));
                Iterator srcPropertyIter = srcProperties.iterator();
                while (srcPropertyIter.hasNext()) {
                    Item srcProperty = (Item) srcPropertyIter.next();
                    String srcPropertyValue = getAttributeValue(srcProperty, "value");
                    if ("gene designation".equals(getAttributeValue(srcProperty, "type"))
                        && (geneNames.contains(srcPropertyValue)
                            || geneNames.contains(srcPropertyValue
                                                  .substring(srcPropertyValue
                                                             .lastIndexOf("\\") + 1)))) {
                        geneIdentifier = new String(getAttributeValue(
                                                                      srcDbReference, "id"));
                    }
                }
                if (geneIdentifier == null) {
                    LOG.error("Found dbRefs (" + srcDbRefs.size()
                              + ") but unable to match gene designation");
                }
            } else {
                LOG.error("Found a non-FlyBase dbReference when it should have been"
                          + " filtered out");
            }
        }
        return geneIdentifier;
    }

    public List getItemsInCollection(ReferenceList col) throws ObjectStoreException {
        return getItemsInCollection(col, null, null);
    }


    /**
     * Given a ReferenceList fetch items and return a list of Items in the order that their
     * identifiers appear in the original ReferenceList
     * @param col the ReferenceList
     * @param attributeName the name of the attribute
     * @param attributeValue the value of that attribute
     * @return List the list of Items
     * @throws ObjectStoreException if an error occurs
     */
    public List getItemsInCollection(ReferenceList col, String attributeName, String attributeValue)
        throws ObjectStoreException {
        if ((col == null) || col.getRefIds().isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        StringBuffer refIds = new StringBuffer();
        boolean needComma = false;
        Iterator refIdIter = col.getRefIds().iterator();
        while (refIdIter.hasNext()) {
            if (needComma) {
                refIds.append(" ");
            }
            needComma = true;
            refIds.append((String) refIdIter.next());
        }
        FieldNameAndValue fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER,
                refIds.toString(), true);
        Set description;
        if (attributeName == null) {
            description = Collections.singleton(fnav);
        } else {
            description = new HashSet();
            description.add(fnav);
            description.add(new FieldNameAndValue(attributeName, attributeValue, false));
        }
        List unconvertedResults = srcItemReader.getItemsByDescription(description);
        Map itemMap = new HashMap();
        Iterator uncoIter = unconvertedResults.iterator();
        while (uncoIter.hasNext()) {
            Item item = ItemHelper.convert((org.intermine.model.fulldata.Item) uncoIter.next());
            itemMap.put(item.getIdentifier(), item);
        }
        // now put items in same order as identifiers in ReferenceList
        List retval = new ArrayList();
        refIdIter = col.getRefIds().iterator();
        while (refIdIter.hasNext()) {
            Item tmpItem = (Item) itemMap.get(refIdIter.next());
            if (tmpItem != null) {
                retval.add(tmpItem);
            }
        }
        return retval;
    }

    public static String getAttributeValue(Item item, String name) {
        Attribute attribute = item.getAttribute(name);
        if (attribute != null) {
            return attribute.getValue();
        }
        return null;
    }

    /**
     * @see DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();

        Set descs = new HashSet();
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("entryType.organisms");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("organisms",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                "entryType.organisms.dbReference");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("dbReference",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entryType.accessions");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("accessions",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entryType.comments");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("comments",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entryType.protein");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("protein",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("entryType.protein.names");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entryType.references");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("references",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("entryType.references.citation");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("citation",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        ItemPrefetchDescriptor desc3 = new ItemPrefetchDescriptor(
                "entryType.references.citation.dbReferences");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("dbReferences",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entryType.dbReferences");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("dbReferences",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addConstraint(new FieldNameAndValue("type", "FlyBase", false));
        desc2 = new ItemPrefetchDescriptor("entryType.dbReferences.propertys");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("propertys",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entryType.genes");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("genes",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("entryType.genes.names");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        paths.put(SRC_NS + "EntryType", descs);

        return paths;
    }
}
