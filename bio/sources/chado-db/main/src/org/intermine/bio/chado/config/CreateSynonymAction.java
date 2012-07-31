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
 * An action that sets a Synonym.
 *
 * @author Kim Rutherford
 */
public class CreateSynonymAction extends MatchingFieldConfigAction
{
    /**
     * Make a synonym
     */
    public CreateSynonymAction() {
        super(null);
    }

    /**
     * Make a synonym and use the type from chado ("symbol", "identifier" etc.) as the Synonym
     * type.  Only create the synonym if the pattern matches.
     * @param pattern the pattern that the value must match
     */
    public CreateSynonymAction(Pattern pattern) {
        super(pattern);
    }
}
