package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Read Protein Atlas expression data.
 * @author Richard Smith
 */
public class ProteinAtlasConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Protein Atlas expression";
    private static final String DATA_SOURCE_NAME = "Protein Atlas";
    private Map<String,String> genes = new HashMap<String,String>();
    private Map<String,String> tissues = new HashMap<String,String>();

    private String taxonId = "9606";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ProteinAtlasConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Read Protein Atlas normal_tissue.csv file.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        if ("normal_tissue.csv".equals(currentFile.getName())) {
           processNormalTissue(reader);
        } else {
            throw new RuntimeException("Don't know how to process file: " + currentFile.getName());
        }
    }

    private void processNormalTissue(Reader reader) throws ObjectStoreException, IOException {
        // data has format
        // "Gene","Tissue","Cell type","Level","Expression type","Reliability"
        // "ENSG00000000003","adrenal gland","glandular cells","Negative","Staining","Supportive"

        Iterator lineIter = FormattedTextParser.parseCsvDelimitedReader(reader);
        lineIter.next();  // discard header

        // Read all lines into gene records
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String geneId = getGeneId(line[0]);
            String tissueId = getTissueId(line[1]);

            String cellType = line[2];
            String level = line[3];
            String expressionType = line[4];
            String reliability = line[5];

            Item expression = createItem("ProteinAtlasExpression");
            expression.setAttribute("cellType", cellType);
            expression.setAttribute("level", level);
            expression.setAttribute("expressionType", expressionType);
            expression.setAttribute("reliability", reliability);
            expression.setReference("gene", geneId);
            expression.setReference("tissue", tissueId);
            store(expression);
        }
    }

    private String getTissueId(String tissueName) throws ObjectStoreException {
        String tissueId = tissues.get(tissueName);
        if (tissueId == null) {
            Item tissue = createItem("Tissue");
            tissue.setAttribute("name", tissueName);
            store(tissue);
            tissueId = tissue.getIdentifier();
            tissues.put(tissueName, tissueId);
        }
        return tissueId;
    }

    private String getGeneId(String primaryIdentifier) throws ObjectStoreException {
        String geneId = genes.get(primaryIdentifier);
        if (geneId == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
            gene.setReference("organism", getOrganism(taxonId));
            store(gene);
            geneId = gene.getIdentifier();
            genes.put(primaryIdentifier, geneId);
        }
        return geneId;
    }
}
