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

import java.security.PublicKey;
import java.util.Collection;

/**
 * An object that can provide public keys by name, or all known keys, or a subset of keys.
 * @author Alex Kalderimis
 *
 */
public interface PublicKeySource {

	public PublicKey get(String name) throws KeySourceException;

	public Collection<PublicKey> getAll() throws KeySourceException;

	public Collection<PublicKey> getSome(String... names) throws KeySourceException;
}
