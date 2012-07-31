package org.intermine.modelviewer.swing.referencetable;

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

import org.intermine.modelviewer.model.ForeignKey;
import org.intermine.modelviewer.model.ModelClass;

/**
 * Class wrapping information about a model class reference or collection to aid its
 * display in the attribute JTable.
 */
public class ReferenceInfo implements Serializable
{
    private static final long serialVersionUID = -1027532204528154729L;

    /**
     * The class this reference is from.
     * @serial
     */
    ModelClass sourceClass;
    
    /**
     * The reference itself.
     * @serial
     */
    ForeignKey reference;
    
    /**
     * Whether this reference is from a superclass of the class selected
     * for display.
     * @serial
     */
    boolean derived;
    
    /**
     * Whether this reference is a collection (<code>true</code>) or a
     * reference (<code>false</code>) in the model class.
     * @serial
     */
    boolean collection;
    
    
    /**
     * Initialise a new ReferenceInfo wrapper.
     * 
     * @param ref The reference object.
     * @param sourceClass The class this reference is from.
     * @param derived Whether this reference is from a superclass of the class selected
     * for display.
     * @param collection Whether this reference is a collection (<code>true</code>) or a
     * reference (<code>false</code>) in the model class.
     */
    public ReferenceInfo(ForeignKey ref, ModelClass sourceClass,
                         boolean derived, boolean collection) {
        this.reference = ref;
        this.sourceClass = sourceClass;
        this.derived = derived;
        this.collection = collection;
    }

    /**
     * Get the reference.
     * @return The ForeignKey object.
     */
    public ForeignKey getReference() {
        return reference;
    }

    /**
     * Get the flag indicating this reference is a collection.
     * @return <code>true</code> if this reference is a collection,
     * <code>false</code> if it is a reference.
     */
    public boolean isCollection() {
        return collection;
    }

    /**
     * Get the model class.
     * @return The ModelClass object.
     */
    public ModelClass getSourceClass() {
        return sourceClass;
    }

    /**
     * Get the derived flag.
     * @return The derived flag.
     */
    public boolean isDerived() {
        return derived;
    }
}
