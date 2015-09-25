/**
 *
 */
package org.intermine.pathquery;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * @author sc
 *
 */
public class ConstraintValueParserTest  extends TestCase {

    private static final String DATE_STRING = "2014-07-02";
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    static Date date = new Date(System.currentTimeMillis());

       @Test
        public static void testParsing() throws Exception {
           String aDate=date.toString();
           // checking if the returned value is the one expected for string and date types.
           Object convertedDate = ConstraintValueParser.parse(aDate, Date.class);
           assertEquals("qq", aDate, convertedDate);
           Object converted = ConstraintValueParser.parse(DATE_STRING, String.class);
            assertEquals("qq", DATE_STRING, converted);

        }

}
