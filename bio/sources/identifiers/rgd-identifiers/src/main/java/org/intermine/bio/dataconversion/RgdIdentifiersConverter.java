package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;


/**
 * Read RGD RAT_GENES identifiers file to create canonical rat genes with correct identifiers.
 * @author Richard Smith
 */
public class RgdIdentifiersConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "RGD gene identifiers";
    private static final String DATA_SOURCE_NAME = "Rat Genome Database";

    private static final String RAT_TAXON = "10116";

    protected static final Logger LOG = Logger.getLogger(RgdIdentifiersConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public RgdIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {


        Set<String> duplicateEnsembls = new HashSet<String>();
        Map<String, Integer> storedGeneIds = new HashMap<String, Integer>();
        Map<String, String> geneEnsemblIds = new HashMap<String, String>();

        // Read all lines into id pairs, track any ensembl ids or symbols that appear twice
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // remove header line
        // check symbol and ncbiGeneNumber unique

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("GENE_RGD_ID")) {
                continue;
            }
            String rgdId = line[0];
            String symbol = line[1];
            String name = line[2];
            String description = line[3];
            String entrez = line[20];
            String ensembl = line[37];

            Item gene = createItem("Gene");
            gene.setReference("organism", getOrganism(RAT_TAXON));
            gene.setAttribute("primaryIdentifier", "RGD:" + rgdId);
            gene.setAttribute("symbol", symbol);

            Set<String> ensemblIds = parseEnsemblIds(ensembl);
            for (String ensemblId : ensemblIds) {
                createCrossReference(gene.getIdentifier(), ensemblId, "Ensembl", true);
            }

            if (!StringUtils.isBlank(name)) {
                gene.setAttribute("name", name);
            }
            if (!StringUtils.isBlank(description)) {
                gene.setAttribute("description", description);
            }
            if (!StringUtils.isBlank(entrez)) {
                createCrossReference(gene.getIdentifier(), entrez, "NCBI", true);
            }

            Integer storedGeneId = store(gene);
            storedGeneIds.put(gene.getIdentifier(), storedGeneId);
        }

        LOG.info("ENSEMBL: duplicateEnsemblIds.size() = " + duplicateEnsembls.size());
        LOG.info("ENSEMBL: duplicateEnsemblIds = " + duplicateEnsembls);
        // now check that we only saw each ensembl id once
        for (Map.Entry<String, String> entry : geneEnsemblIds.entrySet()) {
            String geneIdentifier = entry.getKey();
            String ensemblId = entry.getValue();
            if (!duplicateEnsembls.contains(ensemblId)) {
                Attribute att = new Attribute("primaryIdentifier", ensemblId);
                store(att, storedGeneIds.get(geneIdentifier));
            }
        }
    }

    private Set<String> parseEnsemblIds(String fromFile) {
        Set<String> ensembls = new HashSet<String>();
        if (!StringUtils.isBlank(fromFile)) {
            ensembls.addAll(Arrays.asList(fromFile.split(";")));
        }
        return ensembls;
    }
}
