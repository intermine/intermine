/**
 * 
 */
package org.intermine.task.project;

public class SourceProperty
{
    String name, value, location;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public boolean isLocation() {
        return (location != null);
    }
}