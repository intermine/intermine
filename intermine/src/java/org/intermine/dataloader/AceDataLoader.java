package org.flymine.dataloader;

import java.util.Iterator;
import java.util.Collection;

import org.acedb.Ace;
import org.acedb.AceException;
import org.acedb.AceURL;
import org.acedb.AceSet;
import org.acedb.AceNode;
import org.acedb.AceObject;
import org.acedb.StringValue;
import org.acedb.IntValue;
import org.acedb.FloatValue;
import org.acedb.Reference;

import java.lang.reflect.Field;

import org.flymine.FlyMineException;
import org.flymine.util.TypeUtil;
import org.flymine.util.ModelUtil;

/**
 * DataLoader for AceDB data
 * @author Andrew Varley
 */
public class AceDataLoader extends AbstractDataLoader
{
    /**
     * Static method to unmarshall business objects from a given xml file and call
     * store on each.
     *
     * @param model name of data model being used
     * @param iw writer to handle storing data
     * @param source access to AceDb
     * @throws FlyMineException if anything goes wrong with xml or storing
     */
    public static void processAce(String model, IntegrationWriter iw,
                           AceURL source) throws FlyMineException {
        try {
            Ace.registerDriver(new org.acedb.socket.SocketDriver());

            // Go through each class in the model and get a dump of the objects of
            // that class

            String clazzName = "Sequence";

            AceURL objURL = source.relativeURL(clazzName);
            AceSet fetchedObjects = (AceSet) Ace.fetch(objURL);

            Iterator objIter = fetchedObjects.iterator();
            while (objIter.hasNext()) {
                processAceObject((AceObject) objIter.next(), null);
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
     * @param model the model this object comes from, or null if AceObject name is fully qualified
     * @return an instance of the object
     *
     * @throws AceException if an error occurs with the Ace data
     * @throws FlyMineException if object cannot be instantiated
     */
    protected static Object processAceObject(AceObject aceObject, String model)
        throws AceException, FlyMineException {
        Object currentObject = null;
        try {
            String clazzName = ((AceObject) aceObject).getClassName();
            if (model != null) {
                clazzName = "org.flymine.model." + model + "."
                    + clazzName;
            }
            currentObject = Class.forName(clazzName).newInstance();
            setField(currentObject, "identifier", aceObject.getName());
        } catch (ClassNotFoundException e) {
            throw new FlyMineException(e);
        } catch (InstantiationException e) {
            throw new FlyMineException(e);
        } catch (IllegalAccessException e) {
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
    protected static void processAceNode(AceNode aceNode, Object currentObject)
        throws AceException, FlyMineException {
        String nodeType;
        Object nodeValue;
        String nodeName;
        if (aceNode instanceof Reference) {
            nodeName = getName(aceNode);
            nodeValue = aceNode.getName();
        } else if (aceNode instanceof FloatValue) {
            nodeName = getName(aceNode);
            nodeValue = new Float(((FloatValue) aceNode).toFloat());
            setField(currentObject, nodeName, nodeValue);
        } else if (aceNode instanceof IntValue) {
            nodeName = getName(aceNode);
            nodeValue = new Integer(((IntValue) aceNode).toInt());
            setField(currentObject, nodeName, nodeValue);
        } else if (aceNode instanceof StringValue) {
            nodeName = getName(aceNode);
            nodeValue = ((StringValue) aceNode).toString();
            setField(currentObject, nodeName, nodeValue);
        } else if (aceNode instanceof AceNode) {
            nodeName = aceNode.getName();
            // Give it a chance to set a Boolean flag
            Field nodeField = TypeUtil.getField(currentObject.getClass(), nodeName);
            if ((nodeField != null) && (nodeField.getType() == Boolean.class)) {
                setField(currentObject, nodeName, Boolean.TRUE);
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
    protected static void setField(Object target, String fieldName, Object fieldValue)
        throws FlyMineException {
        try {
            if (ModelUtil.getFieldType(target.getClass(), fieldName) == ModelUtil.COLLECTION) {
                ((Collection) TypeUtil.getFieldValue(target, fieldName)).add(fieldValue);
            } else {
                TypeUtil.setFieldValue(target, fieldName, fieldValue);
            }
        } catch (IllegalAccessException e) {
            throw new FlyMineException(e);
        }
    }

    /**
     * Get the name of an AceNode, or of the parent if it is a data node
     *
     * @param aceNode the node
     * @return the name of the node, or the parent's name if this node is a data node
     */
    protected static String getName(AceSet aceNode) {
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
            name = node.getName() + "_" + count;;
        }
        return name;

    }

    /**
     * Used for testing access to an AceDB server
     *
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String [] args) throws Exception {
        if (args.length != 4) {
            throw new RuntimeException("AceDataLoader hostname port username password");
        }

        String host = args[0];
        String port = args[1];
        String user = args[2];
        String passwd = args[3];

        // URL _dbURL = new URL("acedb://" + host + ":" + port);
        // AceURL dbURL = new AceURL(_dbURL, user, passwd, null);
        AceURL dbURL = new AceURL("acedb://" + user + ':' + passwd + '@' + host + ':' + port);
        AceDataLoader adl = new AceDataLoader();
        adl.processAce(null, null, dbURL);
    }
}
