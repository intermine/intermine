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

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.flymine.ontology.OntologyUtil;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.ItemHelper;

public class EnsemblDataTranslatorTest extends TestCase {
    private String srcNs = "http://www.flymine.org/source#";
    private String tgtNs = "http://www.flymine.org/target#";
    protected Map itemMap;
    
    public void setUp() throws Exception {
        itemMap = new HashMap();
    }

    // [ exon --> contig ] -> [ Exon <-- Location --> Contig ]
    public void testTranslateItemLocated() throws Exception {
        Item srcExon = new Item();
        srcExon.setIdentifier("0");
        srcExon.setClassName(srcNs + "exon");
        srcExon.setImplementations("");
        Attribute start = new Attribute();
        start.setName("contig_start");
        start.setValue("5");
        srcExon.addAttribute(start);
        Attribute end = new Attribute();
        end.setName("contig_end");
        end.setValue("7");
        srcExon.addAttribute(end);
        Attribute strand = new Attribute();
        strand.setName("contig_strand");
        strand.setValue("9");
        srcExon.addAttribute(strand);
        Attribute phase = new Attribute();
        phase.setName("phase");
        phase.setValue("11");
        srcExon.addAttribute(phase);
        Attribute endPhase = new Attribute();
        endPhase.setName("end_phase");
        endPhase.setValue("13");
        srcExon.addAttribute(endPhase);     
        Reference contig = new Reference();
        contig.setName("contig");
        contig.setRefId("1");
        srcExon.addReference(contig);

        Item tgtExon = new Item();
        tgtExon.setIdentifier("0");
        tgtExon.setClassName(tgtNs + "Exon");
        tgtExon.setImplementations("");

        Item tgtLocation = new Item();
        tgtLocation.setIdentifier("-1");
        tgtLocation.setClassName(tgtNs + "Location");
        tgtLocation.setImplementations("");
        Attribute locStart = new Attribute();
        locStart.setName("start");
        locStart.setValue("5");
        tgtLocation.addAttribute(locStart);
        Attribute locEnd = new Attribute();
        locEnd.setName("end");
        locEnd.setValue("7");
        tgtLocation.addAttribute(locEnd);
        Attribute locStrand = new Attribute();
        locStrand.setName("strand");
        locStrand.setValue("9");
        tgtLocation.addAttribute(locStrand);
        Attribute locPhase = new Attribute();
        locPhase.setName("phase");
        locPhase.setValue("11");
        tgtLocation.addAttribute(locPhase);
        Attribute locEndPhase = new Attribute();
        locEndPhase.setName("end_phase");
        locEndPhase.setValue("13");
        tgtLocation.addAttribute(locEndPhase);
        Reference locSubject = new Reference();
        locSubject.setName("subject");
        locSubject.setRefId(tgtExon.getIdentifier());
        tgtLocation.addReference(locSubject);
        Reference locObject = new Reference();
        locObject.setName("object");
        locObject.setRefId(contig.getRefId());
        tgtLocation.addReference(locObject);

        ItemWriter srcWriter = new MockItemWriter(itemMap);
        srcWriter.store(ItemHelper.convert(srcExon));
        DataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap), getFlyMineOwl(), tgtNs);
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        translator.translate(itemWriter);

        Set expected = new HashSet();
        expected.add(tgtExon);
        expected.add(tgtLocation);

        assertEquals(expected, itemWriter.getItems());
    }

    // [ dna_align_feature --> contig ] -> [ nucleotide_match <-- Locations --> Contig ]
    public void testTranslateItemLocated2() throws Exception {
        Item feature = new Item();
        feature.setIdentifier("0");
        feature.setClassName(srcNs + "dna_align_feature");
        feature.setImplementations("");
        Attribute start = new Attribute();
        start.setName("contig_start");
        start.setValue("5");
        feature.addAttribute(start);
        Attribute end = new Attribute();
        end.setName("contig_end");
        end.setValue("7");
        feature.addAttribute(end);
        Attribute strand = new Attribute();
        strand.setName("contig_strand");
        strand.setValue("9");
        feature.addAttribute(strand);
        Attribute hitStart = new Attribute();
        hitStart.setName("hit_start");
        hitStart.setValue("11");
        feature.addAttribute(hitStart);
        Attribute hitEnd = new Attribute();
        hitEnd.setName("hit_end");
        hitEnd.setValue("13");
        feature.addAttribute(hitEnd);
        Attribute hitStrand = new Attribute();
        hitStrand.setName("hit_strand");
        hitStrand.setValue("15");
        feature.addAttribute(hitStrand);
        Attribute score = new Attribute();
        score.setName("score");
        score.setValue("17");
        feature.addAttribute(score);
        Attribute evalue = new Attribute();
        evalue.setName("evalue");
        evalue.setValue("19");
        feature.addAttribute(evalue);
        Attribute percIdent = new Attribute();
        percIdent.setName("perc_ident");
        percIdent.setValue("21");
        feature.addAttribute(percIdent);
        Reference contig = new Reference();
        contig.setName("contig");
        contig.setRefId("1");
        feature.addReference(contig);

        Item tgtMatch = new Item();
        tgtMatch.setIdentifier("0");
        tgtMatch.setClassName(tgtNs + "NucleotideMatch");
        tgtMatch.setImplementations("");
        ReferenceList tgtResults = new ReferenceList();
        tgtResults.setName("analysisResults");
        tgtResults.addRefId("-3");
        tgtMatch.addCollection(tgtResults);

        Item tgtLocation1 = new Item();
        tgtLocation1.setIdentifier("-1");
        tgtLocation1.setClassName(tgtNs + "Location");
        tgtLocation1.setImplementations("");
        Attribute tgtStart1 = new Attribute();
        tgtStart1.setName("start");
        tgtStart1.setValue("5");
        tgtLocation1.addAttribute(tgtStart1);
        Attribute tgtEnd1 = new Attribute();
        tgtEnd1.setName("end");
        tgtEnd1.setValue("7");
        tgtLocation1.addAttribute(tgtEnd1);
        Attribute tgtStrand1 = new Attribute();
        tgtStrand1.setName("strand");
        tgtStrand1.setValue("9");
        tgtLocation1.addAttribute(tgtStrand1);
        Reference tgtSubject1 = new Reference();
        tgtSubject1.setName("subject");
        tgtSubject1.setRefId(tgtMatch.getIdentifier());
        tgtLocation1.addReference(tgtSubject1);
        Reference tgtObject1 = new Reference();
        tgtObject1.setName("object");
        tgtObject1.setRefId(contig.getRefId());
        tgtLocation1.addReference(tgtObject1);

        Item tgtLocation2 = new Item();
        tgtLocation2.setIdentifier("-2");
        tgtLocation2.setClassName(tgtNs + "Location");
        tgtLocation2.setImplementations("");
        Attribute tgtStart2 = new Attribute();
        tgtStart2.setName("start");
        tgtStart2.setValue("11");
        tgtLocation2.addAttribute(tgtStart2);
        Attribute tgtEnd2 = new Attribute();
        tgtEnd2.setName("end");
        tgtEnd2.setValue("13");
        tgtLocation2.addAttribute(tgtEnd2);
        Attribute tgtStrand2 = new Attribute();
        tgtStrand2.setName("strand");
        tgtStrand2.setValue("15");
        tgtLocation2.addAttribute(tgtStrand2);
        Reference tgtSubject2 = new Reference();
        tgtSubject2.setName("subject");
        tgtSubject2.setRefId(contig.getRefId());
        tgtLocation2.addReference(tgtSubject2);
        Reference tgtObject2 = new Reference();
        tgtObject2.setName("object");
        tgtObject2.setRefId(tgtMatch.getIdentifier());
        tgtLocation2.addReference(tgtObject2);

        Item tgtBlast = new Item();
        tgtBlast.setIdentifier("-3");
        tgtBlast.setClassName(tgtNs + "BlastResult");
        tgtBlast.setImplementations("");
        Attribute tgtScore = new Attribute();
        tgtScore.setName("score");
        tgtScore.setValue("17");
        tgtBlast.addAttribute(tgtScore);
        Attribute tgtEvalue = new Attribute();
        tgtEvalue.setName("evalue");
        tgtEvalue.setValue("19");
        tgtBlast.addAttribute(tgtEvalue);
        Attribute tgtPercIdent = new Attribute();
        tgtPercIdent.setName("percIdent");
        tgtPercIdent.setValue("21");
        tgtBlast.addAttribute(tgtPercIdent);
        Reference tgtEntity = new Reference();
        tgtEntity.setName("bioEntity");
        tgtEntity.setRefId("0");
        tgtBlast.addReference(tgtEntity);

        ItemWriter srcWriter = new MockItemWriter(itemMap);
        srcWriter.store(ItemHelper.convert(feature));
        DataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap), getFlyMineOwl(), tgtNs);
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        translator.translate(itemWriter);

        Set expected = new HashSet();
        expected.add(tgtMatch);
        expected.add(tgtBlast);
        expected.add(tgtLocation1);
        expected.add(tgtLocation2);

        assertEquals(expected, itemWriter.getItems());
    }

    protected OntModel getFlyMineOwl() {
        String ENDL = System.getProperty("line.separator");

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL
            + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL + ENDL
            + "@prefix null:  <http://flymine.org/model/null#> ." + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL + ENDL

            + ENDL
            + ":Exon a owl:Class; owl:equivalentClass src:exon." + ENDL
            + "null:exon__contig_start a owl:DatatypeProperty; owl:equivalentProperty src:exon__contig_start." + ENDL
            + "null:exon__contig_end a owl:DatatypeProperty; owl:equivalentProperty src:exon__contig_end." + ENDL
            + "null:exon__contig_strand a owl:DatatypeProperty; owl:equivalentProperty src:exon__contig_strand." + ENDL
            + "null:exon__phase a owl:DatatypeProperty; owl:equivalentProperty src:exon__phase." + ENDL
            + "null:exon__end_phase a owl:DatatypeProperty; owl:equivalentProperty src:exon__end_phase." + ENDL
            + "null:exon__contig a owl:ObjectProperty; owl:equivalentProperty src:exon__contig." + ENDL

            + ":NucleotideMatch a owl:Class; owl:equivalentClass src:dna_align_feature." + ENDL
            + "null:dna_align_feature__contig_start a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__contig_start." + ENDL
            + "null:dna_align_feature__contig_end a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__contig_end." + ENDL
            + "null:dna_align_feature__contig_strand a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__contig_strand." + ENDL
            + "null:dna_align_feature__hit_start a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__hit_start." + ENDL
            + "null:dna_align_feature__hit_end a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__hit_end." + ENDL
            + "null:dna_align_feature__hit_strand a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__hit_strand." + ENDL
            + "null:dna_align_feature__score a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__score." + ENDL
            + "null:dna_align_feature__evalue a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__evalue." + ENDL
            + "null:dna_align_feature__perc_ident a owl:DatatypeProperty; owl:equivalentProperty src:dna_align_feature__perc_ident." + ENDL
            + "null:dna_align_feature__contig a owl:ObjectProperty; owl:equivalentProperty src:dna_align_feature__contig." + ENDL;


        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");
        return ont;
    }
}
