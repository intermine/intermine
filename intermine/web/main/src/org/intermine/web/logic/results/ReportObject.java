package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.intermine.web.displayer.DisplayerManager;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.HeaderConfigLink;
import org.intermine.web.logic.config.HeaderConfigTitle;
import org.intermine.web.logic.config.InlineList;
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

    private HeaderConfigLink headerLink;

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
                            fc.getLabel()
                    );

                    // summary fields should go first
                    if (fc.getShowInSummary()) {
                        objectSummaryFields.add(rof);
                    } else { // show in summary also, but not right now...
                        objectOtherSummaryFields.add(rof);
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
                                false
                        );
                        objectSummaryFields.add(rof);
                    }
                }
            }

            // 4. remove fields that are resolved IN titles
            Map<String, String> mainTitles = getTitleMain();
            Map<String, String> subTitles = getTitleSub();
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
     * Used by JSP
     * @return the main title of this object, i.e.: "eve FBgn0000606" as a Map
     */
    public Map<String, String> getTitleMain() {
        return getTitles("main");
    }

    /**
     * Used by JSP
     * @return the subtitle of this object, i.e.: "D. melanogaster" as a Map
     */
    public Map<String, String> getTitleSub() {
        return getTitles("sub");
    }

    /**
     * Get a title based on the type key we pass it
     * @param key: main|subre
     * @return the titles string as resolved based on the path(s) under key
     */
    private Map<String, String> getTitles(String key) {
        // fetch the Type
        Type type = webConfig.getTypes().get(getClassDescriptor().getName());
        // retrieve the titles map, HeaderConfig serves as a useless wrapper
        HeaderConfigTitle hc = type.getHeaderConfigTitle();
        if (hc != null) {
            Map<String, LinkedHashMap<String, Object>> titles = hc.getTitles();
            // if we have something saved
            if (titles != null && titles.containsKey(key)) {
                Map<String, String> result = new LinkedHashMap<String, String>();
                // specify the maximum number of values to show
                Integer maxCount = ("main".equals(key)
                        && hc.getNumberOfMainTitlesToShow() != null)
                        ? hc.getNumberOfMainTitlesToShow() : 666;
                Integer count = 0;
                // concatenate a space delineated title together as resolved from FieldValues
                Iterator<String> itr = titles.get(key).keySet().iterator();
                while (itr.hasNext() && count < maxCount) {
                    String path = itr.next();
                    // do we have some special formatting chars?
                    char first = path.charAt(0);
                    char last = path.charAt(path.length() - 1);
                    // strip all "non allowed" characters
                    path = path.replaceAll("[^a-zA-Z.]", "");

                    // resolve the field value
                    Object stuff = getFieldValue(path);
                    // maybe not in FieldConfigs - try just resolving the path
                    if (stuff == null) {
                        try {
                            stuff = resolvePath(objectType + "." + path);
                        } catch (PathException e) {
                            LOG.warn("Error resolving path '" + path + "' in titles config for: "
                                    + objectType);
                        }
                    }
                    if (stuff != null) {
                        String stringyStuff = stuff.toString();
                        // String.isEmpty() was introduced in Java release 1.6
                        if (StringUtils.isNotBlank(stringyStuff)) {
                            // apply special formatting
                            if (first == '[' && last == ']') {
                                stringyStuff = first + stringyStuff + last;
                            } else if (first == '*' && first == last) {
                                stringyStuff = "<i>" + stringyStuff + "</i>";
                            }

                            result.put(path, stringyStuff);
                            count++;
                        }
                    }
                }
                return result;
            }
        }

        return null;
    }

    /**
     *
     * @return a resolved link
     */
    public HeaderConfigLink getHeaderLink() {
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
     * @param list retrieved from Type
     * @param bagOfInlineListNames is a bag of names of lists we have resolved so far so these
     *        fields are not resolved elsewhere and skipped instead
     * @see setDescriptorOnInlineList() is still needed when traversing FieldDescriptors
     */
    private void initialiseInlineList(
            InlineList list,
            HashMap<String, Boolean> bagOfInlineListNames
    ) {
        // soon to be list of values
        Set<Object> listOfListObjects = null;
        String columnToDisplayBy = null;
        try {
            // create a new path to the collection of objects
            Path path = new Path(im.getModel(),
                    DynamicUtil.getSimpleClass(object.getClass()).getSimpleName()
                    + '.' + list.getPath());
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
            // resolve path to a collection and save into a list
            listOfListObjects = PathUtil.resolveCollectionPath(path, object);
            list.setListOfObjects(listOfListObjects, columnToDisplayBy);

        } catch (PathException e) {
            throw new RuntimeException("Your collections of inline lists"
                    + "are failing you", e);
        }

        // place the list
        if (list.getShowInHeader()) {
            inlineListsHeader.add(list);
        } else {
            inlineListsNormal.add(list);
        }

        // save name of the collection
        String path = list.getPath();
        bagOfInlineListNames.put(path.substring(0, path.indexOf('.')), true);
    }

    /**
     * Resolve an attribute, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseAttribute(FieldDescriptor fd) {
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
    }

    /**
     * Resolve a Reference, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseReference(FieldDescriptor fd) {
        // bag
        references = (references != null) ? references
                : new TreeMap<String, DisplayReference>(String.CASE_INSENSITIVE_ORDER);

        ReferenceDescriptor ref = (ReferenceDescriptor) fd;

        String refName = ref.getName();
        // do not bother with 'em if they WILL be size 0
        if (nullRefsCols == null || !nullRefsCols.contains(refName)) {
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
    }

    /**
     * Resolve a Collection, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseCollection(FieldDescriptor fd) {
        // bag
        collections = (collections != null) ? collections
                : new TreeMap<String, DisplayCollection>(String.CASE_INSENSITIVE_ORDER);

        String colName = fd.getName();
        // do not bother with 'em if they WILL be size 0
        if (nullRefsCols == null || !nullRefsCols.contains(colName)) {
            Object fieldValue = null;
            try {
                fieldValue = object.getFieldValue(colName);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            // determine the types in the collection
            List<Class<?>> listOfTypes = PathQueryResultHelper.
            queryForTypesInCollection(object, colName, im.getObjectStore());

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
    }

    /**
     * Create the Maps and Lists returned by the getters in this class.
     */
    private void initialise() {
        // combined Map of References & Collections
        refsAndCollections = new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER);

        /** InlineLists **/
        inlineListsHeader = (inlineListsHeader != null) ? inlineListsHeader
                : new ArrayList<InlineList>();
        inlineListsNormal = (inlineListsNormal != null) ? inlineListsNormal
                : new ArrayList<InlineList>();

        Type type = webConfig.getTypes().get(getClassDescriptor().getName());
        // init lists from WebConfig Type
        List<InlineList> inlineListsWebConfig = type.getInlineLists();
        // a map of inlineList object names so we do not include them elsewhere
        HashMap<String, Boolean> bagOfInlineListNames = new HashMap<String, Boolean>();
        // fill up
        for (int i = 0; i < inlineListsWebConfig.size(); i++) {
            initialiseInlineList(inlineListsWebConfig.get(i), bagOfInlineListNames);
        }

        /** Attributes, References, Collections through FieldDescriptors **/
        nullRefsCols =
            im.getObjectStoreSummary()
                .getNullReferencesAndCollections(getClassDescriptor().getName());
        for (FieldDescriptor fd : getClassDescriptor().getAllFieldDescriptors()) {
            // only continue if we have not included this object in an inline list
            if (bagOfInlineListNames.get(fd.getName()) == null) {
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
        Set<String> replacedFieldExprs = new HashSet<String>();
        for (ReportDisplayer reportDisplayer : getAllReportDisplayers()) {
            replacedFieldExprs.addAll(reportDisplayer.getReplacedFieldExprs());
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
