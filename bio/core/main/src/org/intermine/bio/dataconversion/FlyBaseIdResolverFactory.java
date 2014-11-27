package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * Create an IdResolver for Drosophila genes by querying tables in a FlyBase
 * chado database.
 * @author Richard Smith
 *
 */
public class FlyBaseIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(FlyBaseIdResolverFactory.class);

    private final String propName = "db.flybase";
    private final String taxonId = "7227";

    /**
     * Construct with class name/feature type to read from chado database or file.
     */
    public FlyBaseIdResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Construct with class name/feature type to read from chado database or file.
     * @param clsName the feature type to resolve
     */
    public FlyBaseIdResolverFactory(String clsName) {
        this.clsCol = new HashSet<String>(Arrays.asList(new String[] {clsName}));
    }

    /**
     * Construct with class name/feature type to read from chado database or file.
     * @param clsCol a collection of feature type to resolve
     */
    public FlyBaseIdResolverFactory(Set<String> clsCol) {
        this.clsCol = clsCol;
    }

    /**
     * Build an IdResolver for FlyBase by accessing a FlyBase chado database.
     */
    @Override
    protected void createIdResolver() {
        if (resolver != null && resolver.hasTaxonAndClassNames(taxonId, this.clsCol)) {
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
            boolean isCachedIdResolverRestored = restoreFromFile(this.clsCol);
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonAndClassNames(taxonId, this.clsCol))) {
                LOG.info("Creating id resolver from database and caching id resolver to file: "
                        + idResolverCachedFileName);
                createFromDb(clsCol, DatabaseFactory.getDatabase(propName));
                resolver.writeToFile(new File(idResolverCachedFileName));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populate the ID resolver from a cached file
     *
     * @param clsCol set of classes
     * @return true if cached file exists and operation was successful
     */
    protected boolean restoreFromFile(Set<String> clsCol) {
        try {
            File f = new File(idResolverCachedFileName);
            if (f.exists()) {
                LOG.info("Restoring id resolver from cache file: " + idResolverCachedFileName);
                resolver.populateFromFile(f);

                // if file doesn't contain classes, revisit db
                Set<String> existedClsSet = resolver.getClassNames();
                if (!existedClsSet.containsAll(clsCol)) {
                    LOG.info("Id resolver has class names: "
                            + existedClsSet + ", but doesn't contain some classes in "
                            + clsCol + ". Creating and caching id resolver.");
                    existedClsSet.addAll(clsCol);
                    LOG.info("Query from database for: " + existedClsSet
                            + " and caching id resolver to file.");
                    createFromDb(existedClsSet, DatabaseFactory.getDatabase(propName));
                    resolver.writeToFile(f);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void createFromDb(Set<String> clsCol, Database db) {
        Connection conn = null;
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        try {
            conn = db.getConnection();
            LOG.info("Querying FlyBase DB: " + db.getName());
            for (String clsName : clsCol) {
                String query = "select c.cvterm_id"
                    + " from cvterm c, cv"
                    + " where c.cv_id = cv.cv_id"
                    + " and cv.name = \'SO\'"
                    + " and c.name =\'" + clsName + "\'";
                LOG.info("QUERY: " + query);
                Statement stmt = conn.createStatement();
                ResultSet res = stmt.executeQuery(query);
                String soTermId = null;
                res.next();
                soTermId = res.getString("cvterm_id");
                String orgConstraint = "";
                if (taxonId != null) {
                    String abbrev = or.getOrganismDataByTaxon(
                            new Integer(taxonId)).getAbbreviation();
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

                String extraConstraint = "";
                if ("gene".equals(clsName)) {
                    extraConstraint = " and  f.uniquename like \'FBgn%\'";
                }

                // fetch feature name for located genes
                query = "select distinct o.abbreviation, f.uniquename, f.name"
                    + " from feature f, featureloc l, organism o"
                    + " where f.organism_id = o.organism_id"
                    + " and f.is_obsolete = false"
                    + " and f.type_id = " + soTermId
                    + " and l.feature_id = f.feature_id"
                    + orgConstraint
                    + extraConstraint;
                LOG.info("QUERY: " + query);
                stmt = conn.createStatement();
                res = stmt.executeQuery(query);
                int i = 0;
                while (res.next()) {
                    String uniquename = res.getString("uniquename");
                    String name = res.getString("name");
                    String organism = res.getString("abbreviation");
                    String taxId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
                    resolver.addMainIds(taxId, clsName, uniquename,
                            Collections.singleton(uniquename));
                    resolver.addSynonyms(taxId, clsName, uniquename, Collections.singleton(name));
                    i++;
                }
                LOG.info("feature query returned " + i + " rows.");
                stmt.close();

                // fetch gene synonyms
                query = "select distinct o.abbreviation, f.uniquename, s.name, "
                    + " fs.is_current, c.name as type"
                    + " from feature f, feature_synonym fs, synonym s, featureloc l,"
                    + " organism o, cvterm c"
                    + " where f.organism_id = o.organism_id"
                    + " and f.is_obsolete = false"
                    + " and f.type_id = " + soTermId
                    + " and l.feature_id = f.feature_id"
                    + " and fs.feature_id = f.feature_id "
                    + " and fs.synonym_id = s.synonym_id"
                    + " and s.type_id = c.cvterm_id"
                    + orgConstraint
                    + extraConstraint;
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
                        resolver.addMainIds(taxId, clsName, uniquename,
                                Collections.singleton(synonym));
                    } else {
                        resolver.addSynonyms(taxId, clsName, uniquename,
                                Collections.singleton(synonym));
                    }
                    i++;
                }
                stmt.close();
                LOG.info("synonym query returned " + i + " rows.");

                // fetch FlyBase dbxrefs for located genes
                query = "select distinct o.abbreviation, f.uniquename,"
                    + " d.accession, db.name, fd.is_current"
                    + " from feature f, dbxref d, feature_dbxref fd, db, featureloc l, organism o"
                    + " where f.organism_id = o.organism_id"
                    + " and f.is_obsolete = false"
                    + " and f.type_id = " + soTermId
                    + " and f.feature_id = l.feature_id"
                    + " and fd.feature_id = f.feature_id"
                    + " and fd.dbxref_id = d.dbxref_id"
                    + " and d.db_id = db.db_id"
                    + " and db.name like \'FlyBase%\'"
                    + orgConstraint
                    + extraConstraint;
                LOG.info("QUERY: " + query);
                stmt = conn.createStatement();
                res = stmt.executeQuery(query);
                i = addIdsFromResultSet(res, or, clsName);
                stmt.close();
                LOG.info("dbxref query returned " + i + " rows.");
            }
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
     * @param or organism repository
     * @param clsName type of data to resolve
     * @return number of IDs parsed
     * @throws Exception if error parsing query results
     */
    protected int addIdsFromResultSet(ResultSet res, OrganismRepository or,
            String clsName) throws Exception {
        int i = 0;
        while (res.next()) {
            String uniquename = res.getString("uniquename");
            String accession = res.getString("accession");
            String organism = res.getString("abbreviation");
            String dbName = res.getString("name");
            boolean isCurrent = res.getBoolean("is_current");
            String taxId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
            if (isCurrent && "FlyBase Annotation IDs".equals(dbName)) {
                resolver.addMainIds(taxId, clsName, uniquename,
                        Collections.singleton(accession));
            } else {
                resolver.addSynonyms(taxId, clsName, uniquename,
                        Collections.singleton(accession));
            }
            i++;
        }
        return i;
    }
}
