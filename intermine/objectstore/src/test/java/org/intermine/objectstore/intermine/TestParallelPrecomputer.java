package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
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

import org.intermine.objectstore.query.Query;

/**
 * Test implementation of ParallelPrecomputer.
 *
 * @author Matthew Wakeling
 */

public class TestParallelPrecomputer extends ParallelPrecomputer
{
    public TestParallelPrecomputer(ObjectStoreInterMineImpl os, int threadCount) {
        super(os, threadCount);
    }

    public List<Query> testQueries = new ArrayList<Query>();

    @Override
    protected void precomputeQuery(String key, Query query, Collection indexes, boolean allFields,
            String category, int threadNo) {
        testQueries.add(query);
    }
}
