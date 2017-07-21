package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
public class InterProConverter extends BioFileConverter
{
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Item> proteinDomains = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws SAXException if something goes wrong
     */
    public InterProConverter(ItemWriter writer, Model model)
        throws SAXException {
        super(writer, model, "InterPro", "InterPro data set", null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        private Stack<String> stack = new Stack<String>();
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
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {

            // descriptions span multiple lines
            // so don't reset temp var when processing descriptions
            if (attName != null && !"description".equals(attName)) {
                attName = null;
            }

            // <interpro id="IPR000002" type="Domain" short_name="Fizzy" protein_count="256">
            if ("interpro".equals(qName)) {
                String identifier = attrs.getValue("id");
                proteinDomain = getDomain(identifier);
                String name = attrs.getValue("short_name");
                proteinDomain.setAttribute("shortName", name);
                proteinDomain.setAttribute("type", attrs.getValue("type"));

                // reset
                description = new StringBuffer();
            // <publication><db_xref db="PUBMED" dbkey="8606774" />
            }  else if ("db_xref".equals(qName) && "publication".equals(stack.peek())) {
                String refId = getPub(attrs.getValue("dbkey"));
                proteinDomain.addToCollection("publications", refId);
            // <interpro><name>
            } else if ("name".equals(qName) && "interpro".equals(stack.peek())) {
                attName = "name";
            // <interpro><abstract>
            } else if ("abstract".equals(qName) && "interpro".equals(stack.peek())) {
                attName = "description";
           //<member_list><db_xref db="PFAM" dbkey="PF01167" name="SUPERTUBBY" />
            } else if ("db_xref".equals(qName) && "member_list".equals(stack.peek())) {
                String dbkey = attrs.getValue("dbkey");
//                String name = attrs.getValue("name");
                String db = attrs.getValue("db");
                try {
                    Item item = createCrossReference(proteinDomain.getIdentifier(), dbkey,
                            db, false);
                    if (item != null) {
                        delayedItems.add(item);
                    }
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                //<interpro><foundin><rel_ref ipr_ref="IPR300000" /></foundin>
            } else if ("rel_ref".equals(qName) && ("found_in".equals(stack.peek())
                            || "contains".equals(stack.peek())
                            || "child_list".equals(stack.peek())
                            || "parent_list".equals(stack.peek()))) {

                String domainRelationship = stack.peek().toString();
                String interproId = attrs.getValue("ipr_ref");
                Item relative = getDomain(interproId);
                String relationCollection = null;

                if ("found_in".equals(domainRelationship)) {
                    relationCollection = "foundIn";
                } else if ("contains".equals(domainRelationship)) {
                    relationCollection = "contains";
                } else if ("parent_list".equals(domainRelationship)) {
                    relationCollection = "parentFeatures";
                } else if ("child_list".equals(domainRelationship)) {
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
        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);

            stack.pop();
            // finished processing file, store all domains
            if ("interprodb".equals(qName)) {
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
            } else if ("name".equals(qName) && "interpro".equals(stack.peek()) && attName != null) {
                String name = attValue.toString();
                proteinDomain.setAttribute("name", name);
            // <interpro><abstract>
            } else if ("abstract".equals(qName) && "interpro".equals(stack.peek())) {
                proteinDomain.setAttribute("description", description.toString());
                attName = null;
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
        @Override
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
                    if ("description".equals(attName)) {
                        description.append(s);
                    }
                }
            }
        }
    }
}
