package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

import org.flymine.metadata.FieldDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Comparator, that compares two FlymineBusinessObjects, with reference to a particular
 * FieldDescriptor for priority.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class SourcePriorityComparator implements Comparator
{
    private ObjectStoreWriter dataTracker;
    private FieldDescriptor field;
    private Source def;
    private FlyMineBusinessObject defObj;

    /**
     * Constructs a new Comparator for comparing objects for priority for a given field.
     *
     * @param dataTracker the data tracking ObjectStoreWriter
     * @param field the FieldDescriptor the comparison is for
     * @param def the default Source
     * @param defObj a FlyMineBusinessObject that came from a data source, not from the destination
     * objectstore, and should be associated with the default source
     */
    public SourcePriorityComparator(ObjectStoreWriter dataTracker, FieldDescriptor field,
            Source def, FlyMineBusinessObject defObj) {
        this.dataTracker = dataTracker;
        this.field = field;
        this.def = def;
        this.defObj = defObj;
    }

    /**
     * Compares two objects. These objects must both be FlyMineBusinessObjects.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second
     * @throws ClassCastException if either of the two objects is not a FlyMineBusinessObject
     * @throws RuntimeException if an error occurs in the underlying data tracking objectstore
     */
    public int compare(Object o1, Object o2) {
        try {
            if ((o1 instanceof FlyMineBusinessObject) && (o2 instanceof FlyMineBusinessObject)) {
                FlyMineBusinessObject f1 = (FlyMineBusinessObject) o1;
                FlyMineBusinessObject f2 = (FlyMineBusinessObject) o2;
                Source source1 = null;
                Source source2 = null;
                if (o1 == defObj) {
                    source1 = def;
                } else {
                    source1 = DataTracking.getSource(f1, field.getName(), dataTracker);
                }
                if (o2 == defObj) {
                    source2 = def;
                } else {
                    source2 = DataTracking.getSource(f2, field.getName(), dataTracker);
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
                int retval = DataLoaderHelper.comparePriority(field, source1, source2);
                if ((retval == 0) && (!o1.equals(o2)) && (!source1.getSkeleton())) {
                    throw new IllegalArgumentException("Unequivalent objects have the same"
                            + " non-skeleton Source; o1 = \"" + o1 + "\", o2 = \"" + o2
                            + "\", source1 = \"" + source1 + "\", source2 = \"" + source2
                            + "\" for field \"" + field.getName() + "\"");
                }
                return retval;
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        throw new ClassCastException("Trying to compare priorities for objects that are not"
                + " FlyMineBusinessObjects");
    }
}
