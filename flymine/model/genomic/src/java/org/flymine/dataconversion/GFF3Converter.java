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

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.util.TypeUtil;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.objectstore.ObjectStoreException;

import org.flymine.io.gff3.GFF3Parser;
import org.flymine.io.gff3.GFF3Record;

import org.apache.log4j.Logger;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class GFF3Converter
{
    private static final Logger LOG = Logger.getLogger(GFF3Converter.class);


    private Item organism;
    private Reference orgRef;
    protected GFF3Parser parser;
    protected ItemWriter writer;
    private String seqClsName;
    private String orgAbbrev;
    private Item infoSource;
    private String targetNameSpace;
    private int itemid = 0;
    private Map analyses = new HashMap();
    private Map seqs = new HashMap();


    /**
     * Constructor
     * @param parser GFF3Parser;
     * @param writer ItemWriter
     * @param seqClsName sequenceClassName
     * @param orgAbbrev organismAbbreviation. HS this case
     * @param infoSourceTitle title for infoSource
     * @param targetNameSpace target namesace
     */
    public GFF3Converter(GFF3Parser parser, ItemWriter writer,
           String seqClsName, String orgAbbrev, String infoSourceTitle, String targetNameSpace) {

        this.parser = parser;
        this.writer = writer;
        this.seqClsName = seqClsName;
        this.orgAbbrev = orgAbbrev;
        this.targetNameSpace = targetNameSpace;

        this.organism = getOrganism();
        this.infoSource = createItem(targetNameSpace + "InfoSource", "");
        infoSource.addAttribute(new Attribute("title", infoSourceTitle));
    }

    /**
     * parse a bufferedReader and process GFF3 record
     * @param bReader BufferedReader
     * @throws java.io.IOException if an error occurs reading GFF
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void parse(BufferedReader bReader) throws java.io.IOException, ObjectStoreException {
        List list = new ArrayList();
        GFF3Record record;
        long start, now, opCount;

        list = parser.parse(bReader);
        writer.store(ItemHelper.convert(organism));
        writer.store(ItemHelper.convert(infoSource));

        System.err.println("Total " + list.size() + " lines in file");
        LOG.info("Total " + list.size() + " lines in file");
        opCount = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < list.size(); i++) {
            record = (GFF3Record) list.get(i);
            process(record);
            opCount++;
            if (opCount % 1000 == 0) {
                now = System.currentTimeMillis();
                System.err.println("processed " + opCount + " lines --took " + (now - start) + " ms");
                LOG.info("processed " + opCount + " lines --took " + (now - start) + " ms");
                start = System.currentTimeMillis();
            }
        }

        // write ComputationalAnalysis items
        Iterator iter = analyses.values().iterator();
        while (iter.hasNext()) {
            writer.store(ItemHelper.convert((Item) iter.next()));
        }

        // write seq items
        iter = seqs.values().iterator();
        while (iter.hasNext()) {
            writer.store(ItemHelper.convert((Item) iter.next()));
        }
    }

    /**
     * process GFF3 record and give a xml presentation
     * @param record GFF3Record
     * @throws ObjectStoreException if an error occurs storing items
     */
    public void process(GFF3Record record) throws ObjectStoreException {
        Set result = new HashSet();

        Item seq = getSeq(record.getSequenceID());

        String term = record.getType();
        String className = TypeUtil.javaiseClassName(term);
        Item feature = createItem(targetNameSpace + className, "");
        if (record.getId() != null) {
            feature.addAttribute(new Attribute("identifier", record.getId()));
        }
        if (record.getName() != null) {
            feature.addAttribute(new Attribute("name", record.getName()));
        }
        feature.addReference(getOrgRef());

        Item location = createItem(targetNameSpace + "Location", "");
        location.addAttribute(new Attribute("start", String.valueOf(record.getStart())));
        location.addAttribute(new Attribute("end", String.valueOf(record.getEnd())));
        if (record.getStrand() == null ||record.getStrand().equals(".")) {
            location.addAttribute(new Attribute("strand", "0"));
        } else if (record.getStrand().equals("+")) {
            location.addAttribute(new Attribute("strand", "1"));
        } else if (record.getStrand().equals("-")) {
            location.addAttribute(new Attribute("strand", "-1"));
        }
        if (record.getPhase() != null) {
            location.addAttribute(new Attribute("phase", record.getPhase()));
        }
        location.addReference(new Reference("object", seq.getIdentifier()));
        location.addReference(new Reference("subject", feature.getIdentifier()));
        location.addCollection(new ReferenceList("evidence",
                            Arrays.asList(new Object[] {infoSource.getIdentifier()})));
        result.add(location);


        Item computationalResult = createItem(targetNameSpace + "ComputationalResult", "");
        if (String.valueOf(record.getScore()) != null) {
            computationalResult.addAttribute(new Attribute("score",
                                             String.valueOf(record.getScore())));
        }
        if (record.getSource() != null) {
            Item computationalAnalysis = getComputationalAnalysis(record.getSource());
            computationalResult.addReference(new Reference("analysis",
                                             computationalAnalysis.getIdentifier()));
        }
        result.add(computationalResult);

        ReferenceList evidence = new ReferenceList("evidence", Arrays.asList(new Object[]
                {computationalResult.getIdentifier(), infoSource.getIdentifier()}));
        feature.addCollection(evidence);
        result.add(feature);

        try {
            Iterator iter = result.iterator();
            while (iter.hasNext()) {
                writer.store(ItemHelper.convert((Item) iter.next()));
            }
        } catch (ObjectStoreException e) {
            LOG.error("Problem writing item to the itemwriter");
            throw e;
        }
    }

    /**
     * Perform any necessary clean-up after post-conversion
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
    }

    /**
     * @return organism item, for homo_sapiens, abbreviation is HS
     */
    private Item getOrganism() {
        if (organism == null) {
            organism = createItem(targetNameSpace + "Organism", "");
            organism.addAttribute(new Attribute("abbreviation", orgAbbrev));
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
     * @return ComputationalAnalysis item created/from map
     */
    private Item getComputationalAnalysis(String algorithm) {
        Item analysis = (Item) analyses.get(algorithm);
        if (analysis == null) {
            analysis = createItem(targetNameSpace + "ComputationalAnalysis", "");
            analysis.addAttribute(new Attribute("algorithm", algorithm));
            analyses.put(algorithm, analysis);
        }
        return analysis;
    }

    /**
     * @return return/create item of class seqClsName for given identifier
     */
    private Item getSeq(String identifier) {
        Item seq = (Item) seqs.get(identifier);
        if (seq == null) {
            seq = createItem(targetNameSpace + seqClsName, "");
            seq.addAttribute(new Attribute("identifier", identifier));
            seqs.put(identifier, seq);
        }
        return seq;
    }

    /**
     * Create an item with given className, implementation and item identifier
     * @param className
     * @param implementations
     * @param identifier
     * @return the created item
     */
    private Item createItem(String className, String implementations) {
        Item item = new Item();
        item.setClassName(className);
        item.setImplementations(implementations);
        item.setIdentifier("0_" + itemid++);
        return item;
    }

}
