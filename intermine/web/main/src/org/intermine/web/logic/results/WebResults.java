package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathError;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.PathUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.results.flatouterjoins.MultiRow;
import org.intermine.web.logic.results.flatouterjoins.MultiRowFirstValue;
import org.intermine.web.logic.results.flatouterjoins.MultiRowValue;
import org.intermine.web.logic.results.flatouterjoins.ResultsFlatOuterJoinsImpl;

/**
 * The web version of a Results object.  This class handles the mapping between the paths that user
 * selected for the view and the objects that are returned from the query.
 *
 * @author Kim Rutherford
 */
public class WebResults extends AbstractList<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>>
implements WebTable
{
    protected static final Logger LOG = Logger.getLogger(WebResults.class);
    private List<Path> columnPaths;
    protected LinkedHashMap<String, Integer> pathToIndex;
    protected Model model;
    private final List<Column> columns = new ArrayList<Column>();
    private Results osResults;
    private ResultsFlatOuterJoinsImpl flatResults;
    private List columnNames;
    private Map<String, List<FieldDescriptor>> classKeys;
    private Map<String, QuerySelectable> pathToQueryNode;
    private Map<String, BagQueryResult> pathToBagQueryResult;
    private PathQuery pathQuery;

    // incremented each time goFaster() is called, and decremented each time releaseGoFaster() is
    // called.  the objectstore goFaster() method is only called when goingFaster == 1 and the
    // objectstore releaseGoFaster() is only called when goingFaster == 0, so that multiple threads
    // can call goFaster() on this WebResult
    private int goingFaster = 0;

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
                      Map<String, List<FieldDescriptor>> classKeys, 
                      Map<String, BagQueryResult> pathToBagQueryResult) {
        this.osResults = results;
        this.flatResults = new ResultsFlatOuterJoinsImpl((List<ResultsRow>) osResults,
                osResults.getQuery());
        this.model = model;
        this.columnPaths = pathQuery.getView();
        this.classKeys = classKeys;
        this.pathToQueryNode = pathToQueryNode;
        this.pathToBagQueryResult = pathToBagQueryResult;
        this.pathQuery = pathQuery;
        pathToIndex = getPathToIndex();

        addColumnsInternal(columnPaths);
    }

    /**
     * Create a map from string paths to index of QueryNodes in the ObjectStore query
     *
     * @return a map from string paths to the index of QueryNodes
     */
    protected LinkedHashMap<String, Integer> getPathToIndex() {
        List<QuerySelectable> select = flatResults.getFlatSelect();
        List<QuerySelectable> convSelect = new ArrayList<QuerySelectable>();
        for (QuerySelectable qs : select) {
            while (qs instanceof PathExpressionField) {
                QueryObjectPathExpression qope = ((PathExpressionField) qs).getQope();
                qs = qope.getSelect().get(((PathExpressionField) qs).getFieldNumber());
                if (qs.equals(qope.getDefaultClass())) {
                    qs = qope;
                }
            }
            convSelect.add(qs);
        }
        select = convSelect;
        LinkedHashMap<String, Integer> returnMap =  new LinkedHashMap<String, Integer>();
        for (String path : pathToQueryNode.keySet()) {
            QuerySelectable queryNode = pathToQueryNode.get(path);
            if ((queryNode instanceof QueryClass)
                    || (queryNode instanceof QueryObjectPathExpression)
                    || (queryNode instanceof QueryCollectionPathExpression)) {
                int index = select.indexOf(queryNode);
                if (index != -1) {
                    returnMap.put(path, new Integer(index));
                }
            } else if (queryNode instanceof QueryField) {
                String parentPath = path.substring(0, path.lastIndexOf('.'));
                queryNode = pathToQueryNode.get(parentPath);
                int index = select.indexOf(queryNode);
                if (index != -1) {
                    returnMap.put(path, new Integer(index));
                }
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

    /**
     * Adds columns that should be displayed to the table.
     * @param columnPaths columns correspond to paths and columns for these paths should be added
     */
    public void addColumns(List<Path> columnPaths) {
        addColumnsInternal(columnPaths);
    }

    private void addColumnsInternal(List<Path> columnPaths) {
        List<String> types = new ArrayList<String>();
        int i = columns.size();
        for (Path columnPath : columnPaths) {
            String type = TypeUtil.unqualifiedName(columnPath.getLastClassDescriptor()
                                                   .getName());
            Class typeCls = columnPath.getLastClassDescriptor().getType();

            String columnDescription = pathQuery.getPathDescription(columnPath.
                    toStringNoConstraints());
            Column column;

            if (columnDescription.equals(columnPath.toStringNoConstraints())) {
                column = new Column(columnPath, i, typeCls);
            } else {
                column = new Column(columnPath, columnDescription, i,
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
    public MultiRow<ResultsRow<MultiRowValue<ResultElement>>> get(int index) {
        //throw new RuntimeException("Throwing exception in WebResults.get because it has always "
        //        + "returned an incorrect result.");
        return getResultElements(index);
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
    public Map<String, QuerySelectable> getPathToQueryNode() {
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
    public synchronized void goFaster() throws ObjectStoreException {
        goingFaster++;
        if (goingFaster == 1) {
            osResults = WebUtil.changeResultBatchSize(osResults, BIG_BATCH_SIZE);
            ObjectStore os = osResults.getObjectStore();
            if (os instanceof ObjectStoreInterMineImpl) {
                ((ObjectStoreInterMineImpl) os).goFaster(osResults.getQuery());
            }
        }
    }

    /**
     * Calls ObjectStore.releaseGoFaster() if this object wraps a Results object from an
     * ObjectStoreInterMineImpl.
     * @throws ObjectStoreException if ObjectStoreInterMineImpl.releaseGoFaster() throws the
     * exception
     */
    public synchronized void releaseGoFaster() throws ObjectStoreException {
        goingFaster--;
        if (goingFaster == 0) {
            ObjectStore os = osResults.getObjectStore();
            if (os instanceof ObjectStoreInterMineImpl) {
                ((ObjectStoreInterMineImpl) os).releaseGoFaster(osResults.getQuery());
            }
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
    public MultiRow<ResultsRow<MultiRowValue<ResultElement>>> getResultElements(int index) {
        return translateRow(flatResults.get(index));
    }

    // TODO javadoc to describe what this does
    private MultiRow<ResultsRow<MultiRowValue<ResultElement>>> translateRow(
            MultiRow<ResultsRow<MultiRowValue>> multiRow) {
        try {
            MultiRow<ResultsRow<MultiRowValue<ResultElement>>> retval
                = new MultiRow<ResultsRow<MultiRowValue<ResultElement>>>();
            for (ResultsRow<MultiRowValue> initialList : multiRow) {
                ResultsRow<MultiRowValue<ResultElement>> rowCells
                    = new ResultsRow<MultiRowValue<ResultElement>>();
                for (Path columnPath : columnPaths) {
                    String columnName = columnPath.toStringNoConstraints();
                    Integer columnIndexInteger = pathToIndex.get(columnName);
                    String parentColumnName = columnPath.getPrefix().toStringNoConstraints();
                    if (columnIndexInteger == null) {
                        columnIndexInteger = pathToIndex.get(parentColumnName);
                    }

                    if (columnIndexInteger == null) {
                        throw new NullPointerException("Path: \"" + columnName
                                + "\", pathToIndex: \"" + pathToIndex + "\", prefix: \""
                                + parentColumnName + "\", query: \""
                                + PathQueryBinding.marshal(pathQuery, "",
                                    pathQuery.getModel().getName()) + "\"");
                    }
                    int columnIndex = columnIndexInteger.intValue();
                    MultiRowValue origO = initialList.get(columnIndex);
                    Object o = origO.getValue();
                    int rowspan = -1;
                    if (origO instanceof MultiRowFirstValue) {
                        rowspan = ((MultiRowFirstValue) origO).getRowspan();
                    }
                    String lastCd;
                    if (columnPath.endIsAttribute()) {
                        lastCd = columnPath.getLastClassDescriptor().getName();
                    } else {
                        // special case for columns that contain objects eg. Gene.chromosomeLocation
                        lastCd = columnPath.getLastClassDescriptor().getName();
                    }
                    String type = TypeUtil.unqualifiedName(lastCd);
                    Path path;
                    String fieldName = null;
                    if (columnPath.endIsAttribute()) {
                        fieldName = columnName.substring(columnName.lastIndexOf(".") + 1);
                        path = new Path(model, type + '.' + fieldName);
                    } else {
                        // special case for columns that contain objects
                        path = new Path(model, type);
                    }
                    if (o instanceof Collection) {
                        if (((Collection) o).isEmpty()) {
                            o = null;
                        } else {
                            o = ((Collection) o).iterator().next();
                        }
                    }
                    // Three cases:
                    // 1) attribute has a value so create a result element
                    // 2) we have an object but attribute is null -> create a ResultElement with
                    // value null
                    // 3) the object is null (outer join) so add null value rowCells
                    Object fieldValue;
                    try {
                        fieldValue = (o == null ? null : PathUtil.resolvePath(path, o));
                    } catch (PathError e) {
                            throw new IllegalArgumentException(
                            "Path: \""
                            + columnName
                            + "\", pathToIndex: \""
                            + pathToIndex
                            + "\", prefix: \""
                            + parentColumnName
                            + "\", query: \""
                            + PathQueryBinding.marshal(pathQuery, "",
                                            pathQuery.getModel().getName())
                            + "\", columnIndex: \"" + columnIndex
                            + "\", initialList: \"" + initialList + "\"", e);
                    }
                    if (o != null) {
                        String fieldCDName = path.getLastClassDescriptor().getName();
                        String unqualifiedFieldCD = TypeUtil.unqualifiedName(fieldCDName);
                        boolean isKeyField;
                        if (fieldName == null) {
                            // special case for columns that contain objects
                            isKeyField = false;
                        } else {
                            isKeyField = ClassKeyHelper.isKeyField(classKeys, unqualifiedFieldCD,
                                            fieldName);
                        }
                        ResultElement resultElement = new ResultElement(o, path, isKeyField);
                        if (rowspan >= 0) {
                            rowCells.add(new MultiRowFirstValue(resultElement, rowspan));
                        } else {
                            rowCells.add(null);
                        }
                    } else {
                        if (rowspan >= 0) {
                            rowCells.add(new MultiRowFirstValue(null, rowspan));
                        } else {
                            rowCells.add(null);
                        }
                    }
                }
                retval.add(rowCells);
            }
            return retval;
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return iterator over results
     */
    public Iterator<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>> iterator() {
        return new Iter();
    }

    /**
     * @param start - first index from which start iteration
     * @return iterator over results
     */
    public Iterator<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>> iteratorFrom(int start) {
        return new Iter(start);
    }

    /**
     * Returns the columns for these results.
     *
     * @return the columns
     */
    public List getColumns() {
        return columns;
    }

    /**
     * @return path query
     */
    public PathQuery getPathQuery() {
        return pathQuery;
    }

    private class Iter implements Iterator<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>>
    {
        private Iterator<MultiRow<ResultsRow<MultiRowValue>>> subIter;

        public Iter(int start) {
            subIter = flatResults.iteratorFrom(start);
        }        
        
        public Iter() {
            subIter = flatResults.iterator();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return subIter.hasNext();
        }

        /** 
         * {@inheritDoc}
         */
        public MultiRow<ResultsRow<MultiRowValue<ResultElement>>> next() {
            return translateRow(subIter.next());
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw (new UnsupportedOperationException());
        }

    }

}
