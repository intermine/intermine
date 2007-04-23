package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.digester.*;

import org.xml.sax.SAXException;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;

/**
 * Configuration object for web site
 *
 * @author Andrew Varley
 */
public class WebConfig
{
    private Map types = new HashMap();
    private Map tableExportConfigs = new HashMap();

    /**
     * Parse a WebConfig XML file
     *
     * @param is the InputStream to parse
     * @param model the Model to use when reading - used for checking class names and for finding
     * sub and super classes
     * @return a WebConfig object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class is mentioned in the XML that isn't in the model
     */
    public static WebConfig parse(InputStream is, Model model)
        throws IOException, SAXException, ClassNotFoundException {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("webconfig", WebConfig.class);

        digester.addObjectCreate("webconfig/class", Type.class);
        digester.addSetProperties("webconfig/class", "className", "className");
        digester.addSetProperties("webconfig/class", "fieldName", "fieldName");

        digester.addObjectCreate("webconfig/class/tabledisplayer", Displayer.class);
        digester.addSetProperties("webconfig/class/tabledisplayer", "src", "src");
        digester.addSetNext("webconfig/class/tabledisplayer", "setTableDisplayer");
        
        digester.addCallMethod("webconfig/class/tabledisplayer/param", "addParam", 2);
        digester.addCallParam("webconfig/class/tabledisplayer/param", 0, "name");
        digester.addCallParam("webconfig/class/tabledisplayer/param", 1, "value");
        
        digester.addObjectCreate("webconfig/class/fields/fieldconfig", FieldConfig.class);
        digester.addSetProperties("webconfig/class/fields/fieldconfig", "fieldExpr", "fieldExpr");
        digester.addSetProperties("webconfig/class/fields/fieldconfig", "name", "name");
        digester.addSetProperties("webconfig/class/fields/fieldconfig", "displayer", "displayer");
        digester.addSetNext("webconfig/class/fields/fieldconfig", "addFieldConfig");

        digester.addObjectCreate("webconfig/class/longdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/longdisplayers/displayer");
        digester.addSetNext("webconfig/class/longdisplayers/displayer", "addLongDisplayer");

        digester.addCallMethod("webconfig/class/longdisplayers/displayer/param", "addParam", 2);
        digester.addCallParam("webconfig/class/longdisplayers/displayer/param", 0, "name");
        digester.addCallParam("webconfig/class/longdisplayers/displayer/param", 1, "value");

        digester.addObjectCreate("webconfig/class/bagdisplayers/graphdisplayer",
                                 GraphDisplayer.class);
        digester.addSetProperties("webconfig/class/bagdisplayers/graphdisplayer");
        digester.addSetNext("webconfig/class/bagdisplayers/graphdisplayer", "addGraphDisplayer");

        digester.addObjectCreate("webconfig/class/bagdisplayers/bagtabledisplayer",
                                 BagTableDisplayer.class);
        digester.addSetProperties("webconfig/class/bagdisplayers/bagtabledisplayer");
        digester.addSetNext("webconfig/class/bagdisplayers/bagtabledisplayer",
                            "addBagTableDisplayer");
        
        digester.addSetNext("webconfig/class", "addType");
        
        digester.addObjectCreate("webconfig/tableExportConfig", TableExportConfig.class);
        digester.addSetProperties("webconfig/tableExportConfig", "id", "id");
        digester.addSetProperties("webconfig/tableExportConfig", "actionPath", "actionPath");
        digester.addSetProperties("webconfig/tableExportConfig", "className", "className");

        digester.addSetNext("webconfig/tableExportConfig", "addTableExportConfig");
        
        WebConfig webConfig = (WebConfig) digester.parse(is);

        webConfig.setSubClassConfig(model);

        return webConfig;
    }

    /**
     * Add a type to the WebConfig Map.  Use className as the key of the Map if fieldName of the
     * Type is null, otherwise use the class name, a space, and the field name.
     *
     * @param type the Type to add
     */
    public void addType(Type type) {
        types.put(type.getClassName(), type);
    }

    /**
     * Get the types (== classes) stored in this WebConfig.
     * @return the types
     */
    public Map getTypes() {
        return this.types;
    }

    /**
     * Add an TableExportConfig to the Map of TableExportConfig objects using
     * tableExportConfig.getId() as the Map key.
     * @param tableExportConfig the TableExportConfig to add
     */
    public void addTableExportConfig(TableExportConfig tableExportConfig) {
        tableExportConfigs.put(tableExportConfig.getId(), tableExportConfig);
    }

    /**
     * Return a Map of TableExportConfig.id to TableExportConfig objects.
     * @return the TableExportConfig Map 
     */
    public Map getTableExportConfigs() {
        return tableExportConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof WebConfig)) {
            return false;
        }

        WebConfig webConfigObj = (WebConfig) obj;

        return types.equals(webConfigObj.types)
            && tableExportConfigs.equals(webConfigObj.tableExportConfigs);
    }

    /**
     * {@inheritDoc}
     *
     * @return the hashCode for this WebConfig object
     */
    public int hashCode() {
        return types.hashCode();
    }

    /**
     * For each class/Type mentioned in XML files, copy it's displayers and FieldConfigs to all
     * subclasses that don't already have any configuration.
     * This method has package scope so that it can be called from the tests.
     * @param model the Model to use to find sub-classes
     * @throws ClassNotFoundException if any of the classes mentioned in the XML file aren't in the
     * Model
     */
    void setSubClassConfig(Model model) throws ClassNotFoundException {
        for (Iterator modelIter = new TreeSet(model.getClassNames()).iterator();
             modelIter.hasNext();) {
            String className = (String) modelIter.next();
            
            Type thisClassType = (Type) types.get(className);

            if (thisClassType == null) {
                thisClassType = new Type();
                thisClassType.setClassName(className);
                types.put(className, thisClassType);
            }

            Set cds = model.getClassDescriptorsForClass(Class.forName(className));
            List cdList = new ArrayList(cds);

            for (Iterator cdIter = cdList.iterator(); cdIter.hasNext(); ) {
                ClassDescriptor cd = (ClassDescriptor) cdIter.next();

                if (className.equals(cd.getName())) {
                    continue;
                }

                Type superClassType = (Type) types.get(cd.getName());

                if (superClassType != null) {
                    if (thisClassType.getFieldConfigs().size() == 0) {
                        // copy any FieldConfigs from the super class
                        Iterator fieldConfigIter = superClassType.getFieldConfigs().iterator();

                        while (fieldConfigIter.hasNext()) {
                            FieldConfig fc = (FieldConfig) fieldConfigIter.next();

                            thisClassType.addFieldConfig(fc);
                        }
                    }

                    if (thisClassType.getLongDisplayers().size() == 0) {
                        Iterator longDisplayerIter = superClassType.getLongDisplayers().iterator();

                        while (longDisplayerIter.hasNext()) {
                            Displayer ld = (Displayer) longDisplayerIter.next();
                            thisClassType.addLongDisplayer(ld);
                        }
                    }
                    
                    if (thisClassType.getTableDisplayer() == null) {
                        thisClassType.setTableDisplayer(superClassType.getTableDisplayer());
                    }
                }
            }
        }
    }

    /**
     * Return an XML String of this WebConfig object
     *
     * @return a String version of this WebConfig object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<webconfig>");
        Iterator typesIter = types.values().iterator();
        while (typesIter.hasNext()) {
            sb.append(typesIter.next().toString());
        }
        Iterator tableExportConfigIter = tableExportConfigs.values().iterator();
        while (tableExportConfigIter.hasNext()) {
            sb.append(tableExportConfigIter.next().toString());
        }
        sb.append("</webconfig>");
        return sb.toString();
    }

}
