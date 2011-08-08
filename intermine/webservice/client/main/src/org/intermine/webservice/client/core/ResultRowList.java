package org.intermine.webservice.client.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.json.JSONArray;
import org.json.JSONException;

public class ResultRowList implements List<Object>, Iterable<Object> {

	private final JSONArray data;
	
	public ResultRowList(JSONArray ja) {
		this.data = ja;
	}
	
	public ResultRowList(String input) {
		try {
			this.data = new JSONArray(input);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean add(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int arg0, Object arg1) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean addAll(Collection<? extends Object> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends Object> arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean contains(Object value) {
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
			if (thingy.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		boolean c = true;
		for (Object o : arg0) {
			c = c && contains(o);
		}
		return c;
	}

	@Override
	public Object get(int arg0) {
		if (arg0 < 0 || arg0 >= data.length()) {
			throw new IndexOutOfBoundsException();
		}
		try {
			return data.getJSONObject(arg0).get("value");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int indexOf(Object arg0) {
		for (int i = 0; i < data.length(); i++) {
			Object thingy;
			try {
				thingy = data.getJSONObject(i).get("value");
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			if (thingy == null && arg0 == null) {
				return i;
			} else if (thingy != null && thingy.equals(arg0)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean isEmpty() {
		return data.length() == 0;
	}

	@Override
	public Iterator<Object> iterator() {
		List<Object> values = getValuesInternal();
		return values.iterator();
	}

	@Override
	public int lastIndexOf(Object arg0) {
		for (int i = data.length() - 1; i >= 0; i--) {
			Object thingy;
			try {
				thingy = data.getJSONObject(i).get("value");
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			if (thingy == null && arg0 == null) {
				return i;
			} else if (thingy != null && thingy.equals(arg0)) {
				return i;
			}
		}
		return -1;
	}
	
	private List<Object> getValuesInternal() {
		List<Object> values = new ArrayList<Object>();
		for (int i = 0; i < data.length(); i++) {
			try {
				values.add(data.getJSONObject(i).get("value"));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}			
		}
		return values;
	}

	@Override
	public ListIterator<Object> listIterator() {
		return getValuesInternal().listIterator();
	}

	@Override
	public ListIterator<Object> listIterator(int arg0) {
		return getValuesInternal().listIterator(arg0);
	}

	@Override
	public boolean remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object set(int arg0, Object arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return data.length();
	}

	@Override
	public List<Object> subList(int arg0, int arg1) {
		return getValuesInternal().subList(arg0, arg1);
	}

	@Override
	public Object[] toArray() {
		return getValuesInternal().toArray();
	}

	@Override
	public <T> T[] toArray(T[] arg0) {	
		return getValuesInternal().toArray(arg0);
	}

}
