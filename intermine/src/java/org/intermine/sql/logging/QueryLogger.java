package org.flymine.sql.logging;

import org.flymine.sql.query.Query;
import java.io.Writer;
import java.io.IOException;
import java.util.Date;

/**
 * Provides a logging facility for a Query
 *
 * @author Andrew Varley
 */
public class QueryLogger
{

    protected static Object lock = new Object();

    /**
     * Allows a Query to be logged
     *
     * @param q a Query to be logged
     * @param w the Writer on which to log the Query
     * @throws IOException if unable to log correctly
     */
    public static void log(Query q, Writer w) throws IOException {
        synchronized (lock) {
            w.write(new Date().toString() + "\t" + q.getSQLString());
        }
    }

}
