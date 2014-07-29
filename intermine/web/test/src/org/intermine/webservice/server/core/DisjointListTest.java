package org.intermine.webservice.server.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class DisjointListTest {

    @Test
    public void testCreate() {
        DisjointList<Integer, String> numsAndStrings =
                new DisjointList<Integer, String>();
        numsAndStrings.addLeft(1);
        numsAndStrings.addRight("two");
        numsAndStrings.addLeft(3);
        numsAndStrings.addRight("four");
        assertEquals(numsAndStrings.size(), 4);
        @SuppressWarnings("unchecked")
        List<Either<Integer, String>> expected = Arrays.asList(
                new Either.Left<Integer, String>(1),
                new Either.Right<Integer, String>("two"),
                new Either.Left<Integer, String>(3),
                new Either.Right<Integer, String>("four"));
        assertEquals(expected, numsAndStrings);
        Collections.reverse(numsAndStrings);
        assertTrue(!expected.equals(numsAndStrings));
    }

}
