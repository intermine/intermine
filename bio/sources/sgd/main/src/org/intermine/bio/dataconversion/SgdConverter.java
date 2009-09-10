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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

/**
 * Converts results sets into intermine objects
 * @author Julie Sullivan
 */
public class SgdConverter extends BioDBConverter
{
//    private static final Logger LOG = Logger.getLogger(SgdConverter.class);
    private static final String DATASET_TITLE = "SGD data set";
    private static final String DATA_SOURCE_NAME = "SGD";
    private Map<String, String> chromosomes = new HashMap(); 
    private Map<String, String> plasmids = new HashMap();
    private Map<String, String> phenotypes = new HashMap();
    private Map<String, String> literatureTopics = new HashMap();
    private Map<String, Item> genes = new HashMap();
    private Map<String, String> synonyms = new HashMap(); 
    private Map<String, String> publications = new HashMap();
    private static final String TAXON_ID = "4932";
    private Item organism;
//    private Map<String, List<String>> featureMap = new HashMap();
    private static final SgdProcessor PROCESSOR = new SgdProcessor();
    
    /**
     * Construct a new SgdConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     * @throws ObjectStoreException if organism can't be stored
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
        processPhenotypes(connection);
    }
   
    private void processGenes(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = PROCESSOR.getGeneResults(connection);
        while (res.next()) {
            String featureNo = res.getString("feature_no");
            if (genes.get(featureNo) == null) {
                
                // ~~~ gene ~~~                 
                String primaryIdentifier = res.getString("dbxref_id");
                String secondaryIdentifier = res.getString("feature_name");                
                String symbol = res.getString("gene_name");                
                String name = res.getString("name_description");
                String description = res.getString("headline");
                
                Item item = createItem("Gene");
                item.setAttribute("primaryIdentifier", primaryIdentifier); 
                if (StringUtils.isNotEmpty(name)) {
                    item.setAttribute("name", name); 
                }
                item.setAttribute("featureType", res.getString("feature_type"));  
                if (StringUtils.isNotEmpty(description)) {
                    item.setAttribute("description", description); 
                }
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
        ResultSet res = PROCESSOR.getGeneLocationResults(connection);
        while (res.next()) {
            String featureNo = res.getString("feature_no");
            String geneFeatureNo = res.getString("gene_feature_no");
            String featureType = res.getString("feature_type");
            
            Item item = genes.get(geneFeatureNo);

            //  ~~~ chromosome OR plasmid ~~~                   
            String refId = null;            
            if (featureType.equalsIgnoreCase("plasmid")) {
                refId = getPlasmid(featureNo, res.getString("identifier"));
                item.setReference("plasmid", refId);
            } else if (featureType.equalsIgnoreCase("chromosome")) {
                refId = getChromosome(featureNo, res.getString("identifier"));
                item.setReference("chromosome", refId);
            }

            // ~~~ location ~~~
            String locationRefId = getLocation(item, refId, res.getString("start_coord"), 
                                               res.getString("stop_coord"), 
                                               res.getString("strand"));

            if (featureType.equalsIgnoreCase("plasmid")) {
                item.setReference("plasmidLocation", locationRefId);
            } else {
                item.setReference("chromosomeLocation", locationRefId);
            }
                
        }      
    }
        
    private void processCDSs(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = PROCESSOR.getCDSResults(connection);
        while (res.next()) {
            String secondaryIdentifier = res.getString("feature_name");
            String primaryIdentifier = res.getString("dbxref_id"); 
            String geneFeatureNo = res.getString("gene_feature_no");
            Item gene =  genes.get(geneFeatureNo);
            if (gene == null) {
                throw new RuntimeException("Gene not found:  " + geneFeatureNo);
            }
            Item item = createItem("Exon");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setAttribute("secondaryIdentifier", secondaryIdentifier);
            item.setReference("organism", organism);
            item.setReference("gene", gene.getIdentifier());
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            /**
            String residues = res.getString("residues");
            String length = res.getString("seq_length");


            String geneRefId = gene.getIdentifier();
            
            // ~~~ sequence ~~~
            String seq = getSequence(length, residues);
            
    
            
            // ~~~ CDS ~~~            
            Item transcript = createItem("Transcript");
            transcript.setAttribute("primaryIdentifier", primaryIdentifier);
            transcript.setAttribute("secondaryIdentifier", secondaryIdentifier);
            transcript.setAttribute("length", length);
            transcript.setReference("sequence", seq);
            transcript.setReference("organism", organism);
            transcript.setReference("gene", geneRefId);
            
            String refId = transcript.getIdentifier();
            
            // TODO store these last
            getSynonym(refId, "identifier", secondaryIdentifier);
            getSynonym(refId, "identifier", primaryIdentifier);
            
            Item CDS = createItem("CDS");
            CDS.setAttribute("primaryIdentifier", primaryIdentifier);
            CDS.setAttribute("secondaryIdentifier", secondaryIdentifier);
            transcript.setAttribute("length", length);
            transcript.setReference("sequence", seq);
            CDS.setReference("organism", organism);
            CDS.setReference("gene", geneRefId);
            
            refId = CDS.getIdentifier();
            
            // TODO store these last
            getSynonym(refId, "identifier", secondaryIdentifier);
            getSynonym(refId, "identifier", primaryIdentifier);
            
            // ~~~ location ~~~
            String strand = res.getString("strand");
            String start = (strand.equals("C") ? res.getString("start_coord") : 
                res.getString("stop_coord"));
            String stop = (strand.equals("C") ? res.getString("stop_coord") : 
                res.getString("start_coord"));
            
            transcript.setAttribute("length", getLength(start, stop));
            String locationRefId = getLocation(transcript.getIdentifier(), 
                                               geneRefId, start, stop, strand);
            transcript.setReference("chromosomeLocation", locationRefId);
            
            try {
                store(transcript);
                store(CDS);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            **/
        }
    }
    
    private void processPubs(Connection connection)     
    throws SQLException, ObjectStoreException {
        ResultSet res = PROCESSOR.getPubResults(connection);
        while (res.next()) {
            
            String featureNo = res.getString("reference_no");
            String geneFeatureNo = res.getString("gene_feature_no");
            Item gene = genes.get(geneFeatureNo);
            if (gene == null) {
                continue;
            }
            
            String issue = res.getString("issue");                
            String volume = res.getString("volume");
            String pubMedId = res.getString("pubmed");
            String pages = res.getString("page");
            String title = res.getString("title");
            String year = res.getString("year");
            String citation = res.getString("citation");
            String refId = getPub(featureNo, issue, volume, pubMedId, pages, title, year, citation);

            gene.addToCollection("publications", refId);
            
            String topic = res.getString("property_value");            
            if (res.getString("property_type").equals("literature_topic")) {
                Item item = createItem("PublicationAnnotation");
                item.setReference("gene", gene);
                item.setReference("literatureTopic", getLiteratureTopic(topic));
                item.addToCollection("publications", refId);
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new ObjectStoreException(e);
                }                
            }
        }
    }
    
    private void processBindingSites(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = PROCESSOR.getBindingSiteResults(connection);
        while (res.next()) {
            // ~~~ binding site ~~~                 
            String primaryIdentifier = res.getString("dbxref_id");
            String secondaryIdentifier = res.getString("feature_name");                

            Item item = createItem("TFBindingSite");
            item.setAttribute("primaryIdentifier", primaryIdentifier); 
            item.setAttribute("secondaryIdentifier", secondaryIdentifier);
            item.setReference("organism", organism);
            String refId = item.getIdentifier();
            
            // TODO store binding site locations.  the relationship to chromosome isn't clear
//            String locationRefId = getLocation(item, refId, res.getString("start_coord"), 
//                                               res.getString("stop_coord"), 
//                                               res.getString("strand"));
//            item.setReference("chromosomeLocation", locationRefId);
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

    private void processPhenotypes(Connection connection) 
    throws SQLException, ObjectStoreException {
        ResultSet res = PROCESSOR.getPhenotypeResults(connection);
        while (res.next()) {

            String geneFeatureNo = res.getString("gene_feature_no");
            Item gene = genes.get(geneFeatureNo);
            if (gene == null) {
                // TODO this shouldn't happen, throw an exception!
                continue;
            }
            
            String phenotypeNo = res.getString("phenotype2_no");
            String experimentType = res.getString("experiment_type");
            String mutantType = res.getString("mutant_type");
            String qualifier = res.getString("qualifier");
            String observable = res.getString("observable");            
//            String experimentComment = res.getString("experiment_comment");
//            String experimentNo = res.getString("experiment_no");
            
            String phenotypeRefId = getPhenotype(phenotypeNo, experimentType, mutantType, 
                                                 qualifier, observable);
            
            Item item = createItem("PhenotypeAnnotation");
// TODO this doesn't seem to do anything right now
//            item.setReference("experiment", getExperiment(experimentNo, experimentComment));
            item.setReference("gene", gene.getIdentifier());
            item.setReference("phenotype", phenotypeRefId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

        }
    }

//    private void addCollection(String collectionName) {
//        for (Map.Entry<String, List<String>> entry : featureMap.entrySet()) {
//            String featureNo = entry.getKey();
//            List<String> pubRefIds = entry.getValue();
//            Item gene = genes.get(featureNo);
//            if (gene != null) {
//                gene.setCollection(collectionName, pubRefIds);
//            }
//        }
//        featureMap = new HashMap();
//    }
//        
//    private void addFeature(String featureNo, String refId) {
//        if (featureMap.get(featureNo) == null) {
//            featureMap.put(featureNo, new ArrayList());            
//        }
//        featureMap.get(featureNo).add(refId);
//    }
        
    private String getLocation(Item subject, String chromosomeRefId, String startCoord, 
                               String stopCoord, String strand) 
    throws ObjectStoreException {

        String start = (strand.equals("C") ? stopCoord : startCoord);
        String end = (strand.equals("C") ? startCoord : stopCoord);

        if (StringUtils.isEmpty(startCoord)) {
            start = "0";
        }
        if (StringUtils.isEmpty(stopCoord)) {
            end = "0";
        }
        
        subject.setAttribute("length", getLength(start, end));
        
        Item location = createItem("Location");
        location.setAttribute("start", start);
        location.setAttribute("end", end);                
        location.setAttribute("strand", strand);
        location.setReference("subject", subject);
        location.setReference("object", chromosomeRefId);
        try {
            store(location);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
        return location.getIdentifier();
    }
    
    private String getLength(String start, String end)
    throws NumberFormatException {
        Integer a = new Integer(start);
        Integer b = new Integer(end);

        // if the coordinates are on the crick strand, they need to be reversed or they
        // result in a negative number
//        if (a.compareTo(b) > 0) {
//            a = new Integer(end);
//            b = new Integer(start);
//        }

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

    private String getPhenotype(String phenotypeNo, String experimentType, String mutantType, 
                                String qualifier, String observable) 
    throws ObjectStoreException {
        
        String refId = phenotypes.get(phenotypeNo);
        if (refId == null) {
            Item item = createItem("Phenotype");
            item.setAttribute("mutantType", mutantType);
            if (StringUtils.isNotEmpty(qualifier)) {
                item.setAttribute("qualifier", qualifier);
            }
            item.setAttribute("observable", observable);
            item.setAttribute("experimentType", experimentType);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }            
            refId = item.getIdentifier();
            phenotypes.put(phenotypeNo, refId);
        }
        return refId;
    }
    
    private String getLiteratureTopic(String topic) 
    throws ObjectStoreException {        
        String refId = literatureTopics.get(topic);
        if (refId == null) {
            Item item = createItem("LiteratureTopic");
            item.setAttribute("name", topic);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            refId = item.getIdentifier();
            literatureTopics.put(topic, refId);
        }
        return refId;
    }
    
    private String getPub(String featureNo, String issue, String volume, String pubMedId, 
                          String pages, String title, String year, String citation) 
    throws ObjectStoreException {  
        String refId = publications.get(featureNo);
        if (refId == null) {
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
            item.setAttribute("year", year);
            if (StringUtils.isNotEmpty(pages)) {
                item.setAttribute("pages", pages);
            }
            if (StringUtils.isNotEmpty(citation)) {
                item.setAttribute("citation", citation);
            }
            refId = item.getIdentifier();
            publications.put(featureNo, refId);

            try {
                store(item);
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
    public String getDataSetTitle(@SuppressWarnings("unused") int taxonId) {
        return DATASET_TITLE;
    }
}
