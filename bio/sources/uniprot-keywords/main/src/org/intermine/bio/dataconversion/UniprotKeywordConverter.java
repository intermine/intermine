package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;

import java.io.Reader;
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
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     */
    public UniprotKeywordConverter(ItemWriter writer) {
        super(writer);
        
    }


    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
     
        UniprotHandler handler = new UniprotHandler(writer, ontoMaster);
                
        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    
    /**
     * Extension of PathQueryHandler to handle parsing TemplateQueries
     */
    static class UniprotHandler extends DefaultHandler
    {
        private int nextClsId = 0;
        private ItemFactory itemFactory;
        private ItemWriter writer;
        private Map ids = new HashMap();
        private Map aliases = new HashMap();
        private Map keywords = new HashMap();
        private String attName = null;
        private StringBuffer attValue = null;
        private Map ontoMaster;
        private Item ontology;
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param ontoMaster Holds the ontology variable that's used as 1/2 the key
         */
        public UniprotHandler(ItemWriter writer, Map ontoMaster) {
            
            itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
            this.writer = writer;         
            this.ontoMaster = ontoMaster;
          
        }

        
        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
             
            if (qName.equals("name")) {

                attName = "name";

            } else if (qName.equals("description")) {

                attName = "description";
                
            } else if (qName.equals("keywordList")) {
                
                ontology = setOnto("UniProtKeyword");
                
            } 
            
            super.startElement(uri, localName, qName, attrs);
      
            attValue = new StringBuffer();
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void characters(char[] ch, int start, int length) {

            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (length > 0) {
                    boolean whitespace = false;
                    switch(ch[start]) {
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
                    ++start;
                    --length;
                }

                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    attValue.append(s);
                }
            }
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);

            try {      
           
                if (qName.equals("name")) {
                    
                    String name = attValue.toString();
                    Item keyword = createItem("OntologyTerm");
                    keyword.addAttribute(new Attribute("name", name));   
                    keywords.put(name, keyword);
          
                } else if (qName.equals("description")) {

                    String descr = attValue.toString();
                    Iterator i = keywords.keySet().iterator();
                    while (i.hasNext()) {

                        String name = (String) i.next();
                        Item keyword = (Item) keywords.get(name);
                        keyword.addAttribute(new Attribute("description", descr));
                        keyword.addReference(new Reference("ontology", ontology.getIdentifier()));
                        writer.store(ItemHelper.convert(keyword));
                        
                    }  
                    // reset list                    
                    keywords = new HashMap();

                } 

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
       }
            


        private Item setOnto(String title) 
        throws SAXException {

            Item o = (Item) ontoMaster.get(title);
            try {
                if (o == null) {
                    o = createItem("Ontology");
                    o.addAttribute(new Attribute("title", title));
                    ontoMaster.put(title, o);
                    writer.store(ItemHelper.convert(o));
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
            return o;
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

