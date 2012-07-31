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
 *
 */
public class OntologyIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(OntologyIdResolverFactory.class);
    private Database db;
    private String ontology = null;
    private static final String MOCK_TAXON_ID = "0";


    /**
     * Construct with SO term of the feature type to read from chado database.
     *
     * @param ontology the feature type to resolve
     */
    public OntologyIdResolverFactory(String ontology) {
        this.ontology = ontology;
    }

    /**
     * Build an IdResolver for FlyBase by accessing a FlyBase chado database.
     * @return an IdResolver for FlyBase
     */
    @Override
    protected IdResolver createIdResolver() {
        IdResolver resolver = new IdResolver(ontology);

        try {
            // TODO we already know this database, right?
            db = DatabaseFactory.getDatabase("os.production");

            String cacheFileName = "build/" + db.getName() + "." + ontology;
            File f = new File(cacheFileName);
            if (f.exists()) {
                System.out .println("OntologyIdResolver reading from cache file: " + cacheFileName);
                resolver = createFromFile(ontology, f);
            } else {
                System.out .println("OntologyIdResolver creating from database: " + db.getName());
                resolver = createFromDb(db);
                resolver.writeToFile(f);
                System.out .println("OntologyIdResolver caching in file: " + cacheFileName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resolver;
    }


    private IdResolver createFromDb(Database database) {
        IdResolver resolver = new IdResolver(ontology);
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
            int i = 0;
            while (res.next()) {
                String uniquename = res.getString("identifier");
                String synonym = res.getString("name");
                resolver.addMainIds(MOCK_TAXON_ID, uniquename, Collections.singleton(synonym));
            }
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

        return resolver;
    }
}
