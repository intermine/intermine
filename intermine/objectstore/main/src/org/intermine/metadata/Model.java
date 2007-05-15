package org.intermine.metadata;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.net.URI;
import java.net.URISyntaxException;

import org.intermine.model.InterMineObject;
import org.intermine.modelproduction.MetadataManager;
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
    private static Map<String, Model> models = new HashMap<String, Model>();

    private final String modelName;
    private final URI nameSpace;
    private final Map<String, ClassDescriptor> cldMap = new LinkedHashMap<String,
            ClassDescriptor>();
    private final Map<ClassDescriptor, Set<ClassDescriptor>> subMap
        = new LinkedHashMap<ClassDescriptor, Set<ClassDescriptor>>();
    private final Map<Class, Set<ClassDescriptor>> classToClassDescriptorSet
        = new HashMap<Class, Set<ClassDescriptor>>();
    private final Map<Class, Map<String, FieldDescriptor>> classToFieldDescriptorMap
        = new HashMap<Class, Map<String, FieldDescriptor>>();

    /**
     * Return a Model for specified model name (loading Model if necessary)
     * @param name the name of the model
     * @return the relevant metadata
     */
    public static Model getInstanceByName(String name) {
        if (!models.containsKey(name)) {
            models.put(name, MetadataManager.loadModel(name));
        }
        return models.get(name);
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
    public Model(String name, String nameSpace, Set<ClassDescriptor> clds) throws MetaDataException,
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
        LinkedHashSet<ClassDescriptor> orderedClds = new LinkedHashSet<ClassDescriptor>(clds);

        Set<ReferenceDescriptor> emptyRefs = Collections.emptySet();
        Set<CollectionDescriptor> emptyCols = Collections.emptySet();
        ClassDescriptor intermineObject = new ClassDescriptor(
                "org.intermine.model.InterMineObject", null, true,
                Collections.singleton(new AttributeDescriptor("id", "java.lang.Integer")),
                emptyRefs, emptyCols);
        orderedClds.add(intermineObject);

        // 1. Put all ClassDescriptors in model.
        for (ClassDescriptor cld : orderedClds) {
            cldMap.put(cld.getName(), cld);

            // create maps of ClassDescriptor to empty sets for subclasses and implementors
            subMap.put(cld, new LinkedHashSet<ClassDescriptor>());
        }

        // 2. Now set model in each ClassDescriptor, this sets up superDescriptors
        //    etc.  Set ClassDescriptors and reverse refs in ReferenceDescriptors.
        for (ClassDescriptor cld : orderedClds) {
            cld.setModel(this);
        }
 
        for (ClassDescriptor cld : orderedClds) {
            // add this class to subMap sets for any interfaces and superclasses
            Set<ClassDescriptor> supers = cld.getSuperDescriptors();
            for (ClassDescriptor iCld : supers) {
                Set<ClassDescriptor> subs = subMap.get(iCld);
                subs.add(cld);
            }
        }

        // 3. Now run setAllFieldDescriptors on everything
        for (ClassDescriptor cld : orderedClds) {
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
    public Set<ClassDescriptor> getDirectSubs(ClassDescriptor cld) {
        return subMap.get(cld);
    }

    /**
     * Get the ClassDescriptors for the all subclasses of a class
     * @param cld the parent ClassDescriptor
     * @return the ClassDescriptors of all decedents
     */
    public Set<ClassDescriptor> getAllSubs(ClassDescriptor cld) {
        Set<ClassDescriptor> returnSubs = new TreeSet<ClassDescriptor>(); 
        Set<ClassDescriptor> directSubs = getDirectSubs(cld);
        returnSubs.addAll(directSubs);
        for (ClassDescriptor sub : directSubs) {
            returnSubs.addAll(getAllSubs(sub));
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
    public Set<ClassDescriptor> getClassDescriptors() {
        return new LinkedHashSet<ClassDescriptor>(cldMap.values());
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
     * Get a Set of fully qualified class names in this model (i.e. including
     * package name).
     * @return Set of fully qualified class names
     */
    public Set<String> getClassNames() {
        return cldMap.keySet();
    }

    /**
     * Get the name of this model - i.e. package name.
     * @return name of the model
     */
    public String getName() {
        return modelName;
    }

    /**
     * Get the nameSpace URI of this model
     * @return nameSpace URI of the model
     */
    public URI getNameSpace() {
        return nameSpace;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public int hashCode() {
        return 3 * modelName.hashCode()
            + 5 * cldMap.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<model name=\"" + modelName + "\" namespace=\"" + nameSpace + "\">");
        for (ClassDescriptor cld : getClassDescriptors()) {
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
    public Set<ClassDescriptor> getClassDescriptorsForClass(Class c) {
        if (!InterMineObject.class.isAssignableFrom(c)) {
            return Collections.emptySet();
        }
        synchronized (classToClassDescriptorSet) {
            Set<ClassDescriptor> retval = classToClassDescriptorSet.get(c);
            if (retval == null) {
                retval = new LinkedHashSet<ClassDescriptor>();
                Stack<Class> todo = new Stack<Class>();
                Set<Class> done = new HashSet<Class>();
                todo.push(c);
                while (!todo.empty()) {
                    Class toAdd = todo.pop();
                    if (!done.contains(toAdd)) {
                        ClassDescriptor cld = getClassDescriptorByName(toAdd.getName());
                        if (cld != null) {
                            retval.add(cld);
                        }
                        Class superClass = toAdd.getSuperclass();
                        if ((superClass != null)
                                && (InterMineObject.class.isAssignableFrom(superClass))) {
                            todo.push(superClass);
                        }
                        Class[] interfaces = toAdd.getInterfaces();
                        for (int i = 0; i < interfaces.length; i++) {
                            if (InterMineObject.class.isAssignableFrom(interfaces[i])) {
                                todo.push(interfaces[i]);
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
    public Map<String, FieldDescriptor> getFieldDescriptorsForClass(Class c) {
        if (!InterMineObject.class.isAssignableFrom(c)) {
            return Collections.emptyMap();
        }
        synchronized (classToFieldDescriptorMap) {
            Map<String, FieldDescriptor> retval = classToFieldDescriptorMap.get(c);
            if (retval == null) {
                retval = new HashMap<String, FieldDescriptor>();
                for (ClassDescriptor cld : getClassDescriptorsForClass(c)) {
                    for (FieldDescriptor fd : cld.getFieldDescriptors()) {
                        retval.put(fd.getName(), fd);
                    }
                }
                classToFieldDescriptorMap.put(c, retval);
            }
            return retval;
        }
    }
}
