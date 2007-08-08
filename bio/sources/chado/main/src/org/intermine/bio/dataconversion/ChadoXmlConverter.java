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

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.OntologyUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * DataConverter to parse UniProt data into items
 * @author Richard Smith
 */
public class ChadoXmlConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    protected static final String PROP_FILE = "uniprot_config.properties";
    private static final Logger LOG = Logger.getLogger(ChadoXmlConverter.class);
   
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public ChadoXmlConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
    }


    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        ChadoXmlHandler handler = new ChadoXmlHandler(getItemWriter());

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
    static class ChadoXmlHandler extends DefaultHandler
    {
        private int nextClsId = 0;
        private ItemFactory itemFactory;

        private Map ids = new HashMap();
        private Map aliases = new HashMap();
        private Map<String, Item> organisms = new HashMap<String, Item>();
        
        private ItemWriter writer;

        private MatchableStack stack = new MatchableStack();
        private Stack<ObjectHolder> objects = new Stack<ObjectHolder>();
        private String attName = null;
        private StringBuffer charBuffer = null;
        
        private Model model = Model.getInstanceByName("genomic");
        
        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         * @param mapMaster the Map of maps
         * @param createInterpro whether or not to create interpro items
         */
        public ChadoXmlHandler(ItemWriter writer) {
            itemFactory = new ItemFactory(model);
            this.writer = writer;
        }


        /**
         * {@inheritDoc}
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            attName = null;
            // <feature>
            if (qName.equals("feature")) {
                FeatureHolder featureHolder = new FeatureHolder();
                objects.push(featureHolder);
            } else {
                if (qName.equals("featureloc")) {
                    FeatureLocHolder flh = new FeatureLocHolder();
                    objects.push(flh);
                }
            }
 
            // TODO not sure what attName is for
            attName = qName;
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            charBuffer = new StringBuffer();
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void characters(char[] ch, int start, int length) throws SAXException
        {

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
                    charBuffer.append(s);
                }
            }
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);

            if (qName.equals("accession")) {
                if (stack.matches("dbxref>dbxref_id>feature")) {
                    // TODO - for FlyBase genes this is the same as uniquename, ignore it? 
                } else if (stack.matches("dbxref>dbxref_id>cvterm>type_id>feature")) {
                    // this is the SO term identifier of the feature type
                } else if (stack.matches("dbxref>dbxref_id>cvterm_dbxref>cvterm>type_id>feature")) {
                    
                } else if (stack.matches("dbxref>dbxref_id>cvterm>cvterm_id>feature_cvterm>feature")) {
                
                } else if (stack.matches("dbxref>dbxref_id>feature_dbxref>feature")) {
                    
                } else {
                    throw new SAXException("LOST: " + stack);
                }
            } else if (qName.equals("uniquename")) {
                if (stack.matches("feature")) {
                    ((FeatureHolder) objects.peek()).uniquename = charBuffer.toString();
                } else if (stack.matches("pub")) {
                } else {
                    throw new SAXException("LOST: " + stack);
                }
            } else if (qName.equals("genus")) {
                if (stack.matches("organism>organism_id>feature")) {
                    ((FeatureHolder) objects.peek()).genus = charBuffer.toString();
                } else {
                    throw new SAXException("LOST: " + stack);
                }
            } else if (qName.equals("species")) {
                if (stack.matches("organism>organism_id>feature")) {
                    ((FeatureHolder) objects.peek()).species = charBuffer.toString();
                } else {
                    throw new SAXException("LOST: " + stack);
                }
            } else if (qName.equals("name")) {
                if (stack.matches("feature")) {
                    ((FeatureHolder) objects.peek()).name = charBuffer.toString();
                } else if (stack.matches("cvterm>type_id>feature")) {
                    String type = TypeUtil.javaiseClassName(charBuffer.toString());
                    ((FeatureHolder) objects.peek()).type = type;
                } else if (stack.matches("db>db_id>dbxref>dbxref_id>feature")) {
                    // ignoring dbxref name
                } else if (stack.matches("cv>cv_id>cvterm>type_id>feature")) {
                    // throw an error if type of a feature is not a SO term
                    if (!charBuffer.toString().equals("SO")) {
                        throw new SAXException("Feature had a type cvterm that wasn't SO: "
                                               + charBuffer.toString());
                    }
                } else if (stack.matches("db>db_id>dbxref>dbxref_id>cvterm>type_id>feature")) {
                    // this is the SO term identifier
                } else if (stack.matches("db>db_id>dbxref>dbxref_id>cvterm_dbxref>cvterm>type_id>feature")) {
                    
                } else if (stack.matches("cv>cv_id>cvterm>type_id>featureprop>feature")) {
                    // type of featureprop
                } else if (stack.matches("cvterm")) {

                } else if (stack.matches("cv>cv_id>cvterm>cvterm_id>feature_cvterm>feature")) {
                    
                } else if (stack.matches("cv>cv_id>cvterm>type_id>cvtermprop>cvterm>cvterm_id>feature_cvterm>feature")) {
                    
                } else if (stack.matches("cv>cv_id>cvterm>type_id>feature_cvtermprop>feature_cvterm>feature")) {
                               
                } else if (stack.matches("cvterm>type_id>cvtermprop>cvterm>cvterm_id>feature_cvterm>feature")) {
                    
                } else if (stack.matches("cvterm>cvterm_id>feature_cvterm>feature")) {
                    
                } else if (stack.matches("cvterm>type_id>pub>pub_id>feature_cvterm>feature")) {

                } else if (stack.matches("db>db_id>dbxref>dbxref_id>cvterm>cvterm_id>feature_cvterm>feature")) {

                } else if (stack.matches("synonym>synonym_id>feature_synonym>feature")) {

                } else if (stack.matches("db>db_id")) {
                } else if (stack.matches("cv>cv_id")) {

                } else {
                    throw new SAXException("LOST: " + stack);
                }
            } else if (qName.equals("seqlen")) {
                ((FeatureHolder) objects.peek()).seqlen = charBuffer.toString();
            } else if (qName.equals("feature")) {
                FeatureHolder holder = (FeatureHolder) objects.pop();
                if (model.hasClassDescriptor(model.getPackageName() + "." + holder.type)) {
                    Item f = createItem(holder.type);

                    f.setAttribute("identifier", holder.uniquename);
                    if (holder.name != null) {
                        f.setAttribute("name", holder.name);
                    }
                    if (holder.seqlen != null) {
                        f.setAttribute("length", holder.seqlen);
                    }
                    LOG.error("creating feature: " + holder.uniquename);
                    // TODO this should be an error!
                    if (holder.genus != null && holder.species != null) {
                        f.setReference("organism", getOrganism(holder.genus + " " 
                                                               + holder.species));
                    }
                    try {
                        for (FeatureLocHolder locHolder : holder.locations) {
                            Item loc = createItem("Location");
                            loc.setReference("subject", f.getIdentifier());
                            loc.setReference("object", locHolder.subjectId);
                            loc.setAttribute("start", String.valueOf(locHolder.fmin + 1));
                            loc.setAttribute("end", String.valueOf(locHolder.fmax));
                            loc.setAttribute("strand", String.valueOf(locHolder.strand));
                            writer.store(ItemHelper.convert(loc));
                        }
                        writer.store(ItemHelper.convert(f));
                    } catch (ObjectStoreException e) {
                        throw new SAXException(e); 
                    }
                    if (!objects.isEmpty()) {
                        ObjectHolder next = objects.peek();
                        if (next instanceof FeatureLocHolder) {
                            ((FeatureLocHolder) next).subjectId = f.getIdentifier();
                        }
                    }
                }
            } else if (qName.equals("featureloc")) {
                FeatureLocHolder holder = (FeatureLocHolder) objects.pop();
                ((FeatureHolder) objects.peek()).locations.add(holder);
                
            } else if (qName.equals("strand")) {
                if (stack.matches("featureloc>feature")) {
                    ((FeatureLocHolder) objects.peek()).strand = charBuffer.toString();
                }
            } else if (qName.equals("fmin")) {
                    if (stack.matches("featureloc>feature")) {
                        ((FeatureLocHolder) objects.peek()).fmin = charBuffer.toString();
                    }
            } else if (qName.equals("fmax")) {
                if (stack.matches("featureloc>feature")) {
                    ((FeatureLocHolder) objects.peek()).fmax = charBuffer.toString();
                }
            }
            stack.pop();
       }


        private Item getOrganism(String name) throws SAXException {
            LOG.error("XXXcalled getOrganism(" + name + ")");
            String taxonId = null;
            if (name.equals("Drosophila melanogaster")) {
                taxonId = "7227";
            } else {
                throw new RuntimeException("Unrecognised organism abbreviation: "
                                           + name);
            }
            Item organism = organisms.get(taxonId);
            if (organism == null) {
                organism = createItem("Organism");
                organism.setAttribute("taxonId", taxonId);
                try {
                    writer.store(ItemHelper.convert(organism));
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
                    organisms.put(taxonId, organism);
            }
            return organism;        
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
        
        
        private class MatchableStack extends Stack<String>
        {
            public boolean matches(String pattern) {
                String[] elements = StringUtil.split(pattern, ">");
                for (int i = 0; i < elements.length; i++) {
                    if (!this.elementAt(this.size() - i - 2).equals(elements[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        
        private abstract class ObjectHolder {};
        
        private class FeatureHolder extends ObjectHolder
        {
            List<FeatureLocHolder> locations = new ArrayList<FeatureLocHolder>();
            public List<FeatureLocHolder> relations = new ArrayList<FeatureLocHolder>();
            public String species;
            public String genus;
            public String uniquename;
            public String seqlen;
            public String name;
            public String type;
        }
        
        private class RelationshipHolder extends ObjectHolder
        {
            String type;
        }
        
        private class FeatureLocHolder extends ObjectHolder
        {
            public String strand;
            public String subjectId;
            String fmin;
            String fmax;
        }
    }
}