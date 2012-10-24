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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Autocompleter class for initializing and using the autocompletion
 *
 * @author Dominik Grimm
 * @author Michael Menden
 */
public class AutoCompleter
{
    private  HashMap<String, String> fieldIndexMap = new HashMap<String, String>();
    private HashMap<String, LuceneSearchEngine> ramIndexMap =
                                        new HashMap<String, LuceneSearchEngine>();
    private HashMap<String, RAMDirectory> blobMap = new HashMap<String, RAMDirectory>();
    private Properties prob;
    private LuceneSearchEngine search = null;

    private static final File TEMP_DIR =
        new File("build" + File.separatorChar + "autocompleteIndexes");

    private static final Logger LOG = Logger.getLogger(AutoCompleter.class);

    /**
     * Autocompleter standard constructor.
     */
    public AutoCompleter() {
        // empty
    }

    /**
     * Autocompleter build index constructor.
     * @param os Objectstore
     * @param prob Properties
     */
    public AutoCompleter(ObjectStore os, Properties prob) {
        this.prob = prob;
        try {
            buildIndex(os);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Autocompleter rebuild constructor.
     * @param blobInput InputStream from database
     */
    @SuppressWarnings("unchecked")
    public AutoCompleter(InputStream blobInput) {
        try {
            ObjectInputStream objectInput = new ObjectInputStream(blobInput);

            Object object = objectInput.readObject();

            blobInput.close();

            if (object instanceof HashMap<?, ?>) {
                blobMap = (HashMap<String, RAMDirectory>) object;

                for (Iterator<Map.Entry<String, RAMDirectory>> iter = blobMap.entrySet().iterator();
                        iter.hasNext();) {
                    Map.Entry<String, RAMDirectory> entry =
                        iter.next();
                    String key = entry.getKey();
                    RAMDirectory value = null;
                    value = entry.getValue();
                    search = null;
                    search = new LuceneSearchEngine(value);
                    ramIndexMap.put(key, search);
                    fieldIndexMap.put(key, key);
                    LOG.info("AutoCompleter read index for: " + key);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * create the RAMIndex for the search engine
     * @param classDes String of the class and the field (e.g. GOTerm.name)
     */
    public void createRAMIndex(String classDes) {
        if (ramIndexMap.get(classDes) != null) {
            search = null;
            search = ramIndexMap.get(classDes);
        } else {
            try {
                String indexFile = TEMP_DIR.toString() + File.separatorChar + classDes;
                RAMDirectory ram = new RAMDirectory(FSDirectory.open(new File(indexFile)));
                search = new LuceneSearchEngine(ram);
                ramIndexMap.put(classDes, search);
                blobMap.put(classDes, ram);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * returns a string array with the search results of the query in the field
     * @param query is the string used for search
     * @param field is the field in which you like to search (e.g. name)
     * @return stringList string array with the whole search results including
     *           an error flag at position 0
     */
    public String[] getList(String query, String field) {
        String[] stringList = null;
        String status = "true";
        int counter = 1;

        TopDocs topDocs = null;
        try {
            topDocs = search.performSearch(query, field);
        } catch (IOException e) {

        } catch (ParseException e) {
            status = "Please type in more characters to get results.";
        }

        if (topDocs != null) {
            stringList = new String[topDocs.totalHits + 1];

            for (int i = 0; i < topDocs.totalHits; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc;
                try {
                    doc = search.getIndexSearch().doc(scoreDoc.doc);

                    stringList[counter] = doc.get(field);
                    counter++;
                } catch (IOException e) {
                    //TODO: shouldn't this go outside the for loop?
                    status = "No results! Please try again.";
                }
            }
        }
        stringList[0] = status;

        return stringList;
    }

    /**
     * Returns n search results
     * @param query is the string used for search
     * @param field is the field in which you like to search (e.g. name)
     * @param n number of the first n search results
     * @return string array with search results and an error flag at position 0
     */
    public String[] getFastList(String query, String field, int n) {
        return search.fastSearch(query, field, n);
    }

    /**
     * Build the index from the database blob
     * @param os Objectstore
     * @throws IOException IOException
     * @throws ObjectStoreException ObjectStoreException
     * @throws ClassNotFoundException ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public void buildIndex(ObjectStore os)
        throws IOException, ObjectStoreException, ClassNotFoundException {

        if (TEMP_DIR.exists()) {
            if (!TEMP_DIR.isDirectory()) {
                throw new RuntimeException(TEMP_DIR + " exists but isn't a directory - remove it");
            }
        } else {
            TEMP_DIR.mkdirs();
        }

        for (Map.Entry<Object, Object> entry: prob.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.endsWith(".autocomplete")) {
                continue;
            }
            String className = key.substring(0, key.lastIndexOf("."));
            ClassDescriptor cld = os.getModel().getClassDescriptorByName(className);
            if (cld == null) {
                throw new RuntimeException("a class mentioned in ObjectStore summary properties "
                                           + "file (" + className + ") is not in the model");
            }
            List<String> fieldNames = Arrays.asList(value.split(" "));
            for (Iterator<String> i = fieldNames.iterator(); i.hasNext();) {

                String fieldName = i.next();
                String classAndField = cld.getUnqualifiedName() + "." + fieldName;
                System.out .println("Indexing " + classAndField);
                fieldIndexMap.put(classAndField, classAndField);


                Query q = new Query();
                q.setDistinct(true);
                QueryClass qc = new QueryClass(Class.forName(cld.getName()));
                q.addToSelect(new QueryField(qc, fieldName));
                q.addFrom(qc);
                Results results = os.execute(q);

                LuceneObjectClass objectClass = new LuceneObjectClass(classAndField);
                objectClass.addField(fieldName);

                for (Object resRow: results) {
                    Object fieldValue = ((ResultsRow) resRow).get(0);
                    if (fieldValue != null) {
                        objectClass.addValueToField(objectClass.getFieldName(0), fieldValue
                                .toString());
                    }
                }

                String indexFileName = TEMP_DIR.getPath() + File.separatorChar + classAndField;
                LuceneIndex indexer = new LuceneIndex(indexFileName);
                indexer.addClass(objectClass);
                indexer.rebuildClassIndexes();

                createRAMIndex(classAndField);
            }
        }
    }

    /**
     * Returns byte array of the RAMIndexMap
     * @return Returns byte array of the RAMIndexMap
     * @throws IOException IOException
     */
    public byte[] getBinaryIndexMap() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(blobMap);
        objectStream.close();

        return byteStream.toByteArray();
    }

    /**
     * checks if an autocompletin exists
     * @param type classname
     * @param field fieldname
     * @return boolean true if an autocompletion exists
     */
    public boolean hasAutocompleter(String type, String field) {
        if (fieldIndexMap.get(type + "." + field) != null) {
            return true;
        }
        return false;
    }


}
