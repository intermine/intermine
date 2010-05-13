package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse InterPro data into items
 * @author Julie Sullivan
 */
public class InterProConverter extends FileConverter
{
    private Map<String, String> pubs = new HashMap();
    private Map<String, String> datasources = new HashMap();
    private Map<String, String> datasets = new HashMap();
    private Map<String, Item> proteinDomains = new HashMap();
    private Map<String, String> synonyms = new HashMap();
    private String datasourceRefId = null, datasetRefId = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws SAXException if something goes wrong
     */
    public InterProConverter(ItemWriter writer, Model model)
    throws SAXException {
        super(writer, model);
        datasourceRefId = getDataSource("InterPro");
        datasetRefId = getDataSet("InterPro");
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        InterProHandler handler = new InterProHandler(getItemWriter());
        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class InterProHandler extends DefaultHandler
    {
        private Item proteinDomain = null;
        private StringBuffer description = null;
        private Stack stack = new Stack();
        private String attName = null;
        private StringBuffer attValue = null;
        private ArrayList<Item> delayedItems = new ArrayList<Item>();

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param mapMaster the Map of maps
         */
        public InterProHandler(ItemWriter writer) {
            // Nothing to do
        }

        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {

            // descriptions span multiple lines
            // so don't reset temp var when processing descriptions
            if (attName != null && !attName.equals("description")) {
                attName = null;
            }

            // <interpro id="IPR000002" type="Domain" short_name="Fizzy" protein_count="256">
            if (qName.equals("interpro")) {
                String identifier = attrs.getValue("id");
                proteinDomain = getDomain(identifier);
                String name = attrs.getValue("short_name");
                proteinDomain.setAttribute("shortName", name);
                proteinDomain.setAttribute("type", attrs.getValue("type"));
                proteinDomain.addToCollection("dataSets", datasetRefId);

                createSynonym(proteinDomain.getIdentifier(), "name", name, datasetRefId);
                createSynonym(proteinDomain.getIdentifier(), "identifier", identifier,
                              datasetRefId);

                // reset
                description = new StringBuffer();

            // <publication><db_xref db="PUBMED" dbkey="8606774" />
            }  else if (qName.equals("db_xref") && stack.peek().equals("publication")) {
                String refId = getPub(attrs.getValue("dbkey"));
                proteinDomain.addToCollection("publications", refId);

            // <interpro><name>
            } else if (qName.equals("name") && stack.peek().equals("interpro")) {
                attName = "name";
            // <interpro><abstract>
            } else if (qName.equals("abstract") && stack.peek().equals("interpro")) {
                attName = "description";
            } else if (qName.equals("sec_ac")) {
                createSynonym(proteinDomain.getIdentifier(), "identifier", attrs.getValue("acc"),
                              datasetRefId);
           //<member_list><db_xref db="PFAM" dbkey="PF01167" name="SUPERTUBBY" />
            } else if (qName.equals("db_xref") && stack.peek().equals("member_list")) {
                String dbkey = attrs.getValue("dbkey");
                String name = attrs.getValue("name");
                String datasetName = attrs.getValue("db");
                createSynonym(proteinDomain.getIdentifier(), "identifier", dbkey,
                              getDataSet(datasetName));
                createSynonym(proteinDomain.getIdentifier(), "name", name, getDataSet(datasetName));
                //<interpro><foundin><rel_ref ipr_ref="IPR300000" /></foundin>
            } else if (qName.equals("rel_ref") && (stack.peek().equals("found_in")
                            || stack.peek().equals("contains")
                            || stack.peek().equals("child_list")
                            || stack.peek().equals("parent_list"))) {

                String domainRelationship = stack.peek().toString();
                String interproId = attrs.getValue("ipr_ref");
                Item relative = getDomain(interproId);
                String relationCollection = null;

                if (domainRelationship.equals("found_in")) {
                    relationCollection = "foundIn";
                } else if (domainRelationship.equals("contains")) {
                    relationCollection = "contains";
                } else if (domainRelationship.equals("parent_list")) {
                    relationCollection = "parentFeatures";
                } else if (domainRelationship.equals("child_list")) {
                    relationCollection = "childFeatures";
                }
                proteinDomain.addToCollection(relationCollection, relative);
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }

        /**
         * {@inheritDoc}
         */
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            super.endElement(uri, localName, qName);

            stack.pop();
            // finished processing file, store all domains
            if (qName.equals("interprodb")) {
                for (Item item : proteinDomains.values()) {
                    try {
                        store(item);
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e);
                    }
                }
                for (Item item : delayedItems) {
                    try {
                        store(item);
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e);
                    }
                }
            // <interpro><name>
            } else if (qName.equals("name") && stack.peek().equals("interpro")
                            && attName != null) {
                String name = attValue.toString();
                proteinDomain.setAttribute("name", name);
                createSynonym(proteinDomain.getIdentifier(), "name", name, datasetRefId);
            // <interpro><abstract>
            } else if (qName.equals("abstract") && stack.peek().equals("interpro")) {
                proteinDomain.setAttribute("description", description.toString());
                attName = null;
            }
        }

        private void createSynonym(String subject, String type, String value, String refId) {
            String key = value;
            if (synonyms.get(key) == null) {
                Item syn = createItem("Synonym");
                syn.setReference("subject", subject);
                syn.setAttribute("type", type);
                syn.setAttribute("value", value);
                syn.addToCollection("dataSets", refId);
                synonyms.put(key, syn.getIdentifier());
                delayedItems.add(syn);
            }
        }

        private Item getDomain(String identifier) {
            Item item = proteinDomains.get(identifier);
            if (item == null) {
                item = createItem("ProteinDomain");
                item.setAttribute("primaryIdentifier", identifier);
                proteinDomains.put(identifier, item);
            }
            return item;
        }

        private String getPub(String pubMedId)
        throws SAXException {
            String refId = pubs.get(pubMedId);
            if (refId == null) {
                Item item = createItem("Publication");
                item.setAttribute("pubMedId", pubMedId);
                pubs.put(pubMedId, item.getIdentifier());
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                refId = item.getIdentifier();
            }
            return refId;
        }

        /**
         * {@inheritDoc}
         */
        public void characters(char[] ch, int start, int length) {
            int st = start;
            int l = length;
            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (l > 0) {
                    boolean whitespace = false;
                    switch(ch[st]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        whitespace = true;
                        break;
                    default:
                        break;
                    }
                    if (!whitespace) {
                        break;
                    }
                    ++st;
                    --l;
                }

                if (l > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, st, l);
                    attValue.append(s);
                    if (attName.equals("description")) {
                        description.append(s);
                    }
                }
            }
        }
    }

    private String getDataSource(String title)
    throws SAXException {
        String refId = datasources.get(title);
        if (refId == null) {
            Item item = createItem("DataSource");
            item.setAttribute("name", title);
            refId = item.getIdentifier();
            datasources.put(title, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }

    private String getDataSet(String title)
    throws SAXException {
        String refId = datasets.get(title);
        if (refId == null) {
            Item item = createItem("DataSet");
            item.setAttribute("name", title + " data set");
            item.setReference("dataSource", datasourceRefId);
            refId = item.getIdentifier();
            datasets.put(title, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }
}
