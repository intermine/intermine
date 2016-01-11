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
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public interface FastPathObject
{
    /**
     * Returns the value of a field by name.
     *
     * @param fieldName the name of the field
     * @return the value of the field
     * @throws IllegalAccessException when something goes wrong
     */
    Object getFieldValue(String fieldName) throws IllegalAccessException;

    /**
     * Returns the value of a field without dereferencing any ProxyReference objects.
     *
     * @param fieldName the name of the field
     * @return the value of the field, or a ProxyReference representing it
     * @throws IllegalAccessException when something goes wrong
     */
    Object getFieldProxy(String fieldName) throws IllegalAccessException;

    /**
     * Sets the value of a field by name.
     *
     * @param fieldName the name of the field
     * @param value the value of the field, or a ProxyReference representing it
     */
    void setFieldValue(String fieldName, Object value);

    /**
     * Returns the type of a field by name.
     *
     * @param fieldName the name of the field
     * @return the type of the field
     */
    Class<?> getFieldType(String fieldName);
}
