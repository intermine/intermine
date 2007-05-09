package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.TypeUtil;
import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;

import org.apache.log4j.Logger;

/**
 * Class to read a source database and produce a data representation
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class DBRetriever extends DataConverter
{
    private static final Logger LOG = Logger.getLogger(DBRetriever.class);
    protected Connection c = null;
    protected Model model;
    protected Database db;
    protected DBReader reader;
    protected Map uniqueIdMap = new HashMap();
    protected Map uniqueRefIdMap = new HashMap();
    protected Map maxIdMap = new HashMap();
    protected Map indirectionMap = new HashMap();
    protected Map idsProvidedMap = new HashMap();
    protected Map idIsUniqueMap = new HashMap();

    protected long count = 0;
    protected long start, time, times[];

    protected Set excluded;

    /**
     * Constructor
     *
     * @param model the Model
     * @param db the Database
     * @param reader the DBReader used to retrieve Items
     * @param writer the ItemWriter used to handle the resultant Items
     * @param excludeList A list of Source Item Class names to skip
     */
    protected DBRetriever(
            Model model, Database db, DBReader reader, ItemWriter writer, String excludeList) {
        super(writer);
        this.model = model;
        this.db = db;
        this.reader = reader;
        if (excludeList != null) {
            excluded = new HashSet(Arrays.asList(excludeList.toLowerCase().split("\\s")));
        }
    }

    /**
     * Process representations of  all the instances of all the classes in the model present in a
     * database
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        // if source db table has a non-unique id need to create a unique identifier
        // references to the non-unique id will be pointed at an arbitrary unique
        // identifier for that group of items.  It is assumed that the group of items
        // will become one after data translation.

        try {
            c = db.getConnection();

            for (Iterator cldIter = model.getClassDescriptors().iterator();
                 cldIter.hasNext();) {
                ClassDescriptor cld = (ClassDescriptor) cldIter.next();
                if (!cld.getName().equals("org.intermine.model.InterMineObject")) {
                    if (idsProvided(cld) && !idIsUnique(cld)) {
                        buildUniqueIdMap(TypeUtil.unqualifiedName(cld.getName()));
                    }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        start = System.currentTimeMillis();
        time = start;
        times = new long[20];
        for (int i = 0; i < 20; i++) {
            times[i] = -1;
        }

        for (Iterator cldIter = model.getClassDescriptors().iterator(); cldIter.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            if (!cld.getName().equals("org.intermine.model.InterMineObject")) {
                processClassDescriptor(cld);
            }
        }
    }


    /**
     * Get an assigned unique id for an item, if the id provided is already
     * unique it will be returned unaltered.
     * @param id an id to get a unique id for
     * @return a different unique id or the original id
     */
    protected String getUniqueId(String id) {
        if (uniqueIdMap.containsKey(id)) {
            String newId = (String) ((Stack) uniqueIdMap.get(id)).pop();
            if (((Stack) uniqueIdMap.get(id)).empty()) {
                uniqueIdMap.remove(id);
            }
            return newId;
        }
        return id;
    }

    /**
     * Get an assigned unique id for a referenced item, if the id provided is already
     * unique it will be returned unaltered.
     * @param refId an id to get a unique id for
     * @return a different unique id or the original id
     */
    protected String getUniqueRefId(String refId) {
        if (uniqueRefIdMap.containsKey(refId)) {
            return (String) uniqueRefIdMap.get(refId);
        }
        return refId;
    }


    /**
     * Given class that has a non-unique id column in the source database create and store
     * sufficient unique ids that can later be retrieved from a map.
     * @param clsName name of class with a non-unique id
     * @throws SQLException if problem querying database
     */
    protected void buildUniqueIdMap(String clsName) throws SQLException {
        String idCol = clsName + "_id";
        Iterator iter = reader.execute("SELECT " + idCol + " FROM " + clsName).iterator();
        while (iter.hasNext()) {
            String clsId = alias(clsName) + "_" + ((Map) iter.next()).get(idCol);
            if (!uniqueIdMap.containsKey(clsId)) {
                uniqueIdMap.put(clsId, new Stack());
            }
            String newId = alias(clsName) + "_" + getNextTableId(clsName);
            ((Stack) uniqueIdMap.get(clsId)).push(newId);

            if (!uniqueRefIdMap.containsKey(clsId)) {
                uniqueRefIdMap.put(clsId, newId);
            }
        }
    }


    /**
     * Process a single class from the model
     * @param cld the metadata for the class
     * @throws Exception if an error occurs when reading or writing data
     */
    protected void processClassDescriptor(ClassDescriptor cld) throws Exception {
        try {
            String clsName = TypeUtil.unqualifiedName(cld.getName());

            if (isClassExcluded(clsName)) {
                LOG.error("CLASS WAS EXCLUDED:" + clsName);
                return;
            }

            c = db.getConnection();

            Iterator iter;

            boolean idsProvided = idsProvided(cld);
            boolean nonUniqueId = idsProvided && !idIsUnique(cld);
            if (!idsProvided || nonUniqueId) {
                iter = reader.execute("SELECT * FROM " + clsName).iterator();
            } else {
                iter = reader.sqlIterator("SELECT * FROM " + clsName, clsName + "_id", clsName);
            }

            int identifier = 0;
            while (iter.hasNext()) {
                Map row = (Map) iter.next();
                String clsId = idsProvided ? "" + row.get(clsName + "_id") : "" + (++identifier);
                String uniqueId = getUniqueId(alias(clsName) + "_" + clsId);
                Item item = getItem(cld, uniqueId, clsId, row);

                if (nonUniqueId) {
                    Attribute attr = new Attribute();
                    attr.setItem(item);
                    attr.setName("nonUniqueId");
                    attr.setValue(alias(clsName) + "_" + clsId);
                    item.addAttributes(attr);
                }
                writer.store(item);
                count++;
                if (count % 10000 == 0) {
                    long now = System.currentTimeMillis();
                    if (times[(int) ((count / 10000) % 20)] == -1) {
                        LOG.info("Processed " + count + " rows - running at "
                                + (600000000L / (now - time)) + " (avg "
                                + ((60000L * count) / (now - start))
                                + ") rows per minute -- now on "
                                + clsName);
                    } else {
                        LOG.info("Processed " + count + " rows - running at "
                                + (600000000L / (now - time)) + " (200000 avg "
                                + (12000000000L / (now - times[(int) ((count / 10000) % 20)]))
                                + ") (avg " + ((60000L * count) / (now - start))
                                + ") rows per minute -- now on " + clsName);
                    }
                    time = now;
                    times[(int) ((count / 10000) % 20)] = now;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Check whether this class has a single primary key column named className_id
     * @param cld the metadata for the class
     * @return whether such a column exists
     * @throws SQLException if an error occurs when accessing the database
     */
    protected boolean idsProvided(ClassDescriptor cld) throws SQLException {
        Boolean retval = (Boolean) idsProvidedMap.get(cld);
        if (retval == null) {
            String clsName = TypeUtil.unqualifiedName(cld.getName());
            ResultSet r = executeQuery(c, "SELECT * FROM " + clsName + " LIMIT 1");
            boolean idsProvided = false;
            ResultSetMetaData rsmd = r.getMetaData();
            for (int i = rsmd.getColumnCount(); i > 0; i--) { //cols start at 1
                if (rsmd.getColumnName(i).equals(clsName + "_id")) {
                    idsProvided = true;
                }
            }
            retval = (idsProvided ? Boolean.TRUE : Boolean.FALSE);
            idsProvidedMap.put(cld, retval);
        }
        return retval.booleanValue();
    }

    /**
     * Find out if the id column a given table in source database is unique.
     * @param cld class corresponding to table in source db
     * @return true if index is unique
     * @throws SQLException if problem querying database
     */
    protected boolean idIsUnique(ClassDescriptor cld) throws SQLException {
        Boolean retval = (Boolean) idIsUniqueMap.get(cld);
        if (retval == null) {
            String clsName = TypeUtil.unqualifiedName(cld.getName()).toLowerCase();
            String idCol = clsName + "_id";

            ResultSet rs = executeQuery(c, "SELECT " + idCol + ", COUNT(*) FROM " + clsName
                                        + " GROUP BY " + idCol + " HAVING COUNT(*) > 1");
            if (rs.next()) {
                retval = Boolean.FALSE;
            } else {
                retval = Boolean.TRUE;
            }
            idIsUniqueMap.put(cld, retval);
        }
        return retval.booleanValue();
    }

    /**
     * Use a ResultSet and some metadata for a Class and materialise an Item corresponding
     * to that class
     * @param cld metadata for the class
     * @param uniqueId the id to use as the basis for the Item's identifier
     * @param clsId id from the source database, will be the same as uniqueId if source db
     *              table has a uniqe index
     * @param row the Map from which to retrieve data
     * @return the Item that has been constructed
     * @throws SQLException if an error occurs when accessing the database
     */
    protected Item getItem(ClassDescriptor cld, String uniqueId, String clsId, Map row)
        throws SQLException {
        String clsName = TypeUtil.unqualifiedName(cld.getName());
        Item item = new Item();
        item.setIdentifier(uniqueId);
        item.setClassName(cld.getModel().getNameSpace() + clsName);
        item.setImplementations("");
        for (Iterator fdIter = cld.getFieldDescriptors().iterator(); fdIter.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) fdIter.next();
            String fieldName = fd.getName();
            if (fd.isAttribute()) {
                Attribute attr = new Attribute();
                attr.setItem(item);
                attr.setName(fieldName);
                Object value = row.get(fieldName);
                if (value != null) {
                    attr.setValue(TypeUtil.objectToString(value));
                    item.addAttributes(attr);
                }
            } else if (fd.isReference()) {
                Reference ref = new Reference();
                ref.setItem(item);
                ref.setName(fieldName);
                Object value = row.get(fieldName + "_id");
                if (value != null && !TypeUtil.objectToString(value).equals("0")) {
                    String refClsName = ((ReferenceDescriptor) fd).getReferencedClassDescriptor()
                        .getName();
                    if ("org.intermine.model.InterMineObject".equals(refClsName)) {
                        refClsName = ((String) row.get(fieldName + "_object_type")).toLowerCase();
                    } else {
                        refClsName = TypeUtil.unqualifiedName(refClsName);
                    }
                    ref.setRefId(getUniqueRefId(alias(refClsName) + "_"
                                                + TypeUtil.objectToString(value)));
                    item.addReferences(ref);
                }
            } else if (fd.isCollection() && (fd.relationType() == FieldDescriptor.M_N_RELATION)) {
                String sql, refClsName;
                if (fd.relationType() == FieldDescriptor.ONE_N_RELATION) {
                    ReferenceDescriptor rd =
                        ((ReferenceDescriptor) fd).getReverseReferenceDescriptor();
                    refClsName =  TypeUtil.unqualifiedName(rd.getClassDescriptor().getName());
                    sql = "SELECT " + refClsName + "_id FROM " + refClsName + " WHERE "
                        + rd.getName() + "_id = " + clsId;
                } else {
                    refClsName = TypeUtil.unqualifiedName(
                        ((ReferenceDescriptor) fd).getReferencedClassDescriptor().getName());
                    String tableName = findIndirectionTable(clsName, refClsName);
                    sql = "SELECT " + refClsName + "_id FROM " + tableName + " WHERE "
                        + clsName + "_id = " + clsId;
                }
                Iterator idSet = reader.execute(sql).iterator();
                ReferenceList refs = new ReferenceList();
                refs.setItem(item);
                refs.setName(fieldName);
                StringBuffer refIds = new StringBuffer();
                while (idSet.hasNext()) {
                    Map idRow = (Map) idSet.next();
                    refIds.append(getUniqueRefId(alias(refClsName) + "_"
                                                 + idRow.get(refClsName + "_id") + " "));
                }
                if (refIds.length() > 0) {
                    refs.setRefIds(refIds.toString());
                    item.addCollections(refs);
                }
            }
        }
        return item;
    }

    /**
     * Try to find the indirection table for a many-to-many relationship
     *
     * @param clsName the name of one of the classes
     * @param otherClsName the name of the other class
     * @return the name of the indirection table
     * @throws SQLException if an error occurs
     */
    protected String findIndirectionTable(String clsName, String otherClsName) throws SQLException {
        String tableName = clsName + "_" + otherClsName;
        String retval = (String) indirectionMap.get(tableName);
        if (retval == null) {
            retval = tableName;
            if (!DatabaseUtil.tableExists(c, retval)) {
                retval = otherClsName + "_" + clsName;
            }
            indirectionMap.put(tableName, retval);
        }
        return retval;
    }

    /**
     * Execute an SQL query on a specified Connection
     *
     * @param c the Connection
     * @param sql the statement to execute
     * @return a ResultSet
     * @throws SQLException if an error occurs
     */
    protected  ResultSet executeQuery(Connection c, String sql) throws SQLException {
        Statement s = c.createStatement();
        return s.executeQuery(sql);
    }


    /**
     * Generate a new table-context id for a given class, finds maximum in source db
     * and increments.
     * @param clsName class name corresponding to source db table
     * @return next id
     * @throws SQLException if problem querying db
     */
    protected String getNextTableId(String clsName) throws SQLException {
        if (!maxIdMap.containsKey(clsName)) {
            Iterator i = reader.execute("SELECT MAX(" + clsName + "_id) FROM "
                                        + clsName).iterator();
            String id = "" + (((Map) i.next()).get("MAX(" + clsName + "_id)"));
            maxIdMap.put(clsName, id);
        }

        String id = (String) maxIdMap.get(clsName);
        Integer newId = new Integer(Integer.parseInt(id) + 1);
        id = newId.toString();
        maxIdMap.put(clsName, id);
        return id;
    }


    /**
     * Checks to see if the current class should be excluded from the retrieve - possibly due to
     * out of memory problem.
     *
     * @param className A classname to check for exclusion.
     * @return a boolean indicating true if the class is to be excluded otherwise false.
     * */
    protected boolean isClassExcluded(String className) {

        if (excluded != null && className != null
                && excluded.contains(className.toLowerCase())) {
            return true;
        }
        return false;
    }
}
