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
import java.util.List;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;

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
import org.intermine.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * Convert Ensembl data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 * @author Richard Smith
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class EnsemblHumanDataTranslator extends DataTranslator
{
    protected static final Logger LOG = Logger.getLogger(EnsemblHumanDataTranslator.class);
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
    private Item refSeqDb;
    private Reference refSeqRef;
    private Item hugoDb;
    private Reference hugoRef;
    private Item genbankDb;
    private Reference genbankRef;
    private Item gdbDb;
    private Reference gdbRef;
    private Item unistsDb;
    private Reference unistsRef;
    private Map supercontigs = new HashMap();
    private Map scLocs = new HashMap();
    private String orgAbbrev;
    private Item organism;
    private Reference orgRef;
    private Map proteins = new HashMap();
    private Map proteinIds = new HashMap();
    private Set proteinSynonyms = new HashSet();
    private Map chr2Contig = new HashMap();
    private Map sc2Contig = new HashMap();
    private Map clone2Contig = new HashMap();
    private Set chrSet = new HashSet();
    private Set scSet = new HashSet();
    private Set contigSet = new HashSet();
    private Set cloneSet = new HashSet();
    private Map seqIdMap = new HashMap();

    /**
     * @see DataTranslator#DataTranslator
     */
    public EnsemblHumanDataTranslator(ItemReader srcItemReader, OntModel model, String ns,
                                      String orgAbbrev) {
        super(srcItemReader, model, ns);
        this.orgAbbrev = orgAbbrev;
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {
        tgtItemWriter.store(ItemHelper.convert(getOrganism()));
        tgtItemWriter.store(ItemHelper.convert(getEnsemblDb()));
        tgtItemWriter.store(ItemHelper.convert(getEmblDb()));
        tgtItemWriter.store(ItemHelper.convert(getTremblDb()));
        tgtItemWriter.store(ItemHelper.convert(getSwissprotDb()));
        tgtItemWriter.store(ItemHelper.convert(getHugoDb()));
        tgtItemWriter.store(ItemHelper.convert(getRefSeqDb()));
        tgtItemWriter.store(ItemHelper.convert(getGenbankDb()));
        tgtItemWriter.store(ItemHelper.convert(getGdbDb()));
        tgtItemWriter.store(ItemHelper.convert(getUnistsDb()));

        super.translate(tgtItemWriter);

        Iterator i = proteins.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        i = proteinSynonyms.iterator();
        while (i.hasNext()) {
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
                    tgtItem.addReference(getOrgRef());
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    Item location = createLocation(srcItem, tgtItem, true);
                    location.addAttribute(new Attribute("strand", "0"));
                    result.add(location);
                } else if ("exon".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    Item stableId = getStableId("exon", srcItem.getIdentifier(), srcNs);
                    if (stableId != null) {
                        moveField(stableId, tgtItem, "stable_id", "identifier");
                    }
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    Item location = createLocation(srcItem, tgtItem, true);
                    result.add(location);
                } else if ("gene".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    Item location = createLocation(srcItem, tgtItem, true);
                    result.add(location);
                    Item anaResult = createAnalysisResult(srcItem, tgtItem);
                    result.add(anaResult);
                    List comments = getCommentIds(srcItem.getIdentifier(), srcNs);
                    if (!comments.isEmpty()) {
                        tgtItem.addCollection(new ReferenceList("comments", comments));
                    }
                    // gene name should be its stable id (or identifier if none)
                    Item stableId = null;
                    stableId = getStableId("gene", srcItem.getIdentifier(), srcNs);
                    if (stableId != null) {
                         moveField(stableId, tgtItem, "stable_id", "identifier");
                    } else {
                        tgtItem.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
                    }
                    // display_xref is gene name (?)
                    //promoteField(tgtItem, srcItem, "name", "display_xref", "display_label");
                    result.addAll(setGeneSynonyms(srcItem, tgtItem, srcNs));
                    // if no organismDbId set to be same as identifier
                    if (!tgtItem.hasAttribute("organismDbId")) {
                        tgtItem.addAttribute(new Attribute("organismDbId",
                               tgtItem.getAttribute("identifier").getValue()));
                    }
                } else if ("transcript".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    Item geneRelation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, geneRelation, "objects", true, "subject", false);
                    moveField(srcItem, geneRelation, "gene", "object");
                    result.add(geneRelation);
                    // if no identifier set the identifier as name (primary key)
                    if (!tgtItem.hasAttribute("identifier")) {
                        Item stableId = getStableId("transcript", srcItem.getIdentifier(), srcNs);
                        if (stableId != null) {
                            moveField(stableId, tgtItem, "stable_id", "identifier");
                        } else {
                            tgtItem.addAttribute(new Attribute("identifier",
                                                               srcItem.getIdentifier()));
                        }
                    }
                    Item location = createLocation(srcItem, tgtItem, true);
                    result.add(location);
                } else if ("translation".equals(className)) {
                    Item protein = getProteinByPrimaryAccession(srcItem, srcNs);

                    if (protein != null && srcItem.hasReference("transcript")) {
                        String transcriptId = srcItem.getReference("transcript").getRefId();
                        Item transRelation = createItem(tgtNs + "SimpleRelation", "");
                        transRelation.addReference(new Reference("subject",  transcriptId));
                        addReferencedItem(protein, transRelation, "subjects", true,
                                         "object", false);
                        result.add(transRelation);
                    }
                    storeTgtItem = false;
                // stable_ids become syonyms, need ensembl Database as source
                } else if (className.endsWith("_stable_id")) {
                    if (className.endsWith("translation_stable_id")) {
                        storeTgtItem = false;
                    } else {
                        tgtItem.addReference(getEnsemblRef());
                        tgtItem.addAttribute(new Attribute("type", "identifier"));
                    }
                // } else if ("prediction_transcript".equals(className)) {
                //   tgtItem.addReference(getOrgRef());
                //    result.add(createLocation(srcItem, tgtItem, true));
                } else if ("repeat_feature".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, true));
                    promoteField(tgtItem, srcItem, "consensus", "repeat_consensus",
                                "repeat_consensus");
                    promoteField(tgtItem, srcItem, "type", "repeat_consensus", "repeat_class");
                    promoteField(tgtItem, srcItem, "identifier", "repeat_consensus", "repeat_name");
                } else if ("marker".equals(className)) {
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    Set locations = createLocations(srcItem, tgtItem, srcNs);
                    List locationIds = new ArrayList();
                    for (Iterator j = locations.iterator(); j.hasNext(); ) {
                        Item location = (Item) j.next();
                        locationIds.add(location.getIdentifier());
                        result.add(location);
                    }
                    //tgtItem.addCollection(new ReferenceList("locations", locationIds));
                    setNameAttribute(srcItem, tgtItem);
                    //setSynonym
                } else if ("marker_synonym".equals(className)) {
                    tgtItem.addAttribute(new Attribute("type", "identifier"));
                    if (srcItem.hasAttribute("source")) {
                        String source = srcItem.getAttribute("source").getValue();
                        if (source.equals("genbank")) {
                            tgtItem.addReference(getGenbankRef());
                        } else  if (source.equals("gdb")) {
                            tgtItem.addReference(getGdbRef());
                        } else  if (source.equals("unists")) {
                            tgtItem.addReference(getUnistsRef());
                        } else {
                            tgtItem.addReference(getEnsemblRef());
                        }
                    } else {
                        tgtItem.addReference(getEnsemblRef());
                    }
                }
                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        // assembly maps to null but want to create location on a supercontig
        } else if ("assembly".equals(className)) {
            Item location = createAssemblyLocation(srcItem);
            result.add(location);
        // seq_region map to null, become Chromosome, Supercontig, Clone and Contig respectively
         } else if ("seq_region".equals(className)) {
            Item seq = getSeqItem(srcItem.getIdentifier());
            result.add(seq);
         //simple_feature map to null, become TRNA/CpGIsland depending on analysis_id(logic_name)
         } else if ("simple_feature".equals(className)) {
             Item simpleFeature = createSimpleFeature(srcItem);
             result.add(simpleFeature);
             result.add(createLocation(srcItem, simpleFeature, true));
             result.add(createAnalysisResult(srcItem, simpleFeature));
          }
        return result;
    }

    /**
     * Translate a "located" Item into an Item and a location
     * @param srcItem the source Item
     * @param tgtItem the target Item (after translation)
     * @param srcItemIsChild true if srcItem should be subject of Location
     * @throws ObjectStoreException when anything goes wrong.
     * @return the location item
     */
    protected Item createLocation(Item srcItem, Item tgtItem, boolean srcItemIsChild)
        throws ObjectStoreException {
        String namespace = XmlUtil.getNamespaceFromURI(tgtItem.getClassName());
        Item location = createItem(namespace + "Location", "");
        Item seq = new Item();
        moveField(srcItem, location, "seq_region_start", "start");
        moveField(srcItem, location, "seq_region_end", "end");
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
            seq = (Item) getSeqItem(refId);
        }
        if (srcItemIsChild) {
            addReferencedItem(tgtItem, location, "objects", true, "subject", false);
            location.addReference(new Reference("object", seq.getIdentifier()));
            //moveField(srcItem, location, "seq_region", "object");
        } else {
            addReferencedItem(tgtItem, location, "subjects", true, "object", false);
            location.addReference(new Reference("subject", seq.getIdentifier()));
            //moveField(srcItem, location, "seq_region", "subject");
        }
        return location;
    }

    /**
     * @param srcItem ensembl:marker
     * @param tgtItem flymine:Marker
     * @param srcNs source namespace
     * @throws ObjectStoreException when anything goes wrong.
     * @return set of locations
     */
    protected Set createLocations(Item srcItem, Item tgtItem, String srcNs)
        throws ObjectStoreException {
        Set result = new HashSet();
        Set constraints = new HashSet();
        String value = srcItem.getIdentifier();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + "marker_feature", false));
        constraints.add(new FieldNameAndValue("marker", value, true));
        Item location = new Item();
        for (Iterator i = srcItemReader.getItemsByDescription(constraints).iterator();
             i.hasNext(); ) {
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
     * @throws ObjectStoreException when anything goes wrong.
     */
    protected void setNameAttribute(Item srcItem, Item tgtItem) throws ObjectStoreException {
        if (srcItem.hasReference("display_marker_synonym")) {
            Item synonym  = ItemHelper.convert(srcItemReader.getItemById(
                               srcItem.getReference("display_marker_synonym").getRefId()));
            if (synonym.hasAttribute("name")) {
                String name = synonym.getAttribute("name").getValue();
                tgtItem.addAttribute(new Attribute("name", name));
            }
        }
    }

   /**
     * @param srcItem = assembly
     * @return location item which reflects the relations between chromosome and contig,
     * supercontig and contig, clone and contig
     * @throws ObjectStoreException when anything goes wrong.
     */
    protected Item createAssemblyLocation(Item srcItem) throws ObjectStoreException {
        int start, end, asmStart, cmpStart, asmEnd, cmpEnd;
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
        asmEnd = Integer.parseInt(srcItem.getAttribute("asm_end").getValue());
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
        Item seq = new Item();
        seq = (Item) getSeqItem(contigId);
        location.addReference(new Reference("subject", seq.getIdentifier()));
        seq = (Item) getSeqItem(bioEntityId);
        location.addReference(new Reference("object", seq.getIdentifier()));
        return location;
    }

    /**
     * @param refId = refId for the seq_region
     * @return seq item it could be  chromosome, supercontig, clone or contig
     * @throws ObjectStoreException when anything goes wrong.
     */
    protected Item getSeqItem(String refId) throws ObjectStoreException {
        Item seq = new Item();
        Item seqRegion = ItemHelper.convert(srcItemReader.getItemById(refId));
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
            if (property != null && property != "") {
                String s = (property.substring(0, 1)).toUpperCase().concat(property.substring(1));
                seq = createItem(tgtNs + s, "");
                if (seqRegion.hasAttribute("name")) {
                    seq.addAttribute(new Attribute("identifier",
                                          seqRegion.getAttribute("name").getValue()));
                }
                if (seqRegion.hasAttribute("length")) {
                    seq.addAttribute(new Attribute("length",
                                          seqRegion.getAttribute("length").getValue()));
                }
                addReferencedItem(seq, getEnsemblDb(), "evidence", true, "", false);
                seqIdMap.put(refId, seq);
            }
        }
        return seq;
    }

    /**
     * Create an AnalysisResult pointed to by tgtItem evidence reference.  Move srcItem
     * analysis reference and score to new AnalysisResult.
     * @param srcItem item in src namespace to move fields from
     * @param tgtItem item that will reference AnalysisResult
     * @throws ObjectStoreException when anything goes wrong.
     * @return new AnalysisResult item
     */
    protected Item createAnalysisResult(Item srcItem, Item tgtItem) throws ObjectStoreException {
        Item result = createItem(tgtNs + "ComputationalResult", "");
        if (srcItem.hasReference("analysis")) {
            moveField(srcItem, result, "analysis", "analysis");
        }
        if (srcItem.hasAttribute("score")) {
            moveField(srcItem, result, "score", "score");
        }
        result.addReference(getEnsemblRef());
        ReferenceList evidence = new ReferenceList("evidence", Arrays.asList(new Object[]
                      {result.getIdentifier(), getEnsemblDb().getIdentifier()}));
        tgtItem.addCollection(evidence);
        return result;
    }

    /**
     * Create a simpleFeature item depends on the logic_name attribute in analysis
     * will become TRNA, or CpGIsland
     * @param srcItem ensembl: simple_feature
     * @return new simpleFeature item
     * @throws ObjectStoreException when anything goes wrong.
     */
    protected Item createSimpleFeature(Item srcItem) throws ObjectStoreException {
        Item simpleFeature = new Item();
        if (srcItem.hasReference("analysis")) {
            Item analysis = ItemHelper.convert(srcItemReader.getItemById(
                                       srcItem.getReference("analysis").getRefId()));
            if (analysis.hasAttribute("logic_name")) {
                String name = analysis.getAttribute("logic_name").getValue();
                if (name.equals("tRNAscan")) {
                    simpleFeature = createItem(tgtNs + "TRNA", "");
                } else if (name.equals("CpG")) {
                    simpleFeature = createItem(tgtNs + "CpGIsland", "");
                } else if (name.equals("Eponine")) {
                    simpleFeature = createItem(tgtNs + "TranscriptionStartSite", "");
                } else if (name.equals("FirstEF")) {
                    //5 primer exon and promoter including coding and noncoding
                }
                simpleFeature.addReference(getOrgRef());
                simpleFeature.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
                addReferencedItem(simpleFeature, getEnsemblDb(), "evidence", true, "", false);
            }
        }
        return simpleFeature;
    }

    /**
     * @param Item srcItem = e:translation
     * @param String srcNs sourceNamespace
     * @return Item for :Protein
     * @throws ObjectStoreException if anything goes wrong
     */
    private Item getProteinByPrimaryAccession(Item srcItem, String srcNs)
        throws ObjectStoreException {
        Item protein = createItem(tgtNs + "Protein", "");
        Set synonyms = new HashSet();
        String value = srcItem.getIdentifier();
        String swissProtId = null;
        String tremblId = null;
        if (srcItem.hasReference("transcript")) {
            Item transcript = ItemHelper.convert(srcItemReader.getItemById(
                                    srcItem.getReference("transcript").getRefId()));
            if (transcript.hasReference("display_xref")) {
                Item xref = ItemHelper.convert(srcItemReader.getItemById(
                                    transcript.getReference("display_xref").getRefId()));
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
                if (accession != null && !accession.equals("")
                    && dbname != null && !dbname.equals("")) {
                    if (dbname.equals("Uniprot/SWISSPROT")) { //Uniprot/SWISSPROT
                        swissProtId = accession;
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "identifier"));
                        synonym.addReference(getSwissprotRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("Uniprot/SPTREMBL")) { // Uniprot/SPTREMBL
                        tremblId = accession;
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "identifier"));
                        synonym.addReference(getTremblRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("protein_id")
                        || dbname.equals("prediction_SPTREMBL")) {
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "identifier"));
                        synonym.addReference(getEmblRef());
                        synonyms.add(synonym);
                    }
                }
            }
        }
        String primaryAcc = null;
        if (swissProtId != null) {
            primaryAcc = swissProtId;
        } else if (tremblId != null) {
            primaryAcc = tremblId;
        } else {
            // there was no protein accession so use ensembl stable id
            Item stableId = getStableId("translation", srcItem.getIdentifier(), srcNs);
            if (stableId != null) {
                primaryAcc = stableId.getAttribute("stable_id").getValue();
            }
        }

        Item chosenProtein = (Item) proteins.get(primaryAcc);
        if (chosenProtein == null && primaryAcc != null) {
            protein.addAttribute(new Attribute("primaryAccession", primaryAcc));
            addReferencedItem(protein, getEnsemblDb(), "evidence", true, "", false);
            // set up additional references/collections
            protein.addReference(getOrgRef());
            if (srcItem.hasReference("start_exon")) {
                protein.addReference(new Reference("startExon",
                            srcItem.getReference("start_exon").getRefId()));
            }
            if (srcItem.hasReference("end_exon")) {
                protein.addReference(new Reference("endExon",
                            srcItem.getReference("end_exon").getRefId()));
            }
            proteins.put(primaryAcc, protein);
            proteinSynonyms.addAll(synonyms);
            chosenProtein = protein;
        }

        if (chosenProtein != null) {
            proteinIds.put(srcItem.getIdentifier(), chosenProtein.getIdentifier());
        }  else {
            LOG.info("no protein created for translation: " + srcItem.getIdentifier());
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
        if (srcItem.hasReference("display_xref")) {
            Item xref = ItemHelper.convert(srcItemReader
                        .getItemById(srcItem.getReference("display_xref").getRefId()));
            String accession = null;
            String dbname = null;
            String name = null;
            if (xref.hasAttribute("dbprimary_acc")) {
                accession = xref.getAttribute("dbprimary_acc").getValue();
            }
            if (xref.hasAttribute("display_label")) {
                name = xref.getAttribute("display_label").getValue();
            }
            if (xref.hasReference("external_db")) {
                Item externalDb = ItemHelper.convert(srcItemReader
                                        .getItemById(xref.getReference("external_db").getRefId()));
                if (externalDb != null) {
                    dbname =  externalDb.getAttribute("db_name").getValue();
                }
            }
           if (accession != null && !accession.equals("")
                && dbname != null && !dbname.equals("")) {
                if (dbname.equals("HUGO") || dbname.equals("RefSeq")) { //?HUGO RefSeq
                    Item synonym = createItem(tgtNs + "Synonym", "");
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    synonym.addAttribute(new Attribute("value", accession));
                    if (dbname.equals("HUGO")) {
                        synonym.addReference(getHugoRef());
                        synonym.addAttribute(new Attribute("type", "identifier"));
                        tgtItem.addAttribute(new Attribute("organismDbId", accession));
                        tgtItem.addAttribute(new Attribute("name", name));
                    } else {
                        synonym.addReference(getRefSeqRef());
                        synonym.addAttribute(new Attribute("type", "identifier"));
                        tgtItem.addAttribute(new Attribute("name", accession));
                    }
                    synonyms.add(synonym);
                }
            }
        }
        return synonyms;
    }

    /**
     * Find stable_id for various ensembl type
     * @param ensemblType could be gene, exon, transcript or translation
     * it should be part of name before _stable_id
     * @param identifier  srcItem identifier, srcItem could be gene, exon, transcript/translation
     * @param srcNs namespace of source model
     * @return a set of Synonyms
     * @throws ObjectStoreException if problem retrieving items
     */
    private Item getStableId(String ensemblType, String identifier, String srcNs) throws
        ObjectStoreException {
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

    /**
     * change description class to comments
     * @param identifier  ensembl:gene srcItem identifier
     * @param tgtItem translate item flymine:Gene
     * @param srcNs namespace of source model
     * @return a list of commentIds
     * @throws ObjectStoreException if problem retrieving items
     */
    private List getCommentIds(String identifier, String srcNs) throws
        ObjectStoreException {
        String value = identifier;
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + "gene_description", false));
        constraints.add(new FieldNameAndValue("gene", value, true));
        List commentIds = new ArrayList();
        for (Iterator i = srcItemReader.getItemsByDescription(constraints).iterator();
                i.hasNext(); ) {
            Item comment = ItemHelper.convert((org.intermine.model.fulldata.Item) i.next());
            commentIds.add((String) comment.getIdentifier());
        }
        return commentIds;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getEnsemblDb() {
        if (ensemblDb == null) {
            ensemblDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "ensembl");
            Attribute url = new Attribute("url", "http://www.ensembl.org");
            ensemblDb.addAttribute(title);
            ensemblDb.addAttribute(url);
        }
        return ensemblDb;
    }

    private Reference getEnsemblRef() {
        if (ensemblRef == null) {
            ensemblRef = new Reference("source", getEnsemblDb().getIdentifier());
        }
        return ensemblRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getEmblDb() {
        if (emblDb == null) {
            emblDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "embl");
            Attribute url = new Attribute("url", "http://www.ebi.ac.uk/embl");
            emblDb.addAttribute(title);
            emblDb.addAttribute(url);
        }
        return emblDb;
    }

    /**
     * @return db reference
     */
    private Reference getEmblRef() {
        if (emblRef == null) {
            emblRef = new Reference("source", getEmblDb().getIdentifier());
        }
        return emblRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getSwissprotDb() {
        if (swissprotDb == null) {
            swissprotDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "Swiss-Prot");
            Attribute url = new Attribute("url", "http://ca.expasy.org/sprot/");
            swissprotDb.addAttribute(title);
            swissprotDb.addAttribute(url);
        }
        return swissprotDb;
    }

    /**
     * @return db reference
     */
    private Reference getSwissprotRef() {
        if (swissprotRef == null) {
            swissprotRef = new Reference("source", getSwissprotDb().getIdentifier());
        }
        return swissprotRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getTremblDb() {
        if (tremblDb == null) {
            tremblDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "TrEMBL");
            Attribute url = new Attribute("url", "http://ca.expasy.org/sprot/");
            tremblDb.addAttribute(title);
            tremblDb.addAttribute(url);
        }
        return tremblDb;
    }

    /**
     * @return db reference
     */
    private Reference getTremblRef() {
        if (tremblRef == null) {
            tremblRef = new Reference("source", getTremblDb().getIdentifier());
        }
        return tremblRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getFlyBaseDb() {
        if (flybaseDb == null) {
            flybaseDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "FlyBase");
            Attribute url = new Attribute("url", "http://www.flybase.org");
            flybaseDb.addAttribute(title);
            flybaseDb.addAttribute(url);
        }
        return flybaseDb;
    }

    /**
     * @return db reference
     */
    private Reference getFlyBaseRef() {
        if (flybaseRef == null) {
            flybaseRef = new Reference("source", getFlyBaseDb().getIdentifier());
        }
        return flybaseRef;
    }

    /**
     * @return db reference
     */
    private Reference getRefSeqRef() {
        if (refSeqRef == null) {
            refSeqRef = new Reference("source", getRefSeqDb().getIdentifier());
        }
        return refSeqRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getRefSeqDb() {
        if (refSeqDb == null) {
            refSeqDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "RefSeq");
            Attribute url = new Attribute("url", "http://www.ncbi.nlm.nih.gov/RefSeq/");
            refSeqDb.addAttribute(title);
            refSeqDb.addAttribute(url);
        }
        return refSeqDb;
    }

    /**
     * @return db reference
     */
    private Reference getHugoRef() {
        if (hugoRef == null) {
            hugoRef = new Reference("source", getHugoDb().getIdentifier());
        }
        return hugoRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getHugoDb() {
        if (hugoDb == null) {
            hugoDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "HUGO");
            Attribute url = new Attribute("url", "http://www.hugo-international.org/hugo");
            hugoDb.addAttribute(title);
            hugoDb.addAttribute(url);
        }
        return hugoDb;
    }

    /**
     * @return db reference
     */
    private Reference getGenbankRef() {
        if (genbankRef == null) {
            genbankRef = new Reference("source", getGenbankDb().getIdentifier());
        }
        return genbankRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getGenbankDb() {
        if (genbankDb == null) {
            genbankDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "genbank");
            Attribute url = new Attribute("url", "http://www.ncbi.nlm.nih.gov/");
            genbankDb.addAttribute(title);
            genbankDb.addAttribute(url);
        }
        return genbankDb;
    }

    /**
     * @return db reference
     */
    private Reference getGdbRef() {
        if (gdbRef == null) {
            gdbRef = new Reference("source", getGdbDb().getIdentifier());
        }
        return gdbRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getGdbDb() {
        if (gdbDb == null) {
            gdbDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "gdb");
            Attribute url = new Attribute("url", "http://gdbwww.gdb.org/");
            gdbDb.addAttribute(title);
            gdbDb.addAttribute(url);
        }
        return gdbDb;
    }

    /**
     * @return db reference
     */
    private Reference getUnistsRef() {
        if (unistsRef == null) {
            unistsRef = new Reference("source", getUnistsDb().getIdentifier());
        }
        return unistsRef;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getUnistsDb() {
        if (unistsDb == null) {
            unistsDb = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", "unists");
            Attribute url = new Attribute("url",
                        "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=unists");
            unistsDb.addAttribute(title);
            unistsDb.addAttribute(url);
        }
        return unistsDb;
    }

    /**
     * @return organism item, for homo_sapiens, abbreviation is HS
     */
    private Item getOrganism() {
        if (organism == null) {
            organism = createItem(tgtNs + "Organism", "");
            organism.addAttribute(new Attribute("abbreviation", orgAbbrev));
            organism.addAttribute(new Attribute("name", "Homo sapiens"));
            organism.addAttribute(new Attribute("shortName", "H.sapiens"));
            organism.addAttribute(new Attribute("taxonId", "9606"));
            organism.addAttribute(new Attribute("genus", "homo"));
            organism.addAttribute(new Attribute("species", "sapiens"));
        }
        return organism;
    }

    /**
     * @return organism reference
     */
    private Reference getOrgRef() {
        if (orgRef == null) {
            orgRef = new Reference("organism", getOrganism().getIdentifier());
        }
        return orgRef;
    }

    /**
     * @see DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        String identifier = ObjectStoreItemPathFollowingImpl.IDENTIFIER;
        String classname = ObjectStoreItemPathFollowingImpl.CLASSNAME;

        //karyotype
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("karyotype.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        paths.put("http://www.flymine.org/model/ensembl-human#karyotype",
                    Collections.singleton(desc));

        //exon
        Set descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("exon <- exon_stable_id.exon");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "exon"));
        desc.addConstraint(new FieldNameAndValue(classname,
                      "http://www.flymine.org/model/ensembl-human#exon_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("exon.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        ItemPrefetchDescriptor desc1 = new ItemPrefetchDescriptor(
                               "exon.seq_region <- seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#exon", descSet);

        //gene
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("gene <- gene_stable_id.gene");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "gene"));
        desc.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#gene_stable_id", false));
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("gene.analysis");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("analysis", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("gene.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor("gene.seq_region <- seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc1);
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("gene <- gene_description.gene");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "gene"));
        desc.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#gene_description", false));
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("gene.display_xref");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("display_xref", identifier));
         desc1 = new ItemPrefetchDescriptor("gene.display_xref <- xref.external_db");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("external_db", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#xref", false));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#gene", descSet);

         //transcript
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("transcript <- transcript_stable_id.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "transcript"));
        desc.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#transcript_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("transcript.gene");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("gene", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("transcript.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor(
                               "transcript.seq_region <- seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#transcript", descSet);

        //translation
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("translation <- translation_stable_id.translation");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "translation"));
        desc.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#translation_stable_id", false));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("translation.transcript");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("transcript", identifier));
        descSet.add(desc);
        desc1 = new ItemPrefetchDescriptor("(translation.transcript).display_xref");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("display_xref", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#transcript", false));
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                               "transcript.display_xref <- xref.external_db");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("external_db", identifier));
        desc2.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#xref", false));
        desc1.addPath(desc2);
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#translation", descSet);

        //simple_feature
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("simple_feature.analysis");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("analysis", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("simple_feature.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor(
                               "simple_feature.seq_region <- seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#simple_feature", descSet);

        //repeat_feature
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("repeat_feature.repeat_consensus");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("repeat_consensus", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("repeat_feature.seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc1 = new ItemPrefetchDescriptor(
                               "repeat_feature.seq_region <- seq_region.coord_system");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc1.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#repeat_feature", descSet);

        //marker
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("marker.display_marker_synonym");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("display_marker_synonym", identifier));
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("marker <- marker_feature.marker");
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier, "marker"));
        desc.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#marker_feature", false));

        desc1 = new ItemPrefetchDescriptor("marker_feature.seq_region");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("seq_region", identifier));
        desc2 = new ItemPrefetchDescriptor(
                               "marker_feature.seq_region <- seq_region.coord_system");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc2.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc1.addPath(desc2);
        desc.addPath(desc1);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#marker",
                    Collections.singleton(desc));

        //assembly
        descSet = new HashSet();
        desc = new ItemPrefetchDescriptor("assembly.asm_seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("asm_seq_region", identifier));
        desc2 = new ItemPrefetchDescriptor(
                               "assembly.asm_seq_region <- seq_region.coord_system");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc2.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc2);
        descSet.add(desc);
        desc = new ItemPrefetchDescriptor("assembly.cmp_seq_region");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("cmp_seq_region",
                     identifier));
        desc2 = new ItemPrefetchDescriptor(
                               "assembly.cmp_seq_region <- seq_region.coord_system");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        desc2.addConstraint(new FieldNameAndValue(classname,
                    "http://www.flymine.org/model/ensembl-human#seq_region", false));
        desc.addPath(desc2);
        descSet.add(desc);
        paths.put("http://www.flymine.org/model/ensembl-human#assembly", descSet);

        //seq_region
        desc = new ItemPrefetchDescriptor("seq_region.coord_system");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("coord_system", identifier));
        paths.put("http://www.flymine.org/model/ensembl-human#seq_region",
                  Collections.singleton(desc));

        return paths;
    }
}
