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

import java.io.ByteArrayOutputStream;

/**
 * A subclass of ByteArrayOutputStream that behaves a little more sensibly when handling large
 * writes followed by small writes.
 *
 * @author Matthew Wakeling
 */
public class SensibleByteArrayOutputStream extends ByteArrayOutputStream
{
    /**
     * @see ByteArrayOutputStream
     */
    public SensibleByteArrayOutputStream() {
        super();
    }

    /**
     * @see ByteArrayOutputStream
     *
     * @param size the initial size of the byte array
     */
    public SensibleByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void write(int b) {
        assureSize(count + 1);
        super.write(b);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void write(byte b[], int off, int len) {
        assureSize(count + len);
        super.write(b, off, len);
    }

    /**
     * Resizes the buffer to ensure it is as large as newSize.
     *
     * @param newSize the new minimum size of the buffer
     */
    public void assureSize(int newSize) {
        if (newSize > buf.length) {
            byte newBuf[] = new byte[Math.max(buf.length << 1, newSize + (newSize / 10))];
            System.arraycopy(buf, 0, newBuf, 0, count);
            buf = newBuf;
        }
    }
}

