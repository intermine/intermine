package org.flymine.web.config;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.*;

import org.xml.sax.SAXException;
/**
 * Configuration object for web site
 *
 * @author Andrew Varley
 */
public class WebConfig
{
    private List types = new ArrayList();

    /**
     * Parse a WebConfig XML file
     *
     * @param filename the name of the file
     * @return a WebConfig object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     */
    public static WebConfig parse(String filename)
        throws IOException, SAXException {

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("webconfig", WebConfig.class);

        digester.addObjectCreate("webconfig/class", Type.class);
        digester.addSetProperties("webconfig/class", "name", "name");

        digester.addObjectCreate("webconfig/class/shortdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/shortdisplayers/displayer", "src", "src");
        digester.addSetNext("webconfig/class/shortdisplayers/displayer", "addShortDisplayer");

        digester.addObjectCreate("webconfig/class/longdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/longdisplayers/displayer", "src", "src");
        digester.addSetNext("webconfig/class/longdisplayers/displayer", "addLongDisplayer");

        digester.addSetNext("webconfig/class", "addType");

        return (WebConfig) digester.parse(WebConfig.class.getClassLoader()
                                          .getResourceAsStream(filename));

   }

    /**
     * Add a type
     *
     * @param type the Type to add
     */
    public void addType(Type type) {
        types.add(type);
    }

    /**
     * Get the types
     *
     * @return the types
     */
    public List getTypes() {
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

}

