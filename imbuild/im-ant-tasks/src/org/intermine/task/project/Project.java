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
    
    /**
     * Add a Source object
     * @param name the name
     * @param source the Source
     */
    public void addSource(String name, Source source) {
        sources.put(name, source);
    }        
    
    /**
     * Add a project property.
     * @param property the property
     */
    public void addProperty(UserProperty property) {
        properties.add(property);
    }
    
    /**
     * Add a post-processing step
     * @param name the name
     * @param postProcess the PostProcess
     */
    public void addPostProcess(String name, PostProcess postProcess) {
        postProcesses.put(name, postProcess);
    }

    /**
     * Return a Map from source name to Source
     * @return the Sources
     */
    public Map getSources() {
        return sources;
    }
    
    /**
     * Return a list of UserProperty objects for the Project.
     * @return the properties
     */
    public List getProperties() {
        return properties;
    }

    /**
     * Return a Map from post-process name to PostProcess objects
     * @return the PostProcess objects
     */
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
     * @return the type
     */
    public String getType() {
        return type;
    }

}