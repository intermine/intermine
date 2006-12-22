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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
import java.util.Iterator;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ReferenceList;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


import java.util.HashSet;
import java.util.Set;


import org.apache.log4j.Logger;


/**
 * DataConverter to parse an FlyAtlas expression data into items
 * @author Richard Smith
 */
public class UniprotConverter extends FileConverter
{
    //TODO: This should come from props files!!!!
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final Logger LOG = Logger.getLogger(UniprotDataTranslator.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public UniprotConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
    }


    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {

        UniprotHandler handler = new UniprotHandler(writer);

        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    /**
     * Extension of PathQueryHandler to handle parsing TemplateQueries
     */
    static class UniprotHandler extends DefaultHandler
    {

        private ItemFactory itemFactory;
        private Item protein;
        private Item sequence;
        private Item organism;
        private Item gene;
        private Item pub;
        private Item comment;
        private Item dataSource;
        private Item dataSet;
        private Item synonym;
        private ItemWriter writer;
        private Map ids = new HashMap();
        private Map aliases = new HashMap();
        private Map pubs = new HashMap();
        private Map orgs = new HashMap();
        private Map databases = new HashMap();
        private Map taxIdToDb = new HashMap();
        private Map genes = new HashMap();
        private Map synonyms = new HashMap();
        private Map geneIdentifierToId = new HashMap();
        private Map dbReferences; // ok
        private Map geneNameTypeToName = new HashMap();
        private Set geneNames; // ok
        private Set geneIdentifiers = new HashSet();
        private int nextClsId = 0;
        private Stack stack = new Stack();
        private String attName = null;
        private StringBuffer attValue = null;
        private ReferenceList publications = null;
        private ReferenceList comments = null;
        private ReferenceList geneCollection = null;
        private StringBuffer descr = null;
        private String geneIdentifier;
        private String primaryGeneName;
        private String dbName;
        private String geneOrganismDbId;
        private String taxonId = null;
        private boolean hasEvidence = false;
        private String possibleId;
        private String possibleIdSource;

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public UniprotHandler(ItemWriter writer) {
            itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
            this.writer = writer;
        }


        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {

            attName = null;

            try {

                // <entry>
                if (qName.equals("entry")) {

                    protein = createItem("Protein");

                    // create, clear all lists for each new protein
                    initProtein();

                // <entry><protein>
                } else if (qName.equals("protein")) {

                    String isFragment = "false";

                    // check for <protein type="fragment*">
                    if (attrs.getValue("type") != null) {
                        String type = attrs.getValue("type");
                        if (type.startsWith("fragment")) {
                            isFragment = "true";
                        }
                    }
                    protein.setAttribute("isFragment", isFragment);

                    // <entry><protein evidence="*">
                    String strEvidence = attrs.getValue("evidence");
                    if (strEvidence != null) {

                        strEvidence = " (Evidence " + strEvidence + ")";
                        synonym  = createSynonym(protein.getIdentifier(), "name",
                                                         strEvidence, dataSource.getIdentifier());

                        // don't have the descr of this protein yet
                        // so set flag to indicate this needs to be added later
                        if (synonym != null) {
                            hasEvidence = true;
                        }
                    }

                // <entry><protein><name>
                } else if (qName.equals("name") && stack.peek().equals("protein")) {

                    attName = "name";

                // <entry><name>
                } else if (qName.equals("name") && stack.peek().equals("entry")) {

                    attName = "identifier";

                // <entry><accession>
                } else if (qName.equals("accession")) {

                    attName = "value";

                // <entry><sequence>
                } else if (qName.equals("sequence")) {

                    sequence = createItem("Sequence");
                    sequence.setAttribute("length", attrs.getValue("length"));

                    protein.setAttribute("length", attrs.getValue("length"));
                    protein.setAttribute("molecularWeight", attrs.getValue("mass"));

                    attName = "residues";

                // <entry><organism><dbreference>
                } else if (qName.equals("dbReference") && stack.peek().equals("organism")) {

                    taxonId = (String) attrs.getValue("id");

                    // if organism isn't in master list, add
                    if (orgs.get(taxonId) == null) {

                        organism = createItem("Organism");
                        orgs.put(attrs.getValue("id"), organism.getIdentifier());
                        organism.setAttribute("taxonId", taxonId);
                        writer.store(ItemHelper.convert(organism));

                    }

                    protein.setReference("organism", organism.getIdentifier());
                    // get relevant database for this organism
                    dbName = (String) taxIdToDb.get(taxonId);
                    // now that we know the taxonID, we can store the genes
                    finaliseGenes();

                // <entry><reference><citation><dbreference>
                } else if (qName.equals("dbReference") && stack.peek().equals("citation")
                           && attrs.getValue("type").equals("PubMed")) {

                    String pubId = null;

                    // if publication isn't in  master list, add it
                    // otherwise, just get the publicationID from the master list
                    if (pubs.get(attrs.getValue("id")) == null) {

                        pub = createItem("Publication");
                        pub.setAttribute("pubMedId", attrs.getValue("id"));
                        pubs.put(attrs.getValue("id"), pub.getIdentifier());
                        pubId = pub.getIdentifier();

                    } else {
                        pubId = (String) pubs.get(attrs.getValue("id"));
                    }

                    // if this is the first publication for this protein, add collection
                    if (publications.getRefIds().isEmpty()) {
                        protein.addCollection(publications);
                    }

                    // add this publication to list of publications for this protein
                    publications.addRefId(pubId);

                // <entry><comment>
                } else if (qName.equals("comment") && attrs.getValue("type") != null) {

                    comment = createItem("Comment");
                    comment.setAttribute("type", attrs.getValue("type"));

                // <entry><comment><text>
                } else if (qName.equals("text") && stack.peek().equals("comment")) {

                    attName = "text";

                // gene
                } else if (qName.equals("gene")) {

                    gene = createItem("Gene");

                    if (geneCollection == null) {
                        geneCollection = new ReferenceList("genes", new ArrayList());
                        protein.addCollection(geneCollection);
                    }
                    geneCollection.addRefId(gene.getIdentifier());

                    initGene();


                // <entry><gene>
                } else if (qName.equals("name") && stack.peek().equals("gene")) {

                    // will be primary, ORF, synonym or ordered locus
                    attName = attrs.getValue("type");

                // <dbreference type="EC">
                } else if (qName.equals("dbReference") && attrs.getValue("type").equals("EC")) {

                    String ecNumber = attrs.getValue("id");
                    if (ecNumber != null) {
                        protein.setAttribute("ecNumber", ecNumber);
                    }

                // <dbreference type="FlyBase/UniProt/etc.." id="*" key="12345">
                } else if (qName.equals("dbReference")
                           && taxIdToDb.containsValue(attrs.getValue("type"))) {

                    // could be identifiers but check next tag to see if this is a gene designation
                    possibleId = attrs.getValue("id");
                    possibleIdSource = attrs.getValue("type");


                // <dbreference><property type="gene designation" value="*">
                } else if (qName.equals("property") && stack.peek().equals("dbReference")
                           && attrs.getValue("type").equals("gene designation")
                           && geneNames.contains(attrs.getValue("value"))) {

                    // add to map if valid
                    dbReferences.put(possibleIdSource, new String(possibleId));
                    geneOrganismDbId = possibleId;

                // <uniprot>
                } else if (qName.equals("uniprot")) {

                    // set up datasources
                    init();

                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();

        }


        /**
         * @see DefaultHandler#endElement
         */
        public void characters(char[] ch, int start, int length) throws SAXException
        {

            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (length > 0) {
                    boolean whitespace = false;
                    switch(ch[start]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        whitespace = true;
                        break;
                    default:
                        break;
                    }
                    if (!whitespace) {
                        break;
                    }
                    ++start;
                    --length;
                }

                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    attValue.append(s);
                }
            }
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);

            try {
                stack.pop();

                // <entry>
                if (qName.equals("entry")) {

                    // only store the protein if it has a primary accession value
                    // ~~~~ what about other related items?  they are already stored? ~~~~
                    if (protein.getAttribute("primaryAccession") != null) {

                        protein.setAttribute("description", descr.toString());
                        ReferenceList evidence = new ReferenceList("evidence", new ArrayList());
                        protein.addCollection(evidence);
                        evidence.addRefId(dataSource.getIdentifier());
                        writer.store(ItemHelper.convert(protein));

                        // if <protein evidence="*">
                        if (hasEvidence) {

                            String s = protein.getAttribute("description").getValue();
                            s += synonym.getAttribute("value").getValue();
                            synonym.setAttribute("value", s);
                            writer.store(ItemHelper.convert(synonym));

                        }
                    } else {
                       LOG.info("Entry " + protein.getAttribute("name")
                                + " does not have any accessions");
                    }


                // <entry><sequence>
                } else if (qName.equals("sequence")) {

                    sequence.setAttribute(attName, attValue.toString());
                    writer.store(ItemHelper.convert(sequence));

                // <entry><protein><name>
                } else if (qName.equals("name") && stack.peek().equals("protein")) {

                    if (!protein.hasAttribute("name")) {
                        protein.setAttribute(attName, attValue.toString());
                        descr.append(attValue.toString());
                    } else {
                        descr.append(" (" + attValue.toString() + ")");
                    }

                    // all names are synonyms
                    Item syn = createSynonym(protein.getIdentifier(), "name", attValue.toString(),
                                  dataSource.getIdentifier());
                    if (syn != null) {
                        writer.store(ItemHelper.convert(syn));
                    }

                // <entry><reference><citation><dbreference>
                } else if (qName.equals("dbReference") && stack.peek().equals("citation")) {

                    writer.store(ItemHelper.convert(pub));

                // <entry><comment><text>
                } else if (qName.equals("text") && attName != null) {

                    if (comment.hasAttribute("type") && attValue.toString() != null) {

                        comment.setAttribute(attName, attValue.toString());
                        comment.setReference("source", dataSet.getIdentifier());
                        if (comments.getRefIds().isEmpty()) {
                            protein.addCollection(comments);
                        }
                        comments.addRefId(comment.getIdentifier());
                        writer.store(ItemHelper.convert(comment));
                    }

                // <entry><gene><name>
                } else if (qName.equals("name") && stack.peek().equals("gene")) {

                    String type = attName;
                    String name = attValue.toString();

                    geneNames.add(new String(name));
                    geneNameTypeToName.put(type, name);


                // <entry><name>
                } else if (qName.equals("name")) {

                    if (attName != null) {
                        protein.setAttribute(attName, attValue.toString());
                    }

                // <entry><accession>
                } else if (qName.equals("accession")) {

                    Item syn = createSynonym(protein.getIdentifier(), "accession",
                                           attValue.toString(), dataSource.getIdentifier());
                    if (syn != null) {

                        writer.store(ItemHelper.convert(syn));

                        // if this is the first accession value, its the primary accession
                        if (protein.getAttribute("primaryAccession") == null) {
                            protein.setAttribute("primaryAccession", attValue.toString());
                        }
                    }
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        // if synonym not already create and put in synonyms map for this <entry>
        private Item createSynonym(String subjectId, String type, String value, String dbId) {
            String key = subjectId + type + value + dbId;
            if (!synonyms.containsKey(key)) {
                Item syn = createItem("Synonym");
                syn.addReference(new Reference("subject", subjectId));
                syn.addAttribute(new Attribute("type", type));
                syn.addAttribute(new Attribute("value", value));
                syn.addReference(new Reference("source", dbId));
                synonyms.put(key, syn);
                return syn;
            } else {
                return null;
            }
        }

        /* clears all protein-related lists/values
           called when new protein is created    */
        private void initProtein() {

            // ~~~ maybe don't create this until comments are created? ~~~
            // ~~~ rename to commentCollection ~~~ ?
            comments = new ReferenceList("comments", new ArrayList());
            descr = new StringBuffer();
            publications = new ReferenceList("publications", new ArrayList());
            taxonId = null;
            synonym = null;
            geneCollection = null; // why aren't we creating a new list?
            dbName = null;
            comment = null; // need this?
            dbReferences = new HashMap();
         }

        private void initGene() {
            geneNames = new HashSet();
        }

        /*  sets up datasource and dataset, done only once */
        private void init()
            throws SAXException {
            try {

                // TODO: the dataset name shouldn't be hard coded:
                dataSource = getDataSource("Uniprot");

                dataSet = createItem("DataSet");
                dataSet.setAttribute("title", "Uniprot data set");

                writer.store(ItemHelper.convert(dataSource));
                writer.store(ItemHelper.convert(dataSet));

                // map which databases to use with which organisms
                mapDatabases();

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }


        private Item getDataSource(String title)
            throws SAXException {

            Item database = (Item) databases.get(title);
            try {

                if (database == null) {
                    database = createItem("DataSource");
                    database.addAttribute(new Attribute("name", title));
                    databases.put(title, database);
                    writer.store(ItemHelper.convert(database));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return database;
        }


        private void finaliseGenes()
            throws SAXException {
            try {

                // Gene.identifier = <entry><gene><name type="ORF">
                geneIdentifier = null;
                // Gene.name = <entry><gene><name type="primary">
                primaryGeneName = null;

                Iterator i = geneNameTypeToName.keySet().iterator();
                while (i.hasNext()) {

                    String type = (String) i.next();
                    String name = (String) geneNameTypeToName.get(type);
                    String synonymDescr = "";
                    String notCG = null;

                    if (type.equals("primary")) {

                        primaryGeneName = name;
                        synonymDescr = "symbol";

                    } else if (type.equals("ORF")) {

                        if (taxonId.equals("7227") && !name.startsWith("CG")) {
                            notCG = name;
                        } else {
                            geneIdentifier = name;
                        }
                        synonymDescr = "identifier";

                    } else if (type.equals("synonym")) {

                        synonymDescr =  "symbol";

                    } else if (type.equals("ordered locus")) {

                        synonymDescr = type;

                    }

                    // Some UniProt entries have CGxxx as Dmel_CGxxx - need to strip prefix
                    // so that they match identifiers from other sources.  Some genes have
                    // embl identifiers and no FlyBase id, ignore these.
                    if (geneIdentifier == null && notCG != null) {
                        if (notCG.startsWith("Dmel_")) {
                            geneIdentifier = notCG.substring(5);
                            name = geneIdentifier;
                        } else {
                            LOG.info("Found a Drosophila gene without a CG identifer: " + notCG);
                        }
                    }


                    // all gene names are synonyms
                    // ORF may be iden
                    if (!synonymDescr.equals("indentifier")) {
                        Item syn = createSynonym(gene.getIdentifier(), synonymDescr, name,
                                                 getDataSource(dbName).getIdentifier());
                        if (syn != null) {
                            writer.store(ItemHelper.convert(syn));
                        }
                    }
                }


                // define a gene identifier we always expect to find that is unique to this gene
                // is different for each organism
                String uniqueGeneIdentifier = null;
                // geneOrganismDbId = <entry><dbReference><type="FlyBase/WormBase/..">
                //             where designation = primary gene name
                // String geneOrganismDbId = null; <------ this is set above


                if (taxonId.equals("7227")) { // D. melanogaster
                    // UniProt has duplicate pairings of CGxxx and FBgnxxx, just get one id
                    geneOrganismDbId = null;
                    uniqueGeneIdentifier = geneIdentifier;

                } else if (taxonId.equals("6239")) { // C. elegans
                    // just get WBGeneXXX - ORF id is a gene *model* id,
                    // i.e. effectively a transcript
                    //geneOrganismDbId
                    //           = getDataSourceReferenceValue(srcItem, "WormBase", geneNames);
                    uniqueGeneIdentifier = geneOrganismDbId;
                    geneIdentifier = null;

                } else if (taxonId.equals("3702")) { // Arabidopsis thaliana
                    geneOrganismDbId = (String) geneNameTypeToName.get("ordered locus");
                    geneOrganismDbId = geneOrganismDbId.toUpperCase();
                    uniqueGeneIdentifier = geneOrganismDbId;

                } else if (taxonId.equals("4896")) {  // S. pombe
                    geneOrganismDbId = (String) geneNameTypeToName.get("ORF");
                    uniqueGeneIdentifier = geneOrganismDbId;

                } else if (taxonId.equals("180454")) { // A. gambiae str. PEST
                    // no organismDbId and no specific dbxref to ensembl - assume that
                    // geneIdentifier is always ensembl gene stable id and set organismDbId
                    // to be identifier
                    uniqueGeneIdentifier = geneIdentifier;
                    geneOrganismDbId = geneIdentifier;

                } else if (taxonId.equals("9606")) { // H. sapiens
                    //geneOrganismDbId = getDataSourceReferenceValue(srcItem, "Ensembl", null);
                    geneIdentifier = geneOrganismDbId;
                    uniqueGeneIdentifier = geneOrganismDbId;


                } else if (taxonId.equals("4932")) { // S. cerevisiae
                    // need to set SGD identifier to be SGD accession, also set organismDbId
                    geneIdentifier = (String) dbReferences.get("Ensembl");
                    geneOrganismDbId = (String) dbReferences.get("SGD");
                    uniqueGeneIdentifier = geneOrganismDbId;

                } else if (taxonId.equals("36329")) { // Malaria
                    geneOrganismDbId = geneIdentifier;
                    uniqueGeneIdentifier = geneIdentifier;

                } else if (taxonId.equals("10090")) { // Mus musculus

                    geneIdentifier = (String) dbReferences.get("Ensembl");
                    geneOrganismDbId = (String) dbReferences.get("MGI");
                    uniqueGeneIdentifier = geneOrganismDbId;

                } else if (taxonId.equals("10116")) { // Rattus norvegicus

                    geneIdentifier = (String) dbReferences.get("Ensembl");
                    geneOrganismDbId = (String) dbReferences.get("RGD");

                    // HACK in other places the RGD identifers start with 'RGD:'
                    if (geneOrganismDbId != null && !geneOrganismDbId.startsWith("RGD:")) {
                        geneOrganismDbId = "RGD:" + geneOrganismDbId;
                    }
                    uniqueGeneIdentifier = geneOrganismDbId;
                }

                // uniprot data source has primary key of Gene.organismDbId
                // only create gene if a value was found
                if (uniqueGeneIdentifier != null) {


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

                        if (geneOrganismDbId != null) {
                            if (geneOrganismDbId.equals("")) {
                                LOG.info("geneOrganismDbId was empty string");
                            }
                            gene.addAttribute(new Attribute("organismDbId", geneOrganismDbId));


                            Item syn = createSynonym(gene.getIdentifier(), "identifier",
                                                     geneOrganismDbId,
                                                     getDataSource(dbName).getIdentifier());
                            if (syn != null) {
                                writer.store(ItemHelper.convert(syn));
                            }

                        }

                        if (geneIdentifier != null) {
                            gene.addAttribute(new Attribute("identifier", geneIdentifier));
                            // don't create duplicate synonym
                            if (!geneIdentifier.equals(geneOrganismDbId)) {

                                Item syn = createSynonym(gene.getIdentifier(), "identifier",
                                                         geneIdentifier,
                                                         getDataSource(dbName).getIdentifier());
                                if (syn != null) {
                                    writer.store(ItemHelper.convert(syn));
                                }
                            }
                            // keep a track of non-null gene identifiers
                            geneIdentifiers.add(geneIdentifier);
                        }
                        // Problem with gene names for drosophila - ignore
                        if (primaryGeneName != null && !taxonId.equals("7227")) {
                            gene.addAttribute(new Attribute("symbol", primaryGeneName));
                        }

                        geneItemId = gene.getIdentifier();
                        geneIdentifierToId.put(uniqueGeneIdentifier, geneItemId);

                        // set values for gene
                        gene.setAttribute("identifier", uniqueGeneIdentifier);
                        gene.setReference("organism", organism.getIdentifier());
                        ReferenceList evidence = new ReferenceList("evidence", new ArrayList());
                        gene.addCollection(evidence);
                        evidence.addRefId(dataSource.getIdentifier());
                        writer.store(ItemHelper.convert(gene));

                    }
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }

        // makes map so we know which datasource to use for each organism
        private void mapDatabases() {

            //  this map is also used to check if the value here should be saved:
            //   <dbReference type="FlyBase/UniProt/etc" id="*" key="12345">
            taxIdToDb.put("7227", new String("FlyBase"));   // D. melanogaster
            taxIdToDb.put("6239", new String("WormBase"));  // C. elegans
            taxIdToDb.put("3702", new String("UniProt"));   // Arabidopsis thaliana
            taxIdToDb.put("4896", new String("GeneDB"));    // S. pombe
            taxIdToDb.put("180454", new String("Ensembl")); // A. gambiae str. PEST
            taxIdToDb.put("9606", new String("Ensembl"));   // H. sapiens
            taxIdToDb.put("4932", new String("SGD"));       // S. cerevisiae
            taxIdToDb.put("36329", new String("GeneDB"));   // Malaria
            taxIdToDb.put("10090", new String("Ensembl"));  // Mus musculus
            taxIdToDb.put("10116", new String("Ensembl"));  // Rattus norvegicus


            // a. these databases are used to obtain the geneOrganismDbId
            //    (instead of data source)
            // b. dummy taxonIDs are used
            // c. these are here because this map is used as a look up
            //    to see if the value in the dbreference's id field is an identifier
            taxIdToDb.put("-1", new String("SGD")); // S. cerevisiae [geneOrganismDbId]
            taxIdToDb.put("-2", new String("MGI")); // Mus musculus [geneOrganismDbId]
            taxIdToDb.put("-3", new String("RGD")); // Rattus norvegicus [geneOrganismDbId]
        }

        /**
         * Convenience method for creating a new Item
         * @param className the name of the class
         * @return a new Item
         */
        protected Item createItem(String className) {
            return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                        GENOMIC_NS + className, "");
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
         * Uniquely alias a className
         * @param className the class name
         * @return the alias
         */
        protected String alias(String className) {
            String alias = (String) aliases.get(className);
            if (alias != null) {
                return alias;
            }
            String nextIndex = "" + (nextClsId++);
            aliases.put(className, nextIndex);
            return nextIndex;
        }
    }
}

