package org.intermine.bio.chado;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * A factory for ChadoCV objects.
 * @author Kim Rutherford
 */
public class ChadoCVFactory
{
    private static final Logger LOG = Logger.getLogger(ChadoCVFactory.class);
    private final Connection connection;

    /**
     * Create a new ChadoCVFactory.
     * @param connection the connection to use for querying cvterms.
     */
    public ChadoCVFactory(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get a new ChadoCV containing only cv terms from the cv with the given name.
     * @param cvName name of controlled vocabulary, eg. sequence ontology
     * @return the new ChadoCV object
     * @throws SQLException if there is problem while querying
     */
    public ChadoCV getChadoCV(String cvName)
        throws SQLException {
        ChadoCV cv = new ChadoCV(cvName);

        ResultSet cvtermRes = getCVTermResultSet(connection, cvName);
        while (cvtermRes.next()) {
            Integer cvtermId = new Integer(cvtermRes.getInt("cvterm_id"));
            String cvtermName = cvtermRes.getString("cvterm_name");
            ChadoCVTerm cvTerm = new ChadoCVTerm(cvtermName);
            cv.addByChadoId(cvtermId, cvTerm);
        }

        ResultSet cvrelRes = getCVTermRelationshipResultSet(connection, cvName);
        while (cvrelRes.next()) {
            Integer subjectId = new Integer(cvrelRes.getInt("subject_id"));
            Integer objectId = new Integer(cvrelRes.getInt("object_id"));
            ChadoCVTerm subject = cv.getByChadoId(subjectId);
            ChadoCVTerm object = cv.getByChadoId(objectId);

            subject.getDirectParents().add(object);
            object.getDirectChildren().add(subject);
        }

        return cv;
    }

    /**
     * Return the rows from the cvterm_relationship table that relate cvterms from the cv with the
     * given name.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @param cvName the value of the name field to use when finding the cv
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getCVTermRelationshipResultSet(Connection connection, String cvName)
        throws SQLException {
        String query =
            "SELECT cvterm_rel.subject_id, cvterm_rel.object_id, rel_type.name"
            + "  FROM cvterm_relationship cvterm_rel, cvterm subject_cvterm,"
            + "       cvterm object_cvterm, cv cvterm_cv,"
            + "       cvterm rel_type"
            + " WHERE subject_cvterm.cv_id = cvterm_cv.cv_id"
            + "   AND object_cvterm.cv_id = cvterm_cv.cv_id"
            + "   AND cvterm_cv.name = ?"
            + "   AND cvterm_rel.subject_id = subject_cvterm.cvterm_id"
            + "   AND cvterm_rel.object_id = object_cvterm.cvterm_id"
            + "   AND cvterm_rel.type_id = rel_type.cvterm_id"
            + "   AND (rel_type.name = 'isa' OR rel_type.name = 'is_a')";
        LOG.info("executing: " + query);
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, cvName);
        ResultSet res = stmt.executeQuery();
        return res;
    }

    /**
     * Return the rows from the cvterm table that are from the cv with the given name.
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @param cvName the value of the name field to use when finding the cv
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getCVTermResultSet(Connection connection, String cvName)
        throws SQLException {
        String query =
            "SELECT cvterm.cvterm_id, cvterm.name as cvterm_name"
            + " FROM cvterm, cv WHERE cv.name = ?"
            + " AND cvterm.cv_id = cv.cv_id";
        LOG.info("executing: " + query);
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, cvName);
        ResultSet res = stmt.executeQuery();
        return res;
    }


}
