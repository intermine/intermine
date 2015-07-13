package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.PropertiesUtil;
/**
 * An util class to help looking up genome build by a given organism id.
 *
 * @author Fengyuan Hu
 *
 */
public final class OrganismGenomeBuildLookup
{
  private static Map<Integer, String> taxonMap = new HashMap<Integer, String>();
  private static Map<String, String> abbreviationMap = new HashMap<String, String>();
  private static Map<String, String> fullnameMap = new HashMap<String, String>();
  private static Map<String, String> proteomeMap = new HashMap<String, String>();


  // TODO how genome build can be integrated to database rather than written in a file?

  private OrganismGenomeBuildLookup() {
    //disable external instantiation
  }

  private static void prepareData() {
    // this is vestigial. The old method relied on hard-coded values. Transition to method
    // which uses objectstore
    if (taxonMap.size() == 0 || abbreviationMap.size() == 0 || fullnameMap.size() == 0) {

    }
  }

  private static void prepareData(ObjectStore objectStore) {
    if (taxonMap.size() == 0 || abbreviationMap.size() == 0 || fullnameMap.size() == 0) {
      Query q = new Query();
      QueryClass qcOrg = new QueryClass(Organism.class);
      q.addFrom(qcOrg);
      q.addToSelect(qcOrg);
      Results results = objectStore.execute(q);
      Iterator<?> iter = results.iterator();
      while( iter.hasNext()) {
        ResultsRow<?> row = (ResultsRow<?>) iter.next();
        Organism org = (Organism) row.get(0);
        taxonMap.put(org.getTaxonId(),org.getAnnotationVersion());
        abbreviationMap.put(org.getShortName(),org.getAnnotationVersion());
        fullnameMap.put(org.getName(),org.getAnnotationVersion());
      }
    }
  }
  /**
   * Get genome build by organism full name such as "Drosophila melanogaster"
   * @param fn full name of an organism
   * @param objectStore the database ObjectStore
   * @return genome build
   */
  public static String getGenomeBuildbyOrganismFullName(String fn,ObjectStore objectStore) {
    prepareData(objectStore);
    return getGenomeBuildbyOrganismFullName(fn);
  }
  public static String getGenomeBuildbyOrganismFullName(String fn) {
    prepareData();
    return fullnameMap.get(fn);
  }

  /**
   * Get genome build by organism short name such as "D. melanogaster"
   * @param abbr short name of an organism
   * @param objectStore the database ObjectStore
   * @return genome build
   */
  public static String getGenomeBuildbyOrganismAbbreviation(String abbr, ObjectStore objectStore) {
    prepareData(objectStore);
    return getGenomeBuildbyOrganismAbbreviation(abbr);
  }
  public static String getGenomeBuildbyOrganismAbbreviation(String abbr) {
    prepareData();
    return abbreviationMap.get(abbr);
  }

  /**
   * Get genome build by organism taxon such as 7227
   * @param taxon taxon of an organism
   * @param objectStore the database ObjectStore
   * @return genome build
   */
  public static String getGenomeBuildbyOrganismTaxon(Integer taxon,ObjectStore objectStore) {
    prepareData(objectStore);
    return getGenomeBuildbyOrganismTaxon(taxon);
  }
  public static String getGenomeBuildbyOrganismTaxon(Integer taxon) {
    prepareData();
    return taxonMap.get(taxon);
  }


  /**
   * Get genome build by organism proteome id such as 300
   * @param proteome id of an annotation
   * @param objectStore the database ObjectStore
   * @return genome build
   */
  public static String getGenomeBuildbyOrganismProteomeId(Integer proteomeId,ObjectStore objectStore) {
    prepareData(objectStore);
    return getGenomeBuildbyOrganismProteomeId(proteomeId);
  }
  public static String getGenomeBuildbyOrganismProteomeId(Integer taxon) {
    prepareData();
    return proteomeMap.get(taxon);
  }

  /**
   * Get genome build by any id
   * @param id id of an organism
   * @param objectStore the database ObjectStore
   * @return genome build
   */
  public static String getGenomeBuildbyOrganismId(String id,ObjectStore objectStore) {
    prepareData(objectStore);
    return getGenomeBuildbyOrganismId(id);
  }
  public static String getGenomeBuildbyOrganismId(String id) {
    prepareData();

    if (id.contains(". ")) {
      return abbreviationMap.get(id);
    } else if (Pattern.matches("^\\d+$", id)) {
      return taxonMap.get(id);
    } else {
      return fullnameMap.get(id);
    }
  }

  /**
   * Get genome build by a collection of ids
   * @param c a collection of organism ids
   * @param objectStore the database ObjectStore
   * @return a collection genome builds
   */
  public static Collection<String> getGenomeBuildByOrganismCollection(Collection<String> c,ObjectStore objectStore) {
    prepareData(objectStore);
    return getGenomeBuildByOrganismCollection(c);
  }
  public static Collection<String> getGenomeBuildByOrganismCollection(Collection<String> c) {
    prepareData();

    Collection<String> gbc = new LinkedHashSet<String>();

    for (String id : c) {

      if (id.contains(". ")) {
        gbc.add(abbreviationMap.get(id));
      } else if (Pattern.matches("^\\d+$", id)) {
        gbc.add(taxonMap.get(id));
      } else {
        gbc.add(fullnameMap.get(id));
      }
    }

    return gbc;
  }
}
