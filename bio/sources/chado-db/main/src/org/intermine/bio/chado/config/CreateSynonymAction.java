package org.intermine.bio.chado.config;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.regex.Pattern;

/**
 * An action that sets a Synonym.
 * @author Kim Rutherford
 */
public class CreateSynonymAction extends MatchingFieldConfigAction
{
    private final String synonymType;

    /**
     * Make a synonym and use the type from chado ("symbol", "identifier" etc.) as the Synonym
     * type
     */
    public CreateSynonymAction() {
        super(null);
        synonymType = null;
    }

    /**
     * Make a synonym and use the type from chado ("symbol", "identifier" etc.) as the Synonym
     * type.  Only create the synonym if the pattern matches.
     * @param pattern the pattern that the value must match
     */
    public CreateSynonymAction(Pattern pattern) {
        super(pattern);
        synonymType = null;
    }

    /**
     * Make a synonym and use given type as the Synonym type
     * @param synonymType the synonym type
     */
    public CreateSynonymAction(String synonymType) {
        super(null);
        this.synonymType = synonymType;
    }

    /**
     * Return the synonym type that was passed to the constructor.
     * @return the synonym type
     */
    public String getSynonymType() {
        return synonymType;
    }
}
