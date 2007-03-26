package org.intermine.web.results;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.path.Path;
import org.intermine.path.PathError;
import org.intermine.util.TypeUtil;
import org.intermine.web.ClassKeyHelper;
import org.intermine.web.config.FieldConfig;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.WebConfig;

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
    private List fieldConfigs = null;
    protected List columnFullNames = null;
    // a list of list of values for the table
    protected Model model;
    protected int size = -1;
    protected WebConfig webConfig;
    protected Map webProperties;
    private final Map classKeys;

    /**
     * Construct a new InlineResultsTable object
     * @param results the List to display object
     * @param model the current Model 
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @param classKeys Map of class name to set of keys
     * @param size the maximum number of rows to list from the collection, or -1 if we should
     *   display all rows
     */
    public InlineResultsTable(Collection results, Model model,
                              WebConfig webConfig, Map webProperties, 
                              Map classKeys, int size) {
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

        Iterator columnIter = fieldConfigs.iterator();

        while (columnIter.hasNext()) {
            columnNames.add(((FieldConfig) columnIter.next()).getFieldExpr());
        }
        return Collections.unmodifiableList(columnNames);
    }

    /**
     * Return the full name of the field shown in this column in ClassName.fieldName format.
     * eg. Gene.organismDbId
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
            ids.add(((InterMineObject) i.next()).getId());
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
        fieldConfigs = new ArrayList(); 
        columnFullNames = new ArrayList();
        subList = new ArrayList();
        
        Iterator resultsIter;
        if (getSize() == -1) {
            resultsIter = resultsAsList.iterator();
        } else {
            resultsIter = resultsAsList.subList(0, getSize()).iterator();
        }

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

            List objectFieldConfigs = getRowFieldConfigs(o);
            Iterator objectFieldConfigIter = objectFieldConfigs.iterator();

            while (objectFieldConfigIter.hasNext()) {
                FieldConfig fc = (FieldConfig) objectFieldConfigIter.next();

                if (!fieldConfigs.contains(fc)) {
                    fieldConfigs.add(fc);

                    String expr = fc.getFieldExpr();

                    // only add full column names for simple fieldConfigs - ie. ones that specify a
                    // field in the current class
                    columnFullNames.add(theClass.getUnqualifiedName() + "." + expr);
                }
            }
        }
    }

    /**
     * Find the FieldConfig objects for the the given Object.
     * @param rowObject an Object
     * @return the FieldConfig objects for the the given Object.
     */
    protected List getRowFieldConfigs(Object rowObject) {
        List returnFieldConfigs = new ArrayList();

        Set objectClassDescriptors = DisplayObject.getLeafClds(rowObject.getClass(), model);

        Iterator classDescriptorsIter = objectClassDescriptors.iterator();

        while (classDescriptorsIter.hasNext()) {
            ClassDescriptor thisClassDescriptor = (ClassDescriptor) classDescriptorsIter.next();

            List rowFieldConfigs = getClassFieldConfigs(thisClassDescriptor);

            Iterator fieldConfigIterator = rowFieldConfigs.iterator();

            while (fieldConfigIterator.hasNext()) {
                FieldConfig fc = (FieldConfig) fieldConfigIterator.next();

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
    protected List getClassFieldConfigs(ClassDescriptor cd) {
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
    public List getFieldConfigs() {
        return fieldConfigs;
    }

    /**
     * Return a List containing the ResultElements for a given row in the table.
     * @param os The ObjectStore to pass to the Path constructor
     * @param rowIndex the index of the row to create the List for
     * @return a List of ResultElements, one for each column returned by getColumnFullNames().  If
     *   a particular column isn't relavent for this row, that element of the List will be null.
     */
    public List getResultElementRow(ObjectStore os, int rowIndex) {
        InterMineObject o = (InterMineObject) getRowObjects().get(rowIndex);
        List retList = new ArrayList();
        for (int i = 0; i < getColumnFullNames().size(); i++) {
            Path path;
            try {
                path = new Path(model, (String) getColumnFullNames().get(i));
                Class endType = path.getEndType();
                if (endType == null) {
                    // the end of the Path is not an attribute
                    retList.add(null);
                    continue;
                }
                String endTypeName = TypeUtil.unqualifiedName(endType.getName());
                String lastFieldName = path.getEndFieldDescriptor().getName();
                boolean isKeyField =
                    ClassKeyHelper.isKeyField(classKeys, endTypeName, lastFieldName);
                ResultElement resultElement = new ResultElement(os, path.resolve(o), o.getId(),
                                                                endTypeName, path, isKeyField);
                retList.add(resultElement);
            } catch (PathError e) {
                // if the field can't be resolved then this Path doesn't make sense for this object
                retList.add(null);
            }
        }
        return retList;
    }
}
