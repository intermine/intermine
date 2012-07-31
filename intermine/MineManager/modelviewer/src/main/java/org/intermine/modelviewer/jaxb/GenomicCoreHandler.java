package org.intermine.modelviewer.jaxb;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.modelviewer.genomic.Attribute;
import org.intermine.modelviewer.genomic.Class;
import org.intermine.modelviewer.genomic.ClassReference;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Common super class for the core and genomic additions handlers, this class deals with most
 * of the guts of those two document types that are shared between the two.
 */
class GenomicCoreHandler extends DefaultHandler
{
    /**
     * The XML document locator.
     */
    protected Locator locator;
    
    /**
     * The list of Class objects built up during parsing.
     * <p>Note that these are NOT <code>java.lang.Class</code> instances.
     */
    private List<Class> classes;
    
    /**
     * The Class currently being handled.
     * <p>Note that this is NOT a <code>java.lang.Class</code>.
     */
    private Class currentClass;

    /**
     * Get the list of Class objects built up.
     * @return The Class objects.
     */
    protected List<Class> getClassList() {
        return classes;
    }

    /**
     * Set the XML document locator object.
     * 
     * @param locator The XML Locator.
     * 
     * @see org.xml.sax.ContentHandler#setDocumentLocator
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * Called at the start of the document, this implementation simply resets the
     * state of this handler.
     *        
     * @throws SAXException if there is a problem with the parsing.
     */
    @Override
    public void startDocument() throws SAXException {
        classes = new ArrayList<Class>();
        currentClass = null;
    }
    
    /**
     * Called at the start of a new element, this implementation looks for the
     * "class", "attribute", "reference" and "collection" elements and acts accordingly,
     * creating new {@link Class} objects as it goes.
     * 
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param qName The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     * @param attributes The attributes attached to the element.  If
     *        there are no attributes, it shall be an empty
     *        Attributes object.
     *        
     * @throws SAXException if there is a problem with the parsing.
     */
    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes)
    throws SAXException {

        if ("class".equals(localName)) {
            currentClass = new Class();
            currentClass.setName(attributes.getValue("name"));
            currentClass.setExtends(attributes.getValue("extends"));
            currentClass.setIsInterface(Boolean.parseBoolean(attributes.getValue("is-interface")));
        }
        if ("attribute".equals(localName)) {
            Attribute a = new Attribute();
            a.setName(attributes.getValue("name"));
            a.setType(attributes.getValue("type"));
            currentClass.getAttribute().add(a);
        }
        if ("collection".equals(localName) || "reference".equals(localName)) {
            ClassReference ref = new ClassReference();
            ref.setName(attributes.getValue("name"));
            ref.setReverseReference(attributes.getValue("reverse-reference"));
            ref.setReferencedType(attributes.getValue("referenced-type"));
            if ("collection".equals(localName)) {
                currentClass.getCollection().add(ref);
            } else {
                currentClass.getReference().add(ref);
            }
        }
    }

    /**
     * Called at the end of an element, this implementation looks for the
     * "class" element and adds the current Class object to the list of those
     * already read.
     * 
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param qName The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     *        
     * @throws SAXException if there is a problem with the parsing.
     */
    @Override
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
        if ("class".equals(localName)) {
            classes.add(currentClass);
            currentClass = null;
        }
    }
}
