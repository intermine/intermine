package org.flymine.dataloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
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

import java.lang.reflect.Field;

import org.flymine.FlyMineException;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.modelproduction.acedb.AceModelParser;

//import org.flymine.model.acedb.DateType;
//import org.flymine.model.acedb.Int;
//import org.flymine.model.acedb.Text;

/**
 * DataLoader for AceDB data
 * @author Andrew Varley
 */
public class AceDataLoader extends DataLoader
{
    protected static final org.apache.log4j.Logger LOG;

    protected String packageName;
    // The model is stored here so that we can easily fake one for testing purposes
    protected Model model;

    private static final Class DATETYPE;
    private static final Class FLOAT;
    private static final Class INT;
    private static final Class TEXT;

    static {
        LOG = org.apache.log4j.Logger.getLogger(AceDataLoader.class);
        Class tDatetype, tFloat, tInt, tText;
        tDatetype = null;
        tFloat = null;
        tInt = null;
        tText = null;
        try {
            tDatetype = Class.forName("org.flymine.model.acedb.DateType");
            tFloat = Class.forName("org.flymine.model.acedb.Float");
            tInt = Class.forName("org.flymine.model.acedb.Int");
            tText = Class.forName("org.flymine.model.acedb.Text");
        } catch (Exception e) {
            LOG.error(e.getMessage() + " while looking for acedb primitive objects");
        }
        DATETYPE = tDatetype;
        FLOAT = tFloat;
        INT = tInt;
        TEXT = tText;
    }

    /**
     * No-arg constructor for testing purposes
     */
    protected AceDataLoader() {
    }

    /**
     * @see AbstractDataLoader#Constructor
     */
    public AceDataLoader(IntegrationWriter iw) {
        super(iw);
        model = iw.getObjectStore().getModel();
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

            Collection clazzNames = model.getClassNames();
            Iterator clazzIter = clazzNames.iterator();
            while (clazzIter.hasNext()) {
                String clazzName = (String) clazzIter.next();
                if (true) {
                   /* !("org.flymine.model.acedb.Clone".equals(clazzName)
                            || "org.flymine.model.acedb.Contig".equals(clazzName)
                            || "org.flymine.model.acedb.Oligo".equals(clazzName)
                            || "org.flymine.model.acedb.DNA".equals(clazzName)
                            || "org.flymine.model.acedb.Keyword".equals(clazzName)
                            || "org.flymine.model.acedb.LongText".equals(clazzName)
                            || "org.flymine.model.acedb.Peptide".equals(clazzName)
                            || "org.flymine.model.acedb.Comment".equals(clazzName))) {*/
                    packageName = TypeUtil.packageName(clazzName);

                    String aceClazzName = AceModelParser
                        .unformatAceName(TypeUtil.unqualifiedName(clazzName));
                    AceURL objURL = source.relativeURL(TypeUtil.unqualifiedName(aceClazzName));
                    AceSet fetchedAceObjects = (AceSet) Ace.fetch(objURL);
                    if (fetchedAceObjects != null) {
                        LOG.debug("Fetched " + fetchedAceObjects.size() + " "
                                  + aceClazzName + " objects");
                        //Collection objects = processAceObjects(fetchedAceObjects);
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
                    LOG.error("Not bothering with class " + clazzName);
                }
            }

        } catch (Exception e) {
            throw new FlyMineException(e);
        }
    }

    /**
     * Process a set of Ace objects
     *
     * @param set the set of Ace objects to process
     * @return a set of Java objects
     *
     * @throws AceException if an error occurs with the Ace data
     * @throws FlyMineException if an object cannot be instantiated
     */
    /*
    protected Set processAceObjects(AceSet set)
        throws AceException, FlyMineException {
        if (set == null) {
            throw new NullPointerException("set must not be null");
        }

        HashSet ret = new HashSet();
        Iterator aceObjIter = set.iterator();
        while (aceObjIter.hasNext()) {
            // Convert to Java object
            AceObject aceObj = (AceObject) aceObjIter.next();
            LOG.debug("Processing object: " + aceObj.getName());
            Object obj = processAceObject(aceObj);
            ret.add(obj);
        }
        return ret;
    }
*/

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
        LOG.debug("Processing Ace Object: " + aceObject.getClassName() + ", "
                + aceObject.getName());
        if (aceObject == null) {
            throw new NullPointerException("aceObject must not be null");
        }
        Object currentObject = null;
        try {
            String clazzName = AceModelParser.formatAceName(((AceObject) aceObject)
                                                            .getClassName());
            Object identifier = aceObject.getName();
            if ("".equals(identifier)) {
                identifier = null;
            }
            if ("boolean".equals(clazzName)) {
                clazzName = "java.lang.Boolean";
                currentObject = Boolean.valueOf((String) identifier);
            } else {
                if ("Date".equals(clazzName)) {
                    clazzName = packageName + ".DateType";
                    if (identifier instanceof String) {
                        identifier = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"))
                            .parse((String) identifier);
                    }
                    LOG.error("Handled new Date object -> DateType");
                } else if ("String".equals(clazzName)) {
                    clazzName = packageName + ".Text";
                    LOG.error("Handled new String object -> Text");
                } else {
                    clazzName = packageName + "." + clazzName;
                }
                currentObject = Class.forName(clazzName).newInstance();
                setField(currentObject, "identifier", identifier);
            }
        } catch (ClassNotFoundException e) {
            throw new FlyMineException(e);
        } catch (InstantiationException e) {
            throw new FlyMineException(e);
        } catch (IllegalAccessException e) {
            throw new FlyMineException(e);
        } catch (ParseException e) {
            throw new FlyMineException(e);
        }

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
        String nodeType;
        Object nodeValue;
        String nodeName;
        if (aceNode instanceof Reference) {
            // nodeName is the name of the field in currentObject
            nodeName = getName(aceNode);
            // nodeValue is the identifier of the referred to object
            nodeValue = aceNode.getName();
            // nodeClass is the class of the referred to object, and is part of the target AceURL
            String nodeClass = ((Reference) aceNode).getTarget().getPath();
            //          LOG.info("ACE: " + nodeClass);

            nodeClass = nodeClass.substring(0, nodeClass.indexOf("/", 1));
            // Set up a dummy AceObject to encapsulate this info and convert to proper Object
            AceObject referredToAceObject = new StaticAceObject((String) nodeValue,
                                                                null, nodeClass);
            Object referredToObject = processAceObject(referredToAceObject);
            setField(currentObject, nodeName, referredToObject);
        } else if (aceNode instanceof DateValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = DATETYPE.getConstructor(new Class[] {}).newInstance(new Object[] {});
                DATETYPE.getMethod("setIdentifier", new Class[] {Date.class})
                    .invoke(nodeValue, new Object[] {((DateValue) aceNode).toDate()});
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof FloatValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = FLOAT.getConstructor(new Class[] {}).newInstance(new Object[] {});
                FLOAT.getMethod("setIdentifier", new Class[] {Float.class})
                    .invoke(nodeValue, new Object[] {new Float(((FloatValue) aceNode).toFloat())});
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof IntValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = INT.getConstructor(new Class[] {}).newInstance(new Object[] {});
                INT.getMethod("setIdentifier", new Class[] {Integer.class})
                    .invoke(nodeValue, new Object[] {new Integer(((IntValue) aceNode).toInt())});
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof StringValue) {
            try {
                nodeName = getName(aceNode);
                nodeValue = TEXT.getConstructor(new Class[] {}).newInstance(new Object[] {});
                TEXT.getMethod("setIdentifier", new Class[] {String.class})
                    .invoke(nodeValue, new Object[] {((StringValue) aceNode).toString()});
                setField(currentObject, nodeName, nodeValue);
            } catch (Exception e) {
                throw new FlyMineException(e);
            }
        } else if (aceNode instanceof AceNode) {
            nodeName = aceNode.getName();
            // Give it a chance to set a Boolean flag
            Field nodeField = TypeUtil.getField(currentObject.getClass(), nodeName);
            if ((nodeField != null) && (nodeField.getType() == Boolean.class)) {
                setField(currentObject, nodeName, Boolean.TRUE);
            } else if ((nodeField != null) && !hasChildValues(aceNode)) {
                // Is it a hash? If it is, currentObject will have a field of this name
                // and node will not have any values hanging off it

                // The node could be a Collection or reference
                String nodeClass = null;
                ClassDescriptor cld = model.getClassDescriptorByName(currentObject
                                                                     .getClass().getName());
                FieldDescriptor fd = cld.getFieldDescriptorByName(nodeName);
                if (fd instanceof CollectionDescriptor) {
                    // Find out the type of the elements
                    CollectionDescriptor fdCld = (CollectionDescriptor) fd;
                    ClassDescriptor referencedCld = fdCld.getReferencedClassDescriptor();
                    nodeClass = TypeUtil.unqualifiedName(referencedCld.getClassName());
                } else {
                    nodeClass = TypeUtil.unqualifiedName(nodeField.getType().getName());
                }
                StaticAceObject referredToAceObject = new StaticAceObject("", // no identifier
                                                                    null, // no parent
                                                                    nodeClass);
                // Add all of the child nodes to this AceObject
                Iterator nodesIter = aceNode.iterator();
                while (nodesIter.hasNext()) {
                    referredToAceObject.addNode((AceNode) nodesIter.next());
                }
                Object referredToObject = processAceObject(referredToAceObject);
                setField(currentObject, nodeName, referredToObject);
            }

        }
        // Now iterate through all the child nodes
        if (aceNode instanceof AceNode) {
            Iterator objIter = aceNode.iterator();
            while (objIter.hasNext()) {
                processAceNode((AceNode) objIter.next(), currentObject);
            }
        } else {
            throw new FlyMineException("Node type " + aceNode.getClass() + " not dealt with");
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
        LOG.debug("Setting field: " + fieldName + "=" + fieldValue
                + (fieldValue == null ? "" : " (a " + fieldValue.getClass().getName() + ")"));
        try {
            Field field = TypeUtil.getField(target.getClass(), fieldName);
            if (field != null) {
                Class fieldType = field.getType();
                if (Collection.class.isAssignableFrom(fieldType)) {
                    LOG.debug("Adding to Collection");
                    ((Collection) TypeUtil.getFieldValue(target, fieldName)).add(fieldValue);
                } else {
                    LOG.debug("Setting value");
                    try {
                        if (DATETYPE.isInstance(fieldValue)) {
                            fieldValue = DATETYPE.getMethod("getIdentifier", new Class[] {})
                                .invoke(fieldValue, new Object[] {});
                        } else if (FLOAT.isInstance(fieldValue)) {
                            fieldValue = FLOAT.getMethod("getIdentifier", new Class[] {})
                                .invoke(fieldValue, new Object[] {});
                        } else if (TEXT.isInstance(fieldValue)) {
                            fieldValue = TEXT.getMethod("getIdentifier", new Class[] {})
                                .invoke(fieldValue, new Object[] {});
                        } else if (INT.isInstance(fieldValue)) {
                            fieldValue = INT.getMethod("getIdentifier", new Class[] {})
                                .invoke(fieldValue, new Object[] {});
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
     * @param aceNode the node
     * @return the name of the parent of the node, or the parent's name if this node is a data node
     * @throws AceException if error occurs with the Ace data
     */
    protected String getName(AceSet aceNode) throws AceException {
        String name = aceNode.getParent().getName();
        AceSet node = aceNode;
        int count = 1;

        while (((node = node.getParent()) != null)
               && ((node instanceof StringValue)
                   || (node instanceof IntValue)
                   || (node instanceof FloatValue)
                   || (node instanceof Reference))) {
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
            if ((childNode instanceof StringValue)
                || (childNode instanceof IntValue)
                || (childNode instanceof FloatValue)
                || (childNode instanceof Reference)
                || (childNode instanceof DateValue)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Used for testing access to an AceDB server
     *
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String [] args) throws Exception {
        if (args.length != 5) {
            throw new RuntimeException("AceDataLoader hostname port username password classname");
        }

        String host = args[0];
        String port = args[1];
        String user = args[2];
        String passwd = args[3];
        String clazzName = args[4];

        // URL _dbURL = new URL("acedb://" + host + ":" + port);
        // AceURL dbURL = new AceURL(_dbURL, user, passwd, null);
        AceURL dbURL = new AceURL("acedb://" + user + ':' + passwd + '@' + host + ':' + port);

        Ace.registerDriver(new org.acedb.socket.SocketDriver());
        AceURL objURL = dbURL.relativeURL(clazzName);
        AceSet fetchedAceObjects = (AceSet) Ace.fetch(objURL);

        //        Collection col = processAceObjects(fetchedAceObjects, null);


        Iterator iter = fetchedAceObjects.iterator();

        java.io.PrintStream out = System.out;
        
        while (iter.hasNext()) {
            AceNode node = (AceNode) iter.next();
            out.println(node);
        }

        //Model model = org.flymine.modelproduction.acedb.AceModelParser.readerToModel(
        //        new java.io.BufferedReader(new java.io.FileReader("models.wrm")));
//
        //Set objs = processAceObjects(fetchedAceObjects, model);
//
        //out.println(objs);
    }
}
