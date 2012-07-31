package org.intermine.install.swing.source;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.intermine.install.project.source.PropertyDescriptor;
import org.intermine.install.project.source.PropertyType;


/**
 * A wrapper around a property descriptor and components to edit that
 * property.
 */
public class PropertyComponentWrapper
{
    /**
     * The name of the property.
     */
    private String propertyName;
    
    /**
     * The descriptor for the property.
     */
    private PropertyDescriptor descriptor;
    
    /**
     * The check box component if the property is of BOOLEAN type.
     */
    private JCheckBox checkBox;
    
    /**
     * The text field for editing the value for all fields that are
     * not BOOLEAN.
     */
    private JTextField textField;
    
    /**
     * The component to add to the source panel.
     */
    private JComponent displayComponent;
    
    
    /**
     * Initialise for a boolean property with a check box.
     * <p>The check box becomes the display component.</code>
     * 
     * @param name The name of the property.
     * @param descriptor The property's descriptor.
     * @param check The JCheckBox component.
     */
    public PropertyComponentWrapper(String name, PropertyDescriptor descriptor, JCheckBox check) {
        propertyName = name;
        this.descriptor = descriptor;
        checkBox = check;
        displayComponent = check;
    }
    
    /**
     * Initialise for a non-boolean property with a text box.
     * <p>The text box becomes the display component.</code>
     * 
     * @param name The name of the property.
     * @param descriptor The property's descriptor.
     * @param text The JTextField.
     */
    public PropertyComponentWrapper(String name, PropertyDescriptor descriptor, JTextField text) {
        this(name, descriptor, text, text);
    }
    
    /**
     * Initialise for a non-boolean property with a text box and a specific
     * display component.
     * 
     * @param name The name of the property.
     * @param descriptor The property's descriptor.
     * @param text The JTextField.
     * @param display The display component.
     */
    public PropertyComponentWrapper(String name, PropertyDescriptor descriptor, JTextField text,
                                    JComponent display) {
        propertyName = name;
        this.descriptor = descriptor;
        textField = text;
        displayComponent = display;
    }
    
    /**
     * Get the name of the property.
     * 
     * @return The property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Get the descriptor for the property.
     * 
     * @return The property descriptor.
     */
    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }
    
    /**
     * Get the field type for the property.
     * 
     * @return The field type.
     */
    public PropertyType getFieldType() {
        return descriptor.getType();
    }
    
    /**
     * Check whether the field has a value set.
     * 
     * @return <code>false</code> if the field is required and has no value set,
     * <code>true</code> otherwise.
     */
    public boolean isSet() {
        switch (descriptor.getType()) {
            case BOOLEAN:
                return true;
                
            default:
                return !descriptor.isRequired() || StringUtils.isNotEmpty(textField.getText());
        }
    }

    /**
     * Get the value of the editing field.
     * 
     * @return A String value from the field.
     */
    public String getValue() {
        switch (descriptor.getType()) {
            case BOOLEAN:
                return Boolean.toString(checkBox.isSelected());
                
            default:
                return textField.getText();
        }
    }
    
    /**
     * Get the component that holds the property's value.
     * 
     * @return The editing component.
     */
    public JComponent getValueComponent() {
        switch (descriptor.getType()) {
            case BOOLEAN:
                return checkBox;
                
            default:
                return textField;
        }
    }
    
    /**
     * Get the display component.
     * 
     * @return The display component.
     */
    public JComponent getDisplayComponent() {
        return displayComponent;
    }
}
