package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;

/**
 * An object that represents the mapping of a Model onto a relational database. This handles the
 * complications of truncated class trees.
 *
 * @author Matthew Wakeling
 */
public class DatabaseSchema
{
    private static final Logger LOG = Logger.getLogger(DatabaseSchema.class);

    private Model model;
    private List<ClassDescriptor> truncated;
    private boolean noNotXml;
    private boolean flatMode;
    private Set<String> missingTables;
    private boolean fetchFromInterMineObject;
    private int version;
    protected boolean hasBioSeg;
    protected boolean useRangeTypes;

    private Set<ClassDescriptor> truncatedSet;
    private Map<ClassDescriptor, Fields> tableMasterToFieldDescriptors
        = new IdentityHashMap<ClassDescriptor, Fields>();
    private Map<ClassDescriptor, ClassDescriptor> classDescriptorToTableClassDescriptor
        = new IdentityHashMap<ClassDescriptor, ClassDescriptor>();

    /**
     * Returns an instance of DatabaseSchema, for the given Model and and List of truncated classes.
     *
     * @param model a Model
     * @param truncated a List of ClassDescriptors representing the truncated classes, in order of
     * decreasing priority.
     * @param noNotXml true if NotXML data should be omitted from every table except InterMineObject
     * @param missingTables a Set of lowercase table names which are missing
     * @param version the version number in the database
     * @param hasBioSeg true if the database has the bioseg type installed
     * @param useRangeTypes true if we can use Postgres built-in range types
     * @throws IllegalArgumentException if the truncated class list does not make sense
     */
    public DatabaseSchema(Model model, List<ClassDescriptor> truncated, boolean noNotXml,
            Set<String> missingTables, int version, boolean hasBioSeg, boolean useRangeTypes) {
        this.model = model;
        this.truncated = truncated;
        this.missingTables = missingTables;
        this.noNotXml = noNotXml && (!missingTables.contains("intermineobject"));
        this.flatMode = noNotXml && missingTables.contains("intermineobject");
        this.fetchFromInterMineObject = !missingTables.contains("intermineobject");
        this.version = version;
        this.hasBioSeg = hasBioSeg;
        this.useRangeTypes = useRangeTypes;
        for (int i = 0; i < truncated.size(); i++) {
            Class<?> cA = truncated.get(i).getType();
            for (int o = 0; o < i; o++) {
                Class<?> cB = truncated.get(o).getType();
                if (cB.isAssignableFrom(cA)) {
                    throw new IllegalArgumentException("Truncated class " + cB.getName()
                            + " is completely overridden by truncated class " + cA.getName());
                }
            }
        }
        truncatedSet = new HashSet<ClassDescriptor>(truncated);
    }

    /**
     * Returns the name of the table in which to store a row for the given ClassDescriptor.
     *
     * @param cld the ClassDescriptor from the Model
     * @return the ClassDescriptor that masters the table
     */
    public synchronized ClassDescriptor getTableMaster(ClassDescriptor cld) {
        ClassDescriptor retval = classDescriptorToTableClassDescriptor.get(cld);
        if (retval == null) {
            Iterator<ClassDescriptor> truncIter = truncated.iterator();
            while (truncIter.hasNext() && (retval == null)) {
                ClassDescriptor truncCld = truncIter.next();
                if (truncCld.getType().isAssignableFrom(cld.getType())) {
                    retval = truncCld;
                }
            }
            if (retval == null) {
                retval = cld;
            }
            classDescriptorToTableClassDescriptor.put(cld, retval);
        }
        return retval;
    }

    /**
     * Returns true if a query using the given table-mastering ClassDescriptor would require a
     * constraint on the className field. It is assumed that the ClassDescriptor passed in here is a
     * ClassDescriptor that could be returned by the getTableMaster() method.
     *
     * @param cld the ClassDescriptor
     * @return a boolean
     */
    public boolean isTruncated(ClassDescriptor cld) {
        return truncatedSet.contains(cld);
    }

    /**
     * Returns the model.
     *
     * @return the model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Returns true if NotXML should be omitted from all tables except the InterMineObject table,
     * and the InterMineObject table is present.
     *
     * @return a boolean
     */
    public boolean isMissingNotXml() {
        return noNotXml;
    }

    /**
     * Returns true if the ObjectStore needs to run in flat mode for this Class - if
     * notXml is missing and the InterMineObject table is missing (or if the class is not a
     * subclass of InterMineObject).
     *
     * @param c a Class
     * @return a boolean
     */
    public boolean isFlatMode(Class<?> c) {
        return flatMode || (!InterMineObject.class.isAssignableFrom(c));
    }

    /**
     * Returns true if ProxyReferences can be fetched from the InterMineObject table.
     *
     * @return a boolean
     */
    public boolean isFetchFromInterMineObject() {
        return fetchFromInterMineObject;
    }

    /**
     * Returns the database format version.
     *
     * @return an int
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the Set of table names which are tables missing from the database.
     *
     * @return a Set of lowercase Strings
     */
    public Set<String> getMissingTables() {
        return missingTables;
    }

    /**
     * Returns true if the database has the bioseg type installed.
     *
     * @return a boolean
     */
    public boolean hasBioSeg() {
        return hasBioSeg;
    }

    /**
     * Returns true if we can use Postgres built-in range types on this database. Will return false
     * if this isn't a Postgres database or is earlier than 9.2.
     * @return true if we can use built in range types
     */
    public boolean useRangeTypes() {
        return useRangeTypes;
    }



    /**
     * Returns a Fields object of FieldDescriptors in the given table-mastering ClassDescriptor. It
     * is assumed that the ClassDescriptor passed in here is a ClassDescriptor that could be
     * returned as a of the getTableMaster() method. Where multiple subclasses of a truncated class
     * have a similarly-named field with the same type, one of the FieldDescriptors will be chosen
     * at random. CollectionDescriptors are not included in the result of this method.
     *
     * @param cld the ClassDescriptor
     * @return a Fields object containing AttributeDescriptors and ReferenceDescriptors
     * @throws ObjectStoreException if two similar-named fields are found of different types
     */
    public synchronized Fields getTableFields(ClassDescriptor cld) throws ObjectStoreException {
        Fields retval = tableMasterToFieldDescriptors.get(cld);
        if (retval == null) {
            if (isTruncated(cld)) {
                Map<String, AttributeDescriptor> attributeMap
                    = new HashMap<String, AttributeDescriptor>();
                Map<String, ReferenceDescriptor> referenceMap
                    = new HashMap<String, ReferenceDescriptor>();
                Set<ClassDescriptor> added = new HashSet<ClassDescriptor>();
                Stack<ClassDescriptor> todo = new Stack<ClassDescriptor>();
                todo.push(cld);
                added.add(cld);
                while (!todo.empty()) {
                    ClassDescriptor subCld = todo.pop();
                    for (ClassDescriptor subSubCld : subCld.getSubDescriptors()) {
                        if (!added.contains(subSubCld)) {
                            todo.push(subSubCld);
                            added.add(subSubCld);
                        }
                    }
                }
                for (ClassDescriptor subCld : added) {
                    // Nasty - one truncated class can have priority over another, or even be a
                    // proper subset of another. We check this here, as not all subclasses of
                    // the table master are necessarily mapped onto the same table.
                    ClassDescriptor subsMaster = getTableMaster(subCld);
                    if ((subsMaster == cld) || isFlatMode(cld.getType())) {
                        // This class does map onto this table. We need to look at all the
                        // FieldDescriptors of this class, but we can give a small warning if this
                        // results in fields from classes that are not a subclass of the table
                        // master being included.
                        for (FieldDescriptor field : subCld.getAllFieldDescriptors()) {
                            if (field instanceof AttributeDescriptor) {
                                AttributeDescriptor origField = attributeMap.get(field.getName());
                                if (origField == null) {
                                    attributeMap.put(field.getName(), (AttributeDescriptor) field);
                                } else if (origField != field) {
                                    String type = ((AttributeDescriptor) field).getType();
                                    String origType = origField.getType();
                                    if (!compatible(type, origType)) {
                                        throw new ObjectStoreException("Fields "
                                                + field.getClassDescriptor().getName() + "."
                                                + field.getName() + " (a " + type + ") and "
                                                + origField.getClassDescriptor().getName() + "."
                                                + origField.getName() + " (a " + origType
                                                + ") in truncated class " + cld.getName()
                                                + " are of different types");
                                    }
                                }
                            } else if (!field.isCollection()) {
                                referenceMap.put(field.getName(), (ReferenceDescriptor) field);
                            }
                            // Now we check for our warning:
                            if ((!added.contains(field.getClassDescriptor()))
                                    && (!cld.getSuperDescriptors()
                                        .contains(field.getClassDescriptor()))) {
                                LOG.warn("Field included in truncated class "
                                        + cld.getName() + " " + field.getClassDescriptor()
                                        .getName() + "." + field.getName() + " is from"
                                        + " outside the class. This may result in a table"
                                        + " with lots of columns - consider changing the"
                                        + " truncated class config");
                            }
                        }
                    }
                }
                // At this point, we have a filled attributeMap and referenceMap with all the
                // fields present. Now, we simply transfer that into a Set and return it.
                retval = new Fields(new HashSet<AttributeDescriptor>(attributeMap.values()),
                        new HashSet<ReferenceDescriptor>(referenceMap.values()));
            } else {
                Set<AttributeDescriptor> attributes = new HashSet<AttributeDescriptor>();
                Set<ReferenceDescriptor> references = new HashSet<ReferenceDescriptor>();
                for (FieldDescriptor f : cld.getAllFieldDescriptors()) {
                    if (f instanceof AttributeDescriptor) {
                        attributes.add((AttributeDescriptor) f);
                    } else if (!f.isCollection()) {
                        references.add((ReferenceDescriptor) f);
                    }
                }
                retval = new Fields(attributes, references);
            }
            tableMasterToFieldDescriptors.put(cld, retval);
        }
        return retval;
    }

    private static boolean compatible(String type1, String type2) {
        if ("int".equals(type1) || "java.lang.Integer".equals(type1) || "reference".equals(type1)) {
            if ("int".equals(type2) || "java.lang.Integer".equals(type2)
                    || "reference".equals(type2)) {
                return true;
            }
        } else if ("short".equals(type1) || "java.lang.Short".equals(type1)) {
            if ("short".equals(type2) || "java.lang.Short".equals(type2)) {
                return true;
            }
        } else if ("long".equals(type1) || "java.lang.Long".equals(type1)) {
            if ("long".equals(type2) || "java.lang.Long".equals(type2)) {
                return true;
            }
        } else if ("float".equals(type1) || "java.lang.Float".equals(type1)) {
            if ("float".equals(type2) || "java.lang.Float".equals(type2)) {
                return true;
            }
        } else if ("double".equals(type1) || "java.lang.Double".equals(type1)) {
            if ("double".equals(type2) || "java.lang.Double".equals(type2)) {
                return true;
            }
        } else if ("boolean".equals(type1) || "java.lang.Boolean".equals(type1)) {
            if ("boolean".equals(type2) || "java.lang.Boolean".equals(type2)) {
                return true;
            }
        } else if ("java.lang.String".equals(type1)) {
            if ("java.lang.String".equals(type2)) {
                return true;
            }
        } else if ("java.math.BigDecimal".equals(type1)) {
            if ("java.math.BigDecimal".equals(type2)) {
                return true;
            }
        } else if ("java.util.Date".equals(type1)) {
            if ("java.util.Date".equals(type2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inner class to hold information on Attributes and References.
     *
     * @author Matthew Wakeling
     */
    public static final class Fields
    {
        private Set<AttributeDescriptor> attributes;
        private Set<ReferenceDescriptor> references;

        /**
         * Construct a new Fields object.
         *
         * @param attributes the Set of attributes
         * @param references the Set of references
         */
        private Fields(Set<AttributeDescriptor> attributes, Set<ReferenceDescriptor> references) {
            this.attributes = attributes;
            this.references = references;
        }

        /**
         * Returns the Set of attributes
         *
         * @return a Set
         */
        public Set<AttributeDescriptor> getAttributes() {
            return attributes;
        }

        /**
         * Returns the Set of references
         *
         * @return a Set
         */
        public Set<ReferenceDescriptor> getReferences() {
            return references;
        }
    }
}
