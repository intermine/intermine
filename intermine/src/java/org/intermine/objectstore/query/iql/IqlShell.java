package org.flymine.objectstore.query.fql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.io.PrintStream;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.QueryHelper;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.util.TypeUtil;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.gnu.readline.ReadlineCompleter;
import java.io.File;

/**
 * Shell for doing FQL queries
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class FqlShell
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
        if (args.length > 2) {
            out.println("Usage: java org.flymine.objectstore.query.Query "
                        + "<objectstore alias> - to enter shell-mode");
            out.println("       java org.flymine.objectstore.query.Query "
                        + "<objectstore alias> \"<FQL Query>\" - to run");
            out.println("                      a one-off query");
        } else {
            try {
                //Properties props = new Properties();
                //props.load(new FileInputStream("/home/mnw21/flymine.properties"));
                //System.setProperties(props);

                ObjectStore os = ObjectStoreFactory.getObjectStore(args[0]);
                if (args.length == 2) {
                    runQuery(args[1], os);
                } else {
                    doShell(os);
                }
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
            out.println("couldn't load readline lib. Using simple stdin.");
        }
        Readline.initReadline("FQLShell");
        try {
            Readline.readHistoryFile(System.getProperty("user.home") + File.separator
                    + ".fqlshell_history");
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
                        + ".fqlshell_history");
                } catch (Exception e) {
                    // Don't mind
                }
                PrintStream out = System.out;
                out.println("\n");
                Readline.cleanup();
            }
        });

        out.println("\nFlyMine Query shell. Type in an FQL query, or \"quit;\" to exit.");
        out.println("End your query with \";\" then a newline. Other newlines are ignored");
        out.flush();
        String currentQuery = "";
        String lastQuery = Readline.getHistoryLine(Readline.getHistorySize() - 1);
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
     * @param fql the FQL query string to run
     * @param os the ObjectStore to run it against
     */
    private static void runQuery(String fql, ObjectStore os)
            throws Exception {
        java.util.Date startTime = new java.util.Date();
        PrintStream out = System.out;

        // The following will only work when there is one package in the model
        String modelPackage = TypeUtil.packageName(((ClassDescriptor) os.getModel()
                                                    .getClassDescriptors().iterator().next())
                                                   .getName());
        
        FqlQuery fq = new FqlQuery(fql, modelPackage);
        Query q = fq.toQuery();

        Results res = os.execute(q);
        out.print("Column headings: ");
        outputList(QueryHelper.getColumnAliases(q));
        out.print("Column types: ");
        outputList(QueryHelper.getColumnTypes(q));
        Iterator rowIter = res.iterator();
        while (rowIter.hasNext()) {
            outputList((List) (rowIter.next()));
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
            out.print(o.toString());
        }
        out.println("");
    }

}
