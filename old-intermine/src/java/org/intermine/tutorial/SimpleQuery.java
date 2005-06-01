package org.intermine.tutorial;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintStream;
import java.util.Iterator;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.model.tutorial.Company;

/**
 * Simple demonstration of query and results handling
 *
 * @author Mark Woodbridge
 */
public class SimpleQuery
{
    /**
     * main method
     * @param args command line parameters
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        SimpleQuery sq = new SimpleQuery();
        PrintStream out = System.out;
        out.println(sq.exampleQuery());
    }

    /**
     * Run an example query
     *
     * @return the ouput from the query as a String
     * @throws Exception if any error occurs
     */
    public String exampleQuery() throws Exception {

        // Get an ObjectStore from the ObjectStoreFactory
        // The alias "os.tutorial" should be set up in the intermine.properties
        // file
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.tutorial");

        // Set up an IQL query. "org.intermine.model.tutorial" is used
        // to qualify unqualified classes in the query
        IqlQuery q = new IqlQuery("select c from Company as c", "org.intermine.model.tutorial");

        // Execute the query (note we have to convert to a Query object first)
        Results results = os.execute(q.toQuery());

        // Set up the String that is going to be returned
        String ret = "";

        // Iterate throught the results of the query
        Iterator rrIter = results.iterator();
        while (rrIter.hasNext()) {
            // Each item in a Results object is a ResultsRow
            ResultsRow rr = (ResultsRow) rrIter.next();

            // First item of a ResultsRow is a Company for this query
            Company c = (Company) rr.get(0);

            // We can get attributes of the Company object (eg. getName()) and traverse to
            // related objects (eg. getDepartments().size())
            ret += c.getName() + " has "
                + c.getDepartments().size() + " departments"
                + System.getProperty("line.separator");
        }
        return ret;
    }
}
