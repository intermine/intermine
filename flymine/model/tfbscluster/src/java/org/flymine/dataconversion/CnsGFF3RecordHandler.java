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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

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
    private final Map organismMap = new HashMap();


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

        List conservedOrganismList = getConservedOrganismList(record);
        feature.addCollection(new ReferenceList("conservedOrganisms", conservedOrganismList));

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
     * @param residues String
     * @return item sequence
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


    /**
     * create a collection with all the possible conservedOranisms 
     * attribute name in Gff3 Attributes
     * @param record GFF3Record
     * @return ArrayList with conservedOrganisms
     */
    public List getConservedOrganismList(GFF3Record record) {
        List conservedOrganismList = new ArrayList();
        if (record.getAttributes().get("organism") != null) {
            List orgList = (List) record.getAttributes().get("organism");
            Iterator i = orgList.iterator();
            Item conservedOrg;
            while (i.hasNext()) {
                String orgAbbrev = (String) i.next();
                if (organismMap.containsKey(orgAbbrev)) {
                    conservedOrg = (Item) organismMap.get(orgAbbrev);                 
                } else {
                    conservedOrg = createItem("Organism");
                    conservedOrg.setAttribute("abbreviation", orgAbbrev);
                    addItem(conservedOrg);
                    organismMap.put(orgAbbrev, conservedOrg);
                }
                conservedOrganismList.add(conservedOrg.getIdentifier());   
               
            }
        }
        return conservedOrganismList;
    }

}

