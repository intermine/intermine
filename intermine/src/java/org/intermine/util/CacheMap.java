package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * This is a Map implementation designed specifically for people intending to create a cache.
 * The behaviour is exactly as that of a WeakHashMap. The difference is the circumstances under
 * which entries are garbage collected from the map. In a WeakHashMap, entries are garbage collected
 * almost as soon as the garbage collector notices that the only reference to the key is through the
 * WeakHashMap. Since on Linux the garbage collector performs a minor collection several times a
 * second, entries do not live long enough to serve any use as part of a cache.
 *
 * <p>This CacheMap implementation makes use of SoftReferences to hold references to the most
 * recently created or used entries in the Map, preventing them from being garbage collected until
 * the garbage collector really needs some memory to be freed.
 *
 * <p>The holding system is system-wide, so CacheMap objects that have not been used for a while
 * will empty out even if the system is not running out of memory.
 *
 * <p>Like most collection classes, this class is not synchronized.  A
 * synchronized <tt>CacheMap</tt> may be constructed using the
 * <tt>Collections.synchronizedMap</tt> method.
 *
 * <p>The holding system attempts to follow the following characteristics:
 * <ul><li>When the garbage collector needs to free memory, it will free the oldest items before
 * the newest items.
 *     <li>There is some randomness in the cache replacement policies, which can improve
 * performance in a lot of situations.
 *     <li>The number of objects retained for a particular CacheMap is proportional to the rate of
 * insertions, when several CacheMaps compete.
 *     <li>Accessing entries in the cache causes them to be retained for longer.
 *     <li>The previous mechanism can cope equally with single objects being accessed very
 * frequently, and many objects being accessed.
 * </ul>
 *
 * <p>The holding mechanism is split into two portions - holding for created entries and holding for
 * accessed entries. The holding system for created entries is efficient as long as entries are not
 * created many times. If a given key is put into the map many times, the holder will become
 * useless.
 * <p>The holding system for accessed entries can cope with a certain number of concurrent
 * repetatively accessed entries (currently about 15), after which it degenerates. An individual
 * repeating access will be noticed with a probability exponentially based on the distance between
 * repetitions, with about equal odds at a spacing of ten.
 * 
 * @see java.util.WeakHashMap
 * @see java.lang.ref.SoftReference
 * @author Matthew Wakeling
 */
public class CacheMap extends WeakHashMap
{
    /* Desired characteristics:
     * 1. When the garbage collector needs to free memory, it should free the oldest or least used
     *     entries before the newest or most used.
     * 2. There should be some randomness in the holder replacement policy, so that there isn't a
     *     sudden performance drop when the working set becomes larger than the cache.
     * 3. One large cache with lots of activity shouldn't be able to crowd out a small cache with
     *     not much activity - ie. the number of entries that a particular cache can retain should
     *     be proportional to the rate at which entries are created or accessed.
     * 4. Accessing entries should hold them in memory nearly as effectively as creating them.
     *
     * I think, officially 1 and 2 are incompatible. Oh well.
     * 2 and 3 can both be served by having one large array as holder, and randomly replacing
     *     entries.
     * With 4, the problem is the difference between creating entries, which should only happen
     *     once, and accessing the entries, which may happen multiple times. If the holder is an
     *     array, then inserting a single entry multiple times has the effect of reducing the useful
     *     size of the array.
     *
     * I therefore suggest the following holder implementation:
     *     The main holder is effectively a circular array of "x * y" entries, but split up into "x"
     *     separate arrays, each of size "y". Each array is referenced by a SoftReference that
     *     the garbage collector is welcome to clear. If one of the separate arrays is accessed and
     *     found to be cleared by the garbage collector, then the system merely recreates an empty
     *     array of size "y" before accessing it. Entries are put into a position in the overall
     *     circular array according to a base offset added to a random number between 0 and "z - 1",
     *     where "z" is near "y". After each operation, the base offset is incremented. A good
     *     source of random numbers is java.util.Random - its algorithms appear to be geared towards
     *     fast production of numbers. This provides a reasonable tradeoff between 1 and 2.
     *
     *     A separate holding system is provided for the system to hold entries that are accessed
     *     frequently. It consists of a small array (size ~20) in which new entries are placed, and
     *     checked to find duplicates. Entries which fall out of the bottom of this array are added
     *     to another array of size size "w", which is a single array referenced by a
     *     SoftReference. Entries are placed in it completely randomly (though probably also with
     *     an incrementing offset to avoid holes in the PRNG). Duplicate entries may appear in this
     *     last array.
     *     The small array works like this: First, an entry is searched for linearly through the
     *     array from position 0 to the end. If it exists in the first half, then it is swapped with
     *     the entry one towards position 0. If it exists in the second half, then it is moved to
     *     a random position in the first half, and all the entries in between are shifted towards
     *     the end of the array. New entries are inserted randomly between mid-way through the
     *     array and the end. All entries from that position to the end are shifted one towards the
     *     end. The one that falls off the end is put into the big array.
     *     In this way, the most common entries will end up in the first half of the array, and will
     *     not be placed in the large array. Also, new entries have an exponentially distributed
     *     number of chances to be recognised as a multiple access and moved to the first half of
     *     the small array.
     *
     *     NOTE: We shouldn't call .equals() or .hashCode() on any of the key objects put in the
     *     cache, because that may take a long time.
     */

    protected static final int HOLDER_ARRAY_COUNT = 256;
    protected static final int HOLDER_ARRAY_SIZE = 1024;
    protected static final int HOLDER_RANDOMNESS = 2000;
    protected static final int ACCESS_HOLDER_SMALL = 20;
    protected static final int ACCESS_HOLDER_SMALL_MIDDLE = 10;
    protected static final int ACCESS_HOLDER_LARGE = 2048;

    protected static SoftReference holder[] = new SoftReference[HOLDER_ARRAY_COUNT];
    protected static SoftReference small[] = new SoftReference[ACCESS_HOLDER_SMALL];
    protected static SoftReference large;

    protected static int holderOffset = 0;
    protected static int accessOffset = 0;
    protected static final int HOLDER_SIZE = HOLDER_ARRAY_COUNT * HOLDER_ARRAY_SIZE;

    protected static Random createdRand = new Random();
    protected static Random accessedRand = new Random();

    protected Map canonicalisation = Collections.synchronizedMap(new WeakHashMap());

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the given initial
     * capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the <tt>CacheMap</tt>
     * @param  loadFactor      The load factor of the <tt>CacheMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative,
     *         or if the load factor is nonpositive.
     */
    public CacheMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the given initial
     * capacity and the default load factor, which is <tt>0.75</tt>.
     *
     * @param  initialCapacity The initial capacity of the <tt>CacheMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative.
     */
    public CacheMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a new, empty <tt>CacheMap</tt> with the default initial
     * capacity (16) and the default load factor (0.75).
     */
    public CacheMap() {
        super();
    }

    /**
     * Constructs a new <tt>CacheMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>CacheMap</tt> is created with 
     * default load factor, which is <tt>0.75</tt> and an initial capacity
     * sufficient to hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   t the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    public CacheMap(Map t) {
        super(t);
    }

    /**
     * @see java.util.WeakHashMap#get
     */
    public Object get(Object key) {
        Object canonical = null;
        canonical = canonicalisation.get(key);
        if (canonical != null) {
            canonical = ((WeakReference) canonical).get();
        }
        if (canonical == null) {
            canonical = key;
        }
        accessed(canonical);
        return super.get(key);
    }

    /**
     * @see java.util.WeakHashMap#containsKey
     */
    public boolean containsKey(Object key) {
        Object canonical = null;
        canonical = canonicalisation.get(key);
        if (canonical != null) {
            canonical = ((WeakReference) canonical).get();
        }
        if (canonical == null) {
            canonical = key;
        }
        accessed(canonical);
        return super.containsKey(key);
    }

    /**
     * @see java.util.WeakHashMap#put
     */
    public Object put(Object key, Object value) {
        canonicalisation.put(key, new WeakReference(key));
        created(key);
        return super.put(key, value);
    }

    /**
     * This method places the given Object into a holder system, that attempts to keep the most
     * recently created objects in memory, at the discretion of the garbage collector.
     *
     * @param obj the Object to hold
     */
    protected static void created(Object obj) {
        synchronized (createdRand) {
            int positionToUse = (createdRand.nextInt(HOLDER_RANDOMNESS) + holderOffset)
                % HOLDER_SIZE;
            holderOffset++;
            if (holderOffset >= HOLDER_SIZE) {
                holderOffset = 0;
            }
            int arrayNo = positionToUse / HOLDER_ARRAY_SIZE;
            int arrayPosition = positionToUse % HOLDER_ARRAY_SIZE;
            Object array[] = null;
            if (holder[arrayNo] != null) {
                array = (Object []) holder[arrayNo].get();
            }
            if (array == null) {
                array = new Object[HOLDER_ARRAY_SIZE];
                holder[arrayNo] = new SoftReference(array);
            }
            array[arrayPosition] = obj;
        }
    }

    /**
     * This method paces the given Object into a holder system, that attempts to keep the most
     * recently accessed objects in memory, at the discretion of the garbage collector.
     * The difference between this method and created() is that this holder is usually smaller, and
     * it has some limited protection against objects being added multiple times.
     *
     * @param obj the Object to hold
     */
    protected static void accessed(Object obj) {
        synchronized (accessedRand) {
            // First, see if our object is present in the small array, or if there's a cleared
            // position to fill.
            int lastClearPosition = -1;
            int objectEqualPosition = -1;
            for (int i = 0; (i < ACCESS_HOLDER_SMALL) && (objectEqualPosition == -1); i++) {
                if ((small[i] == null) || (small[i].get() == null)) {
                    lastClearPosition = i;
                } else if ((small[i] != null) && (small[i].get() == obj)) {
                    objectEqualPosition = i;
                }
            }
            if (objectEqualPosition != -1) {
                // Here, we need to move the entry around intelligently.
                if ((objectEqualPosition < ACCESS_HOLDER_SMALL_MIDDLE)
                        && (objectEqualPosition > 0)) {
                    // It is already in the first half, so it needs to be moved one point towards
                    // position 0, by swapping it with the one next to it.
                    SoftReference temp = small[objectEqualPosition];
                    small[objectEqualPosition] = small[objectEqualPosition - 1];
                    small[objectEqualPosition - 1] = temp;
                } else if (objectEqualPosition >= ACCESS_HOLDER_SMALL_MIDDLE) {
                    // It is in the second half, so it needs to be moved to a random position in
                    // the first half, and ones in-between shifted.
                    int newPosition = accessedRand.nextInt(ACCESS_HOLDER_SMALL_MIDDLE);
                    SoftReference temp = small[objectEqualPosition];
                    for (int i = objectEqualPosition; i > newPosition; i--) {
                        small[i] = small[i - 1];
                    }
                    small[newPosition] = temp;
                }
            } else if (lastClearPosition != -1) {
                // There's an empty slot, so fill it, from the bottom up.
                small[lastClearPosition] = new SoftReference(obj);
            } else {
                // It's not already there - in this case we need to insert the new entry somewhere
                // in the second half, shift all the ones below it down, and transfer the one
                // that falls off the end into the large array.
                // Firstly, add the one on the bottom to the large array. We are guaranteed that it
                // is not null.
                Object largeArray[] = null;
                if (large != null) {
                    largeArray = (Object []) large.get();
                }
                if (largeArray == null) {
                    largeArray = new Object[ACCESS_HOLDER_LARGE];
                    large = new SoftReference(largeArray);
                }
                largeArray[(accessedRand.nextInt(ACCESS_HOLDER_LARGE) + accessOffset)
                    % ACCESS_HOLDER_LARGE] = small[ACCESS_HOLDER_SMALL - 1].get();
                // Second, shift all the entries up to the insertion point towards the end.
                int newPosition = ACCESS_HOLDER_SMALL_MIDDLE
                    + accessedRand.nextInt(ACCESS_HOLDER_SMALL - ACCESS_HOLDER_SMALL_MIDDLE);
                for (int i = ACCESS_HOLDER_SMALL - 1; i > newPosition; i--) {
                    small[i] = small[i - 1];
                }
                small[newPosition] = new SoftReference(obj);
            }
        }
    }
}
