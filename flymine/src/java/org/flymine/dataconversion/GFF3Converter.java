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

import com.hp.hpl.jena.ontology.OntModel;

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
    protected static final String NAMESPACE = "http://www.flymine.org/model/genomic#";
    private Item organism;
    private Reference orgRef;
    private int itemid ;
    protected OntModel model;
    protected GFF3Parser parser;
    protected ItemWriter writer;
    private String seqClsName;
    private String orgAbbrev;
    private String infoSourceTitle;

    /**
     * Constructor
     * @param model ontologyModel
     * @param parser GFF3Parser;
     * @param writer ItemWriter
     * @param seqClsName sequenceClassName
     * @param orgAbbrev organismAbbreviation. HS this case
     * @param infoSourceTitle title for infoSource
     * @param itemid counter used as item identifier
     */
    public GFF3Converter(OntModel model, GFF3Parser parser, ItemWriter writer,
           String seqClsName, String orgAbbrev, String infoSourceTitle, int itemid) {
        this.model = model;
        this.parser = parser;
        this.writer = writer;
        this.seqClsName = seqClsName;
        this.orgAbbrev = orgAbbrev;
        this.infoSourceTitle = infoSourceTitle;
        this.itemid = itemid;

    }

    /**
     * parse a bufferedReader and process GFF3 record
     * @param bReader BufferedReader
     * @throws java.io.IOException if an error occurs during processing
     */
    public void parse(BufferedReader bReader) throws java.io.IOException {
        List list = new ArrayList();
        GFF3Record record;
        list = parser.parse(bReader);
        for (int i = 0; i < list.size(); i++) {
            record = (GFF3Record) list.get(i);
            process(record);
        }
    }

    /**
     * process GFF3 record and give a xml presentation
     * @param record GFF3Record
     */
    public void process(GFF3Record record) {
        Item infoSource = createItem(NAMESPACE + "InfoSource", "", itemid++);
        infoSource.addAttribute(new Attribute("title", infoSourceTitle));

        Item seq = createItem(NAMESPACE + seqClsName, "", itemid++);
        seq.addAttribute(new Attribute("identifier", record.getSequenceID()));
        seq.addReference(getOrgRef());

        String term = record.getType();
        String className = TypeUtil.javaiseClassName(term);
        Item feature = createItem(NAMESPACE + className, "", itemid++);
        if (record.getId() != null) {
            feature.addAttribute(new Attribute("identifier", record.getId()));
        }
        if (record.getName() != null) {
            feature.addAttribute(new Attribute("name", record.getName()));
        }
        feature.addReference(getOrgRef());

        Item location = createItem(NAMESPACE + "Location", "", itemid++);
        location.addAttribute(new Attribute("start", String.valueOf(record.getStart())));
        location.addAttribute(new Attribute("end", String.valueOf(record.getEnd())));
        if (record.getStrand().equals("+")) {
            location.addAttribute(new Attribute("strand", "1"));
        } else if (record.getStrand().equals("-")) {
            location.addAttribute(new Attribute("strand", "-1"));
        } else if (record.getStrand().equals(".")) {
            location.addAttribute(new Attribute("strand", "0"));
        }

        location.addReference(new Reference("object", seq.getIdentifier()));
        location.addReference(new Reference("subject", feature.getIdentifier()));
        location.addCollection(new ReferenceList("evidence",
                            Arrays.asList(new Object[] {infoSource.getIdentifier()})));

        Item computationalAnalysis = new Item();
        Item computationalResult = new Item();
        if (String.valueOf(record.getScore()) != null) {
            computationalAnalysis = createItem(NAMESPACE + "ComputationalAnalysis", "", itemid++);
            computationalAnalysis.addAttribute(new Attribute("algorithm",
                                                             record.getSource()));

            computationalResult = createItem(NAMESPACE + "ComputationalResult", "", itemid++);
            computationalResult.addAttribute(new Attribute("score",
                                    String.valueOf(record.getScore())));
            computationalResult.addReference(new Reference("analysis",
                                    computationalAnalysis.getIdentifier()));
            ReferenceList evidence = new ReferenceList("evidence", Arrays.asList(new Object[]
                    {computationalResult.getIdentifier(), infoSource.getIdentifier()}));
                feature.addCollection(evidence);
            }

        try {
            writer.store(ItemHelper.convert(infoSource));
            writer.store(ItemHelper.convert(seq));
            writer.store(ItemHelper.convert(organism));
            writer.store(ItemHelper.convert(location));
            writer.store(ItemHelper.convert(feature));
            writer.store(ItemHelper.convert(computationalAnalysis));
            writer.store(ItemHelper.convert(computationalResult));
        } catch (ObjectStoreException e) {
            LOG.error("Problem writing item to the itemwriter");
                //throw e;
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
            organism = createItem(NAMESPACE + "Organism", "", -1);
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
     * Create an item with given className, implementation and item identifier
     * @param className
     * @param implementations
     * @param identifier
     * @return the created item
     */
    private Item createItem(String className, String implementations, int identifier) {
        Item item = new Item();
        item.setClassName(className);
        item.setImplementations(implementations);
        item.setIdentifier("0_" + identifier);
        return item;
    }

}
