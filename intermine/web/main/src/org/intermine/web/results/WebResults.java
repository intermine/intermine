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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.ClassKeyHelper;
import org.intermine.web.WebUtil;

import org.apache.log4j.Logger;

/**
 * The web version of a Results object.  This class handles the mapping between the paths that user
 * selected for the view and the objects that are returned from the query.
 * @author Kim Rutherford
 */
public class WebResults extends AbstractList
{
    protected static final Logger LOG = Logger.getLogger(WebResults.class);
    private List columnPaths;
    protected LinkedHashMap pathToIndex;
    protected LinkedHashMap pathToType = new LinkedHashMap();
    protected Model model;
    private final List columns = new ArrayList();
    private Results osResults;
    private List columnNames;
    private Map classKeys;
    
    /**
     * Create a new WebResults object.
     * @param columnPaths the Path objects representing the view
     * @param results the underlying Results object
     * @param model the Model
     * @param pathToQueryNode the mapping between Paths (in the columnPaths argument) and the 
     * QueryNodes in the results object
     * @param classKeys the Map from class name to set of defined keys
     */
    public WebResults(List columnPaths, Results results, Model model, Map pathToQueryNode, 
                      Map classKeys) {
        this.osResults = results;
        this.model = model;
        this.columnPaths= columnPaths;
        this.classKeys = classKeys;
        
        pathToIndex = getPathToIndex(pathToQueryNode);
        setColumns(columnPaths);
    }
 
    private LinkedHashMap getPathToIndex(Map pathToQueryNode){
        LinkedHashMap returnMap =  new LinkedHashMap();
        for (Iterator iter = pathToQueryNode.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            QueryNode queryNode = (QueryNode) pathToQueryNode.get(path);
            if (queryNode instanceof QueryClass){
                returnMap.put(path, new Integer(osResults.getQuery().getSelect()
                                .indexOf(queryNode)));
            } else if (queryNode instanceof QueryField){
                QueryField queryField = (QueryField) queryNode;
                returnMap.put(path, new Integer(osResults.getQuery().getSelect()
                                .indexOf(queryField.getFromElement())));
            } else {
                throw new RuntimeException();
            }
        }
        return returnMap;
    }


    /**
     * Return the names of the columns of the results
     * @return the column name
     */
    public List getColumnNames() {
        return columnNames;
    }
    
    private void setColumns(List columnPaths) {
        List types = new ArrayList();
        int i = 0;
        for (Iterator iter = columnPaths.iterator(); iter.hasNext();) {
            Path columnPath = (Path) iter.next();
            Column column = new Column();
            column.setPath(columnPath);
            column.setIndex(i);
            column.setVisible(true);
            String type = null;
            if (columnPath.getElements().size() >= 2) {
                Object pathElement = columnPath.getElements().get(columnPath.getElements().size() - 2);
                if (pathElement instanceof ReferenceDescriptor) {
                    ReferenceDescriptor refdesc = (ReferenceDescriptor) pathElement;
                    type = TypeUtil.unqualifiedName(refdesc.getReferencedClassName());
                }
            } else {
                type = TypeUtil.unqualifiedName(columnPath.getStartClassDescriptor().getName());
            }
            pathToType.put(columnPath.toStringNoConstraints(), type);
            column.setType(type);
            if (!types.contains(type)) {
                String fieldName = columnPath.getEndFieldDescriptor().getName();
                boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, type, fieldName);
                if (isKeyField) {
                    column.setSelectable(true);
                    types.add(type);
                }
            }
            columns.add(column);
            i++;
        }
    }

    /* (non-Javadoc)
     * @see java.util.AbstractList#get(int)
     */
    public Object get(int index) {
        return getElementsInternal(index, false);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    public int size() {
       return getInterMineResults().size();
    }

    /**
     * Return the ResultsInfo object from the underlying Results object.
     * @return the ResultsInfo object
     * @throws ObjectStoreException if there is an exception while getting the info
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        return osResults.getInfo();
    }

    /**
     * Return the underlying results object.
     * @return the results
     */
    public Results getInterMineResults() {
        return osResults;
    }

    
    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 5000;

    /**
     * @throws ObjectStoreException 
     * 
     */
    public void goFaster() throws ObjectStoreException {
        osResults = WebUtil.changeResultBatchSize(osResults, BIG_BATCH_SIZE);
        ObjectStore os = osResults.getObjectStore();
        if (os instanceof ObjectStoreInterMineImpl) {
            ((ObjectStoreInterMineImpl) os).goFaster(osResults.getQuery());
        }
    }

    /**
     * @throws ObjectStoreException 
     * 
     */
    public void releaseGoFaster() throws ObjectStoreException {

        ObjectStore os = osResults.getObjectStore();
        if (os instanceof ObjectStoreInterMineImpl) {
            ((ObjectStoreInterMineImpl) os).releaseGoFaster(osResults.getQuery());
        }
 
    }


    /**
     * @return
     */
    public int getMaxRetrievableIndex() {
        return osResults.getObjectStore().getMaxOffset();
    }

    /**
     * Return a List containing a ResultElement object for each element given rows.  The List will
     * be the same length as the view List.
     * @param index the row of the results to fetch
     * @return the results row
     */
    public List getResultElements(int index) {
        return getElementsInternal(index, true);
    }

    private List getElementsInternal(int index, boolean makeResultElements) {
        ResultsRow resultsRow = (ResultsRow) osResults.get(index);
        ArrayList rowCells = new ArrayList();
        for (Iterator iter = columnPaths.iterator(); iter.hasNext();) {
            Path columnPath = (Path) iter.next();
            String columnName = columnPath.toStringNoConstraints();
            int columnIndex = ((Integer) pathToIndex.get(columnName)).intValue();
            InterMineObject o = (InterMineObject) resultsRow.get(columnIndex);
            String type;
            if (columnPath.getElements().size() >= 2) {
                List elementCDs = columnPath.getElementClassDescriptors();
                type = ((ClassDescriptor) elementCDs.get(elementCDs.size() - 1)).getName();
                type = TypeUtil.unqualifiedName(type);
            } else {
                type = TypeUtil.unqualifiedName(columnPath.getStartClassDescriptor().getName());
            }
            String fieldName = columnName.substring(columnName.lastIndexOf(".") + 1);
            Path path = new Path(model, type + '.' + fieldName);
            Object fieldValue = path.resolve(o);
            if (makeResultElements) {
                String fieldCDName = path.getLastClassDescriptor().getName();
                String unqualifiedFeldCD = TypeUtil.unqualifiedName(fieldCDName);
                boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, unqualifiedFeldCD,
                                                               fieldName);
                String className =
                    TypeUtil.unqualifiedName(path.getStartClassDescriptor().getName());
                ResultElement resultElement = 
                    new ResultElement(osResults.getObjectStore(), 
                                      fieldValue, o.getId(), className, isKeyField);
                rowCells.add(resultElement);
            } else {
                rowCells.add(fieldValue);
            }
        }
        return rowCells;
     }


    /**
     * @return the columns
     */
    public List getColumns() {
        return columns;
    }
}
