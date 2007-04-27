package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class ObjectPipeTest extends TestCase
{
    private volatile int progress;
    private ObjectPipe op;

    public ObjectPipeTest(String arg) {
        super(arg);
    }

    public void test() throws Exception {
        op = new ObjectPipe();

        op.put(new Integer(1));
        op.put(new Integer(2));
        assertTrue(op.hasNext());
        assertEquals(new Integer(1), op.next());
        op.finish();
        assertTrue(op.hasNext());
        assertEquals(new Integer(2), op.next());
        assertFalse(op.hasNext());
        try {
            op.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    public void testMultiThreaded() throws Exception {
        op = new ObjectPipe(2);
        progress = 0;

        Thread receiver = new Thread() {
            @Override
            public void run() {
                while (op.hasNext()) {
                    int i = ((Integer) op.next()).intValue();
                    if (i == progress + 1) {
                        progress = i;
                    } else {
                        progress = 3000;
                    }
                }
                if (progress != 3000) {
                    progress = 2000;
                }
            }
        };

        receiver.start();

        assertEquals(0, progress);
        op.put(new Integer(1));
        for (int i = 0; (i < 40) && (progress < 1); i++) {
            Thread.sleep(50);
        }
        assertEquals(1, progress);
        op.put(new Integer(2));
        for (int i = 0; (i < 40) && (progress < 2); i++) {
            Thread.sleep(50);
        }
        assertEquals(2, progress);

        List l = new ArrayList();
        l.add(new Integer(3));
        l.add(new Integer(4));
        l.add(new Integer(5));
        l.add(new Integer(6));
        op.putAll(l);
        for (int i = 0; (i < 40) && (progress < 6); i++) {
            Thread.sleep(50);
        }
        assertEquals(6, progress);

        op.finish();
        for (int i = 0; (i < 40) && (progress < 2000); i++) {
            Thread.sleep(50);
        }
        assertEquals(2000, progress);
    }

    public void testMultiThreaded2() throws Exception {
        op = new ObjectPipe(2);
        progress = 0;

        Thread sender = new Thread() {
            public void run() {
                List l = new ArrayList();
                l.add(new Integer(1));
                l.add(new Integer(2));
                l.add(new Integer(3));
                l.add(new Integer(4));
                op.putAll(l);
                progress = 4;

                op.put(new Integer(5));
                progress = 5;

                op.put(new Integer(6));
                progress = 6;

                op.finish();
                progress = 2000;
            }
        };

        sender.start();

        for (int i = 0; (i < 40) && (progress < 4); i++) {
            Thread.sleep(50);
        }
        assertEquals(4, progress);
        assertTrue(op.hasNext());
        assertEquals(new Integer(1), op.next());
        Thread.sleep(200);
        assertEquals(4, progress);
        assertTrue(op.hasNext());
        assertEquals(new Integer(2), op.next());
        Thread.sleep(200);
        assertEquals(4, progress);
        assertTrue(op.hasNext());
        assertEquals(new Integer(3), op.next());
        for (int i = 0; (i < 40) && (progress < 5); i++) {
            Thread.sleep(50);
        }
        assertEquals(5, progress);
        assertTrue(op.hasNext());
        assertEquals(new Integer(4), op.next());
        for (int i = 0; (i < 40) && (progress < 2000); i++) {
            Thread.sleep(50);
        }
        assertEquals(2000, progress);
        assertTrue(op.hasNext());
        assertEquals(new Integer(5), op.next());
        assertTrue(op.hasNext());
        assertEquals(new Integer(6), op.next());
        assertFalse(op.hasNext());
    }

    public void testPutFinished() throws Exception {
        op = new ObjectPipe();
        op.finish();
        try {
            op.put(new Integer(1));
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testFinishFinished() throws Exception {
        op = new ObjectPipe();
        op.finish();
        try {
            op.finish();
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testMaxBufferTooSmall() throws Exception {
        try {
            op = new ObjectPipe(0);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
