package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;

import java.io.Reader;
import java.util.Map;
import java.util.HashMap;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.FullHandler;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.xml.sax.InputSource;

/**
 * Loads InterMine XML into an items database. This is an alternative to XmlDataLoader, and it can
 * cope with large input files.
 *
 * @author Matthew Wakeling
 */
public class FullXmlConverter extends DataConverter
{
    private static final Logger LOG = Logger.getLogger(FullXmlConverter.class);

    protected long count = 0;
    protected long start, time, times[];

    /**
     * Constructor.
     *
     * @param writer an ItemWriter to accept the loaded Items
     */
    public FullXmlConverter(ItemWriter writer) {
        super(writer);
    }

    /**
     * Unmarshal the Items from the input XML file, and write them to the writer.
     *
     * @param in a Reader containing the XML text input
     * @throws Exception if an error occurs
     */
    public void process(Reader in) throws Exception {
        start = System.currentTimeMillis();
        time = start;
        times = new long[20];
        for (int i = 0; i < 20; i++) {
            times[i] = -1;
        }
        SAXParser.parse(new InputSource(in), new FullDataXmlHandler());
    }

    /**
     * Extend SAX DefaultHandler to process XML.
     */
    class FullDataXmlHandler extends FullHandler
    {
        /**
         * @see DefaultHandler
         */
        public FullDataXmlHandler() {
        }

        /**
         * Do something useful with the Item.
         */
        public void finishedItem(Item item) {
            try {
                writer.store(ItemHelper.convert(item));
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
            count++;
            if (count % 10000 == 0) {
                long now = System.currentTimeMillis();
                if (times[(int) ((count / 10000) % 20)] == -1) {
                    LOG.info("Processed " + count + " rows - running at "
                            + (600000000L / (now - time)) + " (avg "
                            + ((60000L * count) / (now - start))
                            + ") rows per minute");
                } else {
                    LOG.info("Processed " + count + " rows - running at "
                            + (600000000L / (now - time)) + " (200000 avg "
                            + (12000000000L / (now - times[(int) ((count / 10000) % 20)]))
                            + ") (avg " + ((60000L * count) / (now - start))
                            + ") rows per minute");
                }
                time = now;
                times[(int) ((count / 10000) % 20)] = now;
            }
        }
    }
}
