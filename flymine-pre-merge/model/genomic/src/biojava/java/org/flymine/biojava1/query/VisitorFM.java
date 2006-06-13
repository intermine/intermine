package org.flymine.biojava1.query;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.symbol.Location;
import org.biojava.utils.walker.Visitor;
import org.flymine.biojava1.exceptions.ModelException;
import org.intermine.objectstore.query.Query;

/**
 * This class implements the Visitor Interface to allow BioJava to walk through the FeatureFilter
 * tree. With this implementation BioJava can simply use this Visitor and examine each node. For
 * each node a FlyMine Query is generated. After walking through the tree the resulting Query
 * corresponds to your FeatureFilter you passed in and you can query FlyMine directly.
 * <p>
 * Use this class together with a Walker from WalkerFactory and use walk method to parse and
 * traverse through the Tree. After walking you can simply ask the walker by getValue() for the
 * resulting Query: <code>
 * final VisitorFM visitor = new VisitorFM(QueryFMinstance);
 * final WalkerFactory wf = WalkerFactory.getInstance();
 * final Walker walker = wf.getWalker(visitor);
 * walker.walk(pFilter, visitor);
 * Query query = (Query) walker.getValue();
 * </code>
 * <p>
 * NOTE: Use FeatureFilter.Or sparingly. It is extremely inefficient. Sometimes it is even better to
 * filter A and then B and use union of FeatureHolderUtils (external join). The problem actually is,
 * that the FlyMine Query relies on the Postgresql optimizer to optimize subqueries (like or).
 * </p>
 * If a filter is not supported and you use such a filter in your filter tree, it results in null.<br>
 * Supported filters are:
 * ok FeatureFilter.And <br>
 * ok FeatureFilter.ByClass <br>
 * ok FeatureFilter.ByFeature <br>
 * ok FeatureFilter.ByType <br>
 * ok FeatureFilter.ContainedByLocation <br>
 * ok FeatureFilter.Not <br>
 * ok FeatureFilter.Or <br>
 * ok FeatureFilter.OverlapsLocation <br>
 * ok FeatureFilter.ShadowContainedByLocation <br>
 * ok FeatureFilter.ShadowOverlapsLocation <br>
 * ok FeatureFilter.StrandFilter
 * 
 * @author Markus Brosch
 */
public class VisitorFM implements Visitor {

  /**
   * the underlying QueryFM (map FeatureFilters -\> queries of a QueryFM)
   */
  private final IQueryFM _queryFM; //required

  /**
   * Constructor
   * @param pQueryFM the underlying QueryFM
   */
  public VisitorFM(final IQueryFM pQueryFM) {
    if (pQueryFM == null) { throw new NullPointerException("queryFM must not be null"); }
    _queryFM = pQueryFM;
  }

  /**
   * if a pFilter passed in which is not handled by any of the other filter handlers here, it is not
   * supported
   * @param pFilter a FeatureFilter
   * @return nothing
   * @throws UnsupportedOperationException for FeatureFilters not supported
   */
  public Query featureFilter(final FeatureFilter pFilter) {
    throw new UnsupportedOperationException("This filter has a filter type which is not supported!");
  }

  public Query byClass(final FeatureFilter.ByClass pFilter) throws ModelException {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    return _queryFM.byClass(pFilter.getTestClass());
  }

  public Query byFeature(final FeatureFilter.ByFeature pFilter) {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    return _queryFM.byFeature(pFilter.getFeature());
  }

  public Query byType(final FeatureFilter.ByType pFilter) throws ModelException {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    String type = pFilter.getType();
    return _queryFM.byType(type);
  }

  public Query containedByLocation(final FeatureFilter.ContainedByLocation pFilter) {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    final Location loc = pFilter.getLocation();
    return _queryFM.containedByLocation(new Integer(loc.getMin()), new Integer(loc.getMax()));
  }

  public Query overlapsLocation(final FeatureFilter.OverlapsLocation pFilter) {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    final Location loc = pFilter.getLocation();
    return _queryFM.overlapsLocation(new Integer(loc.getMin()), new Integer(loc.getMax()));
  }

  public Query strandFilter(final FeatureFilter.StrandFilter pFilter) {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    final StrandedFeature.Strand strand = pFilter.getStrand();
    return _queryFM.strandFilter(new Integer(strand.getValue()));
  }

  public Query shadowContainedByLocation(final FeatureFilter.ShadowContainedByLocation pFilter) {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    final Location loc = pFilter.getLocation();
    return _queryFM.shadowContainedByLocation(new Integer(loc.getMin()), new Integer(loc.getMax()));
  }

  public Query shadowOverlapsLocation(final FeatureFilter.ShadowOverlapsLocation pFilter) {
    if (pFilter == null) throw new NullPointerException("pFilter must not be null");
    final Location loc = pFilter.getLocation();
    return _queryFM.shadowOverlapsLocation(new Integer(loc.getMin()), new Integer(loc.getMax()));
  }

  public Query and(FeatureFilter.And pFilter, Query pChild1, Query pChild2) {
    return _queryFM.and(pChild1, pChild2);
  }

  public Query or(FeatureFilter.Or pFilter, Query pChild1, Query pChild2) {
    return _queryFM.or(pChild1, pChild2);
  }

  public Query not(FeatureFilter.Not pFilter, Query pChild1) {
    return _queryFM.not(pChild1);
  }

}

