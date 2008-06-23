package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.File;
import java.io.FileWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;

/**
 * Generate sitemaps for the webapp.  Creates seven maps.  One contains each page of the webapp.
 * The remaining six contain links to the object pages for three organisms' proteins and genes.
 * We chop them up this way because sitemaps can't contain more than 50,000 entries.  The search
 * engines know about these maps because they are listed in sitemap_index.xml which is generated
 * with the website.  We also submit them manually via google and yahoo's websites.
 * @author Julie Sullivan
 */
public class CreateSiteMapLinkIns
{
    // TODO get this from config file
    private static final String LOC = "http://www.flymine.org/query/portal.do?externalid=";
    private static final String SETOPEN =
        "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String SETCLOSE = "</urlset>";
    private static final String ENDL = System.getProperty("line.separator");
    private static final String EXT = ".xml";
    private static String date;
    private static final String PREFIX = "http://www.flymine.org/query/";
    private static final String OBJECTDETAILSWEIGHT = "0.8";
    private static final String WEBPAGEWEIGHT = "0.5";

    /**
     * Create sitemaps
     * NOTE: Sitemaps can't contain more than 50000 entries or be over 10MB.  See sitemap.org.
     * @param os ObjectStore to find object in
     * @param outputFile file to write to
     * @throws Exception if anything goes wrong
     */
    public static void createSiteMap(ObjectStore os, String outputFile) throws Exception {

        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        date = formatter.format(new Date());

        String newFileName = outputFile + EXT;
        FileWriter writer = startFile(new File(newFileName));

        writeStartPages(writer);
        closeFile(writer);

        // TODO put classes, taxonIds and priorities in properties file
        String[] bioentity = {"gene", "protein"};
        String[] ids = {"180454", "6239", "7227", "7237", "7217", "7220",
                        "7222", "7230", "7234", "7238", "7240", "7244", "7260",
                        "7245"};

        // Only create new files if they don't already exist, we have no input file
        // to compare dates to so must rely on output directory being cleaned to
        // force creation of new sitemaps.
        for (String o : bioentity) {
            for (String id : ids) {
                File newFile = new File(outputFile + id + o + EXT);
                if (!newFile.exists()) {
                    Iterator i = getResults(os, o, id);
                    if (i.hasNext()) {  // don't create empty files
                        writer = startFile(newFile);
                        while (i.hasNext()) {
                            ResultsRow r =  (ResultsRow) i.next();
                            String identifier = (String) r.get(0);
                            String priority = OBJECTDETAILSWEIGHT;
                            if (id.equals("180454")
                                            || id.equals("6239")
                                            || id.equals("7227")
                                            || id.equals("7237")) {
                                priority = "1.0";
                            }
                            writer.write(getURL(LOC + identifier + "&amp;class=" + o, priority));
                        }
                        closeFile(writer);
                    }
                }
            }
        }
    }


    private static FileWriter startFile(File f) throws Exception  {
        FileWriter writer = new FileWriter(f);
        writer.write(getHeader());
        return writer;
    }

    private static void closeFile(FileWriter writer) throws Exception {
        writer.write(getFooter());
        writer.flush();
        writer.close();
    }

    private static Iterator getResults(ObjectStore os, String whichQuery, String taxonId) {
        String query = null;
        if (whichQuery.equals("gene")) {
            query = "SELECT DISTINCT a1_.primaryIdentifier as a3_ "
                + "FROM org.flymine.model.genomic.Gene AS a1_, "
                + "org.flymine.model.genomic.Organism AS a2_ WHERE "
                + "a2_.taxonId = " + taxonId + " "
                + "AND a1_.organism CONTAINS a2_ "
                + "AND a1_.primaryIdentifier != \'\') "
                + "ORDER BY a1_.primaryIdentifier\n";
       } else {
            query  = "SELECT DISTINCT a1_.primaryIdentifier as a3_ "
                + "FROM org.flymine.model.genomic.Protein AS a1_, "
                + "org.flymine.model.genomic.Organism AS a2_ WHERE "
                + "a2_.taxonId = " + taxonId + " "
                + "AND a1_.organism CONTAINS a2_ "
                + "AND a1_.primaryIdentifier != \'\') "
                + "ORDER BY a1_.primaryIdentifier\n";
        }
        IqlQuery q = new IqlQuery(query, os.getModel().getPackageName());
        Results r = os.execute(q.toQuery());
        r.setBatchSize(100000);
        Iterator i = r.iterator();
        return i;
    }


    private static String getHeader() {
       return HEAD + ENDL + SETOPEN + ENDL;
    }

    private static String getFooter() {
        return SETCLOSE + ENDL;
     }

    private static String getURL(String identifier, String weight) {
        StringBuffer s = new StringBuffer("<url>" + ENDL);
        s.append("<loc>");
        s.append(identifier);
        s.append("</loc>" + ENDL);
        s.append("<lastmod>");
        s.append(date);
        s.append("</lastmod>" + ENDL);
        s.append("<priority>");
        s.append(weight);
        s.append("</priority>" + ENDL);
        s.append("</url>" + ENDL);
        return s.toString();
    }

    private static void writeStartPages(FileWriter writer) throws Exception {
        // TODO get these from config file
        String[] pages = {"begin.do", "templates.do", "bag.do?subtab=view", "dataCategories.do"};
        for (String s : pages) {
            writer.write(getURL(PREFIX + s, WEBPAGEWEIGHT));
        }
    }

    /*
    <?xml version="1.0" encoding="UTF-8"?>
    < urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
     < url>
      < loc>http://www.example.com/</loc>
      < lastmod>2005-01-01</lastmod>
      < changefreq>monthly</changefreq>
      < priority>0.8</priority>
     </url>
    </urlset>
*/

}
