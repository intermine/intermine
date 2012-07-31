package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * Utility functions for dealing with list requests.
 * @author Alex Kalderimis
 *
 */
public final class ListServiceUtils
{

    private ListServiceUtils() {
        // Uninstantiatable
    }

    private static final String CAST = "_temp_cast";

    /**
     * Given a common type, return a collection of bags cast to that type. The contents
     * of the returned bags is guaranteed to be the same as the input bags, and they may
     * be the same objects (if they do not need casting). The caller is responsible for deleting
     * the temporary lists accumulated in the name accumulator.
     * @param bags The bags to cast
     * @param type The type common to all bags
     * @param nameAccumulator A set of strings to hold the names of temporary lists.
     * @param profile A profile to use to create temporary bags
     * @param classKeys A classKeys to use to create bags
     * @return A set of bags of the given type.
     * @throws ObjectStoreException if there is a problem storing or creating bags.
     */
    public static Collection<InterMineBag> castBagsToCommonType(
            Collection<InterMineBag> bags, String type,
            Set<String> nameAccumulator, Profile profile,
            Map<String, List<FieldDescriptor>> classKeys)
        throws UnknownBagTypeException, ObjectStoreException {
        Set<InterMineBag> castBags = new HashSet<InterMineBag>();
        for (InterMineBag bag: bags) {
            if (bag.isOfType(type)) {
                castBags.add(bag);
            } else {
                String castName = bag.getName() + CAST + type;
                InterMineBag castBag
                    = profile.createBag(castName, type, "", classKeys);
                Query q = new Query();
                q.addToSelect(bag.getOsb());
                castBag.addToBagFromQuery(q);
                castBags.add(castBag);
                nameAccumulator.add(castName);
            }
        }
        return castBags;
    }

    /**
     * Given an array of bag names, populate both a set of bags, and a set of class-descriptors
     * with the objects these names refer to.
     * @param classes A set to hold the class descriptors for the given lists
     * @param lists A set to hold the resolved lists
     * @param profile The profile to use to get lists from (if not global lists)
     * @param manager A bag manager to use.
     * @param model A model to resolve list types to class-descriptors with.
     * @param listNames An array of list names
     * @throws BadRequestException if the lists cannot be resolved
     */
    public static void getBagsAndClasses(
            Set<ClassDescriptor> classes, Set<InterMineBag> lists,
            Profile profile, BagManager manager, Model model,
            String[] listNames) {
        for (String listName: listNames) {
            InterMineBag bag = manager.getUserOrGlobalBag(profile, listName);
            if (bag == null) {
                throw new ServiceForbiddenException(listName + " is not a list you have access to");
            }

            classes.add(model.getClassDescriptorByName(bag.getType()));
            lists.add(bag);
        }
    }

    /**
     * After this method returns, the will be no bag with the given name in the given profile.
     * @param profile The profile to target for list deletion.
     * @param name The name of the bag that must not exist.
     * @throws ObjectStoreException If there is a problem deleting the bag.
     */
    public static void ensureBagIsDeleted(Profile profile, String name)
        throws ObjectStoreException {
        try {
            profile.deleteBag(name);
        } catch (BagDoesNotExistException e) {
            // Ignore.
        }
    }

    /**
     * Finds the common class for a set of classes. So for a set with the members
     * Employee, Manager and CEO, Employee will be returned. For a set with the members
     * Employee, Company, and CEO, an exception will be thrown.
     * @param classes The classes that should have a common type.
     * @return A class name.
     */
    public static String findCommonSuperTypeOf(Set<ClassDescriptor> classes) {
        if (classes == null) {
            throw new IllegalArgumentException("classes is null");
        }
        if (classes.isEmpty()) {
            throw new RuntimeException(
                    "Class set is empty - no type can be determined");
        }
        if (classes.size() == 1) {
            return classes.iterator().next().getUnqualifiedName();
        }

        Set<String> classNames = new HashSet<String>();
        for (ClassDescriptor cd: classes) {
            classNames.add(cd.getUnqualifiedName());
        }
        String nameString = StringUtils.join(classNames, ", ");
        Set<ClassDescriptor> superClasses = null;
        for (ClassDescriptor cd: classes) {
            if (superClasses == null) {
                superClasses = new HashSet<ClassDescriptor>(cd.getAllSuperDescriptors());
                superClasses.add(cd);
            } else {
                Set<ClassDescriptor> toIntersect = cd.getAllSuperDescriptors();
                toIntersect.add(cd);
                superClasses.retainAll(toIntersect);
            }
        }
        CollectionUtils.filter(classes, new Predicate() {
            @Override
            public boolean evaluate(Object arg0) {
                ClassDescriptor cd = (ClassDescriptor) arg0;
                return cd.getAllSuperclassNames().contains("org.intermine.model.InterMineObject");
            }
        });
        if (superClasses.isEmpty()) {
            throw new BadRequestException("Incompatible types: " + nameString);
        }
        List<ClassDescriptor> superList = new ArrayList<ClassDescriptor>(superClasses);

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
        return superList.get(0).getUnqualifiedName();
    }

}
