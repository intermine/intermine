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

import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class NcbiGeneConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";
    private Set<String> taxonIds = null;
    
    protected static final Logger LOG = Logger.getLogger(NcbiGeneConverter.class);
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public NcbiGeneConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Set the organisms to include by a space separated list of taxon ids.
     * @param taxonIds the organisms to include
     */
    public void setOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }
    
    /**
     * Read the NCBI gene_info file and create genes setting identifiers, organism and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if (this.taxonIds == null) {
            throw new IllegalArgumentException("No organisms passed to NcbiGeneConverter.");
        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            
            String taxonId = line[0];
            String entrez = line[1];
            String defaultSymbol = line[2];
            String synonyms = line[4];
            String xrefs = line[5];
            String mapLocation = line[7];
            String defaultName = line[8];
            String officialSymbol = line[10];
            String officialName = line[11];

            if (!taxonIds.contains(taxonId)) {
                continue;
            }

            Item gene = createItem("Gene");
            gene.setReference("organism", getOrganism(taxonId));
            gene.setAttribute("ncbiGeneNumber", entrez);

            // SYMBOL
            if (!"-".equals(officialSymbol)) {
                gene.setAttribute("symbol", officialSymbol);
            } else {
                //gene.setAttribute("symbol", defaultSymbol);
            }
            if (StringUtils.isBlank(officialSymbol)) {
                LOG.info("GENE has no official symbol: " + entrez + " " + defaultSymbol);
            } else {
                if (!officialSymbol.equals(defaultSymbol)) {
                    LOG.info("GENE official symbol " + officialSymbol + " does not match "
                            + defaultSymbol);
                }
            }

            // NAME
            if (!"-".equals(officialName)) {
                gene.setAttribute("name", officialName);
            } else if (!"-".equals(defaultName)) {
                gene.setAttribute("name", defaultName);
            }

            // ENSEMBL ID become primaryIdentifier or CrossReference
            Set<String> ensemblIds = parseXrefs(xrefs, "Ensembl");
            if (ensemblIds.size() == 1) {
                gene.setAttribute("primaryIdentifier", ensemblIds.iterator().next());
            } else {
                // we don't want the primaryIdentifier to be blank
                //gene.setAttribute("primaryIdentifier", gene.getAttribute("symbol").getValue());
                for (String ensemblId : ensemblIds) {
                    Item crossRef = createItem("CrossReference");
                    crossRef.setAttribute("identifier", ensemblId);
                    crossRef.setReference("source", getDataSource("Ensembl"));
                    crossRef.setReference("subject", gene);
                    store(crossRef);
                }
            }

            // SYNONYMS
            for (String syn : synonyms.split("\\|")) {
                Item synonym = createItem("Synonym");
                synonym.setAttribute("value", syn);
                synonym.setReference("subject", gene);
                store(synonym);
            }

            // MAP LOCATION
            if (!"-".equals(mapLocation)) {
                gene.setAttribute("mapLocation", mapLocation);
            }
            store(gene);
        }
    }

    private Set<String> parseXrefs(String xrefs, String prefix) {
        if (!prefix.endsWith(":")) {
            prefix = prefix + ":";
        }
        Set<String> matched = new HashSet<String>();
        for (String xref : xrefs.split("\\|")) {
            System.out.println(xref);
            if (xref.startsWith(prefix)) {
                matched.add(xref.substring(prefix.length()));
            }
        }
        if (matched.size() > 1) {
            LOG.info("Matched multiple " + prefix + " identiifers: " + xrefs);
        }
        return matched;
    }
}
