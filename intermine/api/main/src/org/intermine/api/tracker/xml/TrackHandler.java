package org.intermine.api.tracker.xml;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TrackHandler extends DefaultHandler
{
    private static final Logger LOG = Logger.getLogger(TrackHandler.class);
    protected ObjectStoreWriter osw;
    protected Connection connection;
    protected PreparedStatement stm;

    public TrackHandler(ObjectStoreWriter uosw) {
        this.osw = uosw;
    }

    public void releaseResources()
        throws SAXException {
        if (stm != null) {
            try {
                stm.close();
            } catch (SQLException sqle) {
                LOG.error("problems closing resources", sqle);
            }
        }
        ((ObjectStoreWriterInterMineImpl) osw).releaseConnection(connection);
    }
}
