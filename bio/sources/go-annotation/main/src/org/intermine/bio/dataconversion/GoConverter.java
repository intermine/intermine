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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.OboParser;
import org.intermine.util.PropertiesUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse a go annotation file into Items
 *
 *
 * @author Andrew Varley
 * @author Peter Mclaren - some additions to record the parents of a go term.
 * @author Julie Sullivan - changed evidence to be collection
 */
public class GoConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    protected static final String PROP_FILE = "go-annotation_config.properties";

    protected Map goTerms = new LinkedHashMap();
    protected Map goAnnoItems = new LinkedHashMap();
    protected Map goEvidence = new LinkedHashMap(); // list of evidence terms, e.g. IEA
    protected Map datasources = new LinkedHashMap();
    protected Map publications = new LinkedHashMap();
    protected Map organisms = new LinkedHashMap();
    protected Map termIdNameMap = new LinkedHashMap();
    protected int id = 0;
    protected Map ids = new HashMap();
    protected File ontology;
    protected Map withTypes = new LinkedHashMap();
    protected Map synonymTypes = new HashMap();
    protected Map productWrapperMap = new LinkedHashMap(), geneAttributes = new HashMap();
    protected Map holderMap = new LinkedHashMap();
    protected ItemFactory itemFactory;
    private OboParser oboParser = null;

    /*Some Debugging vars*/
    private static final String STORE_ONE = "store_1";
    //private static final String STORE_TWO = "store_2";
    private static final String STORE_THREE = "store_3";

    private static final Logger LOG = Logger.getLogger(GoConverter.class);

    /**
     * Constructor
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @throws Exception if an error occurs in storing or finding Model
     */
    public GoConverter(ItemWriter writer) throws Exception {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        addWithType("FB", "Gene", "organismDbId");
        addWithType("UniProt", "Protein", "primaryAccession");
        synonymTypes.put("protein", "accession");
        synonymTypes.put("Protein", "accession");
        synonymTypes.put("gene", "identifier");
        synonymTypes.put("Gene", "identifier");

        Item dataSourceItem = createItem("DataSource");
        dataSourceItem.setAttribute("name", "Gene Ontology");

        Item dataSetItem = createItem("DataSet");
        Date now = new Date(System.currentTimeMillis());

        dataSetItem.setAttribute("description", "GO Annotation loaded on " + now.toString());
        dataSetItem.setReference("dataSource", dataSourceItem);

        readConfig();
    }

    // read config file that has specific settings for each organism, key is taxon id
    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }
        Enumeration propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String taxonId = (String) propNames.nextElement();
            taxonId = taxonId.substring(0, taxonId.indexOf("."));
            Properties taxonProps = PropertiesUtil.stripStart(taxonId,
                PropertiesUtil.getPropertiesStartingWith(taxonId, props));
            String geneAttribute = taxonProps.getProperty("geneAttribute").trim();
            if (geneAttribute == null) {
                throw new IllegalArgumentException("Unable to find geneAttribute property for "
                                                   + "taxon: " + taxonId + " in file: "
                                                   + PROP_FILE);
            }
            if (!(geneAttribute.equals("identifier") || geneAttribute.equals("organismDbId"))) {
                throw new IllegalArgumentException("Invalid geneAttribute value for taxon: "
                                                   + taxonId + " was: " + geneAttribute);
            }
            geneAttributes.put(taxonId, geneAttribute);
        }
    }

    /**
     * Set the GO ontology file to be read from DAG format.
     *
     * @param ontology the GO ontology file
     */
    public void setOntologyfile(File ontology) {
        this.ontology = ontology;
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws ObjectStoreException, IOException {

        // Renew this at the start of processing each file
        productWrapperMap = new LinkedHashMap();
        holderMap = new LinkedHashMap();
        goAnnoItems = new LinkedHashMap();

        if (ontology == null) {
            throw new BuildException("ontology must be specified");
        }
        try {
            if (ontology.getName().endsWith(".ontology") || ontology.getName().endsWith(".dag")) {
                termIdNameMap = new DagParser().getTermIdNameMap(new FileReader(ontology));
            } else if (ontology.getName().endsWith(".obo")) {
                oboParser = new OboParser();
                termIdNameMap = oboParser.getTermIdNameMap(new FileReader(ontology));
            } else {
                throw new IllegalArgumentException("Don't know how to deal with ontology file:"
                        + ontology.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new IOException("" + e);
        }

        BufferedReader br = new BufferedReader(reader);
        String line;

        // loop through entire file
        while ((line = br.readLine()) != null) {
            if (line.startsWith("!")) {
                continue;
            }
            String[] array = line.split("\t", -1); //keep trailing empty Strings
            if (array.length < 13) {
                throw new IllegalArgumentException("Not enough elements (should be > 13) in line: "
                                                   + line);
            }

            // We only want to create GOAnnotation objects applied to genes and proteins
            // some file entries apply to type 'transcript' and possibly others
            if (!("gene".equalsIgnoreCase(array[11])
                    || "protein".equalsIgnoreCase(array[11]))) {
                LOG.info("Ignored line with type: " + array[11]);
                continue;
            }

            // parse the line into the different fields
            String productId = array[1];
            String goId = array[4];
            String qualifier = array[3];
            String strEvidence = array[6];
            Item evidence = null;
            if (strEvidence != null) {
                evidence = newGoEvidence(strEvidence);
            }
            String type = array[11];

            // create unique key for go annotation (gene + go term)
            GoTermToGene key = new GoTermToGene(productId, goId, qualifier);

            // "new goanno or not" test
            if (!holderMap.containsKey(key)) {

                // get the rest of the data
                Item newDatasource = newDatasource(array[14]);
                String newPublicationId = newPublication(array[5]);
                Item newGoTerm = newGoTerm(goId);
                ReferenceList newGoEvidenceColl = null;
                if (evidence != null) {
                    newGoEvidenceColl = new ReferenceList("goEvidenceCodes", new ArrayList());
                    newGoEvidenceColl.addRefId(evidence.getIdentifier());
                }
                Item newOrganism = newOrganism(array[12]);

                // new gene
                ItemWrapper newProductWrapper =
                    newProduct(productId, type, newOrganism,
                               newDatasource.getIdentifier(), true, null);

                // temporary object while we are rattling through the file
                // needed because we may have extra publications
                PlaceHolder newPlaceHolder = new PlaceHolder(
                        qualifier, newDatasource, newPublicationId, newGoEvidenceColl,
                        newProductWrapper, newGoTerm, array[7], newOrganism);

                holderMap.put(key, newPlaceHolder);
                LOG.debug("PROCESS - NEW KEY:" + key.toString());

            } else {

                // get temp object
                PlaceHolder holder = (PlaceHolder) holderMap.get(key);

                String extraPubItem = newPublication(array[5]);

                // add extra publication
                if (extraPubItem != null) {
                    holder.getExtraPublicationList().add(extraPubItem);
                // add reference to new evidence object
                } else if (evidence != null) {
                    ReferenceList goEvidenceColl = null;
                    goEvidenceColl = holder.getGoEvidenceColl();
                    if (goEvidenceColl == null) {
                        goEvidenceColl = new ReferenceList("goEvidenceCodes", new ArrayList());
                    }
                    goEvidenceColl.addRefId(evidence.getIdentifier());
                }
                LOG.debug("PROCESS - OLD KEY:" + key.toString());
            }
        }

        // Now create and store all the new items...
        // First put everything on a stack so we don't have to hold too many items in memory
        Stack holderStack = buildHolderStack();

        // Holds a map of all the goterms which act as keys for their respective set of parent
        // terms
        Map goTermId2ParentTermIdSetsMap = null;

        // do we have an obo based ontology file ?
        if (oboParser != null) {
            try {
                goTermId2ParentTermIdSetsMap = oboParser.getTermToParentTermSetMap();
            } catch (Exception e) {
                LOG.error("GoConverter - unable to get parent term map from the OboParser!", e);
            }
        }

        // process terms
        while (!holderStack.isEmpty()) {
            PlaceHolder nextPlaceHolder = (PlaceHolder) holderStack.pop();
            newGoAnnotation(nextPlaceHolder);
        }

        // process their parents
        holderStack = buildHolderStack();

        while (!holderStack.isEmpty()) {
            PlaceHolder nextPlaceHolder = (PlaceHolder) holderStack.pop();
            if (!nextPlaceHolder.getQualifier().equals("NOT")) {
                newParentGoAnnotation(goTermId2ParentTermIdSetsMap, nextPlaceHolder);
            }
        }

        // loop through again and store everything
        for (Iterator iter = goAnnoItems.keySet().iterator(); iter.hasNext();) {

            // gene&goterm combo
            GoTermToGene key = (GoTermToGene) iter.next();

            // go annotation object
            Item goAnnotation = (Item) goAnnoItems.get(key);

            // this is where all go annotation objects are stored
            doStore(goAnnotation, STORE_ONE);
        }

        LOG.debug("productWrapperMap.keyset size:" + productWrapperMap.keySet().size());
        LOG.debug("productWrapperMap.values size:" + productWrapperMap.values().size());

        // store genes
        for (Iterator pwmKeyIt = productWrapperMap.keySet().iterator(); pwmKeyIt.hasNext();) {
            ItemWrapper nextWrapper = (ItemWrapper) productWrapperMap.get(pwmKeyIt.next());
            LOG.debug("productWrapperMap storing item tied to key:" + nextWrapper.getKey());
            Item nextGeneProduct = nextWrapper.getItem();
            doStore(nextGeneProduct, STORE_THREE);
        }

        // Free up some memory
        productWrapperMap = null;
        holderMap = null;
        goAnnoItems = null;
    }

    private Stack buildHolderStack() {
        Stack holderStack = new Stack();

        for (Iterator holderIt = holderMap.values().iterator(); holderIt.hasNext();) {
            holderStack.push(holderIt.next());
        }
        return holderStack;
    }



    /**
     * iterate through the list of parents and process those.
     * @param placeHolder Object that holds all go annotation related information
     * @param goTermId2ParentTermIdSetsMap map of go terms to parent go terms
     * @throws ObjectStoreException if there is a problem
     */
    protected void newParentGoAnnotation(Map goTermId2ParentTermIdSetsMap, PlaceHolder placeHolder)
        throws ObjectStoreException {
        String goId = placeHolder.getGoTerm().getAttribute("identifier").getValue();
        if (goTermId2ParentTermIdSetsMap != null
            && goTermId2ParentTermIdSetsMap.containsKey(goId)) {
            Set parentTermIdsSet = (Set) goTermId2ParentTermIdSetsMap.get(goId);
            if (parentTermIdsSet != null) {
                processParents(placeHolder, parentTermIdsSet);
            }
        }
    }


    //------------------------- Produce a new GOAnnotation object -------------------------
    protected void newGoAnnotation(PlaceHolder placeHolder) throws ObjectStoreException {

        boolean isProductTypeGene = (placeHolder.getGeneProductWrapper().getItem().getClassName()
                        .indexOf("Gene") >= 0);

        // go term id
        String goId = placeHolder.getGoTerm().getAttribute("identifier").getValue();

        ReferenceList actualGoTerms =  new ReferenceList("actualGoTerms", new ArrayList());

        // create new go annotation object
        Item currentGoItem = newGoAnnotationItem(goId, "true", placeHolder.getGoEvidenceColl(),
            placeHolder.getGeneProductWrapper().getItem().getIdentifier(),
            placeHolder.getGoTerm().getIdentifier(),
            actualGoTerms);

        // add this term to actual go terms list
        actualGoTerms.addRefId(placeHolder.getGoTerm().getIdentifier());

        // If the qualifier is not a NOT.
        if (!"".equals(placeHolder.getQualifier())) {
            currentGoItem.addAttribute(new Attribute("qualifier", placeHolder.getQualifier()));
        }

        // if it has a name, add
        if (termIdNameMap.containsKey(goId)) {
            currentGoItem.setAttribute("name", (String) termIdNameMap.get(goId));
        }

        // with objects
        if (!"".equals(placeHolder.getWithText())) {
            currentGoItem.setAttribute("withText", placeHolder.getWithText());
            List with = createWithObjects(placeHolder.getWithText(),
                                          placeHolder.getOrganism(),
                                          placeHolder.getDatasource().getIdentifier());

            if (with.size() != 0) {
                List idList = new ArrayList();
                Iterator withIter = with.iterator();
                while (withIter.hasNext()) {
                    Item withObj = (Item) withIter.next();
                    idList.add(withObj.getIdentifier());
                }
                currentGoItem.addCollection(new ReferenceList("with", idList));
            }
        }

        // evidence collection (datasources and publications)
        ReferenceList references = new ReferenceList();
        references.setName("evidence");
        references.addRefId(placeHolder.getDatasource().getIdentifier());
        if (placeHolder.getPublicationId() != null) {
            references.addRefId(placeHolder.getPublicationId());
        }
        currentGoItem.addCollection(references);

        // add item to gene go collections
        if (isProductTypeGene) {
            placeHolder.getGeneProductWrapper().getItem().addToCollection("goAnnotation",
                                                                          currentGoItem);
            placeHolder.getGeneProductWrapper().getItem().addToCollection("allGoAnnotation",
                                                                          currentGoItem);
        } else {
            LOG.debug("Skipping setting go & allGo annotation collection for a:"
                      + placeHolder.getGeneProductWrapper().getItem().getClassName() 
                      + " with ident:"
                      + placeHolder.getGeneProductWrapper().getItem().getIdentifier());
        }

        // put in our list to store later
        String geneId = placeHolder.getGeneProductWrapper().getItem().getIdentifier();
        GoTermToGene key = new GoTermToGene(geneId, goId, placeHolder.getQualifier());
        if (!goAnnoItems.containsKey(key)) {
            goAnnoItems.put(key, currentGoItem);
        }
    }

    //-------------- PRODUCE THE SET OF PARENT TERM LINKING GOANNOTATION OBJECTS ---------------
    private void processParents(PlaceHolder placeHolder, Set parentTermIdsSet)
    throws ObjectStoreException {

        // loop over the set of Id's and create a GoAnnotation object that links the gene
        // geneProduct, the infered parent term and the current go term items.

        boolean isProductTypeGene = (placeHolder.getGeneProductWrapper().getItem()
                        .getClassName().indexOf("Gene") >= 0);

        for (Iterator parentIdIter = parentTermIdsSet.iterator(); parentIdIter.hasNext();) {

            // go term identifier
            String parentTermGoId = parentIdIter.next().toString();

            // go term object
            Item nextParentGoTermId = newGoTerm(parentTermGoId);

            LOG.debug("GoConverter - parents are being set for go a term:" + parentTermGoId);

            Item parentItem = null;
            String geneId = placeHolder.getGeneProductWrapper().getItem().getIdentifier();
            GoTermToGene key = new GoTermToGene(geneId, parentTermGoId, placeHolder.getQualifier());

            // gene&goterm combo already exists
            if (holderMap.containsKey(key)) {

                PlaceHolder parentPlaceHolder = (PlaceHolder) holderMap.get(key);
                parentItem = parentPlaceHolder.getGoAnno();
                // add this go term to the parent's collection of children
                parentItem.getCollection("actualGoTerms").addRefId(placeHolder.getGoTerm()
                                                                   .getIdentifier());
                if (!goAnnoItems.containsKey(key)) {
                    goAnnoItems.put(key, parentItem);
                }

            } else {
                ReferenceList actualGoTerms = null;
                if (goAnnoItems.containsKey(key)) {
                    parentItem = (Item) goAnnoItems.get(key);
                    actualGoTerms = parentItem.getCollection("actualGoTerms");
                } else {
                    // start a list of kids for this new parent
                    actualGoTerms =  new ReferenceList("actualGoTerms", new ArrayList());

                    // create annotation object
                    parentItem = newGoAnnotationItem(
                                 parentTermGoId, "false", placeHolder.getGoEvidenceColl(),
                                 placeHolder.getGeneProductWrapper().getItem().getIdentifier(),
                                 nextParentGoTermId.getIdentifier(),
                                 actualGoTerms);

                    goAnnoItems.put(key, parentItem);
                }
                actualGoTerms.addRefId(placeHolder.getGoTerm().getIdentifier());
            }

            // add name to parent
            if (termIdNameMap.containsKey(parentTermGoId)) {
                parentItem.setAttribute("name", (String) termIdNameMap.get(parentTermGoId));
            }

            // add parent to collection
            if (isProductTypeGene) {
                if (!placeHolder.getGeneProductWrapper().getItem().getCollection("allGoAnnotation")
                                .getRefIds().contains(parentItem.getIdentifier())) {
                    placeHolder.getGeneProductWrapper().getItem().addToCollection("allGoAnnotation",
                                                                                  parentItem);
                }
            } else {
                LOG.debug("Skipping adding a parent GoAnnotation ref to the "
                          + "allGoAnnotation collection of a non gene item.");
            }
        }

    }


    /**
     * Create a new go annotation item.  This is the actual item that will be stored.
     *
     * @param identifier          the identifier i.e. GO:0000001 etc etc
     * @param isPrimaryAssignment a boolean expressed as a string indicating that this is the
     *                            actual reference between gene and go term, and not just a inferred
     *                            parent relationship.
     * @param goEvidenceColl    collection of evidence terms related to this annotation
     * @param subject gene described by go term
     * @param property go term
     * @param actualGoTerms list of children goterms
     * @return the go annotation item
     */
    private Item newGoAnnotationItem(String identifier,
                                     String isPrimaryAssignment,
                                     ReferenceList goEvidenceColl,
                                     String subject,
                                     String property,
                                     ReferenceList actualGoTerms) {

        Item goAnnoItem = createItem("GOAnnotation");
        goAnnoItem.setAttribute("identifier", identifier);
        goAnnoItem.setAttribute("isPrimaryAssignment", isPrimaryAssignment);
        //goAnnoItem.setAttribute("evidenceCode", evidenceCode);
        if (goEvidenceColl != null) {
            goAnnoItem.addCollection(goEvidenceColl);
        }
        if (actualGoTerms != null) {
            goAnnoItem.addCollection(actualGoTerms);
        }
        goAnnoItem.setReference("subject", subject);
        goAnnoItem.setReference("property", property);
        return goAnnoItem;
    }


    private void addWithType(String prefix, String clsName, String fieldName) {
        withTypes.put(prefix, new WithType(clsName, fieldName));
    }


    /**
     * Given the 'with' text from a gene_association entry parse for recognised identifier
     * types and create Gene or Protein items accordingly.
     *
     * @param withText string from the gene_association entry
     * @param organism organism to reference
     * @param dataSourceId identifier of the datasource this item is from
     * @return a list of Items
     * @throws ObjectStoreException if there is a problem making a new product (gene/protein/etc)
     */
    protected List createWithObjects(String withText, Item organism,
                                     String dataSourceId) throws ObjectStoreException {

        List withProductList = new ArrayList();
        try {
            String[] elements = withText.split("[; |,]");
            for (int i = 0; i < elements.length; i++) {
                String entry = elements[i].trim();
                // rely on the format being type:identifier
                if (entry.indexOf(':') > 0) {
                    String prefix = entry.substring(0, entry.indexOf(':'));
                    String value = entry.substring(entry.indexOf(':') + 1);

                    if (withTypes.containsKey(prefix)) {
                        WithType wt = (WithType) withTypes.get(prefix);
                        ItemWrapper productWrapper = null;

                        // if a UniProt protein it may be from a differnet organism
                        // also FlyBase mey be from a different Drosophila species
                        if (prefix.equals("UniProt")) {
                            productWrapper = newProduct(value, wt.clsName,
                                                        organism, dataSourceId, false, null);
                        } else if (prefix.equals("FB")) {
                            productWrapper = newProduct(value, wt.clsName, organism,
                                                        dataSourceId, false, "organismDbId");
                        } else {
                            productWrapper = newProduct(value, wt.clsName,
                                                        organism, dataSourceId, true, null);
                        }
                        Item withProduct = productWrapper.getItem();
                        withProductList.add(withProduct);
                    } else {
                        LOG.debug("createWithObjects skipping a withType prefix:" + prefix);
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.error("createWithObjects broke with: " + withText);
            throw e;
        }
        return withProductList;
    }


    /**
     * Puts all the store calls in one spot to make debugging a bit easier...
     *
     * @param itemToStore self titled...
     * @param callSource  info string so I can work out where the method was called from...
     */
    private void doStore(Item itemToStore, String callSource) throws ObjectStoreException {

//        Attribute identAttr = null;
//        identAttr = itemToStore.getAttribute("identifier");
//
//
//        String i2sId = itemToStore.getIdentifier();
//
//        if (storedItemMap.containsKey(itemToStore.getIdentifier())) {
//
//            StringBuffer bigLogMsg = new StringBuffer();
//
//            bigLogMsg.append("doStore ");
//            bigLogMsg.append(callSource);
//            bigLogMsg.append(" has seen this item id before:");
//            bigLogMsg.append(i2sId);
//            bigLogMsg.append(" id_attr:");
//            bigLogMsg.append(identAttr != null ? identAttr.getValue() : NO_ID_ATTR);
//            bigLogMsg.append(" previous_id_attr:");
//            bigLogMsg.append(storedItemMap.get(i2sId) != null
//                    ? storedItemMap.get(i2sId) : "no_prev_attr");
//            bigLogMsg.append(" are attrs the same:");
//
//            if (identAttr != null) {
//                bigLogMsg.append(identAttr.getValue().equalsIgnoreCase(
//                        storedItemMap.get(i2sId).toString()) ? "true" : "false");
//            }
//
//            LOG.error(bigLogMsg.toString());
//        } else {
//
//            LOG.debug("doStore " + callSource
//                    + " called on a new item:" + itemToStore.getClassName()
//                    + " id:" + i2sId
//                    + " id_attr:" + (identAttr != null ? identAttr.getValue() : NO_ID_ATTR));
//
//            storedItemMap.put(i2sId,
//                    (identAttr != null ? identAttr.getValue() : NO_ID_ATTR));
//        }

        getItemWriter().store(ItemHelper.convert(itemToStore));
    }


    /**
     * Create a new geneProduct of a certain type (gene or protein) of a certain organism
     *
     * @param accession  the accession or actual identifier of the gene/protein (eg: FBgn0019981)
     * @param type       the type
     * @param organism the organism of the product, may be null if a protein
     * @param dataSourceId the id of the datasource the product is from.
     * @return the geneProduct
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected ItemWrapper newProduct(String accession,
                                     String type,
                                     Item organism,
                                     String dataSourceId,
                                     boolean createOrganism,
                                     String idField) throws ObjectStoreException {
        String clsName;

        // find gene attribute first to see if organism shoudld be part of key
        if ("gene".equalsIgnoreCase(type)) {
            clsName = "Gene";

            if (idField == null) {
                String taxonId = organism.getAttribute("taxonId").getValue();
                idField = (String) geneAttributes.get(taxonId);
                if (idField == null) {
                    throw new RuntimeException("Could not find a geneAttribute property for taxon: "
                                               + taxonId + " check properties file: " + PROP_FILE);
                }
            }
        } else if ("protein".equalsIgnoreCase(type)) {
            clsName = "Protein";
            idField = "primaryAccession";
        } else {
            throw new IllegalArgumentException("Unrecognised geneProduct type '" + type + "'");
        }

        boolean includeOrganism;
        if (idField.equals("organismDbId") || type.equals("protein")) {
            includeOrganism = false;
        } else {
            includeOrganism = createOrganism;
        }
        String key = makeProductKey(accession, type, organism, includeOrganism);

        //Have we already seen this product somewhere before?
        // if so, return the product rather than creating a new one...
        if (productWrapperMap != null && productWrapperMap.containsKey(key)) {
            return ((ItemWrapper) productWrapperMap.get(key));
        }

        Item product = createItem(clsName);
        if (organism != null && createOrganism) {
            product.setReference("organism", organism.getIdentifier());
        }
        product.setAttribute(idField, accession);

        //Record some evidence that says we got/matched the gene from GO data.
        product.addToCollection("evidence", newDatasource("Gene Ontology"));

        Item synonym = newSynonym(
                product.getIdentifier(),
                (String) synonymTypes.get(type),
                accession,
                dataSourceId);

        getItemWriter().store(ItemHelper.convert(synonym));

        ItemWrapper newProductWrapper = new ItemWrapper(key, product);
        productWrapperMap.put(key, newProductWrapper);

        return newProductWrapper;
    }

    /**
     * Makes a unique product key
     *
     * @param identifier the identifier
     * @param type       the type (allways lowercased)
     * @param organism the organism of th eproduct
     * @return A String combining all 3 that can be used as a unique hash key
     */
    private String makeProductKey(String identifier, String type, Item organism,
                                  boolean createOrganism) {

        if (type == null) {
            throw new IllegalArgumentException("No type provided when creating " + organism
                    + ": " + identifier);
        } else if (identifier == null) {
            throw new IllegalArgumentException("No identifier provided when creating "
                    + organism + ": " + type);
        }

        return identifier + type.toLowerCase() + ((createOrganism)
            ? organism.getIdentifier() : "");
    }


    /**
     * Create a new go term
     *
     * @param identifier the identifier
     * @return the go term
     */
    protected Item newGoTerm(String identifier) throws ObjectStoreException {
        Item item = (Item) goTerms.get(identifier);
        if (item == null) {
            item = createItem("GOTerm");
            item.addAttribute(new Attribute("identifier", identifier));
            goTerms.put(identifier, item);
            doStore(item, "newGoTerm");
        }
        return item;
    }


    /**
     * Create new evidence object
     *
     * @param code The code, e.g. ISS, IEA
     * @return the evidence code
     */
    protected Item newGoEvidence(String code) throws ObjectStoreException {
        Item item = (Item) goEvidence.get(code);
        if (item == null) {
            item = createItem("GOEvidenceCode");
            item.addAttribute(new Attribute("code", code));
            goEvidence.put(code, item);
            //doStore(item, "newGoEvidence");
            getItemWriter().store(ItemHelper.convert(item));
        }
        return item;
    }


    /**
     * Create a new DataSource item given a datasource code
     *
     * @param code the code
     * @return the datasource
     */
    protected Item newDatasource(String code) throws ObjectStoreException {

        String title = null;
        // re-write some codes to better data source names
        if ("UniProtKB".equals(code)) {
            title = "UniProt";
        } else if ("FB".equals(code)) {
            title = "FlyBase";
        } else if ("WB".equals(code)) {
            title = "WormBase";
        } else if ("SP".equals(code)) {
            title = "UniProt";
        } else if (code.startsWith("GeneDB")) {
            title = "GeneDB";
        } else if ("SANGER".equals(code)) {
            title = "GeneDB";
        } else if ("GOA".equals(code)) {
            title = "Gene Ontology";
        } else {
            title = code;
        }

        Item item = (Item) datasources.get(title);
        if (item == null) {
            item = createItem("DataSource");
            item.setAttribute("name", title);
            datasources.put(title, item);
            getItemWriter().store(ItemHelper.convert(item));
        }
        return item;
    }

    /**
     * Create a new publication given list of codes
     *
     * @param codes the codes
     * @return the publication
     */
    protected String newPublication(String codes) throws ObjectStoreException {
        String pubId = null;
        String[] array = codes.split("[|]");
        for (int i = 0; i < array.length; i++) {
            if (array[i].startsWith("PMID:")) {
                String code = array[i].substring(5);
                pubId = (String) publications.get(code);
                if (pubId == null) {
                    Item item = createItem("Publication");
                    item.addAttribute(new Attribute("pubMedId", code));
                    pubId = item.getIdentifier();
                    publications.put(code, pubId);
                    getItemWriter().store(ItemHelper.convert(item));
                }
                break;
            }
        }
        return pubId;
    }

    /**
     * Create a new organism given a taxonomy id
     *
     * @param taxonId the taxonId
     * @return the organism
     */
    protected Item newOrganism(String taxonId) throws ObjectStoreException {
        if (taxonId.equals("taxon:")) {
            throw new IllegalArgumentException("No taxon id supplied when creatin organism");
        }
        String taxonIdNew = taxonId.split(":")[1];
        Item item = (Item) organisms.get(taxonIdNew);
        if (item == null) {
            item = createItem("Organism");
            item.addAttribute(new Attribute("taxonId", taxonIdNew));
            organisms.put(taxonIdNew, item);
            getItemWriter().store(ItemHelper.convert(item));
        }
        return item;
    }

    private Item newSynonym(String subjectId, String type, String value, String dataSourceId) {

        LOG.debug("NEW SYNONYM .subject:" + subjectId + " .type:" + type
                + " .value:" + value + " .source:" + dataSourceId);

        Item synonym = createItem("Synonym");
        synonym.setReference("subject", subjectId);
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("source", dataSourceId);
        return synonym;
    }


    /**
     * Convenience method for creating a new Item
     *
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + (id++),
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
     * Class that saves information for making GoAnnotationItem objects until the
     * entire file is processed.
     * <p/>
     * Was introduced to avoid storing the same item more than once...
     */
    class PlaceHolder
    {

        private String qualifier;
        private Item datasource;
        private String publicationId;
        private ReferenceList goEvidenceColl;
        private ItemWrapper geneProduct;
        private Item goTerm;
        private String withText;
        private Item organism;
        private Item goAnno;

        private ArrayList extraPublicationList;

        private PlaceHolder() {
            extraPublicationList = new ArrayList();
        }

        /**
         * @param qualifier   qualifier (eg NOT) or null
         * @param datasource  the datasource
         * @param publicationId the publication id
         * @param goEvidenceColl  collection of evidence objects for this go term
         * @param product     the product - typically a protein or a gene item
         * @param goTerm      the goTerm
         * @param withText    the 'with' column of gene_associationfile
         * @param organism    the current organism as an Item
         */
        public PlaceHolder(
                String qualifier, Item datasource, String publicationId,
                ReferenceList goEvidenceColl, ItemWrapper product, Item goTerm,
                String withText, Item organism) {
            this();
            this.qualifier = qualifier;
            this.datasource = datasource;
            this.publicationId = publicationId;
            this.goEvidenceColl = goEvidenceColl;
            this.geneProduct = product;
            this.goTerm = goTerm;
            this.withText = withText;
            this.organism = organism;
        }

        /**
         * @return the qualifier as a String
         */
        public String getQualifier() {
            return qualifier;
        }

        /**
         * @return the datasource item that represents we are sourcing the go annotations from.
         */
        public Item getDatasource() {
            return datasource;
        }

        /**
         * @return the related Publication item
         */
        public String getPublicationId() {
            return publicationId;
        }

        /**
         * @return the go evidence String
         */
        public ReferenceList getGoEvidenceColl() {
            return goEvidenceColl;
        }

        /**
         * @return an ItemWrapper that allows us to store the gene in a map to ensure it is only
         *         stored once - and after all the GoAnnotation items have been related to it.
         */
        public ItemWrapper getGeneProductWrapper() {
            return geneProduct;
        }

        /**
         * @return the go term that the go annotation object is linking to
         */
        public Item getGoTerm() {
            return goTerm;
        }



        /**
         * @return some more details
         */
        public String getWithText() {
            return withText;
        }

        /**
         * @return the current organism as an Item
         */
        public Item getOrganism() {
            return organism;
        }

        /**
         * @return a list of any extra publication items that we want to link to this go annotation.
         */
        public ArrayList getExtraPublicationList() {
            return extraPublicationList;
        }

        public void setGoAnno(Item goAnno) {
            this.goAnno = goAnno;
        }

        public Item getGoAnno() {
            return goAnno;
        }

    }

    /**
     * Class to hold the current GoAnnotation Item and the set of its parent terms.
     */
//    class GoAnnotationItem
//    {
//
//        private Item goAnnotationItem;
//        private Set parentItems;
//
//        /**
//         * Constructor
//         *
//         * @param goAnnotationItem - current go term of interest
//         * @param parentItems      - set of current items parent terms
//         */
//        GoAnnotationItem(Item goAnnotationItem, Set parentItems) {
//            this.goAnnotationItem = goAnnotationItem;
//            this.parentItems = parentItems;
//        }
//
//        /**
//         * @return Get the current go term.
//         */
//        Item getGoAnnotationItem() {
//            return goAnnotationItem;
//        }
//
//        /**
//         * @return Get the parent go term set for the current go term.
//         */
//        Set getParentItems() {
//            return parentItems;
//        }
//
//        /**
//         * @return a string that is usefull for debugging...
//         */
//        public String toUsefulLogString() {
//
//            StringBuffer tsBuff = new StringBuffer();
//
//            Item item = goAnnotationItem;
//
//            tsBuff.append(" GOANNOITEM: ")
//                    .append(item.getClassName())
//                    .append(" ")
//                    .append(item.getAttribute("identifier").getValue())
//                    .append(" ")
//                    .append(item.getIdentifier() != null ? item.getIdentifier() : "no_id")
//                    .append("\n");
//
//            for (Iterator piit = parentItems.iterator(); piit.hasNext();) {
//                item = (Item) piit.next();
//                tsBuff.append(" NEXTPARENT: ")
//                        .append(item.getClassName())
//                        .append(" ")
//                        .append(item.getAttribute("identifier").getValue())
//                        .append(" ")
//                        .append(item.getIdentifier() != null ? item.getIdentifier() : "no_id")
//                        .append("\n");
//            }
//
//            return tsBuff.toString();
//        }
//    }

    /**
     * Class to identify an Item using a unique key
     */
    class ItemWrapper
    {
        String key;
        Item item;

        /**
         * Constructor
         *
         * @param key  the key
         * @param item the Item
         */
        ItemWrapper(String key, Item item) {
            this.key = key;
            this.item = item;
        }

        /**
         * @return the key string that we will use in a map to id the object
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the item in question
         */
        public Item getItem() {
            return item;
        }
    }

    /**
     * Identify a GoTerm/geneProduct pair with qualifier
     * used to also use evidence code
     */
    class GoTermToGene
    {
        private String productId;
        private String goId;
        //private String code;
        private String qualifier;

        /**
         * Constructor
         *
         * @param productId gene/protein identifier
         * @param goId      GO term id
         * @param qualifier qualifier
         */
        GoTermToGene(String productId, String goId, String qualifier) {
            this.productId = productId;
            this.goId = goId;
            //this.code = code;
            this.qualifier = qualifier;
        }

        /**
         * @see Object#equals(Object)
         */
        public boolean equals(Object o) {
            if (o instanceof GoTermToGene) {
                GoTermToGene go = (GoTermToGene) o;
//              && code.equals(go.code)
                return productId.equals(go.productId)
                        && goId.equals(go.goId)
                        && qualifier.equals(go.qualifier);
            }
            return false;
        }

        /**
         * @see Object#hashCode()
         */
        public int hashCode() {
//          + (7 * code.hashCode())

            return ((3 * productId.hashCode())
                    + (5 * goId.hashCode())
                    + (7 * qualifier.hashCode()));
        }

        /**
         * @see Object#toString()
         */
        public String toString() {

            StringBuffer toStringBuff = new StringBuffer();

            toStringBuff.append("GoTermToGene - productId:");
            toStringBuff.append(productId);
            toStringBuff.append(" goId:");
            toStringBuff.append(goId);
            //toStringBuff.append(" code:");
            //toStringBuff.append(code);
            //toStringBuff.append(" qualifier:");
            //toStringBuff.append(qualifier);

            return toStringBuff.toString();
        }
    }

    /**
     * Class to hold information about a BioEntity item to create for a particular
     * identifier prefix in the gene_association 'with' column'.
     */
    class WithType
    {
        String clsName;
        String fieldName;

        /**
         * Constructor
         *
         * @param clsName   the classname
         * @param fieldName name of field to set
         */
        WithType(String clsName, String fieldName) {
            this.clsName = clsName;
            this.fieldName = fieldName;
        }
    }
}
