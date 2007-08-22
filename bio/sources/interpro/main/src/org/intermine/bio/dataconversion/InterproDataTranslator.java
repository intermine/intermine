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

import java.util.*;

import org.intermine.InterMineException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.*;
import org.intermine.metadata.Model;
import org.intermine.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * Translates the interpro src items database into the interpro tgt items database
 * prior to dataloading.
 *
 * @author Peter Mclaren
 */
public class InterproDataTranslator extends DataTranslator
{
    protected static final String PARENTFEATURES = "parentFeatures";
    protected static final String CHILDFEATURES = "childFeatures";
    protected static final String CONTAINS = "contains";
    protected static final String FOUNDIN = "foundIn";

    protected static final String PROTEINS = "proteins";
    protected static final String PROTEINFEATURES = "proteinFeatures";

    protected static final String CV_DATABASE = "cv_database";
    protected static final String CV_ENTRY_TYPE = "cv_entry_type";
    protected static final String CV_RELATION = "cv_relation";
    protected static final String CV_EVIDENCE = "cv_evidence";
    protected static final String ABBREV = "abbrev";
    protected static final String IDENTIFIER = "identifier";
    protected static final String COMMENTS = "comments";
    protected static final String PROTEIN = "protein";
    protected static final String METHOD = "method";
    protected static final String ENTRY = "entry";
    protected static final String MATCHES = "matches";
    protected static final String ENTRY2ENTRY = "entry2entry";
    protected static final String ENTRY2COMP = "entry2comp";
    protected static final String COMMON_ANNOTATION = "common_annotation";

    protected static final String FAMILY = "Family";
    protected static final String DOMAIN = "Domain";

    protected static final String PARENT = "parent";
    protected static final String ENTRY1 = "entry1";
    protected static final String ENTRY2 = "entry2";
    protected static final String RELATIONSHIP = "relationship";

    protected static final String PROTEIN_AC = "protein_ac";
    protected static final String NAME = "name";
    protected static final String ENTRY_AC = "entry_ac";
    protected static final String METHOD_AC = "method_ac";
    protected static final String EVIDENCE = "evidence";
    protected static final String SOURCE = "source";
    protected static final String SYNONYMS = "synonyms";

    protected static final String PRIMARYACCESSION = "primaryAccession";




    protected static final Logger LOG = Logger.getLogger(InterproDataTranslator.class);

    //little evidence tag item...
    private org.intermine.xml.full.Item interproDataSet = null;

    private HashMap dbNameToDbSourceItemMap;
    private HashMap dataSourceToDbSetItemMap;

    private Item interproDataSource = null;
    private org.intermine.xml.full.Reference interproDataSourceReference = null;
    private String org180454Identifier;

    /**
     * Typical constructor
     *
     * @param itemReader  - the item reader
     * @param properties  - some properties
     * @param sourceModel - the source model
     * @param targetModel - the target model
     */
    public InterproDataTranslator(ItemReader itemReader, Properties properties,
                                  Model sourceModel, Model targetModel) {
        super(itemReader, properties, sourceModel, targetModel);
        interproDataSource = createItem("DataSource");
        interproDataSource.addAttribute(new Attribute("name", "InterPro"));
        interproDataSourceReference =
                new org.intermine.xml.full.Reference(SOURCE, interproDataSource.getIdentifier());

        interproDataSet = createItem("DataSet");
        interproDataSet.setAttribute("title", "InterPro data set");
        interproDataSet.setReference("dataSource", interproDataSource);

        dbNameToDbSourceItemMap = new HashMap();
        dbNameToDbSourceItemMap.put("InterPro",
                new DataSourceAndSetUsageCounter(interproDataSource, interproDataSet));

        dataSourceToDbSetItemMap = new HashMap();
        dataSourceToDbSetItemMap.put(interproDataSource, interproDataSet);
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
    throws ObjectStoreException, InterMineException {
        org180454Identifier = itemFactory.makeItem().getIdentifier();

        super.translate(tgtItemWriter);

        for (Iterator dsIt = dbNameToDbSourceItemMap.values().iterator(); dsIt.hasNext();) {

            DataSourceAndSetUsageCounter counter = (DataSourceAndSetUsageCounter) dsIt.next();

            Item nextDataSrc = counter.getDataSource();

            if (counter.getSourceUseageCount() > 1 || nextDataSrc == interproDataSource) {
                LOG.debug("STORE USED DATASOURCE - ID:" + nextDataSrc.getIdentifier()
                        + " TIMES_USED:" + (counter.getSourceUseageCount() - 1));

                Attribute ndsName = nextDataSrc.getAttribute("name");

                //Convert the SWISS-PROT data source name into UNIPROT - for dataset.xml reasons.
                if (ndsName != null && "SWISS-PROT".equalsIgnoreCase(ndsName.getValue())) {

                    nextDataSrc.removeAttribute("name");
                    Attribute uniprotAttr = new Attribute("name", "UniProt");
                    nextDataSrc.addAttribute(uniprotAttr);
                    LOG.debug("CONVERTED SWISS-PROT TO UniProt");
                } else {
                    LOG.debug("SKIPPING NON SWISS-PROT DATASOURCE:"
                            + (ndsName != null ? ndsName.getValue() : "NO_NAME_ATTR!"));
                }

                tgtItemWriter.store(ItemHelper.convert(nextDataSrc));

                Item nextDataSet = counter.getDataSet();
                LOG.debug("STORING A DATASET:" + nextDataSet.getIdentifier());
                tgtItemWriter.store(ItemHelper.convert(nextDataSet));
            } else {
                Attribute ndsName = nextDataSrc.getAttribute("name");
                LOG.debug("SKIPPED UNUSED DATASOURCE:"
                        + (ndsName != null ? ndsName.getValue() : "NO_NAME_ATTR!"));
            }
        }
    }

    /**
     * @see DataTranslator#translateItem()
     * {@inheritDoc}
     */
    @Override
    protected Collection<Item> translateItem(Item srcItem)
            throws ObjectStoreException, InterMineException {

        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = null;
        try {
        cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        Collection<Item> result = new HashSet();
        String srcItemClassName = XmlUtil.getFragmentFromURI(srcItem.getClassName());


        if (srcItemClassName.equalsIgnoreCase("cv_database")
                && "InterPro".equalsIgnoreCase(srcItem.getAttribute("dbname").getValue())) {

            //skip this one, since we deal with it manually...
            LOG.debug("SKIPPING STORE FOR THE CV_DATABASE.InterPro ITEM!");

            //But we need to set the version field in the interpro dataset
            setVersionInDataSetItem(srcItem, interproDataSet, "InterPro");
        } else {
            Collection<Item> translated = super.translateItem(srcItem);
            if (srcItemClassName.equals("taxonomy")) {
                result = translated;
                if (translated.size() != 1) {
                    throw new RuntimeException("didn't get one target item when translating an "
                                               + "organism");
                } else {
                    Item organism = translated.iterator().next();
                    if (organism.getAttribute("taxonId").getValue().equals("7165")) {
                        result = Collections.EMPTY_SET;
                    }
                    if (organism.getAttribute("taxonId").getValue().equals("180454")) {
                        organism.setIdentifier(org180454Identifier);
                    }
                }
            } else {
                if (translated != null) {

                    for (Iterator i = translated.iterator(); i.hasNext();) {
                        Item tgtItem = (Item) i.next();

                        if (PROTEIN.equals(srcItemClassName)) {
                            result.addAll(processProteinItem(srcItem, tgtItem));
                        } else if (METHOD.equals(srcItemClassName)) {
                            result.addAll(processMethodItem(srcItem, tgtItem));
                        } else if (ENTRY.equals(srcItemClassName)) {
                            result.addAll(processEntryItem(srcItem, tgtItem));
                        } else if (MATCHES.equals(srcItemClassName)) {
                            processMatchesItem(srcItem, tgtItem);
                        } else if (COMMON_ANNOTATION.equals(srcItemClassName)) {
                            processCommonAnnotationItem(tgtItem);
                        }

                        if (tgtItem != null) {
                            result.add(tgtItem);
                        }
                    }
                } else {

                    if (CV_DATABASE.equals(srcItemClassName)) {
                        processCVDatabaseItem(srcItem);
                    } else {
                        LOG.debug("SKIPPING AN UNTRANSLATED CLASS:" + srcItemClassName);
                    }
                }
            }
        }
        return result;


    } catch (Exception e2) {
        throw new RuntimeException(e2);
    } finally {
        Thread.currentThread().setContextClassLoader(cl);
    }

    }

    private Set processProteinItem(
            org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
            throws ObjectStoreException {

        org.intermine.xml.full.Item taxItem
                = getItemViaItemPath(srcItem, TAXONOMY_FROM_PROTEIN, srcItemReader);

        if (taxItem != null) {
            String taxonId = taxItem.getAttribute("taxa_id").getValue();
            if ("180454".equals(taxonId) || "7165".equals(taxonId)) {
                tgtItem.setReference("organism", org180454Identifier);
            } else {
                tgtItem.setReference("organism", taxItem.getIdentifier());
            }
            LOG.debug("PROTEIN.PROTEIN_AC:"
                    + srcItem.getAttribute("protein_ac").getValue()
                    + " has tax_id:" + taxItem.getAttribute("taxa_id").getValue());
        } else {
            LOG.warn("!!! PROTEIN.PROTEIN_AC:"
                    + srcItem.getAttribute("protein_ac").getValue()
                    + " has does NOT have a taxonomy object ref !!!");
        }

        java.util.List matchesList =
                getItemsViaItemPath(srcItem, PROTEIN_FROM_MATCHES, srcItemReader);

        for (Iterator matchIt = matchesList.iterator(); matchIt.hasNext();) {

            org.intermine.xml.full.Item nextMatch = (org.intermine.xml.full.Item) matchIt.next();
            org.intermine.xml.full.Reference methodRef = nextMatch.getReference(METHOD);
            org.intermine.xml.full.Item nextMethod =
                    ItemHelper.convert(this.srcItemReader.getItemById(methodRef.getRefId()));

            addToCollection(tgtItem, PROTEINFEATURES, nextMethod);
        }

        java.util.List supermatchList =
                getItemsViaItemPath(srcItem, PROTEIN_FROM_SUPER_MATCH, srcItemReader);

        for (Iterator supermatchIt = supermatchList.iterator(); supermatchIt.hasNext();) {

            org.intermine.xml.full.Item nextSupermatch =
                    (org.intermine.xml.full.Item) supermatchIt.next();

            org.intermine.xml.full.Reference entryRef = nextSupermatch.getReference(ENTRY);

            org.intermine.xml.full.Item nextEntry =
                    ItemHelper.convert(this.srcItemReader.getItemById(entryRef.getRefId()));

            addToCollection(tgtItem, PROTEINFEATURES, nextEntry);
        }

        Item[] sourceAndSet = setupCvDbDataSrcEvidenceRef(srcItem, tgtItem);
        Item extraDatabaseSynonym =
                createExtraDataSrcSynonym(sourceAndSet[0], tgtItem, PRIMARYACCESSION);

        addReferencedItem(tgtItem, extraDatabaseSynonym, SYNONYMS, true, null, false);

        HashSet items = new HashSet();

        items.add(extraDatabaseSynonym);

        return items;
    }

    private Set processMethodItem(
            org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
            throws ObjectStoreException {

        //NOTE: This extended path only work as the bridge table has a 1 to 1 row count
        // with the method table.
        //HERE WE WANT TO ESTABLISH A LINK BETWEEN THE METHOD AND ITS RELATED INTERPRO_ID FROM THE
        // ENTRY TABLE
        org.intermine.xml.full.Item entryItem =
                getItemViaItemPath(srcItem, METHOD_TO_ENTRY_VIA_ENTRY_TO_METHOD, srcItemReader);

        if (entryItem != null) {

            //Get the interpro id for the related entry...
            org.intermine.xml.full.Attribute interproIdAttribute = new Attribute();
            interproIdAttribute.setName("interproId");
            interproIdAttribute.setValue(entryItem.getAttribute("entry_ac").getValue());
            tgtItem.addAttribute(interproIdAttribute);

            //CHECK THE ENTRY RELATING TO THIS METHOD TO SEE IF IT IS A FAMILY OR AN EQIV DOMAIN...
            tryAndSetEntryTypeInAnEntry(entryItem);

            if (entryItem.getAttribute(ABBREV).getValue().equalsIgnoreCase(FAMILY)) {

                LOG.debug("SETTING METHOD AS PROTEINFAMILY ID:" + interproIdAttribute.getValue());
                tgtItem.setClassName("http://www.flymine.org/model/genomic#ProteinFamily");
            } else if (entryItem.getAttribute(ABBREV).getValue().equalsIgnoreCase(DOMAIN)) {

                LOG.debug("SETTING METHOD AS PROTEINDOMAIN ID:" + interproIdAttribute.getValue());
                tgtItem.setClassName("http://www.flymine.org/model/genomic#ProteinDomain");
            } else {
                LOG.debug("IGNORED AN METHOD-ENTRY MAPPING WITH TYPE:"
                        + entryItem.getAttribute(ABBREV).getValue());
            }

        } else {
            LOG.warn("METHOD WITH NO MAPPED ENTRY ITEM FOUND - "
                    + "CHECK THE METHOD & ENTRY2METHOD & ENTRY TABLES!");
        }

        //This extended path only work as the bridge table has a 1 to 1 row count with the
        // method table.
        //Here we want to fetch the evidence field that describes the relationship between the
        // method and entry items
        org.intermine.xml.full.Item cvEvidenceItem =
              getItemViaItemPath(srcItem, METHOD_TO_CV_EVIDENCE_VIA_ENTRY_TO_METHOD, srcItemReader);

        if (cvEvidenceItem != null) {

            addToCollection(tgtItem, EVIDENCE, cvEvidenceItem);

        //CHECK THE ENTRY RELATING TO THIS METHOD TO SEE IF IT IS A FAMILY OR AN EQUIVALENT DOMAIN.
        } else {
            LOG.warn("METHOD WITH NO MAPPED CV_EVIDENCE ITEM FOUND - "
                    + "CHECK THE METHOD & ENTRY2METHOD & CV_EVIDENCE TABLES!");
        }

        //Fill in the proteins collection which we can reach via the matches table...
        java.util.List matchesList
                = getItemsViaItemPath(srcItem, METHOD_FROM_MATCHES, srcItemReader);

        for (Iterator matchIt = matchesList.iterator(); matchIt.hasNext();) {

            org.intermine.xml.full.Item nextMatch = (org.intermine.xml.full.Item) matchIt.next();
            org.intermine.xml.full.Reference proteinRef = nextMatch.getReference(PROTEIN);
            if (proteinRef != null) {
            org.intermine.xml.full.Item nextProtein =
                    ItemHelper.convert(this.srcItemReader.getItemById(proteinRef.getRefId()));

                if (nextProtein != null) {

                    if (nextProtein.hasAttribute(PROTEIN_AC)) {

                        addToCollection(tgtItem, PROTEINS, nextProtein);
                    } else {
                        LOG.warn("METHOD-MATCHES-PROTEIN - PROTEIN_AC NOT FOUND!");
                    }
                } else {
                    LOG.warn("METHOD-MATCHES-PROTEIN - NULL PROTEIN FROM REFERENCE!");
                }
            } else {
                LOG.warn("METHOD WITH NO RELATED MATCHes FOUND!" + srcItem.getIdentifier());
            }
        }
        {
            //Make sure that the target item has it's IDENTIFIER and NAME fields set...
            org.intermine.xml.full.Attribute methodAcAttribute = srcItem.getAttribute(METHOD_AC);
            org.intermine.xml.full.Attribute nuIdentifierAttribute =
                    new org.intermine.xml.full.Attribute();
            nuIdentifierAttribute.setName(IDENTIFIER);
            nuIdentifierAttribute.setValue(methodAcAttribute.getValue());
            tgtItem.addAttribute(nuIdentifierAttribute);
        }
        {
            //Make sure that the target item has it's IDENTIFIER and NAME fields set...
            org.intermine.xml.full.Attribute nameAttribute = srcItem.getAttribute(NAME);
            org.intermine.xml.full.Attribute nuNameAttribute
                    = new org.intermine.xml.full.Attribute();
            nuNameAttribute.setName(NAME);
            nuNameAttribute.setValue(nameAttribute.getValue());
            tgtItem.addAttribute(nuNameAttribute);
        }

        HashSet items = new HashSet();

        if (tgtItem.hasAttribute("interproId")) {

            Item interproSynonym = createSynonym(tgtItem.getIdentifier(), IDENTIFIER,
                    tgtItem.getAttribute("interproId").getValue(), interproDataSourceReference);

            addReferencedItem(tgtItem, interproSynonym, SYNONYMS, true, null, false);
            addToCollection(tgtItem, EVIDENCE, interproDataSet);
            items.add(interproSynonym);
        }

        Item[] sourceAndSet = setupCvDbDataSrcEvidenceRef(srcItem, tgtItem);

        Item extraDatabaseSynonym = createExtraDataSrcSynonym(sourceAndSet[0], tgtItem, null);

        addReferencedItem(tgtItem, extraDatabaseSynonym, SYNONYMS, true, null, false);

        items.add(extraDatabaseSynonym);

        return items;
    }

    private Set processEntryItem(
            org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
            throws ObjectStoreException {

        //Make sure that the entry object has a type !!!
        tryAndSetEntryTypeInAnEntry(srcItem);

        addToCollection(tgtItem, EVIDENCE, interproDataSet);

        //LINK ACCROSS THE SUPERMATCH BRIDGE TABLE TO SET THE RELATED
        // PROTEINS FOR THIS ENTRY/PROTEIN-DOMAIN/FAMILY
        java.util.List supermatchList =
                getItemsViaItemPath(srcItem, ENTRY_FROM_SUPER_MATCH, srcItemReader);

        for (Iterator supermatchIt = supermatchList.iterator(); supermatchIt.hasNext();) {

            org.intermine.xml.full.Item nextSuperMatch =
                    (org.intermine.xml.full.Item) supermatchIt.next();
            org.intermine.xml.full.Reference proteinRef = nextSuperMatch.getReference(PROTEIN);

            if (proteinRef != null) {

                org.intermine.xml.full.Item nextProtein =
                        ItemHelper.convert(this.srcItemReader.getItemById(proteinRef.getRefId()));

                if (nextProtein != null) {

                    if (nextProtein.hasAttribute(PROTEIN_AC)) {

                        addToCollection(tgtItem, PROTEINS, nextProtein);
                    } else {
                        LOG.warn("ENTRY-SUPERMATCH-PROTEIN - PROTEIN_AC NOT FOUND!");
                    }
                } else {
                    LOG.warn("ENTRY-SUPERMATCH-PROTEIN - NULL PROTEIN FROM REFERENCE!");
                }
            } else {
                LOG.warn("ENTRY-SUPERMATCH-PROTEIN - NO REFERENCE TO ENTRY FOUND!");
            }
        }
        //SET THE INTERPRO ACCESSION (ENTRY_AC) TO BE THE IDENTIFIER IN THE TGT ITEM
        if (tgtItem.hasAttribute(IDENTIFIER)) {
            LOG.debug("ENTRY HAS AN IDENTIFIER:" + tgtItem.getAttribute(IDENTIFIER).getValue());
        } else {

            org.intermine.xml.full.Attribute srcEntryAcAttribute = srcItem.getAttribute(ENTRY_AC);

            if (srcEntryAcAttribute != null) {
                org.intermine.xml.full.Attribute tgtEntryAcAttribute = new Attribute();
                tgtEntryAcAttribute.setName(IDENTIFIER);
                tgtEntryAcAttribute.setValue(srcEntryAcAttribute.getValue());
                tgtItem.addAttribute(tgtEntryAcAttribute);
            } else { //unlikely - but you never know...
                LOG.warn("!!! ENTRY (identifier:" + (tgtItem.getAttribute(IDENTIFIER) != null
                        ? tgtItem.getAttribute(IDENTIFIER).getValue() : "_NO_ID_FOUND_")
                        + ") IS WITHOUT AN ENTRY_AC !!!");
            }
        }

        //SET THE COMMENTS COLLECTION FROM THE COMMON_ANNOTATION TABLE.
        if (tgtItem.hasCollection(COMMENTS)) {
            LOG.debug("ENTRY WITH A COMMENTS LIST FOUND - SKIPPING COMMENT GENERATION");
        } else {

            java.util.List entryToCommmonAnnotationList =
                    getItemsViaItemPath(
                            srcItem, ENTRY_FROM_ENTRY_TO_COMMON_ANNOTATION, srcItemReader);

            for (Iterator e2caIt = entryToCommmonAnnotationList.iterator(); e2caIt.hasNext();) {
                org.intermine.xml.full.Item nextE2CAItem
                        = (org.intermine.xml.full.Item) e2caIt.next();
                org.intermine.xml.full.Reference caRef
                        = nextE2CAItem.getReference(COMMON_ANNOTATION);

                if (caRef != null) {
                    org.intermine.xml.full.Item caItem =
                            ItemHelper.convert(this.srcItemReader.getItemById(caRef.getRefId()));

                    addToCollection(tgtItem, COMMENTS, caItem);
                } else {
                    LOG.debug("ENTRY WITH NO COMMON_ANNOTATION OBJECT FOUND! "
                            + nextE2CAItem.getIdentifier());
                }
            }
        }

        //Link to the entry2entry & entry2comp tables to set PARENT/CHILD or EQUIVALENT relations.
        setupEntryRelations(ENTRY_FROM_ENTRY_TO_ENTRY_VIA_PARENT, srcItem, tgtItem,
                ENTRY, CHILDFEATURES);
        setupEntryRelations(
                ENTRY_FROM_ENTRY_TO_ENTRY_VIA_ENTRY, srcItem, tgtItem, PARENT, PARENTFEATURES);

        setupEntryRelations(
                ENTRY_FROM_ENTRY_TO_COMP_VIA_ENTRY_ONE, srcItem, tgtItem, ENTRY2, CONTAINS);
        setupEntryRelations(
                ENTRY_FROM_ENTRY_TO_COMP_VIA_ENTRY_TWO, srcItem, tgtItem, ENTRY1, FOUNDIN);


        Item interproSynonym = createSynonym(tgtItem.getIdentifier(), IDENTIFIER,
                tgtItem.getAttribute(IDENTIFIER).getValue(), interproDataSourceReference);

        addReferencedItem(tgtItem, interproSynonym, SYNONYMS, true, null, false);

        addToCollection(tgtItem, EVIDENCE, interproDataSet);

        HashSet items = new HashSet();
        items.add(interproSynonym);

        return items;
    }

    private void processMatchesItem(
            org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
            throws ObjectStoreException {

        //CHECK THAT THERE IS A REFERENCE TO THE CV_DATABASE ITEM FOR THIS MATCHES ITEM -
        // WARN IF NONE FOUND!
        org.intermine.xml.full.Reference cvdbRefSrc = srcItem.getReference(CV_DATABASE);

        Item dataSetItem;

        if (cvdbRefSrc != null) {

            org.intermine.xml.full.Item cvdbItem =
                    ItemHelper.convert(this.srcItemReader.getItemById(cvdbRefSrc.getRefId()));

            String sourceDbName = cvdbItem.getAttribute("dbname").getValue();

            Item dataSourceItem =
                    procureDataSourceAndSetItem(sourceDbName);

            dataSetItem = procureDataSetItem(dataSourceItem);

            addToCollection(tgtItem, EVIDENCE, dataSetItem);
        } else {
            LOG.warn("!!! FOUND A MATCHES ITEM WITHOUT A REFERENCED CV_DATABASE ITEM !!!");
        }

        //CHECK THAT THERE IS A REFERENCE TO THE CV_EVIDENCE ITEM FOR THIS MATCHES ITEM -
        // WARN IF NONE FOUND!
        org.intermine.xml.full.Reference cvevRefSrc = srcItem.getReference(CV_EVIDENCE);
        if (cvevRefSrc != null) {

            org.intermine.xml.full.Item cvevItem =
                    ItemHelper.convert(this.srcItemReader.getItemById(cvevRefSrc.getRefId()));

            addToCollection(tgtItem, EVIDENCE, cvevItem);
        } else {
            LOG.warn("!!! FOUND A MATCHES ITEM WITHOUT A REFERENCED CV_EVIDENCE ITEM !!!");
        }
    }

    private void processCommonAnnotationItem(org.intermine.xml.full.Item tgtItem) {

        tgtItem.setReference(SOURCE, interproDataSet.getIdentifier());
    }

    private void processCVDatabaseItem(org.intermine.xml.full.Item srcItem)
            throws ObjectStoreException {

        String dbName = srcItem.getAttribute("dbname").getValue();

        LOG.debug("Processing a CV_DATABASE ITEM:" + dbName);

        if (dbNameToDbSourceItemMap.containsKey(dbName)) {
            LOG.debug("Skipping creating DataSource for a database we have already seen:" + dbName);
        } else {
            procureDataSourceAndSetItem(dbName);
        }

        //Remember to set the version field in the related dataset object.
        Item dataSetItem = procureDataSetItem(procureDataSourceAndSetItem(dbName));
        setVersionInDataSetItem(srcItem, dataSetItem, dbName);
    }

    private Item procureDataSourceAndSetItem(String dbName) {

        Item dataSource;

        if (!dbNameToDbSourceItemMap.containsKey(dbName)) {

            Item nuDataSrc = createItem("DataSource");
            nuDataSrc.addAttribute(new Attribute("name", dbName));
            DataSourceAndSetUsageCounter counter =
                    new DataSourceAndSetUsageCounter(nuDataSrc, procureDataSetItem(nuDataSrc));
            dbNameToDbSourceItemMap.put(dbName, counter);
            dataSource = counter.getDataSource();
        } else {

            DataSourceAndSetUsageCounter useCntr =
                (DataSourceAndSetUsageCounter) dbNameToDbSourceItemMap.get(dbName);

            dataSource = useCntr.getDataSource();
        }

        return dataSource;
    }

    private Item procureDataSetItem(Item parentDataSourceItem) {

        Item dataSet;

        if (!dataSourceToDbSetItemMap.containsKey(parentDataSourceItem)) {

            String dsTitle = parentDataSourceItem.getAttribute("name").getValue() + " data set";

            dataSet = createItem("DataSet");
            dataSet.setAttribute("title", dsTitle);
            dataSet.setReference("dataSource", parentDataSourceItem);

            dataSourceToDbSetItemMap.put(parentDataSourceItem, dataSet);
        } else {
            dataSet = (Item) dataSourceToDbSetItemMap.get(parentDataSourceItem);
        }

        return dataSet;
    }

    private void setVersionInDataSetItem(Item srcCvDbItem, Item dataSetItem, String dbName)
            throws ObjectStoreException {

        if (dataSetItem.hasAttribute("version")) {
            LOG.debug("Skipping setting version for a DataSet we have already seen:"
                    + dbName + " data set");
        } else {

            org.intermine.xml.full.Item versionItem =
                    getItemViaItemPath(srcCvDbItem, CV_DATABASE_VIA_DB_VERSION, srcItemReader);

            if (versionItem != null) {

                Attribute versionAttr = versionItem.getAttribute("version");

                if (versionAttr != null) {

                    dataSetItem.setAttribute("version", versionAttr.getValue());
                } else {
                    LOG.error("Version data is MISSING for this database:" + dbName);
                }

            } else {

                LOG.debug("Version data is NOT available for this database: " + dbName);
            }
        }
    }

    /**
     * Establishs a Datasource evidence item for source items that refer directly to the cv_db
     * table in the interpro schema of which the PROTEIN and METHOD tables are of current interest.
     *
     * Also the call to procureDataSourceAndSetItem ensures that the datasource item will
     * actually be created as the interpro_mappings file no longer handles
     * the cv_database -- datasource item
     * conversion.
     * */
    private Item[] setupCvDbDataSrcEvidenceRef(Item srcItem, Item tgtItem)
            throws ObjectStoreException {

        //CHECK THAT THERE IS A REFERENCE TO THE CV_DATABASE ITEM FOR THIS METHOD ITEM -
        // WARN IF NONE FOUND!
        org.intermine.xml.full.Reference cvdbRefSrc = srcItem.getReference(CV_DATABASE);
        Item[] sourceAndSet = new Item[2];
        Item dataSourceItem = null;
        Item dataSetItem = null;

        if (cvdbRefSrc != null) {

            org.intermine.xml.full.Item cvdbItem
                    = ItemHelper.convert(this.srcItemReader.getItemById(cvdbRefSrc.getRefId()));

            String sourceDbName = cvdbItem.getAttribute("dbname").getValue();
            dataSourceItem = procureDataSourceAndSetItem(sourceDbName);
            dataSetItem = procureDataSetItem(dataSourceItem);

            addToCollection(tgtItem, EVIDENCE, dataSetItem);

        } else {
            LOG.warn("FOUND A " + srcItem.getClassName() + " WITHOUT A REFERENCED CV_DATABASE!");
        }

        sourceAndSet[0] = dataSourceItem;
        sourceAndSet[1] = dataSetItem;
        return sourceAndSet;
    }

    /**
     * @param dataSrcItem - source database we are referencing
     * @param tgtItem - the Feature or Protein we are linking to
     * @param altAttrToUse - NULLABLE - if present try to use this instead of the IDENTIFIER attr.
     * */
    private Item createExtraDataSrcSynonym(Item dataSrcItem, Item tgtItem, String altAttrToUse) {

        Item extraDatabaseSynonym = null;

        if (dataSrcItem != null && tgtItem != null) {
            //Do we want to use an attribute instead of the Identifier...
            if (altAttrToUse != null && tgtItem.hasAttribute(altAttrToUse)) {
                extraDatabaseSynonym = createSynonym(
                        tgtItem.getIdentifier(), altAttrToUse,
                        tgtItem.getAttribute(altAttrToUse).getValue(),
                        new org.intermine.xml.full.Reference(SOURCE, dataSrcItem.getIdentifier()));
            } else if (tgtItem.hasAttribute(IDENTIFIER)) {
                extraDatabaseSynonym = createSynonym(
                        tgtItem.getIdentifier(), IDENTIFIER,
                        tgtItem.getAttribute(IDENTIFIER).getValue(),
                        new org.intermine.xml.full.Reference(SOURCE, dataSrcItem.getIdentifier()));
            } else {
                LOG.warn("CAN'T CREATE SYNONYM FOR TGTITEM:" + tgtItem.getClassName()
                        + " AS IT DOES NOT HAVE AN IDENTIFIER"
                        + (altAttrToUse != null ? (" OR THIS ATTR:" + altAttrToUse) : ""));
            }

        } else {
            LOG.warn("SKIPPING SYNONYM CREATION FOR TGTITEM:"
                    + (tgtItem != null ? tgtItem.getClassName() : "NULL"));
        }

        return extraDatabaseSynonym;
    }

    //Private helper method to assist in setting any reverse self relations in the
    // ProteinFamily object.
    private void setupEntryRelations(
            ItemPath familyFeatureItemPath, org.intermine.xml.full.Item srcItem,
            org.intermine.xml.full.Item tgtItem, String relationToMapTo,
            String targetCollectionName) throws ObjectStoreException {

        java.util.List familyList =
                getItemsViaItemPath(srcItem, familyFeatureItemPath, srcItemReader);

        if (familyList != null && familyList.size() > 0) {

            for (Iterator familyIterator = familyList.iterator(); familyIterator.hasNext();) {

                org.intermine.xml.full.Item relationshipItem =
                        (org.intermine.xml.full.Item) familyIterator.next();

                //check to see that the entry2xxxx item has a reference to the
                // cv_relation table so we can get it's type
                if (relationshipItem.hasReference(relationToMapTo)) {

                    org.intermine.xml.full.Reference relativeRef =
                            relationshipItem.getReference(relationToMapTo);

                    org.intermine.xml.full.Item nextRelative =
                            ItemHelper.convert(
                                    this.srcItemReader.getItemById(relativeRef.getRefId()));

                    addToCollection(tgtItem, targetCollectionName, nextRelative);
                } else {
                    LOG.warn("NO " + relationToMapTo
                            + " RELATION FOUND FOR PATH" + familyFeatureItemPath.toString());
                }
            }
        }
    }

    private Item createSynonym(
            String subjectId, String type, String value, org.intermine.xml.full.Reference ref) {
        Item synonym = createItem("Synonym");
        synonym.addReference(new org.intermine.xml.full.Reference("subject", subjectId));
        synonym.addAttribute(new Attribute("type", type));
        synonym.addAttribute(new Attribute("value", value));
        synonym.addReference(ref);
        return synonym;
    }

    /**
     * Factored this out so I can use it when I am not initially working on an entry item, but when
     * I am dealing with a protein via the supermatch table and need to make sure the entry_type is
     * set in the entry that the protein refers to so we can classify it as either a family or
     * domain and put into the correct collection for each protein...
     * <p/>
     *
     * @return a boolean indicating if we managed to set the entry_type or not...
     */
    private boolean tryAndSetEntryTypeInAnEntry(org.intermine.xml.full.Item entry)
            throws ObjectStoreException {

        //Perhaps we've already set the type for this entry already...
        if (entry.hasAttribute(ABBREV)) {
            return true;
        }

        if (entry.getReference(CV_ENTRY_TYPE) != null) {

            org.intermine.xml.full.Reference cvetRef = entry.getReference(CV_ENTRY_TYPE);

            org.intermine.model.fulldata.Item cvetItem =
                    this.srcItemReader.getItemById(cvetRef.getRefId());

            Set cvetAttrSet = cvetItem.getAttributes();

            Iterator cvetAttrIt = cvetAttrSet.iterator();

            org.intermine.model.fulldata.Attribute cvetAttrNext;

            while (cvetAttrIt.hasNext()) {

                cvetAttrNext = (org.intermine.model.fulldata.Attribute) cvetAttrIt.next();

                if (cvetAttrNext.getName().equalsIgnoreCase(ABBREV)) {
                    org.intermine.xml.full.Attribute srcEntryTypeAttr = new Attribute();
                    srcEntryTypeAttr.setName(ABBREV);
                    srcEntryTypeAttr.setValue(cvetAttrNext.getValue());
                    entry.addAttribute(srcEntryTypeAttr);
                    return true;
                }
            }

            LOG.warn("!! DATA BUG - ENTRY WITH REFERENCE TO CV_ENTRY_TYPE WITH NO ABBREV FOUND !!");
            return false;
        } else {
            LOG.warn("?? DATA BUG - ENTRY WITHOUT A REFERENCE TO A CV_ENTRY_TYPE OBJECT FOUND ??");
            return false;
        }
    }

    /**
     * WE HAVE OVERRIDDEN THE PARENT METHOD TO SET THE BATCH SIZE TO A LOWER VALUE TO AVOID GETTING
     * TO MANY QUERY ITEMS
     * <p/>
     * Returns the Iterator over Items that the DataTranslator will translate.
     *
     * @return an Iterator
     * @throws ObjectStoreException if something goes wrong
     */
    public Iterator getItemIterator() throws ObjectStoreException {

        //have to check this if we want our mock data tests to work...
        if (srcItemReader instanceof ObjectStoreItemReader) {

            ((ObjectStoreItemReader) srcItemReader).setBatchSize(500);
        }

        return srcItemReader.itemIterator();
    }


    protected static final String PATH_NAME_SPACE = "http://www.intermine.org/model/interpro#";

    protected static final ItemPath TAXONOMY_FROM_PROTEIN =
            new ItemPath("(protein <- protein2taxonomy.protein).taxonomy", PATH_NAME_SPACE);
    protected static final ItemPath METHOD_TO_ENTRY_VIA_ENTRY_TO_METHOD =
            new ItemPath("(method <- entry2method.method).entry", PATH_NAME_SPACE);
    protected static final ItemPath METHOD_TO_CV_EVIDENCE_VIA_ENTRY_TO_METHOD =
            new ItemPath("(method <- entry2method.method).cv_evidence", PATH_NAME_SPACE);

    protected static final ItemPath ENTRY_VIA_ENTRY_TO_METHOD =
            new ItemPath("(entry <- entry2method.entry)", PATH_NAME_SPACE);

    protected static final ItemPath ENTRY_FROM_ENTRY_TO_COMMON_ANNOTATION =
            new ItemPath("(entry <- entry2common_annotation.entry)", PATH_NAME_SPACE);

    protected static final ItemPath PROTEIN_FROM_MATCHES =
            new ItemPath("(protein <- matches.protein)", PATH_NAME_SPACE);
    protected static final ItemPath METHOD_FROM_MATCHES =
            new ItemPath("(method <- matches.method)", PATH_NAME_SPACE);

    protected static final ItemPath PROTEIN_FROM_SUPER_MATCH =
            new ItemPath("(protein <- supermatch.protein)", PATH_NAME_SPACE);
    protected static final ItemPath ENTRY_FROM_SUPER_MATCH =
            new ItemPath("(entry <- supermatch.entry)", PATH_NAME_SPACE);

    protected static final ItemPath ENTRY_FROM_ENTRY_TO_COMP_VIA_ENTRY_ONE =
            new ItemPath("(entry <- entry2comp.entry1)", PATH_NAME_SPACE);
    protected static final ItemPath ENTRY_FROM_ENTRY_TO_COMP_VIA_ENTRY_TWO =
            new ItemPath("(entry <- entry2comp.entry2)", PATH_NAME_SPACE);

    protected static final ItemPath ENTRY_FROM_ENTRY_TO_ENTRY_VIA_ENTRY =
            new ItemPath("(entry <- entry2entry.entry)", PATH_NAME_SPACE);
    protected static final ItemPath ENTRY_FROM_ENTRY_TO_ENTRY_VIA_PARENT =
            new ItemPath("(entry <- entry2entry.parent)", PATH_NAME_SPACE);

    protected static final ItemPath ENTRY_TO_ENTRY_TO_CV_RELATION =
            new ItemPath("entry2entry.cv_relation", PATH_NAME_SPACE);
    protected static final ItemPath ENTRY_TO_COMP_TO_CV_RELATION =
            new ItemPath("entry2comp.cv_relation", PATH_NAME_SPACE);

    protected static final ItemPath DB_VERSION_TO_CV_DATABASE =
            new ItemPath("db_version.cv_database", PATH_NAME_SPACE);

    protected static final ItemPath CV_DATABASE_VIA_DB_VERSION =
            new ItemPath("(cv_database <- db_version.cv_database)", PATH_NAME_SPACE);



    /**
     * @return A map of all the interpro related prefetch descriptors.
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();

        {
            Set proteinSet = new HashSet();

            proteinSet.add(TAXONOMY_FROM_PROTEIN.getItemPrefetchDescriptor());

            ItemPrefetchDescriptor p2mDesc =
                    new ItemPrefetchDescriptor("(protein <- matches.protein)");
            p2mDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "protein"));
            p2mDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.intermine.org/model/interpro#matches", false));
            ItemPrefetchDescriptor p2m2mDesc =
                    new ItemPrefetchDescriptor("(protein <- matches.protein).method");
            p2m2mDesc.addConstraint(new ItemPrefetchConstraintDynamic("method",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            p2mDesc.addPath(p2m2mDesc);
            proteinSet.add(p2mDesc);

            ItemPrefetchDescriptor p2smDesc =
                    new ItemPrefetchDescriptor("(protein <- supermatch.protein)");
            p2smDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "protein"));
            p2smDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.intermine.org/model/interpro#supermatch", false));
            ItemPrefetchDescriptor p2sm2eDesc = new
                    ItemPrefetchDescriptor("(protein <- supermatch.protein).entry");
            p2sm2eDesc.addConstraint(new ItemPrefetchConstraintDynamic("entry",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            p2smDesc.addPath(p2sm2eDesc);
            proteinSet.add(p2smDesc);

            paths.put("http://www.intermine.org/model/interpro#protein", proteinSet);
        }
        {
            Set methodSet = new HashSet();

            ItemPrefetchDescriptor m2cvdDesc = new ItemPrefetchDescriptor("(method.cv_database)");
            m2cvdDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_database",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            methodSet.add(m2cvdDesc);

            ItemPrefetchDescriptor m2mDesc =
                    new ItemPrefetchDescriptor("(method <- matches.method)");
            m2mDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "method"));
            m2mDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.intermine.org/model/interpro#matches", false));
            ItemPrefetchDescriptor m2m2pDesc =
                    new ItemPrefetchDescriptor("(method <- matches.method).protein");
            m2m2pDesc.addConstraint(new ItemPrefetchConstraintDynamic("protein",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            m2mDesc.addPath(m2m2pDesc);
            methodSet.add(m2mDesc);

            ItemPrefetchDescriptor m2e2eDesc =
                    new ItemPrefetchDescriptor("(method <- entry2method.method)");
            m2e2eDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "method"));
            m2e2eDesc.addConstraint(new FieldNameAndValue(
                    ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.intermine.org/model/interpro#entry2method", false));
            ItemPrefetchDescriptor m2e2m2eDesc =
                    new ItemPrefetchDescriptor("(method <- entry2method.method).entry");
            m2e2m2eDesc.addConstraint(new ItemPrefetchConstraintDynamic("entry",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            m2e2eDesc.addPath(m2e2m2eDesc);
            ItemPrefetchDescriptor m2e2m2cveDesc =
                    new ItemPrefetchDescriptor("(method <- entry2method.method).cv_evidence");
            m2e2m2cveDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_evidence",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            m2e2eDesc.addPath(m2e2m2cveDesc);
            methodSet.add(m2e2eDesc);

            paths.put("http://www.intermine.org/model/interpro#method", methodSet);
        }
        {
            Set entrySet = getEntryPrefetchDescriptorSet();
            paths.put("http://www.intermine.org/model/interpro#entry", entrySet);
        }
        {
            Set matchesSet = new HashSet();

            ItemPrefetchDescriptor m2cvdbDesc = new ItemPrefetchDescriptor("(matches.cv_database)");
            m2cvdbDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_database",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            matchesSet.add(m2cvdbDesc);

            ItemPrefetchDescriptor m2cvevDesc = new ItemPrefetchDescriptor("(matches.cv_evidence)");
            m2cvevDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_evidence",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            matchesSet.add(m2cvevDesc);

            paths.put("http://www.intermine.org/model/interpro#matches", matchesSet);
        }
        {
            Set entry2entrySet = new HashSet();

            entry2entrySet.add(ENTRY_TO_ENTRY_TO_CV_RELATION.getItemPrefetchDescriptor());

            paths.put("http://www.intermine.org/model/interpro#entry2entry", entry2entrySet);
        }
        {
            Set entry2compSet = new HashSet();

            entry2compSet.add(ENTRY_TO_COMP_TO_CV_RELATION.getItemPrefetchDescriptor());

            paths.put("http://www.intermine.org/model/interpro#entry2comp", entry2compSet);
        }
        {
            Set dbVersionSet = new HashSet();

            dbVersionSet.add(DB_VERSION_TO_CV_DATABASE.getItemPrefetchDescriptor());

            paths.put("http://www.intermine.org/model/interpro#db_version", dbVersionSet);
        }
        {
            Set cvDatabaseSet = new HashSet();

            cvDatabaseSet.add(CV_DATABASE_VIA_DB_VERSION.getItemPrefetchDescriptor());

            ItemPrefetchDescriptor cvDbViaDbVerCvDbDesc
                          = new ItemPrefetchDescriptor("(cv_database <- db_version.cv_database)");
            cvDbViaDbVerCvDbDesc.addConstraint(
                          new ItemPrefetchConstraintDynamic("cv_database",
                          ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            cvDatabaseSet.add(cvDbViaDbVerCvDbDesc);

            paths.put("http://www.intermine.org/model/interpro#cv_database", cvDatabaseSet);
        }

        return paths;
    }


    /**
     * @return A set of the prefetch descriptors related to the Entry source item.
     */
    private static Set getEntryPrefetchDescriptorSet() {

        HashSet entrySet = new HashSet();

        ItemPrefetchDescriptor e2cvetDesc = new ItemPrefetchDescriptor("(entry.cv_entry_type)");
        e2cvetDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_entry_type",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        entrySet.add(e2cvetDesc);

        ItemPrefetchDescriptor e2cDesc =
                new ItemPrefetchDescriptor("(entry <- entry2common_annotation.entry)");
        e2cDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
        e2cDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#entry2common_annotation", false));
        ItemPrefetchDescriptor e2ec2cDesc = new ItemPrefetchDescriptor(
                "(entry <- entry2common_annotation.entry).common_annotation");
        e2ec2cDesc.addConstraint(new ItemPrefetchConstraintDynamic("common_annotation",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        e2cDesc.addPath(e2ec2cDesc);
        entrySet.add(e2cDesc);

        ItemPrefetchDescriptor e2e2mDesc =
                new ItemPrefetchDescriptor("(entry <- entry2method.entry)");
        e2e2mDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
        e2e2mDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#entry2method", false));
        ItemPrefetchDescriptor e2e2m2mDesc =
                new ItemPrefetchDescriptor("(entry <- entry2method.entry).method");
        e2e2m2mDesc.addConstraint(new ItemPrefetchConstraintDynamic("method",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        e2e2mDesc.addPath(e2e2m2mDesc);
        entrySet.add(e2e2mDesc);

        ItemPrefetchDescriptor e2smDesc = new ItemPrefetchDescriptor("(entry <- supermatch.entry)");
        e2smDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
        e2smDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#supermatch", false));
        ItemPrefetchDescriptor e2sm2pDesc =
                new ItemPrefetchDescriptor("(entry <- supermatch.entry).protein");
        e2sm2pDesc.addConstraint(new ItemPrefetchConstraintDynamic("protein",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        e2smDesc.addPath(e2sm2pDesc);
        entrySet.add(e2smDesc);

        ItemPrefetchDescriptor eToe2cViaE1Desc =
                new ItemPrefetchDescriptor("(entry <- entry2comp.entry1)");
        eToe2cViaE1Desc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry1"));
        eToe2cViaE1Desc.addConstraint(new FieldNameAndValue(
                ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#entry2comp", false));
        ItemPrefetchDescriptor eToe2cViaE1ToE2Desc =
                new ItemPrefetchDescriptor("(entry <- entry2comp.entry1).entry2");
        eToe2cViaE1ToE2Desc.addConstraint(new ItemPrefetchConstraintDynamic("entry2",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        eToe2cViaE1Desc.addPath(eToe2cViaE1ToE2Desc);
        entrySet.add(eToe2cViaE1Desc);

        ItemPrefetchDescriptor eToe2cViaE2Desc =
                new ItemPrefetchDescriptor("(entry <- entry2comp.entry2)");
        eToe2cViaE2Desc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry2"));
        eToe2cViaE2Desc.addConstraint(new FieldNameAndValue(
                ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#entry2comp", false));
        ItemPrefetchDescriptor eToe2cViaE2ToE1Desc =
                new ItemPrefetchDescriptor("(entry <- entry2comp.entry2).entry1");
        eToe2cViaE2ToE1Desc.addConstraint(new ItemPrefetchConstraintDynamic("entry1",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        eToe2cViaE2Desc.addPath(eToe2cViaE2ToE1Desc);
        entrySet.add(eToe2cViaE2Desc);

        ItemPrefetchDescriptor eToe2eViaEDesc =
                new ItemPrefetchDescriptor("(entry <- entry2entry.entry)");
        eToe2eViaEDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
        eToe2eViaEDesc.addConstraint(new FieldNameAndValue(
                ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#entry2entry", false));
        ItemPrefetchDescriptor eToe2eViaEToPDesc =
                new ItemPrefetchDescriptor("(entry <- entry2entry.entry).parent");
        eToe2eViaEToPDesc.addConstraint(new ItemPrefetchConstraintDynamic("parent",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        eToe2eViaEDesc.addPath(eToe2eViaEToPDesc);
        entrySet.add(eToe2eViaEDesc);

        ItemPrefetchDescriptor eToe2eViaPDesc =
                new ItemPrefetchDescriptor("(entry <- entry2entry.parent)");
        eToe2eViaPDesc.addConstraint(new ItemPrefetchConstraintDynamic(
                ObjectStoreItemPathFollowingImpl.IDENTIFIER, "parent"));
        eToe2eViaPDesc.addConstraint(new FieldNameAndValue(
                ObjectStoreItemPathFollowingImpl.CLASSNAME,
                "http://www.intermine.org/model/interpro#entry2entry", false));
        ItemPrefetchDescriptor eToe2eViaPToEDesc =
                new ItemPrefetchDescriptor("(entry <- entry2entry.parent).entry");
        eToe2eViaPToEDesc.addConstraint(new ItemPrefetchConstraintDynamic("entry",
                ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        eToe2eViaPDesc.addPath(eToe2eViaPToEDesc);
        entrySet.add(eToe2eViaPDesc);

        return entrySet;
    }

    /**
     * Holds a datasource item & a manual count of how many times the item has been used.
     * */
    class DataSourceAndSetUsageCounter
    {

        private Item dataSource;
        private Item dataSet;

        private int sourceUseageCount;

        /**
         * @param dataSource - the source db item to count for.
         * @param dataSet - the related dataset item.
         * */
        DataSourceAndSetUsageCounter(Item dataSource, Item dataSet) {
            this.dataSource = dataSource;
            this.dataSet = dataSet;
            sourceUseageCount = 0;
        }

        /**
         * @return The datasource item that we are counting for.
         * */
        synchronized Item getDataSource() {
            sourceUseageCount++;
            return dataSource;
        }

        /**
         * @return The dataset item that is related to the datasource item.
         * */
        public Item getDataSet() {
            return dataSet;
        }

        /**
         * @return the current value of our usage counter.
         * */
        synchronized int getSourceUseageCount() {
            return sourceUseageCount;
        }
    }


}
