package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.StringUtil;

/**
 * Merge model additions into a source model to produce a new larger model.
 *
 * @author Thomas Riley
 */
public final class ModelMerger
{
    private static final Logger LOG = Logger.getLogger(ModelMerger.class);

    /**
     * Forbid instantiation
     */
    private ModelMerger() {
       // empty
    }

    /**
     * 'Merges' the information from the set of ClassDescriptors <code>classes</code>
     * into the model <code>target</code>. This method does not actually modify
     * <code>target</code> but creates and returns a new model containing updated class
     * descriptors. Merging may involve the following actions:
     *
     * <ul>
     * <li>Adding fields.
     * <li>Adding references.
     * <li>Adding collections.
     * <li>Adding a new class.
     * <li>Modifying inheritance hierarchy.
     * </ul>
     *
     * @param original the existing model (not modified)
     * @param classes set of ClassDescriptors containing new information to add to the model
     * @return the resulting merged model
     * @throws ModelMergerException if an error occurs during model mergining
     */
    public static Model mergeModel(Model original,
            Set<ClassDescriptor> classes) throws ModelMergerException {
        Map<String, ClassDescriptor> newClasses = new HashMap<String, ClassDescriptor>();
        for (ClassDescriptor mergeClass : classes) {
            ClassDescriptor oldClass = original.getClassDescriptorByName(mergeClass.getName());
            ClassDescriptor newClass;
            // record this old class (
            if (oldClass != null) {
                // Merge two classes
                newClass = mergeClass(oldClass, mergeClass, original, classes);
            } else {
                // It is a new class
                newClass = cloneClassDescriptor(mergeClass);
            }
            newClasses.put(newClass.getName(), newClass);
        }
        // Find the original classes that weren't mentioned classes
        for (ClassDescriptor oldClass : original.getClassDescriptors()) {
            if (!newClasses.containsKey(oldClass.getName())) {
                // We haven't merged this class so we add the old one
                newClasses.put(oldClass.getName(), cloneClassDescriptor(oldClass));
            }
        }
        // Remove any reference or collection descriptors made redundant by additions
        newClasses = removeRedundancy(newClasses);
        try {
            Model newModel = new Model(original.getName(), original.getPackageName(),
                    original.getVersion(), new HashSet<ClassDescriptor>(newClasses.values()));
            if (newModel.hasProblems()) {
                throw new ModelMergerException("There were problems merging the model: "
                        + newModel.getProblems());
            }
            return newModel;

        } catch (MetaDataException err) {
            throw new ModelMergerException(err);
        }
    }

    /**
     * When changes are made to the inheritance hierarchy (especially the addition of
     * superinterfaces), fields may be defined on superinterfaces making previous definitions
     * further down the inheritance hierarchy redundant. This method removes those redundant
     * fields.
     *
     * @param classes starting collection of ClassDescriptors
     * @return a new mapping from class name to ClassDescriptors
     * @throws ModelMergerException if an error occurs during model merging
     */
    protected static Map<String, ClassDescriptor> removeRedundancy(Map<String,
            ClassDescriptor> classes) throws ModelMergerException {
        Map<String, ClassDescriptor> newSet = new HashMap<String, ClassDescriptor>();

        for (ClassDescriptor cd : classes.values()) {
            Set<CollectionDescriptor> cdescs = cloneCollectionDescriptors(
                    cd.getCollectionDescriptors());
            Set<AttributeDescriptor> adescs = cloneAttributeDescriptors(
                    cd.getAttributeDescriptors());
            Set<ReferenceDescriptor> rdescs = cloneReferenceDescriptors(
                    cd.getReferenceDescriptors());
            Set<String> supers = new HashSet<String>();
            findAllSuperclasses(cd, classes, supers);
            // Now remove any attributes, references or collections that are now defined
            // in a superclass/superinterface
            for (String sup : supers) {
                ClassDescriptor scd = classes.get(sup);

                // Check attributes
                for (Iterator<AttributeDescriptor> aiter = adescs.iterator(); aiter.hasNext();) {
                    AttributeDescriptor ad = aiter.next();
                    if (scd.getAttributeDescriptorByName(ad.getName()) != null) {
                        LOG.info("removing attribute " + ad.getName()
                                + " redefinition in " + cd.getName() + " (is now defined in "
                                + scd.getName() + ")");
                        aiter.remove();
                    }
                }

                // Check references
                for (Iterator<ReferenceDescriptor> riter = rdescs.iterator(); riter.hasNext();) {
                    ReferenceDescriptor rd = riter.next();
                    ReferenceDescriptor scdDescriptor =
                        scd.getReferenceDescriptorByName(rd.getName());
                    if (scdDescriptor != null) {
                        LOG.info("removing reference " + rd.getName()
                                + " redefinition in " + cd.getName() + " (is now defined in "
                                + scd.getName() + ")");
                        String revName = rd.getReverseReferenceFieldName();
                        String scdRevFieldName = scdDescriptor.getReverseReferenceFieldName();
                        if (StringUtils.equals(revName, scdRevFieldName)) {
                            riter.remove();
                        } else {
                            String message = "replacing the \"" + sup + "." + rd.getName()
                                + "\" reference with " + cd.getName() + "."
                                + rd.getName() + " failed because the reverse references differ";
                            throw new ModelMergerException(message);
                        }
                    }
                }

                // Check collections
                for (Iterator<CollectionDescriptor> citer = cdescs.iterator(); citer.hasNext();) {
                    CollectionDescriptor cold = citer.next();
                    CollectionDescriptor scdDescriptor =
                        scd.getCollectionDescriptorByName(cold.getName());
                    if (scd.getCollectionDescriptorByName(cold.getName()) != null) {
                        LOG.info("removing collection " + cold.getName()
                                + " redefinition in " + cd.getName() + " (is now defined in "
                                + scd.getName() + ")");
                        String revName = cold.getReverseReferenceFieldName();
                        String scdRevFieldName = scdDescriptor.getReverseReferenceFieldName();
                        if (StringUtils.equals(revName, scdRevFieldName)) {
                            citer.remove();
                        } else {
                            String message = "replacing the \"" + sup + "." + cold.getName()
                                + "\" collection with " + cd.getName() + "."
                                + cold.getName() + " failed because the reverse references differ";
                            throw new ModelMergerException(message);
                        }
                    }
                }
            }

            String supersStr = toSupersString(cd.getSuperclassNames());

            newSet.put(cd.getName(), new ClassDescriptor(cd.getName(), supersStr, cd.isInterface(),
                                                cloneAttributeDescriptors(adescs),
                                                cloneReferenceDescriptors(rdescs),
                                                cloneCollectionDescriptors(cdescs)));
        }

        return newSet;
    }

    private static String toSupersString(Set<String> supers) {
        String supersStr = StringUtil.join(supers, " ");
        if (supersStr != null && "".equals(supersStr)) {
            supersStr = null;
        }
        return supersStr;
    }

    private static void findAllSuperclasses(ClassDescriptor cd,
            Map<String, ClassDescriptor> classes, Set<String> names) throws ModelMergerException {
        Set<String> supers = cd.getSuperclassNames();
        for (String superClassName : supers) {
            if (!"java.lang.Object".equals(superClassName)) {
                names.add(superClassName);
                ClassDescriptor cld = classes.get(superClassName);

                if (cld == null) {
                    throw new ModelMergerException("cannot find superclass \"" + superClassName
                                                   + "\" of " + cd + " in the model");
                }

                findAllSuperclasses(cld, classes, names);
            }
        }
    }

    /**
     * Merge the attributes, collections and references from ClassDescriptor <code>merge</code>
     * into the ClassDescriptor <code>original</code>. The two are different in that inheritance
     * settings on the merge class can override the inheritance present in the original class.
     * This method will throw a ModelMergerException if the two class descriptors return different
     * values from <code>isInterface</code>.<p>
     *
     * If the original class extends a superclass, and the <code>merge</code> also specifies
     * a superclass then the merge superclass will override the old superclass.<p>
     *
     * This method requires <code>originalModel</code> and <code>mergeClasses</code> so it can
     * determine whether the superclass names in <code>merge</code> represente classes or
     * interfaces.
     *
     * @param original the original ClassDescriptor
     * @param merge the ClassDescriptor to merge into the original
     * @param originalModel the original Model we're merging into
     * @param mergeClasses the set of ClassDescriptors being merged
     * @return ClassDescriptor merge "merged" into ClassDescriptor original
     * @throws ModelMergerException if an error occurs during model merging
     */
    public static ClassDescriptor mergeClass(ClassDescriptor original, ClassDescriptor merge,
            Model originalModel, Set<ClassDescriptor> mergeClasses) throws ModelMergerException {
        if (merge.isInterface() != original.isInterface()) {
            throw new ModelMergerException("Same class definition found as a class and interface "
                    + original.getName() + ".isInterface/"
                    + original.isInterface() + " != " + merge.getName() + ".isInterface/"
                    + merge.isInterface());
        }
        Set<AttributeDescriptor> attrs = mergeAttributes(original, merge);
        Set<CollectionDescriptor> cols = mergeCollections(original, merge);
        Set<ReferenceDescriptor> refs = mergeReferences(original, merge);

        Set<String> supers = new TreeSet<String>();
        boolean replacingSuperclass = false;
        // Figure out if we're replacing the superclass
        if (original.getSuperclassDescriptor() != null) {
            Set<String> superNames = merge.getSuperclassNames();
            for (String clsName : superNames) {
                ClassDescriptor cld = originalModel.getClassDescriptorByName(clsName);
                if (cld != null && !cld.isInterface()) {
                    replacingSuperclass = true;
                    break;
                }
                cld = descriptorByName(mergeClasses, clsName);
                if (cld != null && !cld.isInterface()) {
                    replacingSuperclass = true;
                    break;
                }
            }
        }
        supers.addAll(original.getSuperclassNames());
        supers.addAll(merge.getSuperclassNames());
        if (replacingSuperclass) {
            supers.remove(original.getSuperclassDescriptor().getName());
        }
        // supers can't be an empty string
        String supersStr = StringUtil.join(supers, " ");
        if (supersStr != null && "".equals(supersStr)) {
            supersStr = null;
        }
        return new ClassDescriptor(original.getName(), supersStr,
                merge.isInterface(), attrs, refs, cols);
    }

    /**
     * Merge the attributes of a target model class descriptor <code>original</code> with
     * the attributes present in class descriptor <code>merge</code>. Returns a new set of
     * AttributeDescriptors.
     *
     * @param original the target model class descriptor
     * @param merge the additions
     * @return new set of AttributeDescriptors
     * @throws ModelMergerException if an error occurs during model mergining
     */
    public static Set<AttributeDescriptor> mergeAttributes(ClassDescriptor original,
            ClassDescriptor merge) throws ModelMergerException {
        for (AttributeDescriptor merg : merge.getAttributeDescriptors()) {
            // nb: does not look for references in superclasses/superinterfaces
            AttributeDescriptor orig = original.getAttributeDescriptorByName(merg.getName());
            if (orig != null) {
                if (!merg.getType().equals(orig.getType())) {
                    String fldName = original.getName() + "." + orig.getName();
                    throw new ModelMergerException("type mismatch between attributes: "
                            + fldName + ":" + merg.getType() + " != "
                            + fldName + ":" + orig.getType());
                }
            }
        }

        Set<AttributeDescriptor> newSet = new HashSet<AttributeDescriptor>();
        newSet.addAll(cloneAttributeDescriptors(original.getAttributeDescriptors()));
        newSet.addAll(cloneAttributeDescriptors(merge.getAttributeDescriptors()));
        return newSet;
    }

    /**
     * Merge the collections of a target model class descriptor <code>original</code> with
     * the collections present in class descriptor <code>merge</code>. Returns a new set of
     * CollectionDescriptors.
     *
     * @param original the target model class descriptor
     * @param merge the additions
     * @return new set of CollectionDescriptors
     * @throws ModelMergerException if an error occurs during model mergining
     */
    public static Set<CollectionDescriptor> mergeCollections(ClassDescriptor original,
            ClassDescriptor merge) throws ModelMergerException {
        Set<CollectionDescriptor> newSet = new HashSet<CollectionDescriptor>();
        newSet.addAll(cloneCollectionDescriptors(original.getCollectionDescriptors()));
        for (CollectionDescriptor merg : merge.getCollectionDescriptors()) {
            // nb: does not look for references in superclasses/superinterfaces
            CollectionDescriptor orig = original.getCollectionDescriptorByName(merg.getName());

            if (orig != null) {

                // New descriptor may add a reverse reference field name
                if (merg.getReverseReferenceFieldName() != null
                    && orig.getReverseReferenceFieldName() == null) {
                    // This is a valid change - remove original descriptor and replace with new
                    removeFieldDescriptor(newSet, orig.getName());
                    newSet.add(cloneCollectionDescriptor(merg));
                    continue;
                }

                // Check for inconsistencies and throw exceptions if inconsistencies are found
                if (!StringUtils.equals(merg.getReverseReferenceFieldName(),
                                        orig.getReverseReferenceFieldName())) {
                    String fldName = original.getName() + "." + orig.getName();
                    throw new ModelMergerException("mismatch between reverse reference field name: "
                            + fldName + "<-" + merg.getReverseReferenceFieldName() + " != "
                            + fldName + "<-" + orig.getReverseReferenceFieldName());
                }

                if (!merg.getReferencedClassName().equals(orig.getReferencedClassName())) {
                    String fldName = original.getName() + "." + orig.getName();
                    throw new ModelMergerException("type mismatch between collection types: "
                            + fldName + ":" + merg.getReferencedClassName() + " != "
                            + fldName + ":" + orig.getReferencedClassName());
                }
            }

            // New descriptor of no differences, so add merg to newSet
            newSet.add(cloneCollectionDescriptor(merg));
        }
        return newSet;
    }

    /**
     * Merge the references of a target model class descriptor <code>original</code> with
     * the references present in class descriptor <code>merge</code>. Returns a new set of
     * ReferenceDescriptors.
     *
     * @param original the target model class descriptor
     * @param merge the additions
     * @return new set of CollectionDescriptors
     * @throws ModelMergerException if an error occurs during model mergining
     */
    public static Set<ReferenceDescriptor> mergeReferences(ClassDescriptor original,
            ClassDescriptor merge) throws ModelMergerException {
        Set<ReferenceDescriptor> newSet = new HashSet<ReferenceDescriptor>();
        newSet.addAll(cloneReferenceDescriptors(original.getReferenceDescriptors()));
        for (ReferenceDescriptor merg : merge.getReferenceDescriptors()) {
            // nb: does not look for references in superclasses/superinterfaces
            ReferenceDescriptor orig = original.getReferenceDescriptorByName(merg.getName());
            if (orig != null) {

                // New descriptor may add a reverse reference field name
                if (merg.getReverseReferenceFieldName() != null
                    && orig.getReverseReferenceFieldName() == null) {
                    // This is a valid change - remove original descriptor and replace with new
                    removeFieldDescriptor(newSet, orig.getName());
                    newSet.add(cloneReferenceDescriptor(merg));
                    continue;
                }

                if (!merg.getReferencedClassName().equals(orig.getReferencedClassName())) {
                    String fldName = original.getName() + "." + orig.getName();
                    throw new ModelMergerException("type mismatch between reference types: "
                            + fldName + ":" + merg.getReferencedClassName() + " != "
                            + fldName + ":" + orig.getReferencedClassName());
                }
                if (!StringUtils.equals(merg.getReverseReferenceFieldName(),
                        orig.getReverseReferenceFieldName())) {
                    String fldName = original.getName() + "." + orig.getName();
                    throw new ModelMergerException("mismatch between reverse reference field name: "
                            + fldName + "<-" + merg.getReverseReferenceFieldName() + " != "
                            + fldName + "<-" + orig.getReverseReferenceFieldName());
                }
            }

            // New descriptor of no differences, so add merg to newSet
            newSet.add(cloneReferenceDescriptor(merg));
        }

        return newSet;
    }

    /**
     * Clone a set of ReferenceDescriptors.
     *
     * @param refs a set of ReferenceDescriptors
     * @return cloned set of ReferenceDescriptors
     */
    protected static Set<ReferenceDescriptor> cloneReferenceDescriptors(
            Set<ReferenceDescriptor> refs) {
        Set<ReferenceDescriptor> copy = new HashSet<ReferenceDescriptor>();
        for (ReferenceDescriptor ref : refs) {
            copy.add(cloneReferenceDescriptor(ref));
        }
        return copy;
    }

    private static ReferenceDescriptor cloneReferenceDescriptor(ReferenceDescriptor ref) {
        return new ReferenceDescriptor(ref.getName(), ref.getReferencedClassName(),
                ref.getReverseReferenceFieldName());
    }

    /**
     * Clone a set of CollectionDescriptors.
     *
     * @param refs a set of CollectionDescriptors
     * @return cloned set of CollectionDescriptors
     */
    protected static Set<CollectionDescriptor> cloneCollectionDescriptors(
            Set<CollectionDescriptor> refs) {
        Set<CollectionDescriptor> copy = new LinkedHashSet<CollectionDescriptor>();
        for (CollectionDescriptor ref : refs) {
            copy.add(cloneCollectionDescriptor(ref));
        }
        return copy;
    }

    private static CollectionDescriptor cloneCollectionDescriptor(CollectionDescriptor ref) {
        return new CollectionDescriptor(ref.getName(), ref.getReferencedClassName(),
                ref.getReverseReferenceFieldName());
    }

    /**
     * Remove a FieldDescriptor from a Set of FieldDescriptors by name.
     *
     * @param fields set of FieldDescriptors
     * @param name the field name
     */
    protected static void removeFieldDescriptor(Set<? extends FieldDescriptor> fields,
            String name) {
        for (Iterator<? extends FieldDescriptor> iter = fields.iterator(); iter.hasNext();) {
            FieldDescriptor desc = iter.next();
            if (desc.getName().equals(name)) {
                iter.remove();
                return;
            }
        }
    }

    /**
     * Clone a set of AttributeDescriptors.
     *
     * @param refs a set of AttributeDescriptors
     * @return cloned set of AttributeDescriptors
     */
    protected static Set<AttributeDescriptor> cloneAttributeDescriptors(
            Set<AttributeDescriptor> refs) {
        Set<AttributeDescriptor> copy = new HashSet<AttributeDescriptor>();
        for (AttributeDescriptor ref : refs) {
            copy.add(new AttributeDescriptor(ref.getName(), ref.getType()));
        }
        return copy;
    }

    /**
     * Construct a ClassDescriptor that takes on all the properties of <code>cld</code>
     * without attaching to a particular Model.
     *
     * @param cld the ClassDescriptor to clone
     * @return cloned ClassDescriptor
     */
    protected static ClassDescriptor cloneClassDescriptor(ClassDescriptor cld) {
        // supers can't be an empty string
        String supers = StringUtil.join(cld.getSuperclassNames(), " ");
        if (supers != null && "".equals(supers)) {
            supers = null;
        }
        return new ClassDescriptor(cld.getName(), supers, cld.isInterface(),
                cloneAttributeDescriptors(cld.getAttributeDescriptors()),
                cloneReferenceDescriptors(cld.getReferenceDescriptors()),
                cloneCollectionDescriptors(cld.getCollectionDescriptors()));
    }

    /**
     * Find ClassDescriptor in set with given name.
     */
    private static ClassDescriptor descriptorByName(Set<ClassDescriptor> clds, String name) {
        for (ClassDescriptor cld : clds) {
            if (cld.getName().equals(name)) {
                return cld;
            }
        }
        return null;
    }
}
