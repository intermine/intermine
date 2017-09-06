package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.util.XmlBinding;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility functions for creating ObjectStore test infrastructure.
 */
public class ObjectStoreTestUtils {

    /**
     * Get InterMine Item objects from the given item XML without persitently storing them.
     *
     * @param modelName
     * @param itemsXmlFilename
     * @return
     * @throws Exception
     */
    public static Map getTestData(String modelName, String itemsXmlFilename) throws Exception {
        Model model = Model.getInstanceByName(modelName);
        Collection items = ObjectStoreTestUtils.loadItemsFromXml(model, itemsXmlFilename);
        ObjectStoreTestUtils.setIdsOnItems(items);
        return ObjectStoreTestUtils.mapItemsToNames(items);
    }

    public static Collection loadItemsFromXml(Model model, String resourceName) throws Exception {
        XmlBinding binding = new XmlBinding(model);
        return binding.unmarshal(SetupDataTestCase.class.getClassLoader().getResourceAsStream(resourceName));
    }

    public static void setIdsOnItems(Collection c) throws Exception {
        int i=1;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            try {
                DynamicUtil.setFieldValue(iter.next(), "id", new Integer(i++));
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public static Map mapItemsToNames(Collection c) throws Exception {
        Map returnData = new LinkedHashMap();
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            returnData.put(simpleObjectToName(o), o);
        }
        return returnData;
    }

    public static Object objectToName(Object o) throws Exception {
        if (o instanceof Collection) {
            StringBuffer sb = new StringBuffer();
            boolean needComma = false;
            sb.append("[");
            for (Object p : ((Collection) o)) {
                if (needComma) {
                    sb.append(", ");
                }
                needComma = true;
                sb.append(objectToName(p));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return simpleObjectToName(o);
        }
    }

    private static Object simpleObjectToName(Object o) throws Exception {
        Method name = null;
        try {
            name = o.getClass().getMethod("getName", new Class[] {});
        } catch (Exception e) {
            try {
                name = o.getClass().getMethod("getAddress", new Class[] {});
            } catch (Exception e2) {
            }
        }
        if (name != null) {
            return name.invoke(o, new Object[] {});
        } else if (o instanceof InterMineObject) {
            return new Integer(o.hashCode());
        } else {
            return o;
        }
    }

    public static List queryResultsToNames(List res) throws Exception {
        List aNames = new ArrayList();
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            List row = (List) resIter.next();
            List toRow = new ArrayList();
            Iterator rowIter = row.iterator();
            while (rowIter.hasNext()) {
                Object o = rowIter.next();
                if (o instanceof List) {
                    List newO = new ArrayList();
                    for (Object p : ((List) o)) {
                        newO.add(objectToName(p));
                    }
                    toRow.add(newO);
                } else {
                    toRow.add(objectToName(o));
                }
            }
            aNames.add(toRow);
        }
        return aNames;
    }

    public static void storeData(ObjectStoreWriter dataWriter, Map data) throws Exception {
        //checkIsEmpty();
        System.out.println("Storing data");
        long start = new Date().getTime();
        try {
            //Iterator iter = data.entrySet().iterator();
            //while (iter.hasNext()) {
            //    InterMineObject o = (InterMineObject) ((Map.Entry) iter.next())
            //        .getValue();
            //    o.setId(null);
            //}
            dataWriter.beginTransaction();
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object o = entry.getValue();
                dataWriter.store(o);
            }
            dataWriter.commitTransaction();
        } catch (Exception e) {
            dataWriter.abortTransaction();
            throw new Exception(e);
        }

        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data");
    }

    /**
     * Delete all objects in the objectstore that have the given class.
     *
     * @param writer
     * @param clazz
     * @returns Number of objects deleted.
     * @throws Exception
     */
    public static int deleteAllObjectsInClass(ObjectStoreWriter writer, Class clazz) throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(clazz);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = writer.getObjectStore().executeSingleton(q);

        int count = res.size();

        for (Object o : res) {
            writer.delete(((InterMineObject)o));
        }

        return count;
    }

    /**
     * Delete all objects in the given objectstore
     *
     * @param writer
     * @throws Exception
     */
    public static void deleteAllObjectsInStore(ObjectStoreWriter writer) throws Exception {
        System.out.println("Removing data from store");
        long start = new Date().getTime();
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to remove data");
        }

        // FIXME: This is a hack to avoid an Exception if we try and iterate through flatmode objects (even before
        // deletion).  This appears to be an InterMine bug that needs to be fixed, but in the meantime we simply
        // won't clean up these objects (which is not currently causing a problem.
        // See https://gist.github.com/justinccdev/242bb0c4f35eb0b0a1601e0c8844e90c for the failure
        ObjectStoreInterMineImpl osimi = (ObjectStoreInterMineImpl)writer;
        if (osimi.getSchema().getMissingTables().contains("intermineobject")) {
            System.out.println("Skipping deletion of all objects in objectstore because intermineobject table is missing");
            return;
        }

        try {
            writer.beginTransaction();
            Query q = new Query();
            QueryClass qc = new QueryClass(InterMineObject.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            SingletonResults dataToRemove = writer.getObjectStore().executeSingleton(q);
            Iterator<Object> iter = dataToRemove.iterator();
            while (iter.hasNext()) {
                InterMineObject toDelete = (InterMineObject)iter.next();
                writer.delete(toDelete);
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw e;
        }

        System.out.println("Took " + (new Date().getTime() - start) + " ms to remove data from store");
    }

    public static List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
    }
}
