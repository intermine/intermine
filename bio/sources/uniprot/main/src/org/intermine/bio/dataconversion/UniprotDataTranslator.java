package org.intermine.bio.dataconversion;

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
import java.util.Properties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.metadata.Model;
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
 * @author Richard Smith
 */
public class UniprotDataTranslator extends DataTranslator
{
    private static final Logger LOG = Logger.getLogger(UniprotDataTranslator.class);

    private Map databases = new HashMap();
    private Map pubMedIdToPublicationId = new HashMap();
    private Map geneIdentifierToId = new HashMap();
    private int pubLinkCount = 0;
    private Map organisms = new HashMap();
    private FileWriter fw = null;
    private boolean outputIdentifiers = false;
    //geneIdentifier is hugo id from ensembl-human, don't create
    private boolean createGeneIdentifier = true;
    private Item uniprotDataSet;
    private Reference uniprotDataSetRef;
    private Map ids = new HashMap();

    private Set geneIdentifiers = new HashSet();

    //TODO: This should come from props files!!!!
    private static final String SRC_NS = "http://www.intermine.org/model/uniprot#";

    /**
     * @see DataTranslator#DataTranslator(ItemReader, Properties, Model, Model)
     */
    public UniprotDataTranslator(ItemReader srcItemReader, Properties mapping,
                                 Model srcModel, Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
        this.srcItemReader = srcItemReader;

        uniprotDataSet = createItem(tgtNs + "DataSet");
        // TODO: the dataset name shouldn't be hard coded:
        uniprotDataSet.addAttribute(new Attribute("title", "UniProt data set"));
        uniprotDataSetRef = new Reference("source", uniprotDataSet.getIdentifier());
    }

    /**
     * @see DataTranslator#getItemIterator
     */
     public Iterator getItemIterator() {
         Query q = new Query();
         q.setDistinct(false);
         QueryClass qc1 = new QueryClass(org.intermine.model.fulldata.Item.class);
         q.addFrom(qc1);
         q.addToSelect(qc1);
         SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "className"),
                                   ConstraintOp.EQUALS, new QueryValue(SRC_NS + "Entry"));
         q.setConstraint(sc1);
         // TODO batch size usually 1000, increase again if ObjectStoreItemPathFollowingImpl query
         // generation creates queries that can use bag temprorary tables more effectively
         return ((ObjectStoreItemReader) srcItemReader).itemIterator(q, 1000);
     }


    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {
        try {
            tgtItemWriter.store(ItemHelper.convert(uniprotDataSet));

            if (outputIdentifiers) {
                fw = new FileWriter(new File("uniprot_gene_identifiers"));
            }
            super.translate(tgtItemWriter);

            // store databases
            Iterator i = databases.values().iterator();
            while (i.hasNext()) {
                tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
            }

            // store organisms
            i = organisms.values().iterator();
            while (i.hasNext()) {
                tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
            }
            if (outputIdentifiers) {
                fw.flush();
                fw.close();
            }
        } catch (IOException e) {
            throw new InterMineException(e);
        }
    }


    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        // This Item should be an EntryType and only have proteins associated with one organism.
        if (!(SRC_NS + "Entry").equals(srcItem.getClassName())) {
            throw new IllegalArgumentException("Attempted to translate and Uniprot source Item that"
                                               + " is not of class " + SRC_NS + "Entry");
        }

        // First things first: find out the taxonid of the organism of this entry.
        int taxonId = 0;
        List organismList = getItemsInCollection(srcItem.getCollection("organisms"));
        Iterator organismIter = organismList.iterator();
        while (organismIter.hasNext()) {
            // Drosophila melanogaster = 7227
            // Caenorhabditis elegans 6239
            // Anopheles gambiae = 7165
            // Homo sapiens = 9606
            Item organism = (Item) organismIter.next();
            Iterator dbRefIter = getItemsInCollection(
                                      organism.getCollection("dbReferences")).iterator();
            while (dbRefIter.hasNext()) {
                Item dbReference = (Item) dbRefIter.next();
                String type = getAttributeValue(dbReference, "type");
                if ("NCBI Taxonomy".equals(type)) {
                    String taxonString = getAttributeValue(dbReference, "id");
                    if (taxonId != 0) {
                        throw new IllegalStateException("Attempting to set taxon id to "
                                             + taxonString + " when it is already " + taxonId);
                    }
                    taxonId = Integer.parseInt(taxonString);
                }
            }
        }

        if (taxonId != 0) {
            Set retval = new HashSet();
            Reference organismReference = new Reference("organism", getOrganismId(taxonId));

            // 1. create Protein, same for all organisms
            // <entry>
            Item protein = createItem(tgtNs + "Protein");

            // find name and set all names as synonyms
            List proteinNames = getItemsInCollection(srcItem.getCollection("names"));
            String proteinName = getAttributeValue((Item) proteinNames.get(0), "name");
            protein.addAttribute(new Attribute("identifier", proteinName));
            protein.addReference(organismReference);
            retval.add(createSynonym(protein.getIdentifier(), "identifier",
                                     proteinName, getDataSourceId("UniProt")));

            // find primary accession and set others as synonyms <entry><accession>*
            List srcAccessions = getItemsInCollection(srcItem.getCollection("accessions"));
            if (srcAccessions.isEmpty()) {
                // do not create store Protein if no primary accession
                LOG.info("Entry " + proteinName + " does not have any accessions");
                return Collections.EMPTY_SET;
            } else {
                // primary accession is first in list
                Item srcPrimaryAccession = (Item) srcAccessions.get(0);
                protein.addAttribute(new Attribute("primaryAccession", getAttributeValue(
                                                           srcPrimaryAccession, "accession")));
                // all accessions should be Synonyms
                Iterator srcAccIter = srcAccessions.iterator();
                while (srcAccIter.hasNext()) {
                    Item srcAccession = (Item) srcAccIter.next();
                    String srcAccessionString = getAttributeValue(srcAccession, "accession");
                    retval.add(createSynonym(protein.getIdentifier(), "accession",
                                             srcAccessionString, getDataSourceId("UniProt")));
                }
            }
            // add UniProt Database to evidence collection
            ReferenceList evidence = new ReferenceList("evidence", new ArrayList());
            evidence.addRefId(getDataSourceId("UniProt"));
            protein.addCollection(evidence);


            // 2. first name should be protein name, all should be Synonyms
            // <entry><protein><name>*
            Item srcProtein = ItemHelper.convert(srcItemReader.getItemById(srcItem
                                                  .getReference("protein").getRefId()));
            if (srcProtein != null) {
                List srcProteinNames = getItemsInCollection(srcProtein.getCollection("names"));
                Iterator srcProtNameIter = srcProteinNames.iterator();
                while (srcProtNameIter.hasNext()) {
                    Item srcProteinName = (Item) srcProtNameIter.next();
                    String srcProteinNameStr = getAttributeValue(srcProteinName, "name");

                    if (!protein.hasAttribute("name")) {
                        protein.setAttribute("name", srcProteinNameStr);
                    }

                    String srcProteinNameEvidence = getAttributeValue(srcProteinName,
                                                                      "evidence");
                    if (srcProteinNameEvidence != null) {
                        srcProteinNameStr += " (Evidence " + srcProteinNameEvidence + ")";
                    }
                    retval.add(createSynonym(protein.getIdentifier(), "name",
                                             srcProteinNameStr, getDataSourceId("UniProt")));
                }
            }

            // 3. get collection of Comments
            // <entry><comment>*
            List srcComments = getItemsInCollection(srcItem.getCollection("comments"));
            ReferenceList comments = new ReferenceList("comments", new ArrayList());
            Iterator srcComIter = srcComments.iterator();
            while (srcComIter.hasNext()) {
                Item srcComment = (Item) srcComIter.next();
                String srcCommentType = getAttributeValue(srcComment, "type");
                String srcCommentText = getAttributeValue(srcComment, "text");
                if ((srcCommentType != null) && (srcCommentText != null)) {
                    Item comment = createItem(tgtNs + "Comment");
                    comment.addAttribute(new Attribute("type", srcCommentType));
                    comment.addAttribute(new Attribute("text", srcCommentText));
                    comment.addReference(uniprotDataSetRef);
                    comments.addRefId(comment.getIdentifier());
                    retval.add(comment);
                }
            }
            if (comments.getRefIds().size() > 0) {
                protein.addCollection(comments);
            }

            // 4. create a collection of Publications related to this protein
            // <entry><reference>*
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
                            Item publication = createItem(tgtNs + "Publication");
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

            // 5. create a Sequence object and reference from Protein
            // <entry><sequence>
            Reference seqRef = srcItem.getReference("sequence");
            if (seqRef != null) {
                Item srcSeq = ItemHelper.convert(srcItemReader.getItemById(seqRef.getRefId()));
                if (srcSeq != null) {
                    Item sequence = createItem(tgtNs + "Sequence");
                    String residues = srcSeq.getAttribute("sequence").getValue();
                    sequence.addAttribute(new Attribute("residues", residues));
                    sequence.addAttribute(new Attribute("length", "" + residues.length()));
                    retval.add(sequence);
                    protein.addReference(new Reference("sequence", sequence.getIdentifier()));
                }
            }

            // 6. Put an embl_seq_id in a protein so we can merge it with BioGrid's psi format data.
            // <entry><dbReference>
            List srcDbRefs = getItemsInCollection(srcItem.getCollection("dbReferences"));
            Iterator srcDbRefIter = srcDbRefs.iterator();
            while (srcDbRefIter.hasNext()) {
                Item dbRefItem = (Item) srcDbRefIter.next();
                String dbRefType = getAttributeValue(dbRefItem, "type");
                if ("EMBL".equals(dbRefType)) {
                    List props = getItemsInCollection(dbRefItem.getCollection("propertys"));
                    Iterator pIter = props.iterator();
                    String emblProteinId = null;
                    boolean isGenomicDna = false;
                    while (pIter.hasNext()) {
                        Item dbProp = (Item) pIter.next();
                        String dbPropType = getAttributeValue(dbProp, "type");
                        //<property type="protein sequence ID" value="AAB03417.3"/>
                        if ("protein sequence ID".equals(dbPropType)) {
                            emblProteinId = getAttributeValue(dbProp, "value");
                        }
                        //<property type="molecule type" value="Genomic_DNA"/>
                        else if ("molecule type".equals(dbPropType)) {
                            String moleType = getAttributeValue(dbProp, "value");
                            if ("Genomic_DNA".equals(moleType)) {
                                isGenomicDna = true;
                            }
                        }
                        //Potentially we may need to mod this if there is more than one per <entry>
                        if (isGenomicDna && (emblProteinId != null)) {
                            protein.setAttribute("emblProteinId",
                                            (emblProteinId.indexOf(".") > 0
                                            ? emblProteinId.substring(0, emblProteinId.indexOf("."))
                                            : emblProteinId));
                            break;
                        }
                    }
                }
            }
            // 7. now try to create reference to gene, choice of identifier is organism specific
            // <entry><gene>*
            // <entry><dbReference>

            // a protein can be related to multiple genes
            List srcGenes = getItemsInCollection(srcItem.getCollection("genes"));
            ReferenceList geneCollection = null;
            Iterator srcGeneIter = srcGenes.iterator();
            while (srcGeneIter.hasNext()) {
                Item srcGene = (Item) srcGeneIter.next();
                List srcGeneNames = getItemsInCollection(srcGene.getCollection("names"));
                // Gene.identifier = <entry><gene><name type="ORF">
                String geneIdentifier = null;
                // Gene.name = <entry><gene><name type="primary">
                String primaryGeneName = null;
                // stores the contents of the <entry><gene> element:
                Map geneNameTypeToName = new HashMap();
                Set geneNames = new HashSet();
                {
                    Iterator srcGeneNameIter = srcGeneNames.iterator();
                    String notCG = null;
                    while (srcGeneNameIter.hasNext()) {
                        Item srcGeneName = (Item) srcGeneNameIter.next();
                        String type = getAttributeValue(srcGeneName, "type");
                        String name = getAttributeValue(srcGeneName, "name");
                        geneNameTypeToName.put(type, name);
                        if ("primary".equals(type)) {
                            primaryGeneName = name;
                        } else if ("ORF".equals(type)) {
                            String tmp = name;
                            if ((taxonId == 7227) && (!tmp.startsWith("CG"))) {
                                notCG = tmp;
                            } else {
                                geneIdentifier = tmp;
                            }
                        }
                        geneNames.add(new String(name));
                    }
                    // Some UniProt entries have CGxxx as Dmel_CGxxx - need to strip prefix
                    // so that they match identifiers from other sources.  Some genes have
                    // embl identifiers and no FlyBase id, ignore these.
                    if (geneIdentifier == null && notCG != null) {
                        if (notCG.startsWith("Dmel_")) {
                            geneIdentifier = notCG.substring(5);
                        } else {
                            LOG.info("Found a Drosophila gene without a CG identifer: " + notCG);
                        }
                    }
                }

                String dbId = null;
                // define a gene identifier we always expect to find that is unique to this gene
                // is different for each organism
                String uniqueGeneIdentifier = null;
                // geneOrganismDbId = <entry><dbReference><type="FlyBase/WormBase/..">
                //             where designation = primary gene name
                String geneOrganismDbId = null;
                if (taxonId == 7227) { // D. melanogaster
                    geneOrganismDbId = getDataSourceReferenceValue(srcItem, "FlyBase", geneNames);
                    // For fly data use CGxxx as key instead of FBgnxxx
                    uniqueGeneIdentifier = geneIdentifier;
                    dbId = getDataSourceId("FlyBase");
                } else if (taxonId == 6239) { // C. elegans
                    // leave gene identifier as ORF id, is already ensembl id
                    geneOrganismDbId = getDataSourceReferenceValue(srcItem, "WormBase", geneNames);
                    // was organismDbId, ok ot change?
                    uniqueGeneIdentifier = geneIdentifier;
                    dbId = getDataSourceId("WormBase");
                } else if (taxonId == 3702) { // Arabidopsis thaliana
                    geneOrganismDbId = (String) geneNameTypeToName.get("ordered locus");
                    geneOrganismDbId = geneOrganismDbId.toUpperCase();
                    uniqueGeneIdentifier = geneOrganismDbId;
                    dbId = getDataSourceId("UniProt");
                } else if (taxonId == 4896) {  // S. pombe
                    geneOrganismDbId = (String) geneNameTypeToName.get("ORF");
                    uniqueGeneIdentifier = geneOrganismDbId;
                    dbId = getDataSourceId("GeneDB");
                } else if (taxonId == 180454) { // A. gambiae str. PEST
                    // no organismDbId and no specific dbxref to ensembl - assume that
                    // geneIdentifier is always ensembl gene stable id and set organismDbId
                    // to be identifier
                    uniqueGeneIdentifier = geneIdentifier;
                    geneOrganismDbId = geneIdentifier;
                    dbId = getDataSourceId("Ensembl");
                } else if (taxonId == 9606) { // H. sapiens
                    geneOrganismDbId = getDataSourceReferenceValue(srcItem, "Ensembl", null);
                    geneIdentifier = geneOrganismDbId;
                    uniqueGeneIdentifier = geneOrganismDbId;
                    dbId = getDataSourceId("Ensembl");
                } else if (taxonId == 4932) { // S. cerevisiae
                    // need to set SGD identifier to be SGD accession, also set organismDbId
                    geneIdentifier = getDataSourceReferenceValue(srcItem, "Ensembl", geneNames);
                    geneOrganismDbId = getDataSourceReferenceValue(srcItem, "SGD", geneNames);
                    uniqueGeneIdentifier = geneOrganismDbId;
                    dbId = getDataSourceId("SGD");
                } else if (taxonId == 36329) { // Malaria
                    geneOrganismDbId = geneIdentifier;
                    uniqueGeneIdentifier = geneIdentifier;
                    dbId = getDataSourceId("GeneDB");
                } else if (taxonId == 10090) { // Mus musculus
                    geneIdentifier = getDataSourceReferenceValue(srcItem, "Ensembl", geneNames);
                    geneOrganismDbId = getDataSourceReferenceValue(srcItem, "MGI", geneNames);
                    uniqueGeneIdentifier = geneOrganismDbId;
                    dbId = getDataSourceId("Ensembl");
                } else if (taxonId == 10116) { // Rattus norvegicus
                    geneIdentifier = getDataSourceReferenceValue(srcItem, "Ensembl", geneNames);
                    geneOrganismDbId = getDataSourceReferenceValue(srcItem, "RGD", geneNames);
                    // HACK in other places the RGD identifers start with 'RGD:'
                    if (geneOrganismDbId != null && !geneOrganismDbId.startsWith("RGD:")) {
                        geneOrganismDbId = "RGD:" + geneOrganismDbId;
                    }
                    uniqueGeneIdentifier = geneOrganismDbId;
                    dbId = getDataSourceId("Ensembl");
                }

                // output gene identifier details
                if (outputIdentifiers) {
                    try {
                        fw.write(taxonId + "\tprotein: " + proteinName + "\tname: "
                                 + primaryGeneName + "\tidentifier: " + geneIdentifier
                                 + "\tgeneOrganismDbId: " + geneOrganismDbId
                                 + System.getProperty("line.separator"));
                    } catch (IOException e) {
                        throw new InterMineException(e);
                    }
                }

                // uniprot data source has primary key of Gene.organismDbId
                // only create gene if a value was found
                if (uniqueGeneIdentifier != null) {
                    // may alrady have created this gene
                    String geneItemId = (String) geneIdentifierToId.get(uniqueGeneIdentifier);

                    // UniProt sometimes has same identifier paired with two organismDbIds
                    // causes problems merging other data sources.  Simple check to prevent
                    // creating a gene with a duplicate identifier.

                    // log problem genes
                    boolean isDuplicateIdentifier = false;
                    if ((geneItemId == null) && geneIdentifiers.contains(geneIdentifier)) {
                        LOG.warn("already created a gene for identifier: " + geneIdentifier
                                 + " with different organismDbId, discarding this one");
                        isDuplicateIdentifier = true;
                    }
                    if ((geneItemId == null) && !isDuplicateIdentifier) {
                        Item gene = createItem(tgtNs + "Gene");
                        if (geneOrganismDbId != null) {
                            if (geneOrganismDbId.equals("")) {
                                LOG.info("geneOrganismDbId was empty string");
                            }
                            gene.addAttribute(new Attribute("organismDbId", geneOrganismDbId));
                            Item synonym = createSynonym(gene.getIdentifier(), "identifier",
                                                         geneOrganismDbId, dbId);
                            retval.add(synonym);
                        }

                        if (geneIdentifier != null && createGeneIdentifier) {
                            gene.addAttribute(new Attribute("identifier", geneIdentifier));
                            // don't create duplicate synonym
                            if (!geneIdentifier.equals(geneOrganismDbId)) {
                                Item synonym = createSynonym(gene.getIdentifier(), "identifier",
                                                             geneIdentifier, dbId);
                                retval.add(synonym);
                            }
                            // keep a track of non-null gene identifiers
                            geneIdentifiers.add(geneIdentifier);
                        }
                        // Problem with gene names for drosophila - ignore
                        if (primaryGeneName != null && taxonId != 7227) {
                            gene.addAttribute(new Attribute("symbol", primaryGeneName));
                        }
                        gene.addReference(organismReference);

                        // add UniProt Database to evidence collection
                        ReferenceList evidence1 = new ReferenceList("evidence", new ArrayList());
                        evidence1.addRefId(getDataSourceId("UniProt"));
                        gene.addCollection(evidence1);

                        // everything in <entry><gene><name>* becomes a Synonym
                        Iterator srcGeneNameIter = srcGeneNames.iterator();
                        while (srcGeneNameIter.hasNext()) {
                            Item srcGeneName = (Item) srcGeneNameIter.next();
                            String type = getAttributeValue(srcGeneName, "type");
                            String symbol = getAttributeValue(srcGeneName, "name");
                            // synonym already created for ORF as identifer
                            if (!type.equals("ORF")) {
                                Item synonym = createSynonym(gene.getIdentifier(),
                                    (type.equals("primary") || type.equals("synonym"))
                                                             ? "symbol" : type,
                                                             symbol, dbId);
                                retval.add(synonym);
                            }
                        }
                        geneItemId = gene.getIdentifier();
                        geneIdentifierToId.put(uniqueGeneIdentifier, geneItemId);
                        retval.add(gene);
                    }

                    // TODO untidy - sould just do this check once
                    if (!isDuplicateIdentifier) {
                        if (geneCollection == null) {
                            geneCollection = new ReferenceList("genes", new ArrayList());
                            protein.addCollection(geneCollection);
                        }
                        geneCollection.addRefId(geneItemId);
                    }
                }
            }

            retval.add(protein);
            return retval;
        }
        return Collections.EMPTY_SET;
    }


    private Item createSynonym(String subjectId, String type, String value, String dbId) {
        Item synonym = createItem(tgtNs + "Synonym");
        synonym.addReference(new Reference("subject", subjectId));
        synonym.addAttribute(new Attribute("type", type));
        synonym.addAttribute(new Attribute("value", value));
        synonym.addReference(new Reference("source", dbId));
        return synonym;
    }

    private String getDataSourceId(String title) {
        return getDataSource(title).getIdentifier();
    }

    private Item getDataSource(String title) {
        Item database = (Item) databases.get(title);
        if (database == null) {
            database = createItem(tgtNs + "DataSource");
            database.addAttribute(new Attribute("name", title));
            databases.put(title, database);
        }
        return database;
    }

    private String getOrganismId(int taxonId) {
        return getOrganism(taxonId).getIdentifier();
    }

    private Item getOrganism(int taxonId) {
        Integer taxonIdObj = new Integer(taxonId);
        if (organisms.get(taxonIdObj) == null) {
            Item organism = createItem(tgtNs + "Organism");
            organism.addAttribute(new Attribute("taxonId", taxonIdObj.toString()));
            organisms.put(taxonIdObj, organism);
        }
        return (Item) organisms.get(taxonIdObj);
    }


    /**
     * Get a value for a DBReference relating to a gene that has a "gene designation" property
     * that marches a gene name.
     * @param srcItem the uniprot Entry item
     * @param dbName the type of the DBReference to find
     * @param geneNames the available names for the gene
     * @return the DBReference value or null of none found
     * @throws ObjectStoreException if problem reading database
     */
    protected String getDataSourceReferenceValue(Item srcItem, String dbName, Set geneNames)
        throws ObjectStoreException {
        String geneIdentifier = null;
        List srcDbRefs = getItemsInCollection(srcItem.getCollection("dbReferences"),
                                              "type", dbName);
        Iterator srcDbRefIter = srcDbRefs.iterator();
        while (srcDbRefIter.hasNext()) {
            Item srcDbReference = (Item) srcDbRefIter.next();
            String type = getAttributeValue(srcDbReference, "type");
            if (dbName.equals(type)) {
                // uncomment to set geneIdentifier if there is no propertys collection present (?)
                if (srcDbRefs.size() == 1) {
                    geneIdentifier = new String(getAttributeValue(srcDbReference,"id"));
                } else {
                    List srcProperties = getItemsInCollection(srcDbReference
                                                              .getCollection("propertys"));
                    Iterator srcPropertyIter = srcProperties.iterator();
                    while (srcPropertyIter.hasNext()) {
                        Item srcProperty = (Item) srcPropertyIter.next();
                        String srcPropertyValue = getAttributeValue(srcProperty, "value");
                        String srcPropType = getAttributeValue(srcProperty, "type");
                        if (geneNames != null && srcPropType.equals("gene designation")
                            && (geneNames.contains(srcPropertyValue)
                                || geneNames.contains(srcPropertyValue
                                                      .substring(srcPropertyValue
                                                                 .lastIndexOf("\\") + 1)))) {
                            geneIdentifier = new String(getAttributeValue(
                                                                          srcDbReference, "id"));
                        } else if (geneNames == null && srcDbRefs.size() == 1) {
                            geneIdentifier = new String(getAttributeValue(srcDbReference, "id"));
                        }
                        if (geneIdentifier == null) {
                            LOG.info("Found dbRefs (" + srcDbRefs.size()
                                     + ") but unable to match gene designation");
                        }
                    }
                }
            }
        }
        return geneIdentifier;
    }



    /**
     * Given a ReferenceList fetch items and return a list of Items in the order that their
     * identifiers appear in the original ReferenceList.
     * @param col name of the referencelist to retrieve
     * @return list of items in order their identifiers appeared in ReferenceList
     * @throws ObjectStoreException if problems accessing database
     */
    public List getItemsInCollection(ReferenceList col) throws ObjectStoreException {
        return getItemsInCollection(col, null, null);
    }


    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                    className, "");
    }

    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        id = new Integer(id.intValue() + 1);
        ids.put(className, id);
        return id.toString();
    }

    /**
     * Given a ReferenceList fetch items and return a list of Items in the order that their
     * identifiers appear in the original ReferenceList.
     * @param col name of the referencelist to retrieve
     * @param attributeName if not null only retrieve items with given attribute name and value
     * @param attributeValue if not null only retrieve items with given attribute name and value
     * @return list of items in order their identifiers appeared in ReferenceList
     * @throws ObjectStoreException if problems accessing database
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



    private String getAttributeValue(Item item, String name) {
        Attribute attribute = item.getAttribute(name);
        if (attribute != null) {
            return attribute.getValue();
        }
        return null;
    }



    /**
     * @see org.intermine.bio.task.DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        Set descs = new HashSet();
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("entry.organisms");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("organisms",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                "entry.organisms.dbReference");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("dbReference",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entry.accessions");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("accessions",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entry.comments");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("comments",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entry.protein");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("protein",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("entry.protein.names");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entry.references");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("references",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("entry.references.citation");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("citation",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        ItemPrefetchDescriptor desc3 = new ItemPrefetchDescriptor(
                "entry.references.citation.dbReferences");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("dbReferences",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entry.dbReferences");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("dbReferences",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addConstraint(new FieldNameAndValue("type", "FlyBase", false));
        desc2 = new ItemPrefetchDescriptor("entry.dbReferences.propertys");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("propertys",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);
        desc = new ItemPrefetchDescriptor("entry.genes");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("genes",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("entry.genes.names");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("names",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descs.add(desc);

        paths.put(SRC_NS + "Entry", descs);
        return paths;
    }
}
