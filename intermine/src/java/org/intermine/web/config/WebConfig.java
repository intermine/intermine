package org.intermine.web.config;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.digester.*;

import org.xml.sax.SAXException;
/**
 * Configuration object for web site
 *
 * @author Andrew Varley
 */
public class WebConfig
{
    private Map types = new HashMap();

    /**
     * Parse a WebConfig XML file
     *
     * @param is the InputStream to parse
     * @return a WebConfig object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     */
    public static WebConfig parse(InputStream is)
        throws IOException, SAXException {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("webconfig", WebConfig.class);

        digester.addObjectCreate("webconfig/class", Type.class);
        digester.addSetProperties("webconfig/class", "name", "name");
        digester.addSetProperties("webconfig/class", "fieldName", "fieldName");

        digester.addObjectCreate("webconfig/class/shortdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/shortdisplayers/displayer", "src", "src");
        digester.addSetNext("webconfig/class/shortdisplayers/displayer", "addShortDisplayer");

        digester.addObjectCreate("webconfig/class/longdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/longdisplayers/displayer", "src", "src");
        digester.addSetNext("webconfig/class/longdisplayers/displayer", "addLongDisplayer");

        digester.addCallMethod("webconfig/class/longdisplayers/displayer/param", "addParam", 2);
        digester.addCallParam("webconfig/class/longdisplayers/displayer/param", 0, "name");
        digester.addCallParam("webconfig/class/longdisplayers/displayer/param", 1, "value");

        digester.addCallMethod("webconfig/class/shortdisplayers/displayer/param", "addParam", 2);
        digester.addCallParam("webconfig/class/shortdisplayers/displayer/param", 0, "name");
        digester.addCallParam("webconfig/class/shortdisplayers/displayer/param", 1, "value");

        digester.addSetNext("webconfig/class", "addType");

        return (WebConfig) digester.parse(is);

   }

    /**
     * Add a type
     *
     * @param type the Type to add
     */
    public void addType(Type type) {
        types.put(type.getClassName(), type);
    }

    /**
     * Get the types
     *
     * @return the types
     */
    public Map getTypes() {
        return this.types;
    }

    /**
     * @see Object#equals
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof WebConfig)) {
            return false;
        }

        return types.equals(((WebConfig) obj).types);
    }

    /**
     * @see Object#hashCode
     *
     * @return the hashCode for this WebConfig object
     */
    public int hashCode() {
        return types.hashCode();
    }

    /**
     * Return an XML String of this WebConfig object
     *
     * @return a String version of this WebConfig object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<webconfig>");
        Iterator iter = types.values().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().toString());
        }
        sb.append("</webconfig>");
        return sb.toString();
    }

}

