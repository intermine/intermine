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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.query.MainHelper;

/**
 * An Iterator that produces data in a format suitable for exporting. The data is flattened, so if
 * there are outer joined collections, there may be more rows than in the original results.
 *
 * @author Matthew Wakeling
 */
public class ExportResultsIterator implements Iterator<ResultsRow>
{
    private Iterator<ResultsRow> osIter;
    private Iterator<ResultsRow> subIter;
    // This object contains a description of the collections in the input.
    private List columns;
    private static final ResultsRow EMPTY_RESULTSROW = new ResultsRow();
    private int columnCount;

    /**
     * Constructor for ExportResultsIterator. This creates a new instance from the given
     * ObjectStore, PathQuery, and other necessary objects.
     *
     * @param os an ObjectStore that the query will be run on
     * @param pq a PathQuery to run
     * @param savedBags a Map of the bags that the query may have used
     * @param bagQueryRunner a BagQueryRunner for any LOOKUP constraints
     * @throws ObjectStoreException if something goes wrong executing the query
     */
    public ExportResultsIterator(ObjectStore os, PathQuery pq, Map savedBags,
            BagQueryRunner bagQueryRunner) throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Map returnBagQueryResults = new HashMap();
        Query q = MainHelper.makeQuery(pq, savedBags, pathToQueryNode, bagQueryRunner,
                returnBagQueryResults, false);
        osIter = os.execute(q).iterator();
        List<ResultsRow> empty = Collections.emptyList();
        subIter = empty.iterator();
        columns = convertColumnTypes(q.getSelect(), pq, pathToQueryNode);
        columnCount = pq.getView().size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        while ((!subIter.hasNext()) && osIter.hasNext()) {
            subIter = decodeRow(osIter.next()).iterator();
        }
        return subIter.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public ResultsRow next() {
        while ((!subIter.hasNext()) && osIter.hasNext()) {
            subIter = decodeRow(osIter.next()).iterator();
        }
        return subIter.next();
    }

    /**
     * This method is not supported.
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private List convertColumnTypes(List<? extends QuerySelectable> select, PathQuery pq,
            Map<String, QuerySelectable> pathToQueryNode) {
        List retval = new ArrayList();
        for (QuerySelectable qs : select) {
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
                for (Path path : pq.getView()) {
                    Path parent = path.getPrefix();
                    QuerySelectable selectableForPath = pathToQueryNode.get(
                            parent.toStringNoConstraints());
                    if (selectableForPath instanceof QueryCollectionPathExpression) {
                        selectableForPath = ((QueryCollectionPathExpression) selectableForPath)
                            .getDefaultClass();
                    }
                    if (qs.equals(selectableForPath)) {
                        fieldToColumnNumber.put(path, new Integer(columnNo));
                    } else {
                    }
                    columnNo++;
                }
                retval.add(fieldToColumnNumber);
            }
        }
        return retval;
    }

    private List<ResultsRow> decodeRow(ResultsRow row) {
        ResultsRow template = new ResultsRow();
        for (int i = 0; i < columnCount; i++) {
            template.add(null);
        }
        List<ResultsRow> retval = new ArrayList<ResultsRow>();
        expandCollections(row, retval, template, columns);
        return retval;
    }

    private void expandCollections(ResultsRow row, List<ResultsRow> retval, ResultsRow template,
            List columns) {
        if (row.size() != columns.size()) {
            throw new IllegalArgumentException("Column description (size " + columns.size()
                    + ") does not match input data (size " + row.size() + ")");
        }
        template = new ResultsRow(template);
        int columnNo = 0;
        for (Object column : columns) {
            if (column instanceof Map) {
                Map<Path, Integer> desc = (Map<Path, Integer>) column;
                for (Map.Entry<Path, Integer> descEntry : desc.entrySet()) {
                    template.set(descEntry.getValue().intValue(),
                            new ResultElement((InterMineObject) row.get(columnNo),
                                descEntry.getKey(), false));
                }
            }
            columnNo++;
        }
        boolean hasCollections = false;
        columnNo = 0;
        for (Object column : columns) {
            if (column instanceof List) {
                hasCollections = true;
                List<ResultsRow> collection = (List<ResultsRow>) row.get(columnNo);
                for (ResultsRow subRow : collection) {
                    expandCollections(subRow, retval, template, (List) column);
                }
            }
            columnNo++;
        }
        if (!hasCollections) {
            retval.add(template);
        }
    }
}
