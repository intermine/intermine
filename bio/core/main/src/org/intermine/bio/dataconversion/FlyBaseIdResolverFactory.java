package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * Create an IdResolver for Drosophila genes by querying tables in a FlyBase
 * chado database.  
 * @author rns
 *
 */
public class FlyBaseIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(FlyBaseIdResolverFactory.class);
    private Database db;
    
    /**
     * Build an IdResolver for FlyBase by accessing a FlyBase chado database.
     * @return an IdResolver for FlyBase
     */
    protected IdResolver createIdResolver() {
        IdResolver resolver = new IdResolver("Gene");
        Connection conn = null;
        try {
            OrganismRepository or = OrganismRepository.getOrganismRepository();
            
            // TODO maybe this shouldn't be hard coded here?
            db = DatabaseFactory.getDatabase("db.flybase");
            conn = db.getConnection();

            String query = "select c.cvterm_id"
                + " from cvterm c, cv"
                + " where c.cv_id = cv.cv_id"
                + " and cv.name = \'SO\'"
                + " and c.name =\'gene\'";
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            String geneTerm = null;
            res.next();
            geneTerm = res.getString("cvterm_id");
            
            
            // fetch feature name for located genes
            query = "select distinct o.abbreviation, f.uniquename, f.name"
                + " from feature f, featureloc l, organism o"
                + " where f.organism_id = o.organism_id"
                + " and f.is_obsolete = false"
                + " and f.type_id = " + geneTerm 
                + " and  f.uniquename like \'FBgn%\'"
                + " and l.feature_id = f.feature_id";
            // get connection again as unknown problems with closed connection from previous query
            //conn = db.getConnection();
            stmt = conn.createStatement();
            res = stmt.executeQuery(query);
            while (res.next()) {
                String uniquename = res.getString("uniquename");
                String name = res.getString("name");
                String organism = res.getString("abbreviation");
                String taxonId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
                resolver.addEntry(taxonId, uniquename, Collections.singleton(name));
            }
            
            // fetch gene synonyms
            query = "select distinct o.abbreviation, f.uniquename, s.name"
                + " from feature f, feature_synonym fs, synonym s, featureloc l, organism o"
                + " where f.organism_id = o.organism_id"
                + " and f.is_obsolete = false"
                + " and f.type_id = " + geneTerm 
                + " and f.uniquename like \'FBgn%\'"
                + " and l.feature_id = f.feature_id"
                + " and fs.feature_id = f.feature_id "
                + " and fs.synonym_id = s.synonym_id";
            stmt = conn.createStatement();
            res = stmt.executeQuery(query);
            while (res.next()) {
                String uniquename = res.getString("uniquename");
                String name = res.getString("name");
                String organism = res.getString("abbreviation");
                String taxonId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
                resolver.addEntry(taxonId, uniquename, Collections.singleton(name));
            }
            
            // fetch FlyBase dbxrefs for located genes
            query = "select distinct o.abbreviation, f.uniquename, d.accession"
                + " from feature f, dbxref d, feature_dbxref fd, db, featureloc l, organism o"
                + " where f.organism_id = o.organism_id"
                + " and f.is_obsolete = false"
                + " and f.type_id = " + geneTerm
                + " and f.uniquename like \'FBgn%\'"
                + " and f.feature_id = l.feature_id"
                + " and fd.feature_id = f.feature_id"
                + " and fd.dbxref_id = d.dbxref_id"
                + " and d.db_id = db.db_id" 
                + " and db.name like \'FlyBase%\'";
            stmt = conn.createStatement();
            res = stmt.executeQuery(query);
            while (res.next()) {
                String uniquename = res.getString("uniquename");
                String accession = res.getString("accession");
                String organism = res.getString("abbreviation");
                String taxonId = "" + or.getOrganismDataByAbbreviation(organism).getTaxonId();
                resolver.addEntry(taxonId, uniquename, Collections.singleton(accession));
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return resolver; 
    }
}
