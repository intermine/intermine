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

import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.api.tracker.TemplateTracker;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.util.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Code for reading and writing template tracks as XML.
 *
 * @author dbutano
 */
public final class TemplateTrackBinding
{
    private TemplateTrackBinding() {
    }

    private static final Logger LOG = Logger.getLogger(TemplateTrackBinding.class);
    public static final String TEMPLATETRACKS = "templatetracks";
    public static final String TEMPLATETRACK = "templatetrack";
    /**
     * Convert a TemplateTrack to XML and write XML to given writer.
     * @param uos the UserObjectStore
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ObjectStore uos, XMLStreamWriter writer) {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;

        try {
            writer.writeCharacters("\n");
            writer.writeStartElement(TEMPLATETRACKS);
            conn = ((ObjectStoreWriterInterMineImpl) uos).getConnection();
            String sql = "SELECT templatename, username, sessionidentifier,timestamp "
                + "FROM " + TrackerUtil.TEMPLATE_TRACKER_TABLE;
            stm = conn.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                writer.writeCharacters("\n");
                writer.writeStartElement(TEMPLATETRACK);
                writer.writeAttribute("templatename", rs.getString(1));
                String username = rs.getString(2);
                if (!StringUtils.isBlank(username)) {
                    writer.writeAttribute("username", username);
                }
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
            throw new RuntimeException("exception while marshalling template tracks", e);
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

    /**
     * Read the template tracks from an XML stream Reader.
     * @param reader contains the template tracks in XML format
     * @param uosw the objectstore for the userprofile database
     */
    public static void unmarshal(Reader reader, ObjectStoreWriter uosw) {
        try {
            TemplateTrackHandler ttHandler =
                new TemplateTrackHandler(uosw);
            SAXParser.parse(new InputSource(reader), ttHandler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

/**
 * Event handler for the templatetrack xml file
 * @author dbutano
 *
 */
class TemplateTrackHandler extends TrackHandler
{
    /**
     * Create a new TemplateTrackHandler
     * @param uosw an ObjectStoreWriter to the user profile database
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public TemplateTrackHandler(ObjectStoreWriter uosw) {
        super(uosw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (TemplateTrackBinding.TEMPLATETRACKS.equals(qName)) {
            try {
                connection = ((ObjectStoreWriterInterMineImpl) osw).getConnection();
                // Creating a template tracker will create an empty table if it doesn't exist
                TemplateTracker.getInstance(connection, new LinkedList<Track>());
                stm = connection.prepareStatement("INSERT INTO "
                      + TrackerUtil.TEMPLATE_TRACKER_TABLE + " VALUES(?, ?, ?, ?)");
            } catch (SQLException sqle) {
                new BuildException("Problem to retrieve the connection", sqle);
            }
        }
        if (TemplateTrackBinding.TEMPLATETRACK.equals(qName)) {
            try {
                stm.setString(1, attrs.getValue("templatename"));
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
        if (TemplateTrackBinding.TEMPLATETRACKS.equals(qName)) {
            releaseResources();
        }
    }
}
