package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * DataTranslator specific to uniprot.
 *
 * @author Matthew Wakeling
 */
public class UniprotDataTranslator extends DataTranslator
{
    private static final Logger LOG = Logger.getLogger(UniprotDataTranslator.class);

    private String databaseId = null;
    private String drosophilaId = null;
    private String caenorhabditisId = null;
    private Map pubMedIdToPublicationId = new HashMap();
    private Map geneIdentifierToId = new HashMap();
    private int idSequence = 0;
    private int pubLinkCount = 0;
    private int drosophilaCount = 0;
    private int caenorhabditisCount = 0;

    private static final String SRC_NS = "http://www.flymine.org/model#";
    private static final String TGT_NS = "http://www.flymine.org/model/genomic#";

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
        QueryClass qc2 = new QueryClass(org.intermine.model.fulldata.ReferenceList.class);
        q.addFrom(qc2);
        ContainsConstraint cc1 = new ContainsConstraint(new QueryObjectReference(qc2, "item"),
                ConstraintOp.CONTAINS, qc1);
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                ConstraintOp.EQUALS, new QueryValue("organisms"));
        QueryClass qc3 = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc3);
        SimpleConstraint sc3 = new SimpleConstraint(new QueryField(qc2, "refIds"),
                ConstraintOp.EQUALS, new QueryField(qc3, "identifier"));
        QueryClass qc4 = new QueryClass(org.intermine.model.fulldata.Reference.class);
        q.addFrom(qc4);
        ContainsConstraint cc2 = new ContainsConstraint(new QueryObjectReference(qc4, "item"),
                ConstraintOp.CONTAINS, qc3);
        SimpleConstraint sc4 = new SimpleConstraint(new QueryField(qc4, "name"),
                ConstraintOp.EQUALS, new QueryValue("dbReference"));
        QueryClass qc5 = new QueryClass(org.intermine.model.fulldata.Item.class);
        q.addFrom(qc5);
        SimpleConstraint sc5 = new SimpleConstraint(new QueryField(qc4, "refId"),
                ConstraintOp.EQUALS, new QueryField(qc5, "identifier"));
        QueryClass qc6 = new QueryClass(org.intermine.model.fulldata.Attribute.class);
        q.addFrom(qc6);
        ContainsConstraint cc3 = new ContainsConstraint(new QueryObjectReference(qc6, "item"),
                ConstraintOp.CONTAINS, qc5);
        SimpleConstraint sc6 = new SimpleConstraint(new QueryField(qc6, "name"),
                ConstraintOp.EQUALS, new QueryValue("id"));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        SimpleConstraint sc7 = new SimpleConstraint(new QueryField(qc6, "value"),
                ConstraintOp.EQUALS, new QueryValue("7227"));
        SimpleConstraint sc8 = new SimpleConstraint(new QueryField(qc6, "value"),
                ConstraintOp.EQUALS, new QueryValue("6239"));
        cs1.addConstraint(sc7);
        cs1.addConstraint(sc8);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(sc1);
        cs2.addConstraint(cc1);
        cs2.addConstraint(sc2);
        cs2.addConstraint(sc3);
        cs2.addConstraint(cc2);
        cs2.addConstraint(sc4);
        cs2.addConstraint(sc5);
        cs2.addConstraint(cc3);
        cs2.addConstraint(sc6);
        cs2.addConstraint(cs1);
        q.setConstraint(cs2);
        // TODO: This query will fetch all the Drosophila & Caenorhabditis entries that have only
        // one organism. There are three that have multiple organisms, but we can add those later.
        return ((ObjectStoreItemReader) srcItemReader).itemIterator(q);
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem) throws ObjectStoreException, InterMineException {
        // This Item should be an EntryType.
        if ((SRC_NS + "EntryType").equals(srcItem.getClassName())) {
            // First things first: find out the taxonid of the organism of this entry.
            int taxonId = 0;
            List organisms = getItemsInCollection(srcItem.getCollection("organisms"));
            Iterator organismIter = organisms.iterator();
            while (organismIter.hasNext()) {
                Item organism = (Item) organismIter.next();
                Item dbReference = ItemHelper.convert(srcItemReader.getItemById(organism
                            .getReference("dbReference").getRefId()));
                String type = getAttributeValue(dbReference, "type");
                if ("NCBI Taxonomy".equals(type)) {
                    String taxonString = getAttributeValue(dbReference, "id");
                    if ("7227".equals(taxonString)) {
                        // Drosophila melanogaster
                        if (taxonId != 0) {
                            throw new IllegalStateException("Attempting to set taxon id to "
                                    + taxonString + " when it is already " + taxonId);
                        }
                        taxonId = 7227;
                    } else if ("6239".equals(taxonString)) {
                        // Caenorhabditis elegans
                        if (taxonId != 0) {
                            throw new IllegalStateException("Attempting to set taxon id to "
                                    + taxonString + " when it is already " + taxonId);
                        }
                        taxonId = 6239;
                    }
                }
            }
            if (taxonId != 0) {
                // We have a recognised organism
                Set retval = new HashSet();
                if (databaseId == null) {
                    Item database = new Item(getUniqueIdentifier(), TGT_NS + "Database", "");
                    database.addAttribute(new Attribute("title", "Uniprot"));
                    databaseId = database.getIdentifier();
                    retval.add(database);
                }
                Item protein = new Item(getUniqueIdentifier(), TGT_NS + "Protein", "");
                String proteinName = getAttributeValue(srcItem, "name");
                protein.addAttribute(new Attribute("identifier", proteinName));
                List srcAccessions = getItemsInCollection(srcItem.getCollection("accessions"));
                if (srcAccessions.isEmpty()) {
                    LOG.info("Entry " + proteinName + " does not have any accessions");
                } else {
                    Item srcPrimaryAccession = (Item) srcAccessions.get(0);
                    protein.addAttribute(new Attribute("primaryAccession", getAttributeValue(
                                    srcPrimaryAccession, "accession")));
                    Iterator srcAccIter = srcAccessions.iterator();
                    while (srcAccIter.hasNext()) {
                        Item srcAccession = (Item) srcAccIter.next();
                        String srcAccessionString = getAttributeValue(srcAccession, "accession");
                        Item synonym = new Item(getUniqueIdentifier(), TGT_NS + "Synonym", "");
                        synonym.addAttribute(new Attribute("value", srcAccessionString));
                        synonym.addAttribute(new Attribute("type", "accession"));
                        synonym.addReference(new Reference("subject", protein.getIdentifier()));
                        synonym.addReference(new Reference("source", databaseId));
                        retval.add(synonym);
                    }
                }
                List srcComments = getItemsInCollection(srcItem.getCollection("comments"));
                Iterator srcComIter = srcComments.iterator();
                while (srcComIter.hasNext()) {
                    Item srcComment = (Item) srcComIter.next();
                    String srcCommentType = getAttributeValue(srcComment, "type");
                    String srcCommentText = getAttributeValue(srcComment, "text");
                    if ((srcCommentType != null) && (srcCommentText != null)) {
                        Item comment = new Item(getUniqueIdentifier(), TGT_NS + "Comment", "");
                        comment.addAttribute(new Attribute("type", srcCommentType));
                        comment.addAttribute(new Attribute("text", srcCommentText));
                        comment.addReference(new Reference("subject", protein.getIdentifier()));
                        comment.addReference(new Reference("source", databaseId));
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
                        Item synonym = new Item(getUniqueIdentifier(), TGT_NS + "Synonym", "");
                        synonym.addAttribute(new Attribute("type", "name"));
                        synonym.addAttribute(new Attribute("value", srcProteinNameString));
                        synonym.addReference(new Reference("subject", protein.getIdentifier()));
                        synonym.addReference(new Reference("source", databaseId));
                        retval.add(synonym);
                    }
                }
                Reference geneOrganismReference = null;
                if (taxonId == 7227) {
                    if (drosophilaId == null) {
                        Item drosophila = new Item(getUniqueIdentifier(), TGT_NS + "Organism", "");
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
                        Item caenorhabditis = new Item(getUniqueIdentifier(), TGT_NS + "Organism",
                                "");
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
                                Item publication = new Item(getUniqueIdentifier(), TGT_NS
                                        + "Publication", "");
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
                            primaryGeneName = new String(getAttributeValue((Item) srcGeneNames
                                        .get(0), "name"));
                        }
                    }
                    if (taxonId == 7227) {
                        // Drosophila - take the gene id from the FlyBase dbReference that has a
                        // property type = "gene designation" value = the primary gene name
                        if (primaryGeneName == null) {
                            throw new InterMineException("primaryGeneName is null for Drosophila"
                                    + " gene for protein with name " + proteinName);
                        }
                        List srcDbReferences = getItemsInCollection(srcItem
                                .getCollection("dbReferences"), "type", "FlyBase");
                        Iterator srcDbRefIter = srcDbReferences.iterator();
                        while (srcDbRefIter.hasNext()) {
                            Item srcDbReference = (Item) srcDbRefIter.next();
                            String type = getAttributeValue(srcDbReference, "type");
                            if ("FlyBase".equals(type)) {
                                if ((srcDbReferences.size() == 1) && (srcGenes.size() == 1)) {
                                    geneIdentifier = new String(getAttributeValue(srcDbReference,
                                                "id"));
                                } else {
                                    List srcProperties = getItemsInCollection(srcDbReference
                                            .getCollection("propertys"));
                                    Iterator srcPropertyIter = srcProperties.iterator();
                                    while (srcPropertyIter.hasNext()) {
                                        Item srcProperty = (Item) srcPropertyIter.next();
                                        String srcPropertyValue = getAttributeValue(srcProperty,
                                                "value");
                                        if ("gene designation".equals(getAttributeValue(srcProperty,
                                                        "type"))
                                                && (geneNames.contains(srcPropertyValue)
                                                    || geneNames.contains(srcPropertyValue
                                                        .substring(srcPropertyValue
                                                            .lastIndexOf("\\") + 1)))) {
                                            geneIdentifier = new String(getAttributeValue(
                                                        srcDbReference, "id"));
                                        }
                                    }
                                }
                            } else {
                                LOG.error("Found a non-FlyBase dbReference when it should have been"
                                        + " filtered out");
                            }
                        }
                        if (geneIdentifier == null) {
                            LOG.warn("Could not find identifier for Drosophila gene with name "
                                    + primaryGeneName + " for protein with name " + proteinName);
                        }
                    } else if (taxonId == 6239) {
                        // Caenorhabditis - get the gene id from the ORF name
                        Iterator srcGeneNameIter = srcGeneNames.iterator();
                        while (srcGeneNameIter.hasNext()) {
                            Item srcGeneName = (Item) srcGeneNameIter.next();
                            if ("ORF".equals(getAttributeValue(srcGeneName, "type"))) {
                                geneIdentifier = new String(getAttributeValue(srcGeneName, "name"));
                            }
                        }
                        if (geneIdentifier == null) {
                            LOG.warn("Could not find identifier for C. Elegans gene with name "
                                    + primaryGeneName + " for protein with name " + proteinName);
                        }
                    }
                    if (geneIdentifier != null) {
                        // We have a gene id.
                        String geneId = (String) geneIdentifierToId.get(geneIdentifier);
                        if (geneId == null) {
                            Item gene = new Item(getUniqueIdentifier(), TGT_NS + "Gene", "");
                            gene.addAttribute(new Attribute("organismDbId", geneIdentifier));
                            if (primaryGeneName != null) {
                                gene.addAttribute(new Attribute("identifier", primaryGeneName));
                            } else {
                                gene.addAttribute(new Attribute("identifier", geneIdentifier));
                            }
                            gene.addReference(geneOrganismReference);
                            ReferenceList geneSynonyms = new ReferenceList("synonyms",
                                    new ArrayList());
                            String primaryName = null;
                            Iterator srcGeneNameIter = srcGeneNames.iterator();
                            while (srcGeneNameIter.hasNext()) {
                                Item srcGeneName = (Item) srcGeneNameIter.next();
                                String type = getAttributeValue(srcGeneName, "type");
                                String name = getAttributeValue(srcGeneName, "name");
                                if ("primary".equals(type)) {
                                    gene.addAttribute(new Attribute("name", name));
                                }
                                Item synonym = new Item(getUniqueIdentifier(), TGT_NS + "Synonym",
                                        "");
                                synonym.addAttribute(new Attribute("value", name));
                                synonym.addAttribute(new Attribute("type", type));
                                synonym.addReference(new Reference("subject",
                                            gene.getIdentifier()));
                                synonym.addReference(new Reference("source", databaseId));
                                retval.add(synonym);
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
                    }
                }
                retval.add(protein);
                return retval;
            }
        } else {
            LOG.warn("Found non-EntryType Item");
        }
        return Collections.EMPTY_SET;
    }

    public List getItemsInCollection(ReferenceList col) throws ObjectStoreException {
        return getItemsInCollection(col, null, null);
    }

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
        List retval = new ArrayList();
        Iterator uncoIter = unconvertedResults.iterator();
        while (uncoIter.hasNext()) {
            org.intermine.model.fulldata.Item item = (org.intermine.model.fulldata.Item) uncoIter
                .next();
            retval.add(ItemHelper.convert(item));
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

    public String getUniqueIdentifier() {
        return "0_" + (idSequence++);
    }

    /**
     * Main method
     * @param args command line arguments
     * @throws Exception if something goes wrong
     */
    public static void main (String[] args) throws Exception {
        String srcOsName = args[0];
        String tgtOswName = args[1];
        String modelName = args[2];
        String format = args[3];
        String namespace = args[4];

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

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new ObjectStoreItemWriter(oswTgt);
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc, paths);

        //OntModel model = ModelFactory.createOntologyModel();
        //model.read(new FileReader(new File(modelName)), null, format);
        UniprotDataTranslator dt = new UniprotDataTranslator(srcItemReader, namespace);
        //model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
