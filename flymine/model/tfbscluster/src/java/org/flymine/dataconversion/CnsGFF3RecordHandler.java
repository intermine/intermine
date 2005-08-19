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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import org.flymine.io.gff3.GFF3Record;

import org.apache.tools.ant.BuildException;


/**
 * A converter/retriever for Tfbs conserved non-coding site GFF3 files.
 *
 * @author Wenyan Ji
 */

public class CnsGFF3RecordHandler extends GFF3RecordHandler
{

    private final Map sequenceMap = new HashMap();
    private Item conservedOrganism;


    /**
     * Create a new CnsGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public CnsGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }


    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) throws BuildException {
        Item feature = getFeature();

        conservedOrganism = getConservedOrganism(record);

        feature.setReference("conservedOrganism", conservedOrganism);

        if (record.getAttributes().get("type") != null) {
            String type = (String) ((List) record.getAttributes().get("type")).get(0);
            feature.setAttribute("type", type);
        }

        if (record.getAttributes().get("sequence") != null) {
            String residues = (String) ((List) record.getAttributes().get("sequence")).get(0);
            Item sequence = getSequenceItem(residues);
            feature.setReference("sequence", sequence.getIdentifier());
        }


    }

    /**
     * @param clsName
     * @return item
     */
    private Item createItem(String clsName) {
        return getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                                  + clsName);
    }

    /**
     * create geneItem, DistanceRelation item and add relevant reference/collection to feature
     * attribute name in Gff3 Attributes
     * gene attributes like gene1=(geneId using ENSGxxxx),(3'or 5' or intron),distance
     * @param gene: attributes name, either gene1 or gene2
     * @param record: Gff3Record
     * @param feature
     * @throws BuildException if gene is not in right format
     */


    private Item getSequenceItem(String residues) {
        Item sequence = (Item) sequenceMap.get(residues);
        if (sequence == null) {
            sequence = createItem("Sequence");
            sequence.setAttribute("residues", residues);
            addItem(sequence);
            sequenceMap.put(residues, sequence);
        }
        return sequence;
    }

    public Item getConservedOrganism(GFF3Record record) {
        if (conservedOrganism == null) {
            if (record.getAttributes().get("organism") != null) {
                String orgAbbrev = (String) ((List) record.getAttributes().get("organism")).get(0);
                conservedOrganism = createItem("Organism");
                conservedOrganism.setAttribute("abbreviation", orgAbbrev);
                addItem(conservedOrganism);
            }
        }
        return conservedOrganism;
    }

}

