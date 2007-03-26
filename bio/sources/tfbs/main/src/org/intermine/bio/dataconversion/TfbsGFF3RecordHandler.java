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

import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;


/**
 * A converter/retriever for Tfbs GFF3 files.
 *
 * @author Wenyan Ji
 */

public class TfbsGFF3RecordHandler extends GFF3RecordHandler
{

    private final Map proteinMap = new HashMap();
    private final Map organismMap = new HashMap();
    private Item otherOrganism;
    private Item uniprotDb;
    private Reference uniprotRef;
    private ReferenceList uniprotRefList;
    private Item transfacDb;
    private Reference transfacRef;

    /**
     * Create a new TfbsGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public TfbsGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }



    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        if (record.getAttributes().get("zscore") != null) {
            String analysisId = analysis.getIdentifier();
            String zscore = (String) ((List) record.getAttributes().get("zscore")).get(0);
            Item computationalResult = getItemFactory().makeItemForClass(
                        getTargetModel().getNameSpace() + "ComputationalResult");
            computationalResult.setAttribute("type", "zscore");
            computationalResult.setAttribute("score", zscore);
            computationalResult.setReference("analysis", analysisId);
            addItem(computationalResult);
            feature.addToCollection("evidence", computationalResult.getIdentifier());
        }

        if (record.getAttributes().get("accession") != null) {
            String accession = (String) ((List) record.getAttributes().get("accession"))
                                          .get(0);
            feature.setAttribute("accession", accession);
            Item synonym = getItemFactory().makeItemForClass(
                              getTargetModel().getNameSpace() + "Synonym");
            synonym.setAttribute("type", "accession");
            synonym.setAttribute("value", accession);
            synonym.setReference("subject", feature.getIdentifier());
            synonym.addReference(getTransfacRef());
            addItem(synonym);

        }

        if (record.getAttributes().get("factors_id") != null) {
            List factorIds = (List) record.getAttributes().get("factors_id");
            List factors = new ArrayList();
            for (Iterator i = factorIds.iterator(); i.hasNext();) {
                String id = (String) i.next();
                String idAcc = id.substring(0, id.indexOf("_"));
                String idOrg = id.substring(id.indexOf("_") + 1);

                Item synonym;
                Item protein;
                if (!idAcc.equals("N")) {
                    if (proteinMap.containsKey(idAcc)) {
                        protein = (Item) proteinMap.get(idAcc);

                    } else {
                        protein = getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                                  + "Protein");
                        protein.setAttribute("primaryAccession", idAcc);
                        protein.setReference("organism", getOtherOrganism(idOrg).getIdentifier());
                        protein.addCollection(getUniprotRefList());
                        proteinMap.put(idAcc, protein);
                        addItem(protein);

                        synonym = getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                                  + "Synonym");
                        synonym.setAttribute("type", "transfacAccession");
                        synonym.setAttribute("value", idAcc);
                        synonym.setReference("subject",
                                   ((Item) proteinMap.get(idAcc)).getIdentifier());
                        synonym.addReference(getUniprotRef());
                        addItem(synonym);
                    }
                    if (!factors.contains(protein.getIdentifier())) {
                        factors.add(protein.getIdentifier());
                    }
                    feature.addCollection(new ReferenceList("factors", factors));
                }
            }
        }
    }


    /**
     * @return organism item, abbreviation is HS for homo_sapiens, rn for rat, mm for mouse
     */
    private Item getOtherOrganism(String abbrev) {
        String orgAbbrev = abbrev.toUpperCase();
        if (orgAbbrev.equals("HS")) {
            otherOrganism =  getOrganism();
        } else {
            if (organismMap.containsKey(orgAbbrev)) {
                otherOrganism = (Item) organismMap.get(orgAbbrev);
            } else {
                otherOrganism = getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                                 + "Organism");
                otherOrganism.setAttribute("abbreviation", orgAbbrev);
                organismMap.put(orgAbbrev, otherOrganism);
                addItem(otherOrganism);
            }
        }
        return otherOrganism;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getUniprotDb() {
        if (uniprotDb == null) {
            uniprotDb = getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                        + "DataSource");
            uniprotDb.setAttribute("name", "UniProt");
            addItem(uniprotDb);
            //uniprotDb.setAttribute("url", "http://www.uniprot.org/");
        }
        return uniprotDb;
    }

    /**
     * @return db reference
     */
    private Reference getUniprotRef() {
        if (uniprotRef == null) {
            uniprotRef = new Reference("source", getUniprotDb().getIdentifier());
        }
        return uniprotRef;
    }

    /**
     * @return db referenceList
     */
    private ReferenceList getUniprotRefList() {
        if (uniprotRefList == null) {
            List list = new ArrayList();
            list.add(getUniprotDb().getIdentifier());
            uniprotRefList = new ReferenceList("evidence", list);
        }
        return uniprotRefList;
    }

    /**
     * set database object
     * @return db item
     */
    private Item getTransfacDb() {
        if (transfacDb == null) {
            transfacDb = getItemFactory().makeItemForClass(getTargetModel().getNameSpace()
                        + "DataSource");
            transfacDb.setAttribute("name", "transfac");
            addItem(transfacDb);
            //transfacDb.setAttribute("url", "http://www.gene-regulation.com/pub/databases.html");
        }
        return transfacDb;
    }

    /**
     * @return db reference
     */
    private Reference getTransfacRef() {
        if (transfacRef == null) {
            transfacRef = new Reference("source", getTransfacDb().getIdentifier());
        }
        return transfacRef;
    }
}

