package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import org.apache.tools.ant.BuildException;

/**
 * A converter/retriever for Tfbs conserved non-coding site GFF3 files.
 *
 * @author Wenyan Ji
 */

public class CnsGFF3RecordHandler extends GFF3RecordHandler
{
    protected Map conservedOrgMap =  new HashMap();
    protected Map sequenceMap =  new HashMap();

    /**
     * Create a new CnsGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public CnsGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }

    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    public void process(GFF3Record record) throws BuildException {
        Item feature = getFeature();

        if (record.getAttributes().get("type") != null) {
            String type = (String) ((List) record.getAttributes().get("type")).get(0);
            feature.setAttribute("type", type);
        }

        if (record.getAttributes().get("sequence") != null) {
            String residues = (String) ((List) record.getAttributes().get("sequence")).get(0);
            Item sequence = getSequenceItem(residues);
            feature.setReference("sequence", sequence.getIdentifier());
        }

        List conservedOrganismList = getConservedOrganismList(record);
        if (conservedOrganismList != null) {
            feature.addCollection(new ReferenceList("conservedOrganisms", conservedOrganismList));
        }
    }

    private Item makeItem(String clsName) {
        return getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                                  + clsName);
    }

    private List getConservedOrganismList(GFF3Record record) {
        List conservedOrganismList = new ArrayList();
        if (record.getAttributes().get("conservedOrganism") != null) {
            List orgList = (List) record.getAttributes().get("conservedOrganism");
            Iterator i = orgList.iterator();
            Item conservedOrg;
            while (i.hasNext()) {
                String orgAbbrev = (String) i.next();
                if (conservedOrgMap.containsKey(orgAbbrev)) {
                    conservedOrg = (Item) conservedOrgMap.get(orgAbbrev);
                } else {
                    conservedOrg = makeItem("Organism");
                    conservedOrg.setAttribute("abbreviation", orgAbbrev);
                    addItem(conservedOrg);
                    conservedOrgMap.put(orgAbbrev, conservedOrg);
                }
                conservedOrganismList.add(conservedOrg.getIdentifier());
            }
        }
        return conservedOrganismList;
    }

    private Item getSequenceItem(String residues) {
        Item sequence = (Item) sequenceMap.get(residues);
        if (sequence == null) {
            sequence = makeItem("Sequence");
            sequence.setAttribute("residues", residues);
            sequence.setAttribute("length", residues.length() + "");
            addItem(sequence);
            sequenceMap.put(residues, sequence);
        }
        return sequence;
    }

}

