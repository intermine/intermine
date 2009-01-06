package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Importer to add descriptions to UniProt keywords
 * @author Julie Sullivan
 */
public class UniprotKeywordConverter extends FileConverter
{

    //TODO: This should come from props files
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private Map ontoMaster = new HashMap();
    private Map keyMaster = new HashMap();
    private Map synMaster = new HashMap();
    private Map mapMaster = new HashMap();  // map of maps

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public UniprotKeywordConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        mapMaps();
        UniprotHandler handler = new UniprotHandler(getItemWriter(), mapMaster);

        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private void mapMaps() {

        mapMaster.put("ontoMaster", ontoMaster);    // ontology - only one
        mapMaster.put("keyMaster", keyMaster);      // keyword names
        mapMaster.put("synMaster", synMaster);      // synonyms

    }

    /**
     * An implementation of DefaultHandler for parsing UniProt XML.
     */
    static class UniprotHandler extends DefaultHandler
    {
        private int nextClsId = 0;
        private ItemFactory itemFactory;
        private ItemWriter writer;
        private Map ids = new HashMap();
        private Map aliases = new HashMap();
        private String name = null;
        private String attName = null;
        private StringBuffer attValue = null;
        private Map ontoMaster;
        private Map keyMaster;
        private Map synMaster;
        private Item ontology;
        private ReferenceList synCollection;

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param mapMaster Map of all of the maps
         */
        public UniprotHandler(ItemWriter writer, Map mapMaster) {

            this.ontoMaster = (Map) mapMaster.get("ontoMaster");
            this.keyMaster = (Map) mapMaster.get("keyMaster");
            this.synMaster = (Map) mapMaster.get("synMaster");
            itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
            this.writer = writer;

        }


        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            try {
                if (qName.equals("name")) {

                    attName = "name";

                } else if (qName.equals("description")) {

                    attName = "description";

                } else if (qName.equals("keywordList")) {

                    ontology = getItem(ontoMaster, "Ontology", "title", "UniProtKeyword");
                    writer.store(ItemHelper.convert(ontology));
                }
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

            super.startElement(uri, localName, qName, attrs);

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

                if (qName.equals("name")) {

                    if (name == null) {
                        name = attValue.toString();
                        synCollection = new ReferenceList("synonyms", new ArrayList());
                    }  else {
                        boolean store = false;
                        if (!synMaster.containsKey(attValue.toString())) {
                            store = true;
                        }
                        Item syn = getItem(synMaster, "OntologyTermSynonym",
                                           "name", attValue.toString());
                        if (store) {
                            writer.store(ItemHelper.convert(syn));
                        }

                        synCollection.addRefId(syn.getIdentifier());
                    }
                } else if (qName.equals("description")) {

                    // there are category keywords which don't have a name, we will
                    // have to ignore these.
                    if (name != null) {
                        String descr = attValue.toString();

                        Item keyword = getItem(keyMaster, "OntologyTerm", "name", name);
                        if (keyword != null) {
                            if (!synCollection.getRefIds().isEmpty()) {
                                keyword.addCollection(synCollection);
                            }
                            keyword.setAttribute("description", descr);
                            keyword.setReference("ontology", ontology.getIdentifier());
                            writer.store(ItemHelper.convert(keyword));
                        }
                        name = null;
                    }
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        private Item getItem(Map map, String itemType, String titleType, String title) {

            Item item = (Item) map.get(title);
            if (item == null) {
                item = createItem(itemType);
                item.setAttribute(titleType, title);
                map.put(title, item);

            }
            return item;
        }


        /**
         * Convenience method for creating a new Item
         * @param className the name of the class
         * @return a new Item
         */
        protected Item createItem(String className) {

            return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                                                  GENOMIC_NS + className, "");
        }

        private String newId(String className) {
            Integer id = (Integer) ids.get(className);
            if (id == null) {
                id = new Integer(0);
                ids.put(className, id);
            }
            id = new Integer(id.intValue() + 1);
            ids.put(className, id);
            return id.toString();
        }

        /**
         * Uniquely alias a className
         * @param className the class name
         * @return the alias
         */
        protected String alias(String className) {
            String alias = (String) aliases.get(className);
            if (alias != null) {
                return alias;
            }
            String nextIndex = "" + (nextClsId++);
            aliases.put(className, nextIndex);
            return nextIndex;
        }
    }
}

