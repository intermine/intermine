package org.intermine.api.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;

/**
 *
 * @author Alex Kalderimis
 */
public final class TextUtil
{

    private TextUtil() {
        // Uninstantiable
    }

    /**
     * @return a random unique string
     */
    public static String generateRandomUniqueString() {
        return generateRandomUniqueString(20);
    }

    /**
     * @param length length of string
     * @return random string of determined length
     */
    public static String generateRandomUniqueString(int length) {
        String timePrefix = Long.toHexString(new Date().getTime());
        String randomSuffix = RandomStringUtils.randomAlphanumeric(length);

        StringBuilder sb = new StringBuilder();

        // Interleave the random and predictable portions so that
        // the time string isn't so obvious.
        int tpl = timePrefix.length();
        int rsl = randomSuffix.length();
        for (int i = 0; i < tpl || i < rsl; i++) {
            if (i < randomSuffix.length()) {
                sb.append(randomSuffix.charAt(i));
            }
            if (i < timePrefix.length()) {
                sb.append(timePrefix.charAt(i));
            }
        }

        return sb.substring(0, length);
    }
}
