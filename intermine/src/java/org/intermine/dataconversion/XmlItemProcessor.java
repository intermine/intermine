package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Writer;

import org.flymine.xml.full.Item;
import org.flymine.xml.full.FullRenderer;

/**
* ItemProcessor that renders items as XML to a Writer
* @author Mark Woodbridge
*/
public class XmlItemProcessor extends ItemProcessor
{
    protected static final String ENDL = System.getProperty("line.separator");
    protected Writer writer;

    /**
    * Constructor
    * @param writer the Writer used to output XML
    */
    public XmlItemProcessor(Writer writer) {
        this.writer = writer;
    }

    /**
    * @see ItemProcessor#preProcess
    */
    public void preProcess() throws Exception {
         writer.write(FullRenderer.getHeader() + ENDL);
    }

    /**
    * @see ItemProcessor#process
    */
    public void process(Item item) throws Exception {
        writer.write(FullRenderer.render(item));
    }

    /**
    * @see ItemProcessor#postProcess
    */
    public void postProcess() throws Exception {
        writer.write(FullRenderer.getFooter() + ENDL);
    }
}
