package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.util.SAXParser;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;

import org.intermine.model.InterMineObject;
import org.flymine.model.genomic.Publication;
import org.flymine.model.genomic.Author;

import org.apache.log4j.Logger;

/**
 * Class to fill in all publication information from pubmed
 * @author Mark Woodbridge
 */
public class UpdatePublications
{
    protected static final Logger LOG = Logger.getLogger(UpdatePublications.class);
    // see http://eutils.ncbi.nlm.nih.gov/entrez/query/static/esummary_help.html for details
    protected static final String ESUMMARY_URL =
        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=";
    // number of summaries to retrieve per request
    protected static final int BATCH_SIZE = 50;
    protected ObjectStoreWriter osw;

    /**
     * Constructor
     * @param osw the relevent ObjectStoreWriter
     */
    public UpdatePublications(ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Synchronize publications with pubmed using pmid
     * @throws Exception if an error occurs
     */
    public void execute() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Publication.class);
        q.addFrom(qc);
        q.addToSelect(qc);

        Map publications = new HashMap();
        for (Iterator i = ((List) osw.getObjectStore().execute(q)).iterator(); i.hasNext();) {
            Publication publication = (Publication) ((List) i.next()).get(0);
            publications.put(publication.getPubMedId(), publication);
            if (publications.size() == BATCH_SIZE || !i.hasNext()) {
                String ids = StringUtil.join(publications.keySet(), ",");
                BufferedReader reader = 
                    new BufferedReader(new InputStreamReader(new URL(ESUMMARY_URL
                                                                     + ids).openStream()));
                SAXParser.parse(new InputSource(reader), new Handler(publications));
                storePublications(publications.values());
                publications = new HashMap();
            }
        }
    }

    /**
     * Store a collection of publications
     * @param publications the publications
     * @throws ObjectStoreException if an error occurs
     */
    protected void storePublications(Collection publications) throws ObjectStoreException {
        for (Iterator i = publications.iterator(); i.hasNext();) {
            Publication publication = (Publication) i.next();
            osw.store(publication);
            for (Iterator j = publication.getAuthors().iterator(); j.hasNext();) {
                osw.store((InterMineObject) j.next());
            }
        }
    }

    /**
     * Extension of DefaultHandler to handle an esummary for a publication
     */
    class Handler extends DefaultHandler
    {
        Map publications;
        Publication publication;
        String name;
        StringBuffer characters;

        /**
         * Constructor
         * @param publications Map from pubMedId to publication that needs to be updated
         */
        public Handler(Map publications) {
            this.publications = publications;
        }
         
        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if ("ERROR".equals(qName)) {
                throw new SAXException("esummary returned an error message - is the pmid valid?");
            } else if ("Id".equals(qName)) {
                name = "Id";
            } else {
                name = attrs.getValue("Name");
            }
            characters = new StringBuffer();
        }

        /**
         * @see DefaultHandler#characters
         */
        public void characters(char[] ch, int start, int length) throws SAXException {
            characters.append(new String(ch, start, length));
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("Id".equals(name)) {
                publication = (Publication) publications.get(characters.toString());
                publication.setAuthors(new ArrayList());
            } else if ("PubDate".equals(name)) {
                publication.setYear(new Integer(characters.toString().split(" ")[0]));
            } else if ("Source".equals(name)) {
                publication.setJournal(characters.toString());
            } else if ("Title".equals(name)) {
                publication.setTitle(characters.toString());
            } else if ("Volume".equals(name)) {
                publication.setVolume(characters.toString());
            } else if ("Issue".equals(name)) {
                publication.setIssue(characters.toString());
            } else if ("Pages".equals(name)) {
                publication.setPages(characters.toString());
            } else if ("Author".equals(name)) {
                Author author = (Author)
                    DynamicUtil.createObject(Collections.singleton(Author.class));
                author.setName(characters.toString());
                author.getPublications().add(publication);
                publication.getAuthors().add(author);
            }
            name = null;
        }
    }
    
    /**
     * Main method
     * @param args the arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.production");
        List ids = new ArrayList();
        ids.add("10021333");
        for (Iterator i = ids.iterator(); i.hasNext();) {
            Publication publication = (Publication)
                DynamicUtil.createObject(Collections.singleton(Publication.class));
            publication.setPubMedId((String) i.next());
            osw.store(publication);
        }
        new UpdatePublications(osw).execute();
    }
}
