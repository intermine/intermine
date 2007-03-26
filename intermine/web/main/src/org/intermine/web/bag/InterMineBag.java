package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.results.ResultElement;

/**
 * A Set that retains information about the order and number of objects (and Lists) that are added.
 * eg. if 1, 2, 3 and the List (2, 3, 4, 5) are added, the iterator() method will return
 * (1,2,3,4,5) but asListOfLists() will return ((1), (2), (3), (2,3,4,5)).
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */

public class InterMineBag extends AbstractSet
{
    protected static final Logger LOG = Logger.getLogger(InterMineBag.class);

    /** User id, used for storing and retrieving. */
    private Integer userId;
    /** Bag name, used for storing and retrieving. */
    private String name;
    /** Bag size, used to provide this info without having to fetch the entire bag contents. */
    private int size;
    /** User profile ObjectStore used for storing and retrieving BagElemts */
    protected ObjectStore uos;
    /** ObjectStore used for storing and retrieving InterMine Objects*/
    protected ObjectStore os;
    /** SoftReference to a materialised version of the bag. If this holds a reference to a valid
     * List, then the List   is cached data that does not need to be stored, and list will
     * be null. */
    private SoftReference listReference;
    /**
     * Strong reference to a materialised version of the bag. If this holds a List, then
     * the List is data that needs to be stored, and listReference will be null.
     */
    private List list;
    private Set set;
    private Integer id;

    private String type;
    private Map idFieldMap = new HashMap();
    private String description;
    
    /**
     * Constructs a new InterMineIdBag to be lazily-loaded from the userprofile database.
     *
     * @param userId the id of the user, matching the userprofile database
     * @param name the name of the bag, matching the userprofile database
     * @param type the class of objects stored in the bag
     * @param size the size of the bag
     * @param os the ObjectStore to use to retrieve the contents of the bag
     * @param uos the UserProfile ObjectStore
     */
    public InterMineBag(Integer userId, String name, String type, int size, ObjectStore uos,
                        ObjectStore os) {
        listReference = null;
        list = null;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.size = size;
        this.uos = uos;
        this.os = os;
   }

    /**
     * Constructs a new InterMineIdBag with certain contents.
     *
     * @param userId the id of the user, to be saved in the userprofile database
     * @param name the name of the bag, to be saved in the userprofile database
     * @param type the class of objects stored in the bag
     * @param os the ObjectStore to use to store the contents of the bag
     * @param uos the UserProfile ObjectStore
     * @param c the new bag contents
     */
    public InterMineBag(Integer userId, String name, String type, ObjectStore uos, ObjectStore os,
                        Collection c) {
        listReference = null;
        list = new ArrayList(c);
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.size = -1;
        this.uos = uos;
        this.os = os;
    }
    
    private Set asSet() {
        if (set == null) {
            set = new LinkedHashSet() {
                public Iterator iterator() {
                    final Iterator setIter = super.iterator();
                    return new Iterator() {
                        public boolean hasNext() {
                            return setIter.hasNext();
                        }
                        public Object next() {
                            return setIter.next();
                        }
                        public void remove() {
                            throw new RuntimeException("cannot modify InterMineIdBag " 
                                                       + "using iterator");
                        }
                    };
                }
            };
            
            Iterator elementsIter = getRealList().iterator();

            while (elementsIter.hasNext()) {
                Object thisObject = elementsIter.next();
                
                if (thisObject instanceof Collection) {
                    Collection subList = (Collection) thisObject;
                    Iterator subListIter = subList.iterator();
                    
                    while (subListIter.hasNext()) {
                        set.add(subListIter.next());
                    }
                } else {
                    set.add(thisObject);
                }
            }
            
        }
        
        return set;
    }

    /**
     * Return the width of this Bag - ie. the maximum size of any InterMineIdList object in this
     * bag.
     * @return the bag width
     */
    public int width() {
        int width = 1;
        
        Iterator iter = getRealList().iterator();
        
        while (iter.hasNext()) {
            Object thisObject = iter.next();
            if (thisObject instanceof List) {
                List row = (List) thisObject;
                if (row.size() > width) {
                    width = row.size();
                }
            }
        }
        
        return width;
    }

    /**
     * Return the Objects that have been added to this InterMineIdBag as a list of lists.  Single
     * objects are returned in the List as single element Lists.  Any Lists that were add()ed to
     * this InterMineIdBag are returned as themselves.
     * @return the orginal objects and list that were added
     */
    public List asListOfLists() {
        List retList = new ArrayList();
        
        Iterator iter = getRealList().iterator();
        
        while (iter.hasNext()) {
            Object thisObject = iter.next();
            if (thisObject instanceof List) {
                retList.add(thisObject);
            } else {
                retList.add(Collections.singletonList(thisObject));
            }
        }
        
        return retList;
    }

    /**
     * @see AbstractSet#size()
     */
    public synchronized int size() {
        if (list != null) {
            return asSet().size();
        } else {
            return size;
        }
    }
    
    /**
     * @see AbstractSet#size()
     * @return the size of this bag
     */
    public int getSize() {
        return size();
    }
    
    private synchronized List getRealList() {
        // TODO Return Id's out of ResultElement
        if (list != null) {
            return list;
        }
        if (listReference != null) {
            List retval = (List) listReference.get();
            if (retval != null) {
                return retval;
            }
        }
        if (size == 0) {
            return new ArrayList();
        }
        try {
            Query q = new Query();
            QueryClass qc = new QueryClass(SavedBag.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"),
                        ConstraintOp.EQUALS, new QueryValue(name)));
            cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "userProfile"),
                        ConstraintOp.CONTAINS, new ProxyReference(null, userId,
                                                                  UserProfile.class)));
            q.setConstraint(cs);
            Results res = uos.execute(q);
            SavedBag savedBag = (SavedBag) ((List) res.get(0)).get(0);
            setSavedBagId(savedBag.getId());
            
            String bagText = (String) savedBag.getBag();
            Map unmarshalled = InterMineBagBinding.unmarshal(new StringReader(bagText), uos, os,
                    IdUpgrader.ERROR_UPGRADER, userId);
            InterMineBag un = (InterMineBag) unmarshalled.get(name);
            listReference = new SoftReference(un.list);
            return un.list;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized List getRealListForWrite() {
        if (list == null) {
            list = getRealList();
        }
        return list;
    }
    
    /**
     * Get the list of ids of the objects contained in the bag
     * @return a Collection of Ids
     */
    public synchronized Collection getListOfIds() {
        Collection ids = new ArrayList();
        for (Iterator iter = getRealList().iterator(); iter.hasNext();) {
            BagElement element = (BagElement) iter.next();
            ids.add(element.getId());
        }
        return ids;
    }
    
    /**
     * Get the list of InterMineObjects stored in the bag
     * @return a List of InterMineObjects
     * @throws ObjectStoreException an ObjectStoreException
     */
    public synchronized List getInterMineObjects() throws ObjectStoreException {
        Collection ids = getListOfIds();
        return (os.getObjectsByIds(ids));
    }

    /**
     * Return true if and only if this bag needs to be written to the userprofile.
     * @return true iff the bag have been modified since the last save
     */
    public synchronized boolean needsWrite() {
        return list != null;
    }

    /**
     * Arrange for needsWrite() to return false next time it's called.
     */
    public synchronized void resetToDatabase() {
        listReference = new SoftReference(list);
        size = asSet().size();
        list = null;
    }
    
    // TODO write method in the other way...

    /**
     * @see AbstractSet#add
     */
    public boolean add(Object o) {
        set = null;
        return getRealListForWrite().add(o);
    }
    
    /**
     * @see AbstractSet#clear
     */
    public void clear() {
        set = null;
        getRealListForWrite().clear();
    }

    /**
     * @see AbstractSet#contains
     */
    public boolean contains(Object o) {
        return asSet().contains(o);
    }

    /**
     * @see AbstractSet#iterator
     */
    public Iterator iterator() {
        // Need a special iterator here, so we can record when a modification occurs
        return new BagIterator();
    }

    /**
     * @see AbstractSet#remove
     */
    public boolean remove(Object o) {
        set = null;
        return getRealListForWrite().remove(o);
    }
    
    /**
     * Remove a bagElement from its id
     * @param id the id
     */
    public void removeFromId(Integer id) {
        remove(getBagElementFromId(id));
    }
    
    /**
     * Get the BagElement for the given id
     * @param id the id
     * @return a BagElement
     */
    public BagElement getBagElementFromId(Integer id) {
        List list = getRealListForWrite();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            BagElement bagElement = (BagElement) i.next();
            if (bagElement.getId().equals(id)) {
                return bagElement;
            }
        }
        return null;
    }

    /**
     * @see java.util.Set#isEmpty()
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @see java.util.Set#toArray()
     */
    public Object[] toArray() {
       Object[] retArray = new Object[asSet().size()];
       Iterator iter = asSet().iterator();

       int i = 0;
       while (iter.hasNext()) {
           retArray[i++] = iter.next();
       }
       return retArray;
    }

    /**
     * @see java.util.Set#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] arg0) {
        return toArray();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return asSet().equals(o);
    }
    
    private class BagIterator implements Iterator
    {
        private Set iterSet;
        private Iterator iter;

        BagIterator() {
            iterSet = asSet();
            iter = iterSet.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            return iter.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("remove() not available when iterating over "
                                                    + "an InterMineIdBag");
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return asSet().hashCode();
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return asSet().toString();
    }

    /**
     * Returns the value of name
     * @return the name of the bag
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     * @param name the bag name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the value of savedBagId
     * @return an Integer
     */
    public Integer getSavedBagId() {
        return id;
    }

    /**
     * Set the value of savedBagId
     * @param id the saved bag id
     */
    public void setSavedBagId(Integer id) {
        this.id = id;
    }
   
    /**
     * Add a ResultElement.
     * @param resultElement the ResultElement
     * @return a boolean
     */
    public boolean add(ResultElement resultElement) {
        idFieldMap.put(resultElement.getId(), resultElement.getField());
        return super.add(resultElement);
    }

    /**
     * Remove an id.
     * @param id an intermine id
     */
    public void remove(int id) {
        remove(new Integer(id));
    }

    /**
     * Get the type of this bag (a class from InterMine model)
     * @return the type of objects in this bag
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the fully qualifie type of this bag
     * @return the type of objects in this bag
     */
    public String getQualifiedType() {
        return os.getModel().getPackageName() + "." + type;
    }
    
    /**
     * Set the type of this bag (a class from InterMine model)
     * @param type the type of objects in this bag
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Return a collection of InterMineObjects corresponding to the ids in the bag.
     *
     * @return collection of InterMineObjects
     */
    public Collection toObjectCollection() {
        List returnList = new ArrayList();
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            BagElement bagElement = (BagElement) iter.next();
            try {
                returnList.add(os.getObjectById(bagElement.getId()));
            } catch (ObjectStoreException err) {
                LOG.error("Failed to load object by id " + bagElement.getId());
            }
        }
        return returnList;
    }
    
    /**
     * For a given intermine id, return the field
     * saved in the bag
     * @param id the object id
     * @return the field as a generic Object
     */
    public Object getFieldForId(Integer id) {
        return (Object) idFieldMap.get(id);
    }

   /**
    * Set the userId for this bag
    * @param userId the userId from the profile as an Integer
    */
   public void setUserId(Integer userId) {
       this.userId = userId;
   }
}
