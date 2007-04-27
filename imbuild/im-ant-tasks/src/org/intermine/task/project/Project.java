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
    Map<String, Source> sources = new LinkedHashMap<String, Source>();
    List<UserProperty> properties = new ArrayList<UserProperty>();
    Map<String, PostProcess> postProcesses = new LinkedHashMap<String, PostProcess>();
    
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
    public Map<String, Source> getSources() {
        return sources;
    }
    
    /**
     * Return a list of UserProperty objects for the Project.
     * @return the properties
     */
    public List<UserProperty> getProperties() {
        return properties;
    }

    /**
     * Return a Map from post-process name to PostProcess objects
     * @return the PostProcess objects
     */
    public Map<String, PostProcess> getPostProcesses() {
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