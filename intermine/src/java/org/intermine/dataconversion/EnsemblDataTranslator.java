package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.flymine.FlyMineException;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.ItemHelper;
import org.flymine.ontology.OntologyUtil;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;

import org.apache.log4j.Logger;

/**
 * Convert Ensembl data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to FlyMine OWL definition.
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
    private Map supercontigs = new HashMap();
    private Map scLocs = new HashMap();
    private Map exonLocs = new LinkedHashMap();
    private Map exons = new HashMap();
    private String orgName;
    private String orgShortName;
    private String orgTaxonId;
    private Item organism;
    private Reference orgRef;

    /**
     * @see DataTranslator#DataTranslator
     */
    public EnsemblDataTranslator(ItemReader srcItemReader, OntModel model, String ns,
                                 String orgName, String orgShortName, String orgTaxonId) {
        super(srcItemReader, model, ns);
        this.orgName = orgName;
        this.orgShortName = orgShortName;
        this.orgTaxonId = orgTaxonId;
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter) throws ObjectStoreException, FlyMineException {
        tgtItemWriter.store(ItemHelper.convert(getOrganism()));
        tgtItemWriter.store(ItemHelper.convert(getEnsemblDb()));
        tgtItemWriter.store(ItemHelper.convert(getEmblDb()));
        tgtItemWriter.store(ItemHelper.convert(getTremblDb()));
        tgtItemWriter.store(ItemHelper.convert(getSwissprotDb()));

        super.translate(tgtItemWriter);
        Iterator i = createSuperContigs().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        i = exonLocs.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        i = exons.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem) throws ObjectStoreException, FlyMineException {
        Collection result = new HashSet();
        String className = OntologyUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
                if ("karyotype".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    result.add(createLocation(srcItem, tgtItem, "chromosome", "chr", true));
                } else if ("exon".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    storeTgtItem = false;
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    Set locs = processExon(srcItem, tgtItem,
                        createLocation(srcItem, tgtItem, "contig", "contig", true));
                    if (!locs.isEmpty()) {
                        result.addAll(locs);
                    }
                } else if ("simple_feature".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                } else if ("prediction_transcript".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                } else if ("repeat_feature".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                    promoteField(tgtItem, srcItem, "consensus", "repeat_consensus",
                                 "repeat_consensus");
                    promoteField(tgtItem, srcItem, "type", "repeat_consensus", "repeat_class");
                    promoteField(tgtItem, srcItem, "name", "repeat_consensus", "repeat_name");
                } else if ("gene".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    promoteField(tgtItem, srcItem, "name", "display_xref", "display_label");
                    if (!tgtItem.hasAttribute("name")) {
                        tgtItem.addAttribute(new Attribute("name", srcItem.getIdentifier()));
                    }
                } else if ("contig".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    Item relation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, relation, "subjects", true, "object", false);
                    moveField(srcItem, relation, "clone", "subject");
                    result.add(relation);
                    promoteField(tgtItem, srcItem, "residues", "dna", "sequence");
                } else if ("transcript".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    Item geneRelation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, geneRelation, "objects", true, "subject", false);
                    moveField(srcItem, geneRelation, "gene", "object");
                    result.add(geneRelation);
                    Item transRelation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, transRelation, "subjects", true, "object", false);
                    moveField(srcItem, transRelation, "translation", "subject");
                    result.add(transRelation);
                    promoteField(tgtItem, srcItem, "name", "display_xref", "display_label");
                    // if no name set the identifier as name (primary key)
                    if (!tgtItem.hasAttribute("name")) {
                        tgtItem.addAttribute(new Attribute("name", srcItem.getIdentifier()));
                    }
                // stable_ids become syonyms, need ensembl Database as source
                } else if (className.endsWith("_stable_id")) {
                    tgtItem.addReference(getEnsemblRef());
                } else if ("clone".equals(className)) {
                    // clone embl_acc needs to be a synonym in embl database

                    tgtItem.addReference(getOrgRef());
                    Item synonym = createItem(tgtNs + "Synonym", "");
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    moveField(srcItem, synonym, "embl_acc", "synonym");
                    synonym.addReference(getEmblRef());
                    result.add(synonym);
                } else if ("chromosome".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                } else if ("translation".equals(className)) {
                    tgtItem.addReference(getOrgRef());
                    tgtItem.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
                    result.addAll(setProteinIdentifiers(srcItem, tgtItem));
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
                                     Integer.parseInt(srcItem.getAttribute("chr_end").getValue()));
            Item location = createLocation(srcItem, sc, "contig", "superctg", false);
            result.add(location);
        }
        return result;
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
        String namespace = OntologyUtil.getNamespaceFromURI(tgtItem.getClassName());

        Item location = createItem(namespace + "Location", "");

        moveField(srcItem, location, locPrefix + "_start", "start");
        moveField(srcItem, location, locPrefix + "_end", "end");
        location.addAttribute(new Attribute("startIsPartial", "false"));
        location.addAttribute(new Attribute("endIsPartial", "false"));

        if (srcItem.hasAttribute(locPrefix + "_strand")) {
            moveField(srcItem, location, locPrefix + "_strand", "strand");
        }
        if (srcItem.hasAttribute("phase")) {
            moveField(srcItem, location, "phase", "phase");
        }
        if (srcItem.hasAttribute("end_phase")) {
            moveField(srcItem, location, "end_phase", "endPhase");
        }
        if (srcItem.hasAttribute(locPrefix + "_ori")) {
            moveField(srcItem, location, locPrefix + "_ori", "strand");
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
        result.addReference(getEnsemblRef());
        ReferenceList evidence = new ReferenceList("evidence",
                                     Arrays.asList(new Object[] {result.getIdentifier(),
                                                   getEnsemblDb().getIdentifier()}));
        tgtItem.addCollection(evidence);
        return result;
    }


    private Item getSuperContig(String name, String chrId, int start, int end) {
        Item supercontig = (Item) supercontigs.get(name);
        if (supercontig == null) {
            supercontig = createItem(tgtNs + "SuperContig", "");
            Item chrLoc = createItem(tgtNs + "Location", "");
            chrLoc.addAttribute(new Attribute("start", "" + Integer.MAX_VALUE));
            chrLoc.addAttribute(new Attribute("end", "" + Integer.MIN_VALUE));
            chrLoc.addAttribute(new Attribute("startIsPartial", "false"));
            chrLoc.addAttribute(new Attribute("endIsPartial", "false"));
            chrLoc.addReference(new Reference("subject", supercontig.getIdentifier()));
            chrLoc.addReference(new Reference("object", chrId));

            supercontig.addAttribute(new Attribute("name", name));
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


    private Set processExon(Item srcItem, Item exon, Item thisLoc) {
        Set ret = new HashSet();

        if (srcItem.hasAttribute("nonUniqueId")) {
            String nonUniqueId = srcItem.getAttribute("nonUniqueId").getValue();

            if (exonLocs.containsKey(nonUniqueId)) {
                // already seen exon
                // a) throw away second copy
                // b) locations are partial
                // c) make sure the exon in exons map has lowest identifier

                Item chosenExon = null;
                Item otherExon = null;
                String oldIdentifier = ((Item) exons.get(nonUniqueId)).getIdentifier();
                if (Integer.parseInt(oldIdentifier.substring(oldIdentifier.indexOf("_") + 1))
                    > Integer.parseInt(exon.getIdentifier()
                                     .substring(exon.getIdentifier().indexOf("_") + 1))) {
                    // exon in map needs all parents in objects collection
                    otherExon = (Item) exons.get(nonUniqueId);
                    chosenExon = exon;
                    exons.put(nonUniqueId, exon);
                } else {
                    chosenExon = (Item) exons.get(nonUniqueId);
                    otherExon = exon;
                }
                Set objects = new HashSet();
                objects.addAll(chosenExon.getCollection("objects").getRefIds());
                objects.addAll(otherExon.getCollection("objects").getRefIds());
                objects.add(thisLoc.getIdentifier());
                chosenExon.addCollection(new ReferenceList("objects", new ArrayList(objects)));

                Item prevLoc = (Item) exonLocs.get(nonUniqueId);
                exonLocs.remove(nonUniqueId);
                Item firstLoc = null;
                Item secondLoc = null;
                if (srcItem.getAttribute("sticky_rank").getValue().equals("1")) {
                    firstLoc = thisLoc;
                    secondLoc = prevLoc;
                } else {
                    firstLoc = prevLoc;
                    secondLoc = thisLoc;
                }

                int lengthOnFirst = (Integer.parseInt(firstLoc.getAttribute("end").getValue())
                                - Integer.parseInt(firstLoc.getAttribute("start").getValue())) + 1;
                int lengthOnSecond = (Integer.parseInt(secondLoc.getAttribute("end").getValue())
                                - Integer.parseInt(secondLoc.getAttribute("start").getValue())) + 1;

                // add subjectStart and subjectEnd to firstLoc
                firstLoc.setClassName(tgtNs + "PartialLocation");
                firstLoc.addAttribute(new Attribute("subjectStart", "1"));
                firstLoc.addAttribute(new Attribute("subjectEnd", "" + lengthOnFirst));
                firstLoc.getAttribute("endIsPartial").setValue("true");
                firstLoc.addReference(new Reference("subject", chosenExon.getIdentifier()));

                // add subjectStart and subjectEnd to secondLoc
                secondLoc.setClassName(tgtNs + "PartialLocation");
                secondLoc.addAttribute(new Attribute("subjectStart", "" + (lengthOnFirst + 1)));
                secondLoc.addAttribute(new Attribute("subjectEnd", "" + (lengthOnFirst
                                                                         + lengthOnSecond)));
                secondLoc.getAttribute("startIsPartial").setValue("true");
                secondLoc.addReference(new Reference("subject", chosenExon.getIdentifier()));


                ret.add(firstLoc);
                ret.add(secondLoc);
            } else {
                // store this exon and keep Location
                exonLocs.put(nonUniqueId, thisLoc);
                exons.put(nonUniqueId, exon);
            }
        } else {
            ret.add(thisLoc);
            exons.put(exon.getIdentifier(), exon);
        }
        return ret;
    }

    private Collection createSuperContigs() {
        Set results = new HashSet();
        Iterator i = supercontigs.values().iterator();
        while (i.hasNext()) {
            Item sc = (Item) i.next();
            results.add(sc);
            results.add((Item) scLocs.get(sc.getAttribute("name").getValue()));
        }
        return results;
    }

    private Set setProteinIdentifiers(Item srcItem, Item tgtItem) throws ObjectStoreException {
        Set synonyms = new HashSet();
        String srcNs = OntologyUtil.getNamespaceFromURI(srcItem.getClassName());
        String value = srcItem.getIdentifier().substring(srcItem.getIdentifier().indexOf("_") + 1);
        Set constraints = new HashSet();
        constraints.add(new FieldNameAndValue("className", srcNs + "object_xref", false));
        constraints.add(new FieldNameAndValue("ensembl_id", value, false));
        Iterator objectXrefs = srcItemReader.getItemsByDescription(constraints).iterator();
        // set specific ids and add synonyms
        //Iterator i = objectXrefs.iterator();
        while (objectXrefs.hasNext()) {
            Item objectXref = ItemHelper.convert(
                                  (org.flymine.model.fulldata.Item) objectXrefs.next());
            if (objectXref.getAttribute("ensembl_object_type").getValue().equals("Translation")) {
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
                        Attribute a = new Attribute("swissProtId", accession);
                        tgtItem.addAttribute(a);
                        tgtItem.addAttribute(new Attribute("swissProtId", accession));
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("synonym", accession));
                        synonym.addReference(getSwissprotRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("SPTREMBL")) {
                        tgtItem.addAttribute(new Attribute("spTREMBLId", accession));
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("synonym", accession));
                        synonym.addReference(getTremblRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("protein_id")) {
                        tgtItem.addAttribute(new Attribute("emblId", accession));
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("synonym", accession));
                        synonym.addReference(getEmblRef());
                        synonyms.add(synonym);
                    } else if (dbname.equals("prediction_SPTREMBL")) {
                        tgtItem.addAttribute(new Attribute("emblId", accession));
                        Item synonym = createItem(tgtNs + "Synonym", "");
                        addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                        synonym.addAttribute(new Attribute("synonym", accession));
                        synonym.addReference(getEmblRef());
                        synonyms.add(synonym);
                    }
                }
            }
        }
        if (tgtItem.hasAttribute("swissProtId")) {
            tgtItem.addAttribute(new Attribute("identifier",
                                               tgtItem.getAttribute("swissProtId").getValue()));
        } else if (tgtItem.hasAttribute("spTREMBLId")) {
            tgtItem.addAttribute(new Attribute("identifier",
                                               tgtItem.getAttribute("spTREMBLId").getValue()));
        } else if (tgtItem.hasAttribute("emblId")) {
            tgtItem.addAttribute(new Attribute("identifier",
                                               tgtItem.getAttribute("emblId").getValue()));
        } else {
            // there was no protein identifier so use ensembl translation_stable_id
            //String stableIdIdentifier = (String) srcItem.getCollection("translation_stable_ids")
            //    .getRefIds().iterator().next();
            //Item stableId = ItemHelper.convert(srcItemReader.getItemById(stableIdIdentifier));
            //tgtItem.addAttribute(new Attribute("identifier",
            //                                   stableId.getAttribute("stable_id").getValue()));


            // temporarily use item identifier as no reference to stable_id
            tgtItem.addAttribute(new Attribute("identifier", srcItem.getIdentifier()));
        }
        return synonyms;
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

    private Item getOrganism() {
        if (organism == null) {
            organism = createItem(tgtNs + "Organism", "");
            Attribute a1 = new Attribute("name", orgName);
            organism.addAttribute(a1);
            Attribute a2 = new Attribute("shortName", orgShortName);
            organism.addAttribute(a2);
            Attribute a3 = new Attribute("taxonId", orgTaxonId);
            organism.addAttribute(a3);
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
        String orgName = args[5];
        String orgShortName = args[6];
        String orgTaxonId = args[7];

        Map paths = new HashMap();
        ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor();
        desc.addConstraint(new ItemPrefetchConstraintDynamic("repeat_consensus", "identifier"));
        paths.put("http://www.flymine.org/model/ensembl#repeat_feature",
                Collections.singleton(desc));
        desc = new ItemPrefetchDescriptor();
        desc.addConstraint(new ItemPrefetchConstraintDynamic("display_xref", "identifier"));
        paths.put("http://www.flymine.org/model/ensembl#transcript", Collections.singleton(desc));
        desc = new ItemPrefetchDescriptor();
        desc.addConstraint(new ItemPrefetchConstraintDynamic("display_xref", "identifier"));
        paths.put("http://www.flymine.org/model/ensembl#gene", Collections.singleton(desc));
        desc = new ItemPrefetchDescriptor();
        desc.addConstraint(new ItemPrefetchConstraintDynamic("dna", "identifier"));
        paths.put("http://www.flymine.org/model/ensembl#contig", Collections.singleton(desc));

        
        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc, paths);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new BufferedItemWriter(new ObjectStoreItemWriter(oswTgt));

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new EnsemblDataTranslator(srcItemReader, model, namespace, orgName,
                                                      orgShortName, orgTaxonId);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
