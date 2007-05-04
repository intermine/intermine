package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Iterator;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.objectstore.ObjectStore;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Create file to send to FlyBase to link in to each FBGN... identifier
 * available in FlyMine.
 *
 * @author Richard Smith
 */
public class CreateFlyBaseLinkIns
{
    private static final String DBID = "FlyMine";
    private static final String BURL
        = "http://www.flymine.org/query/portal.do?origin=flybase&class=gene&externalid=";
    private static final String NAM = "FlyMine - integrated genomics and proteomics";
    private static final String ICO = "";
    private static final String LKNA = "FlyMine";
    private static final String LKHE = "<a href=\"http://www.flymine.org\">FlyMine</a>"
        + " - integrated Drosophila and Anopheles genomics and proteomics data.";
    private static final String ENDL = System.getProperty("line.separator");

    /**
     * Create link-in file.
     * @param os ObjectStore to find Genes in
     * @param outputFile file to write to
     * @throws Exception if anything goes wrong
     */
    public static void createLinkInFile(ObjectStore os, File outputFile) throws Exception {
        FileWriter writer = new FileWriter(outputFile);
        writeFile(os, writer);
        writer.flush();
        writer.close();
    }

    private static String createHeader() {
        StringBuffer sb = new StringBuffer();
        sb.append("<OPVR>" + ENDL)
            .append("<DBID>" + DBID + "</DBID>" + ENDL)
            .append("<BURL>" + BURL + "</BURL>" + ENDL)
            .append("<NAM>" + NAM + "</NAM>" + ENDL)
            .append("<ICO>" + ICO + "</ICO>" + ENDL)
            .append("<LKNA>" + LKNA + "</LKNA>" + ENDL)
            .append("<LKHE>" + LKHE + "</LKHE>" + ENDL)
            .append("</OPVR>" + ENDL + ENDL);

        return sb.toString();
    }

    private static void writeFile(ObjectStore os, Writer writer) throws IOException {
        writer.write(createHeader());
        writer.write("#FlybaseID" + "\t" + "DbName" + "\t" + "DbID" + "\t"
                      + "DbUrl (relative to base DBurl)" + ENDL);

        Iterator iter = getFlyBaseIds(os);
        while (iter.hasNext()) {
            String fbgn = (String) iter.next();
            if (fbgn.startsWith("FBgn") && (fbgn.indexOf("flymine") == -1)) {
                writer.write(fbgn + "\t" + DBID + "\t" + fbgn + "\t" + fbgn + ENDL);
            }
        }
    }

    private static Iterator getFlyBaseIds(ObjectStore os) {
        Query q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryField qf = new QueryField(qcGene, "organismDbId");
        q.addToSelect(qf);
        q.addFrom(qcGene);
        QueryClass qcOrg = new QueryClass(Organism.class);
        q.addFrom(qcOrg);
        QueryField qfOrgTaxon = new QueryField(qcOrg, "taxonId");
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        SimpleConstraint sc1 = new SimpleConstraint(qfOrgTaxon, ConstraintOp.EQUALS,
                                                    new QueryValue(new Integer(7227)));
        cs.addConstraint(sc1);
        QueryObjectReference ref1 = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcOrg);
        cs.addConstraint(cc1);
        SimpleConstraint sc2 = new SimpleConstraint(qf, ConstraintOp.IS_NOT_NULL);
        cs.addConstraint(sc2);
        q.setConstraint(cs);
        q.addToOrderBy(qf);
        q.setDistinct(true);
        SingletonResults res = os.executeSingleton(q);
        res.setBatchSize(10000);
        return res.iterator();
    }
}
