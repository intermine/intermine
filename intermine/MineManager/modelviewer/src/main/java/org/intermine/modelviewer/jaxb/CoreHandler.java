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

import org.intermine.modelviewer.genomic.Model;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * SAX ContentHandler for reading the core model file (core.xml).
 * <p>Most of the work is done by the <code>GenomicCoreHandler</code> superclass:
 * this implementation handles the outermost "model" element.
 */
class CoreHandler extends GenomicCoreHandler implements BackupContentHandler
{
    /**
     * The model created as the parse occurs.
     */
    private Model model;

    /**
     * Get the resulting Model object.
     * @return The model.
     */
    @Override
    public Model getResult() {
        return model;
    }

    /**
     * Called at the start of the document, this implementation simply resets the
     * state of this handler.
     *        
     * @throws SAXException if there is a problem with the parsing.
     */
    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        model = null;
    }

    /**
     * Called at the start of a new element, this implementation looks for the
     * "model" outermost element and starts the creation of the model.
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

        if ("model".equals(localName)) {

            if (model != null) {
                throw new SAXParseException("A \"model\" element has already been found.", locator);
            }
            
            model = new Model();
            model.setName(attributes.getValue("name"));
            model.setPackage(attributes.getValue("package"));
        } else {
            if (model == null) {
                throw new SAXParseException("No outermost \"model\" element", locator);
            }
            super.startElement(uri, localName, qName, attributes);
        }
    }

    /**
     * Called at the end of an element, this implementation looks for the
     * "model" outermost element and sets the Model object's classes when this occurs.
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
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("model".equals(localName)) {
            model.getClazz().addAll(getClassList());
        } else {
            super.endElement(uri, localName, qName);
        }
    }
}
