package org.intermine.api.results.flatouterjoins;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.metadata.Util;

/**
 * Provides an implementation of List that encapsulates another List (like a Results object), and
 * translates row into flat MultiRow format in a way suitable for displaying in the webapp. Note
 * that this makes the results of a query significantly different. Instead of each row containing a
 * ResultsRow (List of column values), each row contains a MultiRow (list of sub-rows), which
 * contains ResultsRows, which contain MultiRowValues, which contain the actual values.
 *
 * @author Matthew Wakeling
 */
public class ResultsFlatOuterJoinsImpl extends AbstractList<MultiRow<ResultsRow<MultiRowValue>>>
{
    protected static final Logger LOG = Logger.getLogger(ResultsFlatOuterJoinsImpl.class);

    private List<ResultsRow> orig;
    private Query query;
    private int[] columnWidth;
    private List columnTypes;
    private int columnCount = 0;

    /**
     * Constructor for this object.
     *
     * @param orig the List&lt;ResultsRow&gt; to encapsulate
     * @param query the Query that generated the results, in order to get the collection layout
     */
    public ResultsFlatOuterJoinsImpl(List<ResultsRow> orig, Query query) {
        this.orig = orig;
        this.query = query;
        columnWidth = new int[query.getSelect().size()];
        for (int i = 0; i < columnWidth.length; i++) {
            QuerySelectable qs = query.getSelect().get(i);
            if (qs instanceof QueryCollectionPathExpression) {
                columnWidth[i] = ((QueryCollectionPathExpression) qs).getSelect().size();
                columnWidth[i] = (columnWidth[i] == 0 ? 1 : columnWidth[i]);
            } else {
                columnWidth[i] = 1;
            }
        }
        columnTypes = convertColumnTypes(query.getSelect());
    }

    /**
     * {@inheritDoc}
     */
    public MultiRow<ResultsRow<MultiRowValue>> get(int index) {
        return translateRow2(orig.get(index));
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return orig.size();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<MultiRow<ResultsRow<MultiRowValue>>> iterator() {
        return new Iter(orig.iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<MultiRow<ResultsRow<MultiRowValue>>> iteratorFrom(int index) {
        if (((List) orig) instanceof Results) {
            return new Iter((Iterator) ((Results) ((List) orig)).iteratorFrom(index));
        } else {
            return new Iter(orig.subList(index, orig.size()).iterator());
        }
    }

    private class Iter implements Iterator<MultiRow<ResultsRow<MultiRowValue>>>
    {
        private Iterator<ResultsRow> origIter;

        public Iter(Iterator<ResultsRow> origIter) {
            this.origIter = origIter;
        }

        public boolean hasNext() {
            return origIter.hasNext();
        }

        public MultiRow<ResultsRow<MultiRowValue>> next() {
            return translateRow2(origIter.next());
        }

        public void remove() {
            throw (new UnsupportedOperationException());
        }
    }

    private MultiRow<ResultsRow<MultiRowValue>> translateRow(ResultsRow origRow) {
        int columns = origRow.size();
        int lcm = 1;
        int[] collectionSizes = new int[columns];
        int[] rowPosition = new int[columns];
        Collection<MultiRowLaterValue>[] multiRowLaterValue
            = new Collection[columns];
        for (int column = 0; column < columns; column++) {
            Object o = origRow.get(column);
            if (o instanceof Collection) {
                int colSize = ((Collection) o).size();
                collectionSizes[column] = (colSize == 0 ? 1 : colSize);
                lcm = Util.lcm(lcm, collectionSizes[column]);
            } else {
                collectionSizes[column] = 1;
            }
            rowPosition[column] = 0;
        }
        MultiRow<ResultsRow<MultiRowValue>> multiRow
            = new MultiRow<ResultsRow<MultiRowValue>>();
        for (int subRowNo = 0; subRowNo < lcm; subRowNo++) {
            ResultsRow<MultiRowValue> subRow = new ResultsRow<MultiRowValue>();
            for (int column = 0; column < columns; column++) {
                if (rowPosition[column] <= 0) {
                    Object columnValue = origRow.get(column);
                    if ((columnValue instanceof List)
                            && (((List) columnValue).size() > 0)) {
                        columnValue = ((List) columnValue).get(subRowNo
                                / (lcm / collectionSizes[column]));
                    } else if (columnValue instanceof List) {
                        columnValue = null;
                    }
                    if (columnValue == null) {
                        for (int i = 0; i < columnWidth[column]; i++) {
                            MultiRowFirstValue value = new MultiRowFirstValue(null,
                                    lcm / collectionSizes[column]);
                            subRow.add(value);
                            multiRowLaterValue[column] = Collections
                                .singletonList(value.getMrlv());
                            rowPosition[column] = lcm / collectionSizes[column] - 1;
                        }
                    } else if (columnValue instanceof ResultsRow) {
                        multiRowLaterValue[column] = new ArrayList<MultiRowLaterValue>();
                        for (Object columnValue2 : ((ResultsRow) columnValue)) {
                            MultiRowFirstValue value = new MultiRowFirstValue(columnValue2,
                                    lcm / collectionSizes[column]);
                            subRow.add(value);
                            multiRowLaterValue[column].add(value.getMrlv());
                            rowPosition[column] = lcm / collectionSizes[column] - 1;
                        }
                    } else {
                        MultiRowFirstValue value = new MultiRowFirstValue(columnValue,
                                    lcm / collectionSizes[column]);
                        subRow.add(value);
                        multiRowLaterValue[column] = Collections
                            .singletonList(value.getMrlv());
                        rowPosition[column] = lcm / collectionSizes[column] - 1;
                    }
                } else {
                    subRow.addAll(multiRowLaterValue[column]);
                    rowPosition[column]--;
                }
            }
            multiRow.add(subRow);
        }
        return multiRow;
    }

    private List convertColumnTypes(List<? extends QuerySelectable> select) {
        List retval = new ArrayList();
        for (QuerySelectable qs : select) {
            boolean notFinished = true;
            while (notFinished) {
                if (qs instanceof QueryObjectPathExpression) {
                    QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                    List<QuerySelectable> subSelect = qope.getSelect();
                    if (!subSelect.isEmpty()) {
                        qs = subSelect.get(0);
                    } else {
                        notFinished = false;
                    }
                } else if (qs instanceof PathExpressionField) {
                    PathExpressionField pef = (PathExpressionField) qs;
                    qs = pef.getQope().getSelect().get(pef.getFieldNumber());
                } else {
                    notFinished = false;
                }
            }
            if (qs instanceof QueryCollectionPathExpression) {
                QueryCollectionPathExpression qc = (QueryCollectionPathExpression) qs;
                List<QuerySelectable> subSelect = qc.getSelect();
                if (qc.isSingleton()) {
                    if (subSelect.isEmpty()) {
                        retval.add(new ResultsRow(convertColumnTypes(Collections.singletonList(
                                            qc.getDefaultClass()))));
                    } else {
                        retval.add(new ResultsRow(convertColumnTypes(subSelect)));
                    }
                } else {
                    if (subSelect.isEmpty()) {
                        retval.add(convertColumnTypes(Collections.singletonList(
                                        qc.getDefaultClass())));
                    } else {
                        retval.add(convertColumnTypes(subSelect));
                    }
                }
            } else {
                retval.add(new Integer(columnCount++));
            }
        }
        return retval;
    }

    private MultiRow<ResultsRow<MultiRowValue>> translateRow2(ResultsRow origRow) {
        try {
            ResultsRow<MultiRowValue> template = new ResultsRow<MultiRowValue>();
            for (int i = 0; i < columnCount; i++) {
                template.add(null);
            }
            MultiRow<ResultsRow<MultiRowValue>> retval = new MultiRow<ResultsRow<MultiRowValue>>();
            expandCollections(origRow, retval, template, columnTypes, 0);
            return retval;
        } catch (RuntimeException e) {
            LOG.error("Exception while translating row " + origRow);
            LOG.error("columnTypes = " + columnTypes);
            throw e;
        }
    }

    private int expandCollections(List row, MultiRow<ResultsRow<MultiRowValue>> retval,
            ResultsRow<MultiRowValue> template, List columns, int startRow) {
        if (row.size() != columns.size()) {
            throw new IllegalArgumentException("Column description (size " + columns.size()
                    + ") does not match input data (size " + row.size() + "), for query "
                    + query + " and data row " + row);
        }
        int columnNo = 0;
        int maxRowNo = startRow + 1;
        for (Object column : columns) {
            int rowNo = startRow;
            if (column instanceof ResultsRow) {
                List collection = (List) row.get(columnNo);
                if (collection != null) {
                    for (Object subRow : collection) {
                        rowNo = expandCollections(Collections.singletonList(subRow), retval,
                                template, (List) column, rowNo);
                    }
                }
            } else if (column instanceof List) {
                List<ResultsRow> collection = (List<ResultsRow>) row.get(columnNo);
                if (collection != null) {
                    for (ResultsRow subRow : collection) {
                        rowNo = expandCollections(subRow, retval, template, (List) column, rowNo);
                    }
                }
            }
            maxRowNo = Math.max(maxRowNo, rowNo);
            columnNo++;
        }

        if (retval.size() < maxRowNo) {
            retval.add(new ResultsRow<MultiRowValue>(template));
        }
        columnNo = 0;
        for (Object column : columns) {
            if (column instanceof Integer) {
                int outColumnNo = ((Integer) column).intValue();
                MultiRowFirstValue firstValue = new MultiRowFirstValue(row.get(columnNo),
                        maxRowNo - startRow);
                retval.get(startRow).set(outColumnNo, firstValue);
                MultiRowLaterValue laterValue = firstValue.getMrlv();
                for (int i = startRow + 1; i < maxRowNo; i++) {
                    ResultsRow<MultiRowValue> subRow = retval.get(i);
                    subRow.set(outColumnNo, laterValue);
                }
            }
            columnNo++;
        }
        return maxRowNo;
    }

    /**
     * Converts a SELECT list from a normal query into a representation of the columns returned
     * in this converted list.
     *
     * @return a List of QuerySelectables corresponding to the columns returned in this list
     */
    public List<QuerySelectable> getFlatSelect() {
        ArrayList<QuerySelectable> retval = new ArrayList<QuerySelectable>();
        addFlatSelect(retval, query.getSelect(), null, null);
        return retval;
    }

    private static void addFlatSelect(List<QuerySelectable> toAdd, List<QuerySelectable> select,
            QuerySelectable defaultClass, QuerySelectable substitute) {
        if (select.isEmpty() && substitute != null) {
            toAdd.add(substitute);
        } else {
            for (QuerySelectable s : select) {
                while (s instanceof PathExpressionField) {
                    PathExpressionField pef = (PathExpressionField) s;
                    s = pef.getQope().getSelect().get(pef.getFieldNumber());
                    if (s == pef.getQope().getDefaultClass()) {
                        s = pef.getQope();
                    }
                }
                if (s == defaultClass) {
                    toAdd.add(substitute);
                } else if (s instanceof QueryCollectionPathExpression) {
                    addFlatSelect(toAdd, ((QueryCollectionPathExpression) s).getSelect(),
                            ((QueryCollectionPathExpression) s).getDefaultClass(), s);
                } else {
                    toAdd.add(s);
                }
            }
        }
    }
}
