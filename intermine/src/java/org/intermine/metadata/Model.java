package org.flymine.metadata;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/**
 * Represents a named business model, makes availble metadata for each class
 * within model.
 *
 * @author Richard Smith
 */

public class Model
{

    private final String name;
    private final Map cldMap = new HashMap();
    private final Map subclassMap = new HashMap();
    private final Map implementorsMap = new HashMap();

    /**
     * Construct a Model with a name and list of ClassDescriptors.  The model will be
     * set to this in each of the ClassDescriptors.
     * @param name name of model
     * @param clds a List of ClassDescriptors in the model
     * @throws MetaDataException if inconsistencies found in model
     */
    public Model(String name, List clds) throws MetaDataException {
        if (name == null || name == "") {
            throw new IllegalArgumentException("A name must be supplied for the Model");
        }
        this.name = name;  // check for valid package name??
        Iterator cldIter = clds.iterator();

        // 1. Put all ClassDescriptors in model.
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cldMap.put(cld.getClassName(), cld);

            // create maps of ClassDescriptor to empty lists for subclasses and implementors
            subclassMap.put(cld, new ArrayList());
            implementorsMap.put(cld, new ArrayList());
        }

        // 2. Now set model in each ClassDescriptor, this sets up superclass, interface,
        //    etc descriptors.  Set ClassDescriptors and reverse refs in ReferenceDescriptors.
        cldIter = clds.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cld.setModel(this);

            // add this to list of subclasses if a superclass exists
            ClassDescriptor superCld = cld.getSuperclassDescriptor();
            if (superCld != null) {
                List sub = (List) subclassMap.get(superCld);
                sub.add(cld);
            }

            // add this class to implementors lists for any interfaces
            List interfaces = cld.getInterfaceDescriptors();
            if (interfaces.size() > 0) {
                Iterator iter = interfaces.iterator();
                while (iter.hasNext()) {
                    ClassDescriptor iCld = (ClassDescriptor) iter.next();
                    List implementors = (List) implementorsMap.get(iCld);
                    implementors.add(cld);
                }
            }

        }

        // 3. Finally, set completed lists of subclasses and implementors in
        //    each ClassDescriptor.
        cldIter = clds.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();

            List sub = (List) subclassMap.get(cld);
            if (sub.size() > 0) {
                cld.setSubclassDescriptors(sub);
            }

            if (cld.isInterface()) {
                List implementors = (List) implementorsMap.get(cld);
                if (implementors.size() > 0) {
                    cld.setImplementorDescriptors(implementors);
                }
            }
        }
    }

    /**
     * Get a ClassDescriptor by name, null if no ClassDescriptor of given name in Model.
     * @param name name of ClassDescriptor requested
     * @return the requested ClassDescriptor
     */
    public ClassDescriptor getClassDescriptorByName(String name) {
        if (cldMap.containsKey(name)) {
            return (ClassDescriptor) cldMap.get(name);
        } else {
            return null;
        }
    }

    /**
     * Get all ClassDescriptors in this model.
     * @return a list of all ClassDescriptors in the model
     */
    public List getClassDescriptors() {
        return new ArrayList(cldMap.values());
    }

    /**
     * Return true if named ClassDescriptor is found in the model
     * @param name named of ClassDescriptor search for
     * @return true if named descriptor found
     */
    public boolean hasClassDescriptor(String name) {
        return cldMap.containsKey(name);
    }

    /**
     * Get the name of this model - i.e. package name.
     * @return name of the model
     */
    public String getModelName() {
        return this.name;
    }

    /**
     * Get a Collection of fully qualified class names in this model (i.e. including
     * package name).
     * @return Collection of fully qualified class names
     */
    public Collection getClassNames() {
        return (Collection) cldMap.keySet();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<model name=\"" + name + "\">");
        for (Iterator iter = getClassDescriptors().iterator(); iter.hasNext();) {
            sb.append(iter.next().toString());
        }
        sb.append("</model>");
        return sb.toString();
    }
}
