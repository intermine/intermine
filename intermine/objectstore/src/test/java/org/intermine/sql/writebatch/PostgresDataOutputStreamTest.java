package org.intermine.sql.writebatch;

import java.io.IOException;
import junit.framework.TestCase;

public class PostgresDataOutputStreamTest extends TestCase {

    public void testWriteUTFWithCaractersWithOneByte() {
        String input = "ab";
        PostgresByteArrayOutputStream baos = new PostgresByteArrayOutputStream();
        PostgresDataOutputStream dos = new PostgresDataOutputStream(baos);
        try {
            dos.writeLargeUTF(input);
            dos.flush();
            dos.close();
            byte[] output = baos.getBuffer();
            assertEquals((byte) 2, output[3]);
            assertEquals((byte) 97, output[4]);
            assertEquals((byte) 98, output[5]);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void testWriteUTFWithCaractersWithThreeBytes() {
        ////Lao Letter Ko U+0E81 (https://unicode-table.com)
        String input = "abກ";
        PostgresByteArrayOutputStream baos = new PostgresByteArrayOutputStream();
        PostgresDataOutputStream dos = new PostgresDataOutputStream(baos);
        try {
            dos.writeLargeUTF(input);
            dos.flush();
            dos.close();
            byte[] output = baos.getBuffer();
            assertEquals((byte) 5, output[3]);
            assertEquals((byte) 97, output[4]);
            assertEquals((byte) 98, output[5]);
            assertEquals((byte) 224, output[6]);
            assertEquals((byte) 186, output[7]);
            assertEquals((byte) 129, output[8]);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void testWriteUTFWithSupplementayCaracters() {
        String input = "ab𝜅";
        PostgresByteArrayOutputStream baos = new PostgresByteArrayOutputStream();
        PostgresDataOutputStream dos = new PostgresDataOutputStream(baos);
        try {
            dos.writeLargeUTF(input);
            dos.flush();
            dos.close();
            byte[] output = baos.getBuffer();
            assertEquals((byte) 6, output[3]);
            assertEquals((byte) 97, output[4]);
            assertEquals((byte) 98, output[5]);
            assertEquals((byte) 240, output[6]);
            assertEquals((byte) 157, output[7]);
            assertEquals((byte) 156, output[8]);
            assertEquals((byte) 133, output[9]);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
