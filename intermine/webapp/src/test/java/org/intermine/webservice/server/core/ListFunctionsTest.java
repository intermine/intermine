package org.intermine.webservice.server.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ListFunctionsTest {

    private final class GetLengths implements Function<String, Integer> {
        @Override
        public Integer call(String a) {
            return a.length();
        }
    }

    @Test
    public void testMap() {
        List<String> names = Arrays.asList("joe", "carol", "anne-marie");
        List<Integer> lengths = ListFunctions.map(names, new GetLengths());
        assertEquals(lengths, Arrays.asList(3, 5, 10));
    }

}
