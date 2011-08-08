package org.intermine.webservice.client.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;

public class ResultRowMap implements Map<String, Object> {

	private final JSONArray data;
	private final List<String> views;
	
	public ResultRowMap(JSONArray ja, List<String> views) {
		this.data = ja;
		this.views = views;
        verify();
	}

    public ResultRowMap(String json, List<String> views) {
        try {
            this.data = new JSONArray(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        this.views = views;
        verify();
    }

    private void verify() {
		if (data == null || views == null || data.length() != views.size()) {
			throw new IllegalArgumentException();
		}
    }

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return data.length() == 0;
	}

	@Override
	public int size() {
		return data.length();
	}
	
	@Override
	public Object remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		return views.contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (int i = 0; i < data.length(); i++) {
			Object thingy;
			try {
				thingy = data.getJSONObject(i).get("value");
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			if (thingy == null && value == null) {
				return true;
			}
			if (thingy != null && thingy.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		Set<Entry<String, Object>> entries = new LinkedHashSet<Entry<String, Object>>();
		for (int i = 0; i < data.length(); i++) {
			String key = views.get(i);
			Object val;
			try {
				val = data.getJSONObject(i).get("value");
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			entries.add(new RowEntry(key, val));
		}
		return entries;
	}

    private String getRoot() {
        return views.get(0).split("\\.")[0];
    }

	@Override
	public Object get(Object key) {
		if (!views.contains(key)) {
            String root = getRoot();
            key = root + "." + key;
            if (!views.contains(key)) {
    			return null;
            }
		}
		int i = views.indexOf(key);
		try {
			return data.getJSONObject(i).get("value");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> keySet() {
		return new TreeSet<String>(views);
	}

	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Object> values() {
		Collection<Object> values = new ArrayList<Object>();
		for (int i = 0; i < data.length(); i++) {
			try {
				values.add(data.getJSONObject(i).get("value"));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}			
		}
		return values;
	}
	
	private static class RowEntry implements Entry<String, Object> {

		private final String key;
		private final Object o;
		
		public RowEntry(String k, Object v) {
			this.key = k;
			this.o = v;
		}
		
		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return o;
		}

		@Override
		public Object setValue(Object arg0) {
			throw new UnsupportedOperationException();
		}
		
	}

}
