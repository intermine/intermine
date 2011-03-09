package org.intermine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.api.tracker.TemplateTracker;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.util.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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

    /**
     * Convert a Profile to XML and write XML to given writer.
     * @param uos the UserObjectStore
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ObjectStore uos, XMLStreamWriter writer) {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;

        try {
            writer.writeStartElement("templatetracks");
            conn = ((ObjectStoreWriterInterMineImpl) uos).getConnection();
            String sql = "SELECT templatename, username, timestamp, sessionidentifier "
                + "FROM templatetrack";
            stm = conn.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                writer.writeCharacters("\n");
                writer.writeStartElement("templatetrack");
                writer.writeAttribute("templatename", rs.getString(1));
                String username = rs.getString(2);
                if (!StringUtils.isBlank(username)) {
                    writer.writeAttribute("username", username);
                }
                writer.writeAttribute("timestamp", Long.toString(rs.getLong(3)));
                writer.writeAttribute("sessionidentifier", rs.getString(4));
                writer.writeEndElement();
            }
            writer.writeCharacters("\n");
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling template tracks", e);
        } catch (SQLException sqle) {
            throw new RuntimeException("exception while reading template tracks from user profile"
                                       + " database", sqle);
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
class TemplateTrackHandler extends DefaultHandler
{
    private ObjectStoreWriter osw;
    private Connection connection;
    private PreparedStatement stm;
    private static final Logger LOG = Logger.getLogger(TemplateTrackHandler.class);

    /**
     * Create a new TemplateTrackHandler
     * @param uosw an ObjectStoreWriter to the user profile database
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public TemplateTrackHandler(ObjectStoreWriter uosw) {
        super();
        this.osw = uosw;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("templatetracks".equals(qName)) {
            try {
                connection = ((ObjectStoreWriterInterMineImpl) osw).getConnection();
                    // Creating a template tracker will create an empty table if it doesn't exist
                    TemplateTracker templateTracker = TemplateTracker.getInstance(connection);
                stm = connection.prepareStatement("INSERT INTO templatetrack VALUES(?, ?, ?, ?)");
            } catch (SQLException sqle) {
                new BuildException("Problem to retrieve the connection", sqle);
            }
        }
        if ("templatetrack".equals(qName)) {
            try {
                stm.setString(1, attrs.getValue("templatename"));
                stm.setString(2, attrs.getValue("username"));
                stm.setLong(3, Long.parseLong(attrs.getValue("timestamp")));
                stm.setString(4, attrs.getValue("sessionidentifier"));
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
        if ("templatetracks".equals(qName)) {
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
}
