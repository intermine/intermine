package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.results.ResultElement;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * An inline table created from a Collection
 * This table has one object per row
 *
 * @author Radek Stepan
 * @author Mark Woodbridge
 */
public class InlineResultsTable
{
    protected Collection<?> results;
    protected List<?> resultsAsList;
    // just those objects that we will display
    @SuppressWarnings("unchecked")
    protected List rowObjects = new ArrayList();
    protected List<?> columnFullNames = null;
    // a list of list of values for the table
    protected Model model;
    protected int size = -1;
    protected WebConfig webConfig;
    private final Map<String, List<FieldDescriptor>> classKeys;

    /** @var List of all the types a table will hold, so we can fetch all the FCs */
    private List<Class<?>> listOfTypes = null;

    /** @var List of merged FieldConfigs for Collections and References alike */
    private List<FieldConfig> listOfTableFieldConfigs = null;

    /** @var List of InlineResultTableInlineRows */
    private LinkedList<Object> listOfTableRows = null;

    protected static final Logger LOG = Logger.getLogger(InlineResultsTable.class);
    private FieldDescriptor fieldDescriptor = null;

    /**
     * Construct a new InlineResultsTable object
     * @param results the List to display object
     * @param model the current Model
     * @param webConfig the WebConfig object for this webapp
     * @param classKeys Map of class name to set of keys
     * @param size the maximum number of rows to list from the collection, or -1 if we should
     * @param ignoreDisplayers if true don't include any columns that have jsp displayers defined
     * @param listOfTypes resolved using PathQueryResultHelper.queryForTypesInCollection on a
     *  Collection, a Reference object will have null instead and its Type will be resolved
     *  using getListOfTypes()
     */
    @SuppressWarnings("unchecked")
    public InlineResultsTable(Collection<?> results, Model model,
                              WebConfig webConfig, Map<String, List<FieldDescriptor>> classKeys,
                              int size, boolean ignoreDisplayers, List<Class<?>> listOfTypes) {
        long startTime = System.currentTimeMillis();
        this.listOfTypes = listOfTypes;

        this.results = results;
        this.classKeys = classKeys;
        if (results instanceof List<?>) {
            resultsAsList = (List<?>) results;
        } else {
            if (results instanceof LazyCollection<?>) {
                this.resultsAsList = ((LazyCollection<?>) results).asList();
            } else {
                this.resultsAsList = new ArrayList(results);
            }
        }
        this.webConfig = webConfig;

        this.model = model;
        this.size = size;

        Iterator<?> resultsIter;
        if (getSize() == -1) {
            resultsIter = resultsAsList.iterator();
        } else {
            resultsIter = resultsAsList.subList(0, getSize()).iterator();
        }

        // loop through each row object
        while (resultsIter.hasNext()) {
            Object o = resultsIter.next();
            if (o instanceof ProxyReference) {
                // special case for ProxyReference from DisplayReference objects
                o = ((ProxyReference) o).getObject();
            }
            rowObjects.add(o);
        }
        long took = System.currentTimeMillis() - startTime;
        LOG.info("TIME - InlineResultsTable constructor took: " + took + "ms.");
    }

    /**
     * Construct with parent type and a field descriptor.
     * @param results the List to display object
     * @param model the current Model
     * @param webConfig the WebConfig object for this webapp
     * @param classKeys Map of class name to set of keys
     * @param size the maximum number of rows to list from the collection, or -1 if we should
     * @param ignoreDisplayers if true don't include any columns that have jsp displayers defined
     * @param listOfTypes resolved using PathQueryResultHelper.queryForTypesInCollection on a
     *  Collection, a Reference object will have null instead and its Type will be resolved
     *  using getListOfTypes()
     * @param parentType The type of the parent for this list
     * @param fd The field descriptor this list represents.
     */
    public InlineResultsTable(Collection<?> results, Model model,
                              WebConfig webConfig, Map<String, List<FieldDescriptor>> classKeys,
                              int size, boolean ignoreDisplayers, List<Class<?>> listOfTypes,
                              String parentType, FieldDescriptor fd) {
        this(results, model, webConfig, classKeys, size, ignoreDisplayers, listOfTypes);
//        this.parentType = parentType;
        this.fieldDescriptor = fd;
    }

    /**
     * Get the parent's type
     * @return the type of the parent
     */
    public String getParentType() {
        return fieldDescriptor.getClassDescriptor().getUnqualifiedName();
    }

    /**
     * Get the fields descriptor this list represents.
     * @return a field descriptor.
     */
    public FieldDescriptor getFieldDescriptor() {
        return fieldDescriptor;
    }

    /**
    * @see the reason for retrieving types for Reference here is that
    *  PathQueryResultHelper.queryForTypesInCollection() does not work
    *  for References (obviously)
    *
    * @return a list of types in a Collection or a Reference contained in this table
    */
    public List<Class<?>> getListOfTypes() {
        if (listOfTypes == null) { // for uninitialized References
            InterMineObject o = (InterMineObject) rowObjects.iterator()
                .next(); // based on the first object
            if (o instanceof ProxyReference) {
                // special case for ProxyReference from DisplayReference objects
                o = ((ProxyReference) o).getObject();
            }
            // init new
            listOfTypes = new ArrayList<Class<?>>();
            listOfTypes.add(DynamicUtil.getSimpleClass(o));
        }
        return listOfTypes;
    }

    /**
     *
     * @return true if the table has more than one Type of object
     * @see bear in mind that this method will return true if there are different Types in the
     *  whole table, not just the "30" odd rows subset!
     */
    public Boolean getHasMoreThanOneType() {
        return (getListOfTypes().size() > 1);
    }

    /**
     *
     * @return a list of (all) FieldConfigs/Columns for this table
     */
    public List<FieldConfig> getTableFieldConfigs() {
        if (listOfTableFieldConfigs == null) { // init
            listOfTableFieldConfigs = new ArrayList<FieldConfig>();
            // fetch all the object types we have,
            //  and traverse-add them to a list of FCs
            for (Class<?> clazz : getListOfTypes()) {
                for (FieldConfig fc : getClassFieldConfigs(
                        model.getClassDescriptorByName(clazz.getName()))) {
                    if (fc.getShowInInlineCollection() && !listOfTableFieldConfigs
                            .contains(fc)) {
                        listOfTableFieldConfigs.add(fc);
                    }
                }
            }
        }
        return listOfTableFieldConfigs;
    }

    /**
     * Find the FieldConfig objects for the the given ClassDescriptor.
     * @param cd a ClassDescriptor
     * @return the FieldConfig objects for the the given ClassDescriptor
     */
    protected List<FieldConfig> getClassFieldConfigs(ClassDescriptor cd) {
        return FieldConfigHelper.getClassFieldConfigs(webConfig, cd);
    }

    /**
     * Return the size that was passed to the constructor.
     */
    private int getSize() {
        return size;
    }

    /**
     * Answers the ever pressing question as to whether or not an object of a given Class has this
     *  FieldConfig (column) defined. As an inline table can have multiple object Types, an instance
     *  of an object (row) has to be resolved against a bag of FieldConfigs (columns)
     * @param clazz
     * @param fc
     * @return true if a Class has a FieldConfig defined
     */
    private Boolean isThisFieldConfigInThisObject(Class<?> clazz, FieldConfig fc) {
        return (getClassFieldConfigs(model.getClassDescriptorByName(clazz.getName())).contains(fc));
    }

    /**
     * Create a table row object, overriden on BagUploadConfirm where we go custom
     *
     * @return an InlineResultsTableRow (default)
     */
    protected Object returnNewTableRow() {
        return new InlineResultsTableRow();
    }

    /**
     * Set a class name on table row, used for type safety as overriden on BagUploadConfirm
     *
     * @param className string name of the class of the object
     * @param tableRowObject InlineResultsTableRow (default)
     */
    protected void setClassNameOnTableRow(String className, Object tableRowObject) {
        InlineResultsTableRow tableRow = (InlineResultsTableRow) tableRowObject;
        tableRow.setClassName(className);
    }

    /**
     * Add a result element (RE or "" String), used for type safety as overriden on BagUploadConfirm
     *
     * @param resultElement to be saved in the list
     * @param tableRowObject InlineResultsTableRow (default)
     */
    protected void addResultElementToTableRow(Object resultElement, Object tableRowObject) {
        InlineResultsTableRow tableRow = (InlineResultsTableRow) tableRowObject;
        tableRow.add(resultElement);
    }

    /**
     * Used on BagUploadConfirm
     * @param id imObj
     * @param tableRowObject InlineResultsTableRow (default)
     */
    protected void saveObjectIdOnTableRow(Integer id, Object tableRowObject) { }

    /**
     * Main method used from report to resolve a tablefull of ResultElements
     *
     * @see this method partly uses Template Pattern, check that calls to table row objects are
     *  made generic as BagUploadConfirm AND ReportPage both use this method while using different
     *  types of table row objects
     * @return a list of lists of ResultElements
     */
    public List<Object> getResultElementRows() {
        if (listOfTableRows == null) {
            listOfTableRows = new LinkedList<Object>();
            int columnsSize = getColumnsSize();
            // for a row object
            for (int i = 0; i < rowObjects.size(); i++) {
                // fetch the object in the row
                Object o = rowObjects.get(i);
                String column = null;
                Path path = null;
                ResultElement re = null;
                String className = null;
                Boolean foundMainIdentifier = false;

                // create a row of elements
                Object columnList = returnNewTableRow();
                for (int j = 0; j < columnsSize; j++) {
                    try {
                        FieldConfig fc = getTableFieldConfigs().get(j);
                        // column name also known as field expression
                        column = fc.getFieldExpr();
                        // determine the class of the object
                        Class<?> clazz = DynamicUtil.getSimpleClass((FastPathObject) o);
                        // does THIS row object have THIS column?
                        if (isThisFieldConfigInThisObject(clazz, fc)) {
                            // resolve class name
                            className =
                                DynamicUtil.getSimpleClass((FastPathObject) o).getSimpleName();
                            setClassNameOnTableRow(className, columnList);
                            // form a new path
                            path = new Path(model, className + '.' + column);

                            // key field?
                            String endTypeName = path.getLastClassDescriptor().getName();
                            String lastFieldName = path.getEndFieldDescriptor().getName();
                            Boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, endTypeName,
                                    lastFieldName);

                            // finalObject
                            FastPathObject imObj = null;
                            try {
                                imObj = (FastPathObject) PathUtil.resolvePath(path.getPrefix(), o);
                            } catch (PathException e) {
                                e.printStackTrace();
                            }

                            // save the InterMine Object identifier
                            if (!foundMainIdentifier) {
                                Class<?> objectType = DynamicUtil.getSimpleClass(imObj);
                                if (InterMineObject.class.isAssignableFrom(objectType)) {
                                    saveObjectIdOnTableRow(((InterMineObject) imObj).getId(),
                                            columnList);
                                }
                                foundMainIdentifier = true;
                            }

                            // create new inline result element
                            String displayer = fc.getDisplayer();
                            // ...displayer kind
                            if (displayer != null && displayer.length() > 0) {
                                re = new InlineTableResultElement(imObj, path, fc, isKeyField);
                            } else {
                                String finalPath =
                                    path.getLastClassDescriptor().getUnqualifiedName()
                                    + "." + path.getLastElement();
                                re = new InlineTableResultElement(imObj, new Path(path.getModel(),
                                        finalPath), fc, isKeyField);
                            }
                            // save the ResultElement
                            addResultElementToTableRow(re, columnList);
                        } else {
                            // empty string instead
                            addResultElementToTableRow("", columnList);
                        }
                    } catch (PathException e) {
                        LOG.error(e);
                    }
                }
                listOfTableRows.add(columnList);
            }
        }
        return listOfTableRows;
    }

    /**
     *
     * @return the number of columns in each table, based on all FieldConfigs for all objects
     */
    public Integer getColumnsSize() {
        return getTableFieldConfigs().size();
    }
}
