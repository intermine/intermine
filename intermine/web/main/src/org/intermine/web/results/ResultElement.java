package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.path.Path;

/**
 * Cell of results table containing information
 * for the parent Object
 * 
 * @author Xavier Watkins
 */
public class ResultElement implements Serializable
{
    protected Object field;
    protected String type;
    protected Integer id;
    protected String htmlId;
    private final boolean keyField;
    private final ObjectStore os;
    private final Path path;
    

    /**
     * Constructs a new ResultCell object
     * @param os the ObjectStore to use in getInterMineObject()
     * @param value the value of the field from the results table
     * @param id the id of the InterMineObject this field belongs to
     * @param type the Class of the InterMineObject this field belongs to
     * @param path the Path
     * @param isKeyField should be true if this is an identifying field
     */
    public ResultElement(ObjectStore os, Object value, Integer id, String type,
                         Path path, boolean isKeyField) {
        this.os = os;
        this.field = value;
        this.id = id;
        this.type = type;
        this.keyField = isKeyField;
        this.path = path;
        setHtmlId(path.toString().substring(0, path.toString().lastIndexOf(".")) + "_" + type);
    }
    
    /**
     * Get the field value
     * @return the value
     */
    public Object getField() {
        return field;
    }
    
    /**
     * Set the field value
     * @param field the field
     */
    public void setField(Object field) {
        this.field = field;
    }
    
    /**
     * Get the type
     * @return the type
     */
    public String getType() {
        return type;
    }
   
    
    /**
     * Set the type
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Return true if this object represents a field that is an identifying field (according to
     * ClassKeyHelper.isKeyField())
     * @return true if this is a key field
     */
    public boolean isKeyField() {
        return keyField;
    }
    
    /**
     * Get the Id
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Set the Id
     * @param id the id
     */
    public void setId(Integer id) {
        this.id = id;
    }
    
    /**
     * @return the path
     */
    public Path getPath() {
        return path;
    }

    
    /**
     * @return the htmlId
     */
    public String getHtmlId() {
        return htmlId;
    }

    /**
     * @param htmlId the htmlId to set
     */
    public void setHtmlId(String htmlId) {
        this.htmlId = htmlId;
    }

    /**
     * Return the InterMineObject that contains this result element.
     * @return the InterMineObject
     * @throws ObjectStoreException if there is a problem getting the object from the ObjectStore
     */
    public InterMineObject getInterMineObject() throws ObjectStoreException {
        return os.getObjectById(getId());
    }
    
    /**
     * Returns a String reprtesentation of the ResultElement
     * @return a String
     */
    public String toString() {
        return " " + field + " " + id + " " + type;
    }
    
    /**
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        ResultElement cell = (ResultElement) obj;
        return (field.equals(cell.getField())  && id.equals(cell.getId()) 
                        && type.equals(cell.getType()));
    }
    
    /**
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return field.hashCode() + 3 * id.hashCode() + 7 * type.hashCode();
    }
    
}
