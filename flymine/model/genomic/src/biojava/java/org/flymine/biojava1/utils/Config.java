package org.flymine.biojava1.utils;

import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * Utility class to load properties for the biojava mapping. <br>
 * Once one of the public static fields are used, the static block is proecessed before. So you can
 * simply use the public static fields.
 * 
 * @author Markus Brosch
 */
public final class Config {

  // =======================================================================
  // Attributes
  // =======================================================================

  ////// config files //////

  /**
   * filepath to the biojavaMapping.properties file
   */
  private static final String FILEPATHBJ = "resources/biojava1/bjMapping.properties";

  /**
   * filepath to log4j.properties file - defined in bjMapping.properties
   */
  private static final String FILEPATHLOG4J;

  /**
   * filepath to intermine.properties file - defined in bjMapping.properties
   */
  private static final String FILEPATHINTERMINE;

  ////// global config constants //////

  /**
   * states wether log4j logging is on or off. Even if log4j is set to WARN or ERROR, the logging
   * statements are evaluated although. If each logging statement is tested with
   * <code>if(Config.LOG) {log.debug("foo");}</code> the part is ignored by the compiler: LOG is
   * final, therefore it is substituted whereever it is used; if the compiler then sees a statement
   * like <code>if (false) { log.debug("foo"); }</code> it is a unreachable code and is
   * eliminated.
   */
  public static final boolean LOG;

  //// general ////
  
  /**
   * true: memory mode (all data dumped) / false: non memory mode (lazy and dynamic queries)
   */
  public static final boolean ISMEMORYMODE;
  
  /**
   * true: all hasA relations of features are loaded into the annotation bundle
   */
  public static final boolean HASA;
  
  /**
   * true: all inverse hasA relations of features are loaded into the annotation bundle
   */
  public static final boolean INVHASA;
  
  /**
   * true: all FlyMine Synonyms of a Feature are loaded into the annotation bundle
   */
  public static final boolean SYNONYMS;
  
  //// naming ////
  
  /**
   * the Feature source can be configured; e.g. FlyMine
   */
  public static final String SOURCE;
  
  //// model ////
  
  /**
   * states the default flymine genomic model, e.g. "genomic"
   */
  public static final String FLYMINE_GENOMICMODELNAME;

  /**
   * the namespace of SOFA model used by the biojava mapping
   */
  public static final String SOFA_NAMESPACE; 
  
  /**
   * the package prefix of the SOFA model objects (e.g. a Gene results in
   * org.flymine.model.genomic.Gene) - this is a trick, so I can instantiate a SoFa gene and 
   * use it in FlyMine as a "normal" gene.
   */
  public static final String SOFA_PACKAGE = "org.flymine.model.genomic";

  /**
   * the SOFA ontology file
   */
  public static final String SOFA_FILEPATH;

  /**
   * default ObjectStore default (os.default)
   */
  public static final String OS_DEFAULT;

  // =======================================================================
  // Static
  // =======================================================================

  static {


    //load bioJava mapping properties
    try {

      final Properties properties = new Properties();
      properties.load(new FileInputStream(FILEPATHBJ));
      
      //logging on/off
      final String log = properties.getProperty("logging");
      if (log.equals("on")) {
        LOG = true;
      } else if (log.equals("off")) {
        LOG = false;
      } else {
        throw new RuntimeException(FILEPATHBJ + ": logging is not specified correctly");
      }
      
      //memory
      final String memory = properties.getProperty("memoryMode");
      if (memory.equals("true")) {
        ISMEMORYMODE = true;
      } else if (memory.equals("false")) {
        ISMEMORYMODE = false;
      } else {
        throw new RuntimeException(FILEPATHBJ + ": memoryMode is not specified correctly");
      }      
      
      //hasA
      final String hasA = properties.getProperty("hasA");
      if (hasA.equals("true")) {
        HASA = true;
      } else if (hasA.equals("false")) {
        HASA = false;
      } else {
        throw new RuntimeException(FILEPATHBJ + ": hasA is not specified correctly");
      } 
      
      //inverse hasA
      final String invHasA = properties.getProperty("invHasA");
      if (invHasA.equals("true")) {
        INVHASA = true;
      } else if (invHasA.equals("false")) {
        INVHASA = false;
      } else {
        throw new RuntimeException(FILEPATHBJ + ": invHasA is not specified correctly");
      }
      
      //synonyms
      final String synonyms = properties.getProperty("synonyms");
      if (synonyms.equals("true")) {
        SYNONYMS = true;
      } else if (synonyms.equals("false")) {
        SYNONYMS = false;
      } else {
        throw new RuntimeException(FILEPATHBJ + ": invHasA is not specified correctly");
      }

      //files
      SOFA_FILEPATH = properties.getProperty("sofa.ontology.path");

      if (SOFA_FILEPATH == null) {
        throw new RuntimeException(FILEPATHBJ + ": sofa.ontology.path not found");
      }

      FILEPATHLOG4J = properties.getProperty("log4j.properties.path");
      if (FILEPATHLOG4J == null) {
        throw new RuntimeException(FILEPATHBJ + ": log4j.properties.path not found");
      }

      FILEPATHINTERMINE = properties.getProperty("intermine.properties.path");
      if (FILEPATHINTERMINE == null) {
        throw new RuntimeException(FILEPATHBJ + ": intermine.properties.path not found");
      }

      //source
      SOURCE = properties.getProperty("source");
      if (SOURCE == null) { throw new RuntimeException(FILEPATHBJ
          + ": source not found"); }
      
      //flymine default genomic model
      FLYMINE_GENOMICMODELNAME = properties.getProperty("flymine_gmodel");
      if (FLYMINE_GENOMICMODELNAME == null) { throw new RuntimeException(FILEPATHBJ
          + ": flymine_gmodel not found"); }

      //sofa model properties
      SOFA_NAMESPACE = properties.getProperty("sofa_namespace");
      if (SOFA_NAMESPACE == null) { throw new RuntimeException(FILEPATHBJ
          + ": sofa_namespace not found"); }

    } catch (IOException e) {
      throw new RuntimeException(FILEPATHBJ + " not found", e);
    }
    
    //load intermine.properties
    try {
      final Properties properties = new Properties();
      properties.load(new FileInputStream(FILEPATHINTERMINE));
      OS_DEFAULT = properties.getProperty("os.default");
    } catch (IOException e) {
      throw new RuntimeException(FILEPATHINTERMINE + " not found", e);
    }

    //load log4j properties
    PropertyConfigurator.configure(FILEPATHLOG4J);

  }

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * private Constructor
   */
  private Config() {}

}