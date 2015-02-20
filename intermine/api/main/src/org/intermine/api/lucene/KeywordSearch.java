package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.intermine.api.types.Producer;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;

/**
 * Allows for full-text searches over all metadata using the apache lucene
 * engine.
 *
 * Main entry point: contains methods for creating indices, restoring saved indices and
 * running searches over them.
 *
 * @author nils
 */

public final class KeywordSearch
{
    private static final String LUCENE_INDEX_DIR = "keyword_search_index";
    static final String CONFIG_FILE = "keyword_search.properties";
    private static final Logger LOG = Logger.getLogger(KeywordSearch.class);


    private KeywordSearch() {
        //don't
    }

    /**
     * Return an object which can search an object-store.
     * @param os The object store, which should contain a pre-built index.
     * @param dirPath The context path.
     * @return A browser.
     * @throws IOException If we can't read the configuration.
     */
    public static Browser getBrowser(ObjectStore os, String dirPath) throws IOException {
        return new Browser(getIndexProducer(os, dirPath), getConfig(os));
    }

    private static Producer<LuceneIndexContainer> getIndexProducer(
            final ObjectStore os, final String dirPath) {
        return new Producer<LuceneIndexContainer>() {
            @Override
            public LuceneIndexContainer produce() {
                return loadIndexFromDatabase(os, dirPath);
            }
        };
    }

    /**
     * writes index and associated directory to the database using the metadatamanager.
     * TODO - should not be squashing errors to RuntimeException!
     * @param os intermine objectstore
     * @param classKeys map of classname to key field descriptors (from InterMineAPI)
     */
    public static void createIndex(
            ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys) {

        try {
            Configuration config = getConfig(os);
            Indexer indexer = Indexer.getIndexer(os, classKeys, config);
            LuceneIndexContainer index = indexer.createIndex();
            indexer.saveIndex(index);
            deleteIndexDirectory(index, config);
        } catch (IOException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        } catch (SQLException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        }
    }

    private static Configuration getConfig(ObjectStore os) throws IOException {
        return new Configuration(os.getModel(), readProperties());
    }

    private static Properties readProperties() throws IOException {
        // load config file to figure out special classes
        String configFileName = KeywordSearch.CONFIG_FILE;
        ClassLoader classLoader = KeywordSearch.class.getClassLoader();
        InputStream configStream = classLoader.getResourceAsStream(configFileName);
        Properties options = new Properties();
        if (configStream == null) {
            LOG.error("keyword_search.properties: file '" + configFileName + "' not found!");
        } else {
            options.load(configStream);
        }
        return options;
    }


    private static LuceneIndexContainer loadIndexFromDatabase(ObjectStore os, String path) {
        LOG.debug("Attempting to restore search index from database...");
        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            LuceneIndexContainer ret = null;
            try {
                ret = restoreIndex(db);

                if (ret != null) {
                    String indexDirectoryType = ret.getDirectoryType();
                    Directory dir = restoreSearchDirectory(indexDirectoryType, path, db);
                    if (dir == null) {
                        LOG.error("Could not load directory");
                        return null;
                    }
                    ret.setDirectory(dir);
                    return ret;
                }

            } catch (ClassNotFoundException e) {
                LOG.error("Could not load search index", e);
            } catch (SQLException e) {
                LOG.error("Could not load search index", e);
            } catch (IOException e) {
                LOG.error("Could not load search index", e);
            }
        } else {
            LOG.error("ObjectStore is of wrong type!");
        }
        return null;
    }

    private static Directory restoreSearchDirectory(String dirType, String path, Database db)
        throws SQLException, IOException, FileNotFoundException, ClassNotFoundException {
        InputStream is;
        LOG.debug("Attempting to restore search directory from database...");
        is = MetadataManager.readLargeBinary(db, MetadataManager.SEARCH_INDEX_DIRECTORY);


        if (is != null) {
            try {
                if ("FSDirectory".equals(dirType)) {
                    return readFSDirectory(path, is);
                } else if ("RAMDirectory".equals(dirType)) {
                    return readRAMDirectory(is);
                } else {
                    LOG.warn("Unknown directory type specified: " + dirType);
                }
            } finally {
                is.close();
            }

        } else {
            LOG.warn("Could not find search directory!");
        }
        return null;
    }

    private static FSDirectory readFSDirectory(String path, InputStream is)
        throws IOException, FileNotFoundException {
        long time = System.currentTimeMillis();
        final int bufferSize = 2048;
        File directoryPath = new File(path + File.separator + LUCENE_INDEX_DIR);
        LOG.debug("Directory path: " + directoryPath);

        // make sure we start with a new index
        if (directoryPath.exists()) {
            String[] files = directoryPath.list();
            for (int i = 0; i < files.length; i++) {
                LOG.info("Deleting old file: " + files[i]);
                new File(directoryPath.getAbsolutePath() + File.separator
                        + files[i]).delete();
            }
        } else {
            directoryPath.mkdir();
        }

        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                LOG.info("Extracting: " + entry.getName() + " (" + entry.getSize()
                        + " MB)");

                FileOutputStream fos =
                        new FileOutputStream(directoryPath.getAbsolutePath()
                                + File.separator + entry.getName());
                BufferedOutputStream bos =
                        new BufferedOutputStream(fos, bufferSize);

                int count;
                byte[] data = new byte[bufferSize];

                try {
                    while ((count = zis.read(data, 0, bufferSize)) != -1) {
                        bos.write(data, 0, count);
                    }
                } finally {
                    bos.flush();
                    bos.close();
                }
            }
        } finally {
            zis.close();
        }

        FSDirectory directory = FSDirectory.open(directoryPath);

        LOG.info("Successfully restored FS directory from database in "
                + (System.currentTimeMillis() - time) + " ms");
        return directory;
    }

    private static RAMDirectory readRAMDirectory(InputStream is)
        throws IOException, ClassNotFoundException {
        long time = System.currentTimeMillis();
        GZIPInputStream gzipInput = new GZIPInputStream(is);
        ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

        try {
            Object object = objectInput.readObject();

            if (object instanceof FSDirectory) {
                RAMDirectory directory = (RAMDirectory) object;

                time = System.currentTimeMillis() - time;
                LOG.info("Successfully restored RAM directory"
                        + " from database in " + time + " ms");
                return directory;
            }
        } finally {
            objectInput.close();
            gzipInput.close();
        }
        return null;
    }

    private static LuceneIndexContainer restoreIndex(Database db)
        throws IOException, ClassNotFoundException, SQLException {

        long time = System.currentTimeMillis();
        InputStream is = MetadataManager.readLargeBinary(db, MetadataManager.SEARCH_INDEX);

        if (is == null) {
            LOG.warn("No search index stored in this DB.");
            return null;
        } else {
            GZIPInputStream gzipInput = new GZIPInputStream(is);
            ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

            try {
                Object object = objectInput.readObject();

                if (object instanceof LuceneIndexContainer) {

                    LOG.info("Successfully restored search index information"
                            + " from database in " + (System.currentTimeMillis() - time)
                            + " ms");
                    LOG.debug("Index: " + object);
                    return (LuceneIndexContainer) object;
                } else {
                    LOG.warn("Object from DB has wrong class:"
                            + object.getClass().getName());
                    return null;
                }
            } finally {
                objectInput.close();
                gzipInput.close();
                is.close();
            }
        }
    }


    private static boolean shouldDeleteIndex(File indexDir, Configuration config) {
        if (!indexDir.exists()) {
            LOG.warn("Index directory does not exist!");
            return false;
        }
        // Deletes by default - set delete.index = false for debugging.
        return config.shouldDelete();
    }

    /**
     * delete the directory used for the index (used in postprocessing)
     * @param index the Index to delete
     * @param config Information telling us whether to delete this or not.
     */
    public static void deleteIndexDirectory(
            LuceneIndexContainer index, Configuration config) {
        if (index != null && "FSDirectory".equals(index.getDirectoryType())) {
            File tempFile = ((FSDirectory) index.getDirectory()).getFile();

            if (shouldDeleteIndex(tempFile, config)) {
                LOG.debug("Deleting index directory: " + tempFile.getAbsolutePath());
                String[] files = tempFile.list();
                for (int i = 0; i < files.length; i++) {
                    LOG.debug("Deleting index file: " + files[i]);
                    new File(tempFile.getAbsolutePath() + File.separator + files[i]).delete();
                }
                tempFile.delete();
                LOG.info("Deleted index directory!");
            }
        }
    }

}
