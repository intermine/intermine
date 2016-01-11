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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.sql.Database;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * Create an IdResolver for Worm genes by querying tables in a WormBase
 * chado database.
 *
 * @author Richard Smith
 */
public class WormBaseIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(WormBaseIdResolverFactory.class);

    private final String taxonId = "6239";
    private final String propKeyFile = "resolver.file.rootpath";
    private final String resolverFileSymboWormId = "wormid";
    // HACK
    private final String resolverFileSymboWb2Ncbi = "wb2ncbi";

    /**
     * constructor
     */
    public WormBaseIdResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Construct with SO term of the feature type to read from chado database.
     * @param clsName the feature type to resolve
     */
    public WormBaseIdResolverFactory(String clsName) {
        this.clsCol = new HashSet<String>(Arrays.asList(new String[] {clsName}));
    }

    /**
     * Build an IdResolver for WormBase by accessing a WormBase chado database.
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
//                LOG.info("Creating id resolver from WormBase Chado database and caching it.");
//                System.out. println("Creating id resolver from WormBase Chado database and " +
//                        "caching it.");
//                createFromDb(DatabaseFactory.getDatabase(propKeyDb));

                // Create resolver from worm identifier file
                String resolverFileRoot = PropertiesUtil.getProperties()
                        .getProperty(propKeyFile);

                if (StringUtils.isBlank(resolverFileRoot)) {
                    String message = "Resolver data file root path is not specified.";
                    LOG.warn(message);
                    return;
                }

                LOG.info("To process WormId file");
                String wormIdFileName = resolverFileRoot.trim() + "/" + resolverFileSymboWormId;
                File wormIdDataFile = new File(wormIdFileName);

                if (wormIdDataFile.exists()) {
                    createFromWormIdFile(wormIdDataFile);

                    // HACK - Additionally, load WB2NCBI to have ncbi ids
                    LOG.info("To process WB2NCBI file");
                    String wb2NcbiFileName = resolverFileRoot.trim() + "/" + resolverFileSymboWb2Ncbi;
                    File wb2NcbiDataFile = new File(wb2NcbiFileName);

                    if (wb2NcbiDataFile.exists()) {
                        createFromWb2NcbiFile(wb2NcbiDataFile);
                    } else {
                        LOG.warn("Resolver file does not exist: " + wb2NcbiFileName);
                    }
                    // END OF HACK

                    resolver.writeToFile(new File(idResolverCachedFileName));
                } else {
                    LOG.warn("Resolver file does not exist: " + wormIdFileName);
                }
            } else {
                LOG.info("Using previously cached id resolver file: " + idResolverCachedFileName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createFromDb(Database database) {
        Connection conn = null;
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        try {
            conn = database.getConnection();
            String query = "select c.cvterm_id"
                + " from cvterm c, cv"
                + " where c.cv_id = cv.cv_id"
                + " and cv.name = \'sequence\'"
                + " and c.name =\'" + this.clsCol.iterator().next() + "\'";
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            String soTermId = null;
            res.next();
            soTermId = res.getString("cvterm_id");

            String orgConstraint = "";
            if (taxonId != null) {
                String abbrev = or.getOrganismDataByTaxon(new Integer(taxonId)).getAbbreviation();
                query = "select organism_id"
                    + " from organism"
                    + " where abbreviation = \'" + abbrev + "\'";
                LOG.info("QUERY: " + query);
                stmt = conn.createStatement();
                res = stmt.executeQuery(query);
                String organismId = null;
                res.next();
                organismId = res.getString("organism_id");
                stmt.close();
                orgConstraint = " and o.organism_id = " + organismId;
            }

            // fetch feature name for located genes
            query = "select distinct o.abbreviation, f.uniquename, f.name"
                + " from feature f, featureloc l, organism o"
                + " where f.organism_id = o.organism_id"
                + " and f.is_obsolete = false"
                + " and f.type_id = " + soTermId
                + " and l.feature_id = f.feature_id"
                + orgConstraint;
            LOG.info("QUERY: " + query);
            stmt = conn.createStatement();
            res = stmt.executeQuery(query);
            int i = 0;
            while (res.next()) {
                String uniquename = res.getString("uniquename");
                String name = res.getString("name");
                String organism = res.getString("abbreviation");
                String taxId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
                resolver.addMainIds(taxId, uniquename, Collections.singleton(name));
                i++;
            }
            LOG.info("feature query returned " + i + " rows.");
            stmt.close();

            // fetch gene synonyms
            query = "select distinct o.abbreviation, f.uniquename, s.name, "
                + " fs.is_current, c.name as type"
                + " from feature f, feature_synonym fs, synonym s,"
                + " organism o, cvterm c"
                + " where f.organism_id = o.organism_id"
                + " and f.is_obsolete = false"
                + " and f.type_id = " + soTermId
                + " and fs.feature_id = f.feature_id "
                + " and fs.synonym_id = s.synonym_id"
                + " and s.type_id = c.cvterm_id"
                + orgConstraint;
            LOG.info("QUERY: " + query);
            stmt = conn.createStatement();
            res = stmt.executeQuery(query);
            i = 0;
            while (res.next()) {
                String uniquename = res.getString("uniquename");
                String synonym = res.getString("name");
                String organism = res.getString("abbreviation");
                String taxId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
                boolean isCurrent = res.getBoolean("is_current");
                String type = res.getString("type");
                if (isCurrent && "symbol".equals(type)) {
                    resolver.addMainIds(taxId, uniquename, Collections.singleton(synonym));
                } else {
                    resolver.addSynonyms(taxId, uniquename, Collections.singleton(synonym));
                }
            }
            stmt.close();
            LOG.info("synonym query returned " + i + " rows.");

        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Populate the ID resolver from a tab delimited file
     *
     * @param wormIdDataFile the file
     * @throws IOException if we can't read from the file
     */
    protected void createFromWormIdFile(File wormIdDataFile) throws IOException {
        Iterator<?> lineIter = FormattedTextParser
                .parseTabDelimitedReader(new BufferedReader(new FileReader(
                        wormIdDataFile)));
        //WormBase id \t symbol \t secondaryIdentifier
        LOG.info("Parsing WormId file...");
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String wbId = line[0];
            String symbol = line[1];
            String secId = line[2];

            resolver.addMainIds(taxonId, wbId, Collections.singleton(wbId));

            if (!StringUtils.isBlank(symbol)) {
                resolver.addMainIds(taxonId, wbId, Collections.singleton(symbol));
            }

            if (!StringUtils.isBlank(secId)) {
                resolver.addMainIds(taxonId, wbId, Collections.singleton(secId));
            }
        }
    }

    /**
     * Populate the ID resolver from a tab delimited file
     *
     * @param wb2NcbiDataFile the file
     * @throws IOException if we can't read from the file
     */
    protected void createFromWb2NcbiFile(File wb2NcbiDataFile) throws IOException {
        Iterator<?> lineIter = FormattedTextParser.parseDelimitedReader(
                new BufferedReader(new FileReader(wb2NcbiDataFile)), ' ');
        LOG.info("Parsing WB2NCBI file...");
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("#")) {
                continue;
            }

            String wbId = line[0];
            String entrez = line[1];

            if (!StringUtils.isBlank(entrez)) {
                resolver.addSynonyms(taxonId, wbId, Collections.singleton(entrez));
            }
        }
    }
}
