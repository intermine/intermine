package org.flymine.metadata;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

import org.flymine.modelproduction.ModelParser;
import org.flymine.modelproduction.xml.FlyMineModelParser;

/**
 * Represents a named business model, makes available metadata for each class
 * within model.
 *
 * @author Richard Smith
 */

public class Model
{
    private static Map models = new HashMap();

    private final String name;
    private final Map cldMap = new LinkedHashMap();
    private final Map subclassMap = new LinkedHashMap();
    private final Map implementorsMap = new LinkedHashMap();

    /**
     * Return a Model for specified model name (loading Model if necessary)
     * @param name the name of the model
     * @return the relevant metadata
     * @throws MetaDataException if there is problem parsing the model xml
     */
    public static Model getInstanceByName(String name) throws MetaDataException {
        if (!models.containsKey(name)) {
            Model model = null;
            String filename = name + "_model.xml";
            InputStream is = Model.class.getClassLoader().getResourceAsStream(filename);
            if (is == null) {
                throw new IllegalArgumentException("Model '" + name + "' cannot be found ("
                                                   + filename + ")");
            }
            try {
                ModelParser parser = new FlyMineModelParser();
                model = parser.process(new InputStreamReader(is));
            } catch (Exception e) {
                throw new MetaDataException("Error parsing metadata: " + e);
            }
            models.put(name, model);
        }
        return (Model) models.get(name);
    }

    /**
     * Construct a Model with a name and set of ClassDescriptors.  The model will be
     * set to this in each of the ClassDescriptors. NB This method should only be called
     * by members of the modelproduction package, eventually it may be replaced with
     * a static addModel method linked to getInstanceByName
     * @param name name of model
     * @param clds a Set of ClassDescriptors in the model
     * @throws MetaDataException if inconsistencies found in model
     */
    public Model(String name, Set clds) throws MetaDataException {
        if (name == null) {
            throw new NullPointerException("Model name cannot be null");
        }
        if (name.equals("")) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        if (clds == null) {
            throw new NullPointerException("Model ClassDescriptors list cannot be null");
        }

        this.name = name;  // check for valid package name??
        LinkedHashSet orderedClds = new LinkedHashSet(clds);
        Iterator cldIter = orderedClds.iterator();

        // 1. Put all ClassDescriptors in model.
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cldMap.put(cld.getName(), cld);

            // create maps of ClassDescriptor to empty sets for subclasses and implementors
            subclassMap.put(cld, new LinkedHashSet());
            implementorsMap.put(cld, new LinkedHashSet());
        }

        // 2. Now set model in each ClassDescriptor, this sets up superclass, interface,
        //    etc descriptors.  Set ClassDescriptors and reverse refs in ReferenceDescriptors.
        cldIter = orderedClds.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            cld.setModel(this);

            // add this to set of subclasses if a superclass exists
            ClassDescriptor superCld = cld.getSuperclassDescriptor();
            if (superCld != null) {
                Set sub = (Set) subclassMap.get(superCld);
                sub.add(cld);
            }

            // add this class to implementors sets for any interfaces
            Set interfaces = cld.getInterfaceDescriptors();
            if (interfaces.size() > 0) {
                Iterator iter = interfaces.iterator();
                while (iter.hasNext()) {
                    ClassDescriptor iCld = (ClassDescriptor) iter.next();
                    Set implementors = (Set) implementorsMap.get(iCld);
                    implementors.add(cld);
                }
            }

        }
    }
    
    /**
     * Get the ClassDescriptors for the subclasses of a class
     * @param cld the parent ClassDescriptor
     * @return the ClassDescriptors of its children
     */
    public Set getSubclasses(ClassDescriptor cld) {
        return (Set) subclassMap.get(cld);
    }

    /**
     * Get the ClassDescriptors for the classes that implement an interface
     * @param cld the ClassDescriptor of the interface
     * @return the ClassDescriptors of its implementors
     */
    public Set getImplementors(ClassDescriptor cld) {
        if (!cld.isInterface()) {
            throw new IllegalArgumentException("getImplementors is only valid for an interface");
        }
        return (Set) implementorsMap.get(cld);
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
     * @return a set of all ClassDescriptors in the model
     */
    public Set getClassDescriptors() {
        return new LinkedHashSet(cldMap.values());
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
    public String getName() {
        return this.name;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof Model) {
            Model model = (Model) obj;
            return name.equals(model.name)
                && cldMap.equals(model.cldMap);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * name.hashCode()
            + 5 * cldMap.hashCode();
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
