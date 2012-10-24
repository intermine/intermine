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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.api.tracker.KeySearchTracker;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class SearchTrackBinding
{
    private static final Logger LOG = Logger.getLogger(SearchTrackBinding.class);
    public static final String SEARCHTRACKS = "searchtracks";
    public static final String SEARCHTRACK = "searchtrack";


    /**
     * Convert a SearchTrack to XML and write XML to given writer.
     * @param uos the UserObjectStore
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ObjectStore uos, XMLStreamWriter writer) {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;

        try {
            writer.writeCharacters("\n");
            writer.writeStartElement(SEARCHTRACKS);
            conn = ((ObjectStoreWriterInterMineImpl) uos).getConnection();
            String sql = "SELECT keyword, username, sessionidentifier, timestamp FROM "
                         + TrackerUtil.SEARCH_TRACKER_TABLE;
            stm = conn.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                writer.writeCharacters("\n");
                writer.writeStartElement(SEARCHTRACK);
                writer.writeAttribute("keyword", rs.getString(1));
                writer.writeAttribute("username", rs.getString(2));
                writer.writeAttribute("sessionidentifier", rs.getString(3));
                writer.writeAttribute("timestamp", rs.getTimestamp(4).toString());
                writer.writeEndElement();
            }
            writer.writeCharacters("\n");
            writer.writeEndElement();
        } catch (SQLException sqle) {
            LOG.error("The templatetrack table does't exist!", sqle);
            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling search tracks", e);
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
 * Event handler for the searchtrack xml file
 * @author dbutano
 *
 */
class SearchTrackHandler extends TrackHandler
{
    /**
     * Create a new SearchTrackHandler
     * @param uosw an ObjectStoreWriter to the user profile database
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public SearchTrackHandler(ObjectStoreWriter uosw) {
        super(uosw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (SearchTrackBinding.SEARCHTRACKS.equals(qName)) {
            try {
                connection = ((ObjectStoreWriterInterMineImpl) osw).getConnection();
                // Creating a search tracker will create an empty table if it doesn't exist
                KeySearchTracker.getInstance(connection, new LinkedList<Track>());
                stm = connection.prepareStatement("INSERT INTO "
                      + TrackerUtil.SEARCH_TRACKER_TABLE + " VALUES(?, ?, ?, ?)");
            } catch (SQLException sqle) {
                new BuildException("Problem to retrieve the connection", sqle);
            }
        }
        if (SearchTrackBinding.SEARCHTRACK.equals(qName)) {
            try {
                stm.setString(1, attrs.getValue("keyword"));
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
        if (SearchTrackBinding.SEARCHTRACKS.equals(qName)) {
            releaseResources();
        }
    }
}


