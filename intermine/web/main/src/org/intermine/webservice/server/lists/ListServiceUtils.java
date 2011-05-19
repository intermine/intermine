package org.intermine.webservice.server.lists;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.webservice.server.exceptions.BadRequestException;

class ListServiceUtils {

    private ListServiceUtils() {
        // Uninstantiatable
    }

    public static final String CAST = "_temp_cast";

    /**
     * Given a common type, return a collection of bags cast to that type. The contents
     * of the returned bags is guaranteed to be the same as the input bags, and they may
     * be the same objects (if they do not need casting). The caller is responsible for deleting
     * the temporary lists accumulated in the name accumulator.
     * @param bags The bags to cast
     * @param type The type common to all bags
     * @param nameAccumulator A set of strings to hold the names of temporary lists.
     * @param profile A profile to use to create temporary bags
     * @return A set of bags of the given type.
     * @throws ObjectStoreException if there is a problem storing or creating bags.
     */
    public static Collection<InterMineBag> castBagsToCommonType(
            Collection<InterMineBag> bags, String type,
            Set<String> nameAccumulator, Profile profile)
        throws ObjectStoreException {
        Set<InterMineBag> castBags = new HashSet<InterMineBag>();
        for (InterMineBag bag: bags) {
            if (bag.isOfType(type)) {
                castBags.add(bag);
            } else {
                String castName = bag.getName() + CAST + type;
                InterMineBag castBag
                    = profile.createBag(castName, type, "");
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
                throw new BadRequestException(listName + " is not a list you have access to");
            }

            classes.add(model.getClassDescriptorByName(bag.getType()));
            lists.add(bag);
        }
    }

    public static void ensureBagIsDeleted(Profile profile, String name)
        throws ObjectStoreException {
        try {
            profile.deleteBag(name);
        } catch (BagDoesNotExistException e) {
            // Ignore.
        }
    }

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
        ClassDescriptor currentClass = null;
        Set<String> classNames = new HashSet<String>();
        for (ClassDescriptor cd: classes) {
            classNames.add(cd.getUnqualifiedName());
        }
        String nameString = StringUtils.join(classNames, ", ");
        for (ClassDescriptor cd: classes) {
            String thisType = cd.getName();
            if (currentClass == null || ListServiceUtils.getAllSuperclassNames(currentClass).contains(thisType)) {
                currentClass = cd;
                continue;
            }
            if (!getAllSuperclassNames(cd).contains(currentClass.getName())) {
                throw new RuntimeException("Incompatible types: " + nameString);
            }
        }
        return currentClass.getUnqualifiedName();
    }

    private static Set<String> getAllSuperclassNames(ClassDescriptor cd) {
        Set<String> classNames = new HashSet<String>();
        classNames.addAll(cd.getSuperclassNames());
        for (ClassDescriptor superCd: cd.getSuperDescriptors()) {
            classNames.addAll(getAllSuperclassNames(superCd));
        }
        return classNames;
    }

}
