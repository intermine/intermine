package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * A Task to run acceptance tests, configurable from a file.
 *
 * @author Kim Rutherford
 */

public class AcceptanceTestTask extends Task
{
    private List fileSets = new ArrayList();
    private String database;
    private File outputFile;
    private File configFile;

    /**
     * Set the File to read configuration from
     * @param configFile the config File
     */
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Set the File to write output to
     * @param outputFile the output file
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Set the database alias
     * @param database the database alias
     */
    public void setDatabase(String database) {
        this.database = database;
    }
    
    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        
        if (configFile == null) {
            throw new BuildException("configFile attribute is not set");
        }
        
        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }
        
        try {            
            Database db = DatabaseFactory.getDatabase(database);
            System.err .println("Processing configuration file: " + configFile.getName());
            LineNumberReader reader = new LineNumberReader(new FileReader(configFile));            
            List testResults = runAllTests(db, reader);
            processResults(testResults, outputFile);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
    
    /**
     * Run all the tests and return a List of AcceptanceTestResult objects.
     * @param db the Database to run the queries against
     * @param configReader the reader to get configuration information from.
     * @return a List of AcceptanceTestResult objects
     * @throws SQLException if there is a problem running the SQL query
     * @throws IOException if there is a problem reading or parsing the config file
     */
    protected List runAllTests(Database db, LineNumberReader configReader)
        throws IOException, SQLException {
        Connection con = db.getConnection();
        List testResults = new ArrayList();

        try {
            AcceptanceTest test;
                    
            while ((test = readOneTestConfig(configReader)) != null) {
                AcceptanceTestResult testResult = runTest(con, test);                            
                testResults.add(testResult);
            }
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: "
                                     + configFile, e);
        }
        
        return testResults;
    }
    
    private void processResults(List testResults, File outputFile) throws BuildException {
        Iterator testResultsIter = testResults.iterator();

        FileWriter fw;
        try {
            fw = new FileWriter(outputFile);
        } catch (IOException e) {
            throw new BuildException("failed to open outout file: " + outputFile, e);
        }
        PrintWriter pw = new PrintWriter(fw);
        
        pw.println("<html>");
        pw.println("<head><title>Acceptance Test Results</title></head>");
        pw.println("<body>");
        
        while (testResultsIter.hasNext()) {
            AcceptanceTestResult atr = (AcceptanceTestResult) testResultsIter.next();

            pw.println("<h2>Testing: " + atr.getTest().getSql() + "</h2>");
            pw.println("<h3>test type: " + atr.getTest().getType() + "</h3>");
            if (atr.getTest().getNote() != null) {
                pw.println("<h3>Description: " + atr.getTest().getNote() + "</h3>");
            }
            if (atr.isSuccessful()) {
                pw.println("Result: <font color=\"green\">successful</font>");
            } else {
                pw.println("Result: <font color=\"red\">FAILED</font>");
            }
            
            if ((atr.getTest().getType().equals(AcceptanceTest.NO_RESULTS_TEST)
                || atr.getTest().getType().equals(AcceptanceTest.RESULTS_REPORT))
                && atr.getResults().size() > 0) {
                pw.println("<table border=1>");
                Iterator resultsIter = atr.getResults().iterator();
                while (resultsIter.hasNext()) {
                    List row = (List) resultsIter.next();
                    pw.println("<tr>");
                    Iterator rowIter = row.iterator();
                    while (rowIter.hasNext()) {
                        pw.println("<td>");
                        Object o = rowIter.next();
                        if (o != null) {
                            pw.println(o);
                        } else {
                            pw.println("<font color=\"grey\" size=\"-1\">null</font>");
                        }
                        pw.println("</td>");
                    }
                    pw.println("</tr>");
                }
                pw.println("</table>");
            }
            pw.println("<hr>");
        }
        pw.println("</ul></body></html>");
        pw.close();
        try {
            fw.close();
        } catch (IOException e) {
            throw new BuildException("couldn't close " + outputFile, e);
        }
    }

    /**
     * Read and return one AcceptanceTest object from the given Reader.
     * @param configReader the reader to get configuration information from
     * @return an AcceptanceTest object
     * @throws IOException if there is a problem reading or parsing the config file
     */
    public static AcceptanceTest readOneTestConfig(LineNumberReader configReader)
        throws IOException {
        String sql = null;
        String note = null;
        String type = null;
        Integer maxResults = null;
        String line;
        while ((line = (configReader.readLine())) != null) {
            if (line.matches("^\\s*#|^\\s*$")) {
                continue;
            }

            Pattern headerPattern = Pattern.compile("^\\s*(\\S+)\\s*\\{\\s*$");
            Matcher headerMatcher = headerPattern.matcher(line);

            if (headerMatcher.matches()) {
                type = headerMatcher.group(1);
                if (type.equals(AcceptanceTest.NO_RESULTS_TEST)
                    || type.equals(AcceptanceTest.SOME_RESULTS_TEST)
                    || type.equals(AcceptanceTest.ASSERT_TEST)
                    || type.equals(AcceptanceTest.RESULTS_REPORT)) {
                    continue;
                } else {
                    throw new IOException("unknown acceptance test type: "
                                          + headerMatcher.group(1) + " at line "
                                          + configReader.getLineNumber());
                }
            }

            // must be inside the braces now
            Pattern linePattern = Pattern.compile("^\\s*(\\S+)\\s*:\\s*(\\S.*)");
            Matcher lineMatcher = linePattern.matcher(line);

            if (lineMatcher.matches()) {
                if (lineMatcher.group(1).equals("sql")) {
                    sql = lineMatcher.group(2);
                    continue;
                } else if (lineMatcher.group(1).equals("note")) {
                    note = lineMatcher.group(2);
                    continue;
                } else if (lineMatcher.group(1).equals("max-results")) {
                    try {
                        maxResults = Integer.valueOf(lineMatcher.group(2));
                    } catch (NumberFormatException e) {
                        throw new IOException("cannot parse number: " + lineMatcher.group(2)
                                              + " at line " + configReader.getLineNumber());
                    }

                    continue;
                } else {
                    throw new IOException("unknown field: " + lineMatcher.group(1) + " at line "
                                          + configReader.getLineNumber());
                }
            }

            if (line.trim().equals("}")) {
                if (sql == null) {
                    throw new IOException("no sql in test at line "
                                          + configReader.getLineNumber());
                }
                return new AcceptanceTest(type, sql, note, maxResults);
            } else {
                throw new IOException("cannot parse line: " + line + " at line "
                                      + configReader.getLineNumber());
            }
        }
        
        // end of file
        return null;
    }
    
    private AcceptanceTestResult runTest(Connection con, AcceptanceTest test) {
        Statement sm = null;
        ResultSet rs = null;
        try {
            sm = con.createStatement();
            rs = sm.executeQuery(test.getSql());

            AcceptanceTestResult atr = new AcceptanceTestResult(test, rs);
            return atr;
        } catch (SQLException e) {
            return new AcceptanceTestResult(test, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (sm != null) {
                    sm.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException("exception while closing Statement or ResultSet", e);
            }
        }
    }
}

/**
 * A class holding information about one acceptance test.
 * @author Kim Rutherford
 */
class AcceptanceTest
{
    /**
     * the default number of result line to save
     */
    public static final int DEFAULT_MAX_RESULTS = 20;
    
    String type = null;
    private String sql = null;
    private Integer maxResults;
    private String note;

    /**
     * Type of test when some results are expected.
     */
    static final String SOME_RESULTS_TEST = "some-results";
    
    /**
     * Type of test when no results are expected.
     */
    static final String NO_RESULTS_TEST = "no-results";
    
    /**
     * Type of test when meaning save and display the results of running a query
     */
    static final String RESULTS_REPORT = "results-report";
    
    /**
     * Type of test that false if the ResultSet isn't a one-row set with just true as the result.
     */
    static final String ASSERT_TEST = "assert";
    
    /**
     * Create a new AcceptanceTest object.
     * @param type the type of the test
     * @param sql the SQL to run in the test
     * @param note a note or description of the test
     * @param maxResults the maximum number of row to report for type RESULTS_REPORT and the
     * maximum to show when NO_RESULTS_TEST fails
     */
    public AcceptanceTest(String type, String sql, String note, Integer maxResults) {
        if (!type.equals(AcceptanceTest.ASSERT_TEST)
            && !type.equals(AcceptanceTest.NO_RESULTS_TEST)
            && !type.equals(AcceptanceTest.SOME_RESULTS_TEST)
            && !type.equals(AcceptanceTest.RESULTS_REPORT)) {
            throw new RuntimeException("unknown test type: " + type);
        }
        
        this.type = type;
        this.sql = sql;
        this.note = note;
        
        if (maxResults == null) {
            this.maxResults = new Integer(DEFAULT_MAX_RESULTS);
        } else {
            this.maxResults = maxResults;
        }
    }

    /**
     * Constructor an empty object.
     */
    AcceptanceTest() {
        // empty
    }

    /**
     * Return the type parameter that was passed to the constructor.
     * @return the type parameter that was passed to the constructor.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Return the sql parameter that was passed to the constructor.
     * @return the sql parameter that was passed to the constructor.
     */
    public String getSql() {
        return sql;
    }
    
    /**
     * Return the note parameter that was passed to the constructor.
     * @return the note parameter that was passed to the constructor.
     */ 
   public String getNote() {
        return note;
    }

    /**
     * Return the maxResults parameter that was passed to the constructor.
     * @return the maxResults parameter that was passed to the constructor.
     */
    public Integer getMaxResults() {
        return maxResults;
    }
}

/**
 * A class holding information about the results of one acceptance test.
 * @author Kim Rutherford
 */
class AcceptanceTestResult
{
    AcceptanceTest test;
    SQLException sqlException = null;
    List results = null;

    /**
     * Create a new AcceptanceTestResult object.
     * @param test the AcceptanceTest that generated this AcceptanceTestResult
     * @param rs the ResultSet generated by the query for this test
     */
    AcceptanceTestResult(AcceptanceTest test, ResultSet rs) {
        this.test = test;
        try {
            results = copyResults(test, rs);
        } catch (SQLException e) {
            sqlException = e;
        }
    }
    
    /**
     * Create a new AcceptanceTestResult object.
     * @param test the AcceptanceTest that generated this AcceptanceTestResult
     * @param sqlException the exception that occurred when running the test
     */
    AcceptanceTestResult(AcceptanceTest test, SQLException sqlException) {
        this.test = test;
        this.sqlException = sqlException;
    }

    /**
     * Return true if and only if the test was successful
     * @return true if and only if the test was successful
     */
    public boolean isSuccessful() {
        if (sqlException != null) {
            return false;
        }
        if (test.getType().equals(AcceptanceTest.ASSERT_TEST)) {
            if (results.size() == 1) {
                Object o = ((List) results.get(0)).get(0);
                if (o instanceof Boolean) {
                    return ((Boolean) o).booleanValue();
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (test.getType().equals(AcceptanceTest.NO_RESULTS_TEST)) {
            return results.size() == 0;
        } else if (test.getType().equals(AcceptanceTest.SOME_RESULTS_TEST)) {
            return results.size() != 0;
        } else if (test.getType().equals(AcceptanceTest.RESULTS_REPORT)) {
            return true;
        } else {
            throw new RuntimeException("unkown test type: " + test.getType());
        }
    }
    
    /**
     * Return a List of Lists containing the results generated by the query for this test.  Only 
     * the first maxResults rows are returned.
     * @return the query results
     */
    public List getResults() {
        return results;
    }
    
    private List copyResults(AcceptanceTest test, ResultSet rs) throws SQLException {
        System.err.println ("copying results in: " + test.getSql());

        List returnList = new ArrayList();
        
        int columnCount = rs.getMetaData().getColumnCount();
        
        for (int rowIndex = 0; rowIndex < test.getMaxResults().intValue(); rowIndex++) {
            if (rs.next()) {
                List rowCopy = new ArrayList();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    rowCopy.add(rs.getObject(columnIndex));
                } 
                System.err.println ("row: " + rowCopy);
                returnList.add(rowCopy);
            }
        }
        
        return returnList;
    }

    /**
     * Return the test that was passed to the constructor.
     * @return the AcceptanceTest
     */
    public AcceptanceTest getTest() {
        return test;
    }
}
