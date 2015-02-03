package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2014 Phytozome
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 
 * @author
 */
public class PhytozomeHomologConverter extends BioDBConverter
{
    // 
    private static final String DATASET_TITLE = "Phytozome Homologs";
    private static final String DATA_SOURCE_NAME = "Phytozome";


    // a comma delimited string of proteome id's
    private String proteomeIds = null;
    private Connection connection;
    private static final Logger LOG =
        Logger.getLogger(PhytozomeHomologConverter.class);
    // a map of entered genes, organisms and families
    private HashMap<Integer,HashMap<String,String>> geneMap;
    private HashMap<Integer,String> orgMap;
    private HashMap<Integer,String> familyMap;
    // the homolog relationships between gene 1 (key) and gene 2 (set)
    // to check for duplicates.
    private HashMap<String,HashSet<String>> homoMap;
    

    /**
     * Construct a new PhytozomeHomologConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     * @throws SQLException 
     */
    public PhytozomeHomologConverter(Database database, Model model, ItemWriter writer) throws SQLException {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
        connection = getDatabase().getConnection();
        geneMap = new HashMap<Integer,HashMap<String,String>>();
        orgMap = new HashMap<Integer,String>();
        familyMap = new HashMap<Integer,String>();
        homoMap = new HashMap<String,HashSet<String>>();
    }


    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
      // a database has been initialised from properties starting with db.phytozome-ortholog

      ResultSet res = getOrthologData();
      int ctr = 0;
      while (res.next()) {
        Integer p1 = res.getInt(1);
        if (res.wasNull()) {
          throw new BuildException("Proteome Id cannot be null");
        }
        String g1 = res.getString(2);
        if (res.wasNull()) {
          throw new BuildException("Gene name cannot be null");
        }
        Integer p2 = res.getInt(3);
        if (res.wasNull()) {
          throw new BuildException("Proteome Id cannot be null");
        }
        String g2 = res.getString(4);
        if (res.wasNull()) {
          throw new BuildException("Gene name cannot be null");
        }
        String rel = res.getString(5);
        if (res.wasNull()) {
          rel = null;
        }
        Double score = res.getDouble(6);
        if (res.wasNull()) {
          score = null;
        }
        Integer cluster = res.getInt(7);
        if (res.wasNull()) {
          cluster = null;
        }
        String treeType = res.getString(8);
        if (res.wasNull()) {
          treeType = null;
        }
        String tree = res.getString(9);
        if (res.wasNull()) {
          tree = null;
        }

        storeIfNeeded(p1,g1);
        storeIfNeeded(p2,g2);

        Item homolog = createItem("Homolog");
        if (rel != null)
        homolog.setAttribute("relationship",rel);
        if (treeType != null)
        homolog.setAttribute("type",treeType);
        if (tree != null) 
        homolog.setAttribute("tree" ,tree);
        if (score != null) 
        homolog.setAttribute("bootscore",score.toString());
        if (homoMap.containsKey(geneMap.get(p1).get(g1)) ){
          if (homoMap.get(geneMap.get(p1).get(g1)).contains(geneMap.get(p2).get(g2)) ) {
          throw new BuildException("Duplicated genes in relationships.");
          } else {
            homoMap.get(geneMap.get(p1).get(g1)).add(geneMap.get(p2).get(g2));
          }
        } else {
          homoMap.put(geneMap.get(p1).get(g1), new HashSet<String>());
        }
        homoMap.get(geneMap.get(p1).get(g1)).add(geneMap.get(p2).get(g2));
        homolog.setReference("organism1",orgMap.get(p1));
        homolog.setReference("gene1",geneMap.get(p1).get(g1));
        homolog.setReference("organism2",orgMap.get(p2));
        homolog.setReference("gene2",geneMap.get(p2).get(g2));
        if (cluster != null) {
          if (!familyMap.containsKey(cluster)) {
            Item pF = createItem("ProteinFamily");
            pF.setAttribute("clusterId", cluster.toString());
            try {
              store(pF);
            } catch (ObjectStoreException e) {
              throw new BuildException("Trouble storing protein family: "+e.getMessage());
            }
            familyMap.put(cluster, pF.getIdentifier());
          }
          homolog.setReference("cluster", familyMap.get(cluster));
        }
        try {
          store(homolog);
        } catch (ObjectStoreException e) {
          throw new BuildException("Trouble storing homolog: "+e.getMessage());
        }
        ctr++;
        if (ctr%10000 == 0) {
          LOG.info("Processed "+ctr+" records...");
        }
      }
      
      LOG.info("Processed "+ctr+" records.");
    }
    
    private void storeIfNeeded(Integer p, String g) {

      if (!geneMap.get(p).containsKey(g)) {
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier",g);
        gene.setReference("organism",orgMap.get(p));
        try {
          store(gene);
        } catch (ObjectStoreException e) {
          throw new BuildException("Trouble storing gene "+g);
        }
        geneMap.get(p).put(g, gene.getIdentifier());
      }
    }
  private ResultSet getOrthologData() {
    ResultSet res = null;
    if (proteomeIds == null) {
      throw new BuildException("Proteome Ids must set.");
    }
    try {
      Statement stmt = connection.createStatement();
      // we're going to want transcript id and taxon id as strings. so cast them here
      String query = "select t1.proteomeId as proteome_1, " +
          "t1.locusName as gene_1, " +
          "t2.proteomeId as proteome_2, " +
          "t2.locusName as gene_2, " +
          "r.name as relationship, " +
          "bootscore, clusterId, " +
          "t.name as tree_type, tree " +
          "FROM " +
          "transcript t1, transcript t2, homolog h, " +
          "homologRelationship r, treeType t, tree " +
          "WHERE " +
          "t1.proteomeId in ("+ proteomeIds + ") AND " +
          "t2.proteomeId in ("+ proteomeIds + ") AND " +
          "t1.id=transcriptId_1 AND " +
          "t2.id=transcriptId_2 AND " +
          "tree.treeType=t.id AND " +
          "homologRelationshipId=r.id AND " +
          "reconTreeId=tree.id";
      res = stmt.executeQuery(query);
    } catch (SQLException e) {
      throw new BuildException("Trouble getting family members names: " + e.getMessage());
    }

    return res;
  }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        return DATASET_TITLE;
    }
    /*
     * set the list of proteome ids as a comma-delimited list.
     */
    public void setProteomeIds(String inString)
    {
      StringBuilder newString = new StringBuilder();
      // take the opportunity to initialize the geneMap.
      // this also validates the proteome ids'
      for (String proteome : inString.split("[, ]") ) {
        try {
          Integer i = Integer.valueOf(proteome);
          if (!geneMap.containsKey(i)) {
            // we'll silently ignore duplicated proteome ids'
            geneMap.put(i, new HashMap<String,String>());
            Item org = createItem("Organism");
            org.setAttribute("proteomeId", i.toString());
            try {
              store(org);
            } catch (ObjectStoreException e) {
              throw new BuildException("Trouble storing organism "+i+": "+e.getMessage());
            }
            orgMap.put(i, org.getIdentifier());
          }
        } catch (NumberFormatException e) {
          throw new BuildException(proteome+" is not a valid integer.");
        }
        if (newString.length()>0 ) newString.append(",");
        newString.append(proteome);
      }
      proteomeIds = newString.toString();
    }
    public String getProteomeIds()
    {
      return proteomeIds;
    }


}
