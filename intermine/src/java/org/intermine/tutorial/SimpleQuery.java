package org.flymine.tutorial;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;

import org.flymine.model.tutorial.Company;

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

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.tutorial");

        FqlQuery q = new FqlQuery("select c from Company as c", "org.flymine.model.tutorial");

        Results r = os.execute(q.toQuery());
        Iterator rrIter = r.iterator();
        while (rrIter.hasNext()) {
            ResultsRow rr = (ResultsRow) rrIter.next();
            Company c = (Company) rr.get(0);
            System
                .out.println(c.getName() + " has " + c.getDepartments().size() + " departments");
        }
    }
}
