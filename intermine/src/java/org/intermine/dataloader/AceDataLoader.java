package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.acedb.Ace;
import org.acedb.AceException;
import org.acedb.AceURL;
import org.acedb.AceSet;
import org.acedb.AceNode;
import org.acedb.AceObject;
import org.acedb.DateValue;
import org.acedb.StringValue;
import org.acedb.IntValue;
import org.acedb.FloatValue;
import org.acedb.Reference;
import org.acedb.staticobj.StaticAceObject;

import org.flymine.FlyMineException;
import org.flymine.util.StringUtil;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.modelproduction.acedb.AceModelParser;

import org.apache.log4j.Logger;

/**
 * DataLoader for AceDB data
 * @author Andrew Varley
 */
public class AceDataLoader extends DataLoader
{
    protected static final Logger LOG = Logger.getLogger(AceDataLoader.class);

    protected static final String DATETYPE = "DateType";
    protected static final String FLOAT = "Float";
    protected static final String INT = "Int";
    protected static final String TEXT = "Text";

    protected Model model;

    protected String pkgName; //as ace namespace is flat, assume all classes in same package
    protected String dateType, floatType, intType, textType;

    /**
     * Constructor for testing purposes
     * @param model a Model independent of an IntegrationWriter
     */
    protected AceDataLoader(Model model) {
        this.model = model;
        initialise();
    }

    /**
     * @see AbstractDataLoader#Constructor
     */
    public AceDataLoader(IntegrationWriter iw) {
        super(iw);
        model = iw.getObjectStore().getModel();
        initialise();
    }

    /**
     * Initialise the built-in types
     */
    protected void initialise() {
        pkgName = TypeUtil.packageName(((String) model.getClassNames().iterator().next())) + ".";
        dateType = pkgName + DATETYPE;
        floatType = pkgName + FLOAT;
        intType = pkgName + INT;
        textType = pkgName + TEXT;
    }        

    /**
     * Static method to unmarshall business objects from a given Ace server and call
     * store on each.
     *
     * @param source access to AceDb
     * @throws FlyMineException if anything goes wrong with ace or storing
     */
    public void processAce(AceURL source) throws FlyMineException {
        try {
            Ace.registerDriver(new org.acedb.socket.SocketDriver());

            // Go through each class in the model and get a dump of the objects of
            // that class

            Collection clsNames = model.getClassNames();
            Iterator clazzIter = clsNames.iterator();
            while (clazzIter.hasNext()) {
                String clsName = (String) clazzIter.next();
                if (true) {
                    String aceClazzName = AceModelParser
                        .unformatAceName(TypeUtil.unqualifiedName(clsName));
                    AceURL objURL = source.relativeURL(aceClazzName);
                    AceSet fetchedAceObjects = (AceSet) Ace.fetch(objURL);
                    if (fetchedAceObjects != null) {
                        LOG.debug("Fetched " + fetchedAceObjects.size() + " "
                                  + aceClazzName + " objects");
                        Iterator aceObjIter = fetchedAceObjects.iterator();
                        while (aceObjIter.hasNext()) {
                            AceObject aceObj = null;
                            try {
                                aceObj = (AceObject) aceObjIter.next();
                                LOG.debug("Processing object: " + aceObj.getName());
                                Object obj = processAceObject(aceObj);
                                // Now store that object
                                LOG.debug("Storing object: " + obj);
                                store(obj);
                            } catch (NoSuchElementException e) {
                                LOG.error("Object not retrievable: " + e.getMessage());
                            }
                        }
                        LOG.debug("Stored " + fetchedAceObjects.size() + " "
                                + aceClazzName + " objects");
                    } else {
                        LOG.debug("No " + aceClazzName + " objects found");
                    }
                } else {
                    LOG.error("Not bothering with class " + clsName);
                }
            }

        } catch (Exception e) {
            throw new FlyMineException(e);
        }
    }

    /**
     * Process an AceObject. This will create a new instance of the
     * object and set the identifier.
     *
     * @param aceObject the AceObject to process
     * @return an instance of the object
     *
     * @throws AceException if an error occurs with the Ace data
     * @throws FlyMineException if object cannot be instantiated
     */
    protected Object processAceObject(AceObject aceObject)
        throws AceException, FlyMineException {
        if (aceObject == null) {
            throw new NullPointerException("aceObject must not be null");
        }
        LOG.debug("Processing Ace Object: " + aceObject.getClassName() + ", "
                + aceObject.getName());

        String clsName = pkgName 
            + AceModelParser.formatAceName(((AceObject) aceObject).getClassName());
        Object currentObject = null;        
        try {
            currentObject = Class.forName(clsName).newInstance();
        } catch (Exception e) {
            LOG.error(e.toString());
        }

        Object identifier = aceObject.getName();
//         if ("".equals(identifier)) {
//             identifier = null;
//         }
        setField(currentObject, "identifier", identifier);
        
        processAceNode(aceObject, currentObject);
        return currentObject;
    }

    /**
     * Process an AceNode. This will set field values in the given
     * object if the node is a data node.
     *
     * @param aceNode the AceNode to process
     * @param currentObject the object in which to set field
     *
     * @throws AceException if an error occurs with the Ace data
     * @throws FlyMineException if object cannot be instantiated
     */
    protected void processAceNode(AceNode aceNode, Object currentObject)
        throws AceException, FlyMineException {
        String nodeName; //name of the field in currentObject
        Object nodeValue; //identifier of the referred to object
        if (aceNode instanceof Reference) {
            nodeName = getName(aceNode);
            nodeValue = aceNode.getName();
            // nodeClass is the class of the referred to object, and is part of the target AceURL
            String nodeClass = ((Reference) aceNode).getTarget().getPath();
            nodeClass = nodeClass.substring(0, nodeClass.indexOf("/", 1));
            AceObject referredToAceObject = new StaticAceObject((String) nodeValue,
                                                                null, nodeClass);
            Object referredToObject = processAceObject(referredToAceObject);
            setField(currentObject, nodeName, referredToObject);
        } else if (aceNode instanceof DateValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = instantiate(dateType);
                TypeUtil.setFieldValue(nodeValue, "identifier", ((DateValue) aceNode).toDate());
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof FloatValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = instantiate(floatType);
                TypeUtil.setFieldValue(nodeValue, "identifier",
                                       new Float(((FloatValue) aceNode).toFloat()));
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof IntValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = instantiate(intType);
                TypeUtil.setFieldValue(nodeValue, "identifier",
                                       new Integer(((IntValue) aceNode) .toInt()));
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof StringValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = instantiate(textType);
                TypeUtil.setFieldValue(nodeValue, "identifier", ((StringValue) aceNode).toString());
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (!(aceNode instanceof AceObject)) { //node representing a field
            nodeName = aceNode.getName();
            //check whether currentObject has a field of this name (if not, it's a nesting tag)
            Field nodeField = TypeUtil.getField(currentObject.getClass(), nodeName);
            if (nodeField != null) {
                 if (!aceNode.iterator().hasNext()) {
                    if (nodeField.getType().equals(Boolean.TYPE)) {
                        // there's no boolean value type - if the tag's present, the value is true
                        nodeValue = Boolean.TRUE;
                    } else {
                        // no children at all - a non-boolean tag without value. not handled (yet).
                        nodeValue = null;
                    }
                    setField(currentObject, nodeName, nodeValue);
                } else if (!hasChildValues(aceNode)) {
                    // node has child values, so it's a hash. we create a new object in its place
                    String nodeClass = null;
                    ClassDescriptor cld = model.getClassDescriptorByName(currentObject
                                                                         .getClass().getName());
                    FieldDescriptor fd = cld.getFieldDescriptorByName(nodeName);
                    // the node could be a collection or reference
                    if (fd instanceof CollectionDescriptor) {
                        // find out the type of the elements
                        CollectionDescriptor fdCld = (CollectionDescriptor) fd;
                        ClassDescriptor referencedCld = fdCld.getReferencedClassDescriptor();
                        nodeClass = TypeUtil.unqualifiedName(referencedCld.getClassName());
                    } else {
                        nodeClass = TypeUtil.unqualifiedName(nodeField.getType().getName());
                    }
                    StaticAceObject referredToAceObject =
                        new StaticAceObject(StringUtil.uniqueString(), null, nodeClass);
                    // add all of the child nodes to our new AceObject
                    Iterator nodesIter = aceNode.iterator();
                    while (nodesIter.hasNext()) {
                        referredToAceObject.addNode((AceNode) nodesIter.next());
                    }
                    nodeValue = processAceObject(referredToAceObject);
                    setField(currentObject, nodeName, nodeValue);
                }
            } else {
                LOG.debug(currentObject.getClass() + " has no field named " + nodeName);
            }
        }
        // Now iterate through all the child nodes
        Iterator objIter = aceNode.iterator();
        while (objIter.hasNext()) {
            processAceNode((AceNode) objIter.next(), currentObject);
        }
    }

    /**
     * Sets a field in a target object, or adds the piece of data to a collection
     *
     * @param target the object in which to set the field
     * @param fieldName the name of the field to set
     * @param fieldValue the value to set or to be added to a collection
     * @throws FlyMineException if the field cannot be accessed
     */
    protected void setField(Object target, String fieldName, Object fieldValue)
        throws FlyMineException {
        try {
            Field field = TypeUtil.getField(target.getClass(), fieldName);
            if (field != null) {
                if (Collection.class.isAssignableFrom(field.getType()) && fieldValue != null) {
                    LOG.debug("Adding to Collection");
                    ((Collection) TypeUtil.getFieldValue(target, fieldName)).add(fieldValue);
                } else {
                    LOG.debug("Setting value");
                    try {
                        if (fieldValue != null && isBuiltIn(fieldValue.getClass())) {
                            fieldValue = TypeUtil.getFieldValue(fieldValue, "identifier");
                        }
                    } catch (Exception e) {
                        throw new FlyMineException(e);
                    }
                    TypeUtil.setFieldValue(target, fieldName, fieldValue);
                }
            } else {
                // else the field cannot be found -- do nothing
                if (!"Quoted_in".equals(fieldName)) {
                    LOG.error("Field \"" + fieldName + "\" not found in object \""
                            + target.toString() + "\" (a " + target.getClass().getName()
                            + ") - would have set to \"" + fieldValue + "\"");
                }
            }
        } catch (IllegalAccessException e) {
            throw new FlyMineException(e);
        }
    }

    /**
     * Get the name of the parent of an AceNode, with suffix if a multi-valued tag
     *
     * @param node the node
     * @return the name of the parent of the node, or the parent's name if this node is a data node
     * @throws AceException if error occurs with the Ace data
     */
    protected String getName(AceSet node) throws AceException {
        String name = node.getParent().getName();
        int count = 1;
        while (((node = node.getParent()) != null)
               && (node instanceof DateValue
                   || node instanceof FloatValue
                   || node instanceof IntValue
                   || node instanceof StringValue
                   || node instanceof Reference)) {
            count++;
        }
        if (count > 1) {
            name = node.getName() + "_" + count;
        }
        return name;
    }

    /**
     * Returns true if the given node has values as children
     *
     * @param node the node to test
     * @return true if the node has values as children
     * @throws AceException if error occurs with the Ace data
     */
    protected boolean hasChildValues(AceNode node) throws AceException {
        Iterator childIter = node.iterator();
        while (childIter.hasNext()) {
            AceNode childNode = (AceNode) childIter.next();
            if (childNode instanceof DateValue
                || childNode instanceof FloatValue
                || childNode instanceof IntValue
                || childNode instanceof StringValue
                || childNode instanceof Reference) {
                return true;
            }
        }
        return false;
    }

    private Object instantiate(String clsName) {
        Object obj = null;
        try {
            obj = Class.forName(clsName).newInstance();
        } catch (Exception e) {
            LOG.error("Cannot instantiate '" + clsName + "':" + e);
        }
        return obj;
    }

    private boolean isBuiltIn(Class c) {
        String clsName = c.getName();
        if (dateType.equals(clsName)
            || floatType.equals(clsName)
            || intType.equals(clsName)
            || textType.equals(clsName)) {
            return true;
        }
        return false;
    }

    /**
     * Used for testing access to an AceDB server
     *
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    /*
    public static void main(String [] args) throws Exception {
        if (args.length != 5) {
            throw new RuntimeException("AceDataLoader hostname port username password classname");
        }

        String host = args[0];
        String port = args[1];
        String user = args[2];
        String passwd = args[3];
        String clsName = args[4];

        AceURL dbURL = new AceURL("acedb://" + user + ':' + passwd + '@' + host + ':' + port);

        Ace.registerDriver(new org.acedb.socket.SocketDriver());
        AceURL objURL = dbURL.relativeURL(clsName);
        AceSet fetchedAceObjects = (AceSet) Ace.fetch(objURL);

        Iterator iter = fetchedAceObjects.iterator();
        while (iter.hasNext()) {
            AceNode node = (AceNode) iter.next();
            System.
            out.println(node);
        }
    }
    */
}
