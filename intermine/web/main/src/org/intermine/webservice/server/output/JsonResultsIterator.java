package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.export.ResultElementConverter;
import org.json.JSONObject;

public class JsonResultsIterator implements Iterator<JSONObject> {

	private static final String CLASS_KEY = "class";
	private static final String ID_KEY = "objectId";

	private ExportResultsIterator subIter;
	private List<String> views;
	private List<ResultElement> holdOver;
	private List<Path> viewPaths;
	private transient Map<String, Object> currentMap;
	private transient List<Map<String, Object>> currentArray;

	public JsonResultsIterator(ExportResultsIterator it,
			List<String> viewPathStrings) {
		this.subIter = it;
		this.views = viewPathStrings;
		init();
	}

	private void init() {
		for (String view : views) {
			Path path = new Path(model, view);
			viewPaths.add(path);
		}
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
		setOrCheckClassAndId(results.get(0), jsonMap);
		for (int i = 0; i < results.size(); i++) {
			ResultElement cell = results.get(i);
			Path columnPath = viewPaths.get(i);
			addCellToJsonMap(cell, columnPath, jsonMap);
		}
	}

	private void setOrCheckClassAndId(ResultElement cell,
			Map<String, Object> jsonMap) {
		if (jsonMap.containsKey(CLASS_KEY)) {
			if (!jsonMap.get(CLASS_KEY).equals(cell.getType())) {
				throw new RuntimeException(
						"This result element does not belong on this map - classes don't match");
			}
		} else {
			jsonMap.put(CLASS_KEY, cell.getType());
		}
		if (jsonMap.containsKey(ID_KEY)) {
			if (!jsonMap.get(ID_KEY).equals(cell.getId())) {
				throw new RuntimeException(
						"This result element does not belong on this map - ids don't match");
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

	private void addFieldToMap(ResultElement cell, Path column,
			Map<String, Object> objectMap) {
		setOrCheckClassAndId(cell, objectMap);
		String key = column.getLastElement();
		if (objectMap.containsKey(key)) {
			if (! objectMap.get(key).equals(cell.getField())) {
				throw new RuntimeException("This field is already set in this map, but to a different value");
			}
		} else {
			String value = cell.getField().toString();
			objectMap.put(key, value);	
		}
	}

	private void addReferencedCellToJsonMap(ResultElement cell, Path column,
			Map<String, Object> objectMap) {
		currentMap = objectMap;
		List<Path> columnSections = column.decomposePath();
		try {
			for (Path section : columnSections) {
				if (section.isRootPath()) {
					continue;
				} else if (section.endIsAttribute()) {
					addAttributeToCurrentNode(section, cell);
				} else if (section.endIsReference()) {
					addReferenceToCurrentNode(section, cell);
				} else if (section.endIsCollection()) {
					addCollectionToCurrentNode(section, cell);
				} else {
					throw new RuntimeException("Bad path type: "
							+ section.toString());
				}
			}
		} catch(Error e) {
			throw new RuntimeException(e);
		} finally {
			currentMap = null;
		}
	}
	
	private void setCurrentMapFromCurrentArray(ResultElement cell) {
		if (currentArray == null) {
			throw new RuntimeException("Nowhere to put this field");
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
	
	private void setCurrentMapFromCurrentArray() {
		if (currentArray == null) {
			throw new RuntimeException("Nowhere to put this reference");
		} 
		if (currentArray.isEmpty()) {
			throw new RuntimeException("There are no maps in this array");
		}
		currentMap = currentArray.get(currentArray.size() - 1);
		currentArray = null;
	}

	private void addAttributeToCurrentNode(Path attributePath, ResultElement cell) {
		if (currentMap == null) {
			setCurrentMapFromCurrentArray(cell);
		}
		addFieldToMap(cell, attributePath, currentMap);
	}

	private void addReferenceToCurrentNode(Path referencePath, ResultElement cell) {
		if (currentMap == null) {
			setCurrentMapFromCurrentArray();
		}
		
		String key = referencePath.getLastElement();
		if (currentMap.containsKey(key)) {
			if ( ! (currentMap.get(key) instanceof Map<?,?>)) {
				throw new RuntimeException("Trying to set a reference, but this node already "
						+ "has this key set, and to something other than a map");
			}
		} else {
			Map<String, Object> newMap = new HashMap<String, Object>();
			currentMap.put(key, newMap);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addCollectionToCurrentNode(Path collectionPath, ResultElement cell) {
		if (currentMap == null) {
			setCurrentMapFromCurrentArray();
		}
		String key = collectionPath.getLastElement();
		if (currentMap.containsKey(key)) {
			if (! (currentMap.get(key) instanceof List<?>)) {
				throw new RuntimeException("Trying to set a collection, but this node already "
						+ "has this key set to something other than a list");
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
		// TODO Auto-generated method stub

	}

}
