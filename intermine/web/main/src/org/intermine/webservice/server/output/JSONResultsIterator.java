package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2010 FlyMine
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

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.json.JSONObject;

public class JSONResultsIterator implements Iterator<JSONObject> {

	private static final String CLASS_KEY = "class";
	private static final String ID_KEY = "objectId";
	private static final String ID_FIELD = "id";

	private ExportResultsIterator subIter;
	private List<ResultElement> holdOver;
	private List<Path> viewPaths = new ArrayList<Path>();
	protected transient Map<String, Object> currentMap;
	protected transient List<Map<String, Object>> currentArray;

	public JSONResultsIterator(ExportResultsIterator it) {
		this.subIter = it;
		init();
	}

	private void init() {
		List<String> views = subIter.getQuery().getView();
		viewPaths.addAll(subIter.getViewPaths());
	}

	@Override
	public boolean hasNext() {
		if (subIter.hasNext() || holdOver != null) {
			return true;
		}
		return false;
	}

	@Override
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
			System.err.println(result.toString());
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
		setOrCheckClassAndId(results.get(0), jsonMap);
		if (! jsonMap.get(CLASS_KEY).equals(viewPaths.get(0).getStartClassDescriptor().getUnqualifiedName())) {
			throw new JSONFormattingException(
					"Head of the object is missing");
		}
		for (int i = 0; i < results.size(); i++) {
			ResultElement cell = results.get(i);
			if (cell == null || cell.getType() == null) {
				continue;
			}
			Path columnPath = viewPaths.get(i);
			addCellToJsonMap(cell, columnPath, jsonMap);
		}
	}

	protected void setOrCheckClassAndId(ResultElement cell,
			Map<String, Object> jsonMap) {
		if (jsonMap.containsKey(CLASS_KEY)) {
			String storedType = (String) jsonMap.get(CLASS_KEY);
			String cellType = cell.getType();
			if (!storedType.equals(cellType)) {
				throw new JSONFormattingException(
						"This result element (" + cell + ") does not belong on this map (" + jsonMap + 
						") - classes don't match (" + jsonMap.get(CLASS_KEY) + " != " + cell.getType() + ")");
			}
		} else {
			jsonMap.put(CLASS_KEY, cell.getType());
		}
		if (jsonMap.containsKey(ID_KEY)) {
			if (!jsonMap.get(ID_KEY).equals(cell.getId())) {
				throw new JSONFormattingException(
						"This result element (" + cell + ") does not belong on this map (" + jsonMap + 
						") - objectIds don't match (" + jsonMap.get(ID_KEY) + " != " + cell.getId() + ")");
			}
		} else {
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

	protected void addFieldToMap(ResultElement cell, Path column,
			Map<String, Object> objectMap) {
		setOrCheckClassAndId(cell, objectMap);
		String key = column.getLastElement();
		if (ID_FIELD.equals(key)) {
			return;
		}
		Object newValue;
		if (cell.getField() instanceof Date) {
			newValue = ConstraintValueParser.ISO_DATE_FORMAT.format(cell.getField());
		} else {
			newValue = cell.getField();
		}
		if (objectMap.containsKey(key)) {
			if (! newValue.toString().equals(objectMap.get(key).toString())) {
				throw new JSONFormattingException("Trying to set key " + key + " as " + cell.getField() + " in " + objectMap + " but it already has the value " + objectMap.get(key));
			}
		} else {
			objectMap.put(key, newValue);	
		}
	}

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
						"Bad path type: "+ section.toString());
			}
		}
		
	}
	
	protected void setCurrentMapFromCurrentArray(ResultElement cell) {
		if (currentArray == null) {
			throw new JSONFormattingException("Nowhere to put this field");
		}
		boolean foundMap = false;
		for (Map<String, Object> obj : currentArray) {
			foundMap = obj.get(ID_KEY).equals(cell.getId()); 
			if (foundMap) {
				currentMap = obj;
				break;
			}
		}
		if (! foundMap) {
			Map<String, Object> newMap = new HashMap<String, Object>();
			currentArray.add(newMap);
			currentMap = newMap;
		}
		currentArray = null;
	}
	
	protected void setCurrentMapFromCurrentArray() {
		try {
			currentMap = currentArray.get(currentArray.size() - 1);
		} catch (NullPointerException e) {
			throw new JSONFormattingException("Nowhere to put this reference", e);
		} catch (IndexOutOfBoundsException e) {
			throw new JSONFormattingException("This array is empty - is the view in the wrong order?", e);
		}
		currentArray = null;
	}

	private void addAttributeToCurrentNode(Path attributePath, ResultElement cell) {
		if (currentMap == null) {
			setCurrentMapFromCurrentArray(cell);
		}
		addFieldToMap(cell, attributePath, currentMap);
	}

	protected void addReferenceToCurrentNode(Path referencePath) {
		if (currentMap == null) {
			throw new JSONFormattingException("The current map should have been set by a preceding attribute - is the view in the right order?");
		}
		
		String key = referencePath.getLastElement();
		if (currentMap.containsKey(key)) {
			Object storedItem = currentMap.get(key);
			boolean storedItemIsMap = storedItem instanceof Map<?,?>;
			if ( ! storedItemIsMap) {
				throw new JSONFormattingException("Trying to set a reference on " + key + 
						", but this node " + currentMap + " already "
						+ "has this key set, and to something other than a map " +
						"(" + storedItem.getClass().getName() + ": " + storedItem + ")");
			}
			Map<String, Object> foundMap = (Map<String, Object>) currentMap.get(key);
			if (! foundMap.containsKey(ID_KEY)) {
				throw new JSONFormattingException("This node is not fully initialised: it has no objectId");
			}
			currentMap = foundMap;
		} else {
			Map<String, Object> newMap = new HashMap<String, Object>();
			ReferenceDescriptor refDesc = (ReferenceDescriptor) referencePath.getEndFieldDescriptor();
			newMap.put(CLASS_KEY, refDesc.getReferencedClassDescriptor().getUnqualifiedName());
			
			currentMap.put(key, newMap);
			currentMap = newMap;
		}
	}
	
	/**
	 * Adds a new list, representing a collection to the current node (map)
	 * @param collectionPath
	 */
	@SuppressWarnings("unchecked")
	protected void addCollectionToCurrentNode(Path collectionPath) {
		if (currentMap == null) {
			setCurrentMapFromCurrentArray();
		}
		String key = collectionPath.getLastElement();
		if (! currentMap.containsKey(ID_KEY)) {
			throw new JSONFormattingException("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?");
		}
		if (currentMap.containsKey(key)) {
			Object storedValue = currentMap.get(key);
			if (! (storedValue instanceof List<?>)) {
				throw new JSONFormattingException("Trying to set a collection on " + key + 
						", but this node " + currentMap + " already "
						+ "has this key set to something other than a list " +
						"(" + storedValue.getClass().getName() + ": " + storedValue + ")");
			}
		} else {
			List<Map<String, Object>> newArray = new ArrayList<Map<String, Object>>();
			currentMap.put(key, newArray);
		}
		currentArray = (List<Map<String, Object>>) currentMap.get(key);
		currentMap = null;		
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove is not implemented in this class");
	}

}
