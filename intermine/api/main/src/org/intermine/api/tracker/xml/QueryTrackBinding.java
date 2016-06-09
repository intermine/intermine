package org.intermine.api.tracker.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.api.tracker.QueryTracker;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Daniela Butano
 */
public final class QueryTrackBinding
{
    private static final Logger LOG = Logger.getLogger(QueryTrackBinding.class);
    /**
     * label for XML
     */
    public static final String QUERYTRACKS = "querytracks";
    /**
     * label for XML
     */
    public static final String QUERYTRACK = "querytrack";

    private QueryTrackBinding() {
        // don't
    }

    /**
     * Convert a QueryTrack to XML and write XML to given writer.
     * @param uos the UserObjectStore
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ObjectStore uos, XMLStreamWriter writer) {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;

        try {
            writer.writeCharacters("\n");
            writer.writeStartElement(QUERYTRACKS);
            conn = ((ObjectStoreWriterInterMineImpl) uos).getConnection();
            String sql = "SELECT type, username, sessionidentifier, timestamp FROM "
                         + TrackerUtil.QUERY_TRACKER_TABLE;
            stm = conn.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                writer.writeCharacters("\n");
                writer.writeStartElement(QUERYTRACK);
                writer.writeAttribute("type", rs.getString(1));
                writer.writeAttribute("username", rs.getString(2));
                writer.writeAttribute("sessionidentifier", rs.getString(3));
                writer.writeAttribute("timestamp", rs.getTimestamp(4).toString());
                writer.writeEndElement();
            }
            writer.writeCharacters("\n");
            writer.writeEndElement();
        } catch (SQLException sqle) {
            LOG.error("The querytrack table does't exist!", sqle);
            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                LOG.error("XML broke", e);
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling query tracks", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    if (stm != null) {
                        stm.close();
                    }
                } catch (SQLException e) {
                    LOG.error("Problem closing  resources.", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uos).releaseConnection(conn);
        }
    }
}

/**
 * Event handler for the querytrack xml file
 * @author dbutano
 *
 */
class QueryTrackHandler extends TrackHandler
{
    /**
     * Create a new QueryTrackHandler
     * @param uosw an ObjectStoreWriter to the user profile database
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public QueryTrackHandler(ObjectStoreWriter uosw) {
        super(uosw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (QueryTrackBinding.QUERYTRACKS.equals(qName)) {
            try {
                connection = ((ObjectStoreWriterInterMineImpl) osw).getConnection();
                // Creating a query tracker will create an empty table if it doesn't exist
                QueryTracker.getInstance(connection, new LinkedList<Track>());
                stm = connection.prepareStatement("INSERT INTO " + TrackerUtil.QUERY_TRACKER_TABLE
                                                  + " VALUES(?, ?, ?, ?)");
            } catch (SQLException sqle) {
                throw new BuildException("Problem to retrieve the connection", sqle);
            }
        }
        if (QueryTrackBinding.QUERYTRACK.equals(qName)) {
            try {
                stm.setString(1, attrs.getValue("type"));
                stm.setString(2, attrs.getValue("username"));
                stm.setString(3, attrs.getValue("sessionidentifier"));
                stm.setTimestamp(4, Timestamp.valueOf(attrs.getValue("timestamp")));
                stm.executeUpdate();
            } catch (SQLException sqle) {
                throw new BuildException("problems during update", sqle);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (QueryTrackBinding.QUERYTRACKS.equals(qName)) {
            releaseResources();
        }
    }
}


