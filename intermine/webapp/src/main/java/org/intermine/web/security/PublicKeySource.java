package org.intermine.web.security;

/*
 * Copyright (C) 2002-2016 FlyMine
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
public interface PublicKeySource
{

    /**
     * Get a specific key by name.
     * @param name The name of the key.
     * @return The key.
     * @throws KeySourceException If we can't get that key.
     */
    PublicKey get(String name) throws KeySourceException;

    /**
     * @return All the keys in this key source.
     * @throws KeySourceException If we have issues getting any of them.
     */
    Collection<PublicKey> getAll() throws KeySourceException;

    /**
     * Get multiple keys given a set of names.
     * @param names The names of the keys we want.
     * @return The matching keys.
     * @throws KeySourceException If we can't get any of the keys.
     */
    Collection<PublicKey> getSome(String... names) throws KeySourceException;
}
