package org.intermine.metadata;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 *
 * @author Alex Kalderimis
 */
public final class DescriptorUtils
{
    private DescriptorUtils() {
        // Hidden.
    }

    /**
     * Find the ClassDescriptor for the type that any random element selected from a collection
     * of objects must be an instance of, where each object is an instance of one of the classes
     * represented by the class descriptors in the input collection. This method does not
     * consider any of the sub-types of the classes in question.
     *
     * eg:
     * <dl>
     *   <dt>Given <code>[Employee, Manager, CEO]</code></dt>
     *   <dd>returns <code>Employee</code></dd>
     *   <dt>Given <code>[CEO, Company]</code></dt>
     *   <dd>returns <code>HasAddress</code></dd>
     *   <dt>Given <code>[HasAddress, Employable]</code></dt>
     *   <dd>throws an exception</dd>
     *   <dt>Given <code>[]</code></dt>
     *   <dd>throws an exception</dd>
     * </dl>
     * @param classes The classes to consider.
     * @return The lowest common denominator.
     * @throws MetaDataException if no such type exists.
     */
    public static ClassDescriptor findSumType(Collection<ClassDescriptor> classes)
        throws MetaDataException {
        return findCommonClasses(classes).get(0);
    }

    /**
     * Find the ClassDescriptor for the type that any two or more objects must be of
     * where those objects are randomly selected from a collection containing objects which are of
     * the types represented by the class descriptors in the provided collection.
     *
     * In the case where the classes given share a common super type, but do not themselves
     * contain it, then that super type is returned. Where the classes present a lineage (eg.
     * <code>Employee &rarr; Manager &rarr; CEO</code> then the bottom of the lineage is
     * returned. For cases where the type structure contains a lineage that branches (eg.
     * <code>Thing &rarr; Employable &rarr; [Employee, Contractor]</code> the bottom of
     * the lineage before the branch (the last common ancestor) will be returned.
     *
     * @param classes The classes to consider.
     * @return The most specific common type.
     * @throws MetaDataException If no such type exists.
     */
    public static ClassDescriptor findIntersectionType(Collection<ClassDescriptor> classes)
        throws MetaDataException {
        List<ClassDescriptor> commonTypes = findCommonClasses(classes);
        ClassDescriptor commonSuperType = commonTypes.get(0);

        // Determine if this is a lineage.
        boolean isLineage = true;
        Set<ClassDescriptor> copyOfClasses = new HashSet<ClassDescriptor>(classes);
        ClassDescriptor lastCommonType = commonSuperType;
        while (isLineage) {
            copyOfClasses.remove(lastCommonType);
            if (copyOfClasses.isEmpty()) {
                break;
            }
            ClassDescriptor nextCommonSuperType = findSumType(copyOfClasses);
            isLineage = nextCommonSuperType != lastCommonType;
            lastCommonType = nextCommonSuperType;
            if (!(classes.contains(lastCommonType)
                    || lastCommonType.getAllSuperDescriptors().contains(commonSuperType))) {
                // This set of classes is disjoint.
                throw new MetaDataException("Disjoint set.");
            }
        }

        if (isLineage) {
            ClassDescriptor mostSpecific = sortClassesBySpecificity(classes).get(0);
            return mostSpecific;
        } else {
            return lastCommonType;
        }
    }

    /**
     * Return a list of the classes which present in the inheritance tree of all the given
     * classes. If more than one is returned, they will be sorted so that the most specific one
     * is at the head of the list.
     *
     * ie:, given <code>Employee, Manager, CEO</code>, this method should return
     * a list such as <code>Employee, Employable, HasAddress, Thing</code>. The returned
     * list should never contain <code>InterMineObject</code>. All returned classes are guaranteed
     * to be subclasses of {@link InterMineObject}. The returned list is guaranteed to never
     * be empty.
     *
     * @param classes The classes to investigate.
     * @return A liat of classes common to the inheritance tree of all of them.
     * @throws MetaDataException If
     */
    public static List<ClassDescriptor> findCommonClasses(Collection<ClassDescriptor> classes)
        throws MetaDataException {

        if (classes == null) {
            throw new IllegalArgumentException("classes is null");
        }
        if (classes.isEmpty()) {
            throw new MetaDataException("No classes provided");
        }
        if (classes.size() == 1) {
            return new ArrayList<ClassDescriptor>(classes);
        }

        Set<ClassDescriptor> superClasses = null;

        for (ClassDescriptor cd: classes) {
            Set<ClassDescriptor> ancestry = cd.getAllSuperDescriptors();
            ancestry.add(cd);
            // If this is the first one, initialise superClasses with this class and all its parents
            // else, Make superClasses the intersection of the current state of superClasses and the
            // ancestry of the current class.
            if (superClasses == null) {
                superClasses = ancestry;
            } else {
                superClasses.retainAll(ancestry);
            }
        }

        // Make sure all classes are InterMineObjs
        CollectionUtils.filter(classes, new Predicate() {
            @Override
            public boolean evaluate(Object arg0) {
                ClassDescriptor cd = (ClassDescriptor) arg0;
                return cd.getAllSuperclassNames().contains("org.intermine.model.InterMineObject");
            }
        });

        if (superClasses == null || superClasses.isEmpty()) {
            throw new MetaDataException("No common type");
        }
        return sortClassesBySpecificity(superClasses);
    }

    // Return a collection of ClassDescriptors sorted so that the most specific one (ie. the one
    // with the longest inheritance tree) is at index 0.
    private static List<ClassDescriptor> sortClassesBySpecificity
    (Collection<ClassDescriptor> classes) {
        List<ClassDescriptor> superList = new ArrayList<ClassDescriptor>(classes);

        Collections.sort(superList, new Comparator<ClassDescriptor>() {
            @Override
            public int compare(ClassDescriptor o1, ClassDescriptor o2) {
                int depth1 = o1.getAllSuperDescriptors().size();
                int depth2 = o2.getAllSuperDescriptors().size();
                if (depth1 <= depth2) {
                    return 1;
                } else if (depth1 >= depth2) {
                    return -1;
                } else {
                    return 0;
                }
            }

        });
        return superList;
    }
}
