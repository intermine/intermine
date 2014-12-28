package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

/**
 * 
 * @author
 */
public class PacClustersConverter extends BioDBConverter
{
    // 
    private static final String DATASET_TITLE = "Phytozome Protein Families";;
    private static final String DATA_SOURCE_NAME = "Phytozome";

    protected Map<Integer,ArrayList<Integer>> methodToClusterList = new HashMap<Integer,ArrayList<Integer>>();
    protected Map<String,String> methodNames = new HashMap<String,String>();
    protected String methodIds = null;
    protected Map<String,Map<String,Item>> proteinMap = new HashMap<String,Map<String,Item>>();
    protected Map<String,Map<String,Item>> geneMap = new HashMap<String,Map<String,Item>>();
    protected Map<String,String> organismMap = new HashMap<String,String>();
    protected Map<String,String> crossrefMap = new HashMap<String,String>();
    protected Map<String,String> dataSourceMap = new HashMap<String,String>();
    private Connection connection;

    private static final Logger LOG =
        Logger.getLogger(PacClustersConverter.class);

    /**
     * Construct a new PacClustersConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     */
    public PacClustersConverter(Database database, Model model, ItemWriter writer) {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
    }


    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
      // a database has been initialised from properties starting with db.pac-clusters
      connection = getDatabase().getConnection();
      fillMethodMap();
      ResultSet res = getProteinFamilies();

      int ctr = 0;
      while (res.next()) {
        // uncompress and process msa
        Blob zMSA = res.getBlob("zMSA");
        StringBuffer msaString = null;
        if ( zMSA != null) {
          Inflater inf = new Inflater();
          inf.setInput(zMSA.getBytes(1,(int) zMSA.length()));
          int increment;
          msaString = new StringBuffer();
          byte[] msaBuffer = new byte[1000];
          do {
            increment = inf.inflate(msaBuffer,0,1000);
            msaString.append(new String(msaBuffer,0,increment,"UTF-8"));
          } while (inf.getRemaining() > 0);
        }

        Item proFamily = createItem("ProteinFamily");
        String clusterId = res.getString("clusterId");
        proFamily.setAttribute("clusterId", clusterId);
        setIfNotNull(proFamily,"clusterName",res.getString("clusterName"));
        
        proFamily.setAttribute("methodId", res.getString("methodId"));
        setIfNotNull(proFamily,"methodName", methodNames.get(res.getString("methodId")));
        String consensusSequence = res.getString("sequence");

        HashMap<String,String> idToName = registerProteins(clusterId,proFamily);
        
        // for singletons, there will (never?) be a consensus sequence
        // it is the protein sequence.
        if ((idToName.keySet().size()==1) && (consensusSequence==null) ) {
          consensusSequence = getPeptideSequence((String)idToName.keySet().toArray()[0]);
        }
        if (consensusSequence != null && !consensusSequence.isEmpty()) {
          String sequenceIdentifier = storeSequence(consensusSequence);
          proFamily.setReference("consensus", sequenceIdentifier);
        }
        
        if (msaString != null) {
          String newMSA = reformatMSA(msaString,idToName);
          Item msa = createItem("MSA");
          msa.setAttribute("primaryIdentifier","Cluster "+clusterId+" alignment");
          msa.setAttribute("alignment", newMSA);
          try {
            store(msa);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing MSA: " + e.getMessage());
          }
          proFamily.setReference("msa",msa.getIdentifier());
        }
        registerCrossReferences(clusterId,proFamily);
        try {
          store(proFamily);
          ctr++;
          if (ctr%10000 == 0) {
            LOG.info("Stored "+ctr+" clusters...");
          }
        } catch (ObjectStoreException e) {
          throw new BuildException("Problem storing protein family." + e.getMessage());
        }
      }
      LOG.info("Stored "+ctr+" clusters.");
      // now register proteins and genes
      for( String proteomeId : proteinMap.keySet()) {
        Map<String,Item> prots = proteinMap.get(proteomeId);
        for( String protein: prots.keySet()) {
          store(prots.get(protein));
        }
      }
      for( String proteomeId : geneMap.keySet()) {
        Map<String,Item> genes = geneMap.get(proteomeId);
        for( String gene: genes.keySet()) {
          store(genes.get(gene));
        }
      }
    }
    
    HashMap<String,String> registerProteins(String clusterId, Item family) throws Exception{
      ResultSet res = getFamilyMembers(clusterId);
      HashMap<String,String> idToName = new HashMap<String,String>();
      Map<String,Integer>organismCount = new HashMap<String,Integer>();
      while( res.next()) {
        String proteomeId = res.getString("proteomeid");
        idToName.put(res.getString("transcriptId"), res.getString("peptideName"));
        if (!organismCount.containsKey(proteomeId)) {
          organismCount.put(proteomeId, new Integer(1));
        } else {
          Integer j = organismCount.get(proteomeId);
          organismCount.put(proteomeId, ++j);
        }
        if (!organismMap.containsKey(proteomeId)) {
          Item o = createItem("Organism");
          o.setAttribute("proteomeId", proteomeId);
          store(o);
          organismMap.put(proteomeId, o.getIdentifier());
          proteinMap.put(proteomeId, new HashMap<String,Item>());
          geneMap.put(proteomeId, new HashMap<String,Item>());
        }
        String proteinName = res.getString("peptideName");
        if (!proteinMap.get(proteomeId).containsKey(proteinName)) {
          Item p = createItem("Protein");
          p.setAttribute("primaryIdentifier",proteinName);
          p.setReference("organism",organismMap.get(proteomeId));
          proteinMap.get(proteomeId).put(proteinName,p);
        }
        //family.addToCollection("protein", proteinMap.get(proteomeId).get(proteinName));
        //proteinMap.get(proteomeId).get(proteinName).addToCollection("proteinFamily",family.getIdentifier());
        String geneName = res.getString("locusName");
        if (!geneMap.get(proteomeId).containsKey(geneName)) {
          Item g = createItem("Gene");
          g.setAttribute("primaryIdentifier",geneName);
          g.setReference("organism",organismMap.get(proteomeId));
          geneMap.get(proteomeId).put(geneName,g);
        }
        family.addToCollection("gene", geneMap.get(proteomeId).get(geneName));
        Item pfm = createItem("ProteinFamilyMember");
        pfm.setAttribute("membershipDetail", res.getString("name"));
        pfm.setReference("organism", organismMap.get(proteomeId));
        pfm.setReference("protein", proteinMap.get(proteomeId).get(proteinName));
        pfm.setReference("proteinFamily",family.getIdentifier());
        store(pfm);
        family.addToCollection("member",pfm.getIdentifier());
       
      }
      res.close();
      // register the organism counts
      Integer memberCount = new Integer(0);
      for( String proteomeId : organismCount.keySet()) {
        memberCount += organismCount.get(proteomeId);/*
        Item count = createItem("ProteinFamilyOrganism");
        count.setAttribute("count", organismCount.get(proteomeId).toString());
        count.setReference("proteinFamily",family.getIdentifier());
        count.setReference("organism",organismMap.get(proteomeId));
        family.addToCollection("proteinFamilyOrganism",count.getIdentifier());
        store(count);*/
      }
      family.setAttribute("memberCount",memberCount.toString());
      return idToName;
    }

    protected String storeSequence(String residues)  throws ObjectStoreException {
      if ( residues.length() == 0) {
        return null;
      }
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new BuildException("No such algorithm for md5?");
      }
      Item sequence = createItem("Sequence");
      sequence.setAttribute("residues",residues);
      md.update(residues.getBytes(),0,residues.length());
      String md5sum = new BigInteger(1,md.digest()).toString(16);
      sequence.setAttribute("md5checksum",md5sum);
      Integer len = new Integer(residues.length());
      sequence.setAttribute("length",len.toString());
      try {
        store(sequence);
        return sequence.getIdentifier();
      } catch (ObjectStoreException e) {
        throw new BuildException("Problem storing sequence." + e.getMessage());
      }
    }
    
    public String getPeptideSequence(String transcriptId) {
      ResultSet res = null;
      try {
        Statement stmt = connection.createStatement();
        String query = "SELECT peptide FROM "
            + " transcript"
            + " WHERE id="+transcriptId;
        res = stmt.executeQuery(query);
        // should only have 1 row; we'll return the first non-empty value,
        // but we'll remove terminal stops.
        while( res.next()) {
          String peptide = res.getString("peptide");
          if (peptide.endsWith("*")) {
            return peptide.substring(0,peptide.length()-1);
          } else if (!peptide.isEmpty() ) {
            return peptide;
          }
        }

      } catch (SQLException e) {
        throw new BuildException("Trouble getting singleton peptide: " + e.getMessage());
      }
      return null;
    }
    
    public ResultSet getFamilyMembers(String clusterId) {
      ResultSet res = null;
      try {
        Statement stmt = connection.createStatement();
        // we're going to want transcript id and proteome id as strings. so cast them here
        String query = "select peptideName,"
                             + " locusName, "
                             + " cast(t.id as char) as transcriptId, "
                             + " cast(p.id as char) as proteomeid, "
                             + " m.name from"
                             + " clusterJoin c, proteome p, transcript t, membershipDetail m"
                             + " where clusterId="+clusterId 
                             + " and memberId=t.id and c.active=1"
                             + " and m.id=memShipDetailId"
                             + " and t.active=1 and p.id=c.proteomeId";
        res = stmt.executeQuery(query);
      } catch (SQLException e) {
        throw new BuildException("Trouble getting family members names: " + e.getMessage());
      }

      return res;
    }
    public ResultSet getProteinFamilies() {
      // process data with direct SQL queries on the source database
      ResultSet res = null;
      try {
        Statement stmt = connection.createStatement();
        // we're going to want clusterId as string. so cast it here
        String query = "SELECT zMSA,sequence,clusterName,"
            + " CAST(clusterDetail.id AS char) as clusterId,"
            + " CAST(methodId as char) AS methodId"
            + " FROM"
            + " clusterDetail LEFT OUTER JOIN"
            + " (msa LEFT OUTER JOIN centroid"
            + " ON centroid.msaId=msa.id) "
            + " ON msa.clusterId=clusterDetail.id"
            + " WHERE"
            + " clusterDetail.active=1 AND"
            + " clusterDetail.methodId IN ("+methodIds+")";
        LOG.info("Executing query: "+query);
        res = stmt.executeQuery(query);
      } catch (SQLException e) {
        throw new BuildException("Trouble method names: " + e.getMessage());
      }
      return res;
    }

    void registerCrossReferences(String clusterId, Item family) throws Exception{
      ResultSet res = getFamilyCrossReferences(clusterId);

      //TODO: right now we only have KOG terms to worry about.
      // This may be more complex later.
      while( res.next()) {
        String value = res.getString("value");
        String dbName = res.getString("name");
        if (!dataSourceMap.containsKey(dbName)) {
          Item source = createItem("DataSource");
          source.setAttribute("name",dbName);
          store(source);
          dataSourceMap.put(dbName,source.getIdentifier());
        }
        if (!crossrefMap.containsKey(value)) {
          Item crossref = createItem("CrossReference");
          crossref.setAttribute("identifier",value);
          crossref.setReference("source",dataSourceMap.get(dbName));
          store(crossref);
          crossrefMap.put(value,crossref.getIdentifier());
        }
        family.addToCollection("crossReferences",crossrefMap.get(value));
      }
      res.close();
    }
      
    protected ResultSet getFamilyCrossReferences(String clusterId) {
      ResultSet res;
      try {
        Statement stmt = connection.createStatement();
        // we're going to want clusterId as string. so cast it here
        String query = "select value,'KOG' as name"
                             + " from"
                             + " annotation a, annotationField f, objectType t"
                             + " where ObjectId="+clusterId 
                             + " and fieldId=f.id and a.active=1"
                             + " and objectTypeId=t.id and t.name ='cluster'"
                             + " and f.name='clusterKogLetter'";
        res = stmt.executeQuery(query);
      } catch (SQLException e) {
        throw new BuildException("Trouble getting cluster annotations: " + e.getMessage());
      }

      return res;
    }
    String reformatMSA(StringBuffer msa, HashMap<String,String>idToName) {
      // idLength is the max length of the id string, nameLength is the
      // max length of the replacement id string. The difference is the
      // number of spaces we need to pad
      //
      // The transcript numbers could be of different width, but I'm 
      // assuming one of them has no space before the start.
      int nameLength = 0;
      int idLength = 0;
      for (String id : idToName.keySet() ) {
        idLength = (id.length() > idLength)?
            id.length():idLength;
        nameLength = (idToName.get(id).length() >nameLength)?
            idToName.get(id).length():nameLength;
      }
      String[] lines = msa.toString().split("\\n");
      String[] processedLines = new String[lines.length];
      // precompile patterns
      HashMap<String,Pattern> idToPattern = new HashMap<String,Pattern>();
      for ( String id : idToName.keySet()) {
        idToPattern.put(id, Pattern.compile(" *"+id+" .*"));
      }
      
      int lineCtr = 0;
      for( String line : lines ) {
        // scan through every line, replacing id with name:id
        for( String id : idToName.keySet() ) {
          // Look for maybe space at the beginning,
          // the number and a single space character
          // followed by the rest of the line
          Pattern p = idToPattern.get(id);
          if (p.matcher(line).matches()) {
            String replacement = idToName.get(id)+":"+id;
            int nSpaces = nameLength - idToName.get(id).length() + 1;
            StringBuffer s = new StringBuffer(line.replaceFirst(" *"+id+" ", replacement));
            for(int i=0;i<nSpaces;i++) s.insert(0, " ");
            processedLines[lineCtr] = s.toString();
            break;
          }
        }
        // not touched? copy. maybe with spaces.
        if (processedLines[lineCtr] == null) {
          if (line.length() == 0) {
            processedLines[lineCtr] = line;
          } else if (lineCtr == 1) {
            // header line. Do not pad with spaces
            processedLines[lineCtr] = line;
          } else {
            // add spaces
            StringBuffer s = new StringBuffer();
            // we're also adding a :. So +1
            for(int i=0;i<nameLength+1;i++) s.append(" ");
            s.append(line);
            processedLines[lineCtr] =s.toString();
          }
        }
        lineCtr++;
      }
      StringBuffer returnMSA = new StringBuffer();
      for( String line: processedLines ) {
        if (returnMSA.length() > 0) returnMSA.append("\\n");
        returnMSA.append(line);
      }
      return returnMSA.toString();
    }
    /*
     * set the list of method ids as a comma-delimited list.
     */
    public void setMethodIds(String inString)
    {
      methodIds = inString;
    }
    public String getMethodIds()
    {
      return methodIds;
    }

    void setIfNotNull(Item s,String field,String value) {
      if (value != null && value.trim().length() > 0) {
        s.setAttribute(field,value.trim());
      }
    }
    private void fillMethodMap()
    {
      if (methodIds == null) return;
      String query = "select id,name,adjective from method where id in ("+methodIds+")";
      try {
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        while (res.next() ) {
          String name = res.getString("name");
          if (name==null) {
            name = res.getString("adjective");
          }
          methodNames.put((new Integer(res.getInt("id"))).toString(),name);
        }
        res.close();
      } catch (SQLException e) {
        throw new BuildException("Trouble method names: " + e.getMessage());
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int proteomeId) {
        return DATASET_TITLE;
    }
}
