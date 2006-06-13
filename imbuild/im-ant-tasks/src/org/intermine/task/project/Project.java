/**
 * 
 */
package org.intermine.task.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.task.Integrate;

public class Project
{
    Map sources = new LinkedHashMap();
    List properties = new ArrayList();
    List postprocesses = new ArrayList();
    
    public void addSource(String name, Source source) {
        sources.put(name, source);
    }        
    
    public void addProperty(SourceProperty property) {
        properties.add(property);
    }
    
    public void addPostProcess(String name) {
        postprocesses.add(name);
    }
    
    public Map getSources() {
        return sources;
    }
    
    public List getProperties() {
        return properties;
    }

    public List getPostProcesses() {
        return postprocesses;
    }
}