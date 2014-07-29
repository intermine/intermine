package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.intermine.test.util.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Class to query friendly mines.
 *
 * @author Alex Kalderimis
 *
 */
public final class FriendlyMineQueryRunnerTest
{

    private FriendlyMineQueryRunner queryRunner;
    private MockRequester requester;
    private Map<String, Mine> mines; 

    private Mine mine;

    private class MockRequester implements MineRequester
    {

        String queryResult = null, urlResult = null;

        @Override
        public BufferedReader runQuery(Mine mine, String xmlQuery) {
            return queryResult == null ? null : new BufferedReader(new StringReader(queryResult));
        }

        @Override
        public BufferedReader requestURL(String urlString) {
            return urlResult == null ? null : new BufferedReader(new StringReader(urlResult));
        }

    }

    @Before
    public void setUp() {
        requester = new MockRequester();
        queryRunner = new FriendlyMineQueryRunner(requester);
        mine = new Mine("dummymine");
        mines = new HashMap<String, Mine>();
        mines.put("foo", mine);
    }

    @Test
    public void testRunJSONWebServiceQueryNoResults() throws IOException, JSONException {
        JSONObject result = queryRunner.runJSONWebServiceQuery(mine, "FOO");
        assertNull(result);
    }

    @Test
    public void testSomeResults() throws IOException, JSONException {
        requester.queryResult = "{\"results\":[[123,\"foo\"],[456,\"bar\"]]}";
        String expected = "{\"results\":[{\"id\":123,\"name\":\"foo\"},{\"id\":456,\"name\":\"bar\"}]}";
        JSONObject result = queryRunner.runJSONWebServiceQuery(mine, "FOO");
        assertEquals(expected, result.toString());
    }

    @Test
    public void testSomeResults3Columns() throws IOException, JSONException {
        requester.queryResult = "{\"results\":[[123,\"foo\",\"x\"],[456,\"bar\",\"y\"]]}";
        String expected = "{\"results\":[{\"id\":123,\"name\":\"foo\",\"ref\":\"x\"},{\"id\":456,\"name\":\"bar\",\"ref\":\"y\"}]}";
        JSONObject result = queryRunner.runJSONWebServiceQuery(mine, "FOO");
        Collection<String> problems = JSONUtils.jsonObjsAreEqual(new JSONObject(expected), result);
        assertTrue(problems.toString(), problems.isEmpty());
    }

    @Test
    public void handleBadJSON() throws IOException, JSONException {
        requester.queryResult = "I am not Jason";
        try {
            queryRunner.runJSONWebServiceQuery(mine, "FOO");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // Have to be careful - don't want to catch out-of-memory errors.
            if (!e.getMessage().contains("Error reading results")) {
                throw e;
            }
        }
    }

    @Test
    public void testUpdateReleaseVersion() {
        requester.urlResult = "foobar";
        queryRunner.updateReleaseVersion(mines);
        assertEquals("foobar", mine.getReleaseVersion());
    }

    @Test
    public void testUpdateReleaseVersionNoResults() {
        queryRunner.updateReleaseVersion(mines);
        assertNull(mine.getReleaseVersion());
    }

    @Test
    public void testCachingStrategy() throws IOException, JSONException {
        requester.urlResult = "foobar";
        requester.queryResult = "{\"results\":[[123,\"foo\"],[456,\"bar\"]]}";
        String expected = "{\"results\":[{\"id\":123,\"name\":\"foo\"},{\"id\":456,\"name\":\"bar\"}]}";
        String expected2 = "{\"results\":[{\"id\":789,\"name\":\"zop\"}]}";
        queryRunner.updateReleaseVersion(mines);
        JSONObject result = queryRunner.runJSONWebServiceQuery(mine, "FOO");
        assertEquals(expected, result.toString());
        requester.queryResult = "Not the same, in fact totally not even json";
        JSONObject result2 = queryRunner.runJSONWebServiceQuery(mine, "FOO");
        // Expect cached result.
        assertEquals(expected, result2.toString());
        assertTrue(result == result2);
        requester.urlResult = "quux";
        requester.queryResult = "{\"results\":[[789,\"zop\"]]}";
        // Invalidate the cache by updating the release version.
        queryRunner.updateReleaseVersion(mines);
        JSONObject result3 = queryRunner.runJSONWebServiceQuery(mine, "FOO");
        assertEquals("quux", mine.getReleaseVersion());
        assertEquals(expected2, result3.toString());
        assertTrue(result != result3);
    }

    @Test
    public void testRunWebServiceQueryNull() {
        BufferedReader res = queryRunner.runWebServiceQuery(null);
        assertNull(res);
    }

    @Test
    public void testRunWebServiceQuery() throws IOException {
        requester.urlResult = "fibble";
        BufferedReader res = queryRunner.runWebServiceQuery("http://boo.com");
        assertEquals("fibble", IOUtils.toString(res));
    }
}

