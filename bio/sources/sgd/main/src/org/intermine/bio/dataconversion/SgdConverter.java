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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private Map<String, String> plasmids = new HashMap();
    private Map<String, Item> genes = new HashMap();
    private Map<String, String> synonyms = new HashMap(); 
    private Map<String, String> publications = new HashMap();
    private static final String TAXON_ID = "4932";
    private Item organism;
    private Map<String, List<String>> featureMap = new HashMap();
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

        processGenes(connection);        
        processPubs(connection);
        processGeneLocations(connection);        
        // processProteins(connection);
        storeGenes();
        
        processCDSs(connection);
        processBindingSites(connection);
    }
   
    private void processGenes(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getGeneResults(connection);
        while (res.next()) {
            String featureNo = res.getString("feature_no");
            if (genes.get(featureNo) == null) {
                
                // ~~~ gene ~~~                 
                String primaryIdentifier = res.getString("dbxref_id");
                String secondaryIdentifier = res.getString("feature_name");                
                String symbol = res.getString("gene_name");                
                String name = res.getString("name_description");
                
                Item item = createItem("Gene");
                item.setAttribute("primaryIdentifier", primaryIdentifier); 
                if (StringUtils.isNotEmpty(name)) {
                    item.setAttribute("name", res.getString("name_description")); 
                }
                item.setAttribute("featureType", res.getString("feature_type"));                
                item.setAttribute("secondaryIdentifier", secondaryIdentifier);
                item.setReference("organism", organism);
                if (StringUtils.isNotEmpty(symbol)) {
                    item.setAttribute("symbol", symbol);
                }                
                String refId = item.getIdentifier();
                genes.put(featureNo, item);
                
                // ~~~ synonyms ~~~
                getSynonym(refId, "symbol", symbol);
                getSynonym(refId, "identifier", secondaryIdentifier);
                getSynonym(refId, "identifier", primaryIdentifier);
                
            }
        }      
    }
    
    private void storeGenes() 
    throws ObjectStoreException {
        for (Item gene : genes.values()) {
            try {
                store(gene);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }

    private void processGeneLocations(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getGeneLocationResults(connection);
        while (res.next()) {
            String featureNo = res.getString("feature_no");
            String geneFeatureNo = res.getString("gene_feature_no");
            String featureType = res.getString("feature_type");
            
            Item item = genes.get(geneFeatureNo);
            String geneRefId = item.getIdentifier();

            //  ~~~ chromosome OR plasmid ~~~                   
            String refId = null;            
            if (featureType.equalsIgnoreCase("plasmid")) {
                refId = getPlasmid(featureNo,res.getString("identifier"));
                item.setReference("plasmid", refId);
            } else if (featureType.equalsIgnoreCase("chromosome")) {
                refId = getChromosome(featureNo,res.getString("identifier"));
                item.setReference("chromosome", refId);
            }

            // ~~~ location ~~~
            String strand = res.getString("strand");
            String start =  res.getString("start_coord"); 
            String end = res.getString("stop_coord");

            if (start == null) {
                start = "0";
            }
            
            if (end == null) {
                end = "0";
            }
            
            start = (strand.equals("W") ? start : end);
            end = (strand.equals("W") ? start : end);

            item.setAttribute("length", getLength(start, end));
            String locationRefId = getLocation(geneRefId, refId, start, end, strand);

            if (featureType.equalsIgnoreCase("plasmid")) {
                item.setReference("plasmidLocation", locationRefId);
            } else {
                item.setReference("chromosomeLocation", locationRefId);
            }
                
        }      
    }
    
    // TODO implement this when we have sequences
    private void processProteins(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getProteinResults(connection);
        while (res.next()) {

            Item gene = genes.get(res.getString("feature_no"));
            if (gene == null) {
                return;
            }
            
            // ~~~ sequence ~~~
            String seq = getSequence(res.getString("seq_length"), res.getString("residues"));
            
            // ~~~ protein ~~~            
            Item protein = createItem("Protein");
            protein.setReference("sequence", seq);
            protein.setReference("organism", organism);
            protein.addToCollection("genes", gene.getIdentifier());
            
            try {
                store(protein);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            
        }
    }
    
    
    private void processCDSs(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getCDSResults(connection);
        while (res.next()) {
            
// TODO no seq yet
//            String residues = res.getString("residues");
//            String length = res.getString("seq_length");
            String gene_feature_no = res.getString("gene_feature_no");
            Item gene =  genes.get(gene_feature_no);
            if (gene == null) {
                throw new RuntimeException("Gene not found:  " + gene_feature_no);
            }
            String geneRefId = gene.getIdentifier();
            
            // ~~~ sequence ~~~
//            String seq = getSequence(length, residues);
            
            String secondaryIdentifier = res.getString("feature_name");
            String primaryIdentifier = res.getString("dbxref_id");            
            
            // ~~~ CDS ~~~            
            Item transcript = createItem("Transcript");
            transcript.setAttribute("primaryIdentifier", primaryIdentifier);
            transcript.setAttribute("secondaryIdentifier", secondaryIdentifier);
//            transcript.setAttribute("length", length);
//            transcript.setReference("sequence", seq);
            transcript.setReference("organism", organism);
            transcript.setReference("gene", geneRefId);
            
            String refId = transcript.getIdentifier();
            
            // TODO store these last
            getSynonym(refId, "identifier", secondaryIdentifier);
            getSynonym(refId, "identifier", primaryIdentifier);
            
            Item CDS = createItem("CDS");
            CDS.setAttribute("primaryIdentifier", primaryIdentifier);
            CDS.setAttribute("secondaryIdentifier", secondaryIdentifier);
//            transcript.setAttribute("length", length);
//            transcript.setReference("sequence", seq);
            CDS.setReference("organism", organism);
            CDS.setReference("gene", geneRefId);
            
            refId = CDS.getIdentifier();
            
            // TODO store these last
            getSynonym(refId, "identifier", secondaryIdentifier);
            getSynonym(refId, "identifier", primaryIdentifier);
            
            // ~~~ location ~~~
//            String strand = res.getString("strand");
//            String start = (strand.equals("C") ? res.getString("start_coord") : 
//                res.getString("stop_coord"));
//            String stop = (strand.equals("C") ? res.getString("stop_coord") : 
//                res.getString("start_coord"));
//            
//            transcript.setAttribute("length", getLength(start, stop));
//            String locationRefId = getLocation(transcript.getIdentifier(), 
//                                               geneRefId, start, stop, strand);
//            transcript.setReference("chromosomeLocation", locationRefId);
            
            try {
                store(transcript);
                store(CDS);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            
        }
    }
    
    private void processPubs(Connection connection)     
    throws SQLException, ObjectStoreException {
        ResultSet res = getPubResults(connection);
        while (res.next()) {
            
            String featureNo = res.getString("reference_no");
            String geneFeatureNo = res.getString("gene_feature_no");
            String refId = publications.get(featureNo);
            
            if (refId == null) {
                String issue = res.getString("issue");                
                String volume = res.getString("volume");
                String pubMedId = res.getString("pubmed");
                String pages = res.getString("page");
                String title = res.getString("title");
                
                Item item = createItem("Publication");
                if (StringUtils.isNotEmpty(issue)) {
                    item.setAttribute("issue", issue);
                }
                if (StringUtils.isNotEmpty(pubMedId)) {
                    item.setAttribute("pubMedId", pubMedId);
                }
                if (StringUtils.isNotEmpty(title)) {
                    item.setAttribute("title", title);
                }
                if (StringUtils.isNotEmpty(volume)) {
                    item.setAttribute("volume", volume);
                }
                item.setAttribute("year", res.getString("year"));
                if (StringUtils.isNotEmpty(pages)) {
                    item.setAttribute("pages", pages);
                }
                //item.setAttribute("citation", res.getString("citation"));                
                refId = item.getIdentifier();
                publications.put(featureNo, refId);
                
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new ObjectStoreException(e);
                }
                addFeature(geneFeatureNo, item.getIdentifier());
            }
        }
        addCollection("publications");
    }
    
    private void processBindingSites(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = getBindingSiteResults(connection);
        while (res.next()) {
            // ~~~ binding site ~~~                 
            String primaryIdentifier = res.getString("dbxref_id");
            String secondaryIdentifier = res.getString("feature_name");                

            Item item = createItem("TFBindingSite");
            item.setAttribute("primaryIdentifier", primaryIdentifier); 
            item.setAttribute("secondaryIdentifier", secondaryIdentifier);
            item.setReference("organism", organism);
            String refId = item.getIdentifier();
            
            String chromosomeFeatureNo = res.getString("chromosome_feature_no");
            String chromosomeRefId = getChromosome(chromosomeFeatureNo, 
                                                   res.getString("chromosomeIdentifier"));
            item.setReference("chromosome", chromosomeRefId);

            // ~~~ location ~~~
            String strand = res.getString("strand");
            String start =  res.getString("start_coord"); 
            String end = res.getString("stop_coord");

            if (start == null) {
                start = "0";
            }

            if (end == null) {
                end = "0";
            }

            start = (strand.equals("W") ? start : end);
            end = (strand.equals("W") ? start : end);

            item.setAttribute("length", getLength(start, end));
            String locationRefId = getLocation(refId, chromosomeRefId, start, end, strand);
            item.setReference("chromosomeLocation", locationRefId);
            
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            
            // ~~~ synonyms ~~~
            getSynonym(refId, "identifier", secondaryIdentifier);
            getSynonym(refId, "identifier", primaryIdentifier);
        }
    }

    private void addCollection(String collectionName) {
        for (Map.Entry<String, List<String>> entry : featureMap.entrySet()) {
            String featureNo = entry.getKey();
            List<String> pubRefIds = entry.getValue();
            Item gene = genes.get(featureNo);
            if (gene != null) {
                gene.setCollection(collectionName, pubRefIds);
            }
        }
        featureMap = new HashMap();
    }
        
    private void addFeature(String featureNo, String refId) {
        if (featureMap.get(featureNo) == null) {
            featureMap.put(featureNo, new ArrayList());            
        }
        featureMap.get(featureNo).add(refId);
    }
    
    /**
     * Return the results of running a query for genes
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getGeneResults(Connection connection)
        throws SQLException {
        String query = " SELECT g.feature_no, g.feature_name, g.dbxref_id, "
            + " g.source, g.coord_version, g.start_coord, g.stop_coord, g.strand, g.gene_name,  "
            + " g.name_description, g.genetic_position, g.headline, g.dbxref_id, g.strand, "
            + " g.feature_type "
            + " FROM feature g "         
            + " WHERE " + GENE_CONSTRAINT; 
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
        String query = " SELECT g.feature_no AS gene_feature_no, "
            + " c.feature_name AS identifier, c.feature_no, c.feature_type, "
            + " g.strand, g.stop_coord, g.start_coord "
            + " FROM feature g, feature c, feat_relationship j "         
            + " WHERE " + GENE_CONSTRAINT            
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
            + " f.name_description, f.genetic_position, f.headline, f.dbxref_id, "
            + " j.parent_feature_no as gene_feature_no, s.residues, s.seq_length "
            + " FROM feature f, feat_relationship j, seq s "
            + " WHERE f.feature_type = 'CDS' "
            + "   AND f.feature_no = j.child_feature_no "
            + "   AND s.feature_no = f.feature_no "
            + "   AND s.seq_type = 'genomic' ";
        
        // TODO no sequences yet
        query = "SELECT c.feature_no, c.feature_name, c.dbxref_id,   "
        + " c.source, c.coord_version, c.start_coord, c.stop_coord, c.strand, c.gene_name,  "
        + " c.name_description, c.genetic_position, c.headline, c.dbxref_id, c.strand, "
        + " g.feature_no AS gene_feature_no "
        + " FROM feature c, feature g, feat_relationship j "
        + " WHERE c.feature_type = 'CDS' "
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
        String query = " SELECT r.reference_no, r.citation, r.issue, r.journal_no, "
                + " r.page, r.pdf_status, r.pubmed, p.source, r.status, r.title, r.volume, r.year, "
                + " g.feature_no AS gene_feature_no "
                + " FROM reference r, feature g, refprop_feat f, ref_property p "
                + " WHERE r.reference_no = p.reference_no "
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
        String query = "SELECT f.feature_no, f.feature_name, f.dbxref_id,   "
        + " f.source, f.coord_version, f.start_coord, f.stop_coord, f.strand, f.gene_name,  "
        + " f.name_description, f.genetic_position, f.headline, f.dbxref_id, "
        + " c.feature_no AS chromosome_feature_no, c.feature_name AS chromosome_identifier "
        + " FROM feature f, feature c, feat_relationship j "
        + " WHERE f.feature_type = 'TF_binding_site' "
        + "   AND c.feature_type = 'Chromosome' " 
        + "   AND f.feature_no = j.child_feature_no "
        + "   AND c.feature_no = j.parent_feature_no ";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
        
    private String getLocation(String refId, String chromosomeRefId, String start, String stop, 
                               String strand) 
    throws ObjectStoreException {
        Item location = createItem("Location");
        location.setAttribute("start", start);
        location.setAttribute("end", stop);                
        location.setAttribute("strand", strand);
        location.setReference("subject", refId);
        location.setReference("object", chromosomeRefId);
        try {
            store(location);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
        return location.getIdentifier();
    }
    
    private String getSequence(String length, String residues) 
    throws ObjectStoreException {
        Item seq = createItem("Sequence");
        seq.setAttribute("residues", residues);
        seq.setAttribute("length", length);
        String md5checksum = Util.getMd5checksum(residues);
        seq.setAttribute("md5checksum", md5checksum);            
        try {
            store(seq);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
        return seq.getIdentifier();
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
    
    private String getPlasmid(String id, String identifier)
    throws ObjectStoreException {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        String refId = plasmids.get(id);
        if (refId == null) {
            Item item = createItem("Plasmid");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", organism);
            refId = item.getIdentifier();
            plasmids.put(id, refId);
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
