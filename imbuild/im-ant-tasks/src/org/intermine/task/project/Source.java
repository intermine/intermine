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
 * A class to hold information about a source from a project.xml file.
 * @author Kim Rutherford
 */

public class Source extends Action
{
    String type;
    
    /**
     * Set the type of this Source.
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
}