package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Writer;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.SAXParser;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.Attribute;

import org.flymine.model.genomic.Publication;

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
        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?tool=flymine&db=pubmed&id=";
    // number of summaries to retrieve per request
    protected static final int BATCH_SIZE = 50;
    protected ObjectStore os;
    protected Writer writer;

    /**
     * Constructor
     * @param os ObjectStore from which to retrieve skeleton publications
     * @param writer the writer to which the publication items are written as XML
     */
    public UpdatePublications(ObjectStore os, Writer writer) {
        this.os = os;
        this.writer = writer;
    }

    /**
     * Synchronize publications with pubmed using pmid
     * @throws Exception if an error occurs
     */
    public void execute() throws Exception {
        Set pubMedIds = new HashSet();
        Set toStore = new HashSet();
        for (Iterator i = getPublications().iterator(); i.hasNext();) {
            pubMedIds.add(((Publication) i.next()).getPubMedId());
            if (pubMedIds.size() == BATCH_SIZE || !i.hasNext()) {
                SAXParser.parse(new InputSource(getReader(pubMedIds)),
                                new Handler(toStore));
                writer.write(FullRenderer.render(toStore));
                pubMedIds.clear();
                toStore.clear();
            }
        }
    }

    /**
     * Retrieve the publications to be updated
     * @return a List of publications
     * @throws ObjectStoreException if an error occurs
     */
    protected List getPublications() throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Publication.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        return new SingletonResults(q, os, os.getSequence());
    }

    /**
     * Obtain the pubmed esummary information for the publications
     * @param ids the pubMedIds of the publications
     * @return a Reader for the information
     * @throws Exception if an error occurs
     */
    protected Reader getReader(Set ids) throws Exception {
        return new BufferedReader(new InputStreamReader(new URL(ESUMMARY_URL + StringUtil
                                                                .join(ids, ",")).openStream()));
    }

    /**
     * Extension of DefaultHandler to handle an esummary for a publication
     */
    class Handler extends DefaultHandler
    {
        Set toStore;
        MyItem publication;
        String name;
        StringBuffer characters;

        /**
         * Constructor
         * @param toStore a set in which the new publication items are stored
         */
        public Handler(Set toStore) {
            this.toStore = toStore;
        }
         
        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if ("ERROR".equals(qName)) {
                name = qName;
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
            if ("ERROR".equals(name)) {
                LOG.error("Unable to retrieve pubmed record: " + characters);
            } else if ("Id".equals(name)) {
                publication = new MyItem("Publication");
                toStore.add(publication);
                publication.setAttribute("pubMedId", characters.toString());
            } else if ("PubDate".equals(name)) {
                publication.setAttribute("year", characters.toString().split(" ")[0]);
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
                MyItem author = new MyItem("Author");
                toStore.add(author);
                author.setAttribute("name", characters.toString());
                author.addReference("publications", publication);
                publication.addReference("authors", author);
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
        ids.add("10021351");
        for (Iterator i = ids.iterator(); i.hasNext();) {
            Publication publication = (Publication)
                DynamicUtil.createObject(Collections.singleton(Publication.class));
            publication.setPubMedId((String) i.next());
            osw.store(publication);
        }
        osw.close();
        Writer writer = new BufferedWriter(new FileWriter("items.xml"));
        new UpdatePublications(ObjectStoreFactory.getObjectStore("os.production"), writer)
            .execute();
        writer.close();
    }
}

/**
 * Convenience subclass of Item
 * @author Mark Woodbridge
 */
class MyItem extends Item
{
    static final String TARGET_NS = "http://www.flymine.org/model/genomic#";
    static int id;

    /**
     * Constructor
     * @param className the class name of the item
     */
    MyItem(String className) {
        super();
        setIdentifier("-1_" + (id++));
        setClassName(TARGET_NS + className);
        setImplementations("");
    }

    /**
     * Add an attribute to this item
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    void setAttribute(String name, String value) {
        addAttribute(new Attribute(name, value));
    }

    /**
     * Add a reference to a collection of this item
     * @param name the name of the collection
     * @param reference the item to add to the collection
     */
    void addReference(String name, Item reference) {
        ReferenceList list = getCollection(name);
        if (list == null) {
            list = new ReferenceList(name);
            addCollection(list);
        }
        list.addRefId(reference.getIdentifier());
    }
}