package org.intermine.bio.like;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.intermine.Coordinates;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.modelproduction.MetadataManager.LargeObjectOutputStream;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.apache.log4j.Logger;

/**
 * Stores object in the database.
 *
 * @author selma
 *
 */
public final class Storing
{

    private static final Logger LOG = Logger.getLogger(Storing.class);

    private Storing() {
        // Don't.
    }

    /**
     * Writes index and associated directory to the database using the metadatamanager.
     * For similarity rating matrices.
     *
     * @param os intermine objectstore
     * @param matrix one row of one precalculated matrix
     * @param aspectNumber save with this file name addition
     * @param geneId save with this file name addition
     */
    public static void saveNormMatToDatabase(ObjectStore os,
            Map<Coordinates, Integer> matrix, String aspectNumber, String geneId) {
        try {
            LOG.info("Deleting previous search index dirctory blob from db...");
            long startTime = System.currentTimeMillis();
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            boolean blobExisted = MetadataManager.deleteLargeBinary(db, aspectNumber
                    + MetadataManager.LIKE_SIMILARITY_MATRIX + geneId);
            if (blobExisted) {
                LOG.debug("Deleting previous search index blob from db took: "
                        + (System.currentTimeMillis() - startTime) + ".");
            } else {
                LOG.debug("No previous search index blob found in db");
            }

            LOG.debug("Saving matrix information to database");
            writeObjectToDB(os, aspectNumber + MetadataManager.LIKE_SIMILARITY_MATRIX
                    + geneId, matrix);
            LOG.debug("Successfully saved " + aspectNumber + MetadataManager.LIKE_SIMILARITY_MATRIX
                    + geneId + " to database.");

        } catch (IOException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        } catch (SQLException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        }
    }

    /**
     * Writes index and associated directory to the database using the metadatamanager.
     * For common items matrices.
     *
     * @param os InterMine object store
     * @param matrix : is actually a row of the matrix. This row is saved to the database
     * @param aspectNumber of the corresponding aspect to the row (matrix). Will be added to the
     * storing name of the row.
     * @param geneId of the corresponding gene to the row (matrix). Will be added to the
     * storing name of the row.
     */
    public static void saveCommonMatToDatabase(ObjectStore os,
            Map<Coordinates, ArrayList<Integer>> matrix, String aspectNumber, String geneId) {
        try {
            LOG.info("Deleting previous search index dirctory blob from db...");
            long startTime = System.currentTimeMillis();
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            boolean blobExisted = MetadataManager.deleteLargeBinary(db, aspectNumber
                    + MetadataManager.LIKE_COMMON_MATRIX + geneId);
            if (blobExisted) {
                LOG.debug("Deleting previous search index blob from db took: "
                        + (System.currentTimeMillis() - startTime) + ".");
            } else {
                LOG.debug("No previous search index blob found in db");
            }

            LOG.debug("Saving matrix information to database");
            writeObjectToDB(os, aspectNumber + MetadataManager.LIKE_COMMON_MATRIX
                    + geneId, matrix);
            LOG.debug("Successfully saved " + aspectNumber + MetadataManager.LIKE_COMMON_MATRIX
                    + geneId + " to database.");

        } catch (IOException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        } catch (SQLException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        }
    }

    /**
     *
     * @param os InterMine object store
     * @param key it is stored with this name
     * @param object the row, that will be stored in the database
     * @throws IOException
     * @throws SQLException
     */
    private static void writeObjectToDB(ObjectStore os, String key, Object object)
        throws SQLException, IOException {
        LOG.debug("Saving stream to database...");
        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
        LargeObjectOutputStream streamOut = MetadataManager.storeLargeBinary(db, key);

        GZIPOutputStream gzipStream = new GZIPOutputStream(new BufferedOutputStream(streamOut));
        ObjectOutputStream objectStream = new ObjectOutputStream(gzipStream);

        LOG.debug("GZipping and serializing object...");
        objectStream.writeObject(object);

        objectStream.flush();
        gzipStream.finish();
        gzipStream.flush();

        streamOut.close();
    }

}
