package org.flymine.xml.full;

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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.SAXException;



/**
 * Read XML Full format into an Object
 *
 * @author Andrew Varley
 */
public class FullParser
{
    private static Log log = LogFactory.getLog(FullParser.class);

    /**
     * Parse a FlyMine Full XML file
     *
     * @param is the InputStream to parse
     * @return a list of Items
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static List parse(InputStream is)
        throws IOException, SAXException, ClassNotFoundException {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        Digester digester = new Digester();
        digester.setValidating(false);
        digester.setLogger(log);

        digester.addObjectCreate("items", ArrayList.class);

        digester.addObjectCreate("items/object", Item.class);
        digester.addSetProperties("items/object", new String[]{"xml_id", "class", "implements"},
                                  new String[] {"identifier", "className", "implementations"});

        digester.addObjectCreate("items/object/field", Field.class);
        digester.addSetProperties("items/object/field");

        digester.addObjectCreate("items/object/reference", Field.class);
        digester.addSetProperties("items/object/reference", "ref_id", "value");

        digester.addObjectCreate("items/object/collection", ReferenceList.class);
        digester.addSetProperties("items/object/collection");
        digester.addCallMethod("items/object/collection/reference", "addValue", 1);
        digester.addCallParam("items/object/collection/reference", 0, "ref_id");

        digester.addSetNext("items/object/field", "addField");
        digester.addSetNext("items/object/reference", "addReference");
        digester.addSetNext("items/object/collection", "addCollection");
        digester.addSetNext("items/object", "add");


        return (List) digester.parse(is);
    }

}
