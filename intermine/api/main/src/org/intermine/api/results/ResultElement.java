package org.intermine.api.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;

import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.Util;

/**
 * Cell of results table containing information for the parent Object
 *
 * @author Xavier Watkins
 */
public class ResultElement implements Serializable, ResultCell
{
    private static final long serialVersionUID = 1L;
    protected Object field;
    protected FastPathObject imObj;
    protected String htmlId;
    /** @boolean protected as we need to determine if the element is a key field from JSP */
    protected final boolean keyField;
    private final Path path;
    private String linkRedirect;

    /**
     * Constructs a new ResultCell object
     * @param imObj the InterMineObject or SimpleObject to wrap
     * @param path the Path
     * @param isKeyField should be true if this is an identifying field
     */
    public ResultElement(FastPathObject imObj, Path path, boolean isKeyField) {
        this.imObj = imObj;
        this.keyField = isKeyField;
        this.path = path;
        if (imObj != null) {
            try {
                field = imObj.getFieldValue(path.getEndFieldDescriptor().getName());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            field = null;
        }
    }

    /**
     * Create a ResultElement that displays a single raw value.
     * @param fieldValue the value to hold in this object
     */
    public ResultElement(Object fieldValue) {
        this.field = fieldValue;
        this.path = null;
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
        if (imObj == null) {
            return null;
        }
        String cls = DynamicUtil.getSimpleClassName(imObj.getClass());
        return TypeUtil.unqualifiedName(cls);
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
     * Get the Id.
     *
     * @return the id
     */
    public Integer getId() {
        if (imObj instanceof InterMineObject) {
            return ((InterMineObject) imObj).getId();
        }
        return null;
    }

    /**
     * @return the path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Return the Object contained in this result element.
     *
     * @return the InterMineObject
     */
    public FastPathObject getObject() {
        return imObj;
    }

    /**
     * @param linkRedirect the linkRedirect to set
     */
    public void setLinkRedirect(String linkRedirect) {
        this.linkRedirect = linkRedirect;
    }

    /**
     * @return the linkRedirect
     */
    public String getLinkRedirect() {
        return linkRedirect;
    }

    /**
     * Returns a String representation of the ResultElement.
     *
     * @return a String
     */
    public String toString() {
        return " " + field + " " + getId() + " " + getType();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            ResultElement cell = (ResultElement) obj;
            return (Util.equals(field, cell.getField())
                    && Util.equals(imObj, cell.getObject()));
        } catch (ClassCastException e) {
            throw new ClassCastException("Comparing a ResultsElement with a "
                    + obj.getClass().getName());
        } catch (NullPointerException e) {
            throw new NullPointerException("field = " + field + ", imObj = " + imObj + ", type = "
                    + TypeUtil.unqualifiedName(imObj.getClass().getName()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (field == null ? 0 : field.hashCode())
            + (imObj == null ? 0 : 3 * imObj.hashCode());
    }


}
