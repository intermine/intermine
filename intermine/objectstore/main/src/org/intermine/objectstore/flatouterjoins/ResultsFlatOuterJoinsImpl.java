package org.intermine.objectstore.flatouterjoins;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.Util;

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
    private List<ResultsRow> orig;
    private Query query;
    private int columnWidth[];

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
    }

    /**
     * {@inheritDoc}
     */
    public MultiRow<ResultsRow<MultiRowValue>> get(int index) {
        return translateRow(orig.get(index));
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
        if (orig instanceof Results) {
            return new Iter((Iterator<ResultsRow>) ((Results) orig).iteratorFrom(index));
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
            return translateRow(origIter.next());
        }

        public void remove() {
            throw (new UnsupportedOperationException());
        }
    }

    private MultiRow<ResultsRow<MultiRowValue>> translateRow(ResultsRow origRow) {
        int columns = origRow.size();
        int lcm = 1;
        int collectionSizes[] = new int[columns];
        int rowPosition[] = new int[columns];
        Collection<MultiRowLaterValue> multiRowLaterValue[]
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
