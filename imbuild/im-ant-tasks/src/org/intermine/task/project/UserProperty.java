package org.intermine.task.project;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Bean to hold property elements from project.xml files.
 * @author Kim Rutherford
 */
public class UserProperty
{
    String name, value, location;
    
    /**
     * Set the name
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Return the name
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the property value
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Get the location
     * @return the location
     */
    public String getLocation() {
        return location;
    }
    
    /**
     * Set the property value
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Set the property location
     * @param location the new location
     */
    public void setLocation(String location) {
        this.location = location;
    }
    
    /**
     * Return true if and only if the location is set
     * @return true if this property has a location
     */
    public boolean isLocation() {
        return (location != null);
    }
}