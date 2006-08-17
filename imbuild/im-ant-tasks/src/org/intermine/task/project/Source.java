/**
 * 
 */
package org.intermine.task.project;


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