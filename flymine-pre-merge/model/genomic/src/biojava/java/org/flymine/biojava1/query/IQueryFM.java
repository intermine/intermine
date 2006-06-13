package org.flymine.biojava1.query;

import org.biojava.bio.seq.Feature;
import org.flymine.biojava1.exceptions.ModelException;
import org.intermine.objectstore.query.Query;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * VisitorFM translates dynamically FeatureFilter hierarchies to FlyMine queries. The underlying
 * query implementation should be exchangeable, therefore use this interface instead of concrete
 * class in VisitorFM.
 * 
 * @author Markus Brosch
 */
public interface IQueryFM {

  /**
   * Logical AND operator of two queries.
   * 
   * @param pQuery1
   *        first query
   * @param pQuery2
   *        second query
   * @return Query statement which combines the two child queries by logical AND.
   */
  public abstract Query and(final Query pQuery1, final Query pQuery2);

  /**
   * return Query for all BioEntities of the given clazz type.
   * 
   * @param pClazz
   *        Class for which you want to search (must be a BioEntity class)
   * @return a new Query which queries for all BioEntities of the given Class clazz.
   * @throws ModelException
   *         if clazz is not a BioEntity
   */
  public abstract Query byClass(final Class pClazz) throws ModelException;

  /**
   * return query for the specified feature
   * 
   * @param pFeature
   *        the given feature
   * @return a new Query which queries for the Feature which was given with pFeature
   */
  public abstract Query byFeature(final Feature pFeature);

  /**
   * returns Query for all BioEntities of the given type.
   * 
   * @param pType
   *        Classname (fully qualified or unqualified) to look for. Must be a BioEntity.
   * @return a Query which queries for all BioEntities of the given type/class.
   * @throws ModelException
   *         if pType is not a BioEntity type
   */
  public abstract Query byType(final String pType) throws ModelException;

  /**
   * returns Query which queries for all BioEntities contained within a location range.
   * 
   * @param pStart
   *        start position
   * @param pEnd
   *        end position
   * @return returns all BioEntities of given range.
   */
  public abstract Query containedByLocation(final Integer pStart, final Integer pEnd);

  /**
   * returns a Query which queries for all BioEntities on a specific strand.
   * 
   * @param pStrand
   *        -1 negative, 0 unknown, 1 positive
   * @return return all BioEntities of specified strand
   */
  public abstract Query strandFilter(final Integer pStrand);

  /**
   * Get all BioEntities and NOT elements fulfil query
   * 
   * @param pQuery
   *        a query
   * @return returns all BioEntities which are not in q1.
   */
  public abstract Query not(final Query pQuery);

  /**
   * Logical OR operator for two given Queries. If possible, do not use with two byType or byClass
   * queries or you have to wait for the result quite a while. <b>Use careful and sparingly </b>
   * 
   * @param pQuery1
   *        query1
   * @param pQuery2
   *        query2
   * @return Query statement which combines the two child queries by logical OR.
   */
  public abstract Query or(final Query pQuery1, final Query pQuery2);

  /**
   * returns Query which queries for all BioEntities overlapping a location range.
   * 
   * @param pStart
   *        start position
   * @param pEnd
   *        end position
   * @return returns all BioEntities of given overlapped range.
   */
  public abstract Query overlapsLocation(final Integer pStart, final Integer pEnd);

  /**
   * Same functionality as containedByLocation (we have only contiguous features on a chromosome)
   * 
   * @see QueryFM#containedByLocation(Integer, Integer)
   */
  public abstract Query shadowContainedByLocation(final Integer pStart, final Integer pEnd);

  /**
   * Same functionality as overlapsLocation (we have only contiguous features on a chromosome)
   * 
   * @see QueryFM#overlapsLocation(Integer, Integer)
   */
  public abstract Query shadowOverlapsLocation(final Integer pStart, final Integer pEnd);
}