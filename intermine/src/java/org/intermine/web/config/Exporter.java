package org.intermine.web.config;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Configuration information for exporting.
 *
 * @author Kim Rutherford
 */

public class Exporter
{
    String id, actionPath, className;

    /**
     * Return the id of this Exporter.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of this Exporter
     * @param id the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the actionPath
     * @return the actionPath
     */
    public String getActionPath() {
        return actionPath;
    }

    /**
     * Set the actionPath of this Exporter
     * @param actionPath the new actionPath
     */
    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }
    
    /**
     * Get the className
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the className of this Exporter
     * @param className the new className
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @see java.lang.String#toString
     */
    public String toString() {
        return "<exporter id=\"" + id + "\" actionPath=\"" + actionPath
            + "\" className=\"" + className + "\"/>";
    }
}
