package org.intermine.model;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A root interface for all objects that can be stored in a InterMine database.
 *
 * @author Julie
 */
public interface InterMineFastPathObject extends FastPathObject
{
    /**
     * Getter for the ID field - ensures that every object in the database has an ID.
     *
     * @return an Integer
     */
    java.lang.Integer getId();

    /**
     * Setter for the ID field.
     *
     * @param id an Integer
     */
    void setId(java.lang.Integer id);

    /**
     * Returns the NotXml serialised version of this object.
     *
     * @return a StringConstructor containing the NotXml
     */
    StringConstructor getoBJECT();

//    TODO FIXME
//    /**
//     * Sets the values of the fields to the values in the given NotXml.
//     *
//     * @param notXml a String containing NotXml for this class
//     * @param os an ObjectStore from which to create proxies
//     */
//    void setoBJECT(String notXml, ObjectStore os);
//
//    /**
//     * Sets the values of the fields to the values in the given split NotXml.
//     *
//     * @param notXml a String array containing the NotXml split with the delimiter
//     * @param os an ObjectStore from which to create proxies
//     */
//    void setoBJECT(String[] notXml, ObjectStore os);

    /**
     * Returns the element type of a collection by name.
     *
     * @param fieldName the name of the collection
     * @return the type of the elements of the collection
     */
    Class<?> getElementType(String fieldName);
}
