package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

import org.flymine.model.genomic.Publication;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;



/**
 * Class to fill in all publication information from pubmed
 * @author Mark Woodbridge
 */
public class EntrezPublicationsRetriever
{
    protected static final Logger LOG = Logger.getLogger(EntrezPublicationsRetriever.class);
    protected static final String ENDL = System.getProperty("line.separator");
    // see http://eutils.ncbi.nlm.nih.gov/entrez/query/static/esummary_help.html for details
    protected static final String ESUMMARY_URL =
        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?tool=flymine&db=pubmed&id=";
    // number of summaries to retrieve per request
    protected static final int BATCH_SIZE = 500;
    // number of times to try the same bacth from the server
    private static final int MAX_TRIES = 5;
    private String osAlias = null, outputFile = null;
    private Set<String> seenPubMeds = new HashSet<String>();
    private Map<String, Item> authorMap = new HashMap<String, Item>();
    private String cacheDirName;
    private ItemFactory itemFactory;

    static final String TARGET_NS = "http://www.flymine.org/model/genomic#";

    /**
     * Set the ObjectStore alias.
     * @param osAlias The ObjectStore alias
     */
    public void setOsAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the output file name
     * @param outputFile The output file name
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Set the cache file name
     * @param cacheDirName The cache file
     */
    public void setCacheDirName(String cacheDirName) {
        this.cacheDirName = cacheDirName;
    }

    /**
     * Synchronize publications with pubmed using pmid
     * @throws Exception if an error occurs
     */
    public void execute() throws Exception {
        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        Database db = null;
        Transaction txn = null;
        try {
            if (osAlias == null) {
                throw new BuildException("osAlias attribute is not set");
            }
            if (outputFile == null) {
                throw new BuildException("outputFile attribute is not set");
            }


            // environment is transactional
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setTransactional(true);
            envConfig.setAllowCreate(true);

            Environment env = new Environment(new File(cacheDirName), envConfig);

            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(true);
            dbConfig.setAllowCreate(true);
            dbConfig.setSortedDuplicates(true);

            db = env.openDatabase(null , "publications_db", dbConfig);

            txn = env.beginTransaction(null, null);
            
            LOG.info("Starting EntrezPublicationsRetriever");

            Writer writer = new FileWriter(outputFile);
            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);

            Set<String> idsToFetch = new HashSet<String>();
            itemFactory = new ItemFactory(os.getModel(), "-1_");
            writer.write(FullRenderer.getHeader() + ENDL);
            for (Iterator<Publication> iter = getPublications(os).iterator(); iter.hasNext();) {
                String pubMedId = iter.next().getPubMedId();
                
                if (seenPubMeds.contains(pubMedId)) {
                    continue;
                }
                
                DatabaseEntry key = new DatabaseEntry(pubMedId.getBytes());
                DatabaseEntry data = new DatabaseEntry();
                if (db.get(txn, key, data, null).equals(OperationStatus.SUCCESS)) {
                    try {
                        ByteArrayInputStream mapInputStream =
                            new ByteArrayInputStream(data.getData());
                        ObjectInputStream deserializer = new ObjectInputStream(mapInputStream);
                        Map<String, Object> pubMap = (Map) deserializer.readObject();
                        writeItems(writer, mapToItems(itemFactory, pubMap));
                        // System.err. println("found in cache: " + pubMedId);
                        seenPubMeds.add(pubMedId);
                    } catch (EOFException e) {
                        // ignore and fetch it again
                        System.err .println("found in cache, but igored due to cache problem: " 
                                            + pubMedId);
                    }
                } else {
                    idsToFetch.add(pubMedId);
                }
            }

            Iterator<String> idIter = idsToFetch.iterator();
            Set<String> thisBatch = new HashSet<String>();
            while (idIter.hasNext()) {
                String pubMedId = idIter.next();
                thisBatch.add(pubMedId);
                if (thisBatch.size() == BATCH_SIZE || !idIter.hasNext() && thisBatch.size() > 0) {
                    try {
                        // the server may return less publications than we ask for, so keep a Map
                        Map<String, Map<String, Object>> fromServerMap = null;
                        
                        for (int i = 0; i < MAX_TRIES; i++) {
                            BufferedReader br = new BufferedReader(getReader(thisBatch));
                            StringBuffer buf = new StringBuffer();
                            String line;
                            while ((line = br.readLine()) != null) {
                                buf.append(line + "\n");
                            }
                            fromServerMap = new HashMap<String, Map<String, Object>>();
                            Throwable throwable = null;
                            try {
                                SAXParser.parse(new InputSource(new StringReader(buf.toString())),
                                                new Handler(fromServerMap));
                            } catch (Throwable e) {
                                // try again or re-throw the Throwable
                                throwable = e;
                            }
                            if (i == MAX_TRIES) {
                                throw new RuntimeException("failed to parse: " + buf.toString()
                                                           + " - tried " + MAX_TRIES + " times",
                                                           throwable);
                            } else {
                                if (throwable != null) {
                                    // try again
                                    continue;
                                }
                            }

                            for (String id: fromServerMap.keySet()) {
                                writeItems(writer, mapToItems(itemFactory, fromServerMap.get(id)));
                            }
                            addToDb(txn, db, fromServerMap);
                            break;
                        }
                        thisBatch.clear();
                    } finally {
                        txn.commit();
                        // start a new transaction incase there is an exception while parsing
                        txn = env.beginTransaction(null, null);
                    }
                }
            }
            writeItems(writer, authorMap.values());
            writer.write(FullRenderer.getFooter() + ENDL);
            writer.flush();
            writer.close();
        } catch (Throwable e) {
            throw new RuntimeException("failed to get all publications", e);
        } finally {
            txn.commit();
            db.close();
            Thread.currentThread().setContextClassLoader(cl);
        }

    }

    private void writeItems(Writer writer, Collection<Item> items) throws IOException {
        for (Item item: items) {
            writer.write(FullRenderer.render(item));
        }
    }

    /**
     * Add a Map of pubication information to the Database 
     */
    private void addToDb(Transaction txn, Database db,
                         Map<String, Map<String, Object>> fromServerMap) 
        throws IOException, DatabaseException {
        for (Map.Entry<String, Map<String, Object>> entry: fromServerMap.entrySet()) {
            String pubMedId = entry.getKey();
            // System.err .println("adding to cache: " + pubMedId);
            DatabaseEntry key = new DatabaseEntry(pubMedId.getBytes());
            Map dataMap = entry.getValue();
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream serializer = new ObjectOutputStream(arrayOutputStream);
            serializer.writeObject(dataMap);
            DatabaseEntry data = new DatabaseEntry(arrayOutputStream.toByteArray());
            
            db.put(txn, key, data);
        }
    }

    /**
     * Retrieve the publications to be updated
     * @param os The ObjectStore to read from
     * @return a List of publications
     */
    protected List<Publication> getPublications(ObjectStore os) {
        Query q = new Query();
        QueryClass qc = new QueryClass(Publication.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        return (List<Publication>) os.executeSingleton(q);
    }

    /**
     * Obtain the pubmed esummary information for the publications
     * @param ids the pubMedIds of the publications
     * @return a Reader for the information
     * @throws Exception if an error occurs
     */
    protected Reader getReader(Set<String> ids) throws Exception {
        String urlString = ESUMMARY_URL + StringUtil.join(ids, ",");
        System.err .println("retrieving: " + urlString);
        return new BufferedReader(new InputStreamReader(new URL(urlString).openStream()));
    }

    private Set<Item> mapToItems(ItemFactory itemFactory, Map map) {
        Set<Item> retSet = new HashSet<Item>();
        Item publication = itemFactory.makeItemForClass(TARGET_NS + "Publication");
        retSet.add(publication);
        publication.setAttribute("pubMedId", (String) map.get("id"));
        publication.setAttribute("journal", (String) map.get("journal"));
        publication.setAttribute("title", (String) map.get("title"));
        publication.setAttribute("volume", (String) map.get("volume"));
        publication.setAttribute("issue", (String) map.get("issue"));
        publication.setAttribute("pages", (String) map.get("pages"));
        if (map.get("year") != null) {
            publication.setAttribute("year", (String) map.get("year"));
        }

        List<String> authors = (List<String>) map.get("authors");
        if (authors != null) {
            for (String authorString : authors) {
                Item author = authorMap.get(authorString);
                if (author == null) {
                    author = itemFactory.makeItemForClass(TARGET_NS + "Author");
                    author.setAttribute("name", authorString);
                    authorMap.put(authorString, author);
                }
                author.addToCollection("publications", publication);
                publication.addToCollection("authors", author);
                if (!publication.hasAttribute("firstAuthor")) {
                    publication.setAttribute("firstAuthor", authorString);
                }
            }
        }
        return retSet;
    }

    /**
     * Extension of DefaultHandler to handle an esummary for a publication
     */
    class Handler extends DefaultHandler
    {
        Map<String, Object> pubMap;
        String name;
        StringBuffer characters;
        boolean duplicateEntry = false;
        Map<String, Map<String, Object>> cache;

        /**
         * Constructor
         * @param itemFactory the factory
         */
        public Handler(Map<String, Map<String, Object>> fromServerMap) {
            this.cache = fromServerMap;
        }

        /**
         * @see DefaultHandler#startElement(String, String, String, Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if ("ERROR".equals(qName)) {
                name = qName;
            } else if ("Id".equals(qName)) {
                name = "Id";
            } else if ("DocSum".equals(qName)) {
                duplicateEntry = false;
            } else {
                name = attrs.getValue("Name");
            }
            characters = new StringBuffer();
        }

        /**
         * @see DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            characters.append(new String(ch, start, length));
        }

        /**
         * @see DefaultHandler#endElement(String, String, String)
         */
        @Override
        public void endElement(String uri, String localName, String qName) {
            // do nothing if we have seen this pubmed id before
            if (duplicateEntry) {
                return;
            }
            if ("ERROR".equals(name)) {
                LOG.error("Unable to retrieve pubmed record: " + characters);
            } else if ("Id".equals(name)) {
                String pubMedId = characters.toString();
                if (seenPubMeds.contains(pubMedId)) {
                    duplicateEntry = true;
                    return;
                }
                pubMap = new HashMap<String, Object>();
                pubMap.put("id", pubMedId);
                seenPubMeds.add(pubMedId);
                cache.put(pubMedId, pubMap);
            } else if ("PubDate".equals(name)) {
                String year = characters.toString().split(" ")[0];
                try {
                    Integer.parseInt(year);
                    pubMap.put("year", year);
                } catch (NumberFormatException e) {
                    LOG.warn("Cannot parse year from publication: " + year);
                }
            } else if ("Source".equals(name)) {
                pubMap.put("journal", characters.toString());
            } else if ("Title".equals(name)) {
                pubMap.put("title", characters.toString());
            } else if ("Volume".equals(name)) {
                pubMap.put("volume", characters.toString());
            } else if ("Issue".equals(name)) {
                pubMap.put("issue", characters.toString());
            } else if ("Pages".equals(name)) {
                pubMap.put("pages", characters.toString());
            } else if ("Author".equals(name)) {
                String authorString = characters.toString();
                List<String> authorList = (List<String>) pubMap.get("authors");
                if (authorList == null) {
                    authorList = new ArrayList<String>();
                    pubMap.put("authors", authorList);
                }
                authorList.add(authorString);
            }
            name = null;
        }
    }
}
