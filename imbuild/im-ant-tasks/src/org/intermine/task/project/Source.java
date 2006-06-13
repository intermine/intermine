/**
 * 
 */
package org.intermine.task.project;

import java.util.ArrayList;
import java.util.List;



public class Source
{
    List properties = new ArrayList();
    String type;
    
    public void addProperty(SourceProperty property) {
        properties.add(property);
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public List getProperties() {
        return properties;
    }
}