package org.intermine.modelviewer.model;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class represents a class in the Intermine model.
 */
public class ModelClass implements Serializable
{
    private static final long serialVersionUID = 7555239176517412495L;
    
    /**
     * The class name.
     * @serial
     */
    protected String name;
    
    /**
     * The super class to this class, if there is one.
     * @serial
     */
    protected ModelClass superclass;
    
    /**
     * Flag indicating that this class is an interface.
     * @serial
     */
    protected boolean isInterface;
    
    /**
     * The class attributes: a map of attribute name to Attribute object.
     * @serial
     */
    protected Map<String, Attribute> attributes = new HashMap<String, Attribute>();
    
    /**
     * The class references: a map of reference name to ForeignKey object.
     * @serial
     */
    protected Map<String, ForeignKey> references = new HashMap<String, ForeignKey>();
    
    /**
     * The class collections: a map of collection name to ForeignKey object.
     * @serial
     */
    protected Map<String, ForeignKey> collections = new HashMap<String, ForeignKey>();
    
    /**
     * The marker tag for this foreign key's origin. 
     * @serial
     */
    protected String tag;
    

    /**
     * Initialise with a class name but no tag.
     * 
     * @param name The class name.
     */
    public ModelClass(String name) {
        this(name, null);
    }

    /**
     * Initialise with a class name and a marker tag.
     * 
     * @param name The class name.
     * @param tag The marker tag.
     */
    public ModelClass(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }

    /**
     * Get the marker tag for the source of this model class.
     * @return The tag.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get the super class to this model class.
     * 
     * @return The super class, or <code>null</code> if this is a top
     * level class.
     */
    public ModelClass getSuperclass() {
        return superclass;
    }

    /**
     * Set the super class to this model class.
     * 
     * @param superclass The parent class.
     */
    public void setSuperclass(ModelClass superclass) {
        this.superclass = superclass;
    }

    /**
     * Get the class name.
     * @return The class name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the interface flag.
     * @return The interface flag.
     */
    public boolean isInterface() {
        return isInterface;
    }

    /**
     * Set the interface flag.
     * @param isInterface Flag indicating that this model class is an interface.
     */
    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    /**
     * Get the map of attributes.
     * @return A map of attribute name to Attribute object.
     */
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }
    
    /**
     * Add an attribute to this class.
     * 
     * @param name The attribute name.
     * @param a The Attribute object.
     */
    public void addAttribute(String name, Attribute a) {
        attributes.put(name, a);
    }

    /**
     * Get the map of references.
     * @return A map of reference name to ForeignKey object.
     */
    public Map<String, ForeignKey> getReferences() {
        return references;
    }

    /**
     * Add a reference to this class.
     * 
     * @param name The reference name.
     * @param r The ForeignKey object.
     */
    public void addReference(String name, ForeignKey r) {
        references.put(name, r);
    }

    /**
     * Get the map of collections.
     * @return A map of collection name to ForeignKey object.
     */
    public Map<String, ForeignKey> getCollections() {
        return collections;
    }
    
    /**
     * Add a collection to this class.
     * 
     * @param name The collection name.
     * @param c The ForeignKey object.
     */
    public void addCollection(String name, ForeignKey c) {
        collections.put(name, c);
    }

    /**
     * Create a human-readable representation of this model class.
     * @return A printable String.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Class[name=").append(name);
        if (superclass != null) {
            b.append(",extends=").append(superclass.getName());
        }
        b.append(",interface=").append(isInterface);
        if (!attributes.isEmpty()) {
            b.append(",attributes={");
            Iterator<Attribute> iter = attributes.values().iterator();
            while (iter.hasNext()) {
                Attribute a = iter.next();
                b.append(a);
                if (iter.hasNext()) {
                    b.append(',');
                }
            }
            b.append('}');
        }
        if (!references.isEmpty()) {
            b.append(",references={");
            Iterator<ForeignKey> iter = references.values().iterator();
            while (iter.hasNext()) {
                ForeignKey fk = iter.next();
                b.append(fk);
                if (iter.hasNext()) {
                    b.append(',');
                }
            }
            b.append('}');
        }
        if (!collections.isEmpty()) {
            b.append(",collections={");
            Iterator<ForeignKey> iter = collections.values().iterator();
            while (iter.hasNext()) {
                ForeignKey fk = iter.next();
                b.append(fk);
                if (iter.hasNext()) {
                    b.append(',');
                }
            }
            b.append('}');
        }
        if (tag != null) {
            b.append(",tag=").append(tag);
        }
        b.append(']');
        return b.toString();
    }
    
    /**
     * Get the depth (number of super classes) of this class. Top level classes have
     * a depth of zero.
     * 
     * @return The depth of this class in the hierarchy.
     */
    public int getDepth() {
        return superclass == null ? 0 : superclass.getDepth() + 1;
    }
}
