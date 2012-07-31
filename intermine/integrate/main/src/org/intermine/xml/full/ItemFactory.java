package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.util.TypeUtil;
import org.intermine.xml.XmlHelper;

/**
 * ItemFactory class
 *
 * @author Kim Rutherford
 */

public class ItemFactory
{
    /**
     * An ItemFactory constructed with no Model, so the Items it creates do no type checking.
     */
    public static final ItemFactory NULL_MODEL_ITEM_FACTORY = new ItemFactory();

    private Model model;
    private int newItemId = 1;
    private String prefix = "0_";

    /**
     * Create an ItemFactory that isn't specific to a particular model.  Items created by this
     * ItemFactory will not be checked against a model.
     */
    public ItemFactory () {
        this.model = null;
    }

    /**
     * Create an ItemFactory specific to a particular model.  Items created by this ItemFactory will
     * be checked against the given model.  ie. the set methods in Item will check that the
     * operation is valid for the given model.
     * @param model the Model to use when checking
     */
    public ItemFactory (Model model) {
        this.model = model;
    }

    /**
     * Create an ItemFactory specific to a particular model.  Items created by this ItemFactory will
     * be checked against the given model.  ie. the set methods in Item will check that the
     * operation is valid for the given model.
     * @param model the Model to use when checking - can be null to indicate that no checking should
     * be done
     * @param identifierPrefix the prefix to add to automatically created Item identifiers
     */
    public ItemFactory (Model model, String identifierPrefix) {
        this.model = model;
        this.prefix = identifierPrefix;
    }

    /**
     * Make an empty Item with a unique identifier.  The identifier will be prefixed by the
     * identifierPrefix argument to the constructor, or "" if identifierPrefix was not set.
     * @return the new Item
     */
    public Item makeItem() {
        Item item = new Item();
        item.setIdentifier(prefix + (newItemId++));
        item.setModel(model);
        return item;
    }

    /**
     * Make an empty Item with the given identifier.
     * @param identifier the identifier of the new Item
     * @return the new Item
     */
    public Item makeItem(String identifier) {
        Item item = new Item();
        item.setIdentifier(identifier);
        item.setModel(model);
        return item;
    }

    /**
     * Construct an item from an identifier, a class name and the names of implemented classes
     * @param identifier item identifier - if null create a unique identifier prefixed with the
     * prefix argument to the constructor
     * @param className name of described class
     * @param implementations names of implemented classes
     * @return the new Item
     */
    public Item makeItem(String identifier, String className, String implementations) {
        Item item;
        if (identifier == null) {
            item = makeItem();
        } else {
            item = makeItem(identifier);
        }

        item.setClassName(className);
        item.setImplementations(implementations);
        return item;
    }

    /**
     * Construct an item from a class name.  A new unique identifier will be created for the object
     * by the ItemFactory.
     * @param className name of described class
     * @return the new Item
     */
    public Item makeItemForClass(String className) {
        return makeItem(null, className, "");
    }


    /**
     * Convert an Object to Item format.
     * @param obj object to convert
     * @return a new Full Data Item
     */
    public Item makeItem(FastPathObject obj) {
        return makeItemImpl(obj, TypeUtil.getFieldInfos(obj.getClass()).keySet());
    }


    /**
     * Convert an Object to Item format, writing the fields provided in includeFields.
     * @param obj object to convert
     * @param includeFields the field names to write
     * @return a new Full Data Item
     */
    public Item makeItemImpl(FastPathObject obj, Set<String> includeFields) {
        if ((obj instanceof InterMineObject) && (((InterMineObject) obj).getId() == null)) {
            throw new IllegalArgumentException("Id of object was null (" + obj.toString() + ")");
        }

        String className = XmlHelper.getClassName(obj, model);

        Item item = makeItem(obj instanceof InterMineObject ? ((InterMineObject) obj).getId()
                .toString() : null);
        item.setClassName("".equals(className) ? ""
                : TypeUtil.unqualifiedName(className));
        item.setImplementations(getImplements(obj, model));

        try {
            for (String fieldname : includeFields) {
                // If Reference, value is id of referred-to object
                // If Attribute, value is field value
                // If Collection, contains list of ids of objects in collection

                // Element is not output if the value is null

                Object value = obj.getFieldValue(fieldname);

                if (value == null) {
                    continue;
                }
                // Collection
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    Collection<?> col = (Collection<?>) value;
                    if (col.size() > 0) {
                        ReferenceList refList = new ReferenceList(fieldname);
                        for (Iterator<?> j = col.iterator(); j.hasNext();) {
                            InterMineObject tempobj = (InterMineObject) j.next();
                            refList.addRefId((tempobj).getId().toString());
                        }
                        item.addCollection(refList);
                    }
                } else if (value instanceof InterMineObject) {
                    item.setReference(fieldname, ((InterMineObject) value).getId().toString());
                } else {
                    if (!fieldname.equalsIgnoreCase("id")) {
                        if (value instanceof ClobAccess) {
                            item.setAttribute(fieldname, ((ClobAccess) value).toString());
                        } else {
                            item.setAttribute(fieldname, TypeUtil.objectToString(value));
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            // TODO
        }
        return item;
    }

    /**
     * Get all interfaces that an object implements.
     *
     * @param obj the object
     * @param model the parent model
     * @return space separated list of extended/implemented classes/interfaces
     */
    protected static String getImplements(Object obj, Model model) {
        StringBuffer sb = new StringBuffer();

        Class<?>[] interfaces = obj.getClass().getInterfaces();
        Arrays.sort(interfaces, new RendererComparator());

        for (int i = 0; i < interfaces.length; i++) {
            ClassDescriptor cld = model.getClassDescriptorByName(interfaces[i].getName());
            if (cld != null && cld.isInterface()
                && !"org.intermine.model.InterMineObject".equals(cld.getName())) {
                sb.append(TypeUtil.unqualifiedName(interfaces[i].getName())).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
