package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
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
    private static final String ENSEMBL_GENE_PREFIX = "ENSG";

    /**
     * Construct without SO term of the feature type.
     * @param soTerm the feature type to resolve
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
        } else {
            if (resolver == null) {
                if (clsCol.size() > 1) {
                    resolver = new IdResolver();
                } else {
                    resolver = new IdResolver(clsCol.iterator().next());
                }
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
                String resolverFileName = resolverFileRoot.trim() + resolverFileSymbo;
                File f = new File(resolverFileName);
                if (f.exists()) {
                    createFromFile(f);
                    resolver.writeToFile(new File(ID_RESOLVER_CACHED_FILE_NAME));
                } else {
                    LOG.warn("Resolver file not exists: " + resolverFileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void createFromFile(File f) throws IOException {
        // Ensembl Id | NCBI Id | HGNC Id | HGNC symbol
        Iterator<?> lineIter = FormattedTextParser
                .parseTabDelimitedReader(new BufferedReader(new FileReader(f)));

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String ensembl = line[0];
            String entrez = line[1];
            String hgnc = line[2];
            String symbol = line[3];

            if (ensembl.startsWith(ENSEMBL_GENE_PREFIX)) {
                resolver.addMainIds(taxonId, ensembl, Collections.singleton(ensembl));
                if (!StringUtils.isEmpty(entrez)) {
                    resolver.addMainIds(taxonId, ensembl, Collections.singleton(entrez));
                }
                if (!StringUtils.isEmpty(hgnc)) {
                    resolver.addMainIds(taxonId, ensembl,
                            Collections.singleton(HGNC_PREFIX + hgnc));
                }
                if (!StringUtils.isEmpty(symbol)) {
                    resolver.addMainIds(taxonId, ensembl, Collections.singleton(symbol));
                }
            }
        }
    }
}
