package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ParallelPrecomputer;
import org.intermine.objectstore.intermine.TestParallelPrecomputer;

/**
 * Test implementation of PrecomputeTask.
 *
 * @author Matthew Wakeling
 */

public class TestPrecomputeTask extends PrecomputeTask
{
    private ParallelPrecomputer testPrecomputer = null;

    protected ParallelPrecomputer getPrecomputer(ObjectStoreInterMineImpl os) {
        if (testPrecomputer == null) {
            testPrecomputer = new TestParallelPrecomputer(os, THREAD_COUNT);
        }
        return testPrecomputer;
    }
}
