package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Writer;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;

import org.intermine.metadata.Model;
import org.intermine.InterMineException;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;

/**
 * Represents an XML mapping - performs XML (un)marshalling of objects/Full Data XML
 * @author Mark Woodbridge
 */
public class XmlBinding
{
    protected Model model;

    /**
     * Constructor
     * @param model an object model
     */
    public XmlBinding(Model model) {
        this.model = model;
    }

    /**
     * Marshal a collection of objects to an XML file.
     * @param col business objects to marshal into XML
     * @param writer the Writer to use
     * @throws IOException if error encountered with writer
     */
    public void marshal(Collection col, Writer writer) throws IOException {
        writer.write(FullRenderer.render(col, model));
        writer.flush();
    }

    /**
     * Unmarshal an XML file to an object.
     * @param is the InputStream to read from
     * @return a collection of business objects
     * @throws InterMineException if an error occurs during unmarshalling
     */
    public Collection unmarshal(InputStream is) throws InterMineException {
        try {
            return FullParser.realiseObjects(FullParser.parse(is), model, false);
        } catch (Exception e) {
            throw new InterMineException("Error during unmarshalling", e);
        }
    }
}
