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
    @SuppressWarnings("unused")
    private final String propNameDb = "db.wormbase";
    private final String propNameWormId = "resolver.wormid.file";
    // HACK
    private final String propNameWb2Ncbi = "resolver.wb2ncbi.file";

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
     * @return an IdResolver for WormBase
     */
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
//                LOG.info("Creating id resolver from WormBase Chado database and caching it.");
//                System.out. println("Creating id resolver from WormBase Chado database and " +
//                        "caching it.");
//                createFromDb(DatabaseFactory.getDatabase(propNameDb));

                // Create resolver from worm identifier file
                String WormIdFileName = PropertiesUtil.getProperties()
                        .getProperty(propNameWormId);

                if (StringUtils.isBlank(WormIdFileName)) {
                    String message = "WormBase gene resolver has no file name specified: "
                            + propNameWormId;
                    LOG.warn(message);
                    return;
                }

                LOG.info("To process WormId file");
                createFromWormIdFile(new BufferedReader(new FileReader(new File(
                        WormIdFileName.trim()))));

                // HACK - Additionally, load WB2NCBI to have ncbi ids
                String Wb2NcbiFileName = PropertiesUtil.getProperties()
                        .getProperty(propNameWb2Ncbi);

                if (StringUtils.isBlank(Wb2NcbiFileName)) {
                    String message = "WormBase gene resolver has no file name specified: "
                            + propNameWb2Ncbi;
                    LOG.warn(message);
                    return;
                }

                LOG.info("To process WB2NCBI file");
                createFromWb2NcbiFile(new BufferedReader(new FileReader(new File(
                        Wb2NcbiFileName.trim()))));
                // END OF HACK

                resolver.writeToFile(new File(ID_RESOLVER_CACHED_FILE_NAME));
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

    private void createFromWormIdFile(BufferedReader reader) throws IOException {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
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

    // HACK
    private void createFromWb2NcbiFile(BufferedReader reader) throws IOException {
        Iterator<?> lineIter = FormattedTextParser.parseDelimitedReader(reader, ' ');
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
