package org.intermine.api.util;

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;

public class TextUtil {

	private TextUtil() {
		// Uninstantiable
	}
	
	public static String generateRandomUniqueString() {
		return generateRandomUniqueString(20);
	}
	
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
