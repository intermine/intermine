package org.intermine.bio.webservice;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.webservice.server.exceptions.BadRequestException;
import org.junit.Before;
import org.junit.Test;

public class FastaQueryServiceTest {

    private Map<String, Integer> extensionsToValues;
    private Set<String> badExtensions;

    @Before
    public void setup() {
        extensionsToValues = new HashMap<String, Integer>();
        extensionsToValues.put("", 0);
        extensionsToValues.put(null, 0);
        extensionsToValues.put("   ", 0);
        extensionsToValues.put("1", 1);
        extensionsToValues.put("020", 20);
        extensionsToValues.put("123", 123);
        extensionsToValues.put("1b", 1);
        extensionsToValues.put("020b", 20);
        extensionsToValues.put("123b", 123);
        extensionsToValues.put("1bp", 1);
        extensionsToValues.put("020bp", 20);
        extensionsToValues.put("123bp", 123);
        extensionsToValues.put("1kbp", 1000);
        extensionsToValues.put("1.25kbp", 1250);
        extensionsToValues.put("01.234kbp", 1234);
        extensionsToValues.put("300kbp", 300000);
        extensionsToValues.put("1mbp", 1000000);
        extensionsToValues.put("010mbp", 10000000);
        extensionsToValues.put("1.23mbp", 1230000);
        extensionsToValues.put("1.234567mbp", 1234567);

        badExtensions = new HashSet<String>(Arrays.asList(
            "klsajhfd", "-1", "123kilobases", "0.5", "1.5bp", "1.23456kb", "-1.23", "1mkbp"
        ));
    }

    @Test
    public void goodExtensions() {
        int tested = 0;
        for (String ext: extensionsToValues.keySet()) {
            assertEquals("Can parse this extension: " + ext,
                extensionsToValues.get(ext), Integer.valueOf(FastaQueryService.parseExtension(ext)));
            tested++;
        }
        assertEquals(tested, extensionsToValues.size());
    }

    @Test
    public void badExtensions() {
        for (String ext: badExtensions) {
            try {
                FastaQueryService.parseExtension(ext);
                fail("Did not throw an exception at " + ext);
            } catch (BadRequestException e) {
                // Expected behaviour.
            }
        }
    }

}
