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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.displayer.DisplayerManager;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.HeaderConfigTitle;
import org.intermine.web.logic.config.InlineList;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;

/**
 * Class to represent an object for display in the webapp. Various maps and collections
 * are calculated here the first time an object is viewed and cached for later use. Only
 * information that remains static throughout the session should be precalcualted here.
 * Anything dynamic is computed in ObjectDetailsController.
 *
 * @author Mark Woodbridge
 * @author Radek Stepan
 *
 * @deprecated ReportObject replaces
 */
@java.lang.Deprecated
public class DisplayObject
{
    private InterMineObject object;
    private WebConfig webConfig;
    private Map<String, String> webProperties;
    private Model model;

    private Set<ClassDescriptor> clds;
    private Map<String, Object> fieldValues = new HashMap<String, Object>();
    private Map<String, Object> attributes = null;
    private Map<String, String> longAttributes = null;
    private Map<String, Object> longAttributesTruncated = null;
    private Map<String, FieldDescriptor> attributeDescriptors = null;
    private Map<String, DisplayReference> references = null;
    private Map<String, DisplayCollection> collections = null;
    private Map<String, DisplayField> refsAndCollections = null;
    private Map<String, FieldConfig> fieldConfigMap = null;
    private List<String> fieldExprs = null;
    private Map<String, String> verbosity = new HashMap<String, String>();
    private InterMineAPI im;
    private final Map<String, List<FieldDescriptor>> classKeys;

    /** @var List header inline lists set by the WebConfig */
    private List<InlineList> inlineListsHeader = null;
    /** @var List of 'unplaced' normal InlineLists */
    private List<InlineList> inlineListsNormal = null;

    /** @var ObjectStore so we can use PathQueryResultHelper.queryForTypesInCollection */
    private ObjectStore os = null;

    /**
     * Create a new DisplayObject.
     *
     * @param object the object to display
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public DisplayObject(InterMineObject object, InterMineAPI im, WebConfig webConfig,
            Map webProperties)
        throws Exception {

        this.im = im;
        this.os = im.getObjectStore();
        this.object = object;
        this.model = im.getModel();
        this.webConfig = webConfig;
        this.webProperties = webProperties;
        this.classKeys = im.getClassKeys();
        clds = getLeafClds(object.getClass(), model);
    }

    private static ThreadLocal<Map<Model, Map<Class<?>, Set<ClassDescriptor>>>> getLeafCldsCache
        = new ThreadLocal<Map<Model, Map<Class<?>, Set<ClassDescriptor>>>>() {
            @Override protected Map<Model, Map<Class<?>, Set<ClassDescriptor>>> initialValue() {
                return new IdentityHashMap<Model, Map<Class<?>, Set<ClassDescriptor>>>();
            }
        };

    /**
     * Get the set of leaf ClassDescriptors for a given InterMineObject class.
     *
     * @param clazz object type
     * @param model model
     * @return Set of ClassDescriptor objects
     */
    public static Set<ClassDescriptor> getLeafClds(Class<?> clazz, Model model) {
        Map<Model, Map<Class<?>, Set<ClassDescriptor>>> cache = getLeafCldsCache.get();
        Map<Class<?>, Set<ClassDescriptor>> classCache = cache.get(model);
        if (classCache == null) {
            classCache = new HashMap<Class<?>, Set<ClassDescriptor>>();
            cache.put(model, classCache);
        }
        Set<ClassDescriptor> leafClds = classCache.get(clazz);
        if (leafClds == null) {
            leafClds = new LinkedHashSet<ClassDescriptor>();
            for (Class<?> c : DynamicUtil.decomposeClass(clazz)) {
                ClassDescriptor cld = model.getClassDescriptorByName(c.getName());
                if (cld != null) {
                    leafClds.add(cld);
                }
            }
            classCache.put(clazz, leafClds);
        }
        return leafClds;
    }

    /**
     * Get the real business object
     * @return the object
     */
    public InterMineObject getObject() {
        return object;
    }

    public String getType() {
        return DynamicUtil.getSimpleClass(object).getSimpleName();
    }

    /**
     * Get the id of this object
     * @return the id
     */
    public int getId() {
        return object.getId().intValue();
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
        return (inlineListsHeader != null);
    }

    /**
     * Used from JSP
     * @return true if we have InlineLists with no placement yet
     */
    public Boolean getHasNormalInlineLists() {
        return (inlineListsNormal != null);
    }

    /**
     * Get the class descriptors for this object
     * @return the class descriptors
     */
    public Set<ClassDescriptor> getClds() {
        return clds;
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
     * Get the long attribute fields and values for this object.
     *
     * @return the attributes
     */
    public Map<String, String> getLongAttributes() {
        if (longAttributes == null) {
            initialise();
        }
        return longAttributes;
    }

    /**
     * Get the fields for this object that have had their long values truncated.
     *
     * @return the attributes
     */
    public Map<String, Object> getLongAttributesTruncated() {
        if (longAttributesTruncated == null) {
            initialise();
        }
        return longAttributesTruncated;
    }

    /**
     * Get the reference fields and values for this object
     * @return the references
     */
    public Map<String, DisplayReference> getReferences() {
        if (references == null) {
            initialise();
        }
        return references;
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
     * Return the path expressions for the fields that should be used when summarising this
     * DisplayObject.
     * @return the expressions
     */
    public List<String> getFieldExprs() {
        if (fieldExprs == null) {
            fieldExprs = new ArrayList<String>();
            Set<String> replacedFieldExprs = getReplacedFieldExprs();
            for (Iterator<String> i = getFieldConfigMap().keySet().iterator(); i.hasNext();) {
                String fieldExpr = i.next();
                if (!replacedFieldExprs.contains(fieldExpr)) {
                    fieldExprs.add(fieldExpr);
                }
            }
        }
        return fieldExprs;
    }

    /**
     *
     * @return Set
     */
    public Set<CustomDisplayer> getAllReportDisplayers() {
        DisplayerManager displayerManager = DisplayerManager.getInstance(webConfig, im);
        String clsName = DynamicUtil.getSimpleClass(object).getSimpleName();
        return displayerManager.getAllReportDisplayersForType(clsName);
    }

    /**
    *
    * @return Map
    */
   public Map<String, List<CustomDisplayer>> getReportDisplayers() {
       DisplayerManager displayerManager = DisplayerManager.getInstance(webConfig, im);
       String clsName = DynamicUtil.getSimpleClass(object).getSimpleName();
       return displayerManager.getReportDisplayersForType(clsName);
   }

    /**
     *
     * @return Set
     */
    public Set<String> getReplacedFieldExprs() {
        Set<String> replacedFieldExprs = new HashSet<String>();
        for (CustomDisplayer reportDisplayer : getAllReportDisplayers()) {
            replacedFieldExprs.addAll(reportDisplayer.getReplacedFieldExprs());
        }
        return replacedFieldExprs;
    }


    /**
     * (uses different method than in JSP as that one does not work)
     * @return a count of fields in the header
     */
    public int getHeaderFieldsSize() {
        Set<String> set = new LinkedHashSet<String>();

        for (String fx : getFieldExprs()) {
            if (getFieldConfigMap().get(fx).getShowInSummary()) {
                set.add(fx);
            }
        }

        set.addAll(getAttributes().keySet());

        return set.size();
    }

    /**
     * Get map from field expr to FieldConfig.
     * @return map from field expr to FieldConfig
     */
    public Map<String, FieldConfig> getFieldConfigMap() {
        if (fieldConfigMap == null) {
            fieldConfigMap = new LinkedHashMap<String, FieldConfig>();

            for (ClassDescriptor cld : clds) {
                for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cld)) {
                    fieldConfigMap.put(fc.getFieldExpr(), fc);
                }
            }
        }
        return fieldConfigMap;
    }

    /**
     * Get the map indication whether individuals fields are to be display verbosely
     * @return the map
     */
    public Map<String, String> getVerbosity() {
        return Collections.unmodifiableMap(verbosity);
    }

    /**
     * Set the verbosity for a field
     * @param fieldName the field name
     * @param verbose true or false
     */
    public void setVerbosity(String fieldName, boolean verbose) {
        verbosity.put(fieldName, verbose ? fieldName : null);
    }

    /**
     * Get verbosity of a field
     * @param placementAndField a String that combines the name of the current placement/aspect and
     * a fieldName
     * @return true or false
     */
    public boolean isVerbose(String placementAndField) {
        return verbosity.get(placementAndField) != null;
    }

    /**
     * Create the Maps and Lists returned by the getters in this class.
     */
    @SuppressWarnings("unchecked")
    private void initialise() {
        attributes = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        references = new TreeMap<String, DisplayReference>(String.CASE_INSENSITIVE_ORDER);
        collections = new TreeMap<String, DisplayCollection>(String.CASE_INSENSITIVE_ORDER);
        refsAndCollections = new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER);
        attributeDescriptors = new HashMap<String, FieldDescriptor>();
        longAttributes = new HashMap<String, String>();
        longAttributesTruncated = new HashMap<String, Object>();

        // InlineLists
        inlineListsHeader = new ArrayList<InlineList>();
        inlineListsNormal = new ArrayList<InlineList>();
        List<InlineList> inlineListsWebConfig = null;

        try {
            for (ClassDescriptor cld : clds) {

                /** InlineLists **/
                Type type = webConfig.getTypes().get(cld.getName());
                // init lists from WebConfig Type
                inlineListsWebConfig = type.getInlineLists();
                // a map of inlineList object names so we do not include them elsewhere
                HashMap<String, Boolean> bagOfInlineListNames = new HashMap<String, Boolean>();
                // fill up
                for (int i = 0; i < inlineListsWebConfig.size(); i++) {
                    InlineList list = inlineListsWebConfig.get(i);
                    // soon to be list of values
                    Set<Object> listOfListObjects = null;
                    String columnToDisplayBy = null;
                    try {
                        // create a new path to the collection of objects
                        Path path = new Path(model,
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

                for (FieldDescriptor fd : cld.getAllFieldDescriptors()) {
                    // only continue if we have not included this object in an inline list
                    if (bagOfInlineListNames.get(fd.getName()) == null) {
                        if (fd.isAttribute() && !"id".equals(fd.getName())) {
                            Object fieldValue = object.getFieldValue(fd.getName());
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
                                if (fieldValue instanceof String) {
                                    String fieldString = (String) fieldValue;
                                    if (fieldString.length() > 30) {
                                        StringUtil.LineWrappedString lws = StringUtil.wrapLines(
                                                fieldString, 50, 3, 11);
                                        longAttributes.put(fd.getName(), lws.getString()
                                                .replace("\n", "<BR>"));
                                        if (lws.isTruncated()) {
                                            longAttributesTruncated.put(fd.getName(), Boolean.TRUE);
                                        }
                                    }
                                }
                            }
                        } else if (fd.isReference()) {
                            ReferenceDescriptor ref = (ReferenceDescriptor) fd;

                            //check whether reference is null without dereferencing
                            ProxyReference proxy =
                                (ProxyReference) object.getFieldProxy(ref.getName());

                            DisplayReference newReference =
                                new DisplayReference(proxy, ref, webConfig,
                                    classKeys);
                            references.put(fd.getName(), newReference);
                        } else if (fd.isCollection()) {
                            Object fieldValue = object.getFieldValue(fd.getName());

                            // determine the types in the collection
                            List<Class<?>> listOfTypes = PathQueryResultHelper.
                            queryForTypesInCollection(object, fd.getName(), os);

                            DisplayCollection newCollection =
                                new DisplayCollection(null, null, null, null, null, null);
                            //if (newCollection.getSize() > 0) {
                            collections.put(fd.getName(), newCollection);
                            //}
                        }
                    } else {
                        // assign Descriptor from FieldDescriptors to the InlineList
                        setDescriptorOnInlineList(fd.getName(), fd);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Exception while creating a DisplayObject", e);
        }

        // make a combined Map
        refsAndCollections.putAll(references);
        refsAndCollections.putAll(collections);
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
     * gets the fields to display on the object details page for this display object
     * @return map of fieldnames to display for this object
     */
    public Map<String, Object> getFieldValues() {
        if (fieldValues == null || fieldValues.isEmpty()) {
            fieldValues = new HashMap<String, Object>();
            for (Iterator<String> i = fieldExprs.iterator(); i.hasNext();) {
                String expr = i.next();
                String className = DynamicUtil.getSimpleClass(object.getClass()).getSimpleName();
                String pathString = className + "." + expr;
                try {
                    Path path = new Path(model, pathString);
                    fieldValues.put(expr, PathUtil.resolvePath(path, object));
                } catch (PathException e) {
                    throw new Error("There must be a bug", e);
                }
            }
        }
        return fieldValues;
    }

    /**
     * Used by JSP
     * @return a string representing the type of the object in question, i.e.: "Gene"
     */
    public String getObjectType() {
        String result = "";
        for (Object cld : getClds()) {
            result += ((ClassDescriptor) cld).getUnqualifiedName() + " ";
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * Used by JSP
     * @return the main title of this object, i.e.: "eve FBgn0000606"
     */
    public String getTitleMain() {
        return getTitles("main");
    }

    /**
     * Used by JSP
     * @return the subtitle of this object, i.e.: "D. melanogaster"
     */
    public String getTitleSub() {
        return getTitles("sub");
    }

    /**
     * Get a title based on the type key we pass it
     * @param key: main|sub
     * @return the titles string as resolved based on the path(s) under key
     */
    private String getTitles(String key) {
        // for all ClassDescriptors
        for (ClassDescriptor cld : clds) {
            // fetch the Type
            Type type = webConfig.getTypes().get(cld.getName());
            // retrieve the titles map, HeaderConfig serves as a useless wrapper
            HeaderConfigTitle hc = type.getHeaderConfigTitle();
            if (hc != null) {
                Map<String, LinkedHashMap<String, Object>> titles = hc.getTitles();
                // if we have something saved
                if (titles != null && titles.containsKey(key)) {
                    String result = "";
                    // concatenate a space delineated title together as resolved from FieldValues
                    for (String path : titles.get(key).keySet()) {
                        Object stuff = getFieldValues().get(path);
                        if (stuff != null) {
                            String stringyStuff = stuff.toString();
                            // String.isEmpty() was introduced in Java release 1.6
                            if (StringUtils.isNotBlank(stringyStuff)) {
                                result += stringyStuff + " ";
                            }
                        }
                    }
                    // trailing space & return
                    if (!result.isEmpty()) {
                        return result.substring(0, result.length() - 1);
                    }
                }
            }
        }

        return null;
    }

}
