package org.intermine.bio.dataconversion;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

/**
 * DataConverter to read from AnoEST's MySQL database into items
 * @author Kim Rutherford
 */
public class AnoESTConverter extends BioDBConverter
{
    private static final int ANOPHELES_TAXON_ID = 7165;
    private static final String DATASET_TITLE = "VectorBase AnoEST clusters";
    private static final String DATA_SOURCE_NAME = "VectorBase";
    private final Map<String, Item> clusters = new LinkedHashMap<String, Item>();
    private final Map<String, Item> ests = new LinkedHashMap<String, Item>();
    private final Map<String, String> cloneIds = new LinkedHashMap<String, String>();

    /**
     * Create a new AnoESTConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public AnoESTConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Process the data from the Database and write to the ItemWriter.
     * {@inheritDoc}
     */
    @Override
    public void process() throws Exception {
        Connection connection;
        if (getDatabase() == null) {
            // no Database when testing and no connectio needed
            connection = null;
        } else {
            connection = getDatabase().getConnection();
        }
        makeClusterItems(connection);
        makeEstItems(connection);
    }

    private void makeClusterItems(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getClusterResultSet(connection);

        while (res.next()) {
            String identifier = res.getString(1);
            String chromosomeIdentifier = res.getString(2);
            int start = res.getInt(3);
            int end = res.getInt(4);
            int strand = res.getInt(5);

            Item cluster = createItem("OverlappingESTSet");
            cluster.setAttribute("primaryIdentifier", identifier);
            cluster.setReference("organism", getOrganismItem(ANOPHELES_TAXON_ID));
            store(cluster);

            // some clusters have no location
            if (chromosomeIdentifier != null && !("mitochondrial".equals(chromosomeIdentifier))
                            && start > 0 && end > 0) {
                Item chromosomeItem = getChromosome(chromosomeIdentifier, ANOPHELES_TAXON_ID);
                String chromosomeItemId = chromosomeItem.getIdentifier();
                Item location = makeLocation(chromosomeItemId, cluster.getIdentifier(), start, end,
                                             strand, ANOPHELES_TAXON_ID);
                store(location);
            }
            clusters.put(identifier, cluster);
        }
    }

    /**
     * Return the clusters from the AnoEST database.
     * This is a protected method so that it can be overriden for testing.
     * @param connection the AnoEST database connection
     * @throws SQLException if there is a problem while querying
     * @return the clusters
     */
    protected ResultSet getClusterResultSet(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        String query = "select id as i1, chr, "
               + "(select min(st) from stable_cluster_ids where id = i1 group by id), "
               + "(select max(nd) from stable_cluster_ids where id = i1 group by id), strand "
               + "from stable_cluster_ids group by id;";
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private void makeEstItems(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getEstResultSet(connection);
        while (res.next()) {
            String accession = res.getString(1);
            String clusterId = res.getString(2);
            String cloneId = res.getString(3);

            Item est = ests.get(accession);
            if (est == null) {
                est = createItem("EST");
                ests.put(accession, est);
                est.setAttribute("primaryIdentifier", accession);
                est.setReference("organism", getOrganismItem(ANOPHELES_TAXON_ID));
                cloneIds.put(accession, cloneId);
            }

            Item cluster = clusters.get(clusterId);
            if (cluster != null) {
                est.addToCollection("overlappingESTSets", cluster);
            }
        }

        for (Item item: ests.values()) {
            store(item);
        }
    }

    /**
     * Return the ESTs from the AnoEST database.
     * This is a protected method so that it can be overriden for testing
     * @param connection the AnoEST database connection
     * @throws SQLException if there is a problem while querying
     * @return the ESTs
     */
    protected ResultSet getEstResultSet(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        String query = "select acc, cl_id, clone from est_view order by acc;";
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(@SuppressWarnings("unused") int taxonId) {
        return DATASET_TITLE;
    }

}
