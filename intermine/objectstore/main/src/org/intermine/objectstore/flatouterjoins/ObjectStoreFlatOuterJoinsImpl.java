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
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.Util;

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
        for (ResultsRow origRow : orig) {
            int columns = origRow.size();
            int lcm = 1;
            int collectionSizes[] = new int[columns];
            int rowPosition[] = new int[columns];
            MultiRowLaterValue multiRowLaterValue[] = new MultiRowLaterValue[columns];
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
                        columnValue = ((List) columnValue).get(0);
                    }
                    newRow.add(columnValue);
                }
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
                            }
                            MultiRowFirstValue value = new MultiRowFirstValue(columnValue,
                                        lcm / collectionSizes[column]);
                            subRow.add(value);
                            multiRowLaterValue[column] = value.getMrlv();
                            rowPosition[column] = lcm / collectionSizes[column] - 1;
                        } else {
                            subRow.add(multiRowLaterValue[column]);
                            rowPosition[column]--;
                        }
                    }
                    multiRow.add(subRow);
                }
                retval.add(multiRow);
            }
        }
        return retval;
    }
}
