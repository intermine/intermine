package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.pathquery.Path;
import org.json.JSONObject;

/**
 * A class to to produce a sequence of JSONObjects from a set of database rows. This requires
 * that the view be set up in a specific way.
 * @author Alexis Kalderimis
 *
 */
public class JSONResultsIterator implements Iterator<JSONObject>
{

    private static final String CLASS_KEY = "class";
    private static final String ID_KEY = "objectId";
    private static final String ID_FIELD = "id";

    private final ExportResultsIterator subIter;
    private List<ResultElement> holdOver;
    private final List<Path> viewPaths = new ArrayList<Path>();
    protected transient Map<String, Object> currentMap;
    protected transient List<Map<String, Object>> currentArray;
    private Model model;

    /**
     * Constructor. The JSON Iterator sits on top of the basic export results iterator.
     * @param it An ExportResultsIterator
     */
    public JSONResultsIterator(ExportResultsIterator it) {
        this.subIter = it;
        init();
    }

    private void init() {
        model = subIter.getQuery().getModel();
        viewPaths.addAll(subIter.getViewPaths());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        if (subIter.hasNext() || holdOver != null) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public JSONObject next() {
        Map<String, Object> nextJsonMap = new HashMap<String, Object>();
        Integer lastId = null;

        if (holdOver != null) {
            lastId = holdOver.get(0).getId();
            addRowToJsonMap(holdOver, nextJsonMap);
            holdOver = null;
        }
        while (subIter.hasNext()) {
            List<ResultElement> result = subIter.next();
            Integer currentId = result.get(0).getId(); // id is guarantor of
                                                       // object identity
            if (lastId != null && !lastId.equals(currentId)) {
                holdOver = result;
                lastId = currentId;
                break;
            } else {
                addRowToJsonMap(result, nextJsonMap);
                lastId = currentId;
            }
        }
        JSONObject nextObj = new JSONObject(nextJsonMap);
        return nextObj;
    }

    private void addRowToJsonMap(List<ResultElement> results,
            Map<String, Object> jsonMap) {
        setOrCheckClassAndId(results.get(0), viewPaths.get(0), jsonMap);

        for (int i = 0; i < results.size(); i++) {
            ResultElement cell = results.get(i);
            if (cell == null || cell.getType() == null) {
                continue;
            }
            Path columnPath = viewPaths.get(i);
            addCellToJsonMap(cell, columnPath, jsonMap);
        }
    }

    /**
     * Test whether a result element matches the type of its path.
     * @param cell The Result element
     * @param path The path which represents the view column
     * @return true if the cell is null, or contains null information, extends/implements the
     * type of path.
     */
    protected boolean isCellValidForPath(ResultElement cell, Path path) {
        if (cell == null || cell.getType() == null) {
            return true;
        }
        return aIsaB(cell.getType(), path.getLastClassDescriptor().getName());
    }

    /**
     * Test whether a class named "A" is, or descends from, a class named "B".
     * @param a The name of a class
     * @param b The name of a class
     * @return True if a isa b
     * @throws IllegalArgumentException if the names are not valid class names
     */
    protected boolean aIsaB(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        ClassDescriptor aCls = model.getClassDescriptorByName(a);
        ClassDescriptor bCls = model.getClassDescriptorByName(b);
        if (aCls == null || bCls == null) {
            throw new IllegalArgumentException(
                    "These names are not valid classes: a=" + a + ",b=" + b);
        }
        if (aCls.equals(bCls)) {
            return true;
        }
        return aDescendsFromB(aCls.getName(), bCls.getName());
    }

    /**
     * Test whether a class named "a" descends from a class named "b".
     * @param a the name of a class
     * @param b the name of a class
     * @return True if a descends from b
     * @throws JSONFormattingException if we can't get the super classes for a
     */
    protected boolean aDescendsFromB(String a, String b) {
        Set<String> supers;
        try {
            supers = ClassDescriptor.findSuperClassNames(model, a);
        } catch (MetaDataException e) {
            throw new JSONFormattingException("Problem getting supers for " + a, e);
        }
        if (supers.contains(b)) {
            return true;
        }
        return false;
    }

    /**
     * Sets the basic information (class and objectId) on the jsonMap provided. If the map already
     * has values, it makes sure that these are compatible with those of the result element given.
     * @param cell The result element
     * @param path The path representing the column
     * @param jsonMap The map to put the field on
     * @throws JSONFormattingException if the details do not match
     */
    protected void setOrCheckClassAndId(ResultElement cell, Path path,
            Map<String, Object> jsonMap) {

        setOrCheckClass(cell, path, jsonMap);
        setOrCheckId(cell, jsonMap);
    }

    /**
     * Set the class, or if one is already set on the map, check that this one is valid for it.
     * @param cell The result element
     * @param path The path it represents
     * @param jsonMap The map to set it onto
     */
    protected void setOrCheckClass(ResultElement cell, Path path, Map<String, Object> jsonMap) {
        String thisType = path.getLastClassDescriptor().getUnqualifiedName();
        if (jsonMap.containsKey(CLASS_KEY)) {
            String storedType = (String) jsonMap.get(CLASS_KEY);
            if (!aIsaB(cell.getType(), storedType)) {
                throw new JSONFormattingException(
                    "This result element (" + cell + ") does not belong on this map (" + jsonMap
                    + ") - classes don't match (" + cell.getType() + " ! isa "
                    + jsonMap.get(CLASS_KEY) + ")");
            }
        }

        if (isCellValidForPath(cell, path)) {
            jsonMap.put(CLASS_KEY, cell.getType());
        } else {
            throw new JSONFormattingException(
                    "This result element (" + cell + ") does not match its column because: "
                    + "classes not compatible " + "(" + thisType + " is not a superclass of "
                    + cell.getType() + ")");
        }
    }

    /**
     * Set the id, or if one is already set, check that it matches the one on the map.
     * @param cell The result element.
     * @param jsonMap The map to set it onto,
     */
    protected void setOrCheckId(ResultElement cell, Map<String, Object> jsonMap) {
        Integer cellId = cell.getId();
        if (jsonMap.containsKey(ID_KEY)) {
            Object mapId = jsonMap.get(ID_KEY);
            if (cellId != null && mapId != null && !jsonMap.get(ID_KEY).equals(cell.getId())) {
                throw new JSONFormattingException(
                    "This result element (" + cell + ") does not belong on this map (" + jsonMap
                    + ") - objectIds don't match (" + jsonMap.get(ID_KEY) + " != " + cell.getId()
                    + ")");
            }
        } else {
            // If these are simple objects, then just cross our fingers and pray...
            // TODO: fix this abomination, and actually handle simple objects properly.
            jsonMap.put(ID_KEY, cell.getId());
        }
    }

    private void addCellToJsonMap(ResultElement cell, Path column,
            Map<String, Object> rootMap) {
        if (column.isOnlyAttribute()) {
            addFieldToMap(cell, column, rootMap);
        } else {
            addReferencedCellToJsonMap(cell, column, rootMap);
        }
    }

    /**
     * Adds the attributes contained in the cell to the map given. It will not set the id attribute,
     * as that is handled by setOrCheckClassAndId.
     * @param cell The result element
     * @param column The path representing the view column
     * @param objectMap The map to put the values on
     * @throws JSONFormattingException if the map already has values for this attribute
     *                                  and they are different to the ones in the cell
     */
    protected void addFieldToMap(ResultElement cell, Path column,
            Map<String, Object> objectMap) {
        setOrCheckClassAndId(cell, column, objectMap);
        String key = column.getLastElement();
        if (ID_FIELD.equals(key)) {
            return;
        }
        Object newValue;
        if (cell.getField() instanceof Date) {
            newValue = ConstraintValueParser.ISO_DATE_FORMAT.format(cell.getField());
        } else if (cell.getField() instanceof ClobAccess) {
            newValue = cell.getField().toString();
        } else {
            newValue = cell.getField();
        }
        if (newValue instanceof CharSequence) {
            newValue = newValue.toString();
        }

        if (objectMap.containsKey(key)) {
            Object current = objectMap.get(key);
            if (current == null) {
                if (newValue != null) {
                    throw new JSONFormattingException(
                            "Trying to set key " + key + " as " + newValue
                            + " in " + objectMap + " but it already has the value "
                            + current
                    );
                }
            } else {
                if (!current.equals(newValue)) {
                    throw new JSONFormattingException(
                            "Trying to set key " + key + " as " + newValue
                            + " in " + objectMap + " but it already has the value "
                            + current);
                }
            }
        } else {
            objectMap.put(key, newValue);
        }
    }

    /**
     * Adds the information from a cell representing a reference to the map given.
     * @param cell A cell representing the end of a trail of references.
     * @param column The view column.
     * @param objectMap The map to put the nested trail of object onto
     * @throws JSONFormattingException if the paths that make up the trail
     * contain one that is not an attribute, collection or reference (the only known types at
     * present)
     */
    protected void addReferencedCellToJsonMap(ResultElement cell, Path column,
            Map<String, Object> objectMap) {
        currentMap = objectMap;
        List<Path> columnSections = column.decomposePath();
        for (Path section : columnSections) {
            if (section.isRootPath()) {
                continue;
            } else if (section.endIsAttribute()) {
                addAttributeToCurrentNode(section, cell);
            } else if (section.endIsReference()) {
                addReferenceToCurrentNode(section);
            } else if (section.endIsCollection()) {
                addCollectionToCurrentNode(section);
            } else {
                throw new JSONFormattingException(
                        "Bad path type: " + section.toString());
            }
        }

    }

    /**
     * Finds the object we should be dealing with by getting it from the current array. A search
     * is made by looking for a map which has the objectId attribute set to the same value as the
     * result element we have.
     * @param cell A result element
     * @throws JSONFormattingException if the current array is empty.
     */
    protected void setCurrentMapFromCurrentArray(ResultElement cell) {
        if (currentArray == null) {
            throw new JSONFormattingException("Nowhere to put this field");
        }
        boolean foundMap = false;
        for (Map<String, Object> obj : currentArray) {
            if (obj == null) {
                throw new JSONFormattingException("null map found in current array");
            }
            if (cell == null) {
                throw new JSONFormattingException("trying to add null cell to current array");
            }
            if (cell.getId() == null) {
                foundMap = obj.get(ID_KEY) == null;
            } else {
                foundMap = obj.get(ID_KEY).equals(cell.getId());
            }
            if (foundMap) {
                currentMap = obj;
                break;
            }
        }
        if (!foundMap) {
            Map<String, Object> newMap = new HashMap<String, Object>();
            currentArray.add(newMap);
            currentMap = newMap;
        }
        currentArray = null;
    }

    /**
     * Sets the current map to work with by getting the last one from the current array.
     * @throws JSONFormattingException if the array is null, or empty
     */
    protected void setCurrentMapFromCurrentArray() {
        try {
            currentMap = currentArray.get(currentArray.size() - 1);
        } catch (NullPointerException e) {
            throw new JSONFormattingException(
                    "Nowhere to put this reference - the current array is null", e);
        } catch (IndexOutOfBoundsException e) {
            throw new JSONFormattingException(
                    "This array is empty - is the view in the wrong order?", e);
        }
        currentArray = null;
    }

    private void addAttributeToCurrentNode(Path attributePath, ResultElement cell) {
        if (currentMap == null) {
            try {
                setCurrentMapFromCurrentArray(cell);
            } catch (JSONFormattingException e) {
                throw new JSONFormattingException("While adding processing " + attributePath, e);
            }
        }
        addFieldToMap(cell, attributePath, currentMap);
    }

    /**
     * Adds an intermediate reference to the current node.
     * @param referencePath The path representing the reference.
     * @throws JSONFormattingException if the node has this key set to an incompatible value.
     */
    protected void addReferenceToCurrentNode(Path referencePath) {
        if (currentMap == null) {
            setCurrentMapFromCurrentArray();
        }

        String key = referencePath.getLastElement();
        if (currentMap.containsKey(key)) {
            Object storedItem = currentMap.get(key);
            boolean storedItemIsMap = (storedItem instanceof Map<?, ?>);
            if (!storedItemIsMap) {
                throw new JSONFormattingException("Trying to set a reference on " + key
                    + ", but this node " + currentMap + " already "
                    + "has this key set, and to something other than a map "
                    + "(" + storedItem.getClass().getName() + ": " + storedItem + ")");
            }
            @SuppressWarnings("unchecked") // the checking happens just above.
            Map<String, Object> foundMap = (Map<String, Object>) currentMap.get(key);
            if (!foundMap.containsKey(ID_KEY)) {
                throw new JSONFormattingException(
                        "This node is not fully initialised: it has no objectId");
            }
            currentMap = foundMap;
        } else {
            Map<String, Object> newMap = new HashMap<String, Object>();
            ReferenceDescriptor refDesc =
                (ReferenceDescriptor) referencePath.getEndFieldDescriptor();
            newMap.put(CLASS_KEY, refDesc.getReferencedClassDescriptor().getUnqualifiedName());

            currentMap.put(key, newMap);
            currentMap = newMap;
        }
    }

    /**
     * Adds a new list, representing a collection to the current node (map)
     * @param collectionPath The path representing the collection.
     * @throws JSONFormattingException if the current node is not initialised, or is already set
     * with an incompatible value.
     */
    @SuppressWarnings("unchecked")
    protected void addCollectionToCurrentNode(Path collectionPath) {
        if (currentMap == null) {
            setCurrentMapFromCurrentArray();
        }
        String key = collectionPath.getLastElement();
        if (!currentMap.containsKey(ID_KEY)) {
            throw new JSONFormattingException(
                "This node is not properly initialised (it doesn't have an objectId) - "
                + "is the view in the right order?");
        }
        if (currentMap.containsKey(key)) {
            Object storedValue = currentMap.get(key);
            if (!(storedValue instanceof List<?>)) {
                throw new JSONFormattingException("Trying to set a collection on " + key
                    + ", but this node " + currentMap + " already "
                    + "has this key set to something other than a list "
                    + "(" + storedValue.getClass().getName() + ": " + storedValue + ")");
            }
        } else {
            List<Map<String, Object>> newArray = new ArrayList<Map<String, Object>>();
            currentMap.put(key, newArray);
        }
        currentArray = (List<Map<String, Object>>) currentMap.get(key);
        currentMap = null;
    }

    /**
     * Remove is not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove is not implemented in this class");
    }

}
