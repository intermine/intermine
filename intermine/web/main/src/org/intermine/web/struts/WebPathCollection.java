package org.intermine.web.struts;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebTable;

/**
 * A wrapper for a collection that makes for easier rendering in the webapp.
 * @author kmr
 */
public class WebPathCollection extends AbstractList implements WebTable
{
    private List list;
    private Model model;
    private WebConfig webConfig;
    private Map classKeys;
    private final String columnName;
    private final ObjectStore os;
    private Path columnPath;

    /**
     * Create a new WebPathCollection object.
     * @param os the ObjectStore used to create ResultElement objects
     * @param columnPath the Path to use when displaying this collection - used as the column name
     * for the single column of results
     * @param collection the Collection, which can be a List of objects or a List of List of
     * objects (like a Results object)
     * @param model the Model to use when making Path objects
     * @param webConfig the WebConfig object the configures the columns in the view
     * @param classKeys map of classname to set of keys
     */
    public WebPathCollection(ObjectStore os, Path columnPath, Collection collection, Model model, 
                         WebConfig webConfig, Map classKeys) {
        this.os = os;
        this.model = model;
        this.webConfig = webConfig;
        this.columnPath = columnPath;
        this.columnName = columnPath.toStringNoConstraints();
        if (collection instanceof List) {
            list = (List) collection;
        } else {
            list = new ArrayList(collection);
        }
        this.classKeys = classKeys;
    }

    /**
     * Return a List containing a ResultElement object for each element in the given row.  The List
     * will be the same length as the view List.
     * @param index the row of the results to fetch
     * @return the results row as ResultElement objects
     */
    public List getResultElements(int index) {
        return getElementsInternal(index, true);
    }

    /**
     * Return the given row as a List of primatives (rather than a List of ResultElement objects)
     * {@inheritDoc}
     */
    public Object get(int index) {
        return getElementsInternal(index, false);
    }

    private List getElementsInternal(int index, boolean makeResultElements) {
        Object object = getList().get(index);
        InterMineObject o = null;
        if (object instanceof ResultsRow) {
            ResultsRow resRow = (ResultsRow) object;
            o = (InterMineObject) resRow.get(0);
        } else if (object instanceof InterMineObject) {
            o = (InterMineObject) object;
        } else {
            throw new RuntimeException();
        }
        ArrayList rowCells = new ArrayList();
        for (Iterator iterator = getColumns().iterator(); iterator.hasNext();) {
            String newColumnName = ((Column) iterator.next()).getName();
            Path path = new Path(model, newColumnName);
            Object fieldValue = path.resolve(o);
            if (makeResultElements) {
                String type = TypeUtil.unqualifiedName(path.getStartClassDescriptor().getName());
                String fieldName = path.getEndFieldDescriptor().getName();
                boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, type,
                                                               fieldName);
                rowCells.add(new ResultElement(os, fieldValue, o.getId(), type,
                                               path, isKeyField));
            } else {
                rowCells.add(fieldValue);
            }
        }
        return rowCells;
    }
    
    private List getList() {
        return list;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        if (list instanceof LazyCollection) {
            try {
                return ((LazyCollection) list).getInfo().getRows();
            } catch (ObjectStoreException e) {
                throw new RuntimeException("unable to get size for collection named: "
                                           + columnPath);
            }
        } else {
            return list.size();
        }
    }

    /**
     * Return the List of Column objects for this WebPathCollection, configured by the WebConfig for
     * the class of the collection we're showing
     * @return the Column object List
     */
    public List getColumns() {
        List<Column> columns = new ArrayList<Column>();
        if (columnPath == null) {
            // we are showing a random collection of objects
            columns.add(new Column(columnName, 0, Object.class));
        } else {
            List<String> types = new ArrayList<String>();
            int i = 0;
            if (columnPath.getEndFieldDescriptor() == null || columnPath.endIsReference()) {
                ClassDescriptor cld = columnPath.getEndClassDescriptor();
                List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
                Iterator cldFieldConfigIter = cldFieldConfigs.iterator();
                while (cldFieldConfigIter.hasNext()) {
                    FieldConfig fc = (FieldConfig) cldFieldConfigIter.next();
                    if (!fc.getShowInResults()) {
                        continue;
                    }
                    String fieldExpr = fc.getFieldExpr();
                    String newColumnName = columnName + "." + fieldExpr;
                    Path colPath = new Path(model, newColumnName);
                    String type = null;
                    if (colPath.getElements().size() >= 2) {
                        Object pathElement =
                            colPath.getElements().get(colPath.getElements().size() - 2);
                        if (pathElement instanceof ReferenceDescriptor) {
                            ReferenceDescriptor refdesc = (ReferenceDescriptor) pathElement;
                            type = TypeUtil.unqualifiedName(refdesc.getReferencedClassName());
                        }
                    } else {
                        type = columnName;
                    }
                    Column column = new Column(colPath, i, type);
                    if (!types.contains(column.getColumnId())) {
                        String fieldName = colPath.getEndFieldDescriptor().getName();
                        boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, type, fieldName);
                        if (isKeyField) {
                            column.setSelectable(true);
                            types.add(column.getColumnId());
                        }
                    }
                    columns.add(column);
                    i++;
                }
            }
        }
        return columns;
    }

    /**
     * {@inheritDoc}
     */
    public int getExactSize() {
        return list.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSizeEstimate() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxRetrievableIndex() {
        return Integer.MAX_VALUE;
    }
}
