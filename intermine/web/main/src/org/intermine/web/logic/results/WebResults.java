package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.flatouterjoins.MultiRow;
import org.intermine.objectstore.flatouterjoins.MultiRowFirstValue;
import org.intermine.objectstore.flatouterjoins.MultiRowValue;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.path.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.PathQueryBinding;

/**
 * The web version of a Results object.  This class handles the mapping between the paths that user
 * selected for the view and the objects that are returned from the query.
 *
 * @author Kim Rutherford
 */
public class WebResults extends AbstractList<List<Object>> implements WebTable
{
    protected static final Logger LOG = Logger.getLogger(WebResults.class);
    private List<Path> columnPaths;
    protected LinkedHashMap pathToIndex;
    protected Model model;
    private final List<Column> columns = new ArrayList<Column>();
    private Results osResults;
    private List columnNames;
    private Map classKeys;
    private Map<String, QuerySelectable> pathToQueryNode;
    private Map<String, BagQueryResult> pathToBagQueryResult;
    private PathQuery pathQuery;

    /**
     * Create a new WebResults object.
     *
     * @param pathQuery used to get the paths of the columns
     * @param results the underlying Results object
     * @param model the Model
     * @param pathToQueryNode the mapping between Paths (in the columnPaths argument) and the
     * QueryNodes in the results object
     * @param classKeys the Map from class name to set of defined keys
     * @param pathToBagQueryResult a Map containing results from LOOKUP operations
     */
    public WebResults(PathQuery pathQuery, Results results, Model model,
                      Map<String, QuerySelectable> pathToQueryNode,
                      Map classKeys, Map<String, BagQueryResult> pathToBagQueryResult) {
        this.osResults = results;
        this.model = model;
        this.columnPaths = pathQuery.getView();
        this.classKeys = classKeys;
        this.pathToQueryNode = pathToQueryNode;
        this.pathToBagQueryResult = pathToBagQueryResult;
        this.pathQuery = pathQuery;
        pathToIndex = getPathToIndex(osResults.getQuery(), pathToQueryNode);

        setColumns(columnPaths);
    }

    // pathToQueryNode is map from string paths to QueryNodes from ObjectStore query
    public static LinkedHashMap getPathToIndex(Query query, Map pathToQueryNode) {
        LinkedHashMap returnMap =  new LinkedHashMap();
        for (Iterator iter = pathToQueryNode.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            QuerySelectable queryNode = (QuerySelectable) pathToQueryNode.get(path);
            if ((queryNode instanceof QueryClass)
                    || (queryNode instanceof QueryObjectPathExpression)
                    || (queryNode instanceof QueryCollectionPathExpression)) {
                returnMap.put(path, new Integer(query.getSelect()
                                .indexOf(queryNode)));
            } else if (queryNode instanceof QueryField) {
                QueryField queryField = (QueryField) queryNode;
                returnMap.put(path, new Integer(query.getSelect()
                                .indexOf(queryField.getFromElement())));
            } else {
                throw new RuntimeException();
            }
        }
        return returnMap;
    }


    /**
     * Return the names of the columns of the results.
     *
     * @return the column name
     */
    public List getColumnNames() {
        return columnNames;
    }

    // TODO javadoc to describe what this does
    private void setColumns(List<Path> columnPaths) {
        List<String> types = new ArrayList<String>();
        int i = 0;
        for (Iterator<Path> iter = columnPaths.iterator(); iter.hasNext();) {
            Object columnPathObject = iter.next();

            Path columnPath = (Path) columnPathObject;
            String type = TypeUtil.unqualifiedName(columnPath.getLastClassDescriptor()
                                                   .getName());
            Class typeCls = columnPath.getLastClassDescriptor().getType();

            String columnString = columnPath.toString();
            int dotIndex = columnString.lastIndexOf('.');
            if (dotIndex == -1) {
                throw new StringIndexOutOfBoundsException("Could not find a dot in "
                        + columnString);
            }
            String columnPrefix = columnString.substring(0, dotIndex);
            String columnPathEnd = columnString.substring(dotIndex + 1);

            String columnDescription = pathQuery.getPathDescription(columnPrefix);
            Column column;

            if (columnDescription == null) {
                column = new Column(columnPath, i, typeCls);
            } else {
                column = new Column(columnPath, columnDescription + '.' + columnPathEnd, i,
                                    typeCls);
            }

            if (!types.contains(column.getColumnId())) {
                String fieldName = columnPath.getEndFieldDescriptor().getName();
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

    /**
     * {@inheritDoc}
     */
    public List<Object> get(int index) {
        return getElementsInternal(index, false);
    }

    /**
     * {@inheritDoc}
     */
    public int getEstimatedSize() {
       try {
           return getInfo().getRows();
       } catch (ObjectStoreException e) {
           throw new RuntimeException("failed to get a ResultsInfo object", e);
       }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSizeEstimate() {
        try {
            return getInfo().getStatus() != ResultsInfo.SIZE;
        } catch (ObjectStoreException e) {
            throw new RuntimeException("failed to get a ResultsInfo object", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public int size() {
        return getInterMineResults().size();
    }

    /**
     * Return the ResultsInfo object from the underlying Results object.
     *
     * @return the ResultsInfo object
     * @throws ObjectStoreException if there is an exception while getting the info
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        return osResults.getInfo();
    }

    /**
     * Return the underlying results object.
     *
     * @return the results
     */
    public Results getInterMineResults() {
        return osResults;
    }

    /**
     * Returns the pathToQueryNode Map.
     *
     * @return a Map
     */
    public Map getPathToQueryNode() {
        return pathToQueryNode;
    }

    /**
     * Returns the pathToBagQueryResult Map.
     *
     * @return a Map
     */
    public Map<String, BagQueryResult> getPathToBagQueryResult() {
        return pathToBagQueryResult;
    }

    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 5000;

    /**
     * Calls ObjectStore.goFaster() if this object wraps a Results object from an
     * ObjectStoreInterMineImpl.
     * @throws ObjectStoreException if ObjectStoreInterMineImpl.goFaster() throws the exception
     */
    public void goFaster() throws ObjectStoreException {
        osResults = WebUtil.changeResultBatchSize(osResults, BIG_BATCH_SIZE);
        ObjectStore os = osResults.getObjectStore();
        if (os instanceof ObjectStoreInterMineImpl) {
            ((ObjectStoreInterMineImpl) os).goFaster(osResults.getQuery());
        }
    }

    /**
     * Calls ObjectStore.releaseGoFaster() if this object wraps a Results object from an
     * ObjectStoreInterMineImpl.
     * @throws ObjectStoreException if ObjectStoreInterMineImpl.releaseGoFaster() throws the
     * exception
     */
    public void releaseGoFaster() throws ObjectStoreException {
        ObjectStore os = osResults.getObjectStore();
        if (os instanceof ObjectStoreInterMineImpl) {
            ((ObjectStoreInterMineImpl) os).releaseGoFaster(osResults.getQuery());
        }
    }

    /**
     * Returns the ObjectStore's maximum allowable offset.
     *
     * @return an int
     */
    public int getMaxRetrievableIndex() {
        return osResults.getObjectStore().getMaxOffset();
    }

    /**
     * Return a List containing a ResultElement object for each element in the given row.  The List
     * will be the same length as the view List.
     *
     * @param index the row of the results to fetch
     * @return the results row as ResultElement objects
     */
    public List<ResultElement> getResultElements(int index) {
        return getElementsInternal(index, true);
    }

    private List getElementsInternal(int index, boolean makeResultElements) {
        ResultsRow resultsRow = (ResultsRow) osResults.get(index);
        return translateRow(resultsRow, makeResultElements);
     }

    // TODO javadoc to describe what this does
    private List translateRow(List initialList, boolean makeResultElements) {
        if (initialList instanceof MultiRow) {
            MultiRow retval = new MultiRow();
            for (ResultsRow origRow : ((List<ResultsRow>) initialList)) {
                retval.add(translateRow(origRow, makeResultElements));
            }
            return retval;
        }
        ArrayList rowCells = new ResultsRow();
        for (Iterator iter = columnPaths.iterator(); iter.hasNext();) {
            Path columnPath = (Path) iter.next();
            String columnName = columnPath.toStringNoConstraints();
            Integer columnIndexInteger = (Integer) pathToIndex.get(columnName);
            String parentColumnName = columnPath.getPrefix().toStringNoConstraints();
            if (columnIndexInteger == null) {
                columnIndexInteger = (Integer) pathToIndex.get(parentColumnName);
            }
            if (columnIndexInteger == null) {
                throw new NullPointerException("Path: \"" + columnName + "\", pathToIndex: \""
                        + pathToIndex + "\", prefix: \"" + parentColumnName + "\", query: \""
                        + PathQueryBinding.marshal(pathQuery, "", pathQuery.getModel().getName())
                        + "\"");
            }
            int columnIndex = columnIndexInteger.intValue();
            Object origO = initialList.get(columnIndex);
            Object o = origO;
            int rowspan = -1;
            if (o instanceof MultiRowValue) {
                if (o instanceof MultiRowFirstValue) {
                    rowspan = ((MultiRowFirstValue) o).getRowspan();
                }
                o = ((MultiRowValue) o).getValue();
            }
            String type = TypeUtil.unqualifiedName(columnPath.getLastClassDescriptor().getName());
            String fieldName = columnName.substring(columnName.lastIndexOf(".") + 1);
            Path path = new Path(model, type + '.' + fieldName);
            if (o instanceof Collection) {
                if (((Collection) o).isEmpty()) {
                    o = null;
                } else {
                    o = ((Collection) o).iterator().next();
                }
            }
            // Three cases:
            // 1) attribute has a value so create a result element
            // 2) we have an object but attribute is null -> create a ResultElement with value null
            // 3) the object is null (outer join) so add null value rowCells
            Object fieldValue = (o == null ? null : path.resolve(o));
            if (makeResultElements && o != null) {
                String fieldCDName = path.getLastClassDescriptor().getName();
                String unqualifiedFieldCD = TypeUtil.unqualifiedName(fieldCDName);
                boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, unqualifiedFieldCD,
                                                               fieldName);
                Set classes = DynamicUtil.decomposeClass(o.getClass());
                Class cls = (Class) classes.iterator().next();
                ResultElement resultElement =
                    new ResultElement(osResults.getObjectStore(),
                                      fieldValue, (o instanceof InterMineObject
                                          ? ((InterMineObject) o).getId()
                                          : null), cls, columnPath, isKeyField);
                if (rowspan >= 0) {
                    rowCells.add(new MultiRowFirstValue(resultElement, rowspan));
                } else {
                    rowCells.add(resultElement);
                }
            } else {
                if (rowspan >= 0) {
                    rowCells.add(new MultiRowFirstValue(fieldValue, rowspan));
                } else {
                    rowCells.add(fieldValue);
                }
            }
        }
        return rowCells;
    }

    public Iterator iterator() {
        return new Iter();
    }


    /**
     * Returns the columns for these results.
     *
     * @return the columns
     */
    public List getColumns() {
        return columns;
    }

    public PathQuery getPathQuery() {
        return pathQuery;
    }

    private class Iter implements Iterator
    {
        private Iterator subIter;

        public Iter() {
            subIter = osResults.iterator();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return subIter.hasNext();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public Object next() {
            List row = (List) subIter.next();
            return translateRow(row, true);
        }

        public void remove() {
            throw (new UnsupportedOperationException());
        }

    }

}
