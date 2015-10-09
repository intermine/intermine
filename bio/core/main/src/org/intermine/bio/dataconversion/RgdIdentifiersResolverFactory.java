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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * ID resolver for RGD genes.
 *
 * @author Fengyuan Hu
 */
public class RgdIdentifiersResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(RgdIdentifiersResolverFactory.class);

    // data file path set in ~/.intermine/MINE.properties
    // e.g. resolver.zfin.file=/micklem/data/rgd-identifiers/current/GENES_RAT.txt
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "rgd";
    private final String taxonId = "10116";

    /**
     * Construct with SO term of the feature type.
     */
    public RgdIdentifiersResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Construct with SO term of the feature type.
     * @param clsName the feature type to resolve
     */
    public RgdIdentifiersResolverFactory(String clsName) {
        this.clsCol = new HashSet<String>(Arrays.asList(new String[] {clsName}));
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
        Iterator<?> lineIter = FormattedTextParser.
                parseTabDelimitedReader(new BufferedReader(new FileReader(f)));
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("GENE_RGD_ID") || line[0].startsWith("#")) {
                continue;
            }

            String rgdId = "RGD:" + line[0];
            String symbol = line[1];
            String name = line[2];
            String entrez = line[20];
            String ensembl = line[37];

            resolver.addMainIds(taxonId, rgdId, Collections.singleton(rgdId));
            resolver.addMainIds(taxonId, rgdId, Collections.singleton(symbol));

            Set<String> ensemblIds = parseEnsemblIds(ensembl);
            resolver.addSynonyms(taxonId, rgdId, ensemblIds);

            if (!StringUtils.isBlank(name)) {
                resolver.addMainIds(taxonId, rgdId, Collections.singleton(name));
            }

            if (!StringUtils.isBlank(entrez)) {
                resolver.addSynonyms(taxonId, rgdId, Collections.singleton(entrez));
            }
        }
    }

    private static Set<String> parseEnsemblIds(String fromFile) {
        Set<String> ensembls = new HashSet<String>();
        if (!StringUtils.isBlank(fromFile)) {
            ensembls.addAll(Arrays.asList(fromFile.split(";")));
        }
        return ensembls;
    }
}
