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

import org.intermine.dataconversion.*;
import org.intermine.metadata.Model;
import org.intermine.xml.full.*;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.InterMineException;
import org.intermine.util.XmlUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Convert Ensembl data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Peter McLaren
 */
public class EnsemblDataTranslator extends DataTranslator
{

    /**
     * USEFULL CONSTANT
     * */
    public static final String IDENTIFIER = "identifier";

    protected static final String PATH_NAME_SPACE = "http://www.intermine.org/model/ensembl#";

    protected static final ItemPath TRANSCRIPT_VIA_TRANSLATION =
            new ItemPath("(transcript <- translation.transcript)", PATH_NAME_SPACE);

    protected static final ItemPath EXON_VIA_EXON_TRANSCRIPT =
            new ItemPath("(exon <- exon_transcript.exon)", PATH_NAME_SPACE);

    protected static final ItemPath SEQ_REGION_VIA_DNA =
            new ItemPath("(seq_region <- dna.seq_region)", PATH_NAME_SPACE);

    protected static final ItemPath MARKER_VIA_MARKER_SYNONYM =
            new ItemPath("(marker <- marker_synonym.marker)", PATH_NAME_SPACE);

    protected static final Logger LOG = Logger.getLogger(EnsemblDataTranslator.class);

    private Map seqIdMap = new HashMap();
    private Map itemId2SynMap = new HashMap();

    private Map markerSynonymMap = new HashMap();

    private String srcNs;

    //Holds all the information from the Ensembl Config file - i.e. datasources & sets
    private EnsemblConfig config;

    /**
     * @param srcItemReader Our source item provider.
     * @param mergeSpec The contents of our mapping spec.
     * @param srcModel The data model we are translating items from.
     * @param tgtModel The data model we are translating items into.
     * @param propsFileName File with set of Properties that are organism specific.
     * @param orgAbbrev A suitably short acronym to identify which ogransim to process; 'HS' = Human
     * @deprecated Have to get the translator to look for its props file now...
     */
    public EnsemblDataTranslator(ItemReader srcItemReader,
                                 Properties mergeSpec,
                                 Model srcModel,
                                 Model tgtModel,
                                 String propsFileName,
                                 String orgAbbrev) {

        super(srcItemReader, mergeSpec, srcModel, tgtModel);

        Properties ensemblProps;
        try {
            ensemblProps = getEnsemblProperties(propsFileName);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        config = new EnsemblConfig(ensemblProps, orgAbbrev);
    }

    /**
     * @param srcItemReader Our source item provider.
     * @param mergeSpec The contents of our mapping spec.
     * @param srcModel The data model we are translating items from.
     * @param tgtModel The data model we are translating items into.
     * @param ensemblProps Properties that are organism specific.
     * @param orgAbbrev A suitably short acronym to identify which ogransim to process; 'HS' = Human
     */
    public EnsemblDataTranslator(ItemReader srcItemReader,
                                 Properties mergeSpec,
                                 Model srcModel,
                                 Model tgtModel,
                                 Properties ensemblProps,
                                 String orgAbbrev) {
        super(srcItemReader, mergeSpec, srcModel, tgtModel);

        config = new EnsemblConfig(ensemblProps, orgAbbrev);
    }

    /**
     * {@inheritDoc}
     */
    public void translate(ItemWriter tgtItemWriter)
            throws ObjectStoreException, InterMineException {

        super.translate(tgtItemWriter);

        tgtItemWriter.store(ItemHelper.convert(config.getOrganism()));

        tgtItemWriter.store(ItemHelper.convert(config.getEnsemblDataSet()));

        for (Iterator dsIt = config.getDataSrcItemIterator(); dsIt.hasNext(); ) {

            tgtItemWriter.store(ItemHelper.convert((Item) dsIt.next()));
        }

        for (Iterator seqSynIt = itemId2SynMap.values().iterator(); seqSynIt.hasNext();) {

            tgtItemWriter.store(ItemHelper.convert((Item) seqSynIt.next()));
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Collection translateItem(Item srcItem)
            throws ObjectStoreException, InterMineException {
        Collection result = new HashSet();
        srcNs = XmlUtil.getNamespaceFromURI(srcItem.getClassName());
        String srcItemClassName = XmlUtil.getFragmentFromURI(srcItem.getClassName());

        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
                if ("dna".equals(srcItemClassName)) {

                    if (config.doStoreDna()) {

                        if (srcItem.hasAttribute("sequence")) {
                            int seqLen = srcItem.getAttribute("sequence").getValue().length();
                            tgtItem.setAttribute("length", Integer.toString(seqLen));
                        }
                    } else {
                        storeTgtItem = false;
                    }
                } else if ("karyotype".equals(srcItemClassName)) {

                    translateKaryotype(srcItem, tgtItem, result);
                } else if ("exon".equals(srcItemClassName)) {

                    //Skip this exon if it points to invalid transcripts!
                    // if (!isExonToBeKept(srcItem, true)) {
//                         LOG.debug("isExonToBeKept false for Exon:" + srcItem.getIdentifier());
//                         return result;
//                     }

                    tgtItem.addReference(config.getOrganismRef());
                    Item stableId = getStableId("exon", srcItem.getIdentifier(), srcNs);
                    // <- exon_stable_id.exon
                    if (stableId != null) {
                        moveField(stableId, tgtItem, "stable_id", IDENTIFIER);
                    }
                    addReferencedItem(tgtItem, config.getEnsemblDataSet(),
                            "evidence", true, "", false);
                    Item location = createLocation(srcItem, tgtItem, true); // seq_region
                    // seq_region.coord-sys
                    result.add(location);

                } else if ("gene".equals(srcItemClassName)) {
                    tgtItem.addReference(config.getOrganismRef());
                    addReferencedItem(tgtItem, config.getEnsemblDataSet(),
                            "evidence", true, "", false);
                    Item comment = createComment(srcItem, tgtItem);
                    if (comment != null) {
                        result.add(comment);
                    }
                    Item location = createLocation(srcItem, tgtItem, true); // seq_region
                    // seq_region.coord-sys
                    result.add(location);
                    if (config.createAnalysisResult) {
                        Item anaResult = createAnalysisResult(srcItem, tgtItem); // analysis
                        result.add(anaResult);
                    }

                    // the default gene identifier should be its stable id (or identifier if none)
                    //i.e. for anoph they are the same, but for human they are differant
                    //note: the organismDbId is effecivly what we choose as a primary accession
                    // and thus may differ from what we want to assign as the identifier...
                    result.addAll(setGeneSynonyms(srcItem, tgtItem, srcNs));

                } else if ("transcript".equals(srcItemClassName)) {

                    translateTranscript(srcItem, tgtItem, result, srcNs);

                } else if ("translation".equals(srcItemClassName)) {
                    tgtItem.addReference(config.getOrganismRef());

                    // if no identifier set the identifier as name (primary key)
                    if (!tgtItem.hasAttribute(IDENTIFIER)) {
                        Item stableId = getStableId("translation", srcItem.getIdentifier(), srcNs);
                        // <- transcript_stable_id.transcript
                        if (stableId != null) {
                            moveField(stableId, tgtItem, "stable_id", IDENTIFIER);
                        } else {
                            tgtItem.addAttribute(
                                    new Attribute(IDENTIFIER, srcItem.getIdentifier()));
                        }
                    }

                // stable_ids become syonyms, need ensembl DataSet as evidence
                } else if (srcItemClassName.endsWith("_stable_id")) {

                    //check to see if the exons point to valid transcripts before making a synonym.
                   //  if (srcItemClassName.startsWith("exon")) {
//                         //Skip this exon if it points to invalid transcripts!
//                         if (!isExonToBeKept(srcItem, false)) {
//                             return result;
//                         }
//                     }

                    tgtItem.addToCollection("evidence", config.getEnsemblDataSet());
                    tgtItem.addReference(config.getEnsemblDataSrcRef());
                    tgtItem.addAttribute(new Attribute("type", IDENTIFIER));
                } else if ("repeat_feature".equals(srcItemClassName)) {

                    translateRepeatFeature(srcItem, tgtItem, result);
                    // repeat_consensus

                } else if ("marker".equals(srcItemClassName)) {
                    tgtItem.addReference(config.getOrganismRef());

                    //is there an identifier set?
                    if (!srcItem.hasAttribute("identifier")) {

                        //ok try and look up any related marker_synonym items...

                    }

                    addReferencedItem(tgtItem,
                            config.getEnsemblDataSet(), "evidence", true, "", false);
                    Set locations = createLocations(srcItem, tgtItem, srcNs);
                    // <- marker_feature.marker
                    // (<- marker_feature.marker).seq_region
                    // (<- marker_feature.marker).seq_region.coord_system
                    //List locationIds = new ArrayList();
                    for (Iterator j = locations.iterator(); j.hasNext();) {
                        Item location = (Item) j.next();
                        //locationIds.add(location.getIdentifier());
                        result.add(location);
                    }
                    setNameAttribute(srcItem, tgtItem);
                    // display_marker_synonym
                }
                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        } else if ("marker_synonym".equals(srcItemClassName)) {
            Item synonym = getMarkerSynonym(srcItem);
            if (synonym != null) {
                result.add(synonym);
            }
            // assembly maps to null but want to create location on a supercontig
        } else if ("assembly".equals(srcItemClassName)) {
            Item location = createAssemblyLocation(result, srcItem);
            result.add(location);
            // seq_region map to null, become Chromosome, Supercontig, Clone and Contig respectively
        } else if ("seq_region".equals(srcItemClassName)) {
            Item seq = getSeqItem(srcItem.getIdentifier(), true, srcItem);
            seq.addReference(config.getOrganismRef());
            result.add(seq);
            //simple_feature map to null, become TRNA/CpGIsland depending on analysis_id(logic_name)
        } else if ("simple_feature".equals(srcItemClassName)) {
            Item simpleFeature = createSimpleFeature(srcItem);
            if (simpleFeature != null && simpleFeature.getIdentifier() != null
                    && !simpleFeature.getIdentifier().equals("")) {
                result.add(simpleFeature);
                result.add(createLocation(srcItem, simpleFeature, true));
                result.add(createAnalysisResult(srcItem, simpleFeature));
            }
        }
        return result;
    }

    private void translateKaryotype(Item srcItem, Item tgtItem, Collection result)
            throws ObjectStoreException {

        tgtItem.addReference(config.getOrganismRef());
        addReferencedItem(tgtItem, config.getEnsemblDataSet(), "evidence", true, "", false);
        Item location = createLocation(srcItem, tgtItem, true); // seq_region
        // seq_region.coord-sys
        location.addAttribute(new Attribute("strand", "0"));
        result.add(location);

        if (srcItem.hasReference("seq_region")) {
            Item seq = getSeqItem(srcItem.getReference("seq_region").getRefId(), false, srcItem);

            tgtItem.addReference(new Reference("chromosome", seq.getIdentifier()));
        }

        result.add(createSynonym(tgtItem.getIdentifier(), IDENTIFIER,
                tgtItem.getAttribute(IDENTIFIER).getValue(), config.getEnsemblDataSrcRef())
        );
    }

    private void translateTranscript(Item srcItem, Item tgtItem, Collection result, String srcNs)
            throws ObjectStoreException {

        tgtItem.addReference(config.getOrganismRef());
        addReferencedItem(tgtItem,
                config.getEnsemblDataSet(), "evidence", true, "", false);
        // SimpleRelation between Gene and Transcript

        result.add(createSimpleRelation(tgtItem.getReference("gene").getRefId(),
                tgtItem.getIdentifier()));

        // set transcript identifier to be ensembl stable id
        if (!tgtItem.hasAttribute(IDENTIFIER)) {
            Item stableId = getStableId("transcript", srcItem.getIdentifier(), srcNs);
            if (stableId != null) {
                moveField(stableId, tgtItem, "stable_id", IDENTIFIER);
            } else {
                tgtItem.addAttribute(new Attribute(IDENTIFIER,
                        srcItem.getIdentifier()));
            }
        }

        Item translation = getItemViaItemPath(
                srcItem, TRANSCRIPT_VIA_TRANSLATION, srcItemReader);

        if (translation != null) {
            // need to fetch translation to get identifier for CDS
            // create CDS and reference from MRNA
            Item cds = createItem(tgtNs + "CDS", "");
            Item stableId = getStableId("translation",
                                        translation.getIdentifier(), srcNs);
            cds.setAttribute(IDENTIFIER,
                    stableId.getAttribute("stable_id").getValue() + "_CDS");
            cds.addToCollection("polypeptides", translation.getIdentifier());
            result.add(createSimpleRelation(cds.getIdentifier(),
                    translation.getIdentifier()));

            Item cdsSyn = createSynonym(
                    cds.getIdentifier(),
                    IDENTIFIER,
                    cds.getAttribute(IDENTIFIER).getValue(),
                    config.getEnsemblDataSrcRef());

            result.add(cdsSyn);

            cds.addReference(config.getOrganismRef());
            addReferencedItem(cds,
                    config.getEnsemblDataSet(), "evidence", true, "", false);

            tgtItem.addToCollection("CDSs", cds);
            result.add(createSimpleRelation(
                    tgtItem.getIdentifier(), cds.getIdentifier()));
            result.add(cds);
        }

    }

    private void translateRepeatFeature(Item srcItem, Item tgtItem, Collection result)
            throws ObjectStoreException {
        tgtItem.addReference(config.getOrganismRef());
        addReferencedItem(tgtItem, config.getEnsemblDataSet(), "evidence", true, "", false);
        result.add(createAnalysisResult(srcItem, tgtItem));
        // analysis
        result.add(createLocation(srcItem, tgtItem, true));
        // seq_region
        // seq_region.coord_system
        promoteField(tgtItem, srcItem, "consensus", "repeat_consensus", "repeat_consensus");
        // repeat_consensus
        promoteField(tgtItem, srcItem, "type", "repeat_consensus", "repeat_class");
        // repeat_consensus
        promoteField(tgtItem, srcItem, IDENTIFIER, "repeat_consensus", "repeat_name");

        //Create a more usable identifier field.
        StringBuffer newIdBuff = new StringBuffer();
        newIdBuff.append(tgtItem.getAttribute(IDENTIFIER).getValue());
        newIdBuff.append("_");

        Item seqRegItem = ItemHelper.convert(srcItemReader.getItemById(
                srcItem.getReference("seq_region").getRefId()));

        newIdBuff.append(seqRegItem.getAttribute("name").getValue());
        newIdBuff.append(":");

        newIdBuff.append(srcItem.getAttribute("seq_region_start").getValue());
        newIdBuff.append("..");
        newIdBuff.append(srcItem.getAttribute("seq_region_end").getValue());
        tgtItem.removeAttribute(IDENTIFIER);
        tgtItem.setAttribute(IDENTIFIER, newIdBuff.toString());

        Item rfSyn = createSynonym(tgtItem.getIdentifier(), IDENTIFIER,
                tgtItem.getAttribute(IDENTIFIER).getValue(), config.getEnsemblDataSrcRef());

        result.add(rfSyn);
    }

    /**
     * @param sourceItem - can be an exon or an exon_stable_id source db item.
     * @param srcIsExonNotExonStableId - flag to distinguish between the 2 possible src items.
     *
     * @return a boolean indicating that the exon will be kept
     * - hence we can store it and make a synonym for it
     *
     * @throws ObjectStoreException if there is a problem finding one of the related items.
     * */
    private boolean isExonToBeKept(Item sourceItem, boolean srcIsExonNotExonStableId)
            throws ObjectStoreException {

        int invalidETCount = 0;
        int validETCount = 0;

        Item srcItem;

        if (srcIsExonNotExonStableId) {
            srcItem = sourceItem;
        } else {
            srcItem = ItemHelper.convert(
                    srcItemReader.getItemById(sourceItem.getReference("exon").getRefId()));
        }

        List etList = getItemsViaItemPath(srcItem, EXON_VIA_EXON_TRANSCRIPT, srcItemReader);

        //Loop over the transcripts for each exon to see if they are valid or not...
        for (Iterator etIt = etList.iterator(); etIt.hasNext();) {

            Item etNext = (Item) etIt.next();
            Item tscrpt = ItemHelper.convert(srcItemReader.getItemById(
                          etNext.getReference("transcript").getRefId()));

            //We only allow protein_coding types in...
            String bioType = tscrpt.getAttribute("biotype").getValue();

            if (!bioType.equalsIgnoreCase("bacterial_contaminant")) {
                validETCount++;
            } else {
                invalidETCount++;
                LOG.debug("Found an invalid biotype:" + bioType);
            }
        }

        //Check to see if any of the exons are pointing to invalid transcripts...
        if (validETCount == 0 && invalidETCount == 0) {
            LOG.debug("Exon with no transcript found!" + (srcItem.hasAttribute(IDENTIFIER)
                    ? srcItem.getAttribute(IDENTIFIER).getValue() : srcItem.getIdentifier()));
        } else if (validETCount >= 1 && invalidETCount == 0) {
            return true;
        } else if (validETCount == 0 && invalidETCount >= 1) {
            LOG.debug("Exon with an invalid transcript found!" + (srcItem.hasAttribute(IDENTIFIER)
                    ? srcItem.getAttribute(IDENTIFIER).getValue() : srcItem.getIdentifier()));
        } else if (validETCount >= 1 && invalidETCount >= 1) {
            LOG.debug("Exon with valid and invalid transcripts found!"
                    + (srcItem.hasAttribute(IDENTIFIER)
                    ? srcItem.getAttribute(IDENTIFIER).getValue() : srcItem.getIdentifier()));
            return true; //keep this - but log it
        }

        return false;
    }


    /**
     * @param srcItem = ensembl:gene
     * @param tgtItem = flymine:gene
     * @param srcNs srcItem namespace
     * @return a set of gene synonyms
     * @throws ObjectStoreException if anything goes wrong
     */
    private Set setGeneSynonyms(Item srcItem, Item tgtItem, String srcNs)
            throws ObjectStoreException {

        Set synonyms = new HashSet();
        Reference extDbRef = new Reference();
        //If this is ever null then there's a problem with the ensembl data!!!
        Item stableIdItem = getStableId("gene", srcItem.getIdentifier(), srcNs);
        // <- gene_stable_id.gene
        String stableId = stableIdItem.getAttribute("stable_id").getValue();

        tgtItem.addAttribute(new Attribute("identifier", stableId));

        //Set up all the optional xref synonyms - if the gene is KNOWN it should
        // have an ensembl xref to another db's accesssion for the same gene.

        if (srcItem.hasReference("display_xref")) {
            Item xref = ItemHelper.convert(srcItemReader.getItemById(
                    srcItem.getReference("display_xref").getRefId()));

            String dbname = null;
            String xrefIdentifier = null;
            String symbol = null;

            //look for the reference to an xref database - the external_db
            if (xref.hasReference("external_db")) {
                Item extDb = ItemHelper.convert(srcItemReader.getItemById(
                             xref.getReference("external_db").getRefId()));
                if (extDb.hasAttribute("db_name")) {
                    dbname = extDb.getAttribute("db_name").getValue();
                }
            }
            //look for a primary accession - if we have one we may be able to create a synonym
            if (xref.hasAttribute("dbprimary_acc")) {
                xrefIdentifier = xref.getAttribute("dbprimary_acc").getValue();
            }
            //look for the display label - since we might be able to use it as a symbol synonym.
            if (xref.hasAttribute("display_label")) {
                symbol = xref.getAttribute("display_label").getValue();
            }

            //check to see if we have a valid accession & dbname.
            if (xrefIdentifier != null && !xrefIdentifier.equals("")
                && dbname != null && !dbname.equals("")) {
                //If we have a valid xref for this dsname then we can create the synonym for the
                // external database accession.
                //NOTE: if the xref is being used as an alternative identifier it will already have
                // a synonym with type 'identifier'.
                if (config.useXrefDbsForGeneOrganismDbId()
                    && config.geneXrefDbName.equalsIgnoreCase(dbname)) {
                    tgtItem.addAttribute(new Attribute("organismDbId", xrefIdentifier));
                    extDbRef = config.getDataSrcRefByDataSrcName(dbname);
                    Item synonym = createProductSynonym(tgtItem, "accession",
                                   xrefIdentifier, extDbRef);
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    synonyms.add(synonym);

                } else {
                    tgtItem.addAttribute(new Attribute("identifier", stableId));
                }

                if (config.containsXrefDataSourceNamed(dbname)) {
                    String identifierType;

                    if (config.useXrefDbsForGeneSymbol()) {
                        identifierType = "symbol";

                        extDbRef = config.getDataSrcRefByDataSrcName(dbname);
                        Item synonym =
                        createProductSynonym(tgtItem, identifierType, xrefIdentifier, extDbRef);
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        synonyms.add(synonym);
                        tgtItem.addAttribute(new Attribute(identifierType, xrefIdentifier));
                    }
                }
            } else {
                tgtItem.addAttribute(new Attribute("identifier", stableId));
            }


            //check to see if we have a valid display_lable (synonym) & dbname.
            if (symbol != null && !symbol.equals("")
                && dbname != null && !dbname.equals("")) {
                //Now check to see if we can create a synonym that represents a symbol as well.
                if (config.containsXrefSymbolDataSourceNamed(dbname)) {
                    extDbRef = config.getDataSrcRefByDataSrcName(dbname);
                    Item synonym = createProductSynonym(tgtItem, "symbol", symbol, extDbRef);
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    synonyms.add(synonym);
                    tgtItem.addAttribute(new Attribute("symbol", symbol));

                }
            }

        } else {
            tgtItem.addAttribute(new Attribute("identifier", stableId));
        }

        return synonyms;

    }


    private Item createSimpleRelation(String objectId, String subjectId) {
        Item sr = createItem("SimpleRelation");
        sr.setReference("object", objectId);
        sr.setReference("subject", subjectId);
        return sr;
    }

    /**
     * Translate a "located" Item into an Item and a location
     *
     * @param srcItem        the source Item
     * @param tgtItem        the target Item (after translation)
     * @param srcItemIsChild true if srcItem should be subject of Location
     * @return the location item
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Item createLocation(Item srcItem, Item tgtItem,
                                  boolean srcItemIsChild)
            throws ObjectStoreException {
        Item location = createItem(tgtNs + "Location", "");

        if (srcItem.hasAttribute("seq_region_start")) {
            moveField(srcItem, location, "seq_region_start", "start");
        }
        if (srcItem.hasAttribute("seq_region_end")) {
            moveField(srcItem, location, "seq_region_end", "end");
        }
        location.addAttribute(new Attribute("startIsPartial", "false"));
        location.addAttribute(new Attribute("endIsPartial", "false"));

        if (srcItem.hasAttribute("seq_region_strand")) {
            moveField(srcItem, location, "seq_region_strand", "strand");
        }
        if (srcItem.hasAttribute("phase")) {
            moveField(srcItem, location, "phase", "phase");
        }
        if (srcItem.hasAttribute("end_phase")) {
            moveField(srcItem, location, "end_phase", "endPhase");
        }
        if (srcItem.hasAttribute("ori")) {
            moveField(srcItem, location, "ori", "strand");
        }
        if (srcItem.hasReference("seq_region")) {

            String refId = srcItem.getReference("seq_region").getRefId();
            Item seq = getSeqItem(refId, false, srcItem);

            if (srcItemIsChild) {
                addReferencedItem(tgtItem, location, "objects", true, "subject", false);
                location.addReference(new Reference("object", seq.getIdentifier()));
            } else {
                addReferencedItem(tgtItem, location, "subjects", true, "object", false);
                location.addReference(new Reference("subject", seq.getIdentifier()));
            }
        } else {

            LOG.warn("No seq_region found for:" + srcItem.getClassName()
                    + " id:" + srcItem.getIdentifier());
        }

        return location;
    }

    /**
     * @param srcItem ensembl:marker
     * @param tgtItem flymine:Marker
     * @param srcNs   source namespace
     * @return set of locations
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Set createLocations(Item srcItem, Item tgtItem, String srcNs)
            throws ObjectStoreException {
        Set result = new HashSet();
        Set constraints = new HashSet();
        String value = srcItem.getIdentifier();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                srcNs + "marker_feature", false));
        constraints.add(new FieldNameAndValue("marker", value, true));
        Item location;
        for (Iterator i = srcItemReader.getItemsByDescription(constraints).iterator();
             i.hasNext();) {
            Item feature = ItemHelper.convert((org.intermine.model.fulldata.Item) i.next());

            location = createLocation(feature, tgtItem, true);
            location.addAttribute(new Attribute("strand", "0"));
            result.add(location);
        }
        return result;
    }

    /**
     * @param srcItem ensembl:marker
     * @param tgtItem flymine:Marker
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected void setNameAttribute(Item srcItem, Item tgtItem) throws ObjectStoreException {
        if (srcItem.hasReference("display_marker_synonym")) {

            String markerSynRefId = srcItem.getReference("display_marker_synonym").getRefId();

            org.intermine.model.fulldata.Item markerSyn = srcItemReader.getItemById(markerSynRefId);

            if (markerSyn != null) {

                if (markerSyn.getClassName().equals(srcNs + "marker_synonym")) {

                    Item synonym = ItemHelper.convert(markerSyn);
                    if (synonym.hasAttribute("name")) {
                        String name = synonym.getAttribute("name").getValue();
                        tgtItem.addAttribute(new Attribute("name", name));
                    }
                } else {
                    LOG.warn("Found a " + markerSyn.getClassName()
                            + " while looking for a marker_synonym");
                }
            } else {
                LOG.warn("setNameAttribute() failed to find marker_synonym:" + markerSynRefId);
            }
        }
    }

    /**
     * @param results the current collection of items to be stored.
     * @param srcItem = assembly
     * @return location item which reflects the relations between chromosome and contig,
     *         supercontig and contig, clone and contig
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Item createAssemblyLocation(Collection results, Item srcItem)
            throws ObjectStoreException {
        int start, end, asmStart, cmpStart, cmpEnd; //asmEnd,
        int contigLength, bioEntityLength, length;
        String ori, contigId, bioEntityId;

        contigId = srcItem.getReference("cmp_seq_region").getRefId();
        bioEntityId = srcItem.getReference("asm_seq_region").getRefId();
        Item contig = ItemHelper.convert(srcItemReader.getItemById(contigId));
        Item bioEntity = ItemHelper.convert(srcItemReader.getItemById(bioEntityId));

        contigLength = Integer.parseInt(contig.getAttribute("length").getValue());
        bioEntityLength = Integer.parseInt(bioEntity.getAttribute("length").getValue());
        asmStart = Integer.parseInt(srcItem.getAttribute("asm_start").getValue());
        cmpStart = Integer.parseInt(srcItem.getAttribute("cmp_start").getValue());
        //asmEnd = Integer.parseInt(srcItem.getAttribute("asm_end").getValue());
        cmpEnd = Integer.parseInt(srcItem.getAttribute("cmp_end").getValue());
        ori = srcItem.getAttribute("ori").getValue();

        //some occasions in ensembl, e.g. contig AC087365.3.1.104495 ||AC144832.1.1.45226
        //Chromosome, Supercontig have shorter length than contig
        //need to truncate the longer part
        if (contigLength < bioEntityLength) {
            length = contigLength;
        } else {
            length = bioEntityLength;
        }
        if (ori.equals("1")) {
            start = asmStart - cmpStart + 1;
            end = start + length - 1;
        } else {
            if (cmpEnd == length) {
                start = asmStart;
                end = start + length - 1;
            } else {
                start = asmStart - (length - cmpEnd);
                end = start + length - 1;
            }
        }

        Item location = createItem(tgtNs + "Location", "");
        location.addAttribute(new Attribute("start", Integer.toString(start)));
        location.addAttribute(new Attribute("end", Integer.toString(end)));
        location.addAttribute(new Attribute("startIsPartial", "false"));
        location.addAttribute(new Attribute("endIsPartial", "false"));
        location.addAttribute(new Attribute("strand", srcItem.getAttribute("ori").getValue()));
        location.addReference(new Reference("subject",
                (getSeqItem(contigId, false, srcItem)).getIdentifier()));
        location.addReference(new Reference("object",
                (getSeqItem(bioEntityId, false, srcItem)).getIdentifier()));
        return location;
    }

    /**
     * @param refId = refId for the seq_region
     * @param findSeq true indicates that seqregion item is srcItem otherwise find it from refid
     * @param srcItem item.
     *
     * @return seq item it could be  chromosome, supercontig, clone or contig
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Item getSeqItem(String refId, boolean findSeq, Item srcItem)
        throws ObjectStoreException {
        Item seq = null;
        Item seqRegion = null;

        if (findSeq) {
            seqRegion = srcItem;
        } else {
            seqRegion = ItemHelper.convert(srcItemReader.getItemById(refId));
        }

        if (seqIdMap.containsKey(refId)) {
            seq = (Item) seqIdMap.get(refId);
        } else {
            String property = null;
            if (seqRegion.hasReference("coord_system")) {
                Item coord = ItemHelper.convert(srcItemReader.getItemById(
                        seqRegion.getReference("coord_system").getRefId()));

                if (coord.hasAttribute("name")) {
                    property = coord.getAttribute("name").getValue();
                }
            }
            if (property != null && !property.equals("")) {

                //Produces the classname/type of the item, i.e. Chromosome, Contig, Chunk etc etc
                String s = (property.substring(0, 1)).toUpperCase().concat(property.substring(1));
                seq = createItem(tgtNs + s, "");
                if (seqRegion.hasAttribute("name")) {
                    seq.addAttribute(new Attribute(IDENTIFIER,
                            seqRegion.getAttribute("name").getValue()));
                }
                if (seqRegion.hasAttribute("length")) {
                    seq.addAttribute(new Attribute("length",
                            seqRegion.getAttribute("length").getValue()));
                }
                addReferencedItem(seq, config.getEnsemblDataSet(), "evidence", true, "", false);
                seqIdMap.put(refId, seq);

                if (config.doStoreDna()) {

                    Item dna = getItemViaItemPath(seqRegion, SEQ_REGION_VIA_DNA, srcItemReader);
                    if (dna != null) {
                        seq.setReference("sequence", dna);
                    }
                }
            }
        }


        //If we need to create a Synonym and one hasn't been made for the seq item yet...

        if (!itemId2SynMap.containsKey(
                seq != null ? seq.getIdentifier() : "NO_ID_HERE!")) {

            if (seq != null && seq.hasAttribute(IDENTIFIER)) {

                Item seqSyn = createSynonym(
                        seq.getIdentifier(),
                        IDENTIFIER,
                        seq.getAttribute(IDENTIFIER).getValue(),
                        config.getEnsemblDataSrcRef());

                itemId2SynMap.put(seq.getIdentifier(), seqSyn);

            }
        }

        return seq;
    }

    /**
     * Create an AnalysisResult pointed to by tgtItem evidence reference.  Move srcItem
     * analysis reference and score to new AnalysisResult.
     *
     * @param srcItem item in src namespace to move fields from
     * @param tgtItem item that will reference AnalysisResult
     * @return new AnalysisResult item
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Item createAnalysisResult(Item srcItem, Item tgtItem)
            throws ObjectStoreException {
        Item result = createItem(tgtNs + "ComputationalResult", "");
        if (srcItem.hasReference("analysis")) {
            moveField(srcItem, result, "analysis", "analysis");
        }
        if (srcItem.hasAttribute("score")) {
            moveField(srcItem, result, "score", "score");
        }
        result.addReference(config.getEnsemblDataSetRef());
        ReferenceList evidence = new ReferenceList("evidence", Arrays.asList(new String[]
        {result.getIdentifier(), config.getEnsemblDataSet().getIdentifier()}));
        tgtItem.addCollection(evidence);
        return result;
    }

    /**
     * Create comment class referenced by Gene item if there is a description field
     * in gene
     *
     * @param srcItem gene
     * @param tgtItem gene
     * @return new comment item
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Item createComment(Item srcItem, Item tgtItem)
            throws ObjectStoreException {
        Item comment = null;
        if (srcItem.hasAttribute("description")) {
            comment = createItem(tgtNs + "Comment", "");
            moveField(srcItem, comment, "description", "text");
            tgtItem.addReference(new Reference("comment", comment.getIdentifier()));
        }

        return comment;
    }

    /**
     * Create a simpleFeature item depends on the logic_name attribute in analysis
     * will become TRNA, or CpGIsland
     *
     * @param srcItem ensembl: simple_feature
     * @return new simpleFeature item
     * @throws org.intermine.objectstore.ObjectStoreException
     *          when anything goes wrong.
     */
    protected Item createSimpleFeature(Item srcItem) throws ObjectStoreException {
        Item simpleFeature = null;
        boolean createSynonym = true;
        String name = null;
        String idPrefix = null;
        if (srcItem.hasReference("analysis")) {

            Item analysis = ItemHelper.convert(
                 srcItemReader.getItemById(srcItem.getReference("analysis").getRefId()));
            if (analysis.hasAttribute("logic_name")) {

                name = analysis.getAttribute("logic_name").getValue();
                if (name.equals("tRNAscan")) {
                    simpleFeature = createItem(tgtNs + "TRNA", "");
                    idPrefix = "TRNA";
                } else if (name.equals("CpG")) {
                    simpleFeature = createItem(tgtNs + "CpGIsland", "");
                    idPrefix = "CpGIsland";
                } else if (name.equals("Eponine")) {
                    simpleFeature = createItem(tgtNs + "TranscriptionStartSite", "");
                    idPrefix = "TranscriptionStartSite";
                } else if (name.equals("FirstEF")) {
                    //5 primer exon and promoter including coding and noncoding
                    createSynonym = false;
                }
            }
        }

        if (createSynonym) {
                StringBuffer newIdBuff = new StringBuffer();
                newIdBuff.append(idPrefix).append("_");

                Item seqRegItem = ItemHelper.convert(srcItemReader.getItemById(
                     srcItem.getReference("seq_region").getRefId()));

                newIdBuff.append(seqRegItem.getAttribute("name").getValue()).append(":");
                newIdBuff.append(srcItem.getAttribute("seq_region_start").getValue()).append("..");
                newIdBuff.append(srcItem.getAttribute("seq_region_end").getValue());

                simpleFeature.addReference(config.getOrganismRef());
                simpleFeature.addAttribute(new Attribute(IDENTIFIER, newIdBuff.toString()));
                addReferencedItem(simpleFeature,
                        config.getEnsemblDataSet(), "evidence", true, "", false);

                Item sfSyn = createSynonym(
                            simpleFeature.getIdentifier(),
                            IDENTIFIER,
                            newIdBuff.toString(),
                            config.getEnsemblDataSrcRef());

                itemId2SynMap.put(simpleFeature.getIdentifier(), sfSyn);
        }
        return simpleFeature;

    }
    /**
     * Creates a synonym for a gene/protein product.
     * <p/>
     * This method is for 1-M
     *
     * @param refItem - the item that the synonym needs to reference.
     * @param type    - i.e. accession or symbol
     * @param value   - the value of the type provided
     * @param dsRef   - which data source is this synonym from
     * @see org.intermine.dataconversion.DataTranslator#addReferencedItem
     */
    private Item createProductSynonym(Item refItem,
                                      String type,
                                      String value,
                                      Reference dsRef) {

        Item synonym = createSynonym(refItem.getIdentifier(), type, value, dsRef);

        addReferencedItem(refItem, synonym, "synonyms", true, "subject", false);

        return synonym;
    }

    /**
     * Find stable_id for various ensembl type
     *
     * @param ensemblType could be gene, exon, transcript or translation
     *                    it should be part of name before _stable_id
     * @param identifier  srcItem identifier, srcItem could be gene, exon, transcript/translation
     * @param srcNs       namespace of source model
     * @return a set of Synonyms
     * @throws org.intermine.objectstore.ObjectStoreException
     *          if problem retrieving items
     */
    private Item getStableId(String ensemblType, String identifier, String srcNs) throws
            ObjectStoreException {
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                srcNs + ensemblType + "_stable_id", false));
        constraints.add(new FieldNameAndValue(ensemblType, identifier, true));
        Iterator stableIds = srcItemReader.getItemsByDescription(constraints).iterator();

        if (stableIds.hasNext()) {
            return ItemHelper.convert((org.intermine.model.fulldata.Item) stableIds.next());
        } else {
            StringBuffer bob = new StringBuffer();
            bob.append("getStableId unable to find a stableId for:");
            bob.append(ensemblType);
            bob.append("__");
            bob.append(identifier);
            bob.append("__");
            bob.append(srcNs);

            return null;
        }
    }

    /**
     * Create synonym item
     *
     * @param subjectId = synonym reference to subject
     * @param type      = synonym type
     * @param value     = synonym value
     * @param ref       = synonym reference to source
     * @return synonym item
     */
    private Item createSynonym(String subjectId, String type, String value, Reference ref) {
        Item synonym = createItem(tgtNs + "Synonym", "");
        synonym.addReference(new Reference("subject", subjectId));
        synonym.addAttribute(new Attribute("type", type));
        synonym.addAttribute(new Attribute("value", value));
        synonym.addReference(ref);
        return synonym;
    }

    /**
     * Create synonym item for marker
     *
     * @param srcItem = marker_synonym
     *                marker_synonym in ensembl may have same marker_id, source and name
     *                but with different marker_synonym_id,
     *                check before create synonym
     * @return synonym item
     */
    private Item getMarkerSynonym(Item srcItem) {
        Item synonym;
        String subjectId = srcItem.getReference("marker").getRefId();
        Set synonymSet;
        synonymSet = (HashSet) markerSynonymMap.get(subjectId);

        String value = srcItem.getAttribute("name").getValue();
        Reference ref;
        //TODO: NO HARDCODING!!!!!
        if (srcItem.hasAttribute("source")) {
            String source = srcItem.getAttribute("source").getValue();
            if (source.equalsIgnoreCase("genbank")) {
                ref = config.getDataSrcRefByDataSrcName("genbank");
            } else if (source.equalsIgnoreCase("gdb")) {
                ref = config.getDataSrcRefByDataSrcName("gdb");
            } else if (source.equalsIgnoreCase("unists")) {
                ref = config.getDataSrcRefByDataSrcName("unists");
            } else {
                ref = config.getEnsemblDataSrcRef();
            }
        } else {
            ref = config.getEnsemblDataSrcRef();
        }

        int createItem = 1;
        int flag;
        if (synonymSet == null) {
            synonym = createSynonym(subjectId, IDENTIFIER, value, ref);
            synonymSet = new HashSet(Arrays.asList(new Object[]{synonym}));
            markerSynonymMap.put(subjectId, synonymSet);
        } else {
            Iterator i = synonymSet.iterator();
            while (i.hasNext()) {
                Item item = (Item) i.next();
                if (item.getReference("source").getRefId().equals(ref.getRefId())
                        && item.getAttribute("value").getValue().equalsIgnoreCase(value)) {
                    flag = 0;
                } else {
                    flag = 1;
                }
                createItem = createItem * flag;
            }

            if (createItem == 1) {
                synonym = createSynonym(subjectId, IDENTIFIER, value, ref);
                synonymSet.add(synonym);
                markerSynonymMap.put(subjectId, synonymSet);
            } else {
                synonym = null;
            }
        }
        return synonym;
    }

    /**
     * Takes a file name to the ensembl properties file and attempts to load it...
     * */
    private Properties getEnsemblProperties(String propsFileName) throws ObjectStoreException {
        try {
            Properties ensemblProps = new Properties();
            FileInputStream fis = new FileInputStream(propsFileName);
            ensemblProps.load(fis);
            return ensemblProps;
        } catch (FileNotFoundException e) {
            throw new ObjectStoreException("No ensembl props file located at:"
                    + System.getProperty("user.dir") + propsFileName, e);
        } catch (IOException e) {
            throw new ObjectStoreException("Problem reading ensembl props:" + propsFileName, e);
        }
    }

    /**
     * @see org.intermine.task.DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        String identifier = ObjectStoreItemPathFollowingImpl.IDENTIFIER;
        String classname = ObjectStoreItemPathFollowingImpl.CLASSNAME;

        //karyotype
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("karyotype.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        ItemPrefetchDescriptor desc1
                = new ItemPrefetchDescriptor("karyotype.seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc1);
        paths.put("http://www.intermine.org/model/ensembl#karyotype",
                Collections.singleton(desc));

        //exon
        Set descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("exon <- exon_transcript.exon");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "exon"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#exon_transcript", false));
        descSet.add(desc);
        desc1 = new ItemPrefetchDescriptor("(<- exon_transcript.exon).transcript");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("transcript", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("exon <- exon_stable_id.exon");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "exon"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#exon_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("exon.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor("exon.seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#exon", descSet);

        //exon_transcript
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("exon_transcript.exon");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("exon", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("exon_transcript.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("transcript", identifier));
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#exon_transcript",
                descSet);

        //exon_stable_id
        desc = new ItemPrefetchDescriptor("exon_stable_id.exon");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("exon", identifier));
        paths.put("http://www.intermine.org/model/ensembl#exon_stable_id",
                Collections.singleton(desc));
        //gene
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("gene <- gene_stable_id.gene");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "gene"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#gene_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("gene.analysis");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("analysis", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("gene.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor("gene.seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("gene.display_xref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("display_xref", identifier));
        desc1 = new ItemPrefetchDescriptor("gene.display_xref.external_db");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("external_db", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#gene", descSet);

        //gene_stable_id
        desc = new ItemPrefetchDescriptor("gene_stable_id.gene");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("gene", identifier));
        paths.put("http://www.intermine.org/model/ensembl#gene_stable_id",
                Collections.singleton(desc));

        //transcript
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("transcript <- translation.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "transcript"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#translation", false));
        desc1 = new ItemPrefetchDescriptor("(transcript <- translation.transcript)"
                 + " <- translation_stable_id");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "translation"));
        desc1.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#translation_stable_id", false));
        desc.addPath(desc1);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("transcript <- transcript_stable_id.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "transcript"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#transcript_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("transcript.gene");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("gene", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("transcript.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor("transcript.seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#transcript", descSet);

         //transcript_stable_id
        desc = new ItemPrefetchDescriptor("transcript_stable_id.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("transcript", identifier));
        paths.put("http://www.intermine.org/model/ensembl#transcript_stable_id",
                Collections.singleton(desc));

        //translation
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("translation <- translation_stable_id.translation");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "translation"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#translation_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("translation.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("transcript", identifier));
        descSet.add(desc);
        desc1 = new ItemPrefetchDescriptor("translation.transcript.display_xref");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("display_xref", identifier));
        ItemPrefetchDescriptor desc2 =
                new ItemPrefetchDescriptor("translation.transcript.display_xref.external_db");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("external_db", identifier));
        desc1.addPath(desc2);
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#translation", descSet);

        //translation_stable_id
        desc = new ItemPrefetchDescriptor("translation_stable_id.translation");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("translation", identifier));
        paths.put("http://www.intermine.org/model/ensembl#translation_stable_id",
                Collections.singleton(desc));

        //repeat_feature
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("repeat_feature.analysis");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("analysis", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("repeat_feature.repeat_consensus");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("repeat_consensus", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("repeat_feature.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor("repeat_feature.seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#repeat_feature", descSet);

        //marker
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("marker.display_marker_synonym");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("display_marker_synonym", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("marker <- marker_synonym.marker");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "marker"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#marker_synonym", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("marker <- marker_feature.marker");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "marker"));
        desc.addConstraint(new FieldNameAndValue(classname,
                "http://www.intermine.org/model/ensembl#marker_feature", false));
        desc1 = new ItemPrefetchDescriptor("(<- marker_feature.marker).seq_region");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc2 = new ItemPrefetchDescriptor("(<- marker_feature.marker).seq_region.coord_system");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc1.addPath(desc2);
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#marker",
                Collections.singleton(desc));

        //assembly
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("assembly.asm_seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("asm_seq_region", identifier));
        desc2 = new ItemPrefetchDescriptor("assembly.asm_seq_region.coord_system");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc2);
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("assembly.cmp_seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("cmp_seq_region", identifier));
        desc2 = new ItemPrefetchDescriptor("assembly.cmp_seq_region.coord_system");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc2);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#assembly", descSet);

        //seq_region
        desc = new ItemPrefetchDescriptor("seq_region.coord_system");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        paths.put("http://www.intermine.org/model/ensembl#seq_region",
                Collections.singleton(desc));

        //simple_feature
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("simple_feature.analysis");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("analysis", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("simple_feature.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor("simple_feature.seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.intermine.org/model/ensembl#simple_feature", descSet);

        return paths;
    }

    /**
     * Utility class to keep the contents of the ensembl_config file in for reference while the
     * translator is running...
     * */
    class EnsemblConfig
    {

        private String orgAbbrev;

        private Item organism = null;
        private Reference organismRef = null;

        private boolean useXrefDbsForGeneOrganismDbId;
        private boolean useXrefDbsForGeneSymbol;
        private boolean storeDna;
        private boolean createAnalysisResult;

        private EnsemblPropsUtil propsUtil;

        private Map dataSourceName2ItemMap;
        private Map dataSourceName2RefMap;

        private Item ensemblDataSrc;
        private Reference ensemblDataSrcRef;

        private Item ensemblDataSet;
        private Reference ensemblDataSetRef;

        private Set xrefDataSourceNames;
        private String geneXrefDbName = null;

        private Set xrefSymbolDataSourceNames;

        /**
         * Constructor.
         *
         * @param ensemblProps a represesntation of the ensembl_config file's contents.
         * @param orgAbbrev A suitably short acronym to identify which ogransim to process.
         * */
        EnsemblConfig(Properties ensemblProps, String orgAbbrev) {

            if (orgAbbrev == null || "".equals(orgAbbrev)) {
                throw new RuntimeException(
                        "EnsemblConfig: must have the organism abbreviation set!");
            }

            this.orgAbbrev = orgAbbrev;

            if (ensemblProps == null) {
                throw new RuntimeException(
                        "EnsemblConfig: can't find any ensembl_config.properties!");
            }

            propsUtil = new EnsemblPropsUtil(ensemblProps);
            Properties organismProps = propsUtil.stripStart(orgAbbrev, ensemblProps);

            organism = createItem(tgtNs + "Organism", "");
            organism.addAttribute(new Attribute("abbreviation", orgAbbrev));
            organism.addAttribute(new Attribute("name",
                    organismProps.getProperty("organism.name")));
            organism.addAttribute(new Attribute("taxonId",
                    organismProps.getProperty("organism.taxonId")));
            organismRef = new Reference("organism", organism.getIdentifier());

            //This boolean indicates that we want to use configurable identifier fields...
            useXrefDbsForGeneOrganismDbId = Boolean.valueOf(
                    organismProps.getProperty("flag.useXrefDbsForGeneOrganismDbId")).booleanValue();
            useXrefDbsForGeneSymbol = Boolean.valueOf(
                    organismProps.getProperty("flag.useXrefDbsForGeneSymbol")).booleanValue();
            createAnalysisResult = Boolean.valueOf(
                    organismProps.getProperty("flag.createAnalysisResult")).booleanValue();
            storeDna = Boolean.valueOf(
                    organismProps.getProperty("flag.storeDna")).booleanValue();

            //Load up all the data source names that we will allow xref synonyms to be set for.
            xrefDataSourceNames = new HashSet(propsUtil.getPropertiesStartingWith(
                    orgAbbrev + ".datasource.xref.synonym").values());

            dataSourceName2ItemMap = new HashMap();
            dataSourceName2RefMap = new HashMap();

            initDataSourceAndDataSourceRefMaps();
            initEnsemblDataSrc();
            initEnsemblDataSet();

            //try and load the props if we need to use custom gene identifiers - i.e. Hugo for Human
            if (useXrefDbsForGeneOrganismDbId) {

                geneXrefDbName = organismProps.getProperty("gene.identifier.xref.dataSourceName");

                //if it aint set - complain!
                if (geneXrefDbName == null) {
                    throw new RuntimeException(
                            "EnsemblConfig: gene.identifier.xref.dataSourceName property not set!");
                }

                if (!dataSourceName2ItemMap.containsKey(geneXrefDbName.toLowerCase())) {
                    throw new RuntimeException(
                            "EnsemblConfig(1): gene.identifier.xref.dataSourceName:"
                            + geneXrefDbName + " has no matching common.datasource");
                }
            }

            //Load up all the data source names that we will allow xref synonyms to be set for.
            xrefSymbolDataSourceNames = new HashSet(propsUtil.getPropertiesStartingWith(
                    orgAbbrev + ".datasource.xref.symbol.synonym").values());
            //Better check there's a matching data source for each of these!
            for (Iterator symbolIt = xrefSymbolDataSourceNames.iterator(); symbolIt.hasNext(); ) {

                Object nextSymbol = symbolIt.next();

                if (!dataSourceName2ItemMap.containsKey(nextSymbol.toString().toLowerCase())) {
                    throw new RuntimeException(
                            "EnsemblConfig(2): datasource.xref.synonym.symbol:"
                            + nextSymbol.toString() + " has no matching common.datasource");
                }
            }

        }


        private void initDataSourceAndDataSourceRefMaps() {

            //Get the names of all the available datasources
            Properties dsNames = propsUtil.getPropertiesStartingWith("common.datasource.name");

            for (Iterator dsnIt = dsNames.values().iterator(); dsnIt.hasNext(); ) {

                String dsName = dsnIt.next().toString().toLowerCase();
                Properties dsnNextProps = propsUtil.getPropertiesStartingWith(
                        "common.datasource." + dsName);

                Item nextDataSource = createItem(tgtNs + "DataSource", "");

                nextDataSource.addAttribute(new Attribute("name", dsName));

                nextDataSource.addAttribute(new Attribute("url",
                        dsnNextProps.getProperty("common.datasource."
                        + dsName.toLowerCase() + ".url")));

                nextDataSource.addAttribute(new Attribute("description",
                        dsnNextProps.getProperty("common.datasource."
                        + dsName.toLowerCase() + ".description")));

                dataSourceName2ItemMap.put(dsName.toLowerCase(), nextDataSource);
                dataSourceName2RefMap.put(dsName.toLowerCase(),
                        new Reference("source", nextDataSource.getIdentifier()));
            }
        }

        private void initEnsemblDataSrc() {

            String ensembl = "ensembl";

            if (hasDataSourceNamed(ensembl)) {
                ensemblDataSrc      = getDataSrcByName(ensembl);
                ensemblDataSrcRef   = getDataSrcRefByDataSrcName(ensembl);
            } else {
                throw new RuntimeException("!!! Ensembl DataSrc Not Found !!!");
            }
        }

        private void initEnsemblDataSet() {

            String propStem = orgAbbrev + ".ensembl.dataset";

            Properties props = propsUtil.getPropertiesStartingWith(propStem);

            ensemblDataSet = createItem(tgtNs + "DataSet", "");
            ensemblDataSet.addAttribute(
                    new Attribute("title", props.getProperty(propStem + ".title")));

            String dsName = props.getProperty(propStem + ".dataSourceName");

            Item dataSrc = getDataSrcByName(dsName);
            ensemblDataSet.addReference(new Reference("dataSource", dataSrc.getIdentifier()));
            ensemblDataSetRef = new Reference("source", ensemblDataSet.getIdentifier());
        }

        //------------------------------------------------------------------------------------------


        /**
         * @return An abbreviated string representing the current organism.
         */
        String getOrgAbbrev() {
            return orgAbbrev;
        }

        /**
         * @return The current Organism Item
         */
        Item getOrganism() {
            return organism;
        }


        /**
         * @return A reference to the current organism
         */
        Reference getOrganismRef() {
            return organismRef;
        }

        /**
         * @return Do we want to use any external db ids - i.e. SwissProt/Uniprot etc for Genes?
         * */
        boolean useXrefDbsForGeneOrganismDbId() {
            return useXrefDbsForGeneOrganismDbId;
        }

        /**
         * Return true if we should put external ids into the symbol field (eg. for Anopheles)
         */
        public boolean useXrefDbsForGeneSymbol() {
            return useXrefDbsForGeneSymbol;
        }

        boolean createAnalysisResult() {
            return createAnalysisResult;
        }

        /**
         * @return Do we want to get the DNA sequences now or later?
         * */
        boolean doStoreDna() {
            return storeDna;
        }

        /**
         * @return The name of the datasource where we want to get any xref dbid's from
         * */
        String getGeneXrefDbName() {
            return geneXrefDbName;
        }

        /**
         * @return Get's the Ensembl Data Source Item
         * */
        Item getEnsemblDataSrc() {
            return ensemblDataSrc;
        }

        /**
         * @return Get's a reference to the Ensembl Data Source Item
         * */
        Reference getEnsemblDataSrcRef() {
            return ensemblDataSrcRef;
        }

        /**
         * @return Gets the current Ensembl Data Set being processed.
         * */
        Item getEnsemblDataSet() {
            return ensemblDataSet;
        }

        /**
         * @return Gets a reference to the current Ensembl Data Set being processed.
         * */
        Reference getEnsemblDataSetRef() {
            return ensemblDataSetRef;
        }

        /**
         * @param xrefDataSourceName a data source name to check
         *
         * @return have we seen this xref data source name before
         * */
        boolean containsXrefDataSourceNamed(String xrefDataSourceName) {
            return xrefDataSourceNames.contains(xrefDataSourceName);
        }

        /**
         * @param xrefSymbolDataSourceName a data source name that we represents a symbol
         *
         * @return have we seen this data source name before
         * */
        boolean containsXrefSymbolDataSourceNamed(String xrefSymbolDataSourceName) {
            return xrefSymbolDataSourceNames.contains(xrefSymbolDataSourceName);
        }

        /**
         * @param dataSrcName a data source name to check for
         *
         * @return have we seen this data source name before
         * */
        boolean hasDataSourceNamed(String dataSrcName) {
            return dataSourceName2ItemMap.containsKey(dataSrcName.toLowerCase());
        }

        /**
         * @param dataSrcName A named data source of interest
         *
         * @return a data source item
         *
         * @throws RuntimeException if we can't find the datasource - check the config file
         * */
        Item getDataSrcByName(String dataSrcName) {

            if (hasDataSourceNamed(dataSrcName.toLowerCase())) {
                    return (Item) dataSourceName2ItemMap.get(dataSrcName.toLowerCase());
            }

            throw new RuntimeException("Can't find a data source named:" + dataSrcName);
        }

        /**
         * @param dataSrcName name of a data source to look for the reference of
         *
         * @return a boolean indicating if we have found the reference or not
         * */
        boolean hasDataSrcRefForDataSrcNamed(String dataSrcName) {
            return dataSourceName2RefMap.containsKey(dataSrcName.toLowerCase());
        }

        /**
         * @param dataSrcName name of a data source to look for the reference of
         *
         * @return A reference to the datasource
         *
         * @throws RuntimeException if we can't find the reference
         * */
        Reference getDataSrcRefByDataSrcName(String dataSrcName) throws RuntimeException {
            if (hasDataSrcRefForDataSrcNamed(dataSrcName.toLowerCase())) {
                return (Reference) dataSourceName2RefMap.get(dataSrcName.toLowerCase());
            }

            throw new RuntimeException("Can't find a reference for a data source named:"
                    + dataSrcName);
        }

        /**
         * @return provides an Iterator of all the known data source items.
         * */
        Iterator getDataSrcItemIterator() {
            return dataSourceName2ItemMap.values().iterator();
        }
    }

}
