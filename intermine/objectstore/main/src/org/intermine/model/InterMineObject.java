package org.intermine.model;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringConstructor;

/**
 * A root interface for all objects that can be stored in a InterMine database.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public interface InterMineObject
{
    /**
     * Getter for the ID field - ensures that every object in the database has an ID.
     *
     * @return an Integer
     */
    public java.lang.Integer getId();
    /**
     * Setter for the ID field.
     *
     * @param id an Integer
     */
    public void setId(java.lang.Integer id);

    /**
     * Returns the NotXml serialised version of this object.
     *
     * @return a StringConstructor containing the NotXml
     */
    public StringConstructor getoBJECT();

    /**
     * Sets the values of the fields to the values in the given NotXml.
     *
     * @param notXml a String containing NotXml for this class
     * @param os an ObjectStore from which to create proxies
     */
    public void setoBJECT(String notXml, ObjectStore os);

    /**
     * Sets the values of the fields to the values in the given split NotXml.
     *
     * @param notXml a String array containing the NotXml split with the delimiter
     * @param os an ObjectStore from which to create proxies
     */
    public void setoBJECT(String[] notXml, ObjectStore os);

    /**
     * Returns the value of a field by name.
     *
     * @param fieldName the name of the field
     * @return the value of the field
     * @throws IllegalAccessException when something goes wrong
     */
    public Object getFieldValue(String fieldName) throws IllegalAccessException;

    /**
     * Returns the value of a field without dereferencing any ProxyReference objects.
     *
     * @param fieldName the name of the field
     * @return the value of the field, or a ProxyReference representing it
     * @throws IllegalAccessException when something goes wrong
     */
    public Object getFieldProxy(String fieldName) throws IllegalAccessException;

    /**
     * Sets the value of a field by name.
     *
     * @param fieldName the name of the field
     * @param value the value of the field, or a ProxyReference representing it
     */
    public void setFieldValue(String fieldName, Object value);

    /**
     * Returns the type of a field by name.
     *
     * @param fieldName the name of the field
     * @return the type of the field
     */
    public Class getFieldType(String fieldName);

    /**
     * Returns the element type of a collection by name.
     *
     * @param fieldName the name of the collection
     * @return the type of the elements of the collection
     */
    public Class getElementType(String fieldName);
}
