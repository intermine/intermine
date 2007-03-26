package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.util.IntPresentSet;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Comparator, that compares two InterMineObjects, with reference to a particular
 * FieldDescriptor for priority.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class SourcePriorityComparator implements Comparator
{
    private static final Logger LOG = Logger.getLogger(SourcePriorityComparator.class);

    private DataTracker dataTracker;
    private FieldDescriptor field;
    private Source def;
    private InterMineObject defObj;
    private IntPresentSet dbIdsStored;
    private IntegrationWriterAbstractImpl iw;
    private Source source, skelSource;

    /**
     * Constructs a new Comparator for comparing objects for priority for a given field.
     *
     * @param dataTracker the data tracker
     * @param field the FieldDescriptor the comparison is for
     * @param def the default Source
     * @param defObj a InterMineObject that came from a data source, not from the destination
     * objectstore, and should be associated with the default source
     * @param iw the IntegrationWriter creating this comparator
     * @param source the main source, as passed to iw.store
     * @param skelSource the skeleton source, as passed to iw.store
     * @param dbIdsStored the set of IDs stored in this dataloader run - improves error messages
     */
    public SourcePriorityComparator(DataTracker dataTracker, FieldDescriptor field,
            Source def, InterMineObject defObj, IntPresentSet dbIdsStored,
            IntegrationWriterAbstractImpl iw, Source source, Source skelSource) {
        this.dataTracker = dataTracker;
        this.field = field;
        this.def = def;
        this.defObj = defObj;
        this.dbIdsStored = dbIdsStored;
        this.iw = iw;
        this.source = source;
        this.skelSource = skelSource;
    }

    /**
     * Compares two objects. These objects must both be InterMineObjects.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second
     * @throws ClassCastException if either of the two objects is not a InterMineObject
     * @throws RuntimeException if an error occurs in the underlying data tracking objectstore
     */
    public int compare(Object o1, Object o2) {
        if ((o1 instanceof InterMineObject) && (o2 instanceof InterMineObject)) {
            InterMineObject f1 = (InterMineObject) o1;
            InterMineObject f2 = (InterMineObject) o2;
            Source source1 = null;
            Source source2 = null;
            Object value1, value2;
            try {
                value1 = TypeUtil.getFieldProxy(o1, field.getName());
                value2 = TypeUtil.getFieldProxy(o2, field.getName());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            boolean storeValue1 = false;
            boolean storeValue2 = false;
            if (o1 == defObj) {
                source1 = def;
                storeValue1 = true;
            } else {
                source1 = dataTracker.getSource(f1.getId(), field.getName());
            }
            if (o2 == defObj) {
                source2 = def;
                storeValue2 = true;
            } else {
                source2 = dataTracker.getSource(f2.getId(), field.getName());
            }
            if (source1 == null) {
                throw new IllegalArgumentException("Object o1 is not in the data"
                        + " tracking system; o1 = \"" + o1 + "\", o2 = \"" + o2
                        + "\" for field \"" + field.getName() + "\"");
            }
            if (source2 == null) {
                throw new IllegalArgumentException("Object o2 is not in the data"
                        + " tracking system; o1 = \"" + o1 + "\", o2 = \"" + o2
                        + "\" for field \"" + field.getName() + "\"");
            }
            int retval = DataLoaderHelper.comparePriority(field, source1, source2,
                    value1, value2, iw, source, skelSource, storeValue1, storeValue2);
            if ((retval == 0) && (!o1.equals(o2)) && (!source1.getSkeleton())) {
                String errMessage = "Unequivalent objects have the same"
                    + " non-skeleton Source; o1 = \"" + o1 + "\" ("
                    + (o1 == defObj ? "from source" : (dbIdsStored.contains(f1.getId())
                                ? "stored in this run" : "from database")) + "), o2 = \"" + o2
                    + "\"(" + (o2 == defObj ? "from source" : (dbIdsStored.contains(f2.getId())
                                ? "stored in this run" : "from database")) + "), source1 = \""
                    + source1 + "\", source2 = \"" + source2 + "\" for field \""
                    + field.getName() + "\"";
                LOG.error(errMessage);
                throw new IllegalArgumentException(errMessage);
            }
            return retval;
        }
        throw new ClassCastException("Trying to compare priorities for objects that are not"
                + " InterMineObjects");
    }
}
