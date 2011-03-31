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
import java.util.LinkedHashMap;
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
import org.intermine.web.logic.config.HeaderConfig;
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

    private Map<String, String> webProperties;
    private Map<String, Object> attributes = null;
    private Map<String, String> longAttributes = null;
    private Map<String, Object> longAttributesTruncated = null;
    private Map<String, FieldDescriptor> attributeDescriptors = null;
    private Map<String, DisplayReference> references = null;
    private Map<String, DisplayCollection> collections = null;
    private Map<String, DisplayField> refsAndCollections = null;

    /** @var ObjectStore so we can use PathQueryResultHelper.queryForTypesInCollection */
    private ObjectStore os = null;

    /**
     * Setup internal ReportObject
     * @param object InterMineObject
     * @param webConfig WebConfig
     * @throws Exception Exception
     */
    public ReportObject(InterMineObject object, WebConfig webConfig,
            InterMineAPI im) throws Exception {
        this.object = object;
        this.webConfig = webConfig;
        this.im = im;

        this.os = im.getObjectStore();

        // infer dynamic type of IM object
        this.objectType = DynamicUtil.getSimpleClass(object).getSimpleName();
    }

    /**
    *
    * @return Map
    */
   public Map<String, List<CustomDisplayer>> getReportDisplayers() {
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

    /**
     * A listing of object fields as pieced together from the various ReportObject methods
     * @return <ReportObjectField>s List
     */
    public List<ReportObjectField> getObjectSummaryFields() {
        // are we setup yet?
        if (objectSummaryFields == null) {
            objectSummaryFields = new ArrayList<ReportObjectField>();
            List<ReportObjectField> objectOtherSummaryFields = new ArrayList<ReportObjectField>();

            // traverse all path expressions for the fields that should be used when
            //  summarising the object
            for (FieldConfig fc : getFieldConfigs()) {
                // get fieldName
                String fieldName = fc.getFieldExpr();

                // get fieldValue
                Object fieldValue = getFieldValue(fieldName);

                // get displayer
                //FieldConfig fieldConfig = fieldConfigMap.get(fieldName);
                String fieldDisplayer = fc.getDisplayer();

                // new ReportObjectField
                ReportObjectField rof = new ReportObjectField(
                        objectType,
                        fieldName,
                        fieldValue,
                        fieldDisplayer,
                        fc.getDoNotTruncate()
                );

                // show in summary...
                if (fc.getShowInSummary()) {
                    objectSummaryFields.add(rof);
                } else { // show in summary also, but not right now...
                    objectOtherSummaryFields.add(rof);
                }
            }
            // append the other fields
            objectSummaryFields.addAll(objectOtherSummaryFields);
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
        Map<String, Type> types = webConfig.getTypes();
        String qualifiedType = DynamicUtil.getSimpleClass(object).getName();
        if (types.containsKey(qualifiedType)) {
            return types.get(qualifiedType).getFieldConfigs();
        }
        return Collections.emptyList();
    }

    /**
     * Get field value for a field name (expression)
     * @param fieldExpression String
     * @return Object
     */
    protected Object getFieldValue(String fieldExpression) {
        // if field values as a whole are not set yet...
        if (fieldValues == null) {
            setupFieldValues();
        }
        // return a field value for a field expression (name)
        return fieldValues.get(fieldExpression);
    }

    /**
     * Setup fieldValues HashMap
     */
    protected void setupFieldValues() {
        // create a new map
        fieldValues = new HashMap<String, Object>();

        // fetch field configs
        for (FieldConfig fc : getFieldConfigs()) {
            // crete a path string
            String pathString = objectType + "." + fc.getFieldExpr();
            try {
                // resolve path
                Path path = new Path(im.getModel(), pathString);
                fieldValues.put(fc.getFieldExpr(), PathUtil.resolvePath(path, object));
            } catch (PathException e) {
                throw new Error("There must be a bug", e);
            }
        }
    }

    public String getType() {
        return objectType;
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
        // fetch the Type
        Type type = webConfig.getTypes().get(getClassDescriptor().getName());
        // retrieve the titles map, HeaderConfig serves as a useless wrapper
        HeaderConfig hc = type.getHeaderConfig();
        if (hc != null) {
            Map<String, LinkedHashMap<String, Object>> titles = hc.getTitles();
            // if we have something saved
            if (titles != null && titles.containsKey(key)) {
                String result = "";
                // concatenate a space delineated title together as resolved from FieldValues
                for (String path : titles.get(key).keySet()) {
                    Object stuff = getFieldValue(path);
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
        // bags
        inlineListsHeader = (inlineListsHeader != null) ? inlineListsHeader
                : new ArrayList<InlineList>();
        inlineListsNormal = (inlineListsNormal != null) ? inlineListsNormal
                : new ArrayList<InlineList>();

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
        longAttributes = (longAttributes != null) ? longAttributes
                : new HashMap<String, String>();
        longAttributesTruncated = (longAttributesTruncated != null) ? longAttributesTruncated
                : new HashMap<String, Object>();
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

        //check whether reference is null without dereferencing
        ProxyReference proxy = null;
        try {
            proxy = (ProxyReference) object.getFieldProxy(ref.getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        DisplayReference newReference = null;
        try {
            newReference = new DisplayReference(proxy, ref, webConfig,
                webProperties, im.getClassKeys());
        } catch (Exception e) {
            e.printStackTrace();
        }
        references.put(fd.getName(), newReference);
    }

    /**
     * Resolve a Collection, part of initialise()
     * @param fd FieldDescriptor
     */
    private void initialiseCollection(FieldDescriptor fd) {
        // bag
        collections = (collections != null) ? collections
                : new TreeMap<String, DisplayCollection>(String.CASE_INSENSITIVE_ORDER);

        Object fieldValue = null;
        try {
            fieldValue = object.getFieldValue(fd.getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // determine the types in the collection
        List<Class<?>> listOfTypes = PathQueryResultHelper.
        queryForTypesInCollection(object, fd.getName(), os);

        DisplayCollection newCollection = null;
        try {
            newCollection = new DisplayCollection((Collection<?>) fieldValue,
                    (CollectionDescriptor) fd, webConfig, webProperties,
                    im.getClassKeys(), listOfTypes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //if (newCollection.getSize() > 0) {
        collections.put(fd.getName(), newCollection);
        //}
    }

    /**
     * Create the Maps and Lists returned by the getters in this class.
     */
    private void initialise() {
        // combined Map of References & Collections
        refsAndCollections = new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER);

        /** InlineLists **/
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
        if (references != null) refsAndCollections.putAll(references);
        if (collections != null) refsAndCollections.putAll(collections);
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
   *
   * @return Set
   */
  public Set<CustomDisplayer> getAllReportDisplayers() {
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
