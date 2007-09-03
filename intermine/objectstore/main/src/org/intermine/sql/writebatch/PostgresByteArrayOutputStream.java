package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.SensibleByteArrayOutputStream;

/**
 * A subclass of SensibleByteArrayOutputStream that has extra methods useful for reducing the memory
 * footprint of the Postgres database write operations.
 *
 * @author Matthew Wakeling
 */
public class PostgresByteArrayOutputStream extends SensibleByteArrayOutputStream
{
    /**
     * @see ByteArrayOutputStream
     */
    public PostgresByteArrayOutputStream() {
        super();
    }

    /**
     * @see ByteArrayOutputStream
     *
     * @param size the initial size of the byte array
     */
    public PostgresByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * Returns the byte buffer without copying it. Note that the buffer may be larger than the data
     * written, so the contents of the buffer after the size are undefined. Also, the contents of
     * the buffer may change if more data is written to this object.
     *
     * @return a byte array
     */
    public byte[] getBuffer() {
        return buf;
    }
}


