package org.intermine.objectstore.flatouterjoins;

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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.Util;

import org.apache.log4j.Logger;

/**
 * Provides an implementation of an ObjectStore that flattens the contents of outer join collections
 * in a way suitable for displaying in the webapp. Note that this makes the results of a query
 * significantly different. Instead of each row containing a ResultsRow (List of column values),
 * each row contains a MultiRow (list of sub-rows), which contains ResultsRows, which contain
 * MultiRowValues, which contain the actual values.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreFlatOuterJoinsImpl extends ObjectStorePassthruImpl
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreFlatOuterJoinsImpl.class);

    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreFlatOuterJoinsImpl(ObjectStore os) {
        super(os);
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q) {
        return new Results(q, this, getSequence(getComponentsForQuery(q)));
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q) {
        return new SingletonResults(q, this, getSequence(getComponentsForQuery(q)));
    }


    /**
     * {@inheritDoc}
     */
    public List<ResultsRow> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        List<ResultsRow> orig = os.execute(q, start, limit, optimise, explain, sequence);
        List<ResultsRow> retval = new ArrayList();
        int columnWidth[] = new int[q.getSelect().size()];
        for (int i = 0; i < columnWidth.length; i++) {
            QuerySelectable qs = q.getSelect().get(i);
            if (qs instanceof QueryCollectionPathExpression) {
                columnWidth[i] = ((QueryCollectionPathExpression) qs).getSelect().size();
                columnWidth[i] = (columnWidth[i] == 0 ? 1 : columnWidth[i]);
            } else {
                columnWidth[i] = 1;
            }
        }
        for (ResultsRow origRow : orig) {
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
            if (lcm == 1) {
                ResultsRow newRow = new ResultsRow();
                for (int column = 0; column < columns; column++) {
                    Object columnValue = origRow.get(column);
                    if ((columnValue instanceof List) && (((List) columnValue).size() > 0)) {
                        // Because lcm = 1, we know that columnValue.size() cannot be more than one.
                        columnValue = ((List) columnValue).get(0);
                    } else if (columnValue instanceof List) {
                        columnValue = null;
                    }
                    if (columnValue == null) {
                        for (int i = 0; i < columnWidth[column]; i++) {
                            newRow.add(null);
                        }
                    } else if (columnValue instanceof ResultsRow) {
                        for (Object columnValue2 : ((ResultsRow) columnValue)) {
                            newRow.add(columnValue2);
                        }
                    } else {
                        newRow.add(columnValue);
                    }
                }
                //LOG.error("Translated row " + origRow + " into " + newRow);
                retval.add(newRow);
            } else {
                MultiRow multiRow = new MultiRow();
                for (int subRowNo = 0; subRowNo < lcm; subRowNo++) {
                    ResultsRow subRow = new ResultsRow();
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
                //LOG.error("Translated row " + origRow + " into " + multiRow);
                retval.add(multiRow);
            }
        }
        return retval;
    }

    /**
     * Converts a SELECT list from a normal query into a representation of the columns returned
     * by this ObjectStore.
     *
     * @param select a List of QuerySelectables - Query.getSelect()
     * @return a List of QuerySelectables corresponding to the columns returned in the results
     * of this ObjectStore
     */
    public static List<QuerySelectable> getFlatSelect(List<QuerySelectable> select) {
        ArrayList<QuerySelectable> retval = new ArrayList<QuerySelectable>();
        addFlatSelect(retval, select, null, null);
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
