package org.intermine.sql.logging;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;
import java.io.Writer;
import java.io.StringWriter;
import java.util.StringTokenizer;
import org.intermine.sql.query.*;

public class QueryLoggerTest extends TestCase
{
    private Query q1;

    public QueryLoggerTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        q1 = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q1.addFrom(t);
        q1.addSelect(sv);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));
    }

    public void testQuery() throws Exception {
        Writer w = new StringWriter();
        QueryLogger.log(q1, w);
        StringTokenizer st = new StringTokenizer(w.toString(), "\t", false);
        String date = st.nextToken();
        String query = st.nextToken();

        assertEquals("SELECT mytable.a FROM mytable WHERE mytable.a = 1", query);
    }

}
