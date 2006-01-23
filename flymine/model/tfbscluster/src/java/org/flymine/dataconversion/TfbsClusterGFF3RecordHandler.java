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
 * A converter/retriever for Tfbs GFF3 files.
 *
 * @author Wenyan Ji
 */

public class TfbsClusterGFF3RecordHandler extends GFF3RecordHandler
{

    private final Map geneMap = new HashMap();
    private final Map organismMap = new HashMap();
    /**
     * Create a new TfbsGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public TfbsClusterGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }



    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) throws BuildException {
        Item feature = getFeature();

        if (record.getAttributes().get("type") != null) {
            String type = (String) ((List) record.getAttributes().get("type")).get(0);
            feature.setAttribute("type", type);
        }

        if (record.getAttributes().get("sequence") != null) {
            Item sequence = createItem("Sequence");
            String residues = (String) ((List) record.getAttributes().get("sequence")).get(0);
            sequence.setAttribute("residues", residues);
            addItem(sequence);
            feature.setReference("sequence", sequence.getIdentifier());
        } 
        
        if (record.getAttributes().get("organism") != null) {
            List orgList = getConservedOrganismList(
                          (List) record.getAttributes().get("organism"));
            feature.addCollection(new ReferenceList("conservedOrganisms", orgList));
        }


        if (record.getAttributes().get("gene1") != null) {
            createGeneAndRelated("gene1", record, feature);

        }
        if (record.getAttributes().get("gene2") != null) {
            createGeneAndRelated("gene2", record, feature);
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
    private void createGeneAndRelated(String gene, GFF3Record record, Item feature)
        throws BuildException {
        List genes = (List) record.getAttributes().get(gene);
        if (genes.size() != 3) {
            throw new BuildException
                ("gene attributes not in right format. please check the gff3 file");
        } else {
            String organismDbId = (String) genes.get(0);
            Item geneItem;
            if (geneMap.containsKey(organismDbId)) {
                geneItem = (Item) geneMap.get(organismDbId);
            } else {
                geneItem = createItem("Gene");
                geneItem.setAttribute("organismDbId", organismDbId);
                geneMap.put(organismDbId, geneItem);
                addItem(geneItem);
            }
            Item distanceRelation = createItem("DistanceRelation");
            distanceRelation.setAttribute("type", (String) genes.get(1));
            distanceRelation.setAttribute("distance", (String) genes.get(2));
            distanceRelation.setReference("object", feature.getIdentifier());
            distanceRelation.setReference("subject", geneItem.getIdentifier());
            feature.addToCollection("geneDistances", distanceRelation.getIdentifier());
            addItem(distanceRelation);
        }
    }


    /**
     * create a collection with all the possible conservedOranisms 
     * attribute name in Gff3 Attributes
     * @param orgList ArrayList from Gff3 organism attribute
     * @return ArrayList with identifier for conservedOrganisms
     */
    
    protected List getConservedOrganismList(List orgList) {
        List conservedOrganismList = new ArrayList();
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
        return conservedOrganismList;
    }
}
