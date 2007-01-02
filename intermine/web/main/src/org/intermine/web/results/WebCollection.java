package org.intermine.web.results;

/* 
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.ClassKeyHelper;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.config.FieldConfig;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.WebConfig;

/**
 * @author kmr
 *
 */
public class WebCollection extends AbstractList implements WebColumnTable
{
    private List list;
    private Model model;
    private WebConfig webConfig;
    private Map classKeys;
    private final String columnName;
    private final ObjectStore os;

    /**
     * Create a new WebCollection object.
     * @param os the ObjectStore used to create ResultElement objects
     * @param columnName the String to use when displaying this collection - used as the column name
     * for the single column of results
     * @param collection the Collection, which can be a List of objects or a List of List of
     * objects (like a Results object)
     * @param model the Model to use when making Path objects
     * @param webConfig the WebConfig object the configures the columns in the view
     * @param classKeys map of classname to set of keys
     */
    public WebCollection(ObjectStore os, String columnName, Collection collection, Model model, 
                         WebConfig webConfig, Map classKeys) {
        this.os = os;
        this.model = model;
        this.webConfig = webConfig;
        this.columnName = columnName;
        if (collection instanceof List) {
              list = (List) collection;
          } else {
              if (collection instanceof InterMineBag) {
                  try {
                      list = ((InterMineBag) collection).getInterMineObjects();
                  } catch (ObjectStoreException e) {
                      throw new RuntimeException(e.getMessage());
                  }
              } else {
                  list = new ArrayList(collection);
              }
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
     * @see java.util.AbstractList#get(int)
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
            String newColumnName = ((Column) iterator.next()).getPath().toString();
            Path path = new Path(model, newColumnName);
            Object fieldValue = path.resolve(o);
            if (makeResultElements) {
                String type = null;
                if (path.getElements().size() >= 2) {
                     Object pathElement = 
                        path.getElements().get(path.getElements().size() - 2);
                    if (pathElement instanceof ReferenceDescriptor) {
                        ReferenceDescriptor refdesc = (ReferenceDescriptor) pathElement;
                        type = TypeUtil.unqualifiedName(refdesc.getReferencedClassName());
                    }
                } else {
                    type = path.getStartClassDescriptor().getType().getName();
                }
                String unqualifiedClassName = TypeUtil.unqualifiedName(type);
                String fieldName = path.getEndFieldDescriptor().getName();
                boolean isKeyField =
                    ClassKeyHelper.isKeyField(classKeys, unqualifiedClassName, fieldName);
                rowCells.add(new ResultElement(os, fieldValue, o.getId(), type, isKeyField));
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
     * @see java.util.AbstractCollection#size()
     */
    public int size() {
        return list.size();
    }

    /**
     * Return the List of Column objects for this WebCollection, configured by the WebConfig for
     * the class of the collection we're showing
     * @return the Column object List
     */
    public List getColumns() {
        List columns = new ArrayList();
        Path path = new Path(model, columnName);
        int i = 0;
        if (path.getEndFieldDescriptor() == null || path.endIsReference()) {
            ClassDescriptor cld = path.getEndClassDescriptor();
            List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
            Iterator cldFieldConfigIter = cldFieldConfigs.iterator();
            while (cldFieldConfigIter.hasNext()) {
                FieldConfig fc = (FieldConfig) cldFieldConfigIter.next();
                String fieldExpr = fc.getFieldExpr();
                String newColumnName = columnName + "." + fieldExpr;
                Path colPath = new Path(model, newColumnName);
                String type = null;
                if (colPath.getElements().size() >= 2) {
                    Object pathElement = colPath.getElements().get(colPath.getElements().size() - 2);
                    if (pathElement instanceof ReferenceDescriptor) {
                        ReferenceDescriptor refdesc = (ReferenceDescriptor) pathElement;
                        type = TypeUtil.unqualifiedName(refdesc.getReferencedClassName());
                    }
                } else {
                    type = columnName;
                }
                Column column = new Column(colPath, i, type);
                columns.add(column);
                i++;
            }
        }
        return columns;
    }
}
