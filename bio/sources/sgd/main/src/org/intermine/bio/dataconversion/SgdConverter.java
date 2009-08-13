package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.util.Util;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 *
 * @author
 */
public class SgdConverter extends BioDBConverter
{
    private static final Logger LOG = Logger.getLogger(SgdConverter.class);
    private static final String DATASET_TITLE = "SGD data set";
    private static final String DATA_SOURCE_NAME = "SGD";
    private Map<String, String> chromosomes = new HashMap();  
    private Map<String, String> genes = new HashMap();
    private Map<String, String> synonyms = new HashMap(); 
    private static final String TAXON_ID = "4932";
    private Item organism;
    private Map<String, ReferenceList> featureMap = new HashMap();
    
    /**
     * Construct a new SgdConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     */
    public SgdConverter(Database database, Model model, ItemWriter writer) 
    throws ObjectStoreException {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
        
        organism = createItem("Organism");
        organism.setAttribute("taxonId", TAXON_ID);
        store(organism);
    }

    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
        // a database has been initialised from properties starting with db.sgd
        Connection connection = getDatabase().getConnection();
 
        // process genes and chromosomes
        processGenes(connection);
        
        // process proteins
        processProteins(connection);
        
        // process CDSs
        
        // process synonyms
        processSynonyms(connection);
        
    }
    
    private void processGenes(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getGeneResults(connection);
        while (res.next()) {
            
            String featureNo = res.getString("feature_no");

            if (genes.get(featureNo) == null) {
                
                // ~~~ gene ~~~ 
                
                String secondaryIdentifier = res.getString("feature_name");
                String source = res.getString("source");
                String symbol = res.getString("gene_name");
                String primaryIdentifier = res.getString("dbxref_id");
                String featureType = res.getString("feature_type");
                String strand = res.getString("strand");
                
                Item item = createItem("Gene");
                item.setAttribute("primaryIdentifier", primaryIdentifier);                
                item.setAttribute("featureType", featureType);                
                item.setAttribute("secondaryIdentifier", secondaryIdentifier);
                item.setReference("organism", organism);
                if (StringUtils.isNotEmpty(symbol)) {
                    item.setAttribute("symbol", symbol);
                }                
                
                String refId = item.getIdentifier();
                genes.put(featureNo, refId);
                
                //  ~~~ chromosome ~~~ 
                
                String chromosomeIdentifier = res.getString("chromosomeIdentifier");
                String chromosomeId = res.getString("chromosomeId");
                
                String chromosomeRefId = getChromosome(chromosomeId, chromosomeIdentifier);
                item.setReference("chromosome", chromosomeRefId);
                
                // ~~~ location ~~~
                
                String start = (strand.equals("1") ? res.getString("start_coord") : 
                    res.getString("stop_coord"));
                String stop = (strand.equals("1") ? res.getString("stop_coord") : 
                    res.getString("start_coord"));
                                
                item.setAttribute("length", getLength(start, stop));
                
                Item location = createItem("Location");
                location.setAttribute("start", start);
                location.setAttribute("end", stop);                
                location.setAttribute("strand", strand);
                location.setReference("subject", item);
                location.setReference("object", chromosomeRefId);
                
                item.setReference("chromosomeLocation", location);
                
                try {
                    store(item);
                    store(location);
                } catch (ObjectStoreException e) {
                    throw new ObjectStoreException(e);
                }
                
                // ~~~ synonyms ~~~
                
                getSynonym(refId, "symbol", symbol);
                getSynonym(refId, "identifier", secondaryIdentifier);
                getSynonym(refId, "identifier", primaryIdentifier);
                
            }
        }      
    }
        
    private void processProteins(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getProteinResults(connection);
        while (res.next()) {
            String featureNo = res.getString("feature_no");
            String residues = res.getString("residues");
            String length = res.getString("seq_length");
            String md5checksum = Util.getMd5checksum(residues);
            String geneRefId = genes.get(featureNo);
            if (StringUtils.isEmpty(geneRefId)) {
                return;
            }
            
            // ~~~ sequence ~~~
            
            Item seq = createItem("Sequence");
            seq.setAttribute("residues", residues);
            seq.setAttribute("length", length);
            seq.setAttribute("md5checksum", md5checksum);            
            try {
                store(seq);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            
            // ~~~ protein ~~~
            
            Item protein = createItem("Protein");
            protein.setReference("sequence", seq);
            protein.setReference("organism", organism);
            protein.addToCollection("genes", geneRefId);
            
            try {
                store(protein);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            
        }
    }
    
    private void processSynonyms(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getSynonymResults(connection);
        while (res.next()) {
            String featureNo = res.getString("feature_no");
            String alias = res.getString("alias_name");
            String geneRefId = genes.get(featureNo);
            if (StringUtils.isEmpty(geneRefId)) {
                return;
            }
            String synonymRefId = getSynonym(geneRefId, "identifier", alias);
//            addFeature(geneRefId, synonymRefId);
        }
//        storeCollection();        
    }
    
    
    private void storeCollection() 
    throws ObjectStoreException {
        for (Map.Entry<String, ReferenceList> entry : featureMap.entrySet()) {
            Integer refId = new Integer(entry.getKey());
            super.store(entry.getValue(), refId);
        }
        featureMap = new HashMap();
    }
    
    private void addFeature(String key, String refId) {
        if (featureMap.get(key) == null) {
            featureMap.put(key, new ReferenceList());            
        }
        featureMap.get(key).addRefId(refId);
    }
    
    /**
     * Return the results of running a query for the 'ORF' feature types.
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getGeneResults(Connection connection)
        throws SQLException {
        String query = " SELECT f.feature_no, f.feature_name, f.dbxref_id, f.feature_type,  "
            + " f.source, f.coord_version, f.start_coord, f.stop_coord, f.strand, f.gene_name,  "
            + " f.name_description, f.genetic_position, f.headline, f.dbxref_id, f.strand, "
            + " c.feature_name AS chromosomeIdentifier, c.feature_no AS chromosomeId "
            + " FROM feature f, feature c, feat_relationship j "         
            + " WHERE (f.feature_type = 'ORF' "
            + "    OR f.feature_type = 'tRNA' "
            + "    OR f.feature_type = 'pseudogene' "
            + "    OR f.feature_type = 'snRNA' "
            + "    OR f.feature_type = 'snoRNA' "
            + "    OR f.feature_type = 'rRNA' "
            + "    OR f.feature_type = 'ncRNA') "
            + "   AND f.feature_no = j.child_feature_no "
            + "   AND j.parent_feature_no = c.feature_no "
            + "   AND c.feature_type = 'chromosome'";
//            + "   AND j.relationship_type = '' ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    
    /**
     * Return the results of running a query for the 'ORF' feature types.
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getSynonymResults(Connection connection)
        throws SQLException {
        String query = " SELECT fa.feature_no, a.alias_name "
            + " FROM alias a, feat_alias fa "         
            + " WHERE fa.alias_no = a.alias_no ";
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
        String query = " SELECT feature_no, seq_length, residues "
            + " FROM seq s "         
            + " WHERE s.seq_type = 'protein'"; 
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
        String query = " SELECT f.feature_no, f.feature_name, f.dbxref_id, f.feature_type,  "
            + " f.source, f.coord_version, f.start_coord, f.stop_coord, f.strand, f.gene_name,  "
            + " f.name_description, f.genetic_position, f.headline, f.dbxref_id, f.strand, "
            + " c.feature_no AS chromosomeId "
            + " FROM feature f, feature c, feat_relationship j "         
            + " WHERE (f.feature_type = 'ORF' "
            + "    OR f.feature_type = 'tRNA' "
            + "    OR f.feature_type = 'pseudogene' "
            + "    OR f.feature_type = 'snRNA' "
            + "    OR f.feature_type = 'snoRNA' "
            + "    OR f.feature_type = 'rRNA' "
            + "    OR f.feature_type = 'ncRNA') "
            + "   AND f.feature_no = j.child_feature_no "
            + "   AND j.parent_feature_no = c.feature_no "
            + "   AND c.feature_type = 'chromosome'";
//            + "   AND j.relationship_type = '' ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    
    private String getLength(String start, String end)
    throws NumberFormatException {
        Integer a = new Integer(start);
        Integer b = new Integer(end);

        // if the coordinates are on the crick strand, they need to be reversed or they
        // result in a negative number
        if (a.compareTo(b) > 0) {
            a = new Integer(end);
            b = new Integer(start);
        }

        Integer length = new Integer(b.intValue() - a.intValue());
        return length.toString();
    }
    
    private String getChromosome(String id, String identifier)
    throws ObjectStoreException {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        String refId = chromosomes.get(id);
        if (refId == null) {
            Item item = createItem("Chromosome");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", organism);
            refId = item.getIdentifier();
            chromosomes.put(id, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
        return refId;
    }
    
    private String getSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
        String key = subjectId + type + value;
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String refId = synonyms.get(key);
        if (refId == null) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            refId = syn.getIdentifier();
            synonyms.get(key);
            try {
                store(syn);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
        return refId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        return DATASET_TITLE;
    }
}
