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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.session.SessionMethods;
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
    private final Map<String, Type> types = new TreeMap<String, Type>();
    private final Map<String, TableExportConfig> tableExportConfigs =
        new TreeMap<String, TableExportConfig>();
    private final Map<String, WidgetConfig> widgets = new HashMap<String, WidgetConfig>();
    private final List<ReportDisplayerConfig> reportDisplayerConfigs =
        new ArrayList<ReportDisplayerConfig>();

    /**
     * Parse a WebConfig XML file
     *
     * @param context The servlet context we are in.
     * @param model the Model to use when reading - used for checking class names and for finding
     * sub and super classes
     * @return a WebConfig object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class is mentioned in the XML that isn't in the model
     */
    public static WebConfig parse(final ServletContext context, final Model model)
        throws IOException, SAXException, ClassNotFoundException {

        BasicConfigurator.configure();

        final InputStream webconfXML = context.getResourceAsStream("/WEB-INF/webconfig-model.xml");
        if (webconfXML == null) {
            throw new FileNotFoundException("Could not find webconfig-model.xml");
        }

        final Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("webconfig", WebConfig.class);

        digester.addObjectCreate("webconfig/class", Type.class);
        digester.addSetProperties("webconfig/class", "className", "className");
        digester.addSetProperties("webconfig/class", "fieldName", "fieldName");

        /* configure how the "title" of an object is displayed on Type */
        digester.addObjectCreate("webconfig/class/headerconfig/titles", HeaderConfigTitle.class);
        digester.addSetProperties("webconfig/class/headerconfig/titles/title",
                "mainTitles", "mainTitles");
        digester.addSetProperties("webconfig/class/headerconfig/titles/title",
                "subTitles", "subTitles");
        digester.addSetProperties("webconfig/class/headerconfig/titles/title",
                "numberOfMainTitlesToShow", "numberOfMainTitlesToShow");
        digester.addSetProperties("webconfig/class/headerconfig/titles/title",
                "appendConfig", "appendConfig");
        digester.addSetNext("webconfig/class/headerconfig/titles", "addHeaderConfigTitle");

        digester.addObjectCreate("webconfig/class/headerconfig/customlinks",
                HeaderConfigLink.class);
        digester.addSetProperties("webconfig/class/headerconfig/customlinks/customlink",
                "url", "url");
        digester.addSetProperties("webconfig/class/headerconfig/customlinks/customlink",
                "text", "text");
        digester.addSetProperties("webconfig/class/headerconfig/customlinks/customlink",
                "image", "image");
        digester.addSetNext("webconfig/class/headerconfig/customlinks", "addHeaderConfigLink");

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
        digester.addSetProperties("webconfig/class/fields/fieldconfig",
                "showInListAnalysisPreviewTable", "showInListAnalysisPreviewTable");
        digester.addSetNext("webconfig/class/fields/fieldconfig", "addFieldConfig");

        digester.addObjectCreate("webconfig/class/longdisplayers/displayer", Displayer.class);
        digester.addSetProperties("webconfig/class/longdisplayers/displayer");
        digester.addSetNext("webconfig/class/longdisplayers/displayer", "addLongDisplayer");

        digester.addCallMethod("webconfig/class/longdisplayers/displayer/param", "addParam", 2);
        digester.addCallParam("webconfig/class/longdisplayers/displayer/param", 0, "name");
        digester.addCallParam("webconfig/class/longdisplayers/displayer/param", 1, "value");

        /* display inline tables as inline lists instead */
        digester.addObjectCreate("webconfig/class/inlinelist/table", InlineListConfig.class);
        digester.addSetProperties("webconfig/class/inlinelist/table");
        digester.addSetNext("webconfig/class/inlinelist/table", "addInlineList");
        digester.addSetProperties("webconfig/class/inlinelist/table", "path", "path");
        digester.addSetProperties("webconfig/class/inlinelist/table",
                "showLinksToObjects", "showLinksToObjects");
        digester.addSetProperties("webconfig/class/inlinelist/table",
                "showInHeader", "showInHeader");
        digester.addSetProperties("webconfig/class/inlinelist/table", "lineLength", "lineLength");


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

        digester.addObjectCreate("webconfig/reportdisplayers/reportdisplayer",
                ReportDisplayerConfig.class);
        digester.addSetProperties("webconfig/reportdisplayers/reportdisplayer");
        digester.addSetNext("webconfig/reportdisplayers/reportdisplayer", "addReportDisplayer");

        final WebConfig webConfig = (WebConfig) digester.parse(webconfXML);

        webConfig.validate(model);

        webConfig.setSubClassConfig(model);

        webConfig.loadLabelsFromMappingsFile(context, model);

        return webConfig;
    }

    /**
     * Get all the file names of properties files that configure class name mappings.
     * @param props the main configuration to look in.
     */
    private static List<String> getClassMappingFileNames(final Properties props) {
        return getMappingFileNames(props, "web.config.classname.mappings");
    }

    /**
     * Get all the file names of properties files that configure field name mappings.
     * @param props the main configuration to look in.
     */
    private static List<String> getFieldMappingFileNames(final Properties props) {
        return getMappingFileNames(props, "web.config.fieldname.mappings");
    }

    /**
     * Get all the files configured in a properties file with a certain prefix.
     * @param prefix The prefix to use to get the list of values.
     */
    private static List<String> getMappingFileNames(final Properties props, final String prefix) {
        final List<String> returnVal = new ArrayList<String>();
        for (@SuppressWarnings("rawtypes")
        final Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            final String key = (String) e.nextElement();
            if (key.startsWith(prefix)) {
                returnVal.add(props.getProperty(key));
            }
        }
        return returnVal;
    }

    /**
     * Load a set of files into a single merged properties file. These files should all be
     * located in the WEB-INF directory of the webapp war.
     *
     * @param fileNames The file names to load.
     * @throws FileNotFoundException If a file is listed but does not exist in WEB-INF.
     * @throws IllegalStateException If two files configure the same key.
     * @throws IOException if the properties cannot be loaded.
     */
    private static Properties loadMergedProperties(final List<String> fileNames,
        final ServletContext context)
        throws IOException {
        final Properties props = new Properties();
        for (final String fileName : fileNames) {
            LOG.info("Loading properties from " + fileName);
            final Properties theseProps = new Properties();
            final InputStream is = context.getResourceAsStream("/WEB-INF/" + fileName);
            if (is == null) {
                throw new FileNotFoundException("Could not find mappings file: " + fileName);
            }
            try {
                theseProps.load(is);
            } catch (final IOException e) {
                throw new Error("Problem reading from " + fileName, e);
            }
            if (!props.isEmpty()) {
                for (@SuppressWarnings("rawtypes")
                final Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                    final String key = (String) e.nextElement();
                    if (theseProps.containsKey(key)) {
                        throw new IllegalStateException(
                                "Duplicate label found for " + key + " in " + fileName);
                    }
                }
            }
            if (theseProps.isEmpty()) {
                LOG.info("No properties loaded from " + fileName);
            } else {
                LOG.info("Merging in " + theseProps.size() + " mappings from " + fileName);
                props.putAll(theseProps);
            }
        }
        return props;
    }


    /**
     * Load labels specified in any configured mapping files, and apply them to the
     * configuration for the appropriate classes and fields.
     * @param context The servlet context to use to find configuration with.
     * @param model The data model which lists our classes and fields.
     */
    private void loadLabelsFromMappingsFile(
        final ServletContext context,
        final Model model)
        throws IOException {

        final Properties webProperties = SessionMethods.getWebProperties(context);

        final List<String> classFileNames = getClassMappingFileNames(webProperties);
        final List<String> fieldFileNames = getFieldMappingFileNames(webProperties);

        final Properties fieldNameProperties = loadMergedProperties(fieldFileNames, context);
        final Properties classNameProperties = loadMergedProperties(classFileNames, context);

        for (final ClassDescriptor cd : model.getClassDescriptors()) {
            labelClass(cd, classNameProperties, fieldNameProperties);
        }
    }

    /**
     * Apply any labels configured in the property files to the class. This means
     * a label for the class itself, and labels for any of its fields.
     * @param cd a class descriptor specifying the class.
     * @param classNameProperties The mapping from our class names to a readable version
     * @param fieldNameProperties The mapping from our field names to a readable version
     */
    private void labelClass(
            final ClassDescriptor cd,
            final Properties classNameProperties,
            final Properties fieldNameProperties) {
        final String originalName = cd.getUnqualifiedName();
        if ("InterMineObject".equals(originalName)) {
            return;
        }
        Type classConfig = getTypes().get(cd.getName());
        if (classConfig == null) {
            classConfig = new Type();
            classConfig.setClassName(cd.getName());
            addType(classConfig);
        }

        if (classNameProperties.containsKey(originalName)) {
            final String classNameLabel = classNameProperties.getProperty(originalName);
            final String label = deSlashify(classNameLabel);
            if (classConfig.getLabel() == null) {
                LOG.info("Setting label as " + label + " on " + originalName);
                classConfig.setLabel(label);
            }
        }
        for (final FieldDescriptor fd : cd.getAllFieldDescriptors()) {
            if (fieldNameProperties.containsKey(fd.getName())) {
                final String fieldNameLabel = fieldNameProperties.getProperty(fd.getName());
                final String label = deSlashify(fieldNameLabel);
                FieldConfig fc = classConfig.getFieldConfigMap().get(fd.getName());
                if (fc == null) {
                    fc = new FieldConfig();
                    fc.setFieldExpr(fd.getName());
                    fc.setShowInSummary(false);
                    fc.setShowInInlineCollection(false);
                    fc.setShowInResults(false);
                    classConfig.addFieldConfig(fc);
                }
                if (fc.getLabel() == null) {
                    LOG.info("Setting label as " + label + " on " + fd.getName()
                        + " in " + originalName);
                    fc.setLabel(label);
                }
            }
        }
    }

    /**
     * Format strings in a SO format to be more human readable. For example,
     * transcription_factor becomes "Transcription Factor", and "U11_snRNA" becomes
     * "U11 snRNA".
     * @param input the string to format
     * @return A reformatted version of the string.
     */
    private static String deSlashify(final String input) {
        final String[] parts = StringUtils.split(input, "_");
        final String[] outputParts = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (part.equals(StringUtils.lowerCase(part))) {
                outputParts[i] = StringUtils.capitalize(part);
            } else {
                outputParts[i] = part;
            }
        }

        return StringUtils.join(outputParts, " ");
    }

    /**
     * Validate web config according to the model. Test that configured classes exist in
     * model and configured fields in web config exist in model.
     * @param model model used for validation
     */
    void validate(final Model model) {
        final StringBuffer invalidClasses = new StringBuffer();
        final StringBuffer badFieldExpressions = new StringBuffer();
        for (final String typeName : types.keySet()) {
            if (!model.getClassNames().contains(typeName)) {
                invalidClasses.append(" " + typeName);
                continue;
            }
            final Type type = types.get(typeName);
            for (final FieldConfig fieldConfig : type.getFieldConfigs()) {
                String pathString;
                try {
                    pathString = Class.forName(typeName).getSimpleName()
                        + "." + fieldConfig.getFieldExpr();
                } catch (final ClassNotFoundException e) {
                    final String msg = "Invalid web config. '"
                        + typeName + "' doesn't exist in the " + "model.";
                    LOG.warn(msg);
                    continue;
                }
                try {
                    new Path(model, pathString);
                } catch (final PathException e) {
                    badFieldExpressions.append(" " + pathString);
                    continue;
                }
            }
        }
        if (invalidClasses.length() > 0 || badFieldExpressions.length() > 0) {
            final String msg = "Invalid web config. "
                    + (invalidClasses.length() > 0
                            ? "Classes specified in web config that don't exist in model: "
                                    + invalidClasses.toString() + ". " : "")
                            + (badFieldExpressions.length() > 0
                                    ? "Path specified in a fieldExpr does note exist in model: "
                                            + badFieldExpressions + ". " : "");
            LOG.error(msg);
        }
    }

    /**
     * Validate the content (the paths) in the widget config
     * @param model the model used to validate the paths
     * @return the message containing the errors or an empty String
     */
    public String validateWidgetsConfig(final Model model) {
        WidgetConfig widget = null;
        StringBuffer validationMessage = new StringBuffer();
        for (String widgetId : widgets.keySet()) {
            widget = widgets.get(widgetId);
            //verify startClass
            String startClass = widget.getStartClass();
            if (startClass != null && !"".equals(startClass)) {
                startClass = model.getPackageName() + "." + widget.getStartClass();
                if (!model.getClassNames().contains(startClass)) {
                    validationMessage = validationMessage.append("The attribute startClass for the"
                                        + " widget " + widgetId + " is not in the model.");
                }
            }
            //verify typeClass
            String typeClass = widget.getTypeClass();
            if (typeClass != null && !"".equals(typeClass)) {
                if (!model.getClassNames().contains(widget.getTypeClass())) {
                    validationMessage = validationMessage.append("The attribute typeClass for the "
                                        + "widget " + widgetId + " is not in the model.");
                }
            }
            //verify constraints (only path)
            List<PathConstraint> pathConstraints = widget.getPathConstraints();
            for (PathConstraint pathConstraint : pathConstraints) {
                try {
                    new Path(model, widget.getStartClass() + "." + pathConstraint.getPath());
                } catch (final PathException e) {
                    validationMessage.append("The path " + pathConstraint.getPath()
                        + " set in the constraints for the widget " + widgetId
                        + " is not in the model.");
                }
            }
            //verify views
            String views = widget.getViews();
            String simpleStartClass = widget.getStartClass();
            if (views != null) {
                if (!"".equals(views)) {
                    String[] viewsBites = widget.getViews().split("\\s*,\\s*");
                    if (widget instanceof TableWidgetConfig) {
                        simpleStartClass = typeClass.substring(typeClass.lastIndexOf(".") + 1);
                    }
                    for (String viewPath : viewsBites) {
                        viewPath = simpleStartClass + "." + viewPath;
                        try {
                            new Path(model, viewPath);
                        } catch (final PathException e) {
                            validationMessage.append("The path " + viewPath + " set in the views "
                                + "for the widget " + widgetId + " is not in the model.");
                        }
                    }
                }
            }
            //verify enrich and enrichId for enrichement widgets
            if (widget instanceof EnrichmentWidgetConfig) {
                String enrich = ((EnrichmentWidgetConfig) widget).getEnrich();
                validatePath(model, widget.getStartClass(), enrich, "enrich", widgetId,
                             validationMessage);
                String enrichId = ((EnrichmentWidgetConfig) widget).getEnrichIdentifier();
                if (enrichId != null) {
                    validatePath(model, widget.getStartClass(), enrichId, "enrichIdentifier",
                                 widgetId, validationMessage);
                }
            }
            //verify categoryPath and seriesPath for graph widgets
            if (widget instanceof GraphWidgetConfig) {
                String categoryPath = ((GraphWidgetConfig) widget).getCategoryPath();
                validatePath(model, widget.getStartClass(), categoryPath, "categoryPath", widgetId,
                            validationMessage);
                String seriesPath = ((GraphWidgetConfig) widget).getSeriesPath();
                if (!"".equals(seriesPath) && !"ActualExpectedCriteria".equals(seriesPath)) {
                    validatePath(model, widget.getStartClass(), seriesPath, "seriesPath", widgetId,
                            validationMessage);
                }
            }
        }
        return validationMessage.toString();
    }

    private void validatePath(Model model, String startClass, String pathToValidate, String label,
                              String widgetId, StringBuffer validationMessage) {
        try {
            new Path(model, startClass + "." + pathToValidate);
        } catch (final PathException e) {
            validationMessage.append("The attribute " + label + " " + pathToValidate
                + " set for the widget " + widgetId + " is not in the model.");
        }
    }

    /**
     * Add a type to the WebConfig Map.  Use className as the key of the Map if fieldName of the
     * Type is null, otherwise use the class name, a space, and the field name.
     *
     * @param type the Type to add
     */
    public void addType(final Type type) {
        String typeString = type.getClassName();
        if (types.containsKey(typeString)) {
            throw new IllegalArgumentException("Type " + typeString
                    + " defined more than once in webconfig-model.xml");
        } else {
            types.put(type.getClassName(), type);
        }
    }

    /**
     * Get a map from fully qualified class name to the Type config for that class
     * @return the types
     */
    public Map<String, Type> getTypes() {
        return types;
    }

    /**
     * Return a FieldConfigs for a particular class or an empty list if no config is defined.
     * @param clsName the class to fetch field configs for
     * @return the FieldConfigs or an empty collection
     */
    public Collection<FieldConfig> getFieldConfigs(String clsName) {
        Type type = types.get(clsName);
        if (type != null) {
            return type.getFieldConfigs();
        }
        return Collections.emptyList();
    }

    /**
     * Return the FieldConfig for a particular field of the specified field, or null if field not
     * configured.
     * @param clsName the class to fetch field config for
     * @param fieldName the field to fetch config for
     * @return Collection<FieldConfig>
     */
    public FieldConfig getFieldConfig(String clsName, String fieldName) {
        Type type = types.get(clsName);
        if (type != null) {
            return type.getFieldConfig(fieldName);
        }
        return null;
    }

    /**
     * @return the widgets - a map from widget name to config details.
     */
    public Map<String, WidgetConfig> getWidgets() {
        return widgets;
    }

    /**
     * @param widget the widget
     */
    public void addWidget(final WidgetConfig widget) {
        widgets.put(widget.getId(), widget);
        final String[] widgetTypes = widget.getTypeClass().split(",");
        for (final String widgetType: widgetTypes) {
            final Type type = types.get(widgetType);
            if (type == null) {
                final String msg = "Invalid web config. " + widgetType + " is not a valid class. "
                    + "Please correct the entry in the webconfig-model.xml for the "
                    + widget.getId() + " widget.";
                LOG.warn(msg);
            } else {
                type.addWidget(widget);
            }
        }
    }

    /**
     * Add config for a report page displayer.  This checks that a type has been specified
     * before adding the config.
     * @param reportDisplayerConfig config for an individual report page displayer
     */
    public void addReportDisplayer(final ReportDisplayerConfig reportDisplayerConfig) {
        final Set<String> displayForTypes = reportDisplayerConfig.getConfiguredTypes();
        if (displayForTypes.isEmpty()) {
            LOG.error("Report displayer: " + reportDisplayerConfig.getJavaClass() + "/"
                    + reportDisplayerConfig.getJspName() + " is not configured for any types");
        } else {
            reportDisplayerConfigs.add(reportDisplayerConfig);
        }
    }


    /**
     * Fetch config for the report page displayers.
     * @return report page displayer config in the order specified in the config file
     */
    public List<ReportDisplayerConfig> getReportDisplayerConfigs() {
        return reportDisplayerConfigs;
    }

    /**
     * Add an TableExportConfig to the Map of TableExportConfig objects using
     * tableExportConfig.getId() as the Map key.
     * @param tableExportConfig the TableExportConfig to add
     */
    public void addTableExportConfig(final TableExportConfig tableExportConfig) {
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
    public boolean equals(final Object obj) {
        if (!(obj instanceof WebConfig)) {
            return false;
        }

        final WebConfig webConfigObj = (WebConfig) obj;

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
     * subclasses that don't already have any configuration and sometimes when they do.
     * This method has package scope so that it can be called from the tests.
     *
     * @param model the Model to use to find sub-classes
     * @throws ClassNotFoundException if any of the classes mentioned in the XML file aren't in the
     * Model
     */
    void setSubClassConfig(final Model model) throws ClassNotFoundException {

        for (final Iterator<ClassDescriptor> modelIter
                = model.getTopDownLevelTraversal().iterator(); modelIter.hasNext();) {

            final ClassDescriptor cld = modelIter.next();
            Type thisClassType = types.get(cld.getName());

            if (thisClassType == null) {
                thisClassType = new Type();
                thisClassType.setClassName(cld.getName());
                types.put(cld.getName(), thisClassType);
            }

            final Set<ClassDescriptor> cds
                = model.getClassDescriptorsForClass(Class.forName(cld.getName()));
            for (final ClassDescriptor cd : cds) {
                if (cld.getName().equals(cd.getName())) {
                    continue;
                }

                final Type superClassType = types.get(cd.getName());

                if (superClassType != null) {
                    // set title config, the setter itself only adds configs that have not been set
                    // before, see setTitles() in HeaderConfig
                    final HeaderConfigTitle hc = superClassType.getHeaderConfigTitle();
                    if (hc != null) {
                        // set the HeaderConfig titles as HeaderConfig for thisClassType might have
                        //  been configured
                        final HashMap<String, List<HeaderConfigTitle.TitlePart>> titles =
                            hc.getTitles();
                        if (titles != null) {
                            // new childish HeaderConfig
                            final HeaderConfigTitle subclassHc =
                                thisClassType.getHeaderConfigTitle();
                            if (subclassHc != null) {
                                // type A behavior: inherit titles from the parent and append
                                if (subclassHc.getAppendConfig()) {
                                    subclassHc.addTitleParts(hc.getTitles());
                                }
                            } else {
                                // type B behavior: inherit from parent if we are null
                                thisClassType.addHeaderConfigTitle(hc);
                            }
                        }
                    }
                    if (thisClassType.getFieldConfigs().size() == 0) {
                        // copy any FieldConfigs from the super class
                        for (final FieldConfig fc : superClassType.getFieldConfigs()) {
                            thisClassType.addFieldConfig(fc);
                        }
                    } else {
                        // Set labels on overridden field-configs without labels
                        for (final FieldConfig superfc : superClassType.getFieldConfigs()) {
                            for (final FieldConfig thisfc : thisClassType.getFieldConfigs()) {
                                if (thisfc.getFieldExpr().equals(superfc.getFieldExpr())) {
                                    if (superfc.getLabel() != null && thisfc.getLabel() == null) {
                                        thisfc.setLabel(superfc.getLabel());
                                    }
                                }
                            }
                        }
                    }

                    if (thisClassType.getLongDisplayers().size() == 0) {
                        @SuppressWarnings("rawtypes")
                        final Iterator longDisplayerIter
                            = superClassType.getLongDisplayers().iterator();

                        while (longDisplayerIter.hasNext()) {
                            final Displayer ld = (Displayer) longDisplayerIter.next();
                            thisClassType.addLongDisplayer(ld);
                        }
                    }

                    if (thisClassType.getTableDisplayer() == null) {
                        thisClassType.setTableDisplayer(superClassType.getTableDisplayer());
                    }

                    if (thisClassType.getWidgets().size() == 0
                            && superClassType.getWidgets() != null
                            && superClassType.getWidgets().size() > 0) {
                        @SuppressWarnings("rawtypes")
                        final Iterator widgetIter = superClassType.getWidgets().iterator();

                        while (widgetIter.hasNext()) {
                            final WidgetConfig wi = (WidgetConfig) widgetIter.next();
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
        final StringBuffer sb = new StringBuffer();
        sb.append("<webconfig>");
        final Iterator<Type> typesIter = types.values().iterator();
        while (typesIter.hasNext()) {
            sb.append(typesIter.next().toString() + "\n");
        }
        final Iterator<TableExportConfig> tableExportConfigIter
            = tableExportConfigs.values().iterator();
        while (tableExportConfigIter.hasNext()) {
            sb.append(tableExportConfigIter.next().toString());
        }
        sb.append("</webconfig>");
        return sb.toString();
    }

}
