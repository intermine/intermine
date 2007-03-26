package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.util.TypeUtil;
import org.intermine.util.StringUtil;
import org.intermine.model.InterMineObject;
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
    private String prefix = "";

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
    * Convert a data model item to an XML one
    * @param item the data model Item
    * @return an equivalent XML Item
    */
    protected static Item makeItem(org.intermine.model.fulldata.Item item) {
        Item newItem = new Item();
        newItem.setIdentifier(item.getIdentifier());
        newItem.setClassName(item.getClassName());
        newItem.setImplementations(item.getImplementations());

        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            org.intermine.model.fulldata.Attribute attr =
                (org.intermine.model.fulldata.Attribute) i.next();
            Attribute newAttr = new Attribute();
            newAttr.setName(attr.getName());
            newAttr.setValue(attr.getValue());
            newItem.addAttribute(newAttr);
        }

        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            org.intermine.model.fulldata.Reference ref =
                (org.intermine.model.fulldata.Reference) i.next();
            Reference newRef = new Reference();
            newRef.setName(ref.getName());
            newRef.setRefId(ref.getRefId());
            newItem.addReference(newRef);
        }

        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            org.intermine.model.fulldata.ReferenceList refs
                = (org.intermine.model.fulldata.ReferenceList) i.next();
            ReferenceList newRefs = new ReferenceList(refs.getName(),
                                                      StringUtil.tokenize(refs.getRefIds()));
            newItem.addCollection(newRefs);
        }

        return newItem;
    }


    /**
     * Convert a InterMineObject to Item format.
     * @param obj object to convert
     * @return a new Full Data Item
     */
    public Item makeItem(InterMineObject obj) {
        return makeItemImpl(obj, true);
    }


    /**
     * Convert a InterMineObject to Item format.
     * @param obj object to convert
     * @param transferCollections if true add the colections from obj to the new Item, otherwise
     * ignore the collections of obj
     * @return a new Full Data Item
     */
    public Item makeItemImpl(InterMineObject obj, boolean transferCollections) {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("Id of object was null (" + obj.toString() + ")");
        }

        String className = XmlHelper.getClassName(obj, model);

        Item item = makeItem(obj.getId().toString());
        item.setClassName(className.equals("") ? "" : model.getNameSpace()
                          + TypeUtil.unqualifiedName(XmlHelper.getClassName(obj, model)));
        item.setImplementations(getImplements(obj, model));

        try {
            Map infos = TypeUtil.getFieldInfos(obj.getClass());
            Iterator iter = infos.keySet().iterator();
            while (iter.hasNext()) {
                // If Reference, value is id of referred-to object
                // If Attribute, value is field value
                // If Collection, contains list of ids of objects in collection

                // Element is not output if the value is null

                String fieldname = (String) iter.next();
                Object value = TypeUtil.getFieldValue(obj, fieldname);

                if (value == null) {
                    continue;
                }
                // Collection
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    if (transferCollections) {
                        Collection col = (Collection) value;
                        if (col.size() > 0) {
                            ReferenceList refList = new ReferenceList(fieldname);
                            for (Iterator j = col.iterator(); j.hasNext();) {
                                InterMineObject tempobj = (InterMineObject) j.next();
                                refList.addRefId((tempobj).getId().toString());
                            }
                            item.addCollection(refList);
                        }
                    }
                } else if (value instanceof InterMineObject) {
                    Reference ref = new Reference();
                    ref.setName(fieldname);
                    ref.setRefId(((InterMineObject) value).getId().toString());
                    item.addReference(ref);
                } else {
                    if (!fieldname.equalsIgnoreCase("id")) {
                        Attribute attr = new Attribute();
                        attr.setName(fieldname);
                        attr.setValue(TypeUtil.objectToString(value));
                        item.addAttribute(attr);
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
    protected static String getImplements(InterMineObject obj, Model model) {
        StringBuffer sb = new StringBuffer();

        Class [] interfaces = obj.getClass().getInterfaces();
        Arrays.sort(interfaces, new RendererComparator());

        for (int i = 0; i < interfaces.length; i++) {
            ClassDescriptor cld = model.getClassDescriptorByName(interfaces[i].getName());
            if (cld != null && cld.isInterface()
                && !cld.getName().equals("org.intermine.model.InterMineObject")) {
                sb.append(model.getNameSpace().toString()
                          + TypeUtil.unqualifiedName(interfaces[i].getName())).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
