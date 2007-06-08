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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

import org.flymine.model.genomic.Publication;

import java.io.*;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

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

    private String osAlias = null, outputFile = null;
    private Set seenPubMeds = new HashSet();

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
     * Synchronize publications with pubmed using pmid
     * @throws Exception if an error occurs
     */
    public void execute() throws Exception {
        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        if (osAlias == null) {
            throw new BuildException("osAlias attribute is not set");
        }
        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }

        LOG.info("Starting EntrezPublicationsRetriever");

        Writer writer = null;

        try {
            writer = new FileWriter(outputFile);

            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);

            Set pubMedIds = new HashSet();
            Set toStore = new HashSet();
            ItemFactory itemFactory = new ItemFactory(os.getModel(), "-1_");
            writer.write(FullRenderer.getHeader() + ENDL);
            for (Iterator i = getPublications(os).iterator(); i.hasNext();) {
                pubMedIds.add(((Publication) i.next()).getPubMedId());
                if (pubMedIds.size() == BATCH_SIZE || !i.hasNext()) {
                    BufferedReader br = new BufferedReader(getReader(pubMedIds));
                    StringBuffer buf = new StringBuffer();
                    String line;
                    while ((line = br.readLine()) != null) {
                        buf.append(line + "\n");
                    }
                    try {
                        SAXParser.parse(new InputSource(new StringReader(buf.toString())),
                                        new Handler(toStore, itemFactory));
                    } catch (Throwable e) {
                        throw new RuntimeException("failed to parse: " + buf.toString(), e);
                    }
                    for (Iterator j = toStore.iterator(); j.hasNext();) {
                        writer.write(FullRenderer.render((Item) j.next()));
                    }
                    pubMedIds.clear();
                    toStore.clear();
                }
            }
            writer.write(FullRenderer.getFooter() + ENDL);
            writer.flush();
            writer.close();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Retrieve the publications to be updated
     * @param os The ObjectStore to read from
     * @return a List of publications
     */
    protected List getPublications(ObjectStore os) {
        Query q = new Query();
        QueryClass qc = new QueryClass(Publication.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        return os.executeSingleton(q);
    }

    /**
     * Obtain the pubmed esummary information for the publications
     * @param ids the pubMedIds of the publications
     * @return a Reader for the information
     * @throws Exception if an error occurs
     */
    protected Reader getReader(Set ids) throws Exception {
        String urlString = ESUMMARY_URL + StringUtil.join(ids, ",");
        System.err. println("retrieving: " + urlString);
        return new BufferedReader(new InputStreamReader(new URL(urlString).openStream()));
    }

    /**
     * Extension of DefaultHandler to handle an esummary for a publication
     */
    class Handler extends DefaultHandler
    {
        Set toStore;
        Item publication;
        String name;
        StringBuffer characters;
        ItemFactory itemFactory;
        boolean duplicateEntry = false;

        /**
         * Constructor
         * @param toStore a set in which the new publication items are stored
         * @param itemFactory the factory
         */
        public Handler(Set toStore, ItemFactory itemFactory) {
            this.toStore = toStore;
            this.itemFactory = itemFactory;
        }

        /**
         * @see DefaultHandler#startElement(String, String, String, Attributes)
         */
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
        public void characters(char[] ch, int start, int length) {
            characters.append(new String(ch, start, length));
        }

        /**
         * @see DefaultHandler#endElement(String, String, String)
         */
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
                publication = itemFactory.makeItemForClass(TARGET_NS + "Publication");
                publication.setAttribute("pubMedId", pubMedId);
                toStore.add(publication);
                seenPubMeds.add(pubMedId);
            } else if ("PubDate".equals(name)) {
                String year = characters.toString().split(" ")[0];
                try {
                    Integer.parseInt(year);
                    publication.setAttribute("year", year);
                } catch (NumberFormatException e) {
                    LOG.warn("Publication: " + publication + " has a year that cannot be parsed"
                             + " to an Integer: " + year);
                }
            } else if ("Source".equals(name)) {
                publication.setAttribute("journal", characters.toString());
            } else if ("Title".equals(name)) {
                publication.setAttribute("title", characters.toString());
            } else if ("Volume".equals(name)) {
                publication.setAttribute("volume", characters.toString());
            } else if ("Issue".equals(name)) {
                publication.setAttribute("issue", characters.toString());
            } else if ("Pages".equals(name)) {
                publication.setAttribute("pages", characters.toString());
            } else if ("Author".equals(name)) {
                Item author = itemFactory.makeItemForClass(TARGET_NS + "Author");
                toStore.add(author);
                String authorString = characters.toString();
                author.setAttribute("name", authorString);
                author.addToCollection("publications", publication);
                publication.addToCollection("authors", author);
                if (!publication.hasAttribute("firstAuthor")) {
                    publication.setAttribute("firstAuthor", authorString);
                }
            }
            name = null;
        }
    }
}
