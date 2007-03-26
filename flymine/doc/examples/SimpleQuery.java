package org.flymine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.util.TextFileUtil;

/**
 * An example that runs a Query created from from a user IQL String.
 * @author Kim Rutherford
 */

public class SimpleQuery
{
    private String osAlias;
    private String iql;
    private ObjectStore os;

    public static void main(String[] args) throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.production");
        String packageName = os.getModel().getPackageName();
        String queryString = args[0];

        System.err.println ("qyery: " + queryString);
        Query q = new IqlQuery(queryString, packageName).toQuery();

// optional:
//         System.err.println ("starting precompute(): " + q);
//         ((ObjectStoreInterMineImpl) os).precompute(q, "temp");

        System.err.println ("finished precompute()");
        Results r = os.execute(q);

// optional:
        r.setBatchSize(5000);

        Results res = os.execute(q);

        TextFileUtil.writeTabDelimitedTable(System.out, res, null, null, -1, null);
    }
}

