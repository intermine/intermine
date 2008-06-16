package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

public class LuceneIndex {
    private IndexWriter indexWriter = null;
    private List<LuceneObjectClass> lObjClass = null;
    private String fileName = null;
    
    public LuceneIndex(String fileName) {
        this.fileName = fileName;
        lObjClass = new Vector<LuceneObjectClass>();
    }

    public IndexWriter getIndexWriter(boolean create, String fileName) throws IOException {
        if (indexWriter == null) {
            indexWriter = new IndexWriter(fileName,
                                          new StandardAnalyzer(), create);
        }
        return indexWriter;
   }  

    public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.optimize();
            indexWriter.close();
            indexWriter = null;
        }
   }
    
    private void indexClass(LuceneObjectClass objClass) {
        try {
            IndexWriter writer = (IndexWriter) getIndexWriter(false, fileName);

            for (int i = 0; i < objClass.getSizeValues(); i++) {
                Document doc = new Document();
                for (int j = 0; j < objClass.getSizeFields(); j++) {
                    doc.add(new Field(objClass.getFieldName(j),
                            objClass.getValuesForField(objClass.getFieldName(j), i),
                            Field.Store.YES, Field.Index.TOKENIZED));
                }
                writer.addDocument(doc);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean addClass(LuceneObjectClass objClass) {
        if (!lObjClass.contains(objClass)) {
            lObjClass.add(objClass);
            return true;
        }
        return false;
    }
    
    public void rebuildClassIndexes() throws IOException {
        getIndexWriter(true, fileName);
        
        for (int i = 0; i < lObjClass.size(); i++) {
            indexClass(lObjClass.get(i));
        }
        
        closeIndexWriter();
    }
}
