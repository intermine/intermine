package org.flymine.biojava1.utils;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ObjectStoreManager manages ObjectStore instances.
 * 
 * @author Markus Brosch
 */
public final class ObjectStoreManager {

  // =======================================================================
  // Attributes
  // =======================================================================
  
  /**
   * map stores ObjectStoreManager instances <br>
   */
  private static Map _instance = new HashMap();

  /**
   * Objectstore of current ObjectStoreManager instance
   */
  private static ObjectStore _activeObjectStore;

  // =======================================================================
  // Constructor (private)
  // =======================================================================

  /**
   * private constructor <br>
   * sets the activeObjectStore with the default objectStoreName
   */
  private ObjectStoreManager() {
    try {
      _activeObjectStore = ObjectStoreFactory.getObjectStore(Config.OS_DEFAULT);
    } catch (Exception e) {
      throw new RuntimeException("getObjectStore(\"" + Config.OS_DEFAULT + "\") failed", e);
    }
    assert (_activeObjectStore != null);
  }

  /**
   * private constructor <br>
   * sets the activeObjectStore with the passed in ObjectStore
   * 
   * @param pObjectStore
   *          set the active ObjectStore
   */
  private ObjectStoreManager(final ObjectStore pObjectStore) {
    _activeObjectStore = pObjectStore;
  }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * @return instance of ObjectStoreManager
   */
  public static ObjectStoreManager getInstance() {
    return setUp(Config.OS_DEFAULT);
  }

  /**
   * get ObjectStoreManager by name
   * 
   * @param pNewObjectStoreName
   *          name of ObjectStoreManager
   * @return the instance of ObjectStoreManager with name newObjectStoreName
   */
  public static ObjectStoreManager getInstance(final String pNewObjectStoreName) {
    return setUp(pNewObjectStoreName);
  }

  /**
   * set a name for the ObjectStoreManager and pass in the activeObjectStore for this manager
   * 
   * @param pNewObjectStoreName
   *          the name of ObjectStoreManager
   * @param pOS
   *          the activeObjectStore of ObjectStoreManager
   * @return the ObjectStoreManager
   */
  public static ObjectStoreManager setOsAndGetOsManager(final String pNewObjectStoreName,
      final ObjectStore pOS) {
    final ObjectStoreManager oss = new ObjectStoreManager(pOS);
    _instance.put(pNewObjectStoreName, oss);
    return oss;
  }

  /**
   * get the active ObjectStore of a ObjectStoreManager
   * 
   * @return the active ObjectStoreManager of this instance
   */
  public ObjectStore getObjectStore() {
    return _activeObjectStore;
  }

  // -----------------------------------------------------------------------
  // private helper
  // -----------------------------------------------------------------------

  /**
   * gets an ObjectStoreManager instance if available <br>
   * If not, it creates an ObjectStoreManager with pNewObjectStoreName and returns
   * 
   * @param pNewObjectStoreName
   *          name of OSM
   * @return the ObjectStoreManager
   */
  private static ObjectStoreManager setUp(final String pNewObjectStoreName) {
    if (_instance.get(pNewObjectStoreName) != null) {
      return (ObjectStoreManager) _instance.get(pNewObjectStoreName);
    } else {
      final ObjectStoreManager oss = new ObjectStoreManager();
      _instance.put(pNewObjectStoreName, oss);
      return oss;
    }
  }

}