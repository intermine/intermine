package org.flymine.biojava1.app;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import org.flymine.biojava1.utils.TextProgressBar;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.iql.IqlQuery;

/**
 * Internal use only. Precompute tables.
 *
 * @author Markus Brosch
 */
public class PrecomputeTable {

  public static void main(String[] args) throws Exception {
    ObjectStore os = ObjectStoreFactory.getObjectStore();
    Model model = Model.getInstanceByName("genomic");
    String queryString = "SELECT a1_.start AS a2_, a1_.end AS a3_, a1_.strand AS a4_, a6_ "
        + "FROM org.flymine.model.genomic.Location AS a1_, "
        + "org.flymine.model.genomic.Chromosome AS a5_, "
        + "org.flymine.model.genomic.BioEntity AS a6_ "
        + "WHERE (a1_.object CONTAINS a5_ AND a1_.subject CONTAINS a6_)";
    System.err.println("query: " + queryString);
    Query q = new IqlQuery(queryString, model.getPackageName()).toQuery();
    System.err.print("starting precompute(): " + q);
    TextProgressBar p = new TextProgressBar();
    p.start();
    ((ObjectStoreInterMineImpl) os).precompute(q, "temp");
    p.stop();
    System.err.println("finished precompute()");
  }
}
