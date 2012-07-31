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
import org.intermine.api.tracker.LoginTracker;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class LoginTrackBinding
{
    private static final Logger LOG = Logger.getLogger(LoginTrackBinding.class);
    public static final String LOGINTRACKS = "logintracks";
    public static final String LOGINTRACK = "logintrack";


    /**
     * Convert a LoginTrack to XML and write XML to given writer.
     * @param uos the UserObjectStore
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ObjectStore uos, XMLStreamWriter writer) {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;

        try {
            writer.writeCharacters("\n");
            writer.writeStartElement(LOGINTRACKS);
            conn = ((ObjectStoreWriterInterMineImpl) uos).getConnection();
            String sql = "SELECT username, timestamp FROM " + TrackerUtil.LOGIN_TRACKER_TABLE;
            stm = conn.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                writer.writeCharacters("\n");
                writer.writeStartElement(LOGINTRACK);
                writer.writeAttribute("username", rs.getString(1));
                writer.writeAttribute("timestamp", rs.getTimestamp(2).toString());
                writer.writeEndElement();
            }
            writer.writeCharacters("\n");
            writer.writeEndElement();
        } catch (SQLException sqle) {
            LOG.error("The logintrack table does't exist!", sqle);
            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling login tracks", e);
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
 * Event handler for the logintrack xml file
 * @author dbutano
 *
 */
class LoginTrackHandler extends TrackHandler
{
    /**
     * Create a new LoginTrackHandler
     * @param uosw an ObjectStoreWriter to the user profile database
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public LoginTrackHandler(ObjectStoreWriter uosw) {
        super(uosw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (LoginTrackBinding.LOGINTRACKS.equals(qName)) {
            try {
                connection = ((ObjectStoreWriterInterMineImpl) osw).getConnection();
                // Creating a login tracker will create an empty table if it doesn't exist
                LoginTracker.getInstance(connection, new LinkedList<Track>());
                stm = connection.prepareStatement("INSERT INTO "
                      + TrackerUtil.LOGIN_TRACKER_TABLE + " VALUES(?, ?)");
            } catch (SQLException sqle) {
                new BuildException("Problem to retrieve the connection", sqle);
            }
        }
        if (LoginTrackBinding.LOGINTRACK.equals(qName)) {
            try {
                stm.setString(1, attrs.getValue("username"));
                stm.setTimestamp(2, Timestamp.valueOf(attrs.getValue("timestamp")));
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
        if (LoginTrackBinding.LOGINTRACKS.equals(qName)) {
            releaseResources();
        }
    }
}

