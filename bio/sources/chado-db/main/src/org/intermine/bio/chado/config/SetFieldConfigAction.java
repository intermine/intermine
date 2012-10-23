package org.intermine.bio.chado.config;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.regex.Pattern;

/**
 * An action that sets an attribute in a new Item.
 * @author Kim Rutherford
 */
public class SetFieldConfigAction extends MatchingFieldConfigAction
{
    private final String theFieldName;

    /**
     * Create a new SetFieldConfigAction that sets the given field.
     * @param fieldName the name of the InterMine object field to set
     */
    public SetFieldConfigAction(String fieldName) {
        super(null);
        this.theFieldName = fieldName;
    }

    /**
     * Create a new SetFieldConfigAction that sets the given field.  The value to set must
     * match the pattern or the field will not be set.
     * @param fieldName the name of the InterMine object field to set
     * @param pattern the pattern to match
     */
    public SetFieldConfigAction(String fieldName, Pattern pattern) {
        super(pattern);
        this.theFieldName = fieldName;
    }

    /**
     * Return the field name that was passed to the constructor.
     * @return the field name
     */
    public String getFieldName() {
        return theFieldName;
    }
}
