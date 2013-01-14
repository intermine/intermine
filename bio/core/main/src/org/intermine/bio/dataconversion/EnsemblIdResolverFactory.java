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
 * Create an IdResolverFactory for human Ensembl gene ids, this doesn't include any synonyms or id
 * tracking but simply filters for ids that are located on a the main chromosomes.
 *
 * @author Richard Smith
 */
public class EnsemblIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(EnsemblIdResolverFactory.class);
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "ensembl";
    private final String FilePathKey = "resolver.ensembl.file";
    private final String taxonId = "9606";

    /**
     * Construct without SO term of the feature type.
     * @param soTerm the feature type to resolve
     */
    public EnsemblIdResolverFactory() {
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
            boolean isCachedIdResolverRestored = restoreFromFile(this.clsCol);
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonAndClassName(taxonId, this.clsCol.iterator().next()))) {

                String resolverFileName =
                        PropertiesUtil.getProperties().getProperty(FilePathKey);

                if (StringUtils.isBlank(resolverFileName)) {
                    String message = "Resolver data file path is not specified";
                    LOG.warn(message);

                    String resolverFileRoot =
                            PropertiesUtil.getProperties().getProperty(propKey);

                    // File path not set in MINE.properties
                    if (StringUtils.isBlank(resolverFileRoot)) {
                        String msg = "Resolver data file root path is not specified";
                        LOG.warn(msg);
                        return;
                    }

                    LOG.info("Creating id resolver from data file and caching it.");
                    resolverFileName = resolverFileRoot.trim() + resolverFileSymbo;
                }

                File f = new File(resolverFileName.trim());
                if (f.exists()) {
                    createFromFile(new BufferedReader(new FileReader(f)));
                    resolver.writeToFile(new File(ID_RESOLVER_CACHED_FILE_NAME));
                } else {
                    LOG.warn("Resolver file not exists: " + resolverFileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createFromFile(BufferedReader reader) throws IOException {

        Set<String> validChromosomes = validChromosomes();

        // Ensembl Id | chromosome name
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String ensembl = line[0];
            String chr = line[1];
            if (validChromosomes.contains(chr)) {
                resolver.addMainIds(taxonId, ensembl, Collections.singleton(ensembl));
            }
        }
    }

    private Set<String> validChromosomes() {
        Set<String> chrs = new HashSet<String>();
        for (int i = 1; i <= 22; i++) {
            chrs.add("" + i);
        }
        chrs.add("X");
        chrs.add("Y");
        chrs.add("MT");
        return chrs;
    }
}
