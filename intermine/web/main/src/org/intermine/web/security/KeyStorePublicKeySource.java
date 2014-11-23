package org.intermine.web.security;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class KeyStorePublicKeySource implements PublicKeySource
{

	private KeyStore store;

	public KeyStorePublicKeySource(KeyStore store) {
		this.store = store;
	}

	@Override
	public PublicKey get(String name) throws KeySourceException {
		Certificate cert;
		try {
			cert = store.getCertificate(name);
		} catch (KeyStoreException e) {
			throw new KeySourceException(e);
		}
		return cert.getPublicKey();
	}

	@Override
	public Collection<PublicKey> getAll() throws KeySourceException {
		Set<PublicKey> keys = new HashSet<PublicKey>();
		try {
			for (Enumeration<String> aliases = store.aliases(); aliases.hasMoreElements();) {
				keys.add(get(aliases.nextElement()));
			}
		} catch (KeyStoreException e) {
			throw new KeySourceException(e);
		}
		return keys;
	}

	@Override
	public Collection<PublicKey> getSome(String... names) throws KeySourceException {
		Set<PublicKey> keys = new HashSet<PublicKey>();
		for (String name: names) {
			keys.add(get(name));
		}
		return keys;
	}

}
