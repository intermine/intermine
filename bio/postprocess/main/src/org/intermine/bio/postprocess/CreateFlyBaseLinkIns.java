package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;

/**
 * Create file to send to FlyBase to link in to each FBGN... identifier
 * available in FlyMine.
 *
 * @author Richard Smith
 */
public final class CreateFlyBaseLinkIns
{

//    <dbname>_dbinfo.txt
//    DBNAME  FlyMine
//    BASEURL http://www.flymine.org/query/portal.do
//    HOMEURL http://www.flymine.org
//    DESC    A genetic sequence database.
//    EMAIL   richard@flymine.org

//    <dbname>_linkout.txt //
//    #Flybase ID DBNAME  DBID        DBURL
//    FBgn0259750 GENBANK AAA86639    AAA86639
//    FBgn0005561 GENBANK AAB70249    AAB70249


    /* FlyMine */
//    private static final String DBNAME = "FlyMine";
//    private static final String BASEURL
//        = "http://www.flymine.org/query/portal.do?origin=flybase&externalid=";
//    private static final String DESC = "Integrated database of genomic, expression and protein"
//        + " data for Drosophila.";
//    private static final String EMAIL = "richard@flymine.org";

    /* modMine */
    private static final String DBNAME = "modMine";
    private static final String HOMEURL = "http://intermine.modencode.org";
    private static final String DESC = "Integrated database of genomic, expression and protein data"
        + " for Drosophila, and C. elegans.";
    private static final String EMAIL = "help@modencode.org";

    /* both */
    private static final String BASEURL = HOMEURL + "/query/portal.do?origin=flybase&externalid=";
    private static final String GENUS = "Drosophila";
    private static final String INFO_FILENAME = DBNAME + "_dbinfo.txt";
    private static final String LINKOUT_FILENAME = DBNAME + "_linkout.txt";
    private static final String DATASOURCE = "FlyBase";
    private static final String ENDL = System.getProperty("line.separator");

    private CreateFlyBaseLinkIns() {
        //disable external instantiation
    }

    /**
     * Create link-in file.
     * @param os ObjectStore to find Genes in
     * @throws Exception if anything goes wrong
     */
    public static void createLinkInFile(ObjectStore os) throws Exception {
        createDbInfoFile();

        FileWriter writer = new FileWriter(new File(LINKOUT_FILENAME));
        writeFile(os, writer, "gene");
        writeFile(os, writer, "protein");
        writer.flush();
        writer.close();
    }

    private static void createDbInfoFile()
        throws IOException {
        StringBuffer sb = new StringBuffer("DBNAME\t" + DBNAME + ENDL);
        sb.append("BASEURL\t" + BASEURL + ENDL);
        sb.append("HOMEURL\t" + HOMEURL + ENDL);
        sb.append("DESC\t" + DESC + ENDL);
        sb.append("EMAIL\t" + EMAIL + ENDL);
        FileWriter writer = new FileWriter(INFO_FILENAME);
        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    private static void writeFile(ObjectStore os, Writer writer, String objectType)
        throws IOException {

        writer.write("#Flybase ID" + "\t" + "DBNAME" + "\t" + "DBID" + "\t"
                      + "DBURL" + ENDL);

        Iterator<?> iter = getFlyBaseIds(os, objectType);
        while (iter.hasNext()) {
            String fbgn = (String) iter.next();
            if (fbgn.startsWith("FB") && (fbgn.indexOf("flymine") == -1)) {
                String line = fbgn + "\t"
                    + DBNAME + "\t"
                    + fbgn + "\t"
                    + fbgn + "&class=" + objectType;
                writer.write(line + ENDL);
            }
        }
    }

    private static Iterator<?> getFlyBaseIds(ObjectStore os, String objectType) {
        Query q = new Query();
        q.setDistinct(true);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryClass qcObject = null;
        QueryField qf = null;

        if ("protein".equals(objectType)) {
            qcObject = new QueryClass(Protein.class);
            qf = new QueryField(qcObject, "secondaryIdentifier");
        } else {
            qcObject = new QueryClass(Gene.class);
            qf = new QueryField(qcObject, "primaryIdentifier");
        }
        q.addFrom(qcObject);
        q.addToSelect(qf);

        // gene.primaryIdentifier != NULL
        cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.IS_NOT_NULL));

        QueryClass qcOrg = new QueryClass(Organism.class);
        q.addFrom(qcOrg);

        // gene.organism.genus = 'Drosophila'
        QueryField qfOrgTaxon = new QueryField(qcOrg, "genus");
        cs.addConstraint(new SimpleConstraint(qfOrgTaxon, ConstraintOp.EQUALS,
                                              new QueryValue(GENUS)));

        // gene.organism
        QueryObjectReference ref1 = new QueryObjectReference(qcObject, "organism");
        cs.addConstraint(new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcOrg));

        QueryClass qcDatasource = new QueryClass(DataSource.class);
        q.addFrom(qcDatasource);

        QueryClass qcDataset = new QueryClass(DataSet.class);
        q.addFrom(qcDataset);

        // gene.datasets.datasource.name = flybase
        QueryField datasourceName = new QueryField(qcDatasource, "name");
        cs.addConstraint(new SimpleConstraint(datasourceName, ConstraintOp.EQUALS,
                                              new QueryValue(DATASOURCE)));

        // gene.datasets
        QueryCollectionReference ref2 = new QueryCollectionReference(qcObject, "dataSets");
        cs.addConstraint(new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcDataset));

        // gene.datasets.datasource
        QueryObjectReference ref3 = new QueryObjectReference(qcDataset, "dataSource");
        cs.addConstraint(new ContainsConstraint(ref3, ConstraintOp.CONTAINS, qcDatasource));

        q.setConstraint(cs);
        q.addToOrderBy(qf);
        SingletonResults res = os.executeSingleton(q, 10000, true, true, true);
        return res.iterator();
    }
}
