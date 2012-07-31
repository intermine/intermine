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

/**
 * A class to hold information about a source from a project.xml file.
 * @author Kim Rutherford
 * @author Richard Smith
 */

public class Source extends Action
{
    private String name;
    private String type;
    private File location;

    /**
     * Set the name of this Source.
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the type of this object.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the name of this Source.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of this object.
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the location of this Source directory.
     * @param location the directory containing this source
     */
    public void setLocation(File location) {
        this.location = location;
    }

    /**
     * Get the directory that contains this Source.
     * @return the name
     */
    public File getLocation() {
        return location;
    }
}