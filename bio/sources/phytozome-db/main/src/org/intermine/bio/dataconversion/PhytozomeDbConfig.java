/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

/**
 * PhytozomeDbConfig A class to encapsulate the chado details of
 * what we want to put into intermine.
 * 
 * Since many chado db's can have different implementations, the intent
 * of this class is to capture all the local details of the dialect 
 * (i.e. are they 'proteins' or 'polypeptides'), the mapping to the
 * intermine classes (i.e. Protein). Also, this incorporates some
 * knowledge of references and collections in the genomic model. (It
 * might be nice to make this discoverable.)
 * 
 * It also provides some convenience routines for making SQL strings
 * and the like.
 * 
 * @author jcarlson
 *
 */
public class PhytozomeDbConfig {

  //default genome-like feature types - i.e. those types of features
  // that are the collections of chromosome-type features
  public static final List<String> GENOME_FEATURES =
      Arrays.asList("genome");
  // default chromosome-like feature types - i.e. those types of features
  // that are the assembled objects of a sequencing project. Everything
  // will go into the 'chromosome' table in the mine
  public static final List<String> CHROMOSOME_FEATURES =
      Arrays.asList("chromosome");
  // default feature types to query from the feature table
  public  static final  List<String> ANNOTATION_FEATURES =
      Arrays.asList( "gene", "mRNA",
          "polypeptide","intron","exon","CDS",
          "five_prime_UTR","three_prime_UTR");
  // a mapping of chado type -> intermine class. And initialize
  static final HashMap<String,String> typeMap;
  static {
    typeMap = new HashMap<String, String>();
    typeMap.put("gene", "Gene");
    typeMap.put("mRNA", "MRNA");
    typeMap.put("exon", "Exon");
    typeMap.put("polypeptide", "Protein");
    typeMap.put("protein", "Protein");
    typeMap.put("five_prime_UTR", "FivePrimeUTR");
    typeMap.put("three_prime_UTR", "ThreePrimeUTR");
    typeMap.put("intron", "Intron");
    typeMap.put("CDS", "CDS");
  }

  // our knowledge of reference in classes.
  // i.e. protein has a reference to gene, called 'Gene'
  // so there is an entry MultiKey("Protein","Gene"),"Gene"
  static final HashMap<MultiKey,String> referenceMap;
  static {
    referenceMap = new HashMap<MultiKey,String>();
    referenceMap.put(new MultiKey("MRNA","Gene"),"gene");
    referenceMap.put(new MultiKey("Exon","Gene"),"gene");
    referenceMap.put(new MultiKey("MRNA","Protein"),"protein");
    referenceMap.put(new MultiKey("CDS","Gene"),"gene");
    referenceMap.put(new MultiKey("CDS","Protein"),"protein");
    referenceMap.put(new MultiKey("CDS","MRNA"),"transcript");
  }
  
  //TODO: is there a way to discover this?
  static final HashMap<MultiKey,String> collectionMap;
  static {
    collectionMap = new HashMap<MultiKey,String>();
    collectionMap.put(new MultiKey("Exon","MRNA"),"transcripts");
    collectionMap.put(new MultiKey("Intron","MRNA"),"transcripts");
    collectionMap.put(new MultiKey("FivePrimeUTR","MRNA"),"transcripts");
    collectionMap.put(new MultiKey("MRNA","Intron"),"introns");
    collectionMap.put(new MultiKey("MRNA","Exon"),"exons");
    collectionMap.put(new MultiKey("MRNA","FivePrimeUTR"),"UTRs");
    collectionMap.put(new MultiKey("MRNA","ThreePrimeUTR"),"UTRs");
    collectionMap.put(new MultiKey("MRNA","CDS"),"CDSs");
    collectionMap.put(new MultiKey("Gene","Protein"),"proteins");
    collectionMap.put(new MultiKey("Gene","FivePrimeUTR"), "UTRs");
    collectionMap.put(new MultiKey("Gene","MRNA"), "transcripts");
    collectionMap.put(new MultiKey("Gene","ThreePrimeUTR"), "UTRs");
    collectionMap.put(new MultiKey("Gene","CDS"), "CDSs");
  }
  
  public static final List<String> ANALYSIS_FEATURES =
      Arrays.asList("match","match_part","evidence_for_feature");
  // a list of the possible names for the relationship between the
  // different sequence types.
  // This is the relationship type(s) between chromosomes-like things
  // and genome-like things.
  //I'm using contained_in. Maybe someone will use part_of
  public static final List<String> CONTAINED_IN_RELATIONS =
      Arrays.asList("contained_in");
  // The relationship(s) between an exon and an mrna and 
  // between an mrna and a gene
  public static final List<String> PART_OF_RELATIONS =
      Arrays.asList("part_of");
  // The relationship(s) between a polypeptide and a transcript
  public static final List<String> DERIVES_FROM_RELATIONS =
      Arrays.asList("derives_from");
  // featureprop's that we will want
  public static final List<String> GENE_PROPERTIES = 
      Arrays.asList("defline","description","symbol");
  // featureprop's that we will want
  public static final List<String> MRNA_PROPERTIES = 
      Arrays.asList("longest");
  // and a mapping of these names to the relevant attributes
  // some of these only make sense for certain classes.
  static final HashMap<String,String> propMap;
  static {
    propMap = new HashMap<String, String>();
    propMap.put("defline", "briefDescription");
    propMap.put("description", "description");
    propMap.put("longest", "primaryTranscript");
    propMap.put("symbol", "symbol");
  }

  protected static final Logger LOG =
      Logger.getLogger(PhytozomeDbConfig.class);
  // non-static variables
  BioDBConverter converter;
  // and the cvTerms. Indexed by CV, then term_name->cvterm_id
  protected Map<String,Map<String,Integer> > cvTermMap;
  protected Map<Integer,String> cvTermInvMap;
  
  public PhytozomeDbConfig(PhytozomeDbConverter conv) {
    converter = conv;
    cvTermMap =new HashMap<String,Map<String,Integer> >();
    cvTermInvMap =new HashMap<Integer,String>();
    fillCVTable();
  }
  
  private void fillCVTable() {
    String query = "SELECT cv.name, cvterm.name, cvterm.cvterm_id"
        + " FROM  cv, cvterm "
        + " WHERE cvterm.cv_id=cv.cv_id "
        + " AND (( cv.name='sequence' AND cvterm.name in "
        + PhytozomeDbConfig.quoteList(GENOME_FEATURES,
            CHROMOSOME_FEATURES,ANNOTATION_FEATURES,
            ANALYSIS_FEATURES) 
            + " ) "
            + " OR ( cv.name='relationship' AND cvterm.name in "
            + PhytozomeDbConfig.quoteList(CONTAINED_IN_RELATIONS,
                PART_OF_RELATIONS,DERIVES_FROM_RELATIONS)
                + " ) "
                + " OR ( cv.name='feature_property' AND cvterm.name in "
                + PhytozomeDbConfig.quoteList(GENE_PROPERTIES,MRNA_PROPERTIES)
                + " ))";

    LOG.info("executing cvterm query: " + query);
    ResultSet res = null;
    try {
      Statement stmt = converter.getDatabase().getConnection().createStatement();
      res = stmt.executeQuery(query);
    } catch (SQLException e) {
      throw new BuildException("Problem when querying CVTerms " + e);
    }
    LOG.info("got resultset.");
    cvTermMap.put("sequence",new HashMap<String,Integer>());
    cvTermMap.put("relationship",new HashMap<String,Integer>());
    cvTermMap.put("feature_property",new HashMap<String,Integer>());
    int counter = 0;
    try {
      while (res.next()) {
        counter++;
        cvTermMap.get(res.getString(1)).put(res.getString(2),
            new Integer(res.getInt(3)));
        cvTermInvMap.put(new Integer(res.getInt(3)), res.getString(2));
      }
    } catch (SQLException e) {
      throw new BuildException("Problem when querying CVTerms " + e);
    }

    // make sure we have the proper size
    if (counter != GENOME_FEATURES.size() + CHROMOSOME_FEATURES.size() +
        ANNOTATION_FEATURES.size() + CONTAINED_IN_RELATIONS.size() +
        PART_OF_RELATIONS.size() + DERIVES_FROM_RELATIONS.size() +
        GENE_PROPERTIES.size() + MRNA_PROPERTIES.size()) {
      LOG.warn("Did not get the expected number of terms from the CV table.");
    }
  }
  
  public Integer cvTerm(String cv,String term) {
    if (!cvTermMap.containsKey(cv)) {
      throw new BuildException("There is no cv named "+cv);
    } else if (!cvTermMap.get(cv).containsKey(term)) {
      throw new BuildException("The cv "+cv+" has no term "+term);
    } else {
      return cvTermMap.get(cv).get(term);
    }
  }
  public String cvTermInv(Integer i) {
    if (!cvTermInvMap.containsKey(i)) {
      throw new BuildException("There is no cvterm_id named "+i);
    } else {
      return cvTermInvMap.get(i);
    }
  }
  @SafeVarargs
  public static String quoteList(List<String>...lists)
  {
    String quoted = "";
    for (List<String> list : lists) {
      for( String term : list ) {
        if (!quoted.isEmpty()) {
          quoted += ",";
        } else {
          quoted = "(";
        }
        quoted += "'" + term + "'";
      }
    }
    quoted += ")";
    return quoted;
  }
  /**
   * Convert the list of numbers to a string to be used in a SQL query.
   * @return the list of words as a string (in SQL list format), either
   * "=number" if only 1 number or "in (number1,number2,...) if multiple.
   */
  public static String getSQLString(List<Integer> numbers) {
    if (numbers.size() == 0) {
      // we don't expect this to happen. return 'is null'
      return " is null";
    } else if (numbers.size() == 1) {
      return "= " + numbers.get(0).toString();
    } else {
      StringBuffer wordString = new StringBuffer();
      Iterator<Integer> i = numbers.iterator();
      while (i.hasNext()) {
        Integer item = i.next();
        wordString.append(item.toString());
        if (i.hasNext()) {
          wordString.append(", ");
        }
      }
      return wordString.toString();
    }
  }
  
  public static boolean hasReference(String a,String b) {
    return hasReference(new MultiKey(a,b));    
  }
  public static boolean hasReference(MultiKey a) {
    return referenceMap.containsKey(a);    
  }
  public static String referenceName(String a,String b) {
    return referenceName(new MultiKey(a,b));
  }
  public static String referenceName(MultiKey a) {
    if (hasReference(a)) return referenceMap.get(a);
    return null;
  }
  public static boolean hasCollection(String a,String b) {
    return hasCollection(new MultiKey(a,b));    
  }
  public static boolean hasCollection(MultiKey a) {
    return collectionMap.containsKey(a);    
  }
  public static String collectionName(String a,String b) {
    return collectionName(new MultiKey(a,b));    
  }
  public static String collectionName(MultiKey a) {
    return collectionMap.get(a);    
  }
  
  public List<Integer> getFeatures(String type) {
    List<String> rStrings = getFeatureList(type);
    List<Integer> rIntegers = new ArrayList<Integer>();
    for( String r : rStrings) {
      if (cvTermMap.get("sequence").containsKey(r)) {
        rIntegers.add(cvTermMap.get("sequence").get(r));
      };
    }
    return rIntegers;
  }
  
  public String listString(String cv,String[] terms)
  {
    StringBuilder ret = new StringBuilder();
    if (!cvTermMap.containsKey(cv) ) {
      return "";
    }
    Map<String,Integer> cvMap = cvTermMap.get(cv);
    for( String term: terms ) {
      if (cvMap.containsKey(term)) {
        if (ret.length() > 0) {
          ret.append(",");
        }
        ret.append(cvMap.get(term).toString());
      }
    }
    return ret.length()==0?"":ret.toString();

  }
  
  public static String getIntermineType(String chadoType) throws BuildException {
    if (typeMap.containsKey(chadoType)) {
      return typeMap.get(chadoType);
    } else {
      throw new BuildException("Intermine type for "+chadoType+" is not set.");
    }
  }
  public static String getInterminePropertyName(String chadoType)
  throws BuildException {
    if (propMap.containsKey(chadoType)) {
      return propMap.get(chadoType);
    } else {
      throw new BuildException("Intermine type for "+chadoType+" is not set.");
    }
  }
  
  public static List<String> getFeatureList(String type) {
    return "genome".equals(type)?GENOME_FEATURES:
      "chromosome".equals(type)?CHROMOSOME_FEATURES:
        "annotation".equals(type)?ANNOTATION_FEATURES:
          null;
  }
  public static List<String> getRelationList(String type) {
    return "contained".equals(type)?CONTAINED_IN_RELATIONS:
      "part_of".equals(type)?PART_OF_RELATIONS:
        "derives_from".equals(type)?DERIVES_FROM_RELATIONS:
          null;
  }
  public static List<String> getPropertyList(String type) {
    return "gene".equals(type)?GENE_PROPERTIES:
      "mrna".equals(type)?MRNA_PROPERTIES:
        null;
  }
  public List<Integer> getRelations(String type) {
    List<String> rStrings = getRelationList(type);
    List<Integer> rIntegers = new ArrayList<Integer>();
    for( String r : rStrings) {
      if(cvTermMap.get("relationship").containsKey(r)) {
        rIntegers.add(cvTermMap.get("relationship").get(r));
      }
    }
    return rIntegers;
  }
}
