package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Util;

import junit.framework.TestCase;

/**
 * Tests for the WebUtil class.
 *
 * @author Kim Rutherford
 */

public class WebUtilTest extends TestCase
{
    public WebUtilTest (String arg) {
        super(arg);
    }

    public void testWildcardSqlToUser() throws Exception {
        String value = Util.wildcardSqlToUser("a");
        assertEquals("wildcardSqlToUser(a)", "a", value);

        value = Util.wildcardSqlToUser("a%");
        assertEquals("wildcardSqlToUser(a%)", "a*", value);

        value = Util.wildcardSqlToUser("%a");
        assertEquals("wildcardSqlToUser(%a)", "*a", value);

        value = Util.wildcardSqlToUser("a\\\\a");
        assertEquals("wildcardSqlToUser(a\\\\a)", "a\\a", value);

        value = Util.wildcardSqlToUser("\\%a");
        assertEquals("wildcardSqlToUser(\\%a)", "%a", value);

        value = Util.wildcardSqlToUser("_a");
        assertEquals("wildcardSqlToUser(_a)", "?a", value);

        value = Util.wildcardSqlToUser("\\_a");
        assertEquals("wildcardSqlToUser(\\_a)", "_a", value);

        value = Util.wildcardSqlToUser("?a");
        assertEquals("wildcardSqlToUser(?a)", "\\?a", value);

        value = Util.wildcardSqlToUser("*a");
        assertEquals("wildcardSqlToUser(*a)", "\\*a", value);

        value = Util.wildcardSqlToUser("*?%_\\%\\_");
        assertEquals("wildcardSqlToUser(*?%_\\%\\_)", "\\*\\?*?%_", value);
    }


    public void testWildcardUserToSql() throws Exception {
        String value = Util.wildcardUserToSql("a");
        assertEquals("wildcardUserToSql(a)", "a", value);

        value = Util.wildcardUserToSql("a*");
        assertEquals("wildcardUserToSql(a*)", "a%", value);

        value = Util.wildcardUserToSql("*a");
        assertEquals("wildcardUserToSql(*a)", "%a", value);

        value = Util.wildcardUserToSql("a\\a");
        assertEquals("wildcardUserToSql(a\\a)", "a\\\\a", value);

        value = Util.wildcardUserToSql("\\*a");
        assertEquals("wildcardUserToSql(\\*a)", "*a", value);

        value = Util.wildcardUserToSql("?a");
        assertEquals("wildcardUserToSql(?a)", "_a", value);

        value = Util.wildcardUserToSql("\\?a");
        assertEquals("wildcardUserToSql(\\?a)", "?a", value);

        value = Util.wildcardUserToSql("_a");
        assertEquals("wildcardSqlToUser(_a)", "\\_a", value);

        value = Util.wildcardUserToSql("%a");
        assertEquals("wildcardSqlToUser(%a)", "\\%a", value);

        value = Util.wildcardUserToSql("*?%_\\*\\?");
        assertEquals("wildcardUserToSql(*?%_\\*\\?\\%\\_)", "%_\\%\\_*?", value);

        // flybase example
        value = Util.wildcardUserToSql("Dpse\\GA10108");
        assertEquals("wildcardSqlToUser(Dpse\\GA10108)", "Dpse\\\\GA10108", value);
    }

    public void testWildcardRoundTrip() {
        String[] testStrings = {
            "*?%_\\*\\?", "a?b*d\\?e\\*f%g_h",
            "\\\\*?%_a %_\\*..whuiwefhuw\\?\\?\\?????\\??||???? <>",
            "Dpse\\GA10108"
        };

        for (int i = 0; i < testStrings.length; i++) {
            assertEquals("testing: " + testStrings[i], testStrings[i],
                         Util.wildcardSqlToUser(Util.wildcardUserToSql(testStrings[i])));
        }
    }
}
