package org.intermine.task.project;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;

/**
 * A class representing the contents of a project.xml file.
 * @author Tom Riley
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
        if (sources.containsKey(name)) {
            throw new RuntimeException("project.xml contains more than one source named: " + name);
        }
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

    /**
     * Validate contents of a project.xml file once it has been unmarshalled.  This will check
     *     a) that 'source.location' properties point to valid locations
     *     b) that all sources lists can be found in 'source.location' directories
     * @param projectXml project.xml file, location used to resolve relative source.location paths 
     */
    public void validate(File projectXml) {
        File baseDir = projectXml.getParentFile();
        
        String endl = System.getProperty("line.separator");
        
        if (!sources.isEmpty() &&  getSourceLocations().isEmpty()) {
            throw new BuildException("Error in project.xml: no source locations found.  You need to"
                    + " set at least one 'source.location' property in project.xml to specify where"
                    + " source directories can be found.");
        }
        
        // this will hold a list of canonical locations
        List<String> srcLocations = new ArrayList<String>();
        
        // check that directories specified by 'source.location' properties exist
        // resolve relative paths into a canonical file
        List<String> badLocations = new ArrayList<String>();
        for (String srcLocation : getSourceLocations()) {
            if (srcLocation.indexOf('~') >= 0 
                    || srcLocation.indexOf('$') >= 0) {
                throw new BuildException("Error in project.xml: invalid 'source.location'"
                        + " property: " + srcLocation + ".  Must a be relative or absolute"
                        + " path and cannot contain '~' or environment variables: ");
            }
            
            // if starts with / is absolute path, otherwise resolve relative to project.xml
            File tmpDir;
            if (srcLocation.startsWith("/")) {
                tmpDir = new File(srcLocation);
            } else {
                tmpDir = new File(baseDir, srcLocation);
            }
            
            // get rid of any ../ from path
            String canonicalPath;
            try {
                canonicalPath = tmpDir.getCanonicalPath();
            } catch (IOException e) {
                throw new BuildException("Error finding canonical path for 'source.location': "
                        + tmpDir.getPath());
            }
            
            if (tmpDir.exists()) {
                srcLocations.add(canonicalPath);
            } else {
                badLocations.add(canonicalPath);
            }
        }
        
        if (!badLocations.isEmpty()) {
            StringBuffer message = new StringBuffer("Error in project.xml: Can't open directories"
                    + " specified by 'source.location' properties in project.xml: ");
            for (String badLocation : badLocations) {
                message.append(endl + "\t\t" + badLocation);
            }
            throw new BuildException(message.toString());
        }
        
        
        Set<String> badSources = new HashSet<String>();

        // check that all <source> types can be found
        for (Source s : sources.values()) {
            File sourceDir = null;
            for (String srcLocation : srcLocations) {
                // we already know srcLocation directory exists
                File tmpDir = new File(srcLocation, s.getType());
                if (tmpDir.exists()) {
                    if (sourceDir != null) {
                        // already seen a source with this type
                        throw new BuildException("Error in project.xml: multiple directories found"
                        + " for source '" + s.getType() + "'.  Each source type must be"
                        + " uniquely named within the 'source.location' directories"
                        + " specified. Found in: "
                        + endl + "\t\t" + sourceDir.getParent()
                        + endl + "\t\t" + tmpDir.getParent());
                    }
                    sourceDir = tmpDir;
                }
            }

            if (sourceDir != null) {
                s.setLocation(sourceDir);
            } else {
                badSources.add(s.getType());
            }
        }
        
        if (!badSources.isEmpty()) {
            StringBuffer message = new StringBuffer("Error in project.xml: Can't find directories"
                    + " for sources: ");
            for (String badSource : badSources) {
                message.append("'" + badSource + "', ");
            }
            message.append(" looked in: ");       
            
            for (String srcLocation : srcLocations) {
                message.append(endl + "\t\t" + srcLocation);
            }
            throw new BuildException(message.toString());
        }

        
    }
    
    /**
     * Get a list of directories to search for sources, specified by 'source.location. properties.
     * @return a list of source locations
     */
    public List<String> getSourceLocations() {
        List<String> sourceLocations = new ArrayList<String>();
        for (UserProperty up : properties) {
            if ("source.location".equals(up.getName())) {
                if (up.getLocation() == null) {
                    throw new BuildException("Error in project.xml: no 'location' attribute"
                            + " given for a 'source.location' property element.  You must specify"
                            + " the location attribute.");
                }
                sourceLocations.add(up.getLocation());
            }
        }
        return sourceLocations;
    }
}
