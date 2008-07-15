package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ReferenceList;
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
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private Map<String, Item> pubMaster = new HashMap<String, Item>();
    private Map<String, Item> dbMaster = new HashMap<String, Item>();
    private Map<String, Item> dsMaster = new HashMap<String, Item>();
    private Map<String, Item> proteinDomains = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public InterProConverter(ItemWriter writer, Model model) {
        super(writer, model);
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

        private Item proteinDomain;
        private Map synonyms;
        private StringBuffer description;
        private ReferenceList pubCollection;
        private ReferenceList parentsCollection;
        private ReferenceList kidsCollection;
        private ReferenceList foundInCollection;
        private ReferenceList containsCollection;

        private Item domainRelationships;
        private Item datasource;
        private Item dataset;

        private ItemWriter writer;
        private Stack stack = new Stack();
        private String attName = null;
        private StringBuffer attValue = null;

        private ArrayList<Item> delayedItems = new ArrayList();

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param mapMaster the Map of maps
         */
        public InterProHandler(ItemWriter writer) {
            this.writer = writer;
        }


        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
            if (attName != null && !attName.equals("description")) {
                attName = null;
            }

            // <interpro id="IPR000002" type="Domain" short_name="Fizzy" protein_count="256">
            if (qName.equals("interpro")) {

                String identifier = attrs.getValue("id");
                initProteinDomain(identifier);
                proteinDomain.setAttribute("type", attrs.getValue("type"));
                proteinDomain.setAttribute("shortName", attrs.getValue("short_name"));

                // <publication><db_xref db="PUBMED" dbkey="8606774" />
            }  else if (qName.equals("db_xref") && stack.peek().equals("publication")) {

                String identifier = attrs.getValue("dbkey");
                Item pub = getAndStoreItem(pubMaster, identifier, "Publication", "pubMedId");

                if (pubCollection.getRefIds().isEmpty()) {
                    proteinDomain.addCollection(pubCollection);
                }
                pubCollection.addRefId(pub.getIdentifier());

                // <interpro><name>
            } else if (qName.equals("name") && stack.peek().equals("interpro")) {

                attName = "name";

                // <interpro><abstract>
            } else if (qName.equals("abstract") && stack.peek().equals("interpro")) {

                attName = "description";

            } else if (qName.equals("sec_ac")) {

                createSynonym(proteinDomain.getIdentifier(), "identifier",
                              attrs.getValue("acc"), datasource.getIdentifier());

                //<member_list><db_xref db="PFAM" dbkey="PF01167" name="SUPERTUBBY" />
            } else if (qName.equals("db_xref") && stack.peek().equals("member_list")) {

                String dbkey = attrs.getValue("dbkey");
                String name = attrs.getValue("name");

                Item db = getAndStoreItem(dbMaster, attrs.getValue("db"), "DataSource", "name");

                createSynonym(proteinDomain.getIdentifier(), "identifier",
                              dbkey, db.getIdentifier());
                createSynonym(proteinDomain.getIdentifier(), "name",
                              name, db.getIdentifier());

                //<interpro><foundin>
            } else if (qName.equals("rel_ref") && (stack.peek().equals("found_in")
                            || stack.peek().equals("contains")
                            || stack.peek().equals("child_list")
                            || stack.peek().equals("parent_list"))) {

                String domainRelationship = stack.peek().toString();
                String interproId = attrs.getValue("ipr_ref");
                Item domain =
                    getItem(proteinDomains, interproId, "ProteinDomain", "primaryIdentifier");

                ReferenceList relationCollection = null;

                if (domainRelationship.equals("found_in")) {
                    relationCollection = foundInCollection;
                } else if (domainRelationship.equals("contains")) {
                    relationCollection = containsCollection;
                } else if (domainRelationship.equals("parent_list")) {
                    relationCollection = parentsCollection;
                } else if (domainRelationship.equals("child_list")) {
                    relationCollection = kidsCollection;
                }
                if (relationCollection != null) {
                    if (relationCollection.getRefIds().isEmpty()) {
                        domainRelationships.addCollection(relationCollection);
                    }
                    relationCollection.addRefId(domain.getIdentifier());
                }

                // <interprodb>
            }    else if (qName.equals("interprodb")) {
                initData();
            }

            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
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


        /**
         * {@inheritDoc}
         */
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            super.endElement(uri, localName, qName);

            try {
                stack.pop();

                // <interpro>
                if (qName.equals("interprodb")) {

                    for (Item item : proteinDomains.values()) {
                        createSynonym(item.getIdentifier(), "identifier",
                                      item.getAttribute("primaryIdentifier").getValue(),
                                      datasource.getIdentifier());
                        store(item);
                    }

                    for (Item item : delayedItems) {
                        store(item);
                    }

                // <interpro><name>
                } else if (qName.equals("name") && stack.peek().equals("interpro")) {

                    if (attName != null) {
                        proteinDomain.setAttribute("name", attValue.toString());
                    }

                // <interpro><abstract>
                } else if (qName.equals("abstract") && stack.peek().equals("interpro")) {

                    proteinDomain.setAttribute("description", description.toString());
                    attName = null;

                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }
        private void initData()
        throws SAXException    {
            try {
                datasource = getAndStoreItem(dbMaster, "InterPro", "DataSource", "name");
                dataset = getAndStoreItem(dsMaster, "InterPro data set", "DataSet", "title");

            } catch (Exception e) {
                throw new SAXException(e);
            }

        }

        private void createSynonym(String subjectId, String type, String value, String dbId) {
            String key = value.toLowerCase();

            if (synonyms.get(key) == null) {
                Item syn = createItem("Synonym");
                syn.setReference("subject", subjectId);
                syn.setAttribute("type", type);
                syn.setAttribute("value", value);
                syn.setReference("source", dbId);
                synonyms.put(key, syn);
                delayedItems.add(syn);
            }
        }

        private void initProteinDomain(String identifier) {

            proteinDomain =
                getItem(proteinDomains, identifier, "ProteinDomain", "primaryIdentifier");

            domainRelationships = createItem("DomainRelationship");
            proteinDomain.setReference("domainRelationships", domainRelationships);
            delayedItems.add(domainRelationships);

            pubCollection = new ReferenceList("publications", new ArrayList());
            parentsCollection = new ReferenceList("parentFeatures", new ArrayList());
            kidsCollection = new ReferenceList("childFeatures", new ArrayList());
            foundInCollection = new ReferenceList("foundIn", new ArrayList());
            containsCollection = new ReferenceList("contains", new ArrayList());

            synonyms = new HashMap();

            proteinDomain.setCollection("dataSets",
                              new ArrayList(Collections.singleton(dataset.getIdentifier())));

            description = new StringBuffer();
        }

        private Item getItem(Map map, String identifier, String itemType, String attr) {
            Item item = (Item) map.get(identifier);
            if (item == null) {
                item = createItem(itemType);
                item.setAttribute(attr, identifier);
                map.put(identifier, item);
            }
            return item;
        }

        private Item getAndStoreItem(Map map, String identifier, String itemType, String attr)
        throws SAXException {
            Item item = (Item) map.get(identifier);
            try {
                if (item == null) {
                    item = createItem(itemType);
                    item.setAttribute(attr, identifier);
                    map.put(identifier, item);
                    if (itemType.equals("DataSet")) {
                        item.setReference("dataSource", datasource);
                    }

                    writer.store(ItemHelper.convert(item));
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return item;
        }

        /**
         * Convenience method for creating a new Item
         * @param className the name of the class
         * @return a new Item
         */
        protected Item createItem(String className) {
            return InterProConverter.this.createItem(className);
        }
    }
}
