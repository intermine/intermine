package org.flymine.metadata;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;


/**
 * Represents a named business model, makes availble metadata for each class
 * within model.
 *
 * @author Richard Smith
 */

public class Model
{

    private final Map cldMap = new HashMap();
    private String name;

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

        // first put all ClassDescriptors in model
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            //cld.setModel(this);
            cldMap.put(cld.getClassName(), cld);
        }

        // now set up relationships for each
        cldIter = clds.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cld.setModel(this);
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
        return (List) cldMap.values();
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
     * Get a Collection of class names in the model (without full package).
     * @return Collection of class names
     */
    public Collection getNames() {
        return (Collection) cldMap.keySet();
    }

    /**
     * Get a Collection of fully qualified class names in this model (i.e. including
     * package name.
     * @return Collection of fully qualified class names
     */
    public Collection getFullNames() {
        Set names = cldMap.keySet();
        Set fullNames = new HashSet();
        Iterator namesIter = names.iterator();
        while (namesIter.hasNext()) {
            fullNames.add(name + (String) namesIter.next());
        }
        return (Collection) fullNames;
    }
}
