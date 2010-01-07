package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
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

import org.apache.log4j.Logger;

/**
 * Queries the sgd oracle database and returns result sets
 * @author Julie Sullivan
 */
public class SgdProcessor
{
    private static final Logger LOG = Logger.getLogger(SgdProcessor.class);
    private static final String GENE_CONSTRAINT = "(g.feature_type = 'ORF' "
        + "    OR g.feature_type = 'tRNA' "
        + "    OR g.feature_type = 'pseudogene' "
        + "    OR g.feature_type = 'snRNA' "
        + "    OR g.feature_type = 'snoRNA' "
        + "    OR g.feature_type = 'rRNA' "
        + "    OR g.feature_type = 'ncRNA') ";
    private static final String GENE_LOCATION_CONSTRAINT = " (c.feature_type = 'chromosome' " 
        + "    OR c.feature_type = 'plasmid') ";
    
    /**
     * Return the results of running a query for genes
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getGeneResults(Connection connection)
        throws SQLException {
        String query = "SELECT g.feature_no, g.feature_name, g.dbxref_id, "
            + " g.gene_name, g.name_description, g.feature_type, g.headline "
            + "FROM feature g "         
            + "WHERE " + GENE_CONSTRAINT; 
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    /**
     * Return the results of running a query for genes and chromosomes
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getGeneLocationResults(Connection connection)
        throws SQLException {
        String query = "SELECT g.feature_no AS gene_feature_no, "
            + " c.feature_name AS identifier, c.feature_no, c.feature_type, "
            + " g.strand, g.stop_coord, g.start_coord "
            + "FROM feature g, feature c, feat_relationship j "         
            + "WHERE " + GENE_CONSTRAINT            
            + "   AND g.feature_no = j.child_feature_no "
            + "   AND j.parent_feature_no = c.feature_no "
            + "   AND " + GENE_LOCATION_CONSTRAINT;
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the results of running a query for protein sequences
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getProteinResults(Connection connection)
        throws SQLException {
        String query = "SELECT feature_no, seq_length, residues "
            + "FROM seq s "         
            + "WHERE s.seq_type = 'protein'"; 
       
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    /**
     * Return the results of running a query for CDSs and their sequences
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getCDSResults(Connection connection)
        throws SQLException {
        String query = "SELECT c.feature_no, c.feature_name, c.dbxref_id, "
        + " c.start_coord, c.stop_coord, c.strand, "
        + " g.feature_no AS gene_feature_no "
        + "FROM feature c, feature g, feat_relationship j "
        + "WHERE c.feature_type = 'CDS' "
        + "   AND " + GENE_CONSTRAINT 
        + "   AND c.feature_no = j.child_feature_no "
        + "   AND g.feature_no = j.parent_feature_no ";
        
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
        
    /**
     * Return the results of running a query for all publications.  
     * TODO only retreive publications for features we are interested in
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getPubResults(Connection connection)
        throws SQLException {
        String query = "SELECT r.reference_no, r.issue, r.page, r.pubmed, r.status, r.title, "
                + "r.volume, r.year, r.citation, p.property_value, p.property_type, "
                + "g.feature_no AS gene_feature_no "
                + "FROM reference r, feature g, refprop_feat f, ref_property p "
                + "WHERE r.reference_no = p.reference_no "
                + "   AND p.ref_property_no = f.refprop_feat_no "
                + "   AND g.feature_no = f.feature_no ";
                
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    /**
     * Return the results of running a query for CDSs and their sequences
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getBindingSiteResults(Connection connection)
        throws SQLException {
        String query = "SELECT f.feature_no, f.feature_name, f.dbxref_id, "
        + " f.start_coord, f.stop_coord, f.strand, "        
        + " c.feature_no AS chromosome_feature_no, c.feature_name AS chromosome_identifier "
        + "FROM feature f, feature c, feat_relationship j "
        + "WHERE f.feature_type = 'TF_binding_site' "
        + "   AND c.feature_type = 'Chromosome' " 
        + "   AND f.feature_no = j.child_feature_no "
        + "   AND c.feature_no = j.parent_feature_no ";

        query = "SELECT f.feature_no, f.feature_name, f.dbxref_id, "
            + " f.start_coord, f.stop_coord, f.strand "
            + "FROM feature f "
            + "WHERE f.feature_type = 'TF_binding_site' ";
        
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    /**
     * Return the results of running a query for CDSs and their sequences
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getPhenotypeResults(Connection connection)
        throws SQLException {
        String query = "SELECT p.phenotype2_no, p.source, p.experiment_type, p.mutant_type, "
            + " p.qualifier, p.observable, g.feature_no AS gene_feature_no, e.experiment_comment, "
            + " e.experiment_no "
            + "FROM phenotype2 p, pheno_annotation a, feature g, experiment e "
            + "WHERE " + GENE_CONSTRAINT 
            + "  AND a.phenotype2_no = p.phenotype2_no "
            + "  AND a.feature_no = g.feature_no " 
            + "  AND a.experiment_no = e.experiment_no ";
        
        query = "SELECT p.phenotype2_no, p.experiment_type, p.mutant_type, "
        + " p.qualifier, p.observable, g.feature_no AS gene_feature_no "
        + "FROM phenotype2 p, pheno_annotation a, feature g "
        + "WHERE " + GENE_CONSTRAINT 
        + "  AND a.phenotype2_no = p.phenotype2_no "
        + "  AND a.feature_no = g.feature_no "; 
        
        LOG.info("executing: " + query);        
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
}
