package org.flymine.dataloader;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.TypeUtil;

/**
 * Simple implementation of IntegrationWriter - assumes that this is the only (or first) data source
 * to be written to the database.
 *
 * @author Matthew Wakeling
 */
public class IntegrationWriterSingleSourceImpl extends IntegrationWriterAbstractImpl
{

    protected Set nonSkeletons;

    /**
     * Constructs a new instance of IntegrationWriterSingleSourceImpl.
     *
     * @param dataSource the name of the data source. This value is ignored by this class
     * @param osWriter an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterSingleSourceImpl(String dataSource, ObjectStoreWriter osWriter) {
        this.dataSource = dataSource;
        this.osw = osWriter;
        nonSkeletons = new HashSet();
    }

    /**
     * Retrieves an object from the database to match the given object, by primary key, and builds
     * an IntegrationDescriptor for instructions on how to modify the original object.
     *
     * @param obj the object to search for in the database
     * @return details of object in database and which fields canbe overwritten
     * @throws ObjectStoreException if error occurs finding object
     */
    public IntegrationDescriptor getByExample(Object obj) throws ObjectStoreException {
        Object dbObj = osw.getObjectByExample(obj);
        IntegrationDescriptor retval = new IntegrationDescriptor();

        if (dbObj != null) {
            try {
                Class cls = obj.getClass();
                retval.put(cls.getDeclaredField("id"),
                           cls.getMethod("getId", new Class[] {}).invoke(dbObj, new Object[] {}));

                if (nonSkeletons.contains(dbObj)) {
                    // This data was written by us in the past. Therefore, the database version
                    // overrides everything, except collections.
                    Map fieldToGetter = TypeUtil.getFieldToGetter(obj.getClass());
                    Iterator iter = fieldToGetter.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        Field field = (Field) entry.getKey();
                        int fieldType = describeRelation(field);

                        Method method = (Method) entry.getValue();
                        Object value = method.invoke(obj, new Object[] {});
                        retval.put(field, value);
                    }
                }
                //        } else {
                // This data was not written for real in the past by us. Therefore, we do not
                // need to fill in anything (except id) in the return value.
                //        }
            } catch (IntrospectionException e) {
                throw new ObjectStoreException("Something horribly wrong with the model", e);
            } catch (NoSuchFieldException e) {
                throw new ObjectStoreException("Something even worse wrong with the model", e);
            } catch (NoSuchMethodException e) {
                throw new ObjectStoreException("Something nasty with the model", e);
            } catch (IllegalAccessException e) {
                throw new ObjectStoreException("Something upset in java", e);
            } catch (InvocationTargetException e) {
                throw new ObjectStoreException("Something weird in java", e);
            }
        }
        return retval;
    }

    /**
     * Stores an object into the database, either for real or as a skeleton.
     * Collections in the object are merged with collections in any version of the object already in
     * the database. The operation sets the reverse object reference on one-to-many collections.
     *
     * @param obj the object to store
     * @param skeleton whether the object is a skeleton
     * @throws ObjectStoreException if anything goes wrong during store
     */
    public void store(Object obj, boolean skeleton) throws ObjectStoreException {
        // Here, we are assuming that the store(Object) method sets the ID in the object.
        store(obj);
        if (!skeleton) {
            nonSkeletons.add(obj);
        }
    }
}
