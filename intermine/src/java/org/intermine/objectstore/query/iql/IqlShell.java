package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.gnu.readline.ReadlineCompleter;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.SqlGenerator;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.util.TypeUtil;

/**
 * Shell for doing IQL queries
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class IqlShell
{
    /**
     * A testing method - converts the argument into a Query object, and then converts it back to
     * a String again.
     *
     * @param args command-line arguments
     * @throws Exception anytime
     */
    public static void main(String args[]) throws Exception {
        PrintStream out = System.out;
        if (args.length > 1) {
            out.println("Usage: java org.intermine.objectstore.query.iql.IqlShell "
                        + "<objectstore alias> - to enter shell-mode");
        } else {
            try {
                ObjectStore os;
                if (args.length == 0) {
                    os = ObjectStoreFactory.getObjectStore();
                } else {
                    os = ObjectStoreFactory.getObjectStore(args[0]);
                }
                doShell(os);
            } catch (Exception e) {
                out.println("Exception caught: " + e);
                e.printStackTrace(out);
            }
        }
    }

    /**
     * Run the shell on a given ObjectStore
     *
     * @param os the ObjectStore to connect to
     */
    private static void doShell(ObjectStore os) throws Exception {
        PrintStream out = System.out;
        try {
            Readline.load(ReadlineLibrary.GnuReadline);
        } catch (UnsatisfiedLinkError ignore_me) {
            try {
                Readline.load(ReadlineLibrary.Editline);
            } catch (UnsatisfiedLinkError ignore_me2) {
                try {
                    Readline.load(ReadlineLibrary.Getline);
                } catch (UnsatisfiedLinkError ignore_me3) {
                    out.println("couldn't load readline lib. Using simple stdin.");
                }
            }
        }
        Readline.initReadline("IQLShell");
        try {
            Readline.readHistoryFile(System.getProperty("user.home") + File.separator
                    + ".iqlshell_history");
        } catch (RuntimeException e) {
            // Doesn't matter.
        }
        Readline.setCompleter(new ReadlineCompleter() {
            public String completer(String text, int state) {
                return null;
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Readline.writeHistoryFile(System.getProperty("user.home") + File.separator
                        + ".iqlshell_history");
                } catch (Exception e) {
                    // Don't mind
                }
                PrintStream out = System.out;
                out.println("\n");
                Readline.cleanup();
            }
        });

        out.println("\nInterMine Query shell. Type in an IQL query, or \"quit;\" to exit.");
        out.println("End your query with \";\" then a newline. Other newlines are ignored");
        out.flush();
        String currentQuery = "";
        String lastQuery = null;
        if (Readline.getHistorySize() > 0) {
            lastQuery = Readline.getHistoryLine(Readline.getHistorySize() - 1);
        }
        //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        do {
            //currentQuery += in.readLine();
            String line = Readline.readline(currentQuery.equals("") ? "> " : "", false);
            currentQuery += (line == null ? "" : line);
            if (!(currentQuery.equals("") || currentQuery.equals("quit;"))) {
                if (currentQuery.endsWith(";")) {
                    if (!currentQuery.equals(lastQuery)) {
                        Readline.addToHistory(currentQuery);
                        lastQuery = currentQuery;
                    }
                    currentQuery = currentQuery.substring(0, currentQuery.length() - 1);
                    try {
                        runQuery(currentQuery, os);
                    } catch (Exception e) {
                        e.printStackTrace(out);
                    }
                    currentQuery = "";
                } else {
                    currentQuery += "\n";
                }
            }
        } while (!"quit;".equals(currentQuery));
    }

    /**
     * Run a query against a given ObjectStore
     *
     * @param iql the IQL query string to run
     * @param os the ObjectStore to run it against
     */
    private static void runQuery(String iql, ObjectStore os)
            throws Exception {
        java.util.Date startTime = new java.util.Date();
        PrintStream out = System.out;
        PrintStream err = System.err;

        // The following will only work when there is one package in the model
        String modelPackage = TypeUtil.packageName(((ClassDescriptor) os.getModel()
                                                    .getClassDescriptors().iterator().next())
                                                   .getName());
        
        boolean doDots = false;

        if (iql.toUpperCase().startsWith("DOT ")) {
            iql = iql.substring(4);
            doDots = true;
        }
        IqlQuery iq = new IqlQuery(iql, modelPackage);
        Query q = iq.toQuery();

        out.println("Query to run: " + q.toString());
        if (os instanceof ObjectStoreInterMineImpl) {
            String sqlString =
                SqlGenerator.generate(q, 0, Integer.MAX_VALUE,
                                      ((ObjectStoreInterMineImpl) os).getSchema(),
                                      ((ObjectStoreInterMineImpl) os).getDatabase(), (Map) null);
                out.println("SQL: " + sqlString);
        }

        Results res = os.execute(q);
        res.setBatchSize(5000);
        out.print("Column headings: ");
        outputList(QueryHelper.getColumnAliases(q));
        out.print("Column types: ");
        outputList(QueryHelper.getColumnTypes(q));
        int rowNo = 0;
        Iterator rowIter = res.iterator();
        while (rowIter.hasNext()) {
            List row = (List) rowIter.next();
            if (doDots) {
                if (rowNo % 100 == 0) {
                    err.print(rowNo + " ");
                }
                err.print(".");
                if (rowNo % 100 == 99) {
                    err.print("\n");
                }
                err.flush();
            } else {
                outputList(row);
            }
            rowNo++;
        }
    }

    private static void outputList(List l) {
        PrintStream out = System.out;
        boolean needComma = false;
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (needComma) {
                out.print(", ");
            }
            needComma = true;
            out.print(o);
        }
        out.println("");
    }
}
