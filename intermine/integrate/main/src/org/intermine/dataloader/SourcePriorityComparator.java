package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;
import java.util.List;

import static org.intermine.dataloader.IntegrationWriterAbstractImpl.SKELETON;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IntPresentSet;

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
    private Class clazz;
    private String fieldName;
    private Source def;
    private InterMineObject defObj;
    private IntPresentSet dbIdsStored;
    private IntegrationWriterAbstractImpl iw;
    private Source source, skelSource;
    private PriorityConfig priorityConfig;

    /**
     * Constructs a new Comparator for comparing objects for priority for a given field.
     *
     * @param dataTracker the data tracker
     * @param clazz the Class of the resulting object
     * @param fieldName the fieldName the comparison is for
     * @param def the default Source
     * @param defObj a InterMineObject that came from a data source, not from the destination
     * objectstore, and should be associated with the default source
     * @param dbIdsStored the set of IDs stored in this dataloader run - improves error messages
     * @param iw the IntegrationWriter creating this comparator
     * @param source the main source, as passed to iw.store
     * @param skelSource the skeleton source, as passed to iw.store
     * @param priorityConfig a PriorityConfig object for the target Model
     */
    public SourcePriorityComparator(DataTracker dataTracker, Class clazz, String fieldName,
            Source def, InterMineObject defObj, IntPresentSet dbIdsStored,
            IntegrationWriterAbstractImpl iw, Source source, Source skelSource,
            PriorityConfig priorityConfig) {
        this.dataTracker = dataTracker;
        this.clazz = clazz;
        this.fieldName = fieldName;
        this.def = def;
        this.defObj = defObj;
        this.dbIdsStored = dbIdsStored;
        this.iw = iw;
        this.source = source;
        this.skelSource = skelSource;
        this.priorityConfig = priorityConfig;
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
                value1 = f1.getFieldProxy(fieldName);
                value2 = f2.getFieldProxy(fieldName);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            List<String> srcs = priorityConfig.getPriorities(clazz, fieldName);
            if (srcs != null) {
                if (f1 == defObj) {
                    source1 = def;
                } else {
                    source1 = dataTracker.getSource(f1.getId(), fieldName);
                }
                if (f2 == defObj) {
                    source2 = def;
                } else {
                    source2 = dataTracker.getSource(f2.getId(), fieldName);
                }
                if (source1 == null && value1 != null) {
                    throw new IllegalArgumentException("Object o1 is not in the data"
                            + " tracking system; o1 = \"" + DynamicUtil.getFriendlyDesc(o1)
                            + "\", o2 = \"" + DynamicUtil.getFriendlyDesc(o2) + "\" for field \""
                            + fieldName + "\" with value \"" + value1 + "\"");
                }
                if (source2 == null && value2 != null) {
                    throw new IllegalArgumentException("Object o2 is not in the data"
                            + " tracking system; o1 = \"" + DynamicUtil.getFriendlyDesc(o1)
                            + "\", o2 = \"" + DynamicUtil.getFriendlyDesc(o2) + "\" for field \""
                            + fieldName + "\" with value \"" + value2 + "\"");
                }
                if (source1 != null && source2 != null) {
                    if (source1.getName().equals(source2.getName())) {
                        if (source1.getSkeleton() && (!source2.getSkeleton())) {
                            return -1;
                        } else if (source2.getSkeleton() && (!source1.getSkeleton())) {
                            return 1;
                        } else {
                            if ((!o1.equals(o2)) && (!source1.getSkeleton())) {
                                String errMessage = "Duplicate objects from the same data source; "
                                    + "o1 = \"" + (o1 == defObj ? DynamicUtil.getFriendlyName(
                                                o1.getClass()) + "\" (from source, being stored"
                                            : DynamicUtil.getFriendlyDesc(o1) + "\" ("
                                            + ((dbIdsStored.contains(f1.getId())
                                                    ? "stored earlier in this run"
                                                    : "in database")))
                                    + "), o2 = \"" + (o2 == defObj ? DynamicUtil.getFriendlyName(
                                            o2.getClass()) + "\" (from source, being stored"
                                        : DynamicUtil.getFriendlyDesc(o2) + "\" ("
                                        + ((dbIdsStored.contains(f2.getId())
                                                ? "stored earlier in this run" : "in database")))
                                    + "), source1 = \"" + source1 + "\", source2 = \"" + source2
                                    + "\"";
                                LOG.error(errMessage);
                                throw new IllegalArgumentException(errMessage);
                            }
                            return 0;
                        }
                    }
                    int source1Priority = srcs.indexOf(source1.getName());
                    if (source1Priority == -1) {
                        source1Priority = srcs.indexOf("*");
                    }
                    int source2Priority = srcs.indexOf(source2.getName());
                    if (source2Priority == -1) {
                        source2Priority = srcs.indexOf("*");
                    }
                    String errorMessage = null;
                    if (source1Priority == -1) {
                        errorMessage = "Priority configured for "
                            + DynamicUtil.getFriendlyName(clazz) + "." + fieldName
                            + " does not include source " + source1.getName();
                    }
                    if (source2Priority == -1) {
                        if (errorMessage != null) {
                            errorMessage = "Priority configured for "
                                + DynamicUtil.getFriendlyName(clazz) + "." + fieldName
                                + " does not include sources " + source1.getName() + " or "
                                + source2.getName();
                        } else {
                            errorMessage = "Priority configured for "
                                + DynamicUtil.getFriendlyName(clazz) + "." + fieldName
                                + " does not include source " + source2.getName();
                        }
                    }
                    if (errorMessage != null) {
                        if ((value1 == null) && (value2 == null)) {
                            return (f1 == defObj ? 1 : -1);
                        }
                        if (value1 == null) {
                            return -1;
                        }
                        if (value2 == null) {
                            return 1;
                        }
                        LOG.error(errorMessage);
                        throw new IllegalArgumentException(errorMessage);
                    }
                    int retval = source2Priority - source1Priority;
                    if (retval != 0) {
                        return retval;
                    }
                }
            }
            if ((value1 == null) && (value2 == null)) {
                return (f1 == defObj ? 1 : -1);
            }
            if (value1 == null) {
                return -1;
            }
            if (value2 == null) {
                return 1;
            }
            try {
                if ((f1 == defObj) && (value1 instanceof InterMineObject)) {
                    if (value1 instanceof ProxyReference) {
                        value1 = ((ProxyReference) value1).getObject();
                    }
                    value1 = iw.store((InterMineObject) value1, source, skelSource, SKELETON);
                }
                if ((f2 == defObj) && (value2 instanceof InterMineObject)) {
                    if (value2 instanceof ProxyReference) {
                        value2 = ((ProxyReference) value2).getObject();
                    }
                    value2 = iw.store((InterMineObject) value2, source, skelSource, SKELETON);
                }
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
            if (((value1 instanceof InterMineObject) && (value2 instanceof InterMineObject)
                        && (((InterMineObject) value1).getId().equals(((InterMineObject) value2)
                                .getId()))) || value1.equals(value2)) {
                return (f1 == defObj ? 1 : -1);
            }
            if (value1 instanceof ProxyReference) {
                value1 = ((ProxyReference) value1).getObject();
            }
            if (value2 instanceof ProxyReference) {
                value2 = ((ProxyReference) value2).getObject();
            }
            if (source1 == null) {
                if (f1 == defObj) {
                    source1 = def;
                } else {
                    source1 = dataTracker.getSource(f1.getId(), fieldName);
                }
            }
            if (source2 == null) {
                if (f2 == defObj) {
                    source2 = def;
                } else {
                    source2 = dataTracker.getSource(f2.getId(), fieldName);
                }
            }
            if (source1.equals(source2)) {
                throw new IllegalArgumentException("Merging two distinct objects from the same"
                       + " data source (" + source1.getName() + "): "
                       + (o1 == defObj ? DynamicUtil.getFriendlyName(o1.getClass())
                           + " (being stored)" : DynamicUtil.getFriendlyDesc(o1) + " (in database)")
                       + " and " + (o2 == defObj ? DynamicUtil.getFriendlyName(o2.getClass())
                           + " (being stored)" : DynamicUtil.getFriendlyDesc(o2)
                           + " (in database)"));
            }
            throw new IllegalArgumentException("Conflicting values for field "
                    + DynamicUtil.getFriendlyName(clazz) + "." + fieldName + " between "
                    + source1.getName() + " (value \""
                    +  value1 + "\""
                    + (o1 != defObj ? " in database with ID " + ((InterMineObject) o1).getId()
                        : " being stored") + ") and " + source2.getName() + " (value \""
                    + value2
                    + (o2 != defObj ? " in database with ID " + ((InterMineObject) o2).getId()
                        : " being stored") + "). This field needs configuring in the "
                    + iw.getModel().getName() + "_priorities.properties file");
        }
        throw new ClassCastException("Trying to compare priorities for objects that are not"
                + " InterMineObjects");
    }
}
