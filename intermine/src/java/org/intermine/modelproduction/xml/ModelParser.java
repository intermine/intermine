package org.flymine.modelproduction.xml;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.flymine.metadata.*;

/**
 * DefaultHandler extension to support parsing of metadata XML
 *
 * @author Mark Woodbridge
 */
public class ModelParser
{
    protected ModelHandler handler = new ModelHandler();

    /**
     * Parse the metadata xml file
     * @param f the file to parse
     * @throws Exception if an error occuring during parsing
     */
    public void parse(File f) throws Exception {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.newSAXParser().parse(f, handler);
        } catch (ParserConfigurationException e) {
            throw new Exception("The underlying parser does not support "
                                + " the requested features");
        } catch (SAXException e) {
            throw new Exception("Error parsing XML document: " + e);
        }
    }

    /**
     * Return model name
     * @return model name
     */
    public String getModelName() {
        return handler.modelName;
    }
    
    /**
     * Return list of class descriptors
     * @return list of class descriptors
     */
    public List getClasses() {
        return handler.classes;
    } 

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class ModelHandler extends DefaultHandler
    {
        List attributes = new ArrayList();
        List references = new ArrayList();
        List collections = new ArrayList();
        List classes = new ArrayList();
        Attributes classAttrs;
        String modelName; //any more of these and we'll have to build a DOM instead
   
        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.equals("model")) {
                modelName = attrs.getValue("name");
            } else if (qName.equals("class")) {
                classAttrs = attrs;
            } else if (qName.equals("attribute")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("type");
                boolean primaryKey = new Boolean(attrs.getValue("primary-key")).booleanValue();
                attributes.add(new AttributeDescriptor(name, primaryKey, type));
            } else if (qName.equals("reference")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("referenced-type");
                String reverseReference = attrs.getValue("reverse-reference");
                boolean primaryKey = new Boolean(attrs.getValue("primary-key")).booleanValue();
                references.add(new ReferenceDescriptor(name, primaryKey, type, reverseReference));
            } else if (qName.equals("collection")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("referenced-type");
                boolean ordered = new Boolean(attrs.getValue("ordered")).booleanValue();
                String reverseReference = attrs.getValue("reverse-reference");
                boolean primaryKey = new Boolean(attrs.getValue("primary-key")).booleanValue();
                collections.add(new CollectionDescriptor(name, primaryKey, type, reverseReference, 
                                                         ordered));
            }
        }
    
        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("class")) {
                String name = classAttrs.getValue("name");
                String extend = classAttrs.getValue("extends");
                String implement = classAttrs.getValue("implements");
                boolean isInterface = new Boolean(classAttrs.getValue("is-interface"))
                    .booleanValue();
                classes.add(new ClassDescriptor(name, extend, implement, isInterface, attributes, 
                                                references, collections));
            }
        }
    }
}
