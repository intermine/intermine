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

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.util.SAXParser;
import org.intermine.util.DynamicUtil;

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
        for (Iterator i = ((List) osw.getObjectStore().execute(q)).iterator(); i.hasNext();) {
            Publication publication = (Publication) ((List) i.next()).get(0);
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(new URL(ESUMMARY_URL + publication
                                                                 .getPubMedId()).openStream()));
            try {
                SAXParser.parse(new InputSource(reader), new Handler(publication));
                osw.store(publication);
                for (Iterator j = publication.getAuthors().iterator(); j.hasNext();) {
                    osw.store((InterMineObject) j.next());
                }
            } catch (SAXException e) {
                LOG.error("Failed to update publication: " + publication + " due to error: "
                          + e.getMessage());
            }
        }
    }

     /**
      * Extension of DefaultHandler to handle an esummary for a publication
      */
     class Handler extends DefaultHandler
     {
         Publication publication;
         String name;

         /**
          * Constructor
          * @param publication the publication that needs to be updated
          */
         public Handler(Publication publication) {
             this.publication = publication;
             publication.setAuthors(new ArrayList());
         }
         
         /**
          * @see DefaultHandler#startElement
          */
         public void startElement(String uri, String localName, String qName, Attributes attrs)
             throws SAXException {
             if ("ERROR".equals(qName)) {
                 throw new SAXException("esummary returned an error message - is the pmid valid?");
             }
             name = attrs.getValue("Name");
         }

         /**
          * @see DefaultHandler#characters
          */
         public void characters(char[] ch, int start, int length) throws SAXException {
             String string = new String(ch, start, length);
             if ("PubDate".equals(name)) {
                 publication.setYear(new Integer(string.split(" ")[0]));
             } else if ("Source".equals(name)) {
                 publication.setJournal(string);
             } else if ("Title".equals(name)) {
                 publication.setTitle(string);
             } else if ("Volume".equals(name)) {
                 publication.setVolume(string);
             } else if ("Issue".equals(name)) {
                 publication.setIssue(string);
             } else if ("Pages".equals(name)) {
                 publication.setPages(string);
             } else if ("Author".equals(name)) {
                 Author author = (Author)
                     DynamicUtil.createObject(Collections.singleton(Author.class));
                 author.setName(string);
                 author.getPublications().add(publication);
                 publication.getAuthors().add(author);
             }
         }

         /**
          * @see DefaultHandler#endElement
          */
         public void endElement(String uri, String localName, String qName) throws SAXException {
             name = null;
         }
     }
}
