package org.intermine.web.config;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration object describing details of a displayer
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class Displayer
{
    private String src;
    private String aspects = "";
    private Map params = new HashMap();

    /**
     * Set the source of this displayer
     *
     * @param src the source
     */
    public void setSrc(String src) {
        this.src = src;
    }

    /**
     * Get the aspects of this displayer
     *
     * @return the aspects
     */
    public String getAspects() {
        return this.aspects;
    }
    
    /**
     * Set the aspects of this displayer
     *
     * @param aspect the aspects
     */
    public void setAspects(String aspect) {
        this.aspects = aspect;
    }
    
    /**
     * Get the source of this displayer
     *
     * @return the source
     */
    public String getSrc() {
        return this.src;
    }

    /**
     * Add a stored parameter to this Displayer.
     *
     * @param name the name of the parameter to add
     * @param value the value of the parameter to add
     */
    public void addParam(String name, String value) {
        params.put(name, value);
    }

    /**
     * Return the parameters stored in this Displayer.
     *
     * @return the stored parameters
     */
    public Map getParams() {
        return params;
    }

    /**
     * @see Object#equals
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Displayer)) {
            return false;
        }

        return src.equals(((Displayer) obj).src);
    }

    /**
     * @see Object#hashCode
     *
     * @return the hashCode for this Displayer object
     */
    public int hashCode() {
        return src.hashCode();
    }

    /**
     * Return an XML String of this Type object
     *
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return toString("displayer");
    }

    /**
     * Return an XML String of this Type object
     * @param elementName the element name
     * @return a String version of this WebConfig object
     */
    public String toString(String elementName) {
        return "<" + elementName + " src=\"" + src + "\"/>";
    }
}

