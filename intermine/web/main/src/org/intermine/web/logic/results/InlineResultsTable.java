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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.results.ResultElement;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * An inline table created from a Collection
 * This table has one object per row
 * @author Mark Woodbridge
 */
public class InlineResultsTable
{
    protected Collection results;
    protected List resultsAsList;
    // just those objects that we will display
    protected List subList;
    private List<FieldConfig> fieldConfigs = null;
    protected List columnFullNames = null;
    // a list of list of values for the table
    protected Model model;
    protected int size = -1;
    protected WebConfig webConfig;
    protected Map webProperties;
    private final Map<String, List<FieldDescriptor>> classKeys;
    private List<ResultElement> resultElementRow;
    private Map<String, Object> fieldValues;
    private Map<Object, Map<String, Object>> rowFieldValues;
    private boolean ignoreDisplayers;

    /**
     * Construct a new InlineResultsTable object
     * @param results the List to display object
     * @param model the current Model
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @param classKeys Map of class name to set of keys
     * @param size the maximum number of rows to list from the collection, or -1 if we should
     * @param ignoreDisplayers if true don't include any columns that have jsp displayers defined
     *   display all rows
     */
    public InlineResultsTable(Collection results, Model model,
                              WebConfig webConfig, Map webProperties,
                              Map<String, List<FieldDescriptor>> classKeys,
                              int size, boolean ignoreDisplayers) {
        this.results = results;
        this.classKeys = classKeys;
        if (results instanceof List) {
            resultsAsList = (List) results;
        } else {
            if (results instanceof LazyCollection) {
                this.resultsAsList = ((LazyCollection) results).asList();
            } else {
                this.resultsAsList = new ArrayList(results);
            }
        }
        this.webConfig = webConfig;
        this.webProperties = webProperties;

        this.model = model;
        this.size = size;
        this.ignoreDisplayers = ignoreDisplayers;
    }

    /**
     * Return heading for the fieldConfigs
     * @return the column names
     */
    public List getColumnNames() {
        if (fieldConfigs == null) {
            initialise();
        }

        List columnNames = new ArrayList();

        for (FieldConfig fc : fieldConfigs) {
            columnNames.add(fc.getFieldExpr());
        }
        return Collections.unmodifiableList(columnNames);
    }

    /**
     * Return the full name of the field shown in this column in ClassName.fieldName format.
     * eg. Gene.primaryIdentifier
     * Currently returns null if the FieldConfig for the column contains a complex expression,
     * that is one the follows a reference eg. organism.shortName
     * @return the List of column full names
     */
    public List getColumnFullNames() {
        if (columnFullNames == null) {
            initialise();
        }

        return columnFullNames;
    }

    /**
     * Get the class descriptors of each object displayed in the table
     * @return the set of class descriptors for each row
     */
    public List getTypes() {
        List types = new ArrayList();
        for (Iterator i = getRowObjects().iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof ProxyReference) {
                // special case for ProxyReference from DisplayReference objects
                o = ((ProxyReference) o).getObject();
            }
            types.add(DisplayObject.getLeafClds(o.getClass(), model));
        }
        return types;
    }

    /**
     * Get the ids of the objects in the rows
     * @return a List of ids, one per row
     */
    public List getIds() {
        List ids = new ArrayList();
        for (Iterator i = getRowObjects().iterator(); i.hasNext();) {
            Object n = i.next();
            if (n instanceof InterMineObject) {
                ids.add(((InterMineObject) n).getId());
            } else {
                ids.add(null);
            }
        }
        return ids;
    }

    /**
     * Return the Objects that we are displaying in this table.
     * @return a List of Objects, one per row
     */
    public List getRowObjects() {
        if (subList == null) {
            initialise();
        }

        return subList;
    }

    /**
     * Create the tableRows, columnNames and subList Lists by looping over the first elements of the
     * collection.  The names of all fields to be displayed are collected in the fieldConfigs List
     * then and the fieldConfigs to display are collected in the tableRows List.  The fields of the
     * rows in the tableRows List will always be in the same order as the elements of the
     * fieldConfigs List.
     */
    protected void initialise() {
        fieldConfigs = new ArrayList<FieldConfig>();
        columnFullNames = new ArrayList();
        subList = new ArrayList();
        rowFieldValues = new HashMap();

        Iterator resultsIter;
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

            subList.add(o);

            Set clds = DisplayObject.getLeafClds(o.getClass(), model);

            // TODO this doesn't cope properly with dynamic classes
            ClassDescriptor theClass = (ClassDescriptor) clds.iterator().next();

            fieldValues = new HashMap();

            // loop through each column
            for (FieldConfig fc : getRowFieldConfigs(o)) {
                // if ignoreDisplayers we don't want any columns with a displayer defined
                if (ignoreDisplayers && fc.getDisplayer() != null) {
                    continue;
                }
                String className = theClass.getUnqualifiedName();
                String expr = fc.getFieldExpr();
                String pathString = className + "." + expr;

                try {
                    Path path = new Path(model, pathString);
                    fieldValues.put(expr, PathUtil.resolvePath(path, o));
                } catch (PathException e) {
                    throw new Error("Could not create path for \"" + pathString
                            + "\" - check FieldConfig");
                }

//                try {
//                    fieldValues.put(expr, TypeUtil.getFieldValue(o, expr));
//                } catch (IllegalAccessException e) {
//                    // do nothing
//                }

                if (!fieldConfigs.contains(fc) && fc.getShowInInlineCollection()) {
                    fieldConfigs.add(fc);
                    // only add full column names for simple fieldConfigs - ie. ones that specify a
                    // field in the current class
                    columnFullNames.add(className + "." + expr);
                }
            }
            if (!fieldValues.isEmpty()) {
                rowFieldValues.put(o, fieldValues);
            }
        }
    }

    /**
     * Find the FieldConfig objects for the the given Object.
     * @param rowObject an Object
     * @return the FieldConfig objects for the the given Object.
     */
    protected List<FieldConfig> getRowFieldConfigs(Object rowObject) {
        List<FieldConfig> returnFieldConfigs = new ArrayList<FieldConfig>();

        Set<ClassDescriptor> objectClassDescriptors
            = DisplayObject.getLeafClds(rowObject.getClass(), model);

        for (ClassDescriptor thisClassDescriptor : objectClassDescriptors) {
            for (FieldConfig fc : getClassFieldConfigs(thisClassDescriptor)) {
                if (fc.getShowInInlineCollection()) {
                    returnFieldConfigs.add(fc);
                }
            }
        }

        return returnFieldConfigs;
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
     * Return a List of FieldConfig objects - one per column.
     * @return the FieldConfigs
     */
    public List<FieldConfig> getFieldConfigs() {
        if (fieldConfigs == null) {
            initialise();
        }
        return fieldConfigs;
    }

    /**
     * Return a List containing the ResultElements for a given row in the table.
     * @param rowIndex the index of the row to create the List for
     * @return a List of ResultElements, one for each column returned by getColumnFullNames().  If
     *   a particular column isn't relavent for this row, that element of the List will be null.
     */
    public List getResultElementRow(int rowIndex) {
        Object o = getRowObjects().get(rowIndex);
        List retList = new ArrayList();
        for (int i = 0; i < getColumnFullNames().size(); i++) {
            Path path;
            try {
                path = new Path(model, (String) getColumnFullNames().get(i));
                if (!path.endIsAttribute()) {
                    // the end of the Path is not an attribute
                    retList.add(null);
                    continue;
                }
                retList.add(createResultElement(path, o));
            } catch (PathException e) {
                // if the field can't be resolved then this Path doesn't make sense for this object
                retList.add(null);
            }
        }
        return retList;
    }

    /**
     * Creates result element for the end of the path.
     * Example:
     * For Department.name creates result element for Department object and name field
     * For Department.company.name creates result element for Company object and name field
     * @param path
     * @param o
     * @return
     */
    private ResultElement createResultElement(Path path, Object o) {
        String endTypeName = path.getLastClassDescriptor().getName();
        String lastFieldName = path.getEndFieldDescriptor().getName();
        boolean isKeyField =
            ClassKeyHelper.isKeyField(classKeys, endTypeName, lastFieldName);
        // object = Organism, path = Organism.shortName
        InterMineObject finalObject = null;
        if (o != null) {
            try {
                finalObject = (InterMineObject) PathUtil.resolvePath(path.getPrefix(), o);
            } catch (PathException e) {
                throw new IllegalArgumentException("Failed to resolve path on object", e);
            }
        }
        String finalPath = path.getLastClassDescriptor().getUnqualifiedName() + "."
            + path.getLastElement();
        try {
            return new ResultElement(finalObject, new Path(path.getModel(), finalPath),
                    isKeyField);
        } catch (PathException e) {
            throw new Error("There must be a bug", e);
        }
    }

    /**
     * @return the rowFieldValues
     */
    public Map<Object, Map<String, Object>> getRowFieldValues() {
        return rowFieldValues;
    }

    /**
     * For HACK purpose
     * @return resultsAsList
     */
    public List getResultsAsList() {
        return resultsAsList;
    }
}
