package org.intermine.metadata;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.intermine.model.InterMineObject;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.util.TypeUtil;

/**
 * Represents a named business model, makes available metadata for each class
 * within model.
 *
 * @author Richard Smith
 */
public class Model
{
    private static Map<String, Model> models = new HashMap<String, Model>();
    protected static final String ENDL = System.getProperty("line.separator");
    private final String modelName;
    private final String packageName;
    private final Map<String, ClassDescriptor> cldMap = new LinkedHashMap<String,
            ClassDescriptor>();
    private final Map<ClassDescriptor, Set<ClassDescriptor>> subMap
        = new LinkedHashMap<ClassDescriptor, Set<ClassDescriptor>>();
    private final Map<Class<?>, Set<ClassDescriptor>> classToClassDescriptorSet
        = new HashMap<Class<?>, Set<ClassDescriptor>>();
    private final Map<Class<?>, Map<String, FieldDescriptor>> classToFieldDescriptorMap
        = new HashMap<Class<?>, Map<String, FieldDescriptor>>();
    private final Map<Class<?>, Map<String, Class<?>>> classToCollectionsMap
        = new HashMap<Class<?>, Map<String, Class<?>>>();
    private final ClassDescriptor rootCld;
    private List<ClassDescriptor> topDownOrderClasses = null;
    private List<ClassDescriptor> bottomUpOrderClasses = null;
    private List<String> problems = new ArrayList<String>();

    private boolean generatedClassesAvailable = true;

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
     * Adds model to known models.
     *
     * @param name the model name
     * @param model the model
     */
    public static void addModel(String name, Model model) {
        models.put(name, model);
    }

    /**
     * Construct a Model with a name and set of ClassDescriptors.  The model will be
     * set to this in each of the ClassDescriptors. NB This method should only be called
     * by members of the modelproduction package, eventually it may be replaced with
     * a static addModel method linked to getInstanceByName.
     *
     * @param name name of model
     * @param packageName the package name of the model
     * @param clds a Set of ClassDescriptors in the model
     * @throws MetaDataException if inconsistencies found in model
     */
    public Model(String name, String packageName,
            Set<ClassDescriptor> clds) throws MetaDataException {
        if (name == null) {
            throw new NullPointerException("Model name cannot be null");
        }
        if ("".equals(name)) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        if (packageName == null) {
            throw new NullPointerException("Package name cannot be null");
        }
        if (clds == null) {
            throw new NullPointerException("Model ClassDescriptors list cannot be null");
        }

        this.modelName = name;
        this.packageName = packageName;

        LinkedHashSet<ClassDescriptor> orderedClds = new LinkedHashSet<ClassDescriptor>(clds);

        Set<ReferenceDescriptor> emptyRefs = Collections.emptySet();
        Set<CollectionDescriptor> emptyCols = Collections.emptySet();
        ClassDescriptor intermineObject = new ClassDescriptor(
                "org.intermine.model.InterMineObject", null, true,
                Collections.singleton(new AttributeDescriptor("id", "java.lang.Integer")),
                emptyRefs, emptyCols);
        orderedClds.add(intermineObject);
        rootCld = intermineObject;

        // 1. Put all ClassDescriptors in model.
        for (ClassDescriptor cld : orderedClds) {
            String cldName = cld.getName();
            int lastDotPos = cldName.lastIndexOf(".");
            String cldPackage = (lastDotPos == -1 ? "" : cldName.substring(0, lastDotPos));
            if ((!"org.intermine.model.InterMineObject".equals(cldName))
                    && (!packageName.equals(cldPackage))) {
                throw new IllegalArgumentException("Class \"" + cldName + "\" is not in model "
                        + "package \"" + packageName + "\"");
            }
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
        return packageName;
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
     * @param name unqualified or fully-qualified class name of ClassDescriptor requested
     * @return the requested ClassDescriptor
     */
    public ClassDescriptor getClassDescriptorByName(String name) {
        ClassDescriptor cd = cldMap.get(name);
        if (cd == null) {
            return cldMap.get(getPackageName() + "." + name);
        } else {
            return cd;
        }
    }

    /**
     * Get all ClassDescriptors in this model.
     *
     * @return a set of all ClassDescriptors in the model
     */
    public Set<ClassDescriptor> getClassDescriptors() {

        return new LinkedHashSet<ClassDescriptor>(cldMap.values());
    }

    /**
     * Return true if named ClassDescriptor is found in the model.  Looking for fully qualified
     * classname, eg. org.intermine.model.bio.Gene.
     *
     * @param name named of ClassDescriptor search for
     * @return true if named descriptor found
     */
    public boolean hasClassDescriptor(String name) {
        boolean found = cldMap.containsKey(name);
        if (!found) {
            found = cldMap.containsKey(getPackageName() + "." + name);
        }
        return found;
    }

    /**
     * Get a Set of fully qualified class names in this model (i.e. including
     * package name).
     *
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Model) {
            Model model = (Model) obj;
            return modelName.equals(model.modelName)
                && cldMap.equals(model.cldMap);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 3 * modelName.hashCode()
            + 5 * cldMap.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<model name=\"" + modelName + "\" package=\"" + packageName + "\">" + ENDL);
        for (ClassDescriptor cld : getClassDescriptors()) {
            if (!"org.intermine.model.InterMineObject".equals(cld.getName())) {
                sb.append(cld.toString());
            }
        }
        sb.append("</model>");
        return sb.toString();
    }

    /**
     * Returns the JSON serialisation of the model.
     * @return A JSON formatted string.
     */
    public String toJSONString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\"name\":\"" + modelName + "\",\"classes\":{");
        boolean needsComma = false;
        for (ClassDescriptor cld: getClassDescriptors()) {
            if (!"org.intermine.model.InterMineObject".equals(cld.getName())) {
                if (needsComma) {
                    sb.append(",");
                }
                sb.append(cld.toJSONString());
                needsComma = true;
            }
        }
        sb.append("}");
        return sb.toString();
    }


    /**
     * Used to generate the SO additions file
     * @return the model as an additions file
     */
    public String toAdditionsXML() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\"?>"  + ENDL + "<classes>" + ENDL);
        for (ClassDescriptor cld : getClassDescriptors()) {
            if (!"org.intermine.model.InterMineObject".equals(cld.getName())) {
                sb.append(cld.toString());
            }
        }
        sb.append("</classes>");
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
    public Set<ClassDescriptor> getClassDescriptorsForClass(Class<?> c) {
        synchronized (classToClassDescriptorSet) {
            Set<ClassDescriptor> retval = classToClassDescriptorSet.get(c);
            if (retval == null) {
                retval = new LinkedHashSet<ClassDescriptor>();
                Stack<Class<?>> todo = new Stack<Class<?>>();
                Set<Class<?>> done = new HashSet<Class<?>>();
                todo.push(c);
                while (!todo.empty()) {
                    Class<?> toAdd = todo.pop();
                    if (!done.contains(toAdd)) {
                        ClassDescriptor cld = getClassDescriptorByName(toAdd.getName());
                        if (cld != null) {
                            retval.add(cld);
                        }
                        Class<?> superClass = toAdd.getSuperclass();
                        if ((superClass != null)
                                && (InterMineObject.class.isAssignableFrom(superClass))) {
                            todo.push(superClass);
                        }
                        Class<?>[] interfaces = toAdd.getInterfaces();
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
    public Map<String, FieldDescriptor> getFieldDescriptorsForClass(Class<?> c) {
        synchronized (classToFieldDescriptorMap) {
            Map<String, FieldDescriptor> retval = classToFieldDescriptorMap.get(c);
            if (retval == null) {
                retval = new LinkedHashMap<String, FieldDescriptor>();
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

    /**
     * Takes a Class, and generates a Map of all the collections that are in the Class or any of its
     * parents. The Class may be a dynamic class - ie not in the model, although at least one of its
     * parents are in the model.
     *
     * @param c a Class
     * @return a Map from String collection name to Class element type
     */
    public Map<String, Class<?>> getCollectionsForClass(Class<?> c) {
        synchronized (classToCollectionsMap) {
            Map<String, Class<?>> retval = classToCollectionsMap.get(c);
            if (retval == null) {
                retval = new LinkedHashMap<String, Class<?>>();
                for (FieldDescriptor fd : getFieldDescriptorsForClass(c).values()) {
                    if (fd instanceof CollectionDescriptor) {
                        CollectionDescriptor cd = (CollectionDescriptor) fd;
                        retval.put(cd.getName(), cd.getReferencedClassDescriptor().getType());
                    }
                }
                classToCollectionsMap.put(c, retval);
            }
            return retval;
        }
    }

    /**
     * Return the qualified name of the given unqualified class name.  The className must be in the
     * given model or in the java.lang package or one of java.util.Date, java.math.BigDecimal,
     * or org.intermine.objectstore.query.ClobAccess.
     *
     * @param className the name of the class
     * @return the fully qualified name of the class
     * @throws ClassNotFoundException if the class can't be found
     */
    public String getQualifiedTypeName(String className)
        throws ClassNotFoundException {

        if (className.indexOf(".") != -1) {
            throw new IllegalArgumentException("Expected an unqualified class name: " + className);
        }

        if (TypeUtil.instantiate(className) != null) {
            // a primitive type
            return className;
        } else {
            if ("InterMineObject".equals(className)) {
                return "org.intermine.model.InterMineObject";
            } else {
                try {
                    return Class.forName(getPackageName() + "." + className).getName();
                } catch (ClassNotFoundException e) {
                    // fall through and try java.lang
                }
            }

            if ("Date".equals(className)) {
                return Date.class.getName();
            }

            if ("BigDecimal".equals(className)) {
                return BigDecimal.class.getName();
            }

            if ("ClobAccess".equals(className)) {
                return ClobAccess.class.getName();
            }

            return Class.forName("java.lang." + className).getName();
        }
    }

    /**
     * Return the classes in the model in level order from shallowest to deepest, the order of nodes
     * at any given level is undefined.  The list does not include InterMineObject.
     * @return ClassDescriptors from the model in depth order
     */
    public synchronized List<ClassDescriptor> getTopDownLevelTraversal() {
        if (topDownOrderClasses == null) {
            topDownOrderClasses = new ArrayList<ClassDescriptor>();

            // start from InterMineObject which is the root
            LinkedList<ClassDescriptor> queue = new LinkedList<ClassDescriptor>();
            // Simple objects don't have any inheritance so can go at the front
            queue.addAll(getSimpleObjectClassDescriptors());
            queue.add(rootCld);
            while (!queue.isEmpty()) {
                ClassDescriptor node = queue.remove();
                if (!topDownOrderClasses.contains(node)) {
                    topDownOrderClasses.add(node);
                }
                // add direct subclasses to the queue
                if (node.getSubDescriptors() != null) {
                    queue.addAll(node.getSubDescriptors());
                }
            }
        }
        return topDownOrderClasses;
    }


    /**
     * Return the classes in the model in level order from deepest to shallowest, the order of nodes
     * at any given level is undefined.  The list does not include InterMineObject.
     * @return ClassDescriptors from the model in reverse depth order
     */
    public synchronized List<ClassDescriptor> getBottomUpLevelTraversal() {
        if (bottomUpOrderClasses == null) {
            bottomUpOrderClasses = new ArrayList<ClassDescriptor>();

            List<ClassDescriptor> topDown = getTopDownLevelTraversal();

            // Just reverse the top down traversal
            for (int i = topDown.size() - 1; i >= 0; i--) {
                bottomUpOrderClasses.add(topDown.get(i));
            }
        }
        return bottomUpOrderClasses;
    }

    /**
     * Return ClassDescriptors for simple objects only - simple objects are light weight objects
     * without an id and with no inheritance.  They can't be interfaces and inherit directly from
     * java.lang.Object.
     * @return a set of ClassDescriptors for all simple objects in the model
     */
    protected Set<ClassDescriptor> getSimpleObjectClassDescriptors() {
        Set<ClassDescriptor> simpleObjectClds = new HashSet<ClassDescriptor>();
        for (ClassDescriptor cld : getClassDescriptors()) {
            Set<String> superNames = cld.getSuperclassNames();
            if (superNames.size() == 1 && superNames.contains("java.lang.Object")) {
                simpleObjectClds.add(cld);
            }
        }
        return simpleObjectClds;
    }


    /**
     * @return true if generated classes are available
     *
     */
    public boolean isGeneratedClassesAvailable() {
        return generatedClassesAvailable;
    }

    /**
     * Sets if generated classes are available.
     * @param available if generated class is available
     */
    public void setGeneratedClassesAvailable(boolean available) {
        this.generatedClassesAvailable = available;
    }

    /**
     * @param className class name
     * @return true if class is defined else false
     */
    public boolean isGeneratedClassAvailable(String className) {
        try {
            getQualifiedTypeName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Add a problem to the model that doesn't prevent it from being created for backwards
     * compatibility but should be checked when creating a new model.
     * @param problem a description of the problem
     */
    protected void addProblem(String problem) {
        problems.add(problem);
    }

    /**
     * Return a list of problems with the model or an empty list.
     * @return descriptions of problems in the model or an empty list.
     */
    public List<String> getProblems() {
        return problems;
    }

    /**
     * Return true if there are problems with the model despite it's successful creation.
     * @return true if there are problems
     */
    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}
