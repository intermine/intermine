package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.metadata.StringUtil;
import org.intermine.web.displayer.DisplayerManager;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.HeaderConfigLink;
import org.intermine.web.logic.config.HeaderConfigTitle;
import org.intermine.web.logic.config.InlineListConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;

/**
 * Object to be displayed on report.do
 *
 * @author Radek Stepan
 * @author Richard Smith
 */
public class ReportObject
{
    private InterMineObject object;
    private final WebConfig webConfig;
    /** unqualified (!!) object type string */
    private final String objectType;

    private Map<String, Object> fieldValues;

    private InterMineAPI im;

    /**
     * @List<ReportObjectField> setup list of summary fields this object has
     */
    private List<ReportObjectField> objectSummaryFields;

    /** @var List header inline lists set by the WebConfig */
    private List<InlineList> inlineListsHeader = null;
    /** @var List of 'unplaced' normal InlineLists */
    private List<InlineList> inlineListsNormal = null;

    private Map<String, Object> attributes = null;
    private Map<String, FieldDescriptor> attributeDescriptors = null;
    private Map<String, DisplayReference> references = null;
    private Map<String, DisplayCollection> collections = null;
    private Map<String, DisplayField> refsAndCollections = null;
    private Map<String, Map<String, TitleValue>> headerTitles = null;
    private Set<String> replacedFieldExprs = null;

    private HeaderConfigLink headerLink;
    private String pageTitle = null;

    /** @var Set of References & Collections that will always be 0 for this type of object */
    private Set<String> nullRefsCols;

    /**
     * @var webProperties so we can resolve # of rows to show in Collections
     * @see DisplayReference/DisplayCollection -> DisplayField -> getTable()
     */
    private Properties webProperties;

    private static final Logger LOG = Logger.getLogger(ReportObject.class);

    /**
     * Setup internal ReportObject
     * @param object InterMineObject
     * @param webConfig WebConfig
     * @param im InterMineAPI
     * @param webProperties web properties config
     * @throws Exception Exception
     */
    public ReportObject(InterMineObject object, WebConfig webConfig, InterMineAPI im,
            Properties webProperties) throws Exception {
        this.object = object;
        this.webConfig = webConfig;
        this.im = im;
        this.webProperties = webProperties;

        // infer dynamic type of IM object
        this.objectType = DynamicUtil.getSimpleClass(object).getSimpleName();
    }

    /**
     * Get a map from placement (header, summary or a data category) to custom report displayers for
     * that placement.
     * @return map from placement to displayers
     */
    public Map<String, List<ReportDisplayer>> getReportDisplayers() {
        DisplayerManager displayerManager = DisplayerManager.getInstance(webConfig, im);
        return displayerManager.getReportDisplayersForType(objectType);
    }

    /**
     * Give us a displayer by a specific name, called by AjaxServices
     * @param name displayer name
     * @return ReportDisplayer
     */
    public ReportDisplayer getReportDisplayer(String name) {
        DisplayerManager displayerManager = DisplayerManager.getInstance(webConfig, im);
        return displayerManager.getReportDisplayerByName(objectType, name);
    }

    /**
     * Get the id of this object
     * @return the id
     */
    public int getId() {
        return object.getId().intValue();
    }

    /**
     * Get the attribute fields and values for this object
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            initialise();
        }
        return attributes;
    }

    /**
     * Get the class descriptor for this object
     * @return one class descriptor
     */
    public ClassDescriptor getClassDescriptor() {
        return im.getModel().getClassDescriptorByName(objectType);
    }

    /**
     * Get the collection fields and values for this object
     * @return the collections
     */
    public Map<String, DisplayCollection> getCollections() {
        if (collections == null) {
            initialise();
        }
        return collections;
    }

    private String stripTail(String input) {
        Integer dot = input.indexOf(".");
        if (dot > 0) {
            return input.substring(0, dot);
        }
        return input;
    }

    /**
     * A listing of object fields as pieced together from the various ReportObject methods
     * @return <ReportObjectField>s List
     */
    public List<ReportObjectField> getObjectSummaryFields() {
        long startTime = System.currentTimeMillis();
        // are we setup yet?
        if (objectSummaryFields == null) {
            objectSummaryFields = new ArrayList<ReportObjectField>();
            List<ReportObjectField> objectOtherSummaryFields = new ArrayList<ReportObjectField>();

            // to make sure we do not show fields that are replaced elsewhere
            Set<String> replacedFields = getReplacedFieldExprs();

            // temporary track of fieldConfigs so we know which attributes were missed out
            Set<String> fieldConfigPaths = new HashSet<String>();

            //  1. add all fields configured with showInSummary=true
            for (FieldConfig fc : getFieldConfigs()) {
                // get fieldName
                String fieldName = fc.getFieldExpr();

                if (!replacedFields.contains(stripTail(fieldName))) {

                    Object fieldValue = getFieldValue(fieldName);
                    String fieldDisplayer = fc.getDisplayer();

                    if (!isAttribute(fieldName)
                            && fieldDisplayer == null
                            && !fc.getShowInSummary()) {
                        continue; // This is just configured for its label
                    }

                    ReportObjectField rof = new ReportObjectField(
                            objectType,
                            fieldName,
                            fieldValue,
                            fieldDisplayer,
                            fc.getDoNotTruncate(),
                            fc.getEscapeXml(),
                            fc.getLabel()
                    );

                    if (!fc.getHide()) {
                        // summary fields should go first
                        if (fc.getShowInSummary()) {
                            objectSummaryFields.add(rof);
                        } else { // show in summary also, but not right now...
                            objectOtherSummaryFields.add(rof);
                        }
                    }
                    fieldConfigPaths.add(fc.getFieldExpr());
                }
            }

            // 2. then add configured fields that don't have showInSummary=true
            objectSummaryFields.addAll(objectOtherSummaryFields);

            // 3. any attributes not configured at all are shown last
            if (attributes != null) {
                for (String attName : attributes.keySet()) {
                    if (!fieldConfigPaths.contains(attName) && !replacedFields.contains(attName)) {
                        ReportObjectField rof = new ReportObjectField(
                                objectType,
                                attName,
                                attributes.get(attName),
                                null,
                                false,
                                false
                        );
                        objectSummaryFields.add(rof);
                    }
                }
            }

            // 4. remove fields that are resolved IN titles
            Map<String, TitleValue> mainTitles = getTitleMain();
            Map<String, TitleValue> subTitles = getTitleSub();
            Set<String> allTitles = new HashSet<String>();
            if (mainTitles != null) {
                allTitles.addAll(mainTitles.keySet());
            }
            if (subTitles != null) {
                allTitles.addAll(subTitles.keySet());
            }
            if (allTitles.size() > 0) {
                List<ReportObjectField> temp = new ArrayList<ReportObjectField>();
                for (ReportObjectField rof : objectSummaryFields) {
                    String rofName = rof.getName();
                    if (!allTitles.contains(rofName)) {
                        temp.add(rof);
                    }
                }
                objectSummaryFields = temp;
            }
            long endTime = System.currentTimeMillis();
            LOG.info("TIME objectSummaryFields creation took: " + (endTime - startTime) + "ms");
        } else {
            long endTime = System.currentTimeMillis();
            LOG.info("TIME objectSummaryFields already set, took: " + (endTime - startTime) + "ms");
        }
        return objectSummaryFields;
    }

    /**
     * Get InterMine object
     * @return InterMineObject
     */
    public InterMineObject getObject() {
        return object;
    }

    /**
     * Return a list of field configs
     * @return Collection<FieldConfig>
     */
    public Collection<FieldConfig> getFieldConfigs() {
        String qualifiedType = DynamicUtil.getSimpleClass(object).getName();
        return webConfig.getFieldConfigs(qualifiedType);
    }

    /**
     * Return a string to display as the name of a field.
     * @param fieldExpression The name of the field as configured in webconfig-model.xml
     * @return the field's label, or the field's name.
     */
    public String getFieldDisplayName(String fieldExpression) {
        Collection<FieldConfig> configs = getFieldConfigs();
        for (FieldConfig fc: configs) {
            if (fc.getFieldExpr() != null && fc.getFieldExpr().equals(fieldExpression)) {
                if (fc.getLabel() != null) {
                    return fc.getLabel();
                } else {
                    return fieldExpression;
                }
            }
        }
        return fieldExpression;
    }

    /**
     * Get field value for a field name (expression)
     * @param fieldExpression String
     * @return Object
     */
    public Object getFieldValue(String fieldExpression) {
        // if field values as a whole are not set yet...
        if (fieldValues == null) {
            setupFieldValues();
        }
        // return a field value for a field expression (name)
        return fieldValues.get(fieldExpression);
    }

    private boolean isAttribute(String fieldName) {
        Path p = getPathForField(fieldName);
        return p.endIsAttribute();
    }

    private boolean isCollection(String fieldName) {
        Path p = getPathForField(fieldName);
        return p.endIsCollection();
    }

    private Path getPathForField(String fieldName) {
        String pathString = objectType + "." + fieldName;
        Path p;
        try {
            p = new Path(im.getModel(), pathString);
        } catch (PathException e) {
            throw new Error(e);
        }
        return p;
    }

    /**
     * Setup fieldValues HashMap
     */
    protected void setupFieldValues() {
        // create a new map
        fieldValues = new HashMap<String, Object>();

        // fetch field configs
        for (FieldConfig fc : getFieldConfigs()) {
            // create a path string
            if (!isCollection(fc.getFieldExpr())) {
                String pathString = objectType + "." + fc.getFieldExpr();
                try {
                    fieldValues.put(fc.getFieldExpr(), resolvePath(pathString));
                } catch (PathException e) {
                    throw new Error("There must be a bug", e);
                }
            }
        }
    }

    private Object resolvePath(String pathString) throws PathException {
        Path path = new Path(im.getModel(), pathString);
        return PathUtil.resolvePath(path, object);
    }

    /**
     * Get the unqualified class name for the report object.
     * @return an unqualified class name
     */
    public String getType() {
        return objectType;
    }

    /**
     * Fetch a header string to use as the page title.  The title will be type of the object plus
     * the main and sub titles if configured, otherwise any value fetched from a class key.
     *
     * NOTE: This is called from HtmlHeadController so will block the page starting to load until
     * complete, avoid any unnecessary computation.
     *
     * @return a title string for the page
     */
    public String getHtmlHeadTitle() {
        if (pageTitle == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.objectType);
            sb.append(" ");

            Map<String, TitleValue> mainTitles = getTitles(HeaderConfigTitle.MAIN);
            if (!mainTitles.values().isEmpty()) {
                for (TitleValue mainTitle : mainTitles.values()) {
                    sb.append(mainTitle.getUnformatted());
                    sb.append(" ");
                }
            } else {
                // no title configured so attempt to get a value from a class key
                Object keyFieldValue = ClassKeyHelper.getKeyFieldValue(object, im.getClassKeys());
                if (keyFieldValue != null) {
                    sb.append(keyFieldValue);
                }
            }

            Map<String, TitleValue> subTitles = getTitles(HeaderConfigTitle.SUB);
            for (TitleValue subTitle : subTitles.values()) {
                sb.append(subTitle.getUnformatted());
                sb.append(" ");
            }
            pageTitle = sb.toString().trim();
        }

        return pageTitle;
    }

    /**
     * Used by JSP
     * @return the main title of this object, i.e.: "eve FBgn0000606" as a Map
     */
    public Map<String, TitleValue> getTitleMain() {
        return getTitles(HeaderConfigTitle.MAIN);
    }

    /**
     * Used by JSP
     * @return the subtitle of this object, i.e.: "D. melanogaster" as a Map
     */
    public Map<String, TitleValue> getTitleSub() {
        return getTitles(HeaderConfigTitle.SUB);
    }

    /**
     * Get the main or sub title of the page.
     * @param key: main|sub
     * @return the titles string as resolved based on the path(s) under key
     */
    private Map<String, TitleValue> getTitles(String key) {
        if (headerTitles == null) {
            headerTitles = new HashMap<String, Map<String, TitleValue>>();
            Type type = webConfig.getTypes().get(DynamicUtil.getSimpleClassName(object));
            HeaderConfigTitle hc = type.getHeaderConfigTitle();

            for (String part : HeaderConfigTitle.TYPES) {
                headerTitles.put(part, new LinkedHashMap<String, TitleValue>());
                if (hc != null) {
                    for (HeaderConfigTitle.TitlePart tp : hc.getTitles().get(part)) {
                        Object fieldValue = getFieldValue(tp.getPath());
                        // maybe not in FieldConfigs - try just resolving the path
                        if (fieldValue == null) {
                            try {
                                fieldValue = resolvePath(objectType + "." + tp.getPath());
                            } catch (PathException e) {
                                LOG.warn("Error resolving path '" + tp.getPath()
                                        + "' in titles config for: " + objectType);
                            }
                        }
                        if (fieldValue != null) {
                            // Store both formatted and unformatted versions of the title
                            String fieldValueStr = fieldValue.toString();
                            if (StringUtils.isNotBlank(fieldValueStr)) {
                                String formatted =
                                    tp.getPrefix() + fieldValueStr + tp.getSuffix();
                                TitleValue tv = new TitleValue(formatted, fieldValueStr);
                                headerTitles.get(part).put(tp.getPath(), tv);
                            }
                        }
                        // There is optional configuration for a maximum number of main header parts
                        if (part.equals(HeaderConfigTitle.MAIN)) {
                            if (headerTitles.get(HeaderConfigTitle.MAIN).size()
                                    >= hc.getNumberOfMainTitlesToShow()) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (!headerTitles.containsKey(key)) {
            LOG.error("Requested invalid title part '" + key + "', valid parts are: "
                    + StringUtil.prettyList(Arrays.asList(HeaderConfigTitle.TYPES)));
            return null;
        }
        return headerTitles.get(key);
    }

    /**
     * Holder for formatted and unformatted versions of a title part.
     */
    public class TitleValue
    {
        private final String formatted;
        private final String unformatted;

        /**
         * Construct with two versions of title part.
         * @param formatted with formatting, e.g. italics
         * @param unformatted just the string
         */
        TitleValue(String formatted, String unformatted) {
            this.formatted = formatted;
            this.unformatted = unformatted;
        }

        /**
         * Get the formatted version, with e.g. italics tags.
         * @return formatted title part
         */
        public String getFormatted() {
            return formatted;
        }

        /**
         * Get the unformatted version that is just the field value.
         * @return unformatted title part
         */
        public String getUnformatted() {
            return unformatted;
        }
    }

    /**
     *
     * @return a resolved link
     */
    public HeaderConfigLink getHeaderLink() {
        long startTime = System.currentTimeMillis();
        if (this.headerLink == null) {
            // fetch the Type
            Type type = webConfig.getTypes().get(getClassDescriptor().getName());
            // retrieve the titles map, HeaderConfig serves as a useless wrapper
            HeaderConfigLink link = type.getHeaderConfigLink();

            if (link != null) {

                // link URL
                String linkUrl = link.getUrl();

                if (linkUrl != null) {
                    // patternz
                    final Pattern linkPattern = Pattern.compile("\\{(.*?)\\}");
                    final Matcher m = linkPattern.matcher(linkUrl);
                    while (m.find()) {
                        // get the field name and do some filtering just in case
                        final String path = m.group(1).replaceAll("[^a-zA-Z.]", "");
                        // resolve the field value
                        Object stuff = getFieldValue(path);
                        if (stuff != null) {
                            String stringyStuff = stuff.toString();
                            // String.isEmpty() was introduced in Java release 1.6
                            if (StringUtils.isNotBlank(stringyStuff)) {
                                // replace the field with the value & update
                                link.setUrl(linkUrl.replace("{" + path + "}", stringyStuff));
                                this.headerLink = link;
                            }
                        }
                    }
                }
            }
            long endTime = System.currentTimeMillis();
            LOG.info("TIME getHeaderLink creation took: " + (endTime - startTime) + "ms");
        } else {
            long endTime = System.currentTimeMillis();
            LOG.info("TIME getHeaderLink already set, took: " + (endTime - startTime) + "ms");
        }
        return this.headerLink;
    }

    /**
     * The said function will resolve the maximum number of rows to show (in Collections)
     *  from webProperties.
     * @return Integer duh
     */
    public Integer getNumberOfTableRowsToShow() {
        String maxInlineTableSizeString =
            (String) webProperties.get(Constants.INLINE_TABLE_SIZE);
        try {
            return Integer.parseInt(maxInlineTableSizeString);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse " + Constants.INLINE_TABLE_SIZE + " property: "
                     + maxInlineTableSizeString);
        }
        return null;
    }

    /**
     * Resolve an InlineList by filling it up with a list of list objects, part of initialise()
     * @param listConfig retrieved from Type
     * @param bagOfInlineListNames is a bag of names of lists we have resolved so far so these
     *        fields are not resolved elsewhere and skipped instead
     * @see setDescriptorOnInlineList() is still needed when traversing FieldDescriptors
     */
    private void initialiseInlineList(InlineListConfig listConfig,
            HashMap<String, Boolean> bagOfInlineListNames) {
        long startTime = System.currentTimeMillis();
        // soon to be list of values
        Set<Object> listOfListObjects = null;
        String columnToDisplayBy = null;
        InlineList list = null;
        try {
            // create a new path to the collection of objects
            Path path = new Path(im.getModel(),
                    DynamicUtil.getSimpleClass(object.getClass()).getSimpleName()
                    + '.' + listConfig.getPath());
            try {
                // save the suffix, the value we will show the list by
                columnToDisplayBy = path.getLastElement();
                // create only a prefix of the path so we have
                //  Objects and not just Strings
                path = path.getPrefix();
            } catch (Error e) {
                throw new RuntimeException("You need to specify a key to display"
                        + "the list by, not just the root element.");
            }
            // resolve path to a collection and save into a new list
            listOfListObjects = PathUtil.resolveCollectionPath(path, object);
            list = new InlineList(
                    listOfListObjects,
                    columnToDisplayBy,
                    listConfig.getShowLinksToObjects(),
                    listConfig.getPath(),
                    listConfig.getLineLength());

        } catch (PathException e) {
            throw new RuntimeException("Your collections of inline lists"
                    + "are failing you", e);
        }

        // place the list
        if (listConfig.getShowInHeader()) {
            inlineListsHeader.add(list);
        } else {
            inlineListsNormal.add(list);
        }

        // save name of the collection
        String path = listConfig.getPath();
        bagOfInlineListNames.put(path.substring(0, path.indexOf('.')), true);
        long endTime = System.currentTimeMillis();
        LOG.info("TIME initialiseInlineLists took: " + (endTime - startTime) + "ms");
    }

    /**
     * Resolve an attribute, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseAttribute(FieldDescriptor fd) {
        long startTime = System.currentTimeMillis();
        // bags
        attributes = (attributes != null) ? attributes
                : new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        attributeDescriptors = (attributeDescriptors != null) ? attributeDescriptors
                : new HashMap<String, FieldDescriptor>();

        Object fieldValue = null;
        try {
            fieldValue = object.getFieldValue(fd.getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (fieldValue != null) {
            if (fieldValue instanceof ClobAccess) {
                ClobAccess fieldClob = (ClobAccess) fieldValue;
                if (fieldClob.length() > 200) {
                    fieldValue = fieldClob.subSequence(0, 200).toString();
                } else {
                    fieldValue = fieldClob.toString();
                }
            }

            attributes.put(fd.getName(), fieldValue);
            attributeDescriptors.put(fd.getName(), fd);
        }
        long endTime = System.currentTimeMillis();
        LOG.info("TIME initialiseAttribute " + fd.getName() + " took: " + (endTime - startTime)
                + "ms");
    }

    /**
     * Resolve a Reference, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseReference(FieldDescriptor fd) {
        long startTime = System.currentTimeMillis();
        // bag
        references = (references != null) ? references
                : new TreeMap<String, DisplayReference>(String.CASE_INSENSITIVE_ORDER);

        ReferenceDescriptor ref = (ReferenceDescriptor) fd;

        String refName = ref.getName();
        // do not bother with 'em if they WILL be size 0
        if (!nullRefsCols.contains(refName)) {
            // check whether reference is null without dereferencing
            Object proxyObject = null;
            ProxyReference proxy = null;
            try {
                proxyObject = object.getFieldProxy(refName);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            }
            if (proxyObject instanceof org.intermine.objectstore.proxy.ProxyReference) {
                proxy = (ProxyReference) proxyObject;
            } else {
                // no go on objects that are not Proxies, ie Tests
            }
            DisplayReference newReference = null;
            try {
                newReference = new DisplayReference(proxy, ref, webConfig, im.getClassKeys(),
                        objectType);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (newReference != null) {
                references.put(refName, newReference);
            }
        }
        long endTime = System.currentTimeMillis();
        LOG.info("TIME initialiseReference " + fd.getName() + " took: " + (endTime - startTime)
                + "ms");
    }

    /**
     * Resolve a Collection, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseCollection(FieldDescriptor fd) {
        long startTime = System.currentTimeMillis();
        // bag
        collections = (collections != null) ? collections
                : new TreeMap<String, DisplayCollection>(String.CASE_INSENSITIVE_ORDER);

        String colName = fd.getName();
        // do not bother with 'em if they WILL be size 0
        if (!nullRefsCols.contains(colName)) {
            Object fieldValue = null;
            try {
                fieldValue = object.getFieldValue(colName);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            long queryStartTime = System.currentTimeMillis();
            // determine the types in the collection
            List<Class<?>> listOfTypes = PathQueryResultHelper.
                    queryForTypesInCollection(object, colName, im.getObjectStore());
            long queryTime = System.currentTimeMillis() - queryStartTime;
            LOG.info("TIME - query for types in collection: " + colName + " took: " + queryTime);

            DisplayCollection newCollection = null;
            try {
                newCollection = new DisplayCollection((Collection<?>) fieldValue,
                        (CollectionDescriptor) fd, webConfig, webProperties, im.getClassKeys(),
                        listOfTypes, objectType);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (newCollection != null) {
                collections.put(colName, newCollection);
            }
        }
        long endTime = System.currentTimeMillis();
        LOG.info("TIME initialiseCollection " + fd.getName() + " took: " + (endTime - startTime)
                + "ms");
    }

    /**
     * Create the Maps and Lists returned by the getters in this class.
     */
    private void initialise() {
        // TODO don't initialise replaced collections!  Work this out first.

        long startTime = System.currentTimeMillis();
        // combined Map of References & Collections
        refsAndCollections = new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER);

        // Ensure not null
        inlineListsHeader = (inlineListsHeader != null)
                ? inlineListsHeader : new ArrayList<InlineList>();
        inlineListsNormal = (inlineListsNormal != null)
                ? inlineListsNormal : new ArrayList<InlineList>();

        Type type = webConfig.getTypes().get(getClassDescriptor().getName());
        // init lists from WebConfig Type
        List<InlineListConfig> inlineListsWebConfig = type.getInlineListConfig();
        // a map of inlineList object names so we do not include them elsewhere
        HashMap<String, Boolean> bagOfInlineListNames = new HashMap<String, Boolean>();
        // fill up
        for (InlineListConfig listConfig : inlineListsWebConfig) {
            initialiseInlineList(listConfig, bagOfInlineListNames);
        }

        /** Attributes, References, Collections through FieldDescriptors **/
        nullRefsCols =
            im.getObjectStoreSummary()
                .getNullReferencesAndCollections(getClassDescriptor().getName());

        Set<String> replacedFields = getReplacedFieldExprs();
        for (FieldDescriptor fd : getClassDescriptor().getAllFieldDescriptors()) {
            // only continue if we have not included this object in an inline list
            if (!bagOfInlineListNames.containsKey(fd.getName())
                    && !replacedFields.contains(fd.getName())) {
                if (fd.isAttribute() && !"id".equals(fd.getName())) {
                    /** Attribute **/
                    initialiseAttribute(fd);
                } else if (fd.isReference()) {
                    /** Reference **/
                    initialiseReference(fd);
                } else if (fd.isCollection()) {
                    /** Collection **/
                    initialiseCollection(fd);
                }
            } else {
                /** InlineList (cont...) **/
                // assign Descriptor from FieldDescriptors to the InlineList
                setDescriptorOnInlineList(fd.getName(), fd);
            }
        }

        // make a combined Map
        if (references != null) {
            refsAndCollections.putAll(references);
        }
        if (collections != null) {
            refsAndCollections.putAll(collections);
        }
        long endTime = System.currentTimeMillis();
        LOG.info("TIME initialise took: " + (endTime - startTime) + "ms");
    }

    /**
     * Get all the reference and collection fields and values for this object
     * @return the collections
     */
    public Map<String, DisplayField> getRefsAndCollections() {
        if (refsAndCollections == null) {
            initialise();
        }
        return refsAndCollections;
    }

    /**
     * Get fields and paths that are replaced by custom report displayers and should not be shown
     * in the report page.
     * @return fields that should not be shown
     */
    public Set<String> getReplacedFieldExprs() {
        if (replacedFieldExprs == null) {
            replacedFieldExprs = new HashSet<String>();
            for (ReportDisplayer reportDisplayer : getAllReportDisplayers()) {
                replacedFieldExprs.addAll(reportDisplayer.getReplacedFieldExprs());
            }
        }
        return replacedFieldExprs;
    }

    private Set<ReportDisplayer> getAllReportDisplayers() {
        DisplayerManager displayerManager = DisplayerManager.getInstance(webConfig, im);
        String clsName = DynamicUtil.getSimpleClass(object).getSimpleName();
        return displayerManager.getAllReportDisplayersForType(clsName);
    }

    /**
     * Get attribute descriptors.
     * @return map of attribute descriptors
     */
    public Map<String, FieldDescriptor> getAttributeDescriptors() {
        if (attributeDescriptors == null) {
            initialise();
        }
        return attributeDescriptors;
    }

    /**
     * Set Descriptor (for placement) on an InlineList, only done for normal lists
     * @param name
     * @param fd
     */
    private void setDescriptorOnInlineList(String name, FieldDescriptor fd) {
    done:
        for (InlineList list : inlineListsNormal) {
            Object path = list.getPath();
            if (((String) path).substring(0, ((String) path).indexOf('.')).equals(name)) {
                list.setDescriptor(fd);
                break done;
            }
        }
    }

    /**
    *
    * @return InlineLists that are resolved into their respective placements
    */
    public List<InlineList> getNormalInlineLists() {
        if (inlineListsNormal == null) {
            initialise();
        }
        return inlineListsNormal;
    }

    /**
     *
     * @return InlineLists to be shown in the header
     */
    public List<InlineList> getHeaderInlineLists() {
        if (inlineListsHeader == null) {
            initialise();
        }
        return inlineListsHeader;
    }

    /**
     * Used from JSP
     * @return true if we have inlineListsHeader
     */
    public Boolean getHasHeaderInlineLists() {
        return (getHeaderInlineLists() != null);
    }

    /**
     * Used from JSP
     * @return true if we have InlineLists with no placement yet
     */
    public Boolean getHasNormalInlineLists() {
        return (getNormalInlineLists() != null);
    }

}
