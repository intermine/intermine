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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import org.intermine.util.SensibleByteArrayOutputStream;
import org.intermine.util.StringConstructor;

/**
 * A subclass of DataOutputStream that has extra methods useful for reducing the memory
 * footprint of the Postgres database write operations.
 *
 * @author Matthew Wakeling
 */
public class PostgresDataOutputStream extends DataOutputStream
{
    /**
     * @see DataOutputStream
     *
     * @param out the OutputStream to write to
     */
    public PostgresDataOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Writes the given String to the stream in modified UTF-8 format, following its length in bytes
     * as a four-byte integer.
     *
     * @param str the String
     * @return the number of bytes written to the OutputStream
     * @throws IOException if there is an error writing to the underlying OutputStream
     */
    public int writeLargeUTF(String str) throws IOException {
        return writeLargeUTF(Collections.singletonList(str));
    }

    /**
     * Writes the given StringConstructor to the stream in modified UTF-8 format, following its
     * length in bytes as a four-byte integer.
     *
     * @param str the StringConstructor
     * @return the number of bytes written to the OutputStream
     * @throws IOException if there is an error writing to the underlying OutputStream
     */
    public int writeLargeUTF(StringConstructor str) throws IOException {
        return writeLargeUTF(str.getStrings());
    }

    /**
     * Writes the given Collection of Strings to the stream in modified UTF-8 format, following its
     * length in bytes as a four-byte integer.
     *
     * @param strs the Collection of Strings
     * @return the number of bytes written to the OutputStream
     * @throws IOException if there is an error writing to the underlying OutputStream
     */
    protected int writeLargeUTF(Collection<String> strs) throws IOException {
        int utflen = 0;
        int c, count = 0;
        
        for (String str : strs) {
            int strlen = str.length();
            for (int i = 0; i < strlen; i++) {
                c = str.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    utflen++;
                } else if (c > 0x07FF) {
                    utflen += 3;
                } else {
                    utflen += 2;
                }
            }
        }
        
        if (out instanceof SensibleByteArrayOutputStream) {
            ((SensibleByteArrayOutputStream) out).assureSize(((SensibleByteArrayOutputStream) out)
                .size() + 4 + utflen);
        }
        
        writeInt(utflen);
        
        for (String str : strs) {
            int strlen = str.length();
            int i = 0; 
            for (i = 0; i < strlen; i++) {
                c = str.charAt(i);
                if (!((c >= 0x0001) && (c <= 0x007F))) {
                    break;
                }
                writeByte((byte) c);
            }
            
            for (; i < strlen; i++) {
                c = str.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    writeByte((byte) c);
                } else if (c > 0x07FF) {
                    writeByte((byte) (0xE0 | ((c >> 12) & 0x0F)));
                    writeByte((byte) (0x80 | ((c >>  6) & 0x3F)));
                    writeByte((byte) (0x80 | ((c >>  0) & 0x3F)));
                } else {
                    writeByte((byte) (0xC0 | ((c >>  6) & 0x1F)));
                    writeByte((byte) (0x80 | ((c >>  0) & 0x3F)));
                }
            }
        }
        return utflen + 4;
    }
}
