package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Creates the indexes for LuceneObjectClasses
 * @author Dominik Grimm
 */
public class LuceneIndex
{
    private IndexWriter indexWriter = null;
    private List<LuceneObjectClass> lObjClass = null;
    private String fileName = null;

    /**
     * Constructor
     * @param fileName of the luceneIndex files
     */
    public LuceneIndex(String fileName) {
        this.fileName = fileName;
        lObjClass = new Vector<LuceneObjectClass>();
    }

    /**
     * returns the index writer
     * @param create flag for the IndexWriter
     * @param fn which the IndexWriter should use
     * @return current indexWriter
     * @throws IOException IOException
     */
    public IndexWriter getIndexWriter(boolean create, String fn) throws IOException {
        if (indexWriter == null) {
            indexWriter = new IndexWriter(FSDirectory.open(new File(fn)),
                    new StandardAnalyzer(Version.LUCENE_30), create,
                    IndexWriter.MaxFieldLength.UNLIMITED);
        }
        return indexWriter;
    }

    /**
     * close the current indexWriter
     * @throws IOException IOException
     */
    public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.optimize();
            indexWriter.close();
            indexWriter = null;
        }
    }

    private void indexClass(LuceneObjectClass objClass) {
        try {
            IndexWriter writer = getIndexWriter(false, fileName);

            for (int i = 0; i < objClass.getSizeValues(); i++) {
                Document doc = new Document();
                for (int j = 0; j < objClass.getSizeFields(); j++) {
                    doc.add(new Field(objClass.getFieldName(j),
                            objClass.getValuesForField(objClass.getFieldName(j), i),
                            Field.Store.YES, Field.Index.ANALYZED));
                }
                writer.addDocument(doc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * added a new LuceneObjectClass to internal map
     * @param objClass LuceneObjectClass which contains the data for the specific field
     * @return true if adding to the map was successful else objectclass is already added
     */
    public boolean addClass(LuceneObjectClass objClass) {
        if (!lObjClass.contains(objClass)) {
            lObjClass.add(objClass);
            return true;
        }
        return false;
    }
    /**
     * rebuild all indexes from LuceneObjectClasses in the map
     * @throws IOException IOException
     */
    public void rebuildClassIndexes() throws IOException {
        getIndexWriter(true, fileName);

        for (int i = 0; i < lObjClass.size(); i++) {
            indexClass(lObjClass.get(i));
        }

        closeIndexWriter();
    }
}
