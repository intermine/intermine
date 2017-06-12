package org.intermine.api.lucene;

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
 * container class to hold the name and value of an attribute for an object to
 * be added as a field to the document
 * @author nils
 */
class ObjectValueContainer
{
    final String className;
    final String name;
    final String value;

    /**
     * constructor
     * @param className
     *            name of the class the attribute belongs to
     * @param name
     *            name of the field
     * @param value
     *            value of the field
     */
    public ObjectValueContainer(String className, String name, String value) {
        super();
        this.className = className;
        this.name = name;
        this.value = value;
    }

    /**
     * className
     * @return className
     */
    public String getClassName() {
        return className;
    }

    /**
     * name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * value
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * generate the name to be used as a field name in lucene
     * @return lowercase classname and field name
     */
    public String getLuceneName() {
        return (className + "_" + name).toLowerCase();
    }
}
