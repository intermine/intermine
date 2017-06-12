package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;

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
 * Class for tokenising identifier input.
 * @author Alex Kalderimis
 *
 */
public final class LookupTokeniser
{
    private final StrMatcher charSetMatcher;

    private LookupTokeniser(boolean spaceIsSeparator) {
        if (spaceIsSeparator) {
            charSetMatcher = StrMatcher.charSetMatcher(" \n\t,");
        } else {
            charSetMatcher = StrMatcher.charSetMatcher("\n\t,");
        }
    }

    /**
     * Return a tokeniser suitable for parsing tokens received from a lookup constraint context.
     * @return A tokeniser that treats the unquoted space character as a normal character.
     */
    public static LookupTokeniser getLookupTokeniser() {
        return new LookupTokeniser(false);
    }

    /**
     * Return a tokeniser suitable for parsing tokens received from a list upload context.
     * @return A tokeniser that treats the unquoted space character as a delimiter.
     */
    public static LookupTokeniser getListUploadTokeniser() {
        return new LookupTokeniser(true);
    }

    /**
     * Transform an input string into a list of identifiers.
     * @param input A comma, new line, or tab delimited set of identifiers,
     * with optional double quoting.
     * @return A list of identifiers.
     */
    public List<String> tokenise(String input) {
        List<String> ret = new LinkedList<String>();

        StrTokenizer tokeniser = new StrTokenizer(input, charSetMatcher);
        tokeniser.setQuoteChar('"');
        tokeniser.setIgnoreEmptyTokens(true);
        tokeniser.setTrimmerMatcher(StrMatcher.trimMatcher());
        while (tokeniser.hasNext()) {
            String token = tokeniser.nextToken().trim();
            ret.add(token);
        }
        return ret;
    }
}
