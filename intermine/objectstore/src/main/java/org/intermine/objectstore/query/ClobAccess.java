package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.objectstore.query.Clob.CLOB_PAGE_SIZE;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.proxy.Lazy;

/**
 * Class used to read a Clob from the ObjectStore.
 *
 * @author Matthew Wakeling
 */
public class ClobAccess implements CharSequence, Lazy
{
    protected ObjectStore os;
    protected SingletonResults results;
    protected Clob clob;
    protected int offset;
    protected int length;
    protected boolean subSequence;

    /**
     * Protected constructor for use by PendingClob only.
     */
    protected ClobAccess() {
    }

    /**
     * Construct a ClobAccess object from an ObjectStore and a Clob. This method will detect the
     * size of the Clob.
     *
     * @param os the ObjectStore that the Clob is stored in
     * @param clob the Clob to access
     */
    public ClobAccess(ObjectStore os, Clob clob) {
        this.os = os;
        this.clob = clob;
        offset = 0;
        subSequence = false;
    }

    /**
     * Construct a ClobAccess object representing a subsequence of an existing ClobAccess object.
     *
     * @param results the SingletonResults object backing the object
     * @param clob the Clob to access
     * @param offset the offset
     * @param length the length
     */
    private ClobAccess(SingletonResults results, Clob clob, int offset, int length) {
        this.results = results;
        this.clob = clob;
        this.offset = offset;
        this.length = length;
        os = results.getObjectStore();
        subSequence = true;
    }

    /**
     * Initialises the state of this object. This is done lazily, because it requires the use of a
     * database connection to discover the length of the clob, and that cannot be done while inside
     * the ObjectStoreWriter while it has exclusive use of the connection.
     */
    protected void init() {
        if (results == null) {
            Query q = new Query();
            q.addToSelect(clob);
            results = os.executeSingleton(q, 20, false, false, true);
            int pageCount = results.size();
            if (pageCount == 0) {
                length = 0;
            } else {
                String lastPage = (String) results.get(pageCount - 1);
                length = CLOB_PAGE_SIZE * (pageCount - 1) + lastPage.length();
            }
        }
    }

    /**
     * Return the Clob that this object is accessing.
     *
     * @return a Clob object
     */
    public Clob getClob() {
        return clob;
    }

    /**
     * Return the offset into the underlying clob that this object is using.
     *
     * @return an int
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Return a character from the specified index.
     *
     * @param index the position from which to return a character
     * @return a character
     * @throws IndexOutOfBoundsException if the index argument is negative or not less than length()
     */
    @Override
    public char charAt(int index) {
        init();
        if (index < 0) {
            throw new IndexOutOfBoundsException("index is less than zero");
        }
        if (index >= length) {
            throw new IndexOutOfBoundsException("index is not less than length");
        }
        int page = index / CLOB_PAGE_SIZE;
        String pageText = (String) results.get(page);
        return pageText.charAt(index - page * CLOB_PAGE_SIZE);
    }

    /**
     * Returns the length of this character sequence.
     *
     * @return the number of chars in this sequence
     */
    @Override
    public int length() {
        init();
        return length;
    }

    /**
     * Returns a new CharSequence that is a subsequence of this sequence, from start (inclusive) to
     * end (exclusive).
     *
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return the specified sequence
     * @throws IndexOutOfBoundsException if the start or end are negative, if end is greater than
     * length(), or if start is greater than end
     */
    @Override
    public ClobAccess subSequence(int start, int end) {
        init();
        if (start < 0) {
            throw new IndexOutOfBoundsException("start is less than zero");
        }
        if (end > length) {
            throw new IndexOutOfBoundsException("end is greater than the length of this Clob");
        }
        if (end < start) {
            throw new IndexOutOfBoundsException("end is less than start");
        }
        if ((start == 0) && (end == length)) {
            return this;
        }
        return new ClobAccess(results, clob, start + offset, end - start);
    }

    /**
     * Returns the ObjectStore that this object will use to access data.
     *
     * @return an ObjectStore
     */
    @Override
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * Converts the Clob into a String. Be careful that it can fit in memory!
     *
     * @return a String
     */
    @Override
    public String toString() {
        init();
        StringBuilder retval = new StringBuilder();

        if (length > 0) {
            int lowestPage = offset / CLOB_PAGE_SIZE;
            int highestPage = (offset + length - 1) / CLOB_PAGE_SIZE;
            for (int page = lowestPage; page <= highestPage; page++) {
                String pageText = (String) results.get(page);
                if (page == highestPage) {
                    pageText = pageText.substring(0, offset + length - page * CLOB_PAGE_SIZE);
                }
                if (page == lowestPage) {
                    pageText = pageText.substring(offset - page
                            * CLOB_PAGE_SIZE, pageText.length());
                }
                retval.append(pageText);
            }
        }
        return retval.toString();
    }

    /**
     * Sends the entire contents of the Clob into the given PrintStream. This method should be used
     * in preference to toString() in order to save memory with large Clobs.
     *
     * @param out a PrintStream to write the Clob value to
     */
    public void drainToPrintStream(PrintStream out) {
        init();
        int lowestPage = offset / CLOB_PAGE_SIZE;
        int highestPage = (offset + length - 1) / CLOB_PAGE_SIZE;
        for (int page = lowestPage; page <= highestPage; page++) {
            String pageText = (String) results.get(page);
            if (page == highestPage) {
                pageText = pageText.substring(0, offset + length - page * CLOB_PAGE_SIZE);
            }
            if (page == lowestPage) {
                pageText = pageText.substring(offset - page * CLOB_PAGE_SIZE, pageText.length());
            }
            out.print(pageText);
        }
    }

    /**
     * Returns a String that describes this object sufficiently for it to be recreated given an
     * ObjectStore. This format is stored in the database.
     *
     * @return a String
     */
    public String getDbDescription() {
        if (subSequence) {
            return clob.getClobId() + "," + offset + "," + length();
        } else {
            return clob.getClobId() + "";
        }
    }

    /**
     * Returns the ObjectStore that this object uses.
     *
     * @return an ObjectStore
     */
    public ObjectStore getOs() {
        return os;
    }

    /**
     * Returns the (possibly uninitialised) length internal parameter in this object, for use by
     * subclass constructors.
     *
     * @return an int
     */
    public int getLengthWithoutInit() {
        return length;
    }

    /**
     * Returns the (possibly uninitialised) results object used internally by this object, for use
     * by subclass constructors.
     *
     * @return a SingletonResults object
     */
    public SingletonResults getResultsWithoutInit() {
        return results;
    }

    /**
     * Returns true if this object is a subsequence of a main Clob.
     *
     * @return a boolean
     */
    public boolean getSubSequence() {
        return subSequence;
    }

    private static ThreadLocal<Map<String, ClobAccessSubclassFactory>> subclassFactoryCache =
        new ThreadLocal<Map<String, ClobAccessSubclassFactory>>() {
            @Override protected Map<String, ClobAccessSubclassFactory> initialValue() {
                return new HashMap<String, ClobAccessSubclassFactory>();
            }
        };

    /**
     * Decodes a String from the database representing a clob object. See getDbDescription().
     *
     * @param os an ObjectStore that the clob is stored in
     * @param description the description from the database
     * @return a ClobAccess object, or a subclass
     */
    public static ClobAccess decodeDbDescription(ObjectStore os, String description) {
        String[] parts = description.split(",");
        ClobAccess clob = new ClobAccess(os, new Clob(Integer.parseInt(parts[0])));
        if (parts.length >= 3) {
            int offset = Integer.parseInt(parts[1]);
            int length = Integer.parseInt(parts[2]);
            clob = clob.subSequence(offset, offset + length);
        }
        String className = null;
        if (parts.length == 2) {
            className = parts[1];
        } else if (parts.length == 4) {
            className = parts[3];
        }
        if (className != null) {
            // So, className is a class that extends ClobAccess. We expect there to be a static
            // method called getFactory() that returns a ClobAccessSubclassFactory object that we
            // can store in a cache, which will call the constructor quickly.
            Map<String, ClobAccessSubclassFactory> factoryCache = subclassFactoryCache.get();
            ClobAccessSubclassFactory factory = factoryCache.get(className);
            if (factory == null) {
                try {
                    Class<?> subclass = Class.forName(className);
                    Method subMethod = subclass.getMethod("getFactory");
                    factory = (ClobAccessSubclassFactory) subMethod.invoke(null);
                    factoryCache.put(className, factory);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not read Clob subclass " + className
                            + " from database.", e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Could not read Clob subclass " + className
                            + " from database.", e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Could not read Clob subclass " + className
                            + " from database.", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not read Clob subclass " + className
                            + " from database.", e);
                }
            }
            clob = factory.invokeConstructor(clob);
        }
        return clob;
    }

    /**
     * Class to provide fast access to the constructor of plugin subclasses of ClobAccess.
     *
     * @author Matthew Wakeling
     */
    public abstract static class ClobAccessSubclassFactory
    {
        /**
         * Method to return a new instance of the subclass of ClobAccess that this object
         * represents.
         *
         * @param clobAccess the existing ClobAccess object to pass in to the constructor
         * @return a ClobAccess object
         */
        public abstract ClobAccess invokeConstructor(ClobAccess clobAccess);
    }
}
