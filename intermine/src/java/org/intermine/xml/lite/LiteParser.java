package org.flymine.xml.lite;

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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester.*;

import org.xml.sax.SAXException;

import org.flymine.util.DynamicBean;
import org.flymine.util.StringUtil;
import org.flymine.util.TypeUtil;
import org.flymine.objectstore.proxy.LazyInitializer;
import org.flymine.objectstore.query.Query;


/**
 * Read XML Lite format into an Object
 *
 * @author Andrew Varley
 */
public class LiteParser
{
    /**
     * Parse a FlyMine Lite XML file
     *
     * @param is the InputStream to parse
     * @return an object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static Object parse(InputStream is)
        throws IOException, SAXException, ClassNotFoundException {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("object", Item.class);
        digester.addSetProperties("object", "id", "id");
        digester.addSetProperties("object", "class", "className");
        digester.addSetProperties("object", "implements", "implementations");

        digester.addObjectCreate("object/field", Field.class);
        digester.addSetProperties("object/field", "name", "name");
        digester.addSetProperties("object/field", "value", "value");

        digester.addObjectCreate("object/reference", Field.class);
        digester.addSetProperties("object/reference", "name", "name");
        digester.addSetProperties("object/reference", "value", "value");

        digester.addSetNext("object/field", "addField");
        digester.addSetNext("object/reference", "addReference");

        return convertToObject(((Item) digester.parse(is)));

   }

    /**
     * Convert Item to object
     *
     * @param item the Item to convert
     * @return the converted object
     * @throws ClassNotFoundException if a class cannot be found
     */
    protected static Object convertToObject(Item item) throws ClassNotFoundException {
        Class clazz = Class.forName(item.getClassName());
        List interfaces = StringUtil.tokenize(item.getImplementations());
        Iterator intIter = interfaces.iterator();
        List intClasses = new ArrayList();
        while (intIter.hasNext()) {
            intClasses.add(Class.forName((String) intIter.next()));
        }

        Object obj = DynamicBean.create(clazz,
                                        (Class []) intClasses.toArray(new Class [] {}));

        try {
            // Set the data for every given Field
            Iterator fieldIter = item.getFields().iterator();
            while (fieldIter.hasNext()) {
                Field field = (Field) fieldIter.next();
                Class fieldClass = TypeUtil.getFieldInfo(obj.getClass(), field.getName()).getType();
                TypeUtil.setFieldValue(obj, field.getName(),
                                       TypeUtil.stringToObject(fieldClass, field.getValue()));
            }

            // Set the data for every given reference
            Iterator refIter = item.getReferences().iterator();
            while (refIter.hasNext()) {
                Field field = (Field) refIter.next();
                Class fieldClass = TypeUtil.getFieldInfo(obj.getClass(), field.getName()).getType();
                Query query = new Query();
                Integer id = new Integer(Integer.parseInt(field.getValue()));
                TypeUtil.setFieldValue(obj, field.getName(),
                                       LazyInitializer.getDynamicProxy(fieldClass, query, id));
            }
        } catch (IllegalAccessException e) {
        }

        return obj;
    }
}
