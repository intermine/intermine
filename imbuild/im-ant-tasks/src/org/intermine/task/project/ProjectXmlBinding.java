package org.intermine.task.project;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Code for reading project.xml files.
 * @author Kim Rutherford
 */
public class ProjectXmlBinding
{
    /**
     * Create a Project object from a project.xml file.
     * @param file the File
     * @return the Project
     */
    public static Project unmarshall(File file) {
        try {
            FileReader reader = new FileReader(file);
            try {
                ProjectXmlHandler handler = new ProjectXmlHandler();
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(true);
                factory.newSAXParser().parse(new InputSource(reader), handler);
                return handler.project;
            } catch (ParserConfigurationException e) {
                throw new Exception("The underlying parser does not support "
                                    + " the requested features", e);
            } catch (SAXException e) {
                throw new Exception("Error parsing XML document", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class ProjectXmlHandler extends DefaultHandler
    {
        Project project;
        Action action;
        //boolean postProcesses = false;

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.equals("project")) {
                project = new Project();
                if (attrs.getValue("type") == null) {
                    throw new IllegalArgumentException("project type must be set in project.xml");
                } else {
                    project.setType(attrs.getValue("type"));
                }
            } else if (qName.equals("post-process")) {
                PostProcess postProcess = new PostProcess();
                action = postProcess;
                project.addPostProcess(attrs.getValue("name"), postProcess);
            } else if (qName.equals("source")) {
                Source source = new Source();
                source.setType(attrs.getValue("type"));
                project.addSource(attrs.getValue("name"), source);
                action = source;
            } else if (qName.equals("property")) {
                UserProperty property = new UserProperty();
                property.setName(attrs.getValue("name"));
                property.setValue(attrs.getValue("value"));
                property.setLocation(attrs.getValue("location"));
                if (action == null) {
                    // global property
                    project.addProperty(property);
                } else {
                    // property for a source or post-process
                    action.addUserProperty(property);
                }
            }
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("source") || qName.equals("post-process")) {
                action = null;
            }
        }
    }
}
