package org.intermine.webservice.server;

import junit.framework.TestCase;

public class StatusDictionaryTest extends TestCase {

    public void testStatuses() {

        assertEquals(
                "403 Forbidden.",
                StatusDictionary.getDescription(403)
            );

        assertEquals(
                "500 Internal server error.",
                StatusDictionary.getDescription(500)
            );

        assertEquals(
                "400 Bad request. There was a problem with your request parameters:",
                StatusDictionary.getDescription(400)
            );

        assertEquals(
                "404 Resource not found.",
                StatusDictionary.getDescription(404)
            );

        assertEquals(
                "200 OK",
                StatusDictionary.getDescription(200)
            );
        assertEquals(
                "204 Resource representation is empty.",
                StatusDictionary.getDescription(204)
            );
        assertEquals(
                "506 Unknown Status",
                StatusDictionary.getDescription(506)
            );
    }

}
