package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Importer to add descriptions to UniProt keywords
 * @author Julie Sullivan
 */
public class UniprotKeywordConverter extends BioFileConverter
{
    private Map<String, String> ontologies = new HashMap<String, String>();
    private Map<String, String> keywords = new HashMap<String, String>();
    private Map<String, String> synonyms = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public UniprotKeywordConverter(ItemWriter writer, Model model) {
        super(writer, model, "UniProt", "UniProt keywords data set", null);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        UniprotKeywordHandler handler = new UniprotKeywordHandler();
        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * An implementation of DefaultHandler for parsing UniProt XML.
     */
    private class UniprotKeywordHandler extends DefaultHandler
    {
        private String attName = null;
        private StringBuffer attValue = null;
        private String ontologyRefId = null;
        private ReferenceList synRefIds = new ReferenceList("synonyms");
        private String name = null;

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param mapMaster Map of all of the maps
         */
        public UniprotKeywordHandler() {
            try {
                ontologyRefId = getItem(ontologies, "Ontology", "name", "UniProtKeyword");
            } catch (SAXException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if ("name".equals(qName)) {
                attName = "name";
            } else if ("description".equals(qName)) {
                attName = "description";
            }
            super.startElement(uri, localName, qName, attrs);
            attValue = new StringBuffer();
        }

        /**
         * {@inheritDoc}
         */
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);
            if ("name".equals(qName)  && attValue != null
                    && StringUtils.isNotEmpty(attValue.toString())) {
                String synonym = attValue.toString();
                if (name == null) {
                    name = synonym;
                } else {
                    String refId = getItem(synonyms, "OntologyTermSynonym", "name", synonym);
                    synRefIds.addRefId(refId);
                }
            } else if ("description".equals(qName)) {
                String descr = attValue.toString();
                Item keyword = getKeyword(name);
                if (keyword != null) {
                    if (!synRefIds.getRefIds().isEmpty()) {
                        keyword.addCollection(synRefIds);
                    }
                    keyword.setAttribute("description", descr);
                    keyword.setReference("ontology", ontologyRefId);
                    try {
                        store(keyword);
                    } catch (ObjectStoreException e) {
                        throw new SAXException("failed storing", e);
                    }
                }
            } else if ("keyword".equals(qName)) {
                // new keyword, reset
                synRefIds = new ReferenceList("synonyms");
                name = null;
            }
        }

        private String getItem(Map<String, String> map, String itemType, String titleType,
                               String title)
            throws SAXException {
            String refId = map.get(title);
            if (refId == null) {
                Item item = createItem(itemType);
                item.setAttribute(titleType, title);
                refId = item.getIdentifier();
                map.put(title, refId);
                try {
                    store(item);
                } catch (ObjectStoreException e) {
                    throw new SAXException("failed storing", e);
                }
            }
            return refId;
        }

        private Item getKeyword(String keyword) {
            String refId = keywords.get(keyword);
            if (refId == null) {
                Item item = createItem("OntologyTerm");
                item.setAttribute("name", keyword);
                keywords.put(keyword, item.getIdentifier());
                return item;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            if (attName != null) {
                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    String str = s.toString();
                    str = str.replaceAll("[\n\r]", " ");
                    attValue.append(str);
                }
            }
        }
    }
}

