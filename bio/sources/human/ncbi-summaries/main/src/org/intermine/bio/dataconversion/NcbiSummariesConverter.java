package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
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
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * Read file generated from NCBI web service that includes Entrez gene ids with text summaries from
 * RefSeq or OMIM.
 * @author Richard Smith
 */
public class NcbiSummariesConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "NCBI Gene summaries";
    private static final String DATA_SOURCE_NAME = "NCBI Gene";
    private static final String HUMAN_TAXON_ID = "9606";
    protected static final Logger LOG = Logger.getLogger(NcbiSummariesConverter.class);
    private Set<String> genes = new HashSet<String>();

    private IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public NcbiSummariesConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        rslv = IdResolverService.getHumanIdResolver();

        // Data has format:
        // Entrez id | description
        @SuppressWarnings("rawtypes")
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            try {
                String entrez = line[0];
                String description = line[1];
                if (!StringUtils.isBlank(description)) {
                    getGene(entrez, description);
                }
            } catch (IndexOutOfBoundsException e) {
                LOG.info("Failed to read line: " + Arrays.asList(line));
            }
        }
    }

    private void getGene(String entrezIdentifier, String description)
        throws ObjectStoreException {
        if (genes.contains(entrezIdentifier)) {
            LOG.error("DUPLICATE entrez " + entrezIdentifier + " for single descr: " + description);
            return;
        }
        genes.add(entrezIdentifier);

        Item gene = createItem("Gene");
        if (resolveGene(entrezIdentifier) == null) {
            LOG.warn("Unresolved Entrez gene: " + entrezIdentifier);
            return;
        }
        gene.setAttribute("primaryIdentifier", resolveGene(entrezIdentifier));
        gene.setAttribute("description", description);
        gene.setReference("organism", getOrganism(HUMAN_TAXON_ID));
        store(gene);
    }

    private String resolveGene(String entrez) {
        int resCount = rslv.countResolutions("" + HUMAN_TAXON_ID, entrez);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene - MIM:"
                     + entrez + " count: " + resCount + " - "
                     + rslv.resolveId("" + HUMAN_TAXON_ID, entrez));
            return null;
        }
        return rslv.resolveId("" + HUMAN_TAXON_ID, entrez).iterator().next();
    }
}
