package org.intermine.install.project.event;

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
import java.util.EventObject;

import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Source;

/**
 * Event object for project events.
 */
public class ProjectEvent extends EventObject
{
    private static final long serialVersionUID = -5180333197183018756L;
    
    /**
     * The project that is the cause of the event. Only relevant for certain
     * events.
     * @serial
     */
    private Project project;
    
    /**
     * The source that is the cause of the event. Only relevant for certain
     * events.
     * @serial
     */
    private Source projectSource;
    
    /**
     * The project file. Only relevant for certain events.
     */
    private transient File projectFile;
    
    
    /**
     * Creates a new ProjectEvent with only the project file information.
     * 
     * @param eventSource The origin of this event.
     * @param projectFile The project file.
     */
    public ProjectEvent(Object eventSource, File projectFile) {
        super(eventSource);
        this.projectFile = projectFile;
    }

    /**
     * Creates a new ProjectEvent with the Project object and project file information.
     * 
     * @param eventSource The origin of this event.
     * @param project The Project object.
     * @param projectFile The project file.
     */
    public ProjectEvent(Object eventSource, Project project, File projectFile) {
        super(eventSource);
        this.project = project;
        this.projectFile = projectFile;
    }

    /**
     * Creates a new ProjectEvent with only the Project object.
     * 
     * @param eventSource The origin of this event.
     * @param project The Project object.
     */
    public ProjectEvent(Object eventSource, Project project) {
        super(eventSource);
        this.project = project;
    }

    /**
     * Creates a new ProjectEvent with the Project object and a Source object.
     * 
     * @param eventSource The origin of this event.
     * @param project The Project object.
     * @param projectSource The Source object.
     */
    public ProjectEvent(Object eventSource, Project project, Source projectSource) {
        super(eventSource);
        this.project = project;
        this.projectSource = projectSource;
    }

    /**
     * Get the Project object.
     * @return The Project. May be null.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get the Source object.
     * @return The Source. May be null.
     */
    public Source getProjectSource() {
        return projectSource;
    }

    /**
     * Get the Project file.
     * @return The project File. May be null.
     */
    public File getProjectFile() {
        return projectFile;
    }
}
