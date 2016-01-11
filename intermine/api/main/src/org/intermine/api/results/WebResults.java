package org.intermine.api.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.LinkRedirectManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowFirstValue;
import org.intermine.api.results.flatouterjoins.MultiRowValue;
import org.intermine.api.results.flatouterjoins.ResultsFlatOuterJoinsImpl;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
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
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * The web version of a Results object.  This class handles the mapping between the paths that user
 * selected for the view and the objects that are returned from the query.
 *
 * @author Kim Rutherford
 */
public class WebResults
    extends AbstractList<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>>
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
    private LinkRedirectManager redirector;
    private InterMineAPI im;

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
     * @param im intermine API
     * @param pathToQueryNode the mapping between Paths (in the columnPaths argument) and the
     * QueryNodes in the results object
     * @param pathToBagQueryResult a Map containing results from LOOKUP operations
     */
    public WebResults(InterMineAPI im, PathQuery pathQuery, Results results,
            Map<String, QuerySelectable> pathToQueryNode,
            Map<String, BagQueryResult> pathToBagQueryResult) {
        this.im = im;
        this.osResults = results;
        this.flatResults = new ResultsFlatOuterJoinsImpl(((List) osResults),
                osResults.getQuery());
        model = im.getModel();
        this.columnPaths = new ArrayList<Path>();
        try {
            for (String pathString : pathQuery.getView()) {
                this.columnPaths.add(pathQuery.makePath(pathString));
            }
        } catch (PathException e) {
            throw new RuntimeException("Error creating WebResults because PathQuery is invalid", e);
        }
        classKeys = im.getClassKeys();
        this.pathToQueryNode = new HashMap<String, QuerySelectable>();
        if (pathToQueryNode != null) {
            this.pathToQueryNode.putAll(pathToQueryNode);
        }
        this.pathToBagQueryResult = new HashMap<String, BagQueryResult>();
        if (pathToBagQueryResult != null) {
            this.pathToBagQueryResult.putAll(pathToBagQueryResult);
        }
        this.pathQuery = pathQuery;
        pathToIndex = getPathToIndex();
        redirector = im.getLinkRedirector();
        addColumnsInternal(columnPaths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleBatch() {
        return osResults.isSingleBatch();
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
    public List<String> getColumnNames() {
        return columnNames;
    }


    @Override
    public void addColumns(List<Path> paths) {
        addColumnsInternal(paths);
    }

    private void addColumnsInternal(List<Path> paths) {
        List<String> types = new ArrayList<String>();
        int i = columns.size();
        for (Path columnPath : paths) {
            String type = TypeUtil.unqualifiedName(columnPath.getLastClassDescriptor()
                                                   .getName());
            Class typeCls = columnPath.getLastClassDescriptor().getType();

            String columnDescription = pathQuery.getGeneratedPathDescription(columnPath.
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
    @Override
    public MultiRow<ResultsRow<MultiRowValue<ResultElement>>> get(int index) {
        //throw new RuntimeException("Throwing exception in WebResults.get because it has always "
        //        + "returned an incorrect result.");
        return getResultElements(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEstimatedSize() {
        try {
            return getInfo().getRows();
        } catch (ObjectStoreException e) {
            // whoops.  return zero so we can post a nice error message
            LOG.error("failed to get a ResultsInfo object", e);
            return 0;
            //throw new RuntimeException("failed to get a ResultsInfo object", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
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
            osResults = changeResultBatchSize(osResults, BIG_BATCH_SIZE);
            ObjectStore os = osResults.getObjectStore();
            if (os instanceof ObjectStoreInterMineImpl) {
                ((ObjectStoreInterMineImpl) os).goFaster(osResults.getQuery());
            }
        }
    }

    /**
     * Make a copy of a Results object, but with a different batch size.
     * @param oldResults the original Results objects
     * @param newBatchSize the new batch size
     * @return a new Results object with a new batch size
     */
    private static Results changeResultBatchSize(Results oldResults, int newBatchSize) {
        Results newResults = oldResults.getObjectStore().execute(oldResults.getQuery(),
                newBatchSize, true, true, true);
        return newResults;
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
    @Override
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
    @Override
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
                                    pathQuery.getModel().getName(),
                                    PathQuery.USERPROFILE_VERSION) + "\"");
                    }
                    int columnIndex = columnIndexInteger.intValue();
                    MultiRowValue origO = initialList.get(columnIndex);
                    FastPathObject o = (FastPathObject) (origO == null ? null : origO.getValue());
                    int rowspan = -1;
                    if (origO == null) {
                        rowspan = 1;
                    } else if (origO instanceof MultiRowFirstValue) {
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
                    try {
                        if (columnPath.endIsAttribute()) {
                            fieldName = columnName.substring(columnName.lastIndexOf(".") + 1);
                            path = new Path(model, type + '.' + fieldName);
                        } else {
                            // special case for columns that contain objects
                            path = new Path(model, type);
                        }
                    } catch (PathException e) {
                        // Should never happen if the field name is valid
                        throw new Error("There must be a bug", e);
                    }

                    // Three cases:
                    // 1) attribute has a value so create a result element
                    // 2) we have an object but attribute is null -> create a ResultElement with
                    // value null
                    // 3) the object is null (outer join) so add null value rowCells
//                    Object fieldValue;
//                    try {
//                        fieldValue = (o == null ? null : PathUtil.resolvePath(path, o));
//                    } catch (PathException e) {
//                        throw new IllegalArgumentException("Path: \"" + columnName
//                                + "\", pathToIndex: \"" + pathToIndex + "\", prefix: \""
//                                + parentColumnName + "\", query: \""
//                                + PathQueryBinding.marshal(pathQuery, "", pathQuery.getModel()
//                                    .getName(), PathQuery.USERPROFILE_VERSION)
//                                + "\", columnIndex: \"" + columnIndex + "\", initialList: \""
//                                + initialList + "\"", e);
//                    }
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
                        // link to report page by default, unless it says otherwise in config

                        if (redirector != null) {
                            try {
                                String linkRedirect =
                                        redirector.generateLink(im, (InterMineObject) o);
                                if (linkRedirect != null) {
                                    resultElement.setLinkRedirect(linkRedirect);
                                }
                            } catch (ClassCastException e) {
                                // Simple objects cannot be consumed by redirectors.
                            }
                        }

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
    @Override
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
    @Override
    public List<Column> getColumns() {
        return columns;
    }

    @Override
    public List<Path> getColumnsPath() {
        return columnPaths;
    }

    /**
     * @return path query
     */
    @Override
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
        @Override
        public boolean hasNext() {
            return subIter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MultiRow<ResultsRow<MultiRowValue<ResultElement>>> next() {
            return translateRow(subIter.next());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw (new UnsupportedOperationException());
        }

    }

}
