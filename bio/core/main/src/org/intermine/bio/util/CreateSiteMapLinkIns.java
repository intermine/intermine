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


import java.util.Date;
import java.util.Iterator;

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.objectstore.ObjectStore;

import java.io.FileWriter;
import java.text.Format;
import java.text.SimpleDateFormat;

/**
 * Create file to make site map 
 *
 * @author Julie Sullivan
 */
public class CreateSiteMapLinkIns
{
    private static final int MAX = 50000;  // can't have more than 50000 links per
    private static int index = 0;          // what number gene/protein we're on
    private static int fileIndex = 0;      // what number sitemap we're on
    // TODO get this from config file
    private static final String LOC = "http://www.flymine.org/query/portal.do?externalid=";
    private static final String SETOPEN = 
        "< urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String SETCLOSE = "</urlset>";
    private static final String ENDL = System.getProperty("line.separator");
    private static final String EXT = ".xml";
    private static String date;
    private static final String PREFIX = "http://www.flymine.org/query/";
    private static final String WEIGHT = "0.5";
    private static final String STARTWEIGHT = "0.8";
    
    /**
     * Create sitemap
     * @param os ObjectStore to find Genes in
     * @param outputFile file to write to
     * @throws Exception if anything goes wrong
     */
    public static void createSiteMap(ObjectStore os, String outputFile) throws Exception {

        String newFileName = outputFile + fileIndex + EXT;
        FileWriter writer = startFile(newFileName);
        
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        date = formatter.format(new Date());

        writeStartPages(writer);
        
        String[] queries = {"gene", "protein"};
        
        for (String s : queries) {        
            Iterator i = getResults(os, s);
            while (i.hasNext()) {
                ResultsRow r =  (ResultsRow) i.next();
                String identifier = (String) r.get(0);            
                writer.write(getURL(LOC + identifier, WEIGHT));            
                index++;
                if (index > MAX) {
                    writer = getNewFile(writer, outputFile + ++fileIndex + EXT);                
                }
            }
        }
        closeFile(writer);
    }

    private static FileWriter getNewFile(FileWriter writer, String newFilename) throws Exception {
        closeFile(writer);
        FileWriter w = startFile(newFilename);
        index = 0;
        return w;
    }

    
    private static FileWriter startFile(String newFileName) throws Exception  {
        FileWriter writer = new FileWriter(newFileName);
        writer.write(getHeader());
        return writer;
    }
    
    private static void closeFile(FileWriter writer) throws Exception {
        writer.write(getFooter());
        writer.flush();
        writer.close();
    }
    
    private static Iterator getResults(ObjectStore os, String whichQuery) {
        String query = null;
        if (whichQuery.equals("gene")) {
            query = "SELECT DISTINCT a1_.identifier as a3_ "
                + "FROM org.flymine.model.genomic.Gene AS a1_, "
                + "org.flymine.model.genomic.Organism AS a2_ WHERE " 
                + "(a2_.taxonId = 180454 "
                + "OR a2_.taxonId = 7227 "
                + "OR a2_.taxonId = 7237) " 
                + "AND a1_.organism CONTAINS a2_ " 
                + "AND a1_.identifier != \'\') " 
                + "ORDER BY a1_.identifier\n";
       } else {
            query  = "SELECT DISTINCT a1_.identifier as a3_ "
                + "FROM org.flymine.model.genomic.Protein AS a1_, "
                + "org.flymine.model.genomic.Organism AS a2_ WHERE " 
                + "(a2_.taxonId = 180454 "
                + "OR a2_.taxonId = 7227 "
                + "OR a2_.taxonId = 7237) " 
                + "AND a1_.organism CONTAINS a2_ " 
                + "AND a1_.identifier != \'\') " 
                + "ORDER BY a1_.identifier\n";
        }
        IqlQuery q = new IqlQuery(query, os.getModel().getPackageName());
        Results r = os.execute(q.toQuery());
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
        String[] pages = {"begin.do", "templates.do", "bag.do", "dataCategories.do"};
        for (String s : pages) {     
            writer.write(getURL(PREFIX + s, STARTWEIGHT));            
            index++;
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
