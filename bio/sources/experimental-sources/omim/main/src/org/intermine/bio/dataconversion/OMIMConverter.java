package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * OMIM disease converter.
 *
 * @author Dominik Grimm
 */
public class OMIMConverter extends FileConverter
{
    //private data fields
    private ItemWriter writer;
    private BufferedReader in;
    private List<String> values;
    private List<List<String>> valueList;
    private Map<String, String> geneMap = new HashMap<String, String>();
    private Item organism = null;

    /**
     * Create a new OMIMConverter object.
     * @param writer the ItemWriter to write Items to
     * @param model the Model to use when making Items
     */
    public OMIMConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        organism = createItem("Organism");
        organism.setAttribute("taxonId", "9606");
        store(organism);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        in = new BufferedReader(reader);

        values = new Vector<String>();
        valueList = new Vector<List<String>>();

        String delimiter = "\\|";

        String readString;

        //read fields and values
        while ((readString = in.readLine()) != null) {
            String[] tmp = readString.split(delimiter);
            String disorders = null;
            String geneSymbols = tmp[5];
            String status = tmp[6];
            String title = tmp[7];
            String omimId = tmp[9];

            for (int i = 13; i <= 15; i++) {
                if (tmp[i] != null && !tmp[i].equals("")) {
                    disorders += tmp[i];
                }
            }

            if (disorders != null) {
                values.add(disorders);
            }

            Item disease = createItem("Disease");
            if (omimId != null && !"".equals(omimId)) {
                disease.setAttribute("omimId", omimId);

                disease.setAttribute("status", status);
                disease.setAttribute("description", disorders);

                String geneRefId = null;
                String[] geneSymbol = geneSymbols.split(",");
                for (int j = 0; j < geneSymbol.length; j++) {
                    if (geneMap.get(geneSymbol[j]) == null) {
                        Item gene = createItem("Gene");
                        gene.setAttribute("symbol", geneSymbol[j]);
                        geneRefId = gene.getIdentifier();
                        gene.addReference(new Reference("organism", organism.getIdentifier()));
                        store(gene);
                        geneMap.put(geneSymbol[j], geneRefId);
                    } else {
                        geneRefId = geneMap.get(geneSymbol[j]);
                    }
                    disease.setReference("gene", geneRefId);
                }
                store(disease);
            }
        }

    }
}
