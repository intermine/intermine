package org.intermine.api.results;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.query.QueryExecutor;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;

/**
 * An Iterator that produces data in a format suitable for exporting. The data is flattened, so if
 * there are outer joined collections, there may be more rows than in the original results.
 *
 * @author Matthew Wakeling
 */
public class ExportResultsIterator implements Iterator<List<ResultElement>>
{
    private static final Logger LOG = Logger.getLogger(ExportResultsIterator.class);

    private Iterator<List> osIter;
    protected Iterator<List<ResultElement>> subIter;
    // This object contains a description of the collections in the input.
    private List columns;
    private final List<Path> paths = new ArrayList<Path>();
    private int columnCount;
    protected final Results results;
    private boolean isGoingFaster = false;
    protected final PathQuery originatingQuery;

     /**
     * Constructor for ExportResultsIterator. This creates a new instance from the given
     * ObjectStore, PathQuery, and other necessary objects.
     *
     * @param pathQuery a PathQuery to run
     * @param results the results object created when executing the query
     * @param pathToQueryNode a map from path in pathQuery to QuerySelectable in the generated
     * ObjectStore query
     * @throws ObjectStoreException if something goes wrong executing the query
     */
    public ExportResultsIterator(PathQuery pathQuery, Results results,
            Map<String, QuerySelectable> pathToQueryNode) throws ObjectStoreException {
        this.results = results;
        this.originatingQuery = pathQuery;
        init(pathQuery, pathToQueryNode);
    }

    public PathQuery getQuery() {
        return originatingQuery;
    }

    public List<Path> getViewPaths() {
        return Collections.unmodifiableList(paths);
    }

    private void init(PathQuery pq, Map<String, QuerySelectable> pathToQueryNode) {
        osIter = ((List) results).iterator();
      
        List<List<ResultElement>> empty = Collections.emptyList();
        subIter = empty.iterator();
        for (String pathString : pq.getView()) {
            Path path;
            try {
                path = pq.makePath(pathString);
                paths.add(path);
            } catch (PathException e) {
                throw new RuntimeException("Path " + pathString
                        + " in view of PathQuery is invalid", e);
            }
        }
        columns = convertColumnTypes(results.getQuery().getSelect(), pq, pathToQueryNode);
        columnCount = pq.getView().size();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        while ((!subIter.hasNext()) && osIter.hasNext()) {
            subIter = decodeRow(osIter.next()).iterator();
        }
        return subIter.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResultElement> next() {
        while ((!subIter.hasNext()) && osIter.hasNext()) {
            subIter = decodeRow(osIter.next()).iterator();
        }
        return subIter.next();
    }

    /**
     * This method is not supported.
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Switches on the goFaster mode in the objectstore for this query.
     *
     * @throws RuntimeException if something goes wrong
     */
    public void goFaster()  {
        try {
            if ((!results.isSingleBatch()) && (!isGoingFaster)) {
                ((ObjectStoreInterMineImpl) results.getObjectStore()).goFaster(results.getQuery());
                isGoingFaster = true;
            }
        } catch (ObjectStoreException ex) {
            LOG.warn("Error happened during executing goFaster method.", ex);
        }
    }

    /**
     * Switches off the goFaster mode in the objectstore for this query.
     *
     * @throws RuntimeException if something goes wrong
     */
    public void releaseGoFaster() {
        try {
            if (isGoingFaster) {
                ((ObjectStoreInterMineImpl) results.getObjectStore()).releaseGoFaster(results
                        .getQuery());
            }
        } catch (ObjectStoreException ex) {
            LOG.warn("Error happened during executing releaseGoFaster method.", ex);
        }
    }

    /** Analyses the select list to predict what the structure of the results will be. It produces
     *  a list with a disjoint type of element.
     *  
     * Some examples of the return values are presented below:
     * <pre>
     * [
     *    {Employee.age=6, Employee.fullTime=4, Employee.name=0},
     *    [{Employee.department.name=1}, {Employee.department.manager.name=2}, {Employee.department.company.name=3}], 
     *    {Employee.address.address=5}
     * ]
     * [  
     *    {Company.vatNumber=6, Company.name=0},
     *    [
     *      {Company.departments.name=1}, 
     *      [
     *        {Company.departments.employees.age=3, Company.departments.employees.name=2},
     *        {Company.departments.employees.address.address=4}
     *      ]
     *    ], 
     *    [
     *      {Company.secretarys.name=5}
     *    ]
     * ]
     * </pre>
     */
    private List convertColumnTypes(List<? extends QuerySelectable> select, PathQuery pq,
            Map<String, QuerySelectable> pathToQueryNode) {
        List retval = new ArrayList();
        for (QuerySelectable qs : select) {
            boolean notFinished = true;
            while (notFinished) {
                if (qs instanceof QueryObjectPathExpression) {
                    QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                    List<QuerySelectable> subSelect = qope.getSelect();
                    if (!subSelect.isEmpty()) {
                        qs = subSelect.get(0);
                        if (qs.equals(qope.getDefaultClass())) {
                            qs = qope;
                            notFinished = false;
                        }
                    } else {
                        notFinished = false;
                    }
                } else if (qs instanceof PathExpressionField) {
                    PathExpressionField pef = (PathExpressionField) qs;
                    QueryObjectPathExpression qope = pef.getQope();
                    qs = qope.getSelect().get(pef.getFieldNumber());
                    if (qs.equals(qope.getDefaultClass())) {
                        qs = qope;
                        notFinished = false;
                    }
                } else {
                    notFinished = false;
                }
            }
            if (qs instanceof QueryCollectionPathExpression) {
                QueryCollectionPathExpression qc = (QueryCollectionPathExpression) qs;
                List<QuerySelectable> subSelect = qc.getSelect();
                if (subSelect.isEmpty()) {
                    retval.add(convertColumnTypes(Collections.singletonList(qc.getDefaultClass()),
                                pq, pathToQueryNode));
                } else {
                    retval.add(convertColumnTypes(subSelect, pq, pathToQueryNode));
                }
            } else {
                Map<Path, Integer> fieldToColumnNumber = new HashMap<Path, Integer>();
                int columnNo = 0;
                for (Path path : paths) {
                    Path parent = path.getPrefix();
                    QuerySelectable selectableForPath = pathToQueryNode.get(
                            parent.toStringNoConstraints());
                    if (selectableForPath instanceof QueryCollectionPathExpression) {
                        selectableForPath = ((QueryCollectionPathExpression) selectableForPath)
                            .getDefaultClass();
                    }
                    if (qs.equals(selectableForPath)) {
                        fieldToColumnNumber.put(path, new Integer(columnNo));
                    }
                    columnNo++;
                }
                retval.add(fieldToColumnNumber);
            }
        }
        return retval;
    }

    /**
     * Allows test to access column info.
     *
     * @return columns
     */
    protected List getColumns() {
        return columns;
    }

    private List<List<ResultElement>> decodeRow(List row) {
        List<ResultElement> template = new ArrayList<ResultElement>();
        for (int i = 0; i < columnCount; i++) {
            template.add(null);
        }
        List<List<ResultElement>> retval = new ArrayList<List<ResultElement>>();
        expandCollections(row, retval, template, columns);
        return retval;
    }

    private void expandCollections(List row, List<List<ResultElement>> retval,
            List<ResultElement> template, List cols) {
        if (row.size() != cols.size()) {
            throw new IllegalArgumentException("Column description (size " + cols.size()
                    + ") does not match input data (size " + row.size() + ")");
        }
        List<ResultElement> templateResults = new ArrayList(template);
        int columnNo = 0;
        boolean multiRow = false;
        for (Object column : cols) {
            if (column instanceof Map) {
                Map<Path, Integer> desc = (Map<Path, Integer>) column;
                for (Map.Entry<Path, Integer> descEntry : desc.entrySet()) {
                    templateResults.set(descEntry.getValue().intValue(),
                            new ResultElement((FastPathObject) row.get(columnNo),
                                descEntry.getKey(), false));
                }
            } else if (!multiRow) {
                // Check the collection size, to see if we can get away with a single row.
                List<List> collection = (List<List>) row.get(columnNo);
                if (collection != null) {
                    if (collection.size() > 1) {
                        multiRow = true;
                    } else if (collection.size() == 1) {
                        multiRow = isCollectionMultiRow(collection.iterator().next(),
                                (List) column);
                    }
                }
            }
            columnNo++;
        }
        boolean hasCollections = false;
        columnNo = 0;
        for (Object column : cols) {
            if (column instanceof List) {
                List<List> collection = (List<List>) row.get(columnNo);
                if (collection != null) {
                    for (List subRow : collection) {
                        if (multiRow) {
                            hasCollections = true;
                            expandCollections(subRow,
                                    retval, templateResults, (List) column);
                        } else {
                            expandCollectionsJustOneRow(subRow,
                                    retval, templateResults, (List) column);
                        }
                    }
                }
            }
            columnNo++;
        }
        if (!hasCollections) {
            retval.add(templateResults);
        }
    }

    private void expandCollectionsJustOneRow(List row, List<List<ResultElement>> retval,
            List<ResultElement> template, List cols) {
        if (row.size() != cols.size()) {
            throw new IllegalArgumentException("Column description (size " + cols.size()
                    + ") does not match input data (size " + row.size() + ")");
        }
        int columnNo = 0;
        for (Object column : cols) {
            if (column instanceof Map) {
                Map<Path, Integer> desc = (Map<Path, Integer>) column;
                for (Map.Entry<Path, Integer> descEntry : desc.entrySet()) {
                    template.set(descEntry.getValue().intValue(),
                            new ResultElement((FastPathObject) row.get(columnNo),
                                descEntry.getKey(), false));
                }
            } else {
                List<List> collection = (List<List>) row.get(columnNo);
                for (List subRow : collection) {
                    expandCollectionsJustOneRow(subRow, retval, template, (List) column);
                }
            }
            columnNo++;
        }
    }

    private boolean isCollectionMultiRow(List row, List cols) {
        boolean multiRow = false;
        int columnNo = 0;
        for (Object column : cols) {
            if ((column instanceof List) && (!multiRow)) {
                List<List> collection = (List<List>) row.get(columnNo);
                if (collection.size() > 1) {
                    multiRow = true;
                } else if (collection.size() == 1) {
                    multiRow = isCollectionMultiRow(collection.iterator().next(), (List) column);
                }
            }
            columnNo++;
        }
        return multiRow;
    }
}
