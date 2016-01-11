package org.intermine.task.project;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Code for reading project.xml files.
 *
 * @author Kim Rutherford
 */
public final class ProjectXmlBinding
{

    private ProjectXmlBinding() {
        // don't
    }

    /**
     * Create a Project object from a project.xml file.
     * @param file the File
     * @return the Project
     */
    public static Project unmarshall(File file) {

        FileReader reader = null;
        try {
            reader = new FileReader(file);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }

        try {
            ProjectXmlHandler handler = new ProjectXmlHandler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.newSAXParser().parse(new InputSource(reader), handler);
            Project project = handler.project;
            project.validate(file);
            return project;
        } catch (IOException e) {
            throw new RuntimeException (e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("The underlying parser does not support "
                    + " the requested features", e);
        } catch (SAXException e) {
            throw new RuntimeException("Error parsing the project.xml file, "
                    + "please check the format.", e);
        }

    }

    private static class ProjectXmlHandler extends DefaultHandler
    {
        private final Pattern projectPattern = Pattern.compile(".*project$");
        private final Matcher projectMatcher = projectPattern.matcher("");

        Project project;
        Action action;
        //boolean postProcesses = false;

        /**
         * @see DefaultHandler#startElement
         */

        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName == null) {
                return;
            }
            projectMatcher.reset(qName);
            if (projectMatcher.matches()) {
                project = new Project();
                if (attrs.getValue("type") == null) {
                    throw new IllegalArgumentException("project type must be set in project.xml");
                } else {
                    project.setType(attrs.getValue("type"));
                }
            } else if ("post-process".equals(qName)) {
                PostProcess postProcess = new PostProcess();
                action = postProcess;
                project.addPostProcess(attrs.getValue("name"), postProcess);
            } else if ("source".equals(qName)) {
                Source source = new Source();
                source.setType(attrs.getValue("type"));
                source.setName(attrs.getValue("name"));
                project.addSource(attrs.getValue("name"), source);
                action = source;
            } else if ("property".equals(qName)) {
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
            if (qName == null) {
                return;
            }
            if ("source".equals(qName) || "post-process".equals(qName)) {
                action = null;
            }
        }
    }
}
