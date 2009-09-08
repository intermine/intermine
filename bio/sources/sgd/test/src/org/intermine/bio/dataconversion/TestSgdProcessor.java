package org.intermine.bio.dataconversion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mockobjects.sql.MockMultiRowResultSet;


public class TestSgdProcessor extends SgdProcessor
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getGeneResults(Connection connection) {
        String[] columnNames = new String[] {
            "feature_no", "feature_name", "dbxref_id", "gene_name", "name_description", "feature_type", "headline"
        };

        
        Object[][] resObjects = new Object[][] {
            {
                "879",
                "YAL023C",
                "S000000021",
                "ORF",
                "PMT2",
                "Protein O-MannosylTransferase",
                "Protein O-mannosyltransferase, transfers mannose residues from dolichyl phosphate-D-mannose to protein serine/threonine residues; acts in a complex with Pmt1p, can instead interact with Pmt5p in some conditions; target for new antifungals  "
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getGeneLocationResults(Connection connection) {
        String[] columnNames = new String[] {
            "gene_feature_no", "identifier", "feature_no", "feature_type", "strand", "stop_coord","start_coord"            
        };
        Object[][] resObjects = new Object[][] {
            {
                "7551",
                "YLR347C",
                "901",
                "ORF",
                "W",
                "824928",
                "824788"                
            }
        };        
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

//  /**
//  * {@inheritDoc}
//  */
//  @Override
//  protected  ResultSet getProteinResults(Connection connection) {
//  String[] columnNames = new String[] {
//  "feature_id", "name", "uniquename", "type", "seqlen", "residues", "md5checksum",
//  "organism_id"
//  };
//  Object[][] resObjects = new Object[][] {
//  {
//  500 ,    411 ,   3117509 , "partof"
//  }
//  };
//  MockMultiRowResultSet res = new MockMultiRowResultSet();
//  res.setupRows(resObjects);
//  res.setupColumnNames(columnNames);
//  return res;
//  }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getCDSResults(Connection connection)
    {
        String[] columnNames = new String[] {
            "feature_no", "feature_name", "dbxref_id",
            "start_coord", "stop_coord", "strand",            
            "gene_feature_no"            
        };
        Object[][] resObjects = new Object[][] {
            {
                "9620",
                "YNL032W_11003",
                "S000030820",
                "574507",  
                "575352",
                "W",  
                "5417"
                
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getPubResults(Connection connection) {
        String[] columnNames = new String[] {
            "reference_no", "issue", "page", "pubmed", "status", "title", "volume", "year", "citation", 
            "property_value", "property_type", 
            "gene_feature_no"
        };
        Object[][] resObjects = new Object[][] {
            {
                "pubRefNo",
                "1",
                "12-34",
                "387518",
                "Published",
                "Mutagenesis by cytostatic alkylating agents in yeast strains of differing repair capacities.",
                "2009",
                "[CITATION NEEDED]",
                "literature_topic",
                "RegulatoryRole",
                "geneFeatureNumber"
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getBindingSiteResults(Connection connection) {
        String[] columnNames = new String[] {
            "feature_no", "feature_name", "dbxref_id",
            "start_coord", "stop_coord", "strand"
        };
        Object[][] resObjects = new Object[][] {
            {
                "1",
                "SUM1-binding-site-S000083663",
                "S000083663",
                "114088",
                "114098",
                "W"
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getPhenotypeResults(Connection connection) {
        String[] columnNames = new String[] {
            "phenotype2_no", "experiment_type", "mutant_type", "qualifier", "observable", "gene_feature_no"
        };
        Object[][] resObjects = new Object[][] {
            {
                "1", 
                "large-scale survey",
                "null",
                "increased",
                "resistance to chemicals",
                "1"
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

}
