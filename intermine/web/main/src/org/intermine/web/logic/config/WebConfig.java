package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.HTMLWidgetConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.xml.sax.SAXException;

/**
 * Configuration object for web site
 *
 * @author Andrew Varley
 */
public class WebConfig
{
    private static final Logger LOG = Logger.getLogger(WebConfig.class);
    private Map<String, Type> types = new HashMap<String, Type>();
    private Map<String, TableExportConfig> tableExportConfigs =
        new HashMap<String, TableExportConfig>();
    private Map<String, WidgetConfig> widgets = new HashMap<String, WidgetConfig>();

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

        digester.addObjectCreate("webconfig/class/bagdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/bagdisplayers/displayer");
        digester.addSetNext("webconfig/class/bagdisplayers/displayer", "addBagDisplayer");

        digester.addCallMethod("webconfig/class/bagdisplayers/displayer/param", "addParam", 2);
        digester.addCallParam("webconfig/class/bagdisplayers/displayer/param", 0, "name");
        digester.addCallParam("webconfig/class/bagdisplayers/displayer/param", 1, "value");

        digester.addObjectCreate("webconfig/widgets/graphdisplayer", GraphWidgetConfig.class);
        digester.addSetProperties("webconfig/widgets/graphdisplayer");
        digester.addSetNext("webconfig/widgets/graphdisplayer", "addWidget");

        digester.addObjectCreate("webconfig/widgets/enrichmentwidgetdisplayer",
                EnrichmentWidgetConfig.class);
        digester.addSetProperties("webconfig/widgets/enrichmentwidgetdisplayer");
        digester.addSetNext("webconfig/widgets/enrichmentwidgetdisplayer", "addWidget");

        digester.addObjectCreate("webconfig/widgets/bagtabledisplayer", TableWidgetConfig.class);
        digester.addSetProperties("webconfig/widgets/bagtabledisplayer");
        digester.addSetNext("webconfig/widgets/bagtabledisplayer", "addWidget");

        digester.addObjectCreate("webconfig/widgets/htmldisplayer", HTMLWidgetConfig.class);
        digester.addSetProperties("webconfig/widgets/htmldisplayer");
        digester.addSetNext("webconfig/widgets/htmldisplayer", "addWidget");

        digester.addSetNext("webconfig/class", "addType");

        digester.addObjectCreate("webconfig/tableExportConfig", TableExportConfig.class);
        digester.addSetProperties("webconfig/tableExportConfig", "id", "id");
        digester.addSetProperties("webconfig/tableExportConfig", "className", "className");

        digester.addSetNext("webconfig/tableExportConfig", "addTableExportConfig");

        WebConfig webConfig = (WebConfig) digester.parse(is);

        webConfig.validate(model);

        webConfig.setSubClassConfig(model);

        return webConfig;
    }

    /**
     * Validate web config according to the model. Test that configured classes exist in
     * model and configured fields in web config exist in model.
     * @param model model used for validation
     */
    private void validate(Model model) {
        StringBuffer invalidClasses = new StringBuffer();
        StringBuffer badFieldExpressions = new StringBuffer();
        for (String typeName : types.keySet()) {
            if (!model.getClassNames().contains(typeName)) {
                invalidClasses.append(" " + typeName);
                continue;
            }
            Type type = types.get(typeName);
            for (FieldConfig fieldConfig : type.getFieldConfigs()) {
                String pathString;
                try {
                    pathString = Class.forName(typeName).getSimpleName()
                        + "." + fieldConfig.getFieldExpr();
                } catch (ClassNotFoundException e) {
                    String msg = "Invalid web config. '" + typeName + "' doesn't exist in the "
                        + "model.";
                    LOG.warn(msg);
                    continue;
                }
                try {
                    new Path(model, pathString);
                } catch (PathException e) {
                    badFieldExpressions.append(" " + pathString);
                    continue;
                }
            }
        }
        if (invalidClasses.length() > 0 || badFieldExpressions.length() > 0) {
            String msg = "Invalid web config. "
                    + ((invalidClasses.length() > 0)
                            ? "Classes specified in web config that don't exist in model: "
                                    + invalidClasses.toString() + ". " : "")
                            + ((badFieldExpressions.length() > 0)
                                    ? "Path specified in a fieldExpr does note exist in model: "
                                            + badFieldExpressions + ". " : "");
            LOG.error(msg);
        }
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
    public Map<String, Type> getTypes() {
        return this.types;
    }

    /**
     * @return the widgets
     */
    public Map<String, WidgetConfig> getWidgets() {
        return widgets;
    }

    /**
     * @param widget the widget
     */
    public void addWidget(WidgetConfig widget) {
        // TODO validate each widget?
        widgets.put(widget.getId(), widget);
        String[] widgetTypes = widget.getTypeClass().split(",");
        for (String widgetType: widgetTypes) {
            Type type = types.get(widgetType);
            if (type == null) {
                String msg = "Invalid web config. " + widgetType + " is not a valid class. "
                    + "Please correct the entry in the webconfig-model.xml for the "
                    + widget.getId() + " widget.";
                LOG.warn(msg);
            } else {
                type.addWidget(widget);
            }
        }
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
    public Map<String, TableExportConfig> getTableExportConfigs() {
        return tableExportConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param obj the Object to compare with
     * @return true if this is equal to obj
     */
    @Override
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
    @Override
    public int hashCode() {
        return types.hashCode();
    }

    /**
     * For each class/Type mentioned in XML files, copy its displayers and FieldConfigs to all
     * subclasses that don't already have any configuration.
     * This method has package scope so that it can be called from the tests.
     *
     * @param model the Model to use to find sub-classes
     * @throws ClassNotFoundException if any of the classes mentioned in the XML file aren't in the
     * Model
     */
    void setSubClassConfig(Model model) throws ClassNotFoundException {
        TreeSet<String> classes = new TreeSet<String>(model.getClassNames());
        for (Iterator<String> modelIter = classes.iterator(); modelIter.hasNext();) {

            String className = modelIter.next();
            Type thisClassType = types.get(className);

            if (thisClassType == null) {
                thisClassType = new Type();
                thisClassType.setClassName(className);
                types.put(className, thisClassType);
            }

            Set<ClassDescriptor> cds = model.getClassDescriptorsForClass(Class.forName(className));
            for (ClassDescriptor cd : cds) {
                if (className.equals(cd.getName())) {
                    continue;
                }

                Type superClassType = types.get(cd.getName());

                if (superClassType != null) {
                    if (thisClassType.getFieldConfigs().size() == 0) {
                        // copy any FieldConfigs from the super class
                        for (FieldConfig fc : superClassType.getFieldConfigs()) {
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

                    if (thisClassType.getWidgets().size() == 0
                                    && superClassType.getWidgets() != null
                                    && superClassType.getWidgets().size() > 0) {
                        Iterator widgetIter = superClassType.getWidgets().iterator();

                        while (widgetIter.hasNext()) {
                            WidgetConfig wi = (WidgetConfig) widgetIter.next();
                            thisClassType.addWidget(wi);
                        }
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
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<webconfig>");
        Iterator<Type> typesIter = types.values().iterator();
        while (typesIter.hasNext()) {
            sb.append(typesIter.next().toString());
        }
        Iterator<TableExportConfig> tableExportConfigIter = tableExportConfigs.values().iterator();
        while (tableExportConfigIter.hasNext()) {
            sb.append(tableExportConfigIter.next().toString());
        }
        sb.append("</webconfig>");
        return sb.toString();
    }

}
