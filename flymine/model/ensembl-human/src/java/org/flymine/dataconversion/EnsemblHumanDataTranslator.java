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

import java.io.FileReader;
import java.io.File;
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
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.FieldNameAndValue;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.dataconversion.ObjectStoreItemWriter;
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
    private Map supercontigs = new HashMap();
    private Map scLocs = new HashMap();
    private Map exons = new HashMap();
    private Map flybaseIds = new HashMap();
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

        super.translate(tgtItemWriter);
        Iterator i = processChromosome().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
       //   i = processSupercontig().iterator();
//          while (i.hasNext()) {
//              tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//          }

        //Iterator i = createSuperContigs().iterator();
        //while (i.hasNext()) {
        //    tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        //}
        i = exons.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = proteins.values().iterator();
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
                    if (srcItem.hasReference("seq_region")){
                        String refid = srcItem.getReference("seq_region").getRefId();
                        Item seq =  ItemHelper.convert(srcItemReader.getItemById(refid));
                        if (getSeqProperty(seq).equals("chromosome")) {
                            tgtItem.addReference(new Reference("chromosome", refid));
                        }
                    }
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

                    Item geneRelation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, geneRelation, "objects", true, "subject", false);
                    moveField(srcItem, geneRelation, "gene", "object");
                    result.add(geneRelation);

//                      String translationId = srcItem.getReference("translation").getRefId();//??
//                      String proteinId = getChosenProteinId(translationId, srcNs);
//                      tgtItem.addReference(new Reference("protein", proteinId));
//                      Item transRelation = createItem(tgtNs + "SimpleRelation", "");
//                      transRelation.addReference(new Reference("subject", proteinId));
//                      addReferencedItem(tgtItem, transRelation, "subjects", true, "object", false);
//                      result.add(transRelation);
//                      // display_labels are not unique
//                      //promoteField(tgtItem, srcItem, "identifie", "display_xref", "display_label");
//                      // if no identifier set the identifier as name (primary key)
                    if (!tgtItem.hasAttribute("identifier")) {
                        Item stableId = getStableId("transcript", srcItem.getIdentifier(), srcNs);
                        if (stableId != null) {
                            moveField(stableId, tgtItem, "stable_id", "identifier");
                        } else {
                            tgtItem.addAttribute(new Attribute("identifier",
                                                               srcItem.getIdentifier()));
                        }
                    }

                } else if ("translation".equals(className)) {
                    // no UNIPROT id is available so id will be ensembl stable id
                    //  Item stableId = getStableId("translation", srcItem.getIdentifier(), srcNs);
//                      if (stableId != null) {
//                          moveField(stableId, tgtItem, "stable_id", "identifier");
//                      }
                    getProteinByPrimaryAccession(srcItem, srcNs);
                    //transcript translation subject object?
                    if (srcItem.hasReference("transcript")) {
                        String transcriptId = srcItem.getReference("transcript").getRefId();
                        Item transRelation = createItem(tgtNs + "SimpleRelation", "");
                        transRelation.addReference(new Reference("subject",  transcriptId));
                        addReferencedItem(tgtItem, transRelation, "subjects", true, "object", false);
                        result.add(transRelation);
                    }

                    storeTgtItem = false;
                }  // stable_ids become syonyms, need ensembl Database as source
                else if (className.endsWith("_stable_id")) {
                    if (className.endsWith("translation_stable_id")) {
                        storeTgtItem = false;
                    } else {
                        tgtItem.addReference(getEnsemblRef());
                        tgtItem.addAttribute(new Attribute("type", "accession"));
                    }
                } else if ("simple_feature".equals(className)) {
                    System.out.println("seq_region "+ className);
                    tgtItem.addReference(getOrgRef());
                    tgtItem.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem,  true));
                // } else if ("prediction_transcript".equals(className)) {
                //   tgtItem.addReference(getOrgRef());
                //    result.add(createLocation(srcItem, tgtItem, true));
                } else if ("repeat_feature".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, true));
                    promoteField(tgtItem, srcItem, "consensus", "repeat_consensus",
                                "repeat_consensus");
                    promoteField(tgtItem, srcItem, "type", "repeat_consensus", "repeat_class");
                    promoteField(tgtItem, srcItem, "identifier", "repeat_consensus", "repeat_name");

                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        // assembly maps to null but want to create location on a supercontig
        } else if ("assembly".equals(className)) {
           //   Item sc = getSuperContig(srcItem.getAttribute("superctg_name").getValue(),
//                                       srcItem.getReference("chromosome").getRefId(),
//                                       Integer.parseInt(srcItem.getAttribute("chr_start").getValue()),
//                                       Integer.parseInt(srcItem.getAttribute("chr_end").getValue()),
//                                       srcItem.getAttribute("superctg_ori").getValue());

//              // locate contig on supercontig
//              Item location = createLocation(srcItem, sc, false);
//              result.add(location);

            Item location = createAssemblyLocatin(srcItem);
            result.add(location);
        } else if ("seq_region".equals(className)) {
            String property = getSeqProperty(srcItem);//chromosome supercontig clone contig
            Item newItem = null;
            if (property != null && property != "" && property != "clone") {
                String s = (property.substring(0,1)).toUpperCase().concat(property.substring(1));
                newItem = createItem(tgtNs + s, "");
                newItem.setIdentifier(srcItem.getIdentifier());
                newItem.addAttribute(new Attribute("identifier",
                                                   srcItem.getAttribute("name").getValue()));
                newItem.addAttribute(new Attribute("length",
                                                   srcItem.getAttribute("length").getValue()));
                result.add(newItem);
            }

        }

        return result;
    }

    protected String getSeqProperty(Item srcItem) throws ObjectStoreException {
        String prop = null;
        if (srcItem.hasReference("coord_system")) {
            Item coord = ItemHelper.convert(srcItemReader.getItemById(
                                     srcItem.getReference("coord_system").getRefId()));
            prop = coord.getAttribute("name").getValue();

        }
        return prop;
    }

    protected Item createAssemblyLocatin(Item srcItem) throws ObjectStoreException {

        int start, end, asmStart, cmpStart, asmEnd, cmpEnd, contigLength;
        String ori,contigId, bioEntityId;

        //if (srcItem.hasReference("cmp_seq_region") && srcItem.hasReference("asm_seq_region")) {
            contigId = srcItem.getReference("cmp_seq_region").getRefId();
            bioEntityId = srcItem.getReference("asm_seq_region").getRefId();
            Item contig = ItemHelper.convert(srcItemReader.getItemById(contigId));
            contigLength = Integer.parseInt(contig.getAttribute("length").getValue());
            // }

        asmStart = Integer.parseInt(srcItem.getAttribute("asm_start").getValue());
        cmpStart = Integer.parseInt(srcItem.getAttribute("cmp_start").getValue());
        asmEnd = Integer.parseInt(srcItem.getAttribute("asm_end").getValue());
        cmpEnd = Integer.parseInt(srcItem.getAttribute("cmp_end").getValue());
        ori = srcItem.getAttribute("ori").getValue();

        if (ori.equals("1")) {
            start = asmStart - cmpStart + 1;
            end = start + contigLength ;
        } else {
            if (cmpEnd == contigLength) {
                start = asmStart;
                end = start + contigLength -1;
            } else {
                start = asmStart - (contigLength - cmpEnd);
                end = start + contigLength -1;
            }
        }

        Item location = createItem(tgtNs + "Location", "");
        System.out.println("assLoc "+ location.getIdentifier()+ " "+start+" "+end);
        location.addAttribute(new Attribute("start", Integer.toString(start)));
        location.addAttribute(new Attribute("end",Integer.toString(end)));
        location.addAttribute(new Attribute("startIsPartial", "false"));
        location.addAttribute(new Attribute("endIsPartial", "false"));
        location.addAttribute(new Attribute("strand", srcItem.getAttribute("ori").getValue()));
        location.addReference(new Reference("subject", contigId));
        location.addReference(new Reference("object", bioEntityId));

        return location;

    }

     protected Set processChromosome() {
        Set results = new HashSet();
        for (Iterator i = chrSet.iterator(); i.hasNext();) {
            Item chrItem = (Item) i.next();
            String id = chrItem.getIdentifier();
            List contigs = new ArrayList();
            contigs = (ArrayList) chr2Contig.get(id);
            if (contigs != null && !contigs.isEmpty()) {
                System.out.println("contig for "+ id + " " +contigs.toString());
                chrItem.addCollection(new ReferenceList("contigs", contigs));
            }
            //chrItem.addCollection(new ReferenceList("contigs", (ArrayList) chr2Contig.get(id)));
            results.add(chrItem);
        }
        return results;
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
    protected Item createLocation(Item srcItem, Item tgtItem, boolean srcItemIsChild) {
        String namespace = XmlUtil.getNamespaceFromURI(tgtItem.getClassName());

        Item location = createItem(namespace + "Location", "");

        moveField(srcItem, location,"seq_region_start", "start");
        moveField(srcItem, location,"seq_region_end", "end");
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
        if (srcItemIsChild) {
            addReferencedItem(tgtItem, location, "objects", true, "subject", false);
            moveField(srcItem, location, "seq_region", "object");
        } else {
            addReferencedItem(tgtItem, location, "subjects", true, "object", false);
            moveField(srcItem, location, "seq_region", "subject");
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
        if (srcItem.hasAttribute("analysis")) {
            moveField(srcItem, result, "analysis", "analysis");
        }
        if (srcItem.hasAttribute("score")) {
            moveField(srcItem, result, "score", "score");//gene
        }
        result.addReference(getEnsemblRef());
        ReferenceList evidence = new ReferenceList("evidence",Arrays.asList(new Object[]
                      {result.getIdentifier(), getEnsemblDb().getIdentifier()}));
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
            supercontig.addReference(getOrgRef());
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
            chosenId = getProteinByPrimaryAccession(translation, srcNs).getIdentifier();
            proteinIds.put(id, chosenId);
        }
        return chosenId;
    }

    private Item getProteinByPrimaryAccession(Item srcItem, String srcNs)
        throws ObjectStoreException {
        Item protein = createItem(tgtNs + "Protein", "");

        Set synonyms = new HashSet();
        String value = srcItem.getIdentifier();
       //   Set constraints = new HashSet();
//          constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
//                      srcNs + "object_xref", false));
//          constraints.add(new FieldNameAndValue("ensembl", value, true));
//          Iterator objectXrefs = srcItemReader.getItemsByDescription(constraints).iterator();

        // set specific ids and add synonyms
        String swissProtId = null;
        String tremblId = null;
        String emblId = null;
        //  while (objectXrefs.hasNext()) {
//              Item objectXref = ItemHelper.convert(
//                                    (org.intermine.model.fulldata.Item) objectXrefs.next());
//              Item xref = ItemHelper.convert(srcItemReader
//                          .getItemById(objectXref.getReference("xref").getRefId()));

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
                //LOG.error("processing: " + accession + ", " + dbname);

                if (accession != null && !accession.equals("")
                    && dbname != null && !dbname.equals("")) {
                    if (dbname.equals("Uniprot/SWISSPROT")) {//Uniprot/SWISSPROT
                        swissProtId = accession;
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "accession"));
                        synonym.addReference(getSwissprotRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("Uniprot/SPTREMBL")) {// Uniprot/SPTREMBL
                        tremblId = accession;
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "accession"));
                        synonym.addReference(getTremblRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("protein_id")) {
                        emblId = accession;
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "accession"));
                        synonym.addReference(getEmblRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("prediction_SPTREMBL")) {
                        emblId = accession;
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(protein, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("value", accession));
                        synonym.addAttribute(new Attribute("type", "accession"));
                        synonym.addReference(getEmblRef());
                        synonyms.add(synonym);
                    }
                }
            }
        }

        String primaryAcc = srcItem.getIdentifier();
        if (swissProtId != null) {
            primaryAcc = swissProtId;
        } else if (tremblId != null) {
            primaryAcc = tremblId;
        } else if (emblId != null) {
            primaryAcc = emblId;
        } else {
            // there was no protein accession so use ensembl stable id
            Item stableId = getStableId("translation", srcItem.getIdentifier(), srcNs);
            if (stableId != null) {
                // moveField(stableId, protein, "stable_id", "primaryAccession");
                primaryAcc = stableId.getAttribute("stable_id").getValue();
            }
        }
        protein.addAttribute(new Attribute("primaryAccession", primaryAcc));

        Item chosenProtein = (Item) proteins.get(primaryAcc);
        if (chosenProtein == null) {
            // set up additional references/collections
            protein.addReference(getOrgRef());
            if (srcItem.hasAttribute("seq_start")) {
                protein.addAttribute(new Attribute("translationStart",
                            srcItem.getAttribute("seq_start").getValue()));
            }
            if (srcItem.hasAttribute("seq_end")) {
                protein.addAttribute(new Attribute("translationEnd",
                            srcItem.getAttribute("seq_end").getValue()));
            }
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

        proteinIds.put(srcItem.getIdentifier(), chosenProtein.getIdentifier());
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
   //       // get transcript
//          Set constraints = new HashSet();
//          constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
//                      srcNs + "transcript", false));
//          constraints.add(new FieldNameAndValue("gene", srcItem.getIdentifier(), true));
//          Item transcript = ItemHelper.convert((org.intermine.model.fulldata.Item) srcItemReader
//                                          .getItemsByDescription(constraints).iterator().next());

//          String translationId = transcript.getReference("translation").getRefId();//?
//          // find xrefs

//          constraints = new HashSet();
//          constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
//                      srcNs + "object_xref", false));
//          constraints.add(new FieldNameAndValue("ensembl", translationId, true));
//          Iterator objectXrefs = srcItemReader.getItemsByDescription(constraints).iterator();
//          while (objectXrefs.hasNext()) {
//              Item objectXref = ItemHelper.convert(
//                                  (org.intermine.model.fulldata.Item) objectXrefs.next());
//              Item xref = ItemHelper.convert(srcItemReader
//                                 .getItemById(objectXref.getReference("xref").getRefId()));

        if (srcItem.hasReference("display_xref")){

            Item xref = ItemHelper.convert(srcItemReader
                        .getItemById(srcItem.getReference("display_xref").getRefId()));
            String accession = null;
            String dbname = null;

            if (xref.hasAttribute("dbprimary_acc")) {
                accession = xref.getAttribute("dbprimary_acc").getValue();
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
                if (dbname.equals("flybase_gene") || dbname.equals("flybase_symbol")) {//?
                    Item synonym = createItem(tgtNs + "Synonym", "");
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    synonym.addAttribute(new Attribute("value", accession));
                    if (dbname.equals("flybase_symbol")) {
                        synonym.addAttribute(new Attribute("type", "name"));
                        tgtItem.addAttribute(new Attribute("name", accession));
                    } else { // flybase_gene
                        synonym.addAttribute(new Attribute("type", "accession"));
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
                    synonym.addReference(getFlyBaseRef());
                    synonyms.add(synonym);
                }
            }
        }
        return synonyms;
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


   private List getCommentIds(String identifier, String srcNs) throws
        ObjectStoreException {
        String value = identifier;
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    srcNs + "gene_description", false));
        constraints.add(new FieldNameAndValue("gene", value, true));
        List commentIds = new ArrayList();
        for (Iterator i = srcItemReader.getItemsByDescription(constraints).iterator();
                i.hasNext(); ){
            Item comment = ItemHelper.convert((org.intermine.model.fulldata.Item) i.next());
            commentIds.add((String) comment.getIdentifier());
        }
        return commentIds;
    }

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

    private Reference getEmblRef() {
        if (emblRef == null) {
            emblRef = new Reference("source", getEmblDb().getIdentifier());
        }
        return emblRef;
    }


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

    private Reference getSwissprotRef() {
        if (swissprotRef == null) {
            swissprotRef = new Reference("source", getSwissprotDb().getIdentifier());
        }
        return swissprotRef;
    }

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

    private Reference getTremblRef() {
        if (tremblRef == null) {
            tremblRef = new Reference("source", getTremblDb().getIdentifier());
        }
        return tremblRef;
    }

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

    private Reference getFlyBaseRef() {
        if (flybaseRef == null) {
            flybaseRef = new Reference("source", getFlyBaseDb().getIdentifier());
        }
        return flybaseRef;
    }

    private Item getOrganism() {
        if (organism == null) {
            organism = createItem(tgtNs + "Organism", "");
            Attribute a1 = new Attribute("abbreviation", orgAbbrev);
            organism.addAttribute(a1);
        }
        return organism;
    }

    private Reference getOrgRef() {
        if (orgRef == null) {
            orgRef = new Reference("organism", getOrganism().getIdentifier());
        }
        return orgRef;
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
        String orgAbbrev = args[5];

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

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc, paths);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new ObjectStoreItemWriter(oswTgt);

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new EnsemblHumanDataTranslator(srcItemReader, model, namespace, orgAbbrev);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
