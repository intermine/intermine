package org.intermine.task;

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
import java.io.FileInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;

/**
 * An ANT Task to retrieve a string into a property from an XML file using an XPath expression.
 *
 * @author Kim Rutherford
 */

public class XPathSelect extends Task
{
    protected File xmlFile = null;
    protected String expression = null;
    protected String propName = null;

    /**
     * Set the file to write to.
     * @param xmlFile the File to read
     */
    public void setFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
     * Set the XPath expression.
     * @param expression the XPath expression to apply to the contents of the file.
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Specify the property to set when the XPath expression matches.
     * @param propName the property to set
     */
    public void setPropName(String propName) {
        this.propName = propName;
    }

    /**
     * Execute the task.
     * @throws BuildException if the is a problem while executing
     */
    public void execute() throws BuildException {
        if (xmlFile == null) {
            throw new BuildException("no xmlFile specified");
        }

        if (expression == null) {
            throw new BuildException("no expression specified");
        }

        if (propName == null) {
            throw new BuildException("no property specified");
        }

        String returnResult;
        try {
            InputSource in = new InputSource(new FileInputStream(xmlFile));
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            Document doc = dfactory.newDocumentBuilder().parse(in);

            Transformer serializer = TransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            NodeIterator nl = XPathAPI.selectNodeIterator(doc, expression);

            returnResult = null;
            Node n;
            while ((n = nl.nextNode()) != null) {
                StringWriter stringWriter = new StringWriter();
                serializer.transform(new DOMSource(n), new StreamResult(stringWriter));
                if (returnResult == null) {
                    returnResult = stringWriter.toString();
                } else {
                    throw new BuildException("XPath expression (" + expression
                                             + ") matched more than once");
                }
            }

            if (returnResult == null) {
                throw new BuildException("No matches");
            } else {
                this.getProject().setProperty(propName, returnResult);
            }
        } catch (Exception e) {
            throw new BuildException("Exception while applying expression: " + expression, e);
        }
    }
}
