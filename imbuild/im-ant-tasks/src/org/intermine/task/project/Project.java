/**
 * 
 */
package org.intermine.task.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing the contents of a project.xml file.
 */
public class Project
{
    String type = null;
    Map sources = new LinkedHashMap();
    List properties = new ArrayList();
    Map postProcesses = new LinkedHashMap();
    
    public void addSource(String name, Source source) {
        sources.put(name, source);
    }        
    
    public void addProperty(SourceProperty property) {
        properties.add(property);
    }
    
    public void addPostProcess(String name, PostProcess postProcess) {
        postProcesses.put(name, postProcess);
    }

    public Map getSources() {
        return sources;
    }
    
    public List getProperties() {
        return properties;
    }

    public Map getPostProcesses() {
        return postProcesses;
    }

    /**
     * Set the type of this project.
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Return the type of this project
     */
    public String getType() {
        return type;
    }

}