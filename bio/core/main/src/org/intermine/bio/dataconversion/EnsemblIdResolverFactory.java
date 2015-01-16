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
 * Create an IdResolverFactory for human Ensembl gene ids
 *
 * @author Julie Sullivan
 */
public class EnsemblIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(EnsemblIdResolverFactory.class);
    private static final String PROP_KEY = "resolver.file.rootpath";
    private static final String FILE_SYMBOLIC_LINK = "ensembl";
    private static final String TAXON_ID = "9606";

    /**
     * Construct without SO term of the feature type.
     */
    public EnsemblIdResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    @Override
    protected void createIdResolver() {
        if (resolver != null && resolver.hasTaxonAndClassName(TAXON_ID,
                this.clsCol.iterator().next())) {
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
                    && !resolver.hasTaxonAndClassName(TAXON_ID, this.clsCol.iterator().next()))) {

                String resolverFileRoot = PropertiesUtil.getProperties().getProperty(PROP_KEY);

                if (StringUtils.isBlank(resolverFileRoot)) {
                    String message = "Resolver data file root path is not specified";
                    LOG.warn(message);
                    return;
                }

                LOG.info("Creating id resolver from data file and caching it.");
                String resolverFileName = resolverFileRoot.trim() + FILE_SYMBOLIC_LINK;

                File f = new File(resolverFileName);
                if (f.exists()) {
                    populateFromFile(f);
                    resolver.writeToFile(new File(idResolverCachedFileName));
                } else {
                    LOG.warn("Resolver file not exists: " + resolverFileName);
                }
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
    protected void populateFromFile(File f) throws IOException {

        // Ensembl Gene ID EntrezGene ID   HGNC ID(s)      HGNC symbol
        Iterator<?> lineIter = FormattedTextParser
                .parseTabDelimitedReader(new BufferedReader(new FileReader(f)));
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String ensembl = line[0];
            String entrez = line[1];
            String hgncID = line[2];
            String symbol = line[3];

            resolver.addMainIds(TAXON_ID, ensembl, Collections.singleton(ensembl));
            if (!StringUtils.isEmpty(entrez)) {
                resolver.addMainIds(TAXON_ID, ensembl, Collections.singleton(entrez));
            }
            if (!StringUtils.isEmpty(hgncID)) {
                resolver.addMainIds(TAXON_ID, ensembl, Collections.singleton(hgncID));
            }
            if (!StringUtils.isEmpty(symbol)) {
                resolver.addMainIds(TAXON_ID, ensembl, Collections.singleton(symbol));
            }
        }
    }
}
