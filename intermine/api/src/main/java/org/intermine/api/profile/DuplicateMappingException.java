package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 *
 * @author Alex
 *
 */
public class DuplicateMappingException extends RuntimeException
{
    private static final long serialVersionUID = -7950202126281601571L;
    private static final String MESSAGE_FMT =
            "No two users may have the same value for '%s', but the value '%s' is already taken";

    /**
     * @param key key
     * @param value value
     */
    public DuplicateMappingException(String key, String value) {
        super(String.format(MESSAGE_FMT, key, value));
    }
}
