package org.flymine.biojava1.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * FeatureFM Registry. Keeps track of allocated Features. If you have to instantiate a bunch of
 * Features, you can find out which are already allocated and which you still have to process.
 * 
 * @author Markus Brosch
 */
public final class FeatureRegistry extends HashSet {

  /**
   * singleton instance
   */
  private static final FeatureRegistry _INSTANCE = new FeatureRegistry();

  /**
   * private constructor
   */
  private FeatureRegistry() { }

  /**
   * register an Integer key
   * 
   * @param pKey
   *        a key
   */
  public static void registerKey(final Integer pKey) {
    _INSTANCE.add(pKey);
  }

  /**
   * check whether a key is already registered
   * 
   * @param pKey
   *        a key
   * @return true if key is already registred
   */
  public static boolean isRegistered(final Integer pKey) {
    return _INSTANCE.contains(pKey);
  }

  /**
   * you want to instantiate a bunch of features; you want to find out which you have to process
   * (not instantiated yet)
   * 
   * @param pIDs
   *        a bunch of featureIDs to check
   * @return all IDs which you still need to process as they aren't instantiated yet.
   */
  public static Set getRestToProcess(final Collection pIDs) {
    Set rest = new HashSet(pIDs);
    rest.removeAll(_INSTANCE);
    return rest;
  }

  /**
   * you want to instantiate a bunch of features; you want to find out which you have already
   * instantiated
   * 
   * @param pIDs
   *        a bunch of featureIDs to check
   * @return all IDs which are already instantiated
   */
  public static Set getAllocated(final Collection pIDs) {
    Set allocated = new HashSet(pIDs);
    allocated.retainAll(_INSTANCE);
    return allocated;
  }
}