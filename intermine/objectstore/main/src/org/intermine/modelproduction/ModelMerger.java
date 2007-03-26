package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URISyntaxException;
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
import org.intermine.util.StringUtil;

/**
 * Merge model additions into a source model to produce a new larger model.
 *
 * @author Thomas Riley
 */
public class ModelMerger
{
    private static final Logger LOG = Logger.getLogger(ModelMerger.class);
    
    /**
     * Creates a new instance of ModelMerger.
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
    public static Model mergeModel(Model original, Set classes) throws ModelMergerException {
        Map newClasses = new HashMap();
        Iterator iter = classes.iterator();
        while (iter.hasNext()) {
            ClassDescriptor mergeClass = (ClassDescriptor) iter.next();
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
        iter = original.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor oldClass = (ClassDescriptor) iter.next();
            if (!newClasses.containsKey(oldClass.getName())) {
                // We haven't merged this class so we add the old one
                newClasses.put(oldClass.getName(), cloneClassDescriptor(oldClass));
            }
        }
        // Remove any reference or collection descriptors made redundant by additions
        newClasses = removeRedundancy(newClasses);
        try {
            Model newModel = new Model(original.getName(), original.getNameSpace().toString(),
                                        new HashSet(newClasses.values()));
            return newModel;
        
        } catch (URISyntaxException err) {
            throw new ModelMergerException(err);
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
    protected static Map removeRedundancy(Map classes) throws ModelMergerException {
        Map newSet = new HashMap();
        
        for (Iterator iter = classes.values().iterator(); iter.hasNext();) {
            ClassDescriptor cd = (ClassDescriptor) iter.next();
            Set cdescs = cloneCollectionDescriptors(cd.getCollectionDescriptors());
            Set adescs = cloneAttributeDescriptors(cd.getAttributeDescriptors());
            Set rdescs = cloneReferenceDescriptors(cd.getReferenceDescriptors());
            Set supers = new HashSet();
            findAllSuperclasses(cd, classes, supers);
            // Now remove any attributes, references or collections that are now defined
            // in a superclass/superinterface
            for (Iterator siter = supers.iterator(); siter.hasNext();) {
                String sup = (String) siter.next();
                ClassDescriptor scd = (ClassDescriptor) classes.get(sup);
                
                // Check attributes
                for (Iterator aiter = adescs.iterator(); aiter.hasNext(); ) {
                    AttributeDescriptor ad = (AttributeDescriptor) aiter.next();
                    if (scd.getAttributeDescriptorByName(ad.getName()) != null) {
                        LOG.info("removing attribute " + ad.getName()
                                + " redefinition in " + cd.getName() + " (is now defined in "
                                + scd.getName() + ")");
                        aiter.remove();
                    }
                }
                
                // Check references
                for (Iterator riter = rdescs.iterator(); riter.hasNext(); ) {
                    ReferenceDescriptor rd = (ReferenceDescriptor) riter.next();
                    if (scd.getReferenceDescriptorByName(rd.getName()) != null) {
                        LOG.info("removing reference " + rd.getName()
                                + " redefinition in " + cd.getName() + " (is now defined in "
                                + scd.getName() + ")");
                        riter.remove();
                    }
                }
                
                // Check collections
                for (Iterator citer = cdescs.iterator(); citer.hasNext(); ) {
                    CollectionDescriptor cold = (CollectionDescriptor) citer.next();
                    if (scd.getCollectionDescriptorByName(cold.getName()) != null) {
                        LOG.info("removing collection " + cold.getName()
                                + " redefinition in " + cd.getName() + " (is now defined in "
                                + scd.getName() + ")");
                        cdescs.remove(cold);
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
    
    private static String toSupersString(Set supers) {
        String supersStr = StringUtil.join(supers, " ");
        if (supersStr != null && supersStr.equals("")) {
            supersStr = null;
        }
        return supersStr;
    }
    
    private static void findAllSuperclasses(ClassDescriptor cd, Map classes, Set names)
        throws ModelMergerException {
        Set supers = cd.getSuperclassNames();
        names.addAll(supers);
        for (Iterator iter = supers.iterator(); iter.hasNext();) {
            String superClassName = (String) iter.next();
            ClassDescriptor cld = (ClassDescriptor) classes.get(superClassName);

            if (cld == null) {
                throw new ModelMergerException("cannot find superclass \"" + superClassName
                                               + "\" of " + cd + " in the model");
            }

            findAllSuperclasses(cld, classes, names);
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
                                             Model originalModel, Set mergeClasses)
            throws ModelMergerException {
        if (merge.isInterface() != original.isInterface()) {
            throw new ModelMergerException(original.getName() + ".isInterface/"
                    + original.isInterface() + " != " + merge.getName() + ".isInterface/"
                    + merge.isInterface());
        }
        Set attrs = mergeAttributes(original, merge);
        Set cols = mergeCollections(original, merge);
        Set refs = mergeReferences(original, merge);
        
        Set supers = new TreeSet();
        boolean replacingSuperclass = false;
        // Figure out if we're replacing the superclass
        if (original.getSuperclassDescriptor() != null) {
            Set superNames = merge.getSuperclassNames();
            for (Iterator iter = superNames.iterator(); iter.hasNext(); ) {
                String clsName = (String) iter.next();
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
        if (supersStr != null && supersStr.equals("")) {
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
    public static Set mergeAttributes(ClassDescriptor original, ClassDescriptor merge)
            throws ModelMergerException {
        Iterator iter = merge.getAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            AttributeDescriptor merg = (AttributeDescriptor) iter.next();
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
        
        Set newSet = new HashSet();
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
    public static Set mergeCollections(ClassDescriptor original, ClassDescriptor merge)
            throws ModelMergerException {
        Set newSet = new HashSet();
        newSet.addAll(cloneCollectionDescriptors(original.getCollectionDescriptors()));
        Iterator iter = merge.getCollectionDescriptors().iterator();
        
        while (iter.hasNext()) {
            CollectionDescriptor merg = (CollectionDescriptor) iter.next();
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
    public static Set mergeReferences(ClassDescriptor original, ClassDescriptor merge)
            throws ModelMergerException {
        Set newSet = new HashSet();
        newSet.addAll(cloneReferenceDescriptors(original.getReferenceDescriptors()));
        Iterator iter = merge.getReferenceDescriptors().iterator();
        
        while (iter.hasNext()) {
            ReferenceDescriptor merg = (ReferenceDescriptor) iter.next();
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
    protected static Set cloneReferenceDescriptors(Set refs) {
        Set copy = new HashSet();
        for (Iterator iter = refs.iterator(); iter.hasNext(); ) {
            ReferenceDescriptor ref = (ReferenceDescriptor) iter.next();
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
    protected static Set cloneCollectionDescriptors(Set refs) {
        Set copy = new LinkedHashSet();
        for (Iterator iter = refs.iterator(); iter.hasNext(); ) {
            CollectionDescriptor ref = (CollectionDescriptor) iter.next();
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
    protected static void removeFieldDescriptor(Set fields, String name) {
        for (Iterator iter = fields.iterator(); iter.hasNext();) {
            FieldDescriptor desc = (FieldDescriptor) iter.next();
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
    protected static Set cloneAttributeDescriptors(Set refs) {
        Set copy = new HashSet();
        for (Iterator iter = refs.iterator(); iter.hasNext(); ) {
            AttributeDescriptor ref = (AttributeDescriptor) iter.next();
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
        if (supers != null && supers.equals("")) {
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
    private static ClassDescriptor descriptorByName(Set clds, String name) {
        Iterator iter = clds.iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            if (cld.getName().equals(name)) {
                return cld;
            }
        }
        return null;
    }
}
