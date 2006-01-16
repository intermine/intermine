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

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.OboParser;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;

import org.apache.tools.ant.BuildException;

import org.apache.log4j.Logger;

/**
 * DataConverter to parse a go annotation file into Items
 *
 * @author Andrew Varley
 * @author Peter Mclaren - some additions to record the parents of a go term.
 */
public class GoConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Map goTerms = new LinkedHashMap();
    protected Map datasources = new LinkedHashMap();
    protected Map publications = new LinkedHashMap();
    protected Map organisms = new LinkedHashMap();
    protected Map termIdNameMap = new LinkedHashMap();
    protected int id = 0;
    protected File ontology;
    protected Map withTypes = new LinkedHashMap();
    protected Map synonymTypes = new HashMap();
    protected Map productWrapperMap = new LinkedHashMap();;

    protected ItemFactory itemFactory;

    private OboParser oboParser = null;

    /*Some Debugging vars*/
    private Map storedItemMap = new LinkedHashMap();
    private static final String STORE_ONE = "store_1";
    private static final String STORE_TWO = "store_2";
    private static final String STORE_THREE = "store_3";
    private static final String NO_ID_ATTR = "NO_ID_ATTR";

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
    }

    /**
     * Set the GO ontology file to be read from DAG format.
     *
     * @param ontology the GO ontology file
     */
    public void setOntology(File ontology) {
        this.ontology = ontology;
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws ObjectStoreException, IOException {

        //Renew this at the start of processing each file
        productWrapperMap = new LinkedHashMap();

        Map holderMap = new LinkedHashMap();

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
        while ((line = br.readLine()) != null) {
            if (line.startsWith("!")) {
                continue;
            }
            String[] array = line.split("\t", -1); //keep trailing empty Strings

            // We only want to create GOAnnotation objects applied to genes and proteins
            // some file entries apply to type 'transcript' and possibly others
            if (!("gene".equalsIgnoreCase(array[11])
                    || "protein".equalsIgnoreCase(array[11]))) {
                LOG.info("Ignored line with type: " + array[11]);
                continue;
            }

            String qualifier = array[3];
            String goEvidence = array[6];
            String productId = array[1];
            String goId = array[4];
            String type = array[11];

            GoTermProduct key = new GoTermProduct(productId, goId, goEvidence, qualifier);

            if (!holderMap.containsKey(key)) {

                Item newDatasource = newDatasource(array[14]);
                Item newPublication = newPublication(array[5]);
                Item newGoTerm = newGoTerm(goId);
                Item newOrganism = newOrganism(array[12]);

                ItemWrapper newProductWrapper =
                        newProduct(productId, type, newOrganism.getIdentifier(),
                                newDatasource.getIdentifier(), null);

                String pwKey = newProductWrapper.getKey();

                if (productWrapperMap.containsKey(pwKey)) {
                    newProductWrapper = (ItemWrapper) productWrapperMap.get(pwKey);
                } else {
                    productWrapperMap.put(newProductWrapper.getKey(), newProductWrapper);
                }

                GoAnnoWithParentsPlaceHolder newPlaceHolder = new GoAnnoWithParentsPlaceHolder(
                        qualifier, newDatasource, newPublication, goEvidence, newProductWrapper,
                        newGoTerm, array[7], newOrganism);

                holderMap.put(key, newPlaceHolder);
                LOG.debug("PROCESS - NEW KEY:" + key.toString());

            } else {

                GoAnnoWithParentsPlaceHolder holder =
                        (GoAnnoWithParentsPlaceHolder) holderMap.get(key);

                Item extraPubItem = newPublication(array[5]);

                if (extraPubItem != null) {

                    holder.getExtraPublicationList().add(extraPubItem);
                }

                LOG.debug("PROCESS - OLD KEY:" + key.toString());
            }
        }

        //Now create and store all the new items...
        //First put everything on a stack so we don't have to hold too many items in memory
        Stack holderStack = new Stack();
        for (Iterator holderIt = holderMap.values().iterator(); holderIt.hasNext();) {

            holderStack.push(holderIt.next());
        }
        holderMap = null;

        while (!holderStack.isEmpty()) {

            GoAnnoWithParentsPlaceHolder nextPlaceHolder =
                    (GoAnnoWithParentsPlaceHolder) holderStack.pop();

            GoAnnotationItemWithParentSet annoWithParents =
                    newGoAnnotationWithParentTerms(nextPlaceHolder);

            LOG.debug("NEXT GAIWPS:" + annoWithParents.toUsefulLogString());

            //store the new anno item...
            doStore(annoWithParents.getGoAnnotationItem(), STORE_ONE);

            if (annoWithParents != null && annoWithParents.getParentItems() != null) {

                //Store all the parent anno items for the current anno item as well
                for (Iterator annoParentIt = annoWithParents.getParentItems().iterator();
                     annoParentIt.hasNext();) {

                    Item pNext = ((Item) annoParentIt.next());

                    doStore(pNext, STORE_TWO);
                }
            }
        }

        LOG.debug("productWrapperMap.keyset size:" + productWrapperMap.keySet().size());
        LOG.debug("productWrapperMap.values size:" + productWrapperMap.values().size());

        for (Iterator pwmKeyIt = productWrapperMap.keySet().iterator(); pwmKeyIt.hasNext();) {

            ItemWrapper nextWrapper = (ItemWrapper) productWrapperMap.get(pwmKeyIt.next());
            LOG.debug("productWrapperMap storing item tied to key:" + nextWrapper.getKey());
            Item nextGeneProduct = nextWrapper.getItem();
            doStore(nextGeneProduct, STORE_THREE);
        }

        //Free up some memory since we're donw with this global map.
        productWrapperMap = null;
    }

    /**
     * @see FileConverter#close
     */
    public void close() throws ObjectStoreException {
        store(goTerms.values());
        store(datasources.values());
        store(publications.values());
        store(organisms.values());
    }

    /**
     * Create a new annotation item linking a geneProduct with a term - with evidence code -
     * datasource and publication
     * <p/>
     * >------------------------- OLD PARAMS NOW IN THE placeHolder OBJECT -------------------------
     *
     * @param placeHolder Holds the previous params so we can store the GoAnnotation objects later.
     * @return a GoAnnotation item with its parent items in a Set.
     * @throws ObjectStoreException if problems...
     * >--------------------------------------------------------------------------------------------
     * @ param qualifier   qualifier (eg NOT) or null
     * @ param datasource    the datasource
     * @ param publication the publication
     * @ param goEvidence  the goEvidence
     * @ param geneProduct     the geneProduct - typically a protein or a gene item
     * @ param goTerm      the goTerm
     * @ param withText    String from the 'with' column of gene_associationfile
     */
    protected GoAnnotationItemWithParentSet newGoAnnotationWithParentTerms(
            GoAnnoWithParentsPlaceHolder placeHolder) throws ObjectStoreException {

        boolean isProductTypeGene = (
                placeHolder.getGeneProductWrapper().getItem().getClassName().indexOf("Gene")
                >= 0 ? true : false);

        LOG.debug("isProductType was set to :" + isProductTypeGene);

        //Holds a map of all the goterms which act as keys for their respective set of parent terms.
        Map goTermId2ParentTermIdSetsMap = null;

        //do we have an obo based ontology file ?
        if (oboParser != null) {

            try {
                goTermId2ParentTermIdSetsMap = oboParser.getTermToParentTermSetMap();
            } catch (Exception e) {
                LOG.error("GoConverter - unable to get parent term map from the OboParser!", e);
            }
        }

        //------------------------- Produce a new GOAnnotation object -------------------------
        String goId = placeHolder.getGoTerm().getAttribute("identifier").getValue();

        Item currentGoItem = newGoAnnotationItem(goId, "true", placeHolder.getGoEvidence(),
                placeHolder.getGoTerm().getIdentifier(),
                placeHolder.getGeneProductWrapper().getItem().getIdentifier(),
                placeHolder.getGoTerm().getIdentifier());

        //If the qualifier is not a NOT.
        if (!"".equals(placeHolder.getQualifier())) {
            currentGoItem.addAttribute(new Attribute("qualifier", placeHolder.getQualifier()));
        }

        if (termIdNameMap.containsKey(goId)) {
            currentGoItem.setAttribute("name", (String) termIdNameMap.get(goId));
        }

        if (!"".equals(placeHolder.getWithText())) {
            currentGoItem.setAttribute("withText", placeHolder.getWithText());

            List with = createWithObjects(placeHolder.getWithText(),
                    placeHolder.getOrganism().getIdentifier(),
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

        ReferenceList references = new ReferenceList();
        references.setName("evidence");
        references.addRefId(placeHolder.getDatasource().getIdentifier());
        if (placeHolder.getPublication() != null) {
            references.addRefId(placeHolder.getPublication().getIdentifier());
        }
        currentGoItem.addCollection(references);

        if (isProductTypeGene) {
            placeHolder.getGeneProductWrapper().getItem().addToCollection(
                    "goAnnotation", currentGoItem);
            placeHolder.getGeneProductWrapper().getItem().addToCollection(
                    "allGoAnnotation", currentGoItem);
        } else {
            LOG.debug("Skipping setting go & allGo annotation collection for a:"
                    + placeHolder.getGeneProductWrapper().getItem().getClassName() + " with ident:"
                    + placeHolder.getGeneProductWrapper().getItem().getIdentifier());
        }

        //-------------- PRODUCE THE SET OF PARENT TERM LINKING GOANNOTATION OBJECTS ---------------
        Set parentItems = new HashSet();

        //If the go term has some parent terms we will want to create a GoAnnotation object
        // for each of them as well.
        if (goTermId2ParentTermIdSetsMap != null
                && goTermId2ParentTermIdSetsMap.containsKey(goId)) {

            //loop over the set of Id's and create a GoAnnotation object that links the gene
            // geneProduct, the infered parent term and the current go term items.
            Set parentTermIdsSet = (Set) goTermId2ParentTermIdSetsMap.get(goId);

            if (parentTermIdsSet != null) {

                for (Iterator parentIdIter = parentTermIdsSet.iterator(); parentIdIter.hasNext();) {

                    LOG.debug("GoConverter - parents are being set for go a term:" + goId);

                    String nextParentTermGoId = parentIdIter.next().toString();
                    Item nextParentGoTermItem = null;

                    //have we already seen this parent term ? if not we need to create a parent term
                    if (goTerms.containsKey(nextParentTermGoId)) {
                        nextParentGoTermItem = (Item) goTerms.get(nextParentTermGoId);
                    } else {
                        nextParentGoTermItem = newGoTerm(nextParentTermGoId);
                    }

                    Item parentItem = newGoAnnotationItem(
                            nextParentTermGoId, "false", placeHolder.getGoEvidence(),
                            placeHolder.getGoTerm().getIdentifier(),
                            placeHolder.getGeneProductWrapper().getItem().getIdentifier(),
                            nextParentGoTermItem.getIdentifier());

                    if (termIdNameMap.containsKey(nextParentTermGoId)) {
                        parentItem.setAttribute(
                                "name",
                                (String) termIdNameMap.get(nextParentTermGoId));
                    }

                    parentItems.add(parentItem);

                    if (isProductTypeGene) {
                        placeHolder.getGeneProductWrapper().getItem().addToCollection(
                                "allGoAnnotation", parentItem);
                    } else {
                        LOG.debug("Skipping adding a parent GoAnnotation ref to the "
                                + "allGoAnnotation collection of a non gene item.");
                    }
                }
            } else {
                LOG.debug("GoConverter - skipping setting parents for go term:" + goId);
            }
        }

        return new GoAnnotationItemWithParentSet(currentGoItem, parentItems);
    }

    /**
     * Create a new go annotation item
     *
     * @param identifier          the identifier i.e. GO:0000001 etc etc
     * @param isPrimaryAssignment a boolean expressed as a string indicating that this is the
     *                            actual reference between
     * @return the go annotation item
     */
    private Item newGoAnnotationItem(String identifier,
                                     String isPrimaryAssignment,
                                     String evidenceCode,
                                     String actualGoTerm,
                                     String subject,
                                     String property) {

        Item goAnnoItem = createItem("GOAnnotation");
        goAnnoItem.setAttribute("identifier", identifier);
        goAnnoItem.setAttribute("isPrimaryAssignment", isPrimaryAssignment);
        goAnnoItem.setAttribute("evidenceCode", evidenceCode);
        goAnnoItem.setReference("actualGoTerm", actualGoTerm);
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
     * @param withText     string from the gene_association entry
     * @param organismId   identifier of organism to reference
     * @param dataSourceId identifier of the datasource this item is from
     * @return a list of Items
     * @throws ObjectStoreException if there is a problem making a new product (gene/protein/etc)
     */
    protected List createWithObjects(String withText, String organismId,
                                     String dataSourceId) throws ObjectStoreException {

        List withProductList = new ArrayList();
        try {
            String[] elements = withText.split("; |, ");
            for (int i = 0; i < elements.length; i++) {
                String entry = elements[i].trim();
                // rely on the format being type:identifier
                if (entry.indexOf(':') > 0) {
                    String prefix = entry.substring(0, entry.indexOf(':'));
                    String value = entry.substring(entry.indexOf(':') + 1);

                    if (withTypes.containsKey(prefix)) {
                        WithType wt = (WithType) withTypes.get(prefix);

                        String pWrapKey = makeProductKey(value, wt.clsName, organismId);
                        Item withProduct;

                        //Have we already seen this product somewhere before?
                        if (productWrapperMap != null
                                && productWrapperMap.containsKey(pWrapKey)) {

                            withProduct = ((ItemWrapper) productWrapperMap.get(pWrapKey)).getItem();
                            withProductList.add(withProduct);
                            //Ok here we have a new (so far) product - lets create it now...
                        } else {
                            ItemWrapper productWrapper =
                                    newProduct(value, wt.clsName, organismId,
                                            dataSourceId, pWrapKey);
                            withProduct = productWrapper.getItem();
                            withProductList.add(withProduct);
                            productWrapperMap.put(pWrapKey, productWrapper);
                        }
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
     * Puts all the store calls in one spot to make debuging a bit easier...
     *
     * @param itemToStore self titled...
     * @param callSource  info string so I can work out where the method was called from...
     */
    private void doStore(Item itemToStore, String callSource) throws ObjectStoreException {

        Attribute identAttr = itemToStore.getAttribute("identifier");
        String i2sId = itemToStore.getIdentifier();

        if (storedItemMap.containsKey(itemToStore.getIdentifier())) {

            StringBuffer bigLogMsg = new StringBuffer();

            bigLogMsg.append("doStore ");
            bigLogMsg.append(callSource);
            bigLogMsg.append(" has seen this item id before:");
            bigLogMsg.append(i2sId);
            bigLogMsg.append(" id_attr:");
            bigLogMsg.append(identAttr != null ? identAttr.getValue() : NO_ID_ATTR);
            bigLogMsg.append(" previous_id_attr:");
            bigLogMsg.append(storedItemMap.get(i2sId) != null
                    ? storedItemMap.get(i2sId) : "no_prev_attr");
            bigLogMsg.append(" are attrs the same:");

            if (identAttr != null) {
                bigLogMsg.append(identAttr.getValue().equalsIgnoreCase(
                        storedItemMap.get(i2sId).toString()) ? "true" : "false");
            }

            LOG.error(bigLogMsg.toString());
        } else {

            LOG.debug("doStore " + callSource
                    + " called on a new item:" + itemToStore.getClassName()
                    + " id:" + i2sId
                    + " id_attr:" + (identAttr != null ? identAttr.getValue() : NO_ID_ATTR));

            storedItemMap.put(i2sId,
                    (identAttr != null ? identAttr.getValue() : NO_ID_ATTR));
        }

        writer.store(ItemHelper.convert(itemToStore));
    }


    /**
     * Create a new geneProduct of a certain type (gene or protein) of a certain organism
     *
     * @param accession  the accession or actual identifier of the gene/protein (eg: FBgn0019981)
     * @param type       the type
     * @param organismId the organism identifier of the current organism item
     * @param key        optional param that allows us to provide the key if we have already got it.
     * @param dataSourceId the id of the datasource the product is from.
     * @return the geneProduct
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected ItemWrapper newProduct(String accession,
                                     String type,
                                     String organismId,
                                     String dataSourceId,
                                     String key) throws ObjectStoreException {

        if (key == null) {
            key = makeProductKey(accession, type, organismId);
        }

        String idField = null;
        String clsName = null;

        if ("gene".equalsIgnoreCase(type)) {
            clsName = "Gene";
            idField = "organismDbId";
        } else if ("protein".equalsIgnoreCase(type)) {
            clsName = "Protein";
            idField = "primaryAccession";
        } else {
            throw new IllegalArgumentException("Unrecognised geneProduct type '" + type + "'");
        }

        Item product = createItem(clsName);
        product.addReference(new Reference("organism", organismId));
        product.addAttribute(new Attribute(idField, accession));

        Item synonym = newSynonym(
                product.getIdentifier(),
                (String) synonymTypes.get(type),
                accession,
                dataSourceId);

        writer.store(ItemHelper.convert(synonym));

        return new ItemWrapper(key, product);
    }

    /**
     * Makes a unique product key
     *
     * @param identifier the identifier
     * @param type       the type (allways lowercased)
     * @param organismId the organism identifier of the current organism item
     * @return A String combining all 3 that can be used as a unique hash key
     */
    private String makeProductKey(String identifier, String type, String organismId) {

        if (organismId == null) {
            throw new IllegalArgumentException("No organism provided when creating "
                    + type + ": " + identifier);
        } else if (type == null) {
            throw new IllegalArgumentException("No type provided when creating " + organismId
                    + ": " + identifier);
        } else if (identifier == null) {
            throw new IllegalArgumentException("No organism provided when creating "
                    + organismId + ": " + type);
        }

        return identifier + type.toLowerCase() + organismId;
    }


    /**
     * Create a new go term
     *
     * @param identifier the identifier
     * @return the go term
     */
    protected Item newGoTerm(String identifier) {
        Item item = (Item) goTerms.get(identifier);
        if (item == null) {
            item = createItem("GOTerm");
            item.addAttribute(new Attribute("identifier", identifier));
            goTerms.put(identifier, item);
        }
        return item;
    }

    /**
     * Create a new DataSource item given a datasource code
     *
     * @param code the code
     * @return the datasource
     */
    protected Item newDatasource(String code) {
        Item item = (Item) datasources.get(code);
        if (item == null) {
            item = createItem("DataSource");
            String title = null;
            if ("UniProt".equals(code) || "UniProtKB".equals(code)) {
                title = "UniProt";
            } else if ("FB".equals(code)) {
                title = "FlyBase";
            } else if ("WB".equals(code)) {
                title = "WormBase";
            } else if ("SP".equals(code)) {
                // special case for Swiss-Prot
                title = "UniProt";
            } else if ("MGI".equals(code)) {
                title = "MGI";
            } else if ("SGD".equals(code)) {
                title = "SGD";
            } else if ("PINC".equals(code)) {
                title = "PINC";
            } else if ("HGNC".equals(code)) {
                title = "HGNC";
            } else if ("SWALL".equals(code)) {
                title = "SWALL";
            } else {
                throw new IllegalArgumentException("Database with code '" + code
                        + "' not recognised");
            }
            item.addAttribute(new Attribute("name", title));
            datasources.put(code, item);
        }
        return item;
    }

    /**
     * Create a new publication given list of codes
     *
     * @param codes the codes
     * @return the publication
     */
    protected Item newPublication(String codes) {
        Item item = null;
        String[] array = codes.split("[|]");
        for (int i = 0; i < array.length; i++) {
            if (array[i].startsWith("PMID:")) {
                String code = array[i].substring(5);
                item = (Item) publications.get(code);
                if (item == null) {
                    item = createItem("Publication");
                    item.addAttribute(new Attribute("pubMedId", code));
                    publications.put(code, item);
                }
                break;
            }
        }
        return item;
    }

    /**
     * Create a new organism given a taxonomy id
     *
     * @param taxonId the taxonId
     * @return the organism
     */
    protected Item newOrganism(String taxonId) {
        if (taxonId.equals("taxon:")) {
            throw new IllegalArgumentException("No taxon id supplied when creatin organism");
        }
        taxonId = taxonId.split(":")[1];
        Item item = (Item) organisms.get(taxonId);
        if (item == null) {
            item = createItem("Organism");
            item.addAttribute(new Attribute("taxonId", taxonId));
            organisms.put(taxonId, item);
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
        return itemFactory.makeItem(alias(className) + "_" + (id++), GENOMIC_NS + className, "");
    }

    /**
     * Class that saves information for making GoAnnotationItemWithParentSet objects until the
     * entire file is processed.
     * <p/>
     * Was introduced to avoid storing the same item more than once...
     */
    class GoAnnoWithParentsPlaceHolder
    {

        private String qualifier;
        private Item datasource;
        private Item publication;
        private String goEvidence;
        private ItemWrapper geneProduct;
        private Item goTerm;
        private String withText;
        private Item organism;

        private ArrayList extraPublicationList;

        private GoAnnoWithParentsPlaceHolder() {
            extraPublicationList = new ArrayList();
        }

        /**
         * @param qualifier   qualifier (eg NOT) or null
         * @param datasource  the datasource
         * @param publication the publication
         * @param goEvidence  the goEvidence
         * @param product     the product - typically a protein or a gene item
         * @param goTerm      the goTerm
         * @param withText    the 'with' column of gene_associationfile
         * @param organism    the current organism as an Item
         */
        public GoAnnoWithParentsPlaceHolder(
                String qualifier, Item datasource, Item publication, String goEvidence,
                ItemWrapper product, Item goTerm, String withText, Item organism) {
            this();
            this.qualifier = qualifier;
            this.datasource = datasource;
            this.publication = publication;
            this.goEvidence = goEvidence;
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
        public Item getPublication() {
            return publication;
        }

        /**
         * @return the go evidence String
         */
        public String getGoEvidence() {
            return goEvidence;
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
    }

    /**
     * Class to hold the current GoAnnotation Item and the set of its parent terms.
     */
    class GoAnnotationItemWithParentSet
    {

        private Item goAnnotationItem;
        private Set parentItems;

        /**
         * Constructor
         *
         * @param goAnnotationItem - current go term of interest
         * @param parentItems      - set of current items parent terms
         */
        GoAnnotationItemWithParentSet(Item goAnnotationItem, Set parentItems) {
            this.goAnnotationItem = goAnnotationItem;
            this.parentItems = parentItems;
        }

        /**
         * @return Get the current go term.
         */
        Item getGoAnnotationItem() {
            return goAnnotationItem;
        }

        /**
         * @return Get the parent go term set for the current go term.
         */
        Set getParentItems() {
            return parentItems;
        }

        /**
         * @return a string that is usefull for debugging...
         */
        public String toUsefulLogString() {

            StringBuffer tsBuff = new StringBuffer();

            Item item = goAnnotationItem;

            tsBuff.append(" GOANNOITEM: "
                    + item.getClassName() + " "
                    + item.getAttribute("identifier").getValue() + " "
                    + (item.getIdentifier() != null ? item.getIdentifier() : "no_id")
                    + "\n");

            for (Iterator piit = parentItems.iterator(); piit.hasNext();) {
                item = (Item) piit.next();
                tsBuff.append(" NEXTPARENT: "
                        + item.getClassName() + " "
                        + item.getAttribute("identifier").getValue() + " "
                        + (item.getIdentifier() != null ? item.getIdentifier() : "no_id")
                        + "\n");
            }

            return tsBuff.toString();
        }
    }

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
     * Identify a GoTerm/geneProduct pair with evidence code and qualifier
     */
    class GoTermProduct
    {
        String productId;
        String goId;
        String code;
        String qualifier;

        /**
         * Constructor
         *
         * @param productId gene/protein identifier
         * @param goId      GO term id
         * @param code      evidence code
         * @param qualifier qualifier
         */
        GoTermProduct(String productId, String goId, String code, String qualifier) {
            this.productId = productId;
            this.goId = goId;
            this.code = code;
            this.qualifier = qualifier;
        }

        /**
         * @see Object#equals
         */
        public boolean equals(Object o) {
            if (o instanceof GoTermProduct) {
                GoTermProduct go = (GoTermProduct) o;
                return productId.equals(go.productId)
                        && goId.equals(go.goId)
                        && code.equals(go.code)
                        && qualifier.equals(go.qualifier);
            }
            return false;
        }

        /**
         * @see Object#hashCode
         */
        public int hashCode() {
            return (3 * productId.hashCode())
                    + (5 * goId.hashCode())
                    + (7 * code.hashCode())
                    + (11 * qualifier.hashCode());
        }

        /**
         * @see Object#toString()
         */
        public String toString() {

            StringBuffer toStringBuff = new StringBuffer();

            toStringBuff.append("GoTermProduct - productId:");
            toStringBuff.append(productId);
            toStringBuff.append(" goId:");
            toStringBuff.append(goId);
            toStringBuff.append(" code:");
            toStringBuff.append(code);
            toStringBuff.append(" qualifier:");
            toStringBuff.append(qualifier);

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
