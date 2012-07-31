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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, Item> tissues = new HashMap<String, Item>();

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
        } else if ("tissue_to_organ.tsv".equals(currentFile.getName())){
            processTissueToOrgan(reader);
        } else {
            throw new RuntimeException("Don't know how to process file: " + currentFile.getName());
        }
    }

    private void  processTissueToOrgan(Reader reader) throws ObjectStoreException, IOException {
        // file has two colums:
        // Tissue name <\t> Tissue group 
        
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        Map<String, Item> tissueGroups = new HashMap<String, Item>();
        
        // Read all lines into gene records
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String tissueName = line[0];
            String tissueGroupName = line[1];
            
            Item tissue = getTissue(tissueName);
            Item tissueGroup = tissueGroups.get(tissueGroupName);
            if (tissueGroup == null) {
                tissueGroup = createItem("TissueGroup");
                tissueGroup.setAttribute("name", tissueGroupName);
                store(tissueGroup);
                tissueGroups.put(tissueGroupName, tissueGroup);
            }
            tissue.setAttribute("name", tissueName);
            tissue.setReference("tissueGroup", tissueGroup);
            store(tissue);
        }
    }
    
    private void processNormalTissue(Reader reader) throws ObjectStoreException, IOException {
        // data has format
        // "Gene","Tissue","Cell type","Level","Expression type","Reliability"
        // "ENSG00000000003","adrenal gland","glandular cells","Negative","Staining","Supportive"

        // APE - two or more antibodies
        // Staining - one antibody only
        // 0 - very low/ none
        // 1 - low/ not supportive
        // 2 - medium/ unsupportive
        // 3 - high/ supportive

        Iterator lineIter = FormattedTextParser.parseCsvDelimitedReader(reader);
        lineIter.next();  // discard header

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String geneId = getGeneId(line[0]);
            String capitalisedTissueName = StringUtils.capitalize(line[1]);
            Item tissueId = getTissue(capitalisedTissueName);

            String cellType = line[2];
            String level = line[3];
            String expressionType = line[4];
            String reliability = line[5];

            level = alterLevel(level, expressionType);
            reliability = alterReliability(reliability, expressionType);
            
            Item expression = createItem("ProteinAtlasExpression");
            expression.setAttribute("cellType", cellType);
            expression.setAttribute("level", level);
            expression.setAttribute("expressionType", alterExpressionType(expressionType));
            expression.setAttribute("reliability", reliability);
            expression.setReference("gene", geneId);
            expression.setReference("tissue", tissueId);
            store(expression);
        }
    }

    // store tells us we have been called with the upper case name from the tissue_to_organ file
    private Item getTissue(String tissueName) throws ObjectStoreException {
        Item tissue = tissues.get(tissueName);
        if (tissue == null) {
            tissue = createItem("Tissue");
            tissues.put(tissueName, tissue);
        }
        return tissue;
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

    private String alterLevel(String level, String type) {
        if ("staining".equalsIgnoreCase(type)) {
            if ("strong".equalsIgnoreCase(level)) {
                return "High";
            } else if ("moderate".equalsIgnoreCase(level)) {
                return "Medium";
            } else if ("weak".equalsIgnoreCase(level)) {
                return "Low";
            } else if ("negative".equalsIgnoreCase(level)) {
                return "None";
            }
        }
        return level;
    }
    
    private String alterReliability(String reliability, String type) {
        if ("staining".equalsIgnoreCase(type)) {
            if ("supportive".equalsIgnoreCase(reliability)) {
                return "High";
            } else if ("uncertain".equalsIgnoreCase(reliability)) {
                return "Low";
            }
        } else if ("ape".equalsIgnoreCase(type)) {
            if ("hi".equalsIgnoreCase(reliability)) {
                return "High";
            } else if ("medium".equalsIgnoreCase(reliability)) {
                return "High";
            }  else if ("low".equalsIgnoreCase(reliability)) {
                return "Low";
            }  else if ("very low".equalsIgnoreCase(reliability)) {
                return "Low";
            }
        }
        return reliability;
    }
    
    private String alterExpressionType(String expressionType) {
        if ("APE".equals(expressionType)) {
            return "APE - two or more antibodies";
        } else if ("Staining".equals(expressionType)) {
            return "Staining - one antibody only";
        } else {
            return expressionType;
        }
    }
    
    
}
