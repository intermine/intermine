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

import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.OntModel;

import org.flymine.FlyMineException;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.ontology.OntologyUtil;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Convert Ensembl data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to FlyMine OWL definition.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class EnsemblDataTranslator extends DataTranslator
{
    protected int newItemId = -1;
    
    /**
     * @see DataTranslator#DataTranslator
     */
    public EnsemblDataTranslator(ItemReader srcItemReader, OntModel model, String ns) {
        super(srcItemReader, model, ns);
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem) throws ObjectStoreException, FlyMineException {
        Collection result = new HashSet();
        for (Iterator i = super.translateItem(srcItem).iterator(); i.hasNext();) {
            Item item = (Item) i.next();
            result.add(item);
            String className = OntologyUtil.getFragmentFromURI(srcItem.getClassName());
            if ("karyotype".equals(className)) {
                result.add(translate(srcItem, item, "chromosome", "chr"));
            } else if ("assembly".equals(className)) {
                result.add(translate(srcItem, item, "chromosome", "chr"));
                result.add(translate(srcItem, item, "contig", "contig"));
            } else if ("exon".equals(className)) {
                result.add(translate(srcItem, item, "contig", "contig"));
            } else if ("simple_feature".equals(className)) {
                result.add(translate(srcItem, item, "contig", "contig"));
            } else if ("prediction_transcript".equals(className)) {
                result.add(translate(srcItem, item, "contig", "contig"));
            } else if ("repeat_feature".equals(className)) {
                result.add(translate(srcItem, item, "contig", "contig"));
            } else if ("dna_align_feature".equals(className)
                       || "protein_align_feature".equals(className)) {
                result.addAll(translate(srcItem, item));
            }
        }
        return result;
    }

    /**
     * Translate a "located" Item into an Item and a location
     * @param srcItem the source Item
     * @param item the target Item (after translation)
     * @param idPrefix the id prefix for this class
     * @param locPrefix the start, end and strand prefix for this class
     * @return the location
     */
    protected Item translate(Item srcItem, Item item, String idPrefix, String locPrefix) {
        String namespace = OntologyUtil.getNamespaceFromURI(item.getClassName());

        Item location = new Item();
        location.setIdentifier("" + (newItemId--));
        location.setClassName(namespace + "Location");
        location.setImplementations("");
        Attribute start = new Attribute();
        start.setName("start");
        start.setValue(srcItem.getAttribute(locPrefix + "_start").getValue());
        location.addAttribute(start);
        Attribute end = new Attribute();
        end.setName("end");
        end.setValue(srcItem.getAttribute(locPrefix + "_end").getValue());
        location.addAttribute(end);
        if (srcItem.hasAttribute(locPrefix + "_strand")) {
            Attribute strand = new Attribute();
            strand.setName("strand");
            strand.setValue(srcItem.getAttribute(locPrefix + "_strand").getValue());
            location.addAttribute(strand);
        }
        if (srcItem.hasAttribute("phase")) {
            Attribute phase = new Attribute();
            phase.setName("phase");
            phase.setValue(srcItem.getAttribute("phase").getValue());
            location.addAttribute(phase);
        }
        if (srcItem.hasAttribute("end_phase")) {
            Attribute endPhase = new Attribute();
            endPhase.setName("end_phase");
            endPhase.setValue(srcItem.getAttribute("end_phase").getValue());
            location.addAttribute(endPhase);
        }
        Reference subj = new Reference();
        subj.setName("subject");
        subj.setRefId(item.getIdentifier());
        location.addReference(subj);
        Reference obj = new Reference();
        obj.setName("object");
        obj.setRefId(srcItem.getReference(idPrefix).getRefId());
        location.addReference(obj);

        return location;
    }

    /**
     * @see DataTranslator#translate
     */
    protected Collection translate(Item srcItem, Item item) {
        Collection result = new HashSet();

        String namespace = OntologyUtil.getNamespaceFromURI(item.getClassName());

        Item location1 = new Item();
        location1.setIdentifier("" + (newItemId--));
        location1.setClassName(namespace + "Location");
        location1.setImplementations("");
        Attribute start1 = new Attribute();
        start1.setName("start");
        start1.setValue(srcItem.getAttribute("contig_start").getValue());
        location1.addAttribute(start1);
        Attribute end1 = new Attribute();
        end1.setName("end");
        end1.setValue(srcItem.getAttribute("contig_end").getValue());
        location1.addAttribute(end1);
        Attribute strand1 = new Attribute();
        strand1.setName("strand");
        strand1.setValue(srcItem.getAttribute("contig_strand").getValue());
        location1.addAttribute(strand1);
        Reference subj1 = new Reference();
        subj1.setName("subject");
        subj1.setRefId(item.getIdentifier());
        location1.addReference(subj1);
        Reference obj1 = new Reference();
        obj1.setName("object");
        obj1.setRefId(srcItem.getReference("contig").getRefId());
        location1.addReference(obj1);
        result.add(location1);

        Item location2 = new Item();
        location2.setIdentifier("" + (newItemId--));
        location2.setClassName(namespace + "Location");
        location2.setImplementations("");
        Attribute start2 = new Attribute();
        start2.setName("start");
        start2.setValue(srcItem.getAttribute("hit_start").getValue());
        location2.addAttribute(start2);
        Attribute end2 = new Attribute();
        end2.setName("end");
        end2.setValue(srcItem.getAttribute("hit_end").getValue());
        location2.addAttribute(end2);
        Attribute strand2 = new Attribute();
        strand2.setName("strand");
        strand2.setValue(srcItem.getAttribute("hit_strand").getValue());
        location2.addAttribute(strand2);
        Reference subj2 = new Reference();
        subj2.setName("subject");
        subj2.setRefId(srcItem.getReference("contig").getRefId());
        location2.addReference(subj2);
        Reference obj2 = new Reference();
        obj2.setName("object");
        obj2.setRefId(item.getIdentifier());
        location2.addReference(obj2);
        result.add(location2);

        Item blastResult = new Item();
        blastResult.setIdentifier("" + (newItemId--));
        blastResult.setClassName(namespace + "BlastResult");
        blastResult.setImplementations("");
        Attribute score = new Attribute();
        score.setName("score");
        score.setValue(srcItem.getAttribute("score").getValue());
        blastResult.addAttribute(score);
        Attribute eValue = new Attribute();
        eValue.setName("evalue");
        eValue.setValue(srcItem.getAttribute("evalue").getValue());
        blastResult.addAttribute(eValue);
        Attribute percIdent = new Attribute();
        percIdent.setName("percIdent");
        percIdent.setValue(srcItem.getAttribute("perc_ident").getValue());
        blastResult.addAttribute(percIdent);
        Reference bioEntity = new Reference();
        bioEntity.setName("bioEntity");
        bioEntity.setRefId(item.getIdentifier());
        blastResult.addReference(bioEntity);
        result.add(blastResult);

        ReferenceList analysisResults = new ReferenceList();
        analysisResults.setName("analysisResults");
        analysisResults.addRefId(blastResult.getIdentifier());
        item.addCollection(analysisResults);
        
        return result;
    }
}
