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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * Create an IdResolverFactory for human genes - tracking Ensembl ids, NCBI ids, HGNC ids, HGNC
 * symbols.
 *
 * @author Fengyuan Hu
 */
public class HumanIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(HumanIdResolverFactory.class);
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "humangene";
    private final String taxonId = "9606";

    private static final String HGNC_PREFIX = "HGNC:";
    private static final String OMIM_PREFIX = "OMIM:";

    /**
     * Construct without SO term of the feature type.
     */
    public HumanIdResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    @Override
    protected void createIdResolver() {
        if (resolver != null
                && resolver.hasTaxonAndClassName(taxonId, this.clsCol
                        .iterator().next())) {
            return;
        }
        if (resolver == null) {
            if (clsCol.size() > 1) {
                resolver = new IdResolver();
            } else {
                resolver = new IdResolver(clsCol.iterator().next());
            }
        }

        try {
            boolean isCachedIdResolverRestored = restoreFromFile();
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonAndClassName(taxonId, this.clsCol.iterator().next()))) {
                String resolverFileRoot =
                        PropertiesUtil.getProperties().getProperty(propKey);

                if (StringUtils.isBlank(resolverFileRoot)) {
                    String message = "Resolver data file root path is not specified";
                    LOG.warn(message);
                    return;
                }

                LOG.info("Creating id resolver from data file and caching it.");
                String resolverFileName = resolverFileRoot.trim() + "/" + resolverFileSymbo;
                File f = new File(resolverFileName);
                if (f.exists()) {
                    createFromFile(f);
                    resolver.writeToFile(new File(idResolverCachedFileName));
                } else {
                    LOG.warn("Resolver file does not exist: " + resolverFileName);
                }
            } else {
                LOG.info("Using previously cached id resolver file: " + idResolverCachedFileName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populate the ID resolver from a tab delimited file
     *
     * @param f the file
     * @throws IOException if we can't read from the file
     */
    protected void createFromFile(File f) throws IOException {
        // Approved Symbol\tHGNC ID\tEntrez Gene ID\tEnsembl ID\tOMIM ID
        Iterator<?> lineIter = FormattedTextParser
                .parseTabDelimitedReader(new BufferedReader(new FileReader(f)));

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String symbol = line[0];
            String hgnc = line[1];
            String entrez = line[2];
            String ensembl = line[3];
            String omim = line[4];

            resolver.addMainIds(taxonId, symbol, Collections.singleton(symbol));
            resolver.addMainIds(taxonId, symbol, Collections.singleton(HGNC_PREFIX + hgnc));
            if (!StringUtils.isEmpty(entrez)) {
                resolver.addMainIds(taxonId, symbol, Collections.singleton(entrez));
            }
            if (!StringUtils.isEmpty(ensembl)) {
                resolver.addMainIds(taxonId, symbol, Collections.singleton(ensembl));
            }
            if (!StringUtils.isEmpty(omim)) {
                resolver.addMainIds(taxonId, symbol, Collections.singleton(OMIM_PREFIX + omim));
            }
        }
    }
}
