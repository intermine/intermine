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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The outermost class of a model; this class holds the model class objects that make
 * up the model plus some model-wide parameters.
 */
public class Model implements Serializable
{
    private static final long serialVersionUID = -6997604044520379933L;

    /**
     * The model name.
     * @serial
     */
    private String name;
    
    /**
     * The package for generated Java classes.
     * @serial
     */
    private String myPackage;
    
    /**
     * The model class objects, indexed by name.
     * @serial
     */
    private SortedMap<String, ModelClass> classes = new TreeMap<String, ModelClass>();
    
    /**
     * Create a new, empty model.
     */
    public Model() {
    }

    /**
     * Get the model name.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the model name.
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the package for generated Java classes.
     * @return The package path.
     */
    public String getPackage() {
        return myPackage;
    }

    /**
     * Set the package for generated Java classes.
     * @param myPackage The package path.
     */
    public void setPackage(String myPackage) {
        this.myPackage = myPackage;
    }

    /**
     * Get the map of model classes.
     * @return A map of class name to ModelClass object. This map can be
     * manipulated.
     */
    public SortedMap<String, ModelClass> getClasses() {
        return classes;
    }

    /**
     * Create a human-readable representation of this model.
     * @return A printable String.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Model[name=").append(name);
        b.append(",package=").append(myPackage);
        
        b.append(",classes={");
        for (ModelClass mc : classes.values()) {
            b.append(mc);
        }
        b.append("}]");

        return b.toString();
    }
}
