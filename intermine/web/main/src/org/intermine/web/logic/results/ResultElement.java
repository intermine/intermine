package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.util.TypeUtil;
import org.intermine.util.Util;

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
    protected Class typeCls;
    protected Integer id;
    protected String otherLink;
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
    public ResultElement(ObjectStore os, Object value, Integer id, Class typeCls,
                         Path path, boolean isKeyField) {
        this.os = os;
        this.field = value;
        this.id = id;
        this.typeCls = typeCls;
        this.keyField = isKeyField;
        this.path = path;
        setHtmlId(path.toString().substring(0, path.toString().lastIndexOf(".")) + "_" + type);
    }
    
    /**
     * Constructor used to create other types of results (like coming from counts)
     * with a custom link
     * @param value the field value
     * @param otherLink a link to an action/page
     */
    public ResultElement(Object value, String otherLink) {
        this.field = value;
        this.otherLink = otherLink;
        this.keyField = false;
        this.path = null;
        this.os = null;
    }
    
    /**
     * Create a ResultElement that displays a single raw value.
     * @param fieldValue the value to hold in this object
     */
    public ResultElement(Object fieldValue) {
        this.field = fieldValue;
        this.path = null;
        this.os = null;
        this.keyField = false;
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
        return TypeUtil.unqualifiedName(typeCls.getName());
    }
   
    /**
     * Get the type
     * @return the type
     */
    public Class getTypeClass() {
        return typeCls;
    }

    /**
     * Set the type
     * @param type the type
     */
    public void setTypeClass(Class typeCls) {
        this.typeCls = typeCls;
    }
    
    /**
     * Get the action/page link
     * @return a String
     */
    public String getOtherLink() {
        return otherLink;
    }

    /**
     * Set another custom action/page link
     * @param otherLink a String
     */
    public void setOtherLink(String otherLink) {
        this.otherLink = otherLink;
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
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        try {
            ResultElement cell = (ResultElement) obj;
            return (Util.equals(field, cell.getField())  && id.equals(cell.getId())
                            && type.equals(cell.getType()));
        } catch (ClassCastException e) {
            throw new ClassCastException("Comparing a ResultsElement with a "
                    + obj.getClass().getName());
        } catch (NullPointerException e) {
            throw new NullPointerException("field = " + field + ", id = " + id + ", type = "
                    + type);
        }
    }
    
    /**
     * (non-Javadoc)
     * {@inheritDoc}
     */
    public int hashCode() {
        return (field == null ? 0 : field.hashCode()) + 3 * id.hashCode() + 7 * type.hashCode();
    }
    
}
