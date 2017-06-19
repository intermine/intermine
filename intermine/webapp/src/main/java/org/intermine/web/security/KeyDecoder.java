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

/**
 * Classes that can decode public keys from strings.
 * @author Alex Kalderimis
 *
 */
public interface KeyDecoder
{

    /**
     * Decode the string and make the key.
     * @param input The public key, in an encoded form.
     * @return the decoded key.
     * @throws DecodingException If we cannot decode the key.
     */
    PublicKey decode(String input) throws DecodingException;

}
