package org.intermine.webservice.server;

import java.security.PublicKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.web.security.KeySourceException;
import org.intermine.web.security.PublicKeySource;

public class MapPublicKeySource implements PublicKeySource {

	private Map<String, PublicKey> keys;

	public MapPublicKeySource(Map<String, PublicKey> keys) {
		this.keys = keys;
	}

	@Override
	public PublicKey get(String name) throws KeySourceException {
		return keys.get(name);
	}

	@Override
	public Collection<PublicKey> getAll() throws KeySourceException {
		return keys.values();
	}

	@Override
	public Collection<PublicKey> getSome(String... names) throws KeySourceException {
		Set<PublicKey> ret = new HashSet<PublicKey>();
		for (String name: names) {
			ret.add(keys.get(name));
		}
		return ret;
	}

}
