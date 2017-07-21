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

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * @author Julie Sullivan
 */
public class OntologyIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(OntologyIdResolverFactory.class);

    private String ontology = null;
    private static final String MOCK_TAXON_ID = "0";
    private final String propName = "db.production";

    /**
     * Construct with SO term of the feature type to read from chado database.
     *
     * @param ontology the feature type to resolve
     */
    public OntologyIdResolverFactory(String ontology) {
        this.ontology = ontology;
    }

    /**
     * Return an IdResolver, if not already built then create it.
     * @return a specific IdResolver
     */
    @Override
    public IdResolver getIdResolver() {
        return getIdResolver(true);
    }

    /**
     * Return an IdResolver, if not already built then create it.  If failOnError
     * set to false then swallow any exceptions and return null.  Allows code to
     * continue if no resolver can be set up.
     * @param failOnError if false swallow any exceptions and return null
     * @return a specific IdResolver
     */
    @Override
    public IdResolver getIdResolver(boolean failOnError) {
        if (!caughtError) {
            try {
                createIdResolver();
            } catch (Exception e) {
                this.caughtError = true;
                if (failOnError) {
                    throw new RuntimeException(e);
                }
            }
        }
        return resolver;
    }

    /**
     * Build an IdResolver.
     */
    @Override
    protected void createIdResolver() {
        if (resolver != null && resolver.hasTaxonAndClassName(MOCK_TAXON_ID, this.ontology)) {
            return;
        }
        if (resolver == null) {
            resolver = new IdResolver(this.ontology);
        }

        try {
            boolean isCachedIdResolverRestored = restoreFromFile();
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonAndClassName(MOCK_TAXON_ID, this.ontology))) {
                LOG.info("Creating id resolver from database and caching it.");
                createFromDb(DatabaseFactory.getDatabase(propName));
                resolver.writeToFile(new File(idResolverCachedFileName));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void createFromDb(Database database) {
        Connection conn = null;
        try {
            conn = database.getConnection();
            String query = "select t.identifier, s.name "
                + "from ontologytermsynonyms j, ontologytermsynonym s, ontologyterm t, ontology o "
                + "where t.ontologyid = o.id and o.name = 'GO' and t.id = j.ontologyterm "
                + "and j.synonyms = s.id and s.name LIKE 'GO:%'";

            LOG.info("QUERY: " + query);
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            int i = addIdsFromResultSet(res);
            stmt.close();
            LOG.info("dbxref query returned " + i + " rows.");
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
     * Add results from query to ID resolver
     *
     * @param res Result set from query
     * @return number of IDs parsed
     * @throws Exception if error parsing query results
     */
    protected int addIdsFromResultSet(ResultSet res) throws Exception {
        int i = 0;
        while (res.next()) {
            String uniquename = res.getString("identifier");
            String synonym = res.getString("name");
            resolver.addMainIds(MOCK_TAXON_ID, uniquename, Collections.singleton(uniquename));
            resolver.addMainIds(MOCK_TAXON_ID, uniquename, Collections.singleton(synonym));
            i++;
        }
        return i;
    }
}
