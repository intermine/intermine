package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * Create an IdResolver for HGNC previous symbols and aliases to current symbols.
 *
 * @author Richard Smith
 */
public class HgncIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(HgncIdResolverFactory.class);
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "hgnc";
    private final String taxonId = "9606";

    /**
     * Construct without SO term of the feature type.
     */
    public HgncIdResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Build an IdResolver for HGNC.
     */
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
        // HGNC ID | Approved Symbol | Approved Name | Status | Previous Symbols | Aliases
        Iterator<?> lineIter = FormattedTextParser
                .parseTabDelimitedReader(new BufferedReader(new FileReader(f)));
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("HGNC ID")) {
                continue;
            }

            String symbol = line[1];

            resolver.addMainIds(taxonId, symbol, Collections.singleton(symbol));
            addSynonyms(resolver, symbol, line[4]);
            addSynonyms(resolver, symbol, line[5]);
        }
    }

    private void addSynonyms(IdResolver resolver, String symbol, String ids) {
        if (!StringUtils.isBlank(ids)) {
            Set<String> synonyms = new HashSet<String>();
            for (String alias : ids.split(",")) {
                synonyms.add(alias.trim());
            }
            resolver.addSynonyms(taxonId, symbol, synonyms);
        }
    }
}
