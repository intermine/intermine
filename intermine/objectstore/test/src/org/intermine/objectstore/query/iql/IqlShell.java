package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.gnu.readline.ReadlineCompleter;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.intermine.SqlGenerator;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.sql.precompute.QueryOptimiser;
import org.intermine.sql.precompute.QueryOptimiserContext;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;

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
                ObjectStore os = ObjectStoreFactory.getObjectStore(args[0]);
                if (os instanceof ObjectStoreInterMineImpl) {
                    out.println("Using database " + ((ObjectStoreInterMineImpl) os).getDatabase());
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
        } catch (UnsatisfiedLinkError ignoreMe) {
            try {
                Readline.load(ReadlineLibrary.Editline);
            } catch (UnsatisfiedLinkError ignoreMe2) {
                try {
                    Readline.load(ReadlineLibrary.Getline);
                } catch (UnsatisfiedLinkError ignoreMe3) {
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
            @Override
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
            String line = Readline.readline("".equals(currentQuery) ? "> " : ": ", false);
            currentQuery += (line == null ? "" : line);
            if (!("".equals(currentQuery) || currentQuery.equals("quit;"))) {
                if (currentQuery.endsWith(";")) {
                    if (!currentQuery.equals(lastQuery)) {
                        Readline.addToHistory(currentQuery);
                        lastQuery = currentQuery;
                    }
                    currentQuery = currentQuery.substring(0, currentQuery.length() - 1);
                    try {
                        if (currentQuery.toUpperCase().startsWith("DESCRIBE ")) {
                            currentQuery = currentQuery.substring(9);
                            Class c = null;
                            try {
                                c = Class.forName(currentQuery);
                            } catch (ClassNotFoundException e) {
                                // The following will only work when there is one package in the
                                // model
                                String modelPackage = TypeUtil.packageName(os
                                            .getModel().getClassDescriptors().iterator().next()
                                        .getName());
                                if (modelPackage != null) {
                                    try {
                                        c = Class.forName(modelPackage + "." + currentQuery);
                                    } catch (ClassNotFoundException e2) {
                                        throw new IllegalArgumentException("Unknown class name "
                                                + currentQuery + " in package " + modelPackage);
                                    }
                                } else {
                                    throw new IllegalArgumentException("Unknown class name "
                                            + currentQuery);
                                }
                            }
                            ClassDescriptor cld = os.getModel()
                                .getClassDescriptorsForClass(c).iterator().next();
                            System.out .println(cld.getHumanReadableText());
                        } else {
                            runQuery(currentQuery, os);
                        }
                    } catch (Throwable e) {
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
        PrintStream out = System.out;
        PrintStream err = System.err;

        // The following will only work when there is one package in the model
        String modelPackage = TypeUtil.packageName(((ClassDescriptor) os.getModel()
                                                    .getClassDescriptors().iterator().next())
                                                   .getName());

        int output = 0;
        // 0 = normal, 1 = dots, 2 = no output
        boolean noExplain = false;
        boolean doGoFaster = false;
        boolean doPrecompute = false;
        String optimiseMode = QueryOptimiserContext.MODE_NORMAL;

        if (iql.toUpperCase().startsWith("GOFASTER ")) {
            iql = iql.substring(9);
            doGoFaster = true;
        }
        if (iql.toUpperCase().startsWith("VERBOSE_OPTIMISE ")) {
            iql = iql.substring(17);
            optimiseMode = QueryOptimiserContext.MODE_VERBOSE;
        }

        if (iql.toUpperCase().startsWith("VERBOSE_OPTIMISE_LIST ")) {
            iql = iql.substring(22);
            optimiseMode = QueryOptimiserContext.MODE_VERBOSE_LIST;
        }

        if (iql.toUpperCase().startsWith("VERBOSE_OPTIMISE_SUMMARY ")) {
            iql = iql.substring(25);
            optimiseMode = QueryOptimiserContext.MODE_VERBOSE_SUMMARY;
        }

        if (iql.toUpperCase().startsWith("NOEXPLAIN ")) {
            iql = iql.substring(10);
            noExplain = true;
        }

        if (iql.toUpperCase().startsWith("DOT ")) {
            iql = iql.substring(4);
            output = 1;
        }
        if (iql.toUpperCase().startsWith("NOOUTPUT ")) {
            iql = iql.substring(9);
            output = 2;
        }
        if (iql.toUpperCase().startsWith("PRECOMPUTE ")) {
            iql = iql.substring(11);
            doPrecompute = true;
        }
        if ("MAKEEMPLOYEES".equals(iql.toUpperCase())) {
            if (os instanceof ObjectStoreInterMineImpl) {
                out.println("Storing 1,000,000 Employees");
                long startTime = System.currentTimeMillis();
                ObjectStoreWriter osw = os.getNewWriter();
                osw.beginTransaction();
                for (int i = 0; i < 1000000; i++) {
                    Employee emp = new Employee();
                    emp.setFullTime(i % 2 == 0);
                    emp.setEnd("end: " + i);
                    emp.setAge(i);
                    emp.setName("Name" + i);
                    emp.proxyDepartment(new ProxyReference(os, i, Department.class));
                    osw.store(emp);
                    if (i % 10000 == 0) {
                        out.print(".");
                        out.flush();
                    }
                }
                osw.commitTransaction();
                out.println("\nDone in " + (System.currentTimeMillis() - startTime) + " ms.");
            }
            return;
        }
        if ("MAKECOMPANIES".equals(iql.toUpperCase())) {
            if (os instanceof ObjectStoreInterMineImpl) {
                out.println("Storing 1,000,000 Companies");
                long startTime = System.currentTimeMillis();
                ObjectStoreWriter osw = os.getNewWriter();
                osw.beginTransaction();
                for (int i = 0; i < 1000000; i++) {
                    Company comp = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
                    comp.setVatNumber(i);
                    comp.setName("Name" + i);
                    comp.proxycEO(new ProxyReference(os, i, CEO.class));
                    comp.proxyAddress(new ProxyReference(os, i, Address.class));
                    osw.store(comp);
                    if (i % 10000 == 0) {
                        out.print(".");
                        out.flush();
                    }
                }
                osw.commitTransaction();
                out.println("\nDone in " + (System.currentTimeMillis() - startTime) + " ms.");
            }
            return;
        }
        IqlQuery iq = new IqlQuery(iql, modelPackage);
        Query q = iq.toQuery();

        if (doPrecompute) {
            if (os instanceof ObjectStoreInterMineImpl) {
                ObjectStoreInterMineImpl osii = (ObjectStoreInterMineImpl) os;
                out.println("Query to precompute: " + q.toString());
                long startTime = System.currentTimeMillis();
                List<String> precompNames = osii.precompute(q, "IqlShell");
                if (precompNames.isEmpty()) {
                    out.println("No precomputed tables created");
                } else {
                    out.println("Created precomputed tables: " + precompNames + " in "
                            + (System.currentTimeMillis() - startTime) + " ms");
                }
            }
            return;
        }
        out.println("Query to run: " + q.toString());
        try {
            if (os instanceof ObjectStoreInterMineImpl) {
                ObjectStoreInterMineImpl osii = (ObjectStoreInterMineImpl) os;
                if (doGoFaster) {
                    long startTime = System.currentTimeMillis();
                    osii.goFaster(q);
                    out.println("Called goFaster in " + (System.currentTimeMillis() - startTime)
                            + " ms");
                }
                String sqlString = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, osii.getSchema(),
                        osii.getDatabase(), (Map) null);
                out.println("SQL: " + sqlString);
                QueryOptimiserContext context = new QueryOptimiserContext();
                context.setMode(optimiseMode);
                if (!noExplain) {
                    context.setTimeLimit(os.getMaxTime() / 10);
                }
                sqlString = QueryOptimiser.optimise(sqlString + " LIMIT 5000",
                        ((ObjectStoreInterMineImpl) os).getDatabase(), context);
            }

            if ((optimiseMode == QueryOptimiserContext.MODE_NORMAL)
                    || (optimiseMode == QueryOptimiserContext.MODE_VERBOSE)) {
                Results res = os.execute(q, 50000, true, !noExplain, true);
                out.print("Column headings: ");
                outputList(QueryHelper.getColumnAliases(q));
                out.print("Column types: ");
                outputList(QueryHelper.getColumnTypes(q));
                int rowNo = 0;
                long startTime = System.currentTimeMillis();
                Iterator rowIter = res.iterator();
                while (rowIter.hasNext()) {
                    List row = (List) rowIter.next();
                    if (output == 1) {
                        if (rowNo % 100 == 0) {
                            err.print(rowNo + " ");
                        }
                        err.print(".");
                        if (rowNo % 100 == 99) {
                            err.print("\n");
                        }
                        err.flush();
                    } else if (output == 0) {
                        outputList(row);
                    }
                    rowNo++;
                }
                if (output == 1) {
                    err.print("\n");
                }
                out.println("Fetched " + rowNo + " rows in "
                            + (System.currentTimeMillis() - startTime) + " ms");
            }
        } finally {
            if (doGoFaster) {
                if (os instanceof ObjectStoreInterMineImpl) {
                    ((ObjectStoreInterMineImpl) os).releaseGoFaster(q);
                }
            }
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
