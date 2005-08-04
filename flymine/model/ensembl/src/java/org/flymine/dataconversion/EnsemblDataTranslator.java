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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Properties;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.FieldNameAndValue;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
import org.intermine.metadata.Model;
import org.intermine.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * Convert Ensembl data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class EnsemblDataTranslator extends DataTranslator
{
    protected static final Logger LOG = Logger.getLogger(EnsemblDataTranslator.class);

    private Item ensemblDb;
    private Reference ensemblRef;
    private Item emblDb;
    private Reference emblRef;
    private Item tremblDb;
    private Reference tremblRef;
    private Item swissprotDb;
    private Reference swissprotRef;
    private Item flybaseDb;
    private Reference flybaseRef;
    private Map supercontigs = new HashMap();
    private Map scLocs = new HashMap();
    private Map exonLocs = new HashMap();
    private Map exons = new HashMap();
    private Map flybaseIds = new HashMap();
    private String orgAbbrev;
    private Item organism;
    private Reference organismRef;
    private Map proteins = new HashMap();
    private Map proteinIds = new HashMap();
    private Set proteinSynonyms = new HashSet();

    /**
     * @see DataTranslator#DataTranslator
     */
    public EnsemblDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                                 Model tgtModel, String orgAbbrev) {
        super(srcItemReader, mapping, srcModel, tgtModel);
        this.orgAbbrev = orgAbbrev;

        organism = createItem("Organism");
        organism.addAttribute(new Attribute("abbreviation", orgAbbrev));
        organismRef = new Reference("organism", organism.getIdentifier());

        ensemblDb = createItem("Database");
        ensemblDb.addAttribute(new Attribute("title", "ensembl"));
        ensemblRef = new Reference("source", ensemblDb.getIdentifier());

        emblDb = createItem("Database");
        emblDb.addAttribute(new Attribute("title", "embl"));
        emblRef = new Reference("source", emblDb.getIdentifier());

        tremblDb = createItem("Database");
        tremblDb.addAttribute(new Attribute("title", "TrEMBL"));
        tremblRef = new Reference("source", tremblDb.getIdentifier());

        swissprotDb = createItem("Database");
        swissprotDb.addAttribute(new Attribute("title", "Swiss-Prot"));
        swissprotRef = new Reference("source", swissprotDb.getIdentifier());
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {
        tgtItemWriter.store(ItemHelper.convert(organism));
        tgtItemWriter.store(ItemHelper.convert(ensemblDb));
        tgtItemWriter.store(ItemHelper.convert(emblDb));
        tgtItemWriter.store(ItemHelper.convert(tremblDb));
        tgtItemWriter.store(ItemHelper.convert(swissprotDb));

        super.translate(tgtItemWriter);

        for (Iterator i = createSuperContigs().iterator(); i.hasNext();) {
            Item item = (Item) i.next();
            if (item.getClassName().equals(tgtNs + "Supercontig")) {
                Item synonym = createSynonym(item.getIdentifier(), "identifier",
                        item.getAttribute("identifier").getValue(), ensemblRef);
                addReferencedItem(item, synonym, "synonyms", true, "subject", false);
                tgtItemWriter.store(ItemHelper.convert(synonym));
            }
            tgtItemWriter.store(ItemHelper.convert(item));
        }
        for (Iterator i = exons.values().iterator(); i.hasNext();) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        for (Iterator i = exonLocs.values().iterator(); i.hasNext();) {
            for (Iterator j = ((Collection) i.next()).iterator(); j.hasNext();) {
                tgtItemWriter.store(ItemHelper.convert((Item) j.next()));
            }
        }
        for (Iterator i = proteins.values().iterator(); i.hasNext();) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        for (Iterator i = proteinSynonyms.iterator(); i.hasNext();) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        if (flybaseDb != null) {
            tgtItemWriter.store(ItemHelper.convert(flybaseDb));
        }
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        Collection result = new HashSet();
        String srcNs = XmlUtil.getNamespaceFromURI(srcItem.getClassName());
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
                if ("karyotype".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    if (tgtItem.hasAttribute("identifier")) {
                        Item synonym = createSynonym(tgtItem.getIdentifier(), "name",
                                tgtItem.getAttribute("identifier").getValue(), ensemblRef);
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        result.add(synonym);
                    }
                    Item location = createLocation(srcItem, tgtItem, "chromosome", "chr", true);
                    location.addAttribute(new Attribute("strand", "0"));
                    result.add(location);
                } else if ("exon".equals(className)) {
                    tgtItem.addReference(organismRef);
                    Item stableId = getStableId("exon", srcItem.getIdentifier(), srcNs);
                    if (stableId != null) {
                        moveField(stableId, tgtItem, "stable_id", "identifier");
                    }
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    // more than one item representing same exon -> store up in map
                    Item location = createLocation(srcItem, tgtItem, "contig", "contig", true);
                    if (srcItem.hasAttribute("nonUniqueId")) {
                        storeTgtItem = false;
                        processExon(srcItem, tgtItem, location);
                    } else {
                        addToLocations(srcItem.getIdentifier(), location);
                    }
                } else if ("simple_feature".equals(className)) {
                    tgtItem.addReference(organismRef);
                    tgtItem.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                } else if ("repeat_feature".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                    promoteField(tgtItem, srcItem, "consensus", "repeat_consensus",
                                "repeat_consensus");
                    promoteField(tgtItem, srcItem, "type", "repeat_consensus", "repeat_class");
                    promoteField(tgtItem, srcItem, "identifier", "repeat_consensus", "repeat_name");
                } else if ("gene".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    // gene name should be its stable id (or identifier if none)
                    Item stableId = null;
                    stableId = getStableId("gene", srcItem.getIdentifier(), srcNs);
                    if (stableId != null) {
                         moveField(stableId, tgtItem, "stable_id", "identifier");
                    }
                    if (!tgtItem.hasAttribute("identifier")) {
                        tgtItem.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
                    }
                    // display_xref is gene name (?)
                    //promoteField(tgtItem, srcItem, "symbol", "display_xref", "display_label");
                    result.addAll(setGeneSynonyms(srcItem, tgtItem, srcNs));
                    // if no organismDbId set to be same as identifier
                    if (!tgtItem.hasAttribute("organismDbId")) {
                        tgtItem.addAttribute(new Attribute("organismDbId",
                               tgtItem.getAttribute("identifier").getValue()));
                    }

                } else if ("contig".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    if (tgtItem.hasAttribute("identifier")) {
                        Item synonym = createSynonym(tgtItem.getIdentifier(), "identifier",
                                   tgtItem.getAttribute("identifier").getValue(), ensemblRef);
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        result.add(synonym);
                    }
                } else if ("transcript".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    // SimpleRelation between Gene and Transcript
                    result.add(createSimpleRelation(tgtItem.getReference("gene").getRefId(),
                                                    tgtItem.getIdentifier()));

                    // set transcript identifier to be ensembl stable id
                    if (!tgtItem.hasAttribute("identifier")) {
                        Item stableId = getStableId("transcript", srcItem.getIdentifier(), srcNs);
                        if (stableId != null) {
                            moveField(stableId, tgtItem, "stable_id", "identifier");
                        } else {
                            tgtItem.addAttribute(new Attribute("identifier",
                                                               srcItem.getIdentifier()));
                        }
                    }

                    Item translation = ItemHelper.convert(srcItemReader.getItemById(srcItem
                                                         .getReference("translation").getRefId()));
                    // Transcript.translation is set by mapping file - add reference to protein
                    // if no SwissProt or trembl accession found there will not be a protein
                    String proteinId = getChosenProteinId(translation.getIdentifier(), srcNs);

                    if (proteinId != null) {
                        tgtItem.addReference(new Reference("protein", proteinId));
                        result.add(createSimpleRelation(tgtItem.getIdentifier(),
                                                        proteinId));
                    }

                    // need to fetch translation to get identifier for CDS
                    // create CDS and reference from MRNA
                    Item cds = createItem(tgtNs + "CDS", "");
                    if (translation != null) {
                        Item stableId = getStableId("translation",
                                                    translation.getIdentifier(), srcNs);
                        cds.setAttribute("identifier",
                                         stableId.getAttribute("stable_id").getValue() + "_CDS");
                        cds.addToCollection("polypeptides", translation.getIdentifier());
                        result.add(createSimpleRelation(cds.getIdentifier(),
                                                        translation.getIdentifier()));
                    }
                    cds.addReference(organismRef);
                    addReferencedItem(cds, ensemblDb, "evidence", true, "", false);
                    Item synonym = createSynonym(tgtItem.getIdentifier(), "identifier",
                                                 cds.getAttribute("identifier").getValue(),
                                                 ensemblRef);
                    result.add(synonym);


                    if (proteinId != null) {
                        cds.setReference("protein", proteinId);
                    }
                    tgtItem.addToCollection("CDSs", cds);
                    result.add(createSimpleRelation(tgtItem.getIdentifier(),
                                                    cds.getIdentifier()));
                    result.add(cds);

                // stable_ids become syonyms, need ensembl Database as source
                } else if (className.endsWith("_stable_id")) {
                    tgtItem.addReference(ensemblRef);
                    tgtItem.addAttribute(new Attribute("type", "identifier"));
                } else if ("chromosome".equals(className)) {
                    tgtItem.addReference(organismRef);
                    addReferencedItem(tgtItem, ensemblDb, "evidence", true, "", false);
                    if (tgtItem.hasAttribute("identifier")) {
                        Item synonym = createSynonym(tgtItem.getIdentifier(), "name",
                                tgtItem.getAttribute("identifier").getValue(), ensemblRef);
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        result.add(synonym);
                    }
                } else if ("translation".equals(className)) {
                    // if protein can be created it will be put in proteins collection and stored
                    // at end of translating
                    Item protein = getProteinByPrimaryAccession(srcItem, srcNs);

                    // Transcript.translation is set by mapping file - add reference to protein
                    // if no SwissProt or trembl accession found there will not be a protein
                    if (protein != null) {
                        tgtItem.addReference(new Reference("protein", protein.getIdentifier()));
                        result.add(createSimpleRelation(tgtItem.getIdentifier(),
                                                        protein.getIdentifier()));
                    }

                    // set translation identifier to be ensembl stable id
                    Item stableId = getStableId("translation", srcItem.getIdentifier(), srcNs);
                    if (stableId != null) {
                        moveField(stableId, tgtItem, "stable_id", "identifier");
                    } else {
                        tgtItem.addAttribute(new Attribute("identifier",
                                                           srcItem.getIdentifier()));
                    }
                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        // assembly maps to null but want to create location on a supercontig
        } else if ("assembly".equals(className)) {
            Item sc = getSuperContig(srcItem.getAttribute("superctg_name").getValue(),
                                     srcItem.getReference("chromosome").getRefId(),
                                     Integer.parseInt(srcItem.getAttribute("chr_start").getValue()),
                                     Integer.parseInt(srcItem.getAttribute("chr_end").getValue()),
                                     srcItem.getAttribute("superctg_ori").getValue());

            // locate contig on supercontig - for drosophila and anopheles strans is always 1
            Item location = createLocation(srcItem, sc, "contig", "superctg", false);
            result.add(location);
        }
        return result;
    }

    /**
     * Get a reference to flybase
     * @return the reference
     */
    public Reference getFlybaseRef() {
        if (flybaseDb == null) {
            flybaseDb = createItem("Database");
            flybaseDb.addAttribute(new Attribute("title", "FlyBase"));
            flybaseRef = new Reference("source", flybaseDb.getIdentifier());
        }
        return flybaseRef;
    }


    private Item createSimpleRelation(String objectId, String subjectId) {
        Item sr = createItem("SimpleRelation");
        sr.setReference("object", objectId);
        sr.setReference("subject", subjectId);
        return sr;
    }

    /**
     * Translate a "located" Item into an Item and a location
     * @param srcItem the source Item
     * @param tgtItem the target Item (after translation)
     * @param idPrefix the id prefix for this class
     * @param locPrefix the start, end and strand prefix for this class
     * @param srcItemIsChild true if srcItem should be subject of Location
     * @return the location
     */
    protected Item createLocation(Item srcItem, Item tgtItem, String idPrefix, String locPrefix,
                                  boolean srcItemIsChild) {
        String namespace = XmlUtil.getNamespaceFromURI(tgtItem.getClassName());

        Item location = createItem(namespace + "Location", "");

        moveField(srcItem, location, locPrefix + "_start", "start");
        moveField(srcItem, location, locPrefix + "_end", "end");
        location.addAttribute(new Attribute("startIsPartial", "false"));
        location.addAttribute(new Attribute("endIsPartial", "false"));

        if (srcItem.hasAttribute(locPrefix + "_strand")) {
            moveField(srcItem, location, locPrefix + "_strand", "strand");
        }
        // if creating location between contig and supercontig strand will always be 1
        // for drosophila and anopheles
        if (idPrefix.equals("contig") && locPrefix.equals("superctg")) {
            location.setAttribute("strand", "1");
        }

        if (srcItem.hasAttribute("phase")) {
            moveField(srcItem, location, "phase", "phase");
        }
        if (srcItem.hasAttribute("end_phase")) {
            moveField(srcItem, location, "end_phase", "endPhase");
        }
        if (srcItemIsChild) {
            addReferencedItem(tgtItem, location, "objects", true, "subject", false);
            moveField(srcItem, location, idPrefix, "object");
        } else {
            addReferencedItem(tgtItem, location, "subjects", true, "object", false);
            moveField(srcItem, location, idPrefix, "subject");
        }
        return location;
    }

    /**
     * Create an AnalysisResult pointed to by tgtItem evidence reference.  Move srcItem
     * analysis reference and score to new AnalysisResult.
     * @param srcItem item in src namespace to move fields from
     * @param tgtItem item that will reference AnalysisResult
     * @return new AnalysisResult item
     */
    protected Item createAnalysisResult(Item srcItem, Item tgtItem) {
        Item result = createItem(tgtNs + "ComputationalResult", "");
        moveField(srcItem, result, "analysis", "analysis");
        moveField(srcItem, result, "score", "score");
        result.addReference(ensemblRef);
        ReferenceList evidence = new ReferenceList("evidence",
                                     Arrays.asList(new Object[] {result.getIdentifier(),
                                                   ensemblDb.getIdentifier()}));
        tgtItem.addCollection(evidence);
        return result;
    }

    private Item getSuperContig(String name, String chrId, int start, int end, String strand) {
        Item supercontig = (Item) supercontigs.get(name);
        if (supercontig == null) {
            supercontig = createItem(tgtNs + "Supercontig", "");
            Item chrLoc = createItem(tgtNs + "Location", "");
            chrLoc.addAttribute(new Attribute("start", "" + Integer.MAX_VALUE));
            chrLoc.addAttribute(new Attribute("end", "" + Integer.MIN_VALUE));
            chrLoc.addAttribute(new Attribute("startIsPartial", "false"));
            chrLoc.addAttribute(new Attribute("endIsPartial", "false"));
            chrLoc.addAttribute(new Attribute("strand", strand));
            chrLoc.addReference(new Reference("subject", supercontig.getIdentifier()));
            chrLoc.addReference(new Reference("object", chrId));

            supercontig.addAttribute(new Attribute("identifier", name));
            ReferenceList subjects = new ReferenceList();
            subjects.setName("subjects");
            supercontig.addCollection(subjects);
            supercontig.addCollection(new ReferenceList("objects",
                           new ArrayList(Collections.singletonList(chrLoc.getIdentifier()))));
            addReferencedItem(supercontig, ensemblDb, "evidence", true, "", false);
            supercontig.addReference(organismRef);
            supercontigs.put(name, supercontig);
            scLocs.put(name, chrLoc);
        }

        Item chrLoc = (Item) scLocs.get(name);
        if (Integer.parseInt(chrLoc.getAttribute("start").getValue()) > start) {
            chrLoc.getAttribute("start").setValue("" + start);
        }
        if (Integer.parseInt(chrLoc.getAttribute("end").getValue()) < end) {
            chrLoc.getAttribute("end").setValue("" + end);
        }

        return supercontig;
    }

    private Collection createSuperContigs() {
        Set results = new HashSet();
        Iterator i = supercontigs.values().iterator();
        while (i.hasNext()) {
            Item sc = (Item) i.next();
            results.add(sc);
            results.add((Item) scLocs.get(sc.getAttribute("identifier").getValue()));
        }
        return results;
    }

    private String getChosenProteinId(String id, String srcNs) throws ObjectStoreException {
        String chosenId = (String) proteinIds.get(id);
        if (chosenId == null) {
            Item translation = ItemHelper.convert(srcItemReader.getItemById(id));
            Item protein = getProteinByPrimaryAccession(translation, srcNs);
            if (protein != null) {
                chosenId = protein.getIdentifier();
                proteinIds.put(id, chosenId);
            }
        }
        return chosenId;
    }

    private Item getProteinByPrimaryAccession(Item translation, String srcNs)
        throws ObjectStoreException {
        Item protein = createItem(tgtNs + "Protein", "");

        Set synonyms = new HashSet();
        String value = translation.getIdentifier();
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + "object_xref", false));
        constraints.add(new FieldNameAndValue("ensembl", value, true));
        Iterator objectXrefs = srcItemReader.getItemsByDescription(constraints).iterator();
        // set specific ids and add synonyms

        String swissProtId = null;
        String tremblId = null;
        while (objectXrefs.hasNext()) {
            Item objectXref = ItemHelper.convert(
                                  (org.intermine.model.fulldata.Item) objectXrefs.next());
            Item xref = ItemHelper.convert(srcItemReader
                        .getItemById(objectXref.getReference("xref").getRefId()));

            String accession = null;
            String dbname = null;
            if (xref != null) {
                accession = xref.getAttribute("dbprimary_acc").getValue();
                Item externalDb = ItemHelper.convert(srcItemReader
                                  .getItemById(xref.getReference("external_db").getRefId()));
                if (externalDb != null) {
                    dbname =  externalDb.getAttribute("db_name").getValue();
                }
            }
            //LOG.error("processing: " + accession + ", " + dbname);

            if (accession != null && !accession.equals("")
                && dbname != null && !dbname.equals("")) {
                if (dbname.equals("SWISSPROT")) {
                    swissProtId = accession;
                    Item synonym = createSynonym(protein.getIdentifier(), "accession", accession,
                                                 swissprotRef);
                    addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                    synonyms.add(synonym);
                } else if (dbname.equals("SPTREMBL")) {
                    tremblId = accession;
                    Item synonym = createSynonym(protein.getIdentifier(), "accession", accession,
                                                 tremblRef);
                    addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                    synonyms.add(synonym);
                } else if (dbname.equals("protein_id") || dbname.equals("prediction_SPTREMBL")) {
                    Item synonym = createSynonym(protein.getIdentifier(), "identifier", accession,
                                                 emblRef);
                    addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                    synonyms.add(synonym);
                }
            }
        }

        // we have a set of synonyms, if we don't want to create a protein these will be discarded

        // we want to create a Protein only if there is Swiss-Prot or Trembl id
        // set of synonyms will be discarded if no protein created
        // TODO: sort out how we wish to model translations/proteins in genomic model
        String primaryAcc = null;
        if (swissProtId != null) {
            primaryAcc = swissProtId;
        } else if (tremblId != null) {
            primaryAcc = tremblId;
        }

        // try to find a protein with this accession, otherwise create if an accession
        Item chosenProtein = (Item) proteins.get(primaryAcc);
        if (chosenProtein == null && primaryAcc != null) {
            protein.addAttribute(new Attribute("primaryAccession", primaryAcc));
            addReferencedItem(protein, ensemblDb, "evidence", true, "", false);

            // set up additional references/collections
            protein.addReference(organismRef);
//             if (translation.hasReference("start_exon")) {
//                 protein.addReference(new Reference("startExon",
//                             translation.getReference("start_exon").getRefId()));
//             }
//             if (translation.hasReference("end_exon")) {
//                 protein.addReference(new Reference("endExon",
//                             translation.getReference("end_exon").getRefId()));
//             }
            proteins.put(primaryAcc, protein);
            proteinSynonyms.addAll(synonyms);
            chosenProtein = protein;
        }

        // add mapping between this translation and target protein
        if (chosenProtein != null) {
            proteinIds.put(translation.getIdentifier(), chosenProtein.getIdentifier());
        } else {
            LOG.info("no protein created for translation: " + translation.getIdentifier());
        }
        return chosenProtein;
    }

    /**
     * Find external database accession numbers in ensembl to set as Synonyms
     * @param srcItem it in source format ensembl:gene
     * @param tgtItem translate item flymine:Gene
     * @param srcNs namespace of source model
     * @return a set of Synonyms
     * @throws ObjectStoreException if problem retrieving items
     */
    protected Set setGeneSynonyms(Item srcItem, Item tgtItem, String srcNs)
        throws ObjectStoreException {
        // additional gene information is in xref table only accessible via translation
        Set synonyms = new HashSet();
        // get transcript
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + "transcript", false));
        constraints.add(new FieldNameAndValue("gene", srcItem.getIdentifier(), true));
        Item transcript = ItemHelper.convert((org.intermine.model.fulldata.Item) srcItemReader
                                        .getItemsByDescription(constraints).iterator().next());

        String translationId = transcript.getReference("translation").getRefId();
        // find xrefs

        constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + "object_xref", false));
        constraints.add(new FieldNameAndValue("ensembl", translationId, true));
        Iterator objectXrefs = srcItemReader.getItemsByDescription(constraints).iterator();
        while (objectXrefs.hasNext()) {
            Item objectXref = ItemHelper.convert(
                                (org.intermine.model.fulldata.Item) objectXrefs.next());
            Item xref = ItemHelper.convert(srcItemReader
                        .getItemById(objectXref.getReference("xref").getRefId()));
            String accession = null;
            String dbname = null;
            if (xref != null) {
                if (xref.hasAttribute("dbprimary_acc")) {
                    accession = xref.getAttribute("dbprimary_acc").getValue();
                    Reference dbRef = xref.getReference("external_db");
                    if (dbRef != null && dbRef.getRefId() != null) {
                        Item externalDb = ItemHelper.convert(srcItemReader
                                                             .getItemById(dbRef.getRefId()));
                        if (externalDb != null) {
                            dbname =  externalDb.getAttribute("db_name").getValue();
                        }
                    }
                }
            }
            if (accession != null && !accession.equals("")
                && dbname != null && !dbname.equals("")) {
                if (dbname.equals("flybase_gene") || dbname.equals("flybase_symbol")) {
                    Item synonym = createItem(tgtNs + "Synonym", "");
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    synonym.addAttribute(new Attribute("value", accession));
                    if (dbname.equals("flybase_symbol")) {
                        synonym.addAttribute(new Attribute("type", "symbol"));
                        tgtItem.addAttribute(new Attribute("symbol", accession));
                    } else { // flybase_gene
                        synonym.addAttribute(new Attribute("type", "identifier"));
                        // temporary fix to deal with broken FlyBase identfiers in ensembl
                        String value = accession;
                        Set idSet = (Set) flybaseIds.get(accession);
                        if (idSet == null) {
                            idSet = new HashSet();
                        } else {
                            value += "_flymine_" + idSet.size();
                        }
                        idSet.add(value);
                        flybaseIds.put(accession, idSet);
                        tgtItem.addAttribute(new Attribute("organismDbId", value));
                    }
                    synonym.addReference(getFlybaseRef());
                    synonyms.add(synonym);
                }
            }
        }
        return synonyms;
    }

    // keep exon with the lowest identifier
    private void processExon(Item srcItem, Item exon, Item loc) {
        String nonUniqueId = srcItem.getAttribute("nonUniqueId").getValue();
        if (exons.containsKey(nonUniqueId)) {
            Item chosenExon = null;
            Item otherExon = null;
            String oldIdentifier = ((Item) exons.get(nonUniqueId)).getIdentifier();
            if (Integer.parseInt(oldIdentifier.substring(oldIdentifier.indexOf("_") + 1))
                > Integer.parseInt(exon.getIdentifier()
                                   .substring(exon.getIdentifier().indexOf("_") + 1))) {
                chosenExon = exon;
                otherExon = (Item) exons.get(nonUniqueId);
            } else {
                chosenExon = (Item) exons.get(nonUniqueId);
                otherExon = exon;
            }
            // exon in map needs all locations in objects collection
            Set objects = new HashSet();
            objects.addAll(chosenExon.getCollection("objects").getRefIds());
            objects.addAll(otherExon.getCollection("objects").getRefIds());
            objects.add(loc.getIdentifier());
            chosenExon.addCollection(new ReferenceList("objects", new ArrayList(objects)));

            // all locs need chosen exon as subject
            addToLocations(nonUniqueId, loc);
            Iterator iter = ((Collection) exonLocs.get(nonUniqueId)).iterator();
            while (iter.hasNext()) {
                Item location = (Item) iter.next();
                location.addReference(new Reference("subject", chosenExon.getIdentifier()));
            }

            exons.put(nonUniqueId, chosenExon);
        } else {
            exons.put(nonUniqueId, exon);
            addToLocations(nonUniqueId, loc);
        }
    }

    private void addToLocations(String nonUniqueId, Item location) {
        Set locs = (Set) exonLocs.get(nonUniqueId);
        if (locs == null) {
            locs = new HashSet();
            exonLocs.put(nonUniqueId, locs);
        }
        locs.add(location);
    }

    // ensemblType should be part of name before _stable_id
    private Item getStableId(String ensemblType, String identifier, String srcNs) throws
        ObjectStoreException {
        //String value = identifier.substring(identifier.indexOf("_") + 1);
        String value = identifier;
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + ensemblType + "_stable_id", false));
        constraints.add(new FieldNameAndValue(ensemblType, value, true));
        Iterator stableIds = srcItemReader.getItemsByDescription(constraints).iterator();

        if (stableIds.hasNext()) {
            return ItemHelper.convert((org.intermine.model.fulldata.Item) stableIds.next());
        } else {
            return null;
        }
    }

    private Item createSynonym(String subjectId, String type, String value, Reference ref) {
        Item synonym = createItem("Synonym");
        synonym.addReference(new Reference("subject", subjectId));
        synonym.addAttribute(new Attribute("type", type));
        synonym.addAttribute(new Attribute("value", value));
        synonym.addReference(ref);
        return synonym;
    }

    /**
     * @see DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();

        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("repeat_feature.repeat_consensus");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("repeat_consensus",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/ensembl#repeat_feature",
               Collections.singleton(desc));

        HashSet descSet = new HashSet();
        //desc = new ItemPrefetchDescriptor("transcript.display_xref");
        //desc.addConstraint(new ItemPrefetchConstraintDynamic("display_xref",
        //ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        //descSet.add(desc);

        desc = new ItemPrefetchDescriptor(
                "(transcript.translation");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("translation",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc);
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                "((gene <- transcript.gene).translation <- object_xref.ensembl)");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "ensembl"));
        desc2.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#object_xref", false));
        desc.addPath(desc2);
        ItemPrefetchDescriptor desc3 = new ItemPrefetchDescriptor(
                "((gene <- transcript.gene).translation <- object_xref.ensembl).xref");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        ItemPrefetchDescriptor desc4 = new ItemPrefetchDescriptor(
                "((gene <- transcript.gene).translation <- object_xref.ensembl).xref.external_db");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("external_db",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3.addPath(desc4);

        desc = new ItemPrefetchDescriptor("(transcript <- transcript_stable_id.transcript)");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "transcript"));
        desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#transcript_stable_id", false));
        descSet.add(desc);

        paths.put("http://www.flymine.org/model/ensembl#transcript", descSet);


        descSet = new HashSet();
        //desc = new ItemPrefetchDescriptor("gene.display_xref");
        //desc.addConstraint(new ItemPrefetchConstraintDynamic("display_xref",
        //ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        //descSet.add(desc);
        desc = new ItemPrefetchDescriptor("(gene <- gene_stable_id.gene)");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "gene"));
        desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#gene_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("(gene <- transcript.gene)");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "gene"));
        desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#transcript", false));
        desc2 = new ItemPrefetchDescriptor(
                "(gene <- transcript.gene).translation");
        descSet.add(desc);
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("translation",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        desc3 = new ItemPrefetchDescriptor(
                "((gene <- transcript.gene).translation <- object_xref.ensembl)");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "ensembl"));
        desc3.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#object_xref", false));
        desc2.addPath(desc3);
        desc4 = new ItemPrefetchDescriptor(
                "((gene <- transcript.gene).translation <- object_xref.ensembl).xref");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3.addPath(desc4);
        ItemPrefetchDescriptor desc5 = new ItemPrefetchDescriptor(
                "((gene <- transcript.gene).translation <- object_xref.ensembl).xref.external_db");
        desc5.addConstraint(new ItemPrefetchConstraintDynamic("external_db",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc4.addPath(desc5);
        paths.put("http://www.flymine.org/model/ensembl#gene", descSet);

        desc = new ItemPrefetchDescriptor("contig.dna");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("dna",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/ensembl#contig", Collections.singleton(desc));

        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("(translation <- object_xref.ensembl)");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "ensembl"));
        desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#object_xref", false));
        desc2 = new ItemPrefetchDescriptor(
                "(translation <- object_xref.ensembl).xref");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("xref",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3 = new ItemPrefetchDescriptor("(translation <- object_xref.ensembl).xref.external_db");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("external_db",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        desc.addPath(desc2);
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("(translation <- translation_stable_id.translation)");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "translation"));
        desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#translation_stable_id", false));
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl#translation", descSet);

        desc = new ItemPrefetchDescriptor("(exon <- exon_stable_id.exon)");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, "exon"));
        desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/ensembl#exon_stable_id", false));
        paths.put("http://www.flymine.org/model/ensembl#exon", Collections.singleton(desc));

        return paths;
    }
}
