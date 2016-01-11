package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;


/**
 * Ant task to read an IQL query from a -Dquery="QUERY" parameter and execute it in the database.
 * @author Richard Smith
 *
 */
public class RunIqlQueryTask extends Task
{
    protected static final int MAX_ROWS = 10;


    protected String alias;
    protected String query = null;

    /**
     * Set the ObjectStore alias.
     *
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * An query string to execute, may be IQL or SQL by format parameter must be set correctly,
     *richard
     * @param query the query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        if (query == null) {
            throw new BuildException("You need to provide an IQL query string");
        }

        ObjectStoreInterMineImpl os;

        try {
            os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        runIqlQuery(os, query);
    }

    /**
     * Run an IQL query in the objectstore and print the first results to the console.
     * @param os the objectstore
     * @param iql and IQL query string
     */
    private static void runIqlQuery(ObjectStoreInterMineImpl os, String iql) {
        System.out .println("Running query in database " + os.getDatabase().getName()
                + " - " + iql);
        System.out .println("See intermine.log for query execution details.");
        Query q = parseIqlQuery(os.getModel(), iql);
        Results results = os.execute(q);
        Iterator<?> resIter = results.iterator();
        int count = 0;
        while (resIter.hasNext() && count < MAX_ROWS) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            System.out .println(count + " - " + rr);
            count += 1;
        }
        if (resIter.hasNext()) {
            System.out .println("Only fetching first " + MAX_ROWS + " rows.");
        }
    }


    /**
     * For a given IQL query, return a Query object.
     * @param iqlQueryString the IQL String
     * @param model the Model
     * @return a Query object
     * @throws BuildException if the IQL String cannot be parsed.
     */
    private static Query parseIqlQuery(Model model, String iqlQueryString) {
        IqlQuery iqlQuery = new IqlQuery(iqlQueryString, model.getPackageName());

        try {
            return iqlQuery.toQuery();
        } catch (IllegalArgumentException e) {
            throw new BuildException("Exception while parsing query: "
                                     + " = " + iqlQueryString, e);
        }
    }
}
