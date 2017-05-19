package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Object class used by DataTracker for data tracking.
 *
 * @author Matthew Wakeling
 */
public class ObjectDescription
{
    private boolean dirty = false;
    private Map<String, Source> orig = null;
    private Map<String, Source> newData = null;

    /**
     * Constructs a new ObjectDescription.
     */
    public ObjectDescription() {
    }

    /**
     * Constructs a new ObjectDescription from an existing one.
     * This is used for copy-on-write for the write-back cache in the DataTracker.
     *
     * @param desc an existing description
     */
    public ObjectDescription(ObjectDescription desc) {
        dirty = desc.dirty;
        if (desc.orig != null) {
            orig = new HashMap<String, Source>(desc.orig);
        }
        if (desc.newData != null) {
            newData = new HashMap<String, Source>(desc.newData);
        }
    }

    /**
     * Adds a fieldname-source mapping for this ObjectDescription while keeping it clean.
     *
     * @param fieldName the name of the field
     * @param source the Source to map onto
     * @throws IllegalStateException if this ObjectDescription is already dirty
     */
    public void putClean(String fieldName, Source source) {
        if (dirty) {
            throw new IllegalStateException("Can't putClean() on a dirty ObjectDescription");
        }
        if (orig == null) {
            orig = new HashMap<String, Source>();
        }
        orig.put(fieldName, source);
    }

    /**
     * Adds a fieldname-source mapping for this ObjectDescription that is not present in the backing
     * database. This makes the ObjectDescription dirty, so that the change is written back.
     *
     * @param fieldName the name of the field
     * @param source the Source to map onto
     */
    public void put(String fieldName, Source source) {
        if (!dirty) {
            dirty = true;
            newData = new HashMap<String, Source>();
        }
        newData.put(fieldName, source);
    }

    /**
     * Gets the source associated with the given fieldname.
     *
     * @param fieldName the fieldname to look up
     * @return the Source, or null if it doesn't exist
     */
    public Source getSource(String fieldName) {
        if (newData != null) {
            Source retval = newData.get(fieldName);
            if (retval != null) {
                return retval;
            }
        }
        if (orig != null) {
            return orig.get(fieldName);
        }
        return null;
    }

    /**
     * Returns whether this ObjectDescription is dirty.
     *
     * @return true if the ObjectDescriptor is dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Marks the ObjectDescription as clean - that is all the data has been flushed to the backing
     * database.
     */
    public void clean() {
        if (dirty) {
            dirty = false;
            if (orig == null) {
                orig = new HashMap<String, Source>();
            }
            for (Map.Entry<String, Source> entry : newData.entrySet()) {
                orig.put(entry.getKey(), entry.getValue());
            }
            newData = null;
        }
    }

    /**
     * Returns the original data, as reflected in the backing database.
     *
     * @return a Map
     */
    protected Map<String, Source> getOrig() {
        if (orig == null) {
            return Collections.emptyMap();
        }
        return orig;
    }

    /**
     * Returns the new data, which needs to be written back into the backing database.
     *
     * @return a Map
     */
    protected Map<String, Source> getNewData() {
        return newData;
    }
}
