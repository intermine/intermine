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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.modelproduction.MetadataManager.LargeObjectOutputStream;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.util.ObjectPipe;

/**
 * A thing that knows how to create Lucene indices from object stores and then how to store
 * those indices into an object store.
 *
 * You will notice that these are two responsibilities - hence it follows that this class ought
 * to be split up.
 *
 * @author Alex Kalderimis
 *
 */
public final class Indexer
{

    private static final Logger LOG = Logger.getLogger(Indexer.class);
    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private Configuration config;

    /**
     * Return an initialised indexer.
     * @param os The object store we are loading data from
     * @param classKeys The class keys
     * @param config The options for this indexer
     * @return An indexer.
     */
    public static Indexer getIndexer(
            ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            Configuration config) {
        Indexer indexer = new Indexer(os, classKeys, config);
        return indexer;
    }

    private Indexer(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            Configuration config) {
        this.os = os;
        this.classKeys = classKeys;
        this.config = config;
    }

    /**
     * Create an index from the object store. This is a long running method.
     * @return An index.
     * @throws IOException if we have trouble communicating with the real world.
     */
    public LuceneIndexContainer createIndex() throws IOException {
        long time = System.currentTimeMillis();
        File tempFile = null;
        LOG.debug("Creating keyword search index...");

        LOG.debug("Indexing - Temp Dir: " + config.getTempDirectory());

        ObjectPipe<Document> indexingQueue = new ObjectPipe<Document>();

        LOG.debug("Starting fetcher thread...");
        InterMineObjectFetcher fetchThread = new InterMineObjectFetcher(
                os, classKeys, indexingQueue, config);
        fetchThread.start(); // Start fetching in the background.

        // index the docs queued by the fetcher
        LOG.debug("Preparing index...");
        LuceneIndexContainer index = new LuceneIndexContainer();
        try {
            tempFile = makeTempFile(config.getTempDirectory(), index);
        } catch (IOException e) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            LOG.warn(
                String.format("Failed to create temp directory %s, trying %s instead.",
                        config.getTempDirectory(), tmpDir),
                e
            );
            try {
                tempFile = makeTempFile(tmpDir, index);
            } catch (IOException ee) {
                LOG.warn("Failed to create temp directory in " + tmpDir, ee);
                throw ee;
            }
        }

        LOG.debug("Indexing directory: " + tempFile.getAbsolutePath());

        IndexWriter writer;
        writer = new IndexWriter(index.getDirectory(), new WhitespaceAnalyzer(), true,
                 IndexWriter.MaxFieldLength.UNLIMITED); //autocommit = false?
        writer.setMergeFactor(10); //10 default, higher values = more parts
        writer.setRAMBufferSizeMB(64); //flush to disk when docs take up X MB

        int indexed = 0;

        // loop and index while we still have fetchers running
        LOG.debug("Starting to index...");
        while (indexingQueue.hasNext()) {
            Document doc = indexingQueue.next();

            // nothing in the queue?
            if (doc != null) {
                try {
                    writer.addDocument(doc);
                    indexed++;
                } catch (IOException e) {
                    LOG.error("Failed to submit #" + doc.getFieldable("id") + " to the index",
                            e);
                }

                if (indexed % 10000 == 1) {
                    logIndexingUpdate(time, fetchThread, indexed);
                }
            }
        }
        if (fetchThread.getException() != null) {
            try {
                writer.close();
            } catch (Exception e) {
                LOG.error("Error closing writer while handling exception.", e);
            }
            throw new RuntimeException("Indexing failed.", fetchThread.getException());
        }
        index.getFieldNames().addAll(fetchThread.getFieldNames());
        LOG.debug("Indexing done, optimizing index files...");
        try {
            writer.optimize();
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while optimizing and closing IndexWriter", e);
        }

        logIndexingComplete(time, indexed);
        return index;
    }

    private void logIndexingComplete(long time, int indexed) {
        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info(String.format(
                "Indexing of %d documents finished in %02d:%02d.%03d minutes",
                indexed,
                ((int) Math.floor(seconds / 60)),
                seconds % 60,
                time % 1000));
    }

    private void logIndexingUpdate(long time, Thread fetchThread, int indexed) {
        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        long maxMem = Runtime.getRuntime().maxMemory() / 1024;
        double percentUsed = (freeMem * 1d) / (maxMem * 1d) * 100d;
        LOG.debug(String.format(
            "docs indexed=%s; thread=%s; docs/ms=%.2f; memory=%,.2f%% free (of %sk); time=%dms",
                indexed,
                fetchThread.getState(),
                (indexed * 1.0F / (System.currentTimeMillis() - time)),
                percentUsed,
                maxMem,
                System.currentTimeMillis() - time));
    }

    /**
     * Create a temp file and associate it with the lucene index.
     * @param tempDir
     * @param index
     * @return
     * @throws IOException
     */
    private static File makeTempFile(String tempDir, LuceneIndexContainer index)
            throws IOException {
        LOG.debug("Creating search index tmp dir: " + tempDir);
        File tempFile = File.createTempFile("search_index", "", new File(tempDir));
        if (!tempFile.delete()) {
            throw new IOException("Could not delete temp file");
        }

        index.setDirectory(FSDirectory.open(tempFile));
        index.setDirectoryType("FSDirectory");

        // make sure we start with a new index
        if (tempFile.exists()) {
            String[] files = tempFile.list();
            for (int i = 0; i < files.length; i++) {
                LOG.info("Deleting old file: " + files[i]);
                new File(tempFile.getAbsolutePath() + File.separator + files[i]).delete();
            }
        } else {
            tempFile.mkdir();
        }
        return tempFile;
    }

    /**
     *  Save an index into the DB.
     * @param index The index to store.
     * @throws IOException If we have issues doing I/O
     * @throws SQLException If we cannot write into the database.
     */
    public void saveIndex(LuceneIndexContainer index) throws IOException, SQLException {

        LOG.debug("Deleting previous search index dirctory blob from db...");
        long startTime = System.currentTimeMillis();
        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
        boolean blobExisted = MetadataManager.deleteLargeBinary(db,
                MetadataManager.SEARCH_INDEX);
        if (blobExisted) {
            LOG.debug("Deleting previous search index blob from db took: "
                    + (System.currentTimeMillis() - startTime) + ".");
        } else {
            LOG.debug("No previous search index blob found in db");
        }

        LOG.debug("Saving search index information to database...");
        writeObjectToDB(MetadataManager.SEARCH_INDEX, index);
        LOG.debug("Successfully saved search index information to database.");

        // if we have a FSDirectory we need to zip and save that separately
        if ("FSDirectory".equals(index.getDirectoryType())) {
            ZipOutputStream zipOut = null;
            final int bufferSize = 2048;
            try {
                LOG.debug("Zipping up FSDirectory...");

                LOG.debug("Deleting previous search index dirctory blob from db...");
                startTime = System.currentTimeMillis();
                blobExisted = MetadataManager.deleteLargeBinary(db,
                        MetadataManager.SEARCH_INDEX_DIRECTORY);
                if (blobExisted) {
                    LOG.debug("Deleting previous search index directory blob from db took: "
                            + (System.currentTimeMillis() - startTime) + ".");
                } else {
                    LOG.debug("No previous search index directory blob found in db");
                }
                LargeObjectOutputStream streamOut =
                        MetadataManager.storeLargeBinary(db,
                                MetadataManager.SEARCH_INDEX_DIRECTORY);

                zipOut = new ZipOutputStream(streamOut);

                byte[] data = new byte[bufferSize];

                // get a list of files from current directory
                File dir = ((FSDirectory) index.getDirectory()).getFile();
                String[] files = dir.list();

                for (int i = 0; i < files.length; i++) {
                    File file = new File(dir.getAbsolutePath() + File.separator + files[i]);
                    LOG.debug("Getting length of file: " + file.getName());
                    long fileLength = file.length();
                    LOG.debug("Zipping file: " + file.getName() + " (" + file.length() / 1024
                            / 1024 + " MB)");

                    FileInputStream fi = new FileInputStream(file);
                    BufferedInputStream fileInput = new BufferedInputStream(fi, bufferSize);

                    try {
                        ZipEntry entry = new ZipEntry(files[i]);
                        zipOut.putNextEntry(entry);

                        long total = fileLength / bufferSize;
                        long progress = 0;

                        int count;
                        while ((count = fileInput.read(data, 0, bufferSize)) != -1) {
                            zipOut.write(data, 0, count);
                            progress++;
                            if (progress % 1000 == 0) {
                                LOG.debug("Written " + progress + " of " + total
                                        + " batches for file: " + file.getName());
                            }
                        }
                    } finally {
                        LOG.debug("Closing file: " + file.getName() + "...");
                        fileInput.close();
                    }
                    LOG.debug("Finished storing file: " + file.getName());
                }
            } finally {
                if (zipOut != null) {
                    zipOut.close();
                }
            }
        } else if ("RAMDirectory".equals(index.getDirectoryType())) {
            LOG.debug("Saving RAM directory to database...");
            writeObjectToDB(MetadataManager.SEARCH_INDEX_DIRECTORY, index.getDirectory());
            LOG.debug("Successfully saved RAM directory to database.");
        }
    }

    private void writeObjectToDB(String key, Object object)
            throws IOException, SQLException {
        LOG.debug("Saving stream to database...");
        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();

        LargeObjectOutputStream streamOut = null;
        GZIPOutputStream gzipStream = null;
        ObjectOutputStream objectStream = null;

        try {
            streamOut = MetadataManager.storeLargeBinary(db, key);
            gzipStream = new GZIPOutputStream(new BufferedOutputStream(streamOut));
            objectStream = new ObjectOutputStream(gzipStream);

            LOG.debug("GZipping and serializing object...");
            objectStream.writeObject(object);
        } finally {
            if (objectStream != null) {
                objectStream.flush();
                objectStream.close();
            }
            if (gzipStream != null) {
                gzipStream.finish();
                gzipStream.flush();
                gzipStream.close();
            }
            if (streamOut != null) {
                streamOut.close();
            }
        }

    }
}
