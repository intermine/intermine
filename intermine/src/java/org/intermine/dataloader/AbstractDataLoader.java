package org.flymine.dataloader;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.ConsistentSet;
import org.flymine.util.RelationType;
import org.flymine.util.TypeUtil;

/**
 * Loads information from a data source into the Flymine database.
 * This abstract class defines the store method, which can be used by the process method of
 * subclasses.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public abstract class AbstractDataLoader
{

   protected static final org.apache.log4j.Logger LOG
        = org.apache.log4j.Logger.getLogger(AbstractDataLoader.class);

    /**
     * Stores an object, with all of the objects referenced by it as skeletons.
     *
     * @param obj an object to store
     * @param iw an IntegrationWriter to write to
     * @throws ObjectStoreException if something goes wrong
     */
    public static void store(Object obj, IntegrationWriter iw) throws ObjectStoreException {
        store(obj, iw, new ConsistentSet(), false);
    }

    /**
     * Stores an object, with all of the objects referenced by it as skeletons.
     *
     * @param obj an object to store
     * @param iw an IntegrationWriter to write to
     * @param set a Set of objects that we are already dealing with
     * @param skeleton whether this object is a skeleton
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void store(Object obj, IntegrationWriter iw, Set set, boolean skeleton)
            throws ObjectStoreException {
        try {
            if (!set.contains(obj)) {
                set.add(obj);
                IntegrationDescriptor integ = iw.getByExample(obj);

                // if object was in database id needs to be set
                Field id = TypeUtil.getField(obj.getClass(), "id");
                if (integ.containsKey(id)) {
                    TypeUtil.setFieldValue(obj, "id", integ.get(id));
                }

                Map fieldToSetter = TypeUtil.getFieldToSetter(obj.getClass());
                Map fieldToGetter = TypeUtil.getFieldToGetter(obj.getClass());
                Iterator iter = fieldToSetter.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    Field field = (Field) entry.getKey();
                    Method setter = (Method) entry.getValue();
                    Method getter = (Method) fieldToGetter.get(field);
                    Object valueInObjectToStore = getter.invoke(obj, new Object[] {});

                    LOG.warn(obj.getClass() + ": " + field.getName() + " = "
                            + valueInObjectToStore);

                    int fieldType = iw.describeRelation(field);
                    if ((fieldType == RelationType.ONE_TO_N)
                        || (fieldType == RelationType.M_TO_N)) { // it's a collection

                        Collection objs = (Collection) valueInObjectToStore;
                        if (objs != null) {  // if any collection members in new object store them
                            Iterator objIter = objs.iterator();
                            while (objIter.hasNext()) {
                                Object subObj = objIter.next();
                                store(subObj, iw, set, true);
                            }
                        }
                    } else {
                        if ((fieldType == RelationType.ONE_TO_ONE)
                            || (fieldType == RelationType.N_TO_ONE)) { // it's an object reference

                            if (valueInObjectToStore != null) {
                                store(valueInObjectToStore, iw, set, true);
                            }
                        }
                        // A normal attribute, which should be set if the IntegrationDescriptor
                        // thinks so.
                        if (integ.containsKey(field)) {
                            Object oldValue = integ.get(field);
                            //if (fieldType == RelationType.ONE_TO_ONE) {
                                // TODO: set the reverse reference to null
                            //}
                            setter.invoke(obj, new Object[] {oldValue});
                        }
                    }
                }
                LOG.info("Storing object: " + obj);
                iw.store(obj);
            }
        } catch (IntrospectionException e) {
            throw new ObjectStoreException("Something horribly wrong with the model", e);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException("IllegalAccessException was thrown", e);
        } catch (InvocationTargetException e) {
            throw new ObjectStoreException("Something weird in java", e);
        }
    }
}
