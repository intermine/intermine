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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * ID resolver for MGI genes.
 *
 * @author Fengyuan Hu
 */
public class MgiIdentifiersResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(MgiIdentifiersResolverFactory.class);

    // data file path set in ~/.intermine/MINE.properties
    // e.g. resolver.zfin.file=/micklem/data/mgi-identifiers/current/MGI_Coordinate.rpt
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "mgi";
    private final String FilePathKey = "resolver.mgi.file";
    private final String taxonId = "10090";

    private static final String NULL_STRING = "null";

    /**
     * Construct without SO term of the feature type.
     * @param soTerm the feature type to resolve
     */
    public MgiIdentifiersResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Construct with SO term of the feature type.
     * @param soTerm the feature type to resolve
     */
    public MgiIdentifiersResolverFactory(String clsName) {
        this.clsCol = new HashSet<String>(Arrays.asList(new String[] {clsName}));
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
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 16) {
                continue;
            }

            String type = line[1];
            if (!"Gene".equals(type)) {
                continue;
            }

            String identifier = line[0];    // MGI
            String symbol = line[2];
            String name = line[3];
            String entrez = line[10];
            String ensembl = line[15];

            if (StringUtils.isEmpty(identifier)) {
                continue;
            }

            if (!NULL_STRING.equals(identifier)) {
                resolver.addMainIds(taxonId, identifier, Collections.singleton(identifier));

                if (!NULL_STRING.equals(symbol)) {
                    resolver.addMainIds(taxonId, identifier, Collections.singleton(symbol));
                }
                if (!NULL_STRING.equals(name)) {
                    resolver.addMainIds(taxonId, identifier, Collections.singleton(name));
                }
                if (!NULL_STRING.equals(entrez)) {
                    resolver.addSynonyms(taxonId, identifier, Collections.singleton(entrez));
                }
                if (!NULL_STRING.equals(ensembl)) {
                    resolver.addSynonyms(taxonId, identifier, Collections.singleton(ensembl));
                }
            }
        }
    }
}
