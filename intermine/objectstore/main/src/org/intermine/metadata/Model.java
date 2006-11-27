package org.intermine.metadata;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.net.URI;
import java.net.URISyntaxException;

import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;
import org.intermine.util.XmlUtil;

/**
 * Represents a named business model, makes available metadata for each class
 * within model.
 *
 * @author Richard Smith
 */

public class Model
{
    private static Map models = new HashMap();

    private final String modelName;
    private final URI nameSpace;
    private final Map cldMap = new LinkedHashMap();
    private final Map subMap = new LinkedHashMap();
    private final Map classToClassDescriptorSet = new HashMap();
    private final Map classToFieldDescriptorMap = new HashMap();

    /**
     * Return a Model for specified model name (loading Model if necessary)
     * @param name the name of the model
     * @return the relevant metadata
     */
    public static Model getInstanceByName(String name) {
        if (!models.containsKey(name)) {
            models.put(name, MetadataManager.loadModel(name));
        }
        return (Model) models.get(name);
    }

    /**
     * Construct a Model with a name and set of ClassDescriptors.  The model will be
     * set to this in each of the ClassDescriptors. NB This method should only be called
     * by members of the modelproduction package, eventually it may be replaced with
     * a static addModel method linked to getInstanceByName.
     * @param name name of model
     * @param nameSpace the nameSpace uri for this model
     * @param clds a Set of ClassDescriptors in the model
     * @throws MetaDataException if inconsistencies found in model
     * @throws URISyntaxException if nameSpace string is invalid
     */
    public Model(String name, String nameSpace, Set clds) throws MetaDataException,
                                                                 URISyntaxException  {
        if (name == null) {
            throw new NullPointerException("Model name cannot be null");
        }
        if (name.equals("")) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        if (nameSpace == null) {
            throw new NullPointerException("Model nameSpace cannot be null");
        }
        if (nameSpace.equals("")) {
            throw new IllegalArgumentException("Model nameSpace cannot be empty");
        }
        if (clds == null) {
            throw new NullPointerException("Model ClassDescriptors list cannot be null");
        }

        this.modelName = name;

        this.nameSpace = new URI(XmlUtil.correctNamespace(nameSpace));
        LinkedHashSet orderedClds = new LinkedHashSet(clds);

        ClassDescriptor intermineObject = new ClassDescriptor(
                "org.intermine.model.InterMineObject", null, true,
                Collections.singleton(new AttributeDescriptor("id", "java.lang.Integer")),
                Collections.EMPTY_SET, Collections.EMPTY_SET);
        orderedClds.add(intermineObject);

        Iterator cldIter = orderedClds.iterator();
        // 1. Put all ClassDescriptors in model.
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cldMap.put(cld.getName(), cld);

            // create maps of ClassDescriptor to empty sets for subclasses and implementors
            subMap.put(cld, new LinkedHashSet());
        }

        // 2. Now set model in each ClassDescriptor, this sets up superDescriptors
        //    etc.  Set ClassDescriptors and reverse refs in ReferenceDescriptors.
        cldIter = orderedClds.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cld.setModel(this);

            // add this class to subMap sets for any interfaces and superclasses
            Set supers = cld.getSuperDescriptors();
            Iterator iter = supers.iterator();
            while (iter.hasNext()) {
                ClassDescriptor iCld = (ClassDescriptor) iter.next();
                Set subs = (Set) subMap.get(iCld);
                subs.add(cld);
            }
        }

        // 3. Now run setAllFieldDescriptors on everything
        cldIter = orderedClds.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cld.setAllFieldDescriptors();
        }
    }

    /**
     * Return name of the model's package.
     * @return package name
     */
    public String getPackageName() {
        return TypeUtil.packageName(((ClassDescriptor) cldMap.values().iterator().next())
                                    .getName());
    }

    /**
     * Get the ClassDescriptors for the direct subclasses of a class
     * @param cld the parent ClassDescriptor
     * @return the ClassDescriptors of its children
     */
    public Set getDirectSubs(ClassDescriptor cld) {
        return (Set) subMap.get(cld);
    }

    /**
     * Get the ClassDescriptors for the all subclasses of a class
     * @param cld the parent ClassDescriptor
     * @return the ClassDescriptors of all decedents
     */
    public Set getAllSubs(ClassDescriptor cld) {
        Set returnSubs = new HashSet(); 
        Set directSubs = getDirectSubs(cld);
        if (directSubs != null) {
            returnSubs.addAll(directSubs);
        }
        Iterator directSubsIterator = directSubs.iterator();
        while (directSubsIterator.hasNext()) {
            returnSubs.addAll(getAllSubs((ClassDescriptor) directSubsIterator.next()));
        }

        return returnSubs;
    }

    /**
     * Get a ClassDescriptor by name, null if no ClassDescriptor of given name in Model.
     * @param name fully-qualified class name of ClassDescriptor requested
     * @return the requested ClassDescriptor
     */
    public ClassDescriptor getClassDescriptorByName(String name) {
        return (ClassDescriptor) cldMap.get(name);
    }

    /**
     * Get all ClassDescriptors in this model.
     * @return a set of all ClassDescriptors in the model
     */
    public Set getClassDescriptors() {
        return new LinkedHashSet(cldMap.values());
    }

    /**
     * Return true if named ClassDescriptor is found in the model.
     * @param name named of ClassDescriptor search for
     * @return true if named descriptor found
     */
    public boolean hasClassDescriptor(String name) {
        return cldMap.containsKey(name);
    }

    /**
     * Get a Collection of fully qualified class names in this model (i.e. including
     * package name).
     * @return Collection of fully qualified class names
     */
    public Collection getClassNames() {
        return cldMap.keySet();
    }

    /**
     * Get the name of this model - i.e. package name.
     * @return name of the model
     */
    public String getName() {
        return this.modelName;
    }

    /**
     * Get the nameSpace URI of this model
     * @return nameSpace URI of the model
     */
    public URI getNameSpace() {
        return this.nameSpace;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof Model) {
            Model model = (Model) obj;
            return modelName.equals(model.modelName)
                && nameSpace.equals(model.nameSpace)
                && cldMap.equals(model.cldMap);
        }
        return false;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 3 * modelName.hashCode()
            + 5 * cldMap.hashCode();
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<model name=\"" + modelName + "\" namespace=\"" + nameSpace + "\">");
        for (Iterator iter = getClassDescriptors().iterator(); iter.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            if (!"org.intermine.model.InterMineObject".equals(cld.getName())) {
                sb.append(cld.toString());
            }
        }
        sb.append("</model>");
        return sb.toString();
    }

    /**
     * Takes a Class, and generates a Set of all ClassDescriptors that are the Class
     * or any of its parents. The Class may be a dynamic class - ie not in the model, although
     * at least one of its parents are in the model.
     *
     * @param c a Class
     * @return a Set of ClassDescriptor objects
     */
    public Set getClassDescriptorsForClass(Class c) {
        if (!InterMineObject.class.isAssignableFrom(c)) {
            return Collections.EMPTY_SET;
        }
        synchronized (classToClassDescriptorSet) {
            Set retval = (Set) classToClassDescriptorSet.get(c);
            if (retval == null) {
                retval = new LinkedHashSet();
                Stack stack = new Stack();
                Set done = new HashSet();
                stack.push(c);
                while (!stack.empty()) {
                    Class toAdd = (Class) stack.pop();
                    if (!done.contains(toAdd)) {
                        ClassDescriptor cld = getClassDescriptorByName(toAdd.getName());
                        if (cld != null) {
                            retval.add(cld);
                        }
                        Class superClass = toAdd.getSuperclass();
                        if ((superClass != null)
                                && (InterMineObject.class.isAssignableFrom(superClass))) {
                            stack.push(superClass);
                        }
                        Class[] interfaces = toAdd.getInterfaces();
                        for (int i = 0; i < interfaces.length; i++) {
                            if (InterMineObject.class.isAssignableFrom(interfaces[i])) {
                                stack.push(interfaces[i]);
                            }
                        }
                        done.add(toAdd);
                    }
                }
                classToClassDescriptorSet.put(c, retval);
            }
            return retval;
        }
    }

    /**
     * Takes a Class, and generates a Map of all FieldDescriptors that are the class fields
     * or any of its parents. The Class may be a dynamic class - ie not in the model, although
     * at least one of its parents are in the model.
     *
     * @param c a Class
     * @return a Map of FieldDescriptor objects
     */
    public Map getFieldDescriptorsForClass(Class c) {
        if (!InterMineObject.class.isAssignableFrom(c)) {
            return Collections.EMPTY_MAP;
        }
        synchronized (classToFieldDescriptorMap) {
            Map retval = (Map) classToFieldDescriptorMap.get(c);
            if (retval == null) {
                retval = new HashMap();
                Set clds = getClassDescriptorsForClass(c);
                Iterator cldIter = clds.iterator();
                while (cldIter.hasNext()) {
                    ClassDescriptor cld = (ClassDescriptor) cldIter.next();
                    Iterator fieldIter = cld.getFieldDescriptors().iterator();
                    while (fieldIter.hasNext()) {
                        FieldDescriptor fd = (FieldDescriptor) fieldIter.next();
                        retval.put(fd.getName(), fd);
                    }
                }
                classToFieldDescriptorMap.put(c, retval);
            }
            return retval;
        }
    }
}
