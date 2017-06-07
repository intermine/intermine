package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Representation of a call to the Postgresql WIDTH_BUCKET function.
 * @author Alex Kalderimis
 */
public class WidthBucketFunction extends QueryFunction
{
    private QueryEvaluable upperBound;
    private QueryEvaluable lowerBound;
    private QueryEvaluable binWidth;

    /**
     * Constructor.
     * @param qe The expression to bin.
     * @param minQE The lower bound.
     * @param maxQE The upper bound.
     * @param bins The number of bins to separate the range into.
     */
    public WidthBucketFunction(QueryEvaluable qe,
            QueryEvaluable minQE, QueryEvaluable maxQE, QueryEvaluable bins) {
        super();
        QueryEvaluable[] args = {qe, maxQE, minQE, bins};
        for (int i = 0; i < args.length; i++) {
            QueryEvaluable arg = args[i];
            if ((arg instanceof QueryField) || (arg instanceof QueryExpression)
                    || (arg instanceof QueryCast) || (arg instanceof QueryForeignKey)
                    || (arg instanceof QueryValue) || (arg instanceof QueryFunction)) {
                // Cool
            } else {
                throw new IllegalArgumentException("Value unsuitable for WidthBucketFunction: "
                        + arg);
            }
        }
        for (int i = 0; i < 3; i++) {
            QueryEvaluable arg = args[i];
            if (!(Number.class.isAssignableFrom(arg.getType())
                    || arg.getType().equals(UnknownTypeValue.class))) {
                throw new IllegalArgumentException("Invalid argument type: Number expected");
            }
        }
        if (!(Integer.class.isAssignableFrom(bins.getType())
                || bins.getType().equals(UnknownTypeValue.class))) {
            throw new IllegalArgumentException("Invalid type for bins: Integer expected");
        }
        obj = qe;
        op = QueryFunction.WIDTH_BUCKET;
        upperBound = maxQE;
        lowerBound = minQE;
        binWidth = bins;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return Integer.class;
    }

    /**
     * @return The parameter defining the upper bound.
     */
    public QueryEvaluable getMaxParam() {
        return upperBound;
    }

    /**
     * @return The parameter defining the lower bound.
     */
    public QueryEvaluable getMinParam() {
        return lowerBound;
    }

    /**
     * @return The parameter defining the number of bins.
     */
    public QueryEvaluable getBinsParam() {
        return binWidth;
    }

}
