package org.flymine.util;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Iterator;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;

/**
 * Common operations for post processing.
 *
 * @author Richard Smith
 */
public class CreateFlyBaseLinkIns
{
    private static final String DBID = "FlyMine";
    private static final String BURL = "http://www.flymine.org/query/portal.do?origin=flybase&class=Gene&externalid=";
    private static final String NAM = "FlyMine - integrated genomics and proteomics";
    private static final String ICO = "";
    private static final String LKNA = "FlyMine";
    private static final String LKHE = "<a href=\"http://www.flymine.org/query\">FlyMine</a>"
        + " - integrated Drosophila and Anopheles genomics and proteomics data.";
    private static final String ENDL = System.getProperty("line.separator");

    private ObjectStore os;

    public CreateFlyBaseLinkIns(String alias) throws Exception {
        this.os = ObjectStoreFactory.getObjectStore(alias);
    }

    private String createHeader() {
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

    private void writeFile(Writer writer) throws ObjectStoreException, IOException {
        writer.write(createHeader());
        writer.write("#FlybaseID" + "\t" + "DbName" + "\t" + "DbID" + "\t"
                      + "DbUrl (relative to base DBurl)" + ENDL);

        Iterator iter = getFlyBaseIds();
        while (iter.hasNext()) {
            String fbgn = ((Gene) iter.next()).getOrganismDbId();
            if (fbgn.startsWith("FBgn") && (fbgn.indexOf("flymine") == -1)) {
                writer.write(fbgn + "\t" + DBID + "\t" + fbgn + "\t" + fbgn + ENDL);
                System.out.println("written something");
            }
        }
    }


    public Iterator getFlyBaseIds() throws ObjectStoreException {
        Query q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryField qf = new QueryField(qcGene, "organismDbId");
        q.addToSelect(qcGene);
        q.addFrom(qcGene);
        QueryClass qcOrg = new QueryClass(Organism.class);
        q.addFrom(qcOrg);
        QueryObjectReference ref1 = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcOrg);
        q.setConstraint(cc1);
        q.addToOrderBy(qf);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(10000);
        return res.iterator();
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: CreateFlyBaseLinkIns objectstore_alias filename");
        }

        String alias = args[0];
        String filename = args[1];

        CreateFlyBaseLinkIns m = new CreateFlyBaseLinkIns(alias);
        FileWriter writer = new FileWriter(filename);
        m.writeFile(writer);
        writer.flush();
        writer.close();
    }
}
