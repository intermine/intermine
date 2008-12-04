package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import org.intermine.pathquery.Path;
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
    protected Class typeCls;
    protected Integer id;
    protected String htmlId;
    private final boolean keyField;
    private final ObjectStore os;
    private final Path path;
    private boolean isSelected = false;


    /**
     * Constructs a new ResultCell object
     * @param os the ObjectStore to use in getInterMineObject()
     * @param value the value of the field from the results table
     * @param id the id of the InterMineObject this field belongs to
     * @param typeCls the Class of the InterMineObject this field belongs to
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
     * @param typeCls the type
     */
    public void setTypeClass(Class typeCls) {
        this.typeCls = typeCls;
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
     * Return the InterMineObject that contains this result element.
     * @return the InterMineObject
     * @throws ObjectStoreException if there is a problem getting the object from the ObjectStore
     */
    public InterMineObject getInterMineObject() throws ObjectStoreException {
        return os.getObjectById(getId());
    }

    /**
     * Set the selected status of results element
     * @param isSelected whether or not the element is selected
     * @Deprecated selected elements are stored in the PagedTable
     */
    @Deprecated public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
    
    /**
     * Find the selected status of this element.
     * @return true if this element has been selected
     * @Deprecated selected elements are stored in the PagedTable
     */
    @Deprecated public boolean isSelected() {
        return isSelected;
    }
        
    /**
     * Returns a String representation of the ResultElement
     * @return a String
     */
    public String toString() {
        return " " + field + " " + id + " " + TypeUtil.unqualifiedName(typeCls.getName());
    }

    /**
     * (non-Javadoc)
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        try {
            ResultElement cell = (ResultElement) obj;
            return (Util.equals(field, cell.getField())  && id.equals(cell.getId())
                    && typeCls.equals(cell.getTypeClass()));
        } catch (ClassCastException e) {
            throw new ClassCastException("Comparing a ResultsElement with a "
                    + obj.getClass().getName());
        } catch (NullPointerException e) {
            throw new NullPointerException("field = " + field + ", id = " + id + ", type = "
                    + TypeUtil.unqualifiedName(typeCls.getName()));
        }
    }

    /**
     * (non-Javadoc)
     * {@inheritDoc}
     */
    public int hashCode() {
        return (field == null ? 0 : field.hashCode()) + 3 * id.hashCode()
        + 7 * typeCls.hashCode();
    }
    
    /**
     * Get the unqualified class name as a string
     * @return a String
     */
    public String getTypeClsString() {
        return TypeUtil.unqualifiedName(typeCls.getName());
    }
}
