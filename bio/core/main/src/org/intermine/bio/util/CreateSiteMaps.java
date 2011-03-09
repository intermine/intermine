package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;

/**
 * Generate sitemaps for the webapp.
 *
 * Runs query using paths to return list of objects, adds these objects to sitemap xml file.
 * Only 50,000 objects per sitemap, so several are created.
 *
 * TODO: add organism to query, add these sitemaps to sitemap_index.xml
 *
 * The search engines know about these maps because they are listed in sitemap_index.xml
 * which is generated with the website.  We also submit them manually via google and yahoo's
 * websites.
 * @author Julie Sullivan
 */
public final class CreateSiteMaps
{
    protected static final Logger LOG = Logger.getLogger(CreateSiteMaps.class);
    private static final String LOC = "portal.do?externalid=";
    private static final String SETOPEN =
        "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String SETCLOSE = "</urlset>";
    private static final String ENDL = System.getProperty("line.separator");
    private static final String EXT = ".xml";
    private static String date, prefix;
    private static final String WEBPAGEWEIGHT = "0.7";
    private static Random generator = null;

    private CreateSiteMaps() {
        //disable external instantiation
    }

    /**
     * Create sitemaps
     * NOTE: Sitemaps can't contain more than 50000 entries or be over 10MB.  See sitemap.org.
     * @param os ObjectStore to find object in
     * @param outputFile file to write to
     * @param taxonIds organisms to use as a constraint in the query
     * @param paths paths used to build query
     * @param webappPages URLs to add to the sitemap
     * @param targetModel eg org.intermine.model.bio
     * @param sitePrefix URL of site, eg http://www.flymine.org
     * @param defaultContext default path to webapp, eg. /query
     * @throws Exception if anything goes wrong
     */
    public static void createSiteMap(ObjectStore os, String outputFile, String taxonIds,
                                     String paths, String webappPages, String targetModel,
                                     String sitePrefix, String defaultContext) throws Exception {

        if ("".equals(sitePrefix) || "".equals(paths)) {
            LOG.warn("Sitemaps not created.  project.sitePrefix or sitemap.paths properties "
                      + "have not been set ");
            return;
        }

        generator = new Random();

        // date for sitemap files
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        date = formatter.format(new Date());

        // URL
        prefix = sitePrefix + "/" + defaultContext + "/";

        // write webapp pages to be indexed to sitemap.xml
        // these are JSPs with content
        String newFileName = outputFile + EXT;
        FileWriter writer = startFile(new File(newFileName));
        writeStartPages(writer, webappPages);
        closeFile(writer);
        LOG.debug("newFileName-" + newFileName);

        // generate a query for each path
        for (String path : paths.split("[, ]+")) {
            int i = 0;
            int sitemapIndex = 0;

            // eg, Gene.primaryIdentifier
            String[] pathSplit = path.split("\\.");
            if (pathSplit.length != 2) {
                // This LOG.error could possibly be a LOG.warn, but the message is important.
                LOG.error(" invalid path:" + path + ", invalid length:" + pathSplit.length);
                break;
            }
            String className = pathSplit[0];
            String identifierName = pathSplit[1];

            // Only create new files if they don't already exist, we have no input file
            // to compare dates to so must rely on output directory being cleaned to
            // force creation of new sitemaps.
            File newFile = new File(outputFile + className + EXT);
            if (!newFile.exists()) {
                Iterator<?> it = getResults(os, makeQuery(targetModel, className, identifierName));
                if (it.hasNext()) {  // don't create empty files
                    writer = startFile(newFile);
                    while (it.hasNext()) {
                        ResultsRow<?> r =  (ResultsRow<?>) it.next();
                        String identifier = (String) r.get(0);
                        String priority = getPriority();
                        i++;
                        // sitemaps can only have 50000 entries
                        if (i == 50000) {
                            // close sitemap file that's full
                            closeFile(writer);
                            // new file with number appended, eg sitemapGene_1.xml
                            newFile = new File(outputFile + className + "_" + sitemapIndex + EXT);
                            // TODO put this new filename somewhere for sitemap_index.xml
                            // start file
                            writer = startFile(newFile);
                            // update counters
                            i = 0;
                            sitemapIndex++;
                        }
                        writer.write(getURL(prefix + LOC + identifier + "&amp;class="
                                            + className, priority));
                    }
                    closeFile(writer);
                }
            } else {
                // quit if any sitemap is present
                LOG.info("sitemap present: " + newFile.getName());
                return;
            }
        }
    }

    // TODO add organisms to this query
    private static String makeQuery(String targetModel, String className, String identifierName) {
        String q = "SELECT DISTINCT a1_." + identifierName + " AS a2_ "
            + " FROM " + targetModel + "." + className + " AS a1_ "
            + " WHERE a1_." + identifierName + " != \'\'";
        //LOG.debug(q);
        return q;
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

    private static Iterator<?> getResults(ObjectStore os, String query) {
        IqlQuery q = new IqlQuery(query, os.getModel().getPackageName());
        Results r = os.execute(q.toQuery(), 100000, true, true, true);
        Iterator<?> i = r.iterator();
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

    private static void writeStartPages(FileWriter writer, String webappPages) throws Exception {
        String[] pages = webappPages.split("[, ]+");
        for (String s : pages) {
            writer.write(getURL(prefix + s, WEBPAGEWEIGHT));
        }
    }

    // generate random value between 0.1 and 1.0 inclusive
    // google complains if priority is set to all the same value.  i don't think it's used for
    // anything really anyway
    private static String getPriority() {
        BigDecimal bd = new BigDecimal(generator.nextDouble());
        bd = bd.setScale(1, BigDecimal.ROUND_UP);
        return bd.toPlainString();
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
