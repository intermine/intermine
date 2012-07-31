package org.intermine.modelviewer.swing.attributetable;

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

import org.intermine.modelviewer.model.Attribute;
import org.intermine.modelviewer.model.ModelClass;

/**
 * Class wrapping information about a model class attribute to aid its
 * display in the attribute JTable.
 */
public class AttributeInfo implements Serializable
{
    private static final long serialVersionUID = -1027532204528154729L;

    /**
     * The model attribute object.
     * @serial
     */
    Attribute attribute;
    
    /**
     * The model class the attribute belongs to.
     * @serial
     */
    ModelClass sourceClass;
    
    /**
     * Whether this attribute is from a superclass of the class selected
     * for display.
     * @serial
     */
    boolean derived;
    
    
    /**
     * Initialise a new AttributeInfo wrapper.
     * 
     * @param attribute The model attribute object.
     * @param sourceClass The model class the attribute belongs to.
     * @param derived Whether the attribute is from a superclass of the class selected
     * for display.
     */
    public AttributeInfo(Attribute attribute, ModelClass sourceClass, boolean derived) {
        this.attribute = attribute;
        this.sourceClass = sourceClass;
        this.derived = derived;
    }

    /**
     * Get the attribute.
     * @return The Attribute object.
     */
    public Attribute getAttribute() {
        return attribute;
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
