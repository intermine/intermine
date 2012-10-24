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

import org.intermine.modelviewer.project.PostProcess;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.intermine.modelviewer.project.Source;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX ContentHandler for reading project files.
 */
class ProjectHandler extends DefaultHandler implements BackupContentHandler
{
    /**
     * The XML document locator.
     */
    protected Locator locator;
    
    /**
     * The Project object being read.
     */
    private Project project;
    
    /**
     * The Source object being assembled at a given time.
     */
    private Source currentSource;

    /**
     * Get the resulting Project object.
     * @return The project.
     */
    @Override
    public Project getResult() {
        return project;
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
        super.startDocument();
        project = null;
        currentSource = null;
    }

    /**
     * Called at the start of a new element, this implementation looks for the
     * elements of a project XML file and acts accordingly.
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
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    throws SAXException {
        
        if ("project".equals(localName)) {

            if (project != null) {
                throw new SAXParseException("A \"project\" element has already been found.",
                                            locator);
            }
            
            project = new Project();
            project.setType(attributes.getValue("type"));
            project.setSources(new Project.Sources());
            project.setPostProcessing(new Project.PostProcessing());
        } else {
            if (project == null) {
                throw new SAXParseException("No outermost \"project\" element", locator);
            }
        }
        if ("source".equals(localName)) {
            currentSource = new Source();
            currentSource.setName(attributes.getValue("name"));
            currentSource.setType(attributes.getValue("type"));
            currentSource.setDump(Boolean.valueOf(attributes.getValue("dump")));
        }
        if ("post-process".equals(localName)) {
            PostProcess pp = new PostProcess();
            pp.setName(attributes.getValue("name"));
            pp.setDump(Boolean.valueOf(attributes.getValue("dump")));
            project.getPostProcessing().getPostProcess().add(pp);
        }
        if ("property".equals(localName)) {
            Property p = new Property();
            p.setName(attributes.getValue("name"));
            p.setLocation(attributes.getValue("location"));
            p.setValue(attributes.getValue("value"));
            if (currentSource != null) {
                currentSource.getProperty().add(p);
            } else {
                project.getProperty().add(p);
            }
        }
    }

    /**
     * Called at the end of an element, this implementation looks for the
     * "source" element and adds the current Source object to the parent Project
     * object.
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
        if ("source".equals(localName)) {
            project.getSources().getSource().add(currentSource);
            currentSource = null;
        }
    }
}
