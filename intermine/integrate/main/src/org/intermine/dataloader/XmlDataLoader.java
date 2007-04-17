package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;

import org.intermine.InterMineException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.XmlBinding;

import org.apache.log4j.Logger;

/**
 * Provides a method for unmarshalling XML given source  into java
 * business objects then calls store on each.
 * store() is AbstractDataLoader.store().
 *
 * @author Richard Smith
 */

public class XmlDataLoader extends DataLoader
{
    private static final Logger LOG = Logger.getLogger(XmlDataLoader.class);
    private static int idCounter = 1;
    
    /**
     * @see DataLoader#DataLoader(IntegrationWriter)
     */
    public XmlDataLoader(IntegrationWriter iw) {
        super(iw);
    }

    /**
     * Static method to unmarshall business objects from a given xml file and call
     * store on each.
     *
     * @param is access to xml file
     * @param source the main source
     * @param skelSource the skeleton source
     * @throws InterMineException if anything goes wrong with xml or storing
     */
    public void processXml(InputStream is, Source source, Source skelSource)
        throws InterMineException {
        try {
            long times[] = new long[20];
            for (int i = 0; i < 20; i++) {
                times[i] = -1;
            }
            long opCount = 0;
            long time = (new Date()).getTime();
            long startTime = time;
            LOG.info("Starting XmlDataLoader. Loading XML file.");
            XmlBinding binding = new XmlBinding(getIntegrationWriter().getObjectStore().getModel());

            List objects = (List) binding.unmarshal(is);
            LOG.info("Loaded XML file to list of " + objects.size() + " objects");

            Iterator iter = objects.iterator();
            while (iter.hasNext()) {
                InterMineObject o = (InterMineObject) iter.next();
                o.setId(new Integer(idCounter++));
            }

            getIntegrationWriter().beginTransaction();
            iter = objects.iterator();
            while (iter.hasNext()) {
                getIntegrationWriter().store((InterMineObject) iter.next(), source, skelSource);
                opCount++;
                if (opCount % 1000 == 0) {
                    long now = (new Date()).getTime();
                    if (times[(int) ((opCount / 1000) % 20)] == -1) {
                        LOG.info("Dataloaded " + opCount + " objects - running at "
                                + (60000000 / (now - time)) + " (avg "
                                + ((60000L * opCount) / (now - startTime))
                                + ") objects per minute");
                    } else {
                        LOG.info("Dataloaded " + opCount + " objects - running at "
                                + (60000000 / (now - time)) + " (20000 avg "
                                + (1200000000 / (now - times[(int) ((opCount / 1000) % 20)]))
                                + ") (avg = " + ((60000L * opCount) / (now - startTime))
                                + ") objects per minute");
                    }
                    time = now;
                    times[(int) ((opCount / 1000) % 20)] = now;
                    getIntegrationWriter().commitTransaction();
                    getIntegrationWriter().beginTransaction();
                }
            }
            LOG.info("Finished dataloading " + opCount + " objects at " + ((60000L * opCount)
                        / ((new Date()).getTime() - startTime)) + " object per minute");
            getIntegrationWriter().commitTransaction();
        } catch (ObjectStoreException e) {
            throw new InterMineException("Problem with store method", e);
        }
    }
    
    /**
     * Perform any necessary clean-up and close the integration writer
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        getIntegrationWriter().close();    
    }
}
