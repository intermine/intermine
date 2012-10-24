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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * A filter to add name spaces before JAXB sees them, thus preventing errors
 * from the depths of the JAXB unmarshaller.
 * <p>Unfortunately, this was an experiment that didn't really work.</p>
 * 
 * @see <a href="http://stackoverflow.com/questions/277502/jaxb-how-to-ignore-namespace-during-unmarshalling-xml-document">
 * JAXB: How to ignore namespace during unmarshalling XML document?</a>
 * 
 * @see ConfigParser#twoPassUnmarshall
 */
public class NamespaceFilter extends XMLFilterImpl
{
    /**
     * The name space URI to add to the stream.
     */
    private String usedNamespaceUri;

    /**
     * Flag indicating that the name space has been added and should not
     * be added again. 
     */
    private boolean addedNamespace = false;

    /**
     * Initialise with the name space URI to add to the stream.
     * 
     * @param namespace The name space URI.
     */
    public NamespaceFilter(String namespace) {
        this(namespace, null);
    }

    /**
     * Initialise with the name space URI to add to the stream against
     * a parent XMLReader.
     * 
     * @param namespace The name space URI.
     * @param parent The parent XMLReader.
     */
    public NamespaceFilter(String namespace, XMLReader parent) {
        super(parent);
        usedNamespaceUri = namespace;
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        startControlledPrefixMapping();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(usedNamespaceUri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts)
    throws SAXException {
        super.startElement(usedNamespaceUri, localName, qName, atts);
    }

    @Override
    public void startPrefixMapping(String prefix, String url)
    throws SAXException {
        startControlledPrefixMapping();
    }

    /**
     * When the default prefix mapping is invoked, this method is called
     * to pass the desired name space URI to the super class implementation
     * of <code>startPrefixMapping</code>, mapping the empty (default) name
     * space as the name space URI given at creation.
     * 
     * @throws SAXException if there is a SAX problem when calling up the
     * chain.
     * 
     * @see XMLFilterImpl#startPrefixMapping(String, String)
     */
    private void startControlledPrefixMapping() throws SAXException {

        if (!addedNamespace) {
            //We should add namespace since it is set and has not yet been done.
            super.startPrefixMapping("", usedNamespaceUri);

            //Make sure we don't do it twice
            addedNamespace = true;
        }
    }
}
