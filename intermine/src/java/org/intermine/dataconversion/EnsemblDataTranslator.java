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
import java.util.Set;

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

/**
 * Convert Ensembl data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to FlyMine OWL definition.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class EnsemblDataTranslator extends DataTranslator
{
    private Item ensemblDb;
    private Reference ensemblRef;
    private Item emblDb;
    private Reference emblRef;
    private Map supercontigs = new HashMap();
    private Map scLocs = new HashMap();

    /**
     * @see DataTranslator#DataTranslator
     */
    public EnsemblDataTranslator(ItemReader srcItemReader, OntModel model, String ns) {
        super(srcItemReader, model, ns);
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter) throws ObjectStoreException, FlyMineException {
        tgtItemWriter.store(ItemHelper.convert(getEnsemblDb()));
        tgtItemWriter.store(ItemHelper.convert(getEmblDb()));
        super.translate(tgtItemWriter);
        Iterator i = createSuperContigs().iterator();
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
                Item tgtItem = (Item) i.next();
                if ("karyotype".equals(className)) {
                    result.add(createLocation(srcItem, tgtItem, "chromosome", "chr", true));
                } else if ("exon".equals(className)) {
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                } else if ("simple_feature".equals(className)) {
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                } else if ("prediction_transcript".equals(className)) {
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                } else if ("repeat_feature".equals(className)) {
                    result.add(createAnalysisResult(srcItem, tgtItem));
                    result.add(createLocation(srcItem, tgtItem, "contig", "contig", true));
                    promoteField(tgtItem, srcItem, "consensus", "repeat_consensus",
                                 "repeat_consensus");
                    promoteField(tgtItem, srcItem, "type", "repeat_consensus", "repeat_class");
                    promoteField(tgtItem, srcItem, "name", "repeat_consensus", "repeat_name");
                } else if ("gene".equals(className)) {
                    addReferencedItem(tgtItem, getEnsemblDb(), "evidence", true, "", false);
                    promoteField(tgtItem, srcItem, "name", "display_xref", "display_label");

                } else if ("contig".equals(className)) {
                    Item relation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, relation, "subjects", true, "object", false);
                    moveField(srcItem, relation, "clone", "subject");
                    result.add(relation);
                    promoteField(tgtItem, srcItem, "residues", "dna", "sequence");
                } else if ("transcript".equals(className)) {
                    Item geneRelation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, geneRelation, "objects", true, "subject", false);
                    moveField(srcItem, geneRelation, "gene", "object");
                    result.add(geneRelation);
                    Item transRelation = createItem(tgtNs + "SimpleRelation", "");
                    addReferencedItem(tgtItem, transRelation, "subjects", true, "object", false);
                    moveField(srcItem, transRelation, "translation", "subject");
                    result.add(transRelation);
                    promoteField(tgtItem, srcItem, "name", "display_xref", "display_label");

                // stable_ids become syonyms, need ensembl Database as source
                } else if (className.endsWith("_stable_id")) {
                    tgtItem.addReference(getEnsemblRef());
                } else if ("clone".equals(className)) {
                    // clone embl_acc needs to be a synonym in embl database

                    Item synonym = createItem(tgtNs + "Synonym", "");
                    addReferencedItem(tgtItem, synonym, "synonyms", true, "subject", false);
                    moveField(srcItem, synonym, "embl_acc", "synonym");
                    synonym.addReference(getEmblRef());
                    result.add(synonym);
                }
                result.add(tgtItem);
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

        if (srcItem.hasAttribute(locPrefix + "_strand")) {
            moveField(srcItem, location, locPrefix + "_strand", "strand");
        }
        if (srcItem.hasAttribute("phase")) {
            moveField(srcItem, location, "phase", "phase");
        }
        if (srcItem.hasAttribute("end_phase")) {
            moveField(srcItem, location, "end_phase", "end_phase");
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
            chrLoc.addReference(new Reference("subject", supercontig.getIdentifier()));
            chrLoc.addReference(new Reference("object", chrId));

            supercontig.addAttribute(new Attribute("name", name));
            ReferenceList subjects = new ReferenceList();
            subjects.setName("subjects");
            supercontig.addCollection(subjects);
            supercontig.addCollection(new ReferenceList("objects",
                                      Collections.singletonList(chrLoc.getIdentifier())));
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
            results.add((Item) scLocs.get(sc.getAttribute("name").getValue()));
        }
        return results;
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

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new BufferedItemWriter(new ObjectStoreItemWriter(oswTgt));

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new EnsemblDataTranslator(srcItemReader, model, namespace);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
