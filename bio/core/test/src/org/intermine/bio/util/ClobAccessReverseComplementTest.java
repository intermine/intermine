package org.intermine.bio.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.ClobAccess;

public class ClobAccessReverseComplementTest extends TestCase
{
    private ObjectStore os;

    public void setUp() throws Exception {
        os = ObjectStoreFactory.getObjectStore("os.bio-test");
    }

    public void testTranslate() throws Exception {
        Clob clob = new Clob(1);
        ClobAccess ca = new ClobAccess(os, clob);
        ClobAccessReverseComplement carc = new ClobAccessReverseComplement(ca);

        Map<Character, Character> trans = new HashMap<Character, Character>();
        trans.put('A', 'T');
        trans.put('U', 'A');
        trans.put('G', 'C');
        trans.put('C', 'G');
        trans.put('Y', 'R');
        trans.put('R', 'Y');
        trans.put('S', 'S');
        trans.put('W', 'W');
        trans.put('K', 'M');
        trans.put('M', 'K');
        trans.put('B', 'V');
        trans.put('D', 'H');
        trans.put('H', 'D');
        trans.put('V', 'B');
        trans.put('N', 'N');
        // check upper case mappings
        for (Map.Entry<Character, Character> e : trans.entrySet()) {
            char expected = e.getValue();
            char actual = carc.translate(e.getKey());
            assertEquals(expected, actual);
        }
        // check lower case mappings
        for (Map.Entry<Character, Character> e : trans.entrySet()) {
            char expected = e.getValue().charValue();
            char actual = carc.translate(e.getKey());
            assertEquals(expected, actual);
        }
    }

    public void testTranslateInvalid() throws Exception {
        Clob clob = new Clob(1);
        ClobAccess ca = new ClobAccess(os, clob);
        ClobAccessReverseComplement carc = new ClobAccessReverseComplement(ca);

        try {
            carc.translate('Z');
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // as expected
        }

    }

}
