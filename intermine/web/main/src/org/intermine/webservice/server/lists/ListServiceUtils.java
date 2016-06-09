package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.ClassKeysNotFoundException;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.DescriptorUtils;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
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
     * @throws UnknownBagTypeException if the bag type is wrong
     * @throws ClassKeysNotFoundException if the classKeys is empty
     * @throws ObjectStoreException if there is a problem storing or creating bags.
     */
    public static Collection<InterMineBag> castBagsToCommonType(
            Collection<InterMineBag> bags, String type,
            Set<String> nameAccumulator, Profile profile,
            Map<String, List<FieldDescriptor>> classKeys)
        throws UnknownBagTypeException, ClassKeysNotFoundException, ObjectStoreException {
        return bags;
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
            InterMineBag bag = manager.getBag(profile, listName);
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
        try {
            return DescriptorUtils.findSumType(classes).getUnqualifiedName();
        } catch (MetaDataException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    /**
     * @param classes The classes we want to reduce to a common type.
     * @return The most specific common type.
     */
    public static String findMostSpecificCommonTypeOf(Set<ClassDescriptor> classes) {
        try {
            return DescriptorUtils.findIntersectionType(classes).getUnqualifiedName();
        } catch (MetaDataException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    /**
     * @param classes The classes we want to reduce to a set of common types.
     * @return The common types.
     */
    public static List<ClassDescriptor> findCommonClasses(Set<ClassDescriptor> classes) {
        try {
            return DescriptorUtils.findCommonClasses(classes);
        } catch (MetaDataException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

}
