package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private String database;
    private File outputFile;
    private File configFile;

    /**
     * Prefix of the ticket pages in Trac.
     */
    public static final String TRAC_TICKET_URL_PREFIX = "http://trac.flymine.org/ticket/";

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
     * @throws BuildException if a problem occurs
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
            System.err .println("Processing configuration file: " + configFile.getCanonicalPath());
            LineNumberReader reader = new LineNumberReader(new FileReader(configFile));
            List testResults = runAllTests(db, reader);

            FileWriter fw;
            try {
                fw = new FileWriter(outputFile);
            } catch (IOException e) {
                throw new BuildException("failed to open outout file: " + outputFile, e);
            }
            PrintWriter pw = new PrintWriter(fw);

            processResults(testResults, pw);

            try {
                fw.close();
            } catch (IOException e) {
                throw new BuildException("couldn't close " + outputFile, e);
            }
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

    /**
     * Write a formatted HTML summary of the given AcceptanceTestResult objects to the PrintWriter.
     * @param testResults a List of AcceptanceTestResult objects
     * @param pw the PrintWriter
     */
    protected void processResults(List testResults, PrintWriter pw) {
        pw.println("<html>");
        pw.println("<head><title>Acceptance Test Results</title></head>");
        pw.println("<body>");
        pw.println("<h1>Acceptance Test Results</h1>");
        
        int testCount = 0;
        
        int failingTestsCount = 0;
        
        for (Iterator testResultsIter = testResults.iterator(); testResultsIter.hasNext();) {
            AcceptanceTestResult atr = (AcceptanceTestResult) testResultsIter.next();

            if (!atr.isSuccessful()) {
                failingTestsCount++;
            }

            pw.println("</ul>");

            testCount++;
        }
        
        pw.println("<h2>Total tests: " + testCount + "</h2>");
        if (testCount == 0) {
            pw.println("</body></html>");
            return;
        }
        pw.println("<h2>Failing tests: " + failingTestsCount + "</h2>");
        pw.println("<h2>Percentage passed: " 
                   + 100 * (testCount - failingTestsCount) / testCount + "%</h2>");        
        
        int count = 0;
        
        if (failingTestsCount > 0) {
            pw.println("<hr/><h2>Failing tests:</h2>");
            pw.println("<p>");

            for (Iterator testResultsIter = testResults.iterator(); testResultsIter.hasNext();) {
                AcceptanceTestResult atr = (AcceptanceTestResult) testResultsIter.next();

                pw.println("<ul>");

                if (!atr.isSuccessful()) {
                    pw.println("<li><a href=\"#test" + count + "\">");
                    pw.println(atr.getTest().getSql());
                    pw.println("</a><p><font size='-1'>(" + atr.getTest().getNote()
                               + ")</font></p></li>");
                }

                pw.println("</ul>");

                count++;
            }
        }

        pw.println("</p><hr/>");

        count = 0;

        Iterator testResultsIter = testResults.iterator();

        while (testResultsIter.hasNext()) {
            AcceptanceTestResult atr = (AcceptanceTestResult) testResultsIter.next();

            pw.println("<h2><a name=\"test" + count + "\">Testing: <font size=\"-1\">"
                       + atr.getTest().getSql() + "</font></a></h2>");
            pw.println("<h3>test type: " + atr.getTest().getType() + "</h3>");
            pw.println("<p>(completed in " + atr.getTime() / 1000.0 + " seconds)</p>");
            if (atr.getTest().getNote() != null) {
                String hyperlinkedDescription = hyperLinkNote(atr.getTest().getNote());
                pw.println("<h3>Description: " +  hyperlinkedDescription + "</h3>");
            }
            if (atr.isSuccessful()) {
                pw.println("<p>Result: <font color=\"green\">successful</font></p>");
            } else {
                pw.println("<p>Result: <font color=\"red\">FAILED</font></p>");
            }

            if (atr.getException() == null) {
                if ((atr.getTest().getType().equals(AcceptanceTest.NO_RESULTS_TEST)
                     || atr.getTest().getType().equals(AcceptanceTest.RESULTS_REPORT))
                    && atr.getResults().size() > 0) {
                    outputTable(pw, atr, atr.getColumnLabels(), atr.getResults());
                    pw.println("<p>total rows: " + atr.getResultsCount() + "</p>");
                }
            } else {
                pw.println("<p>SQLException while executing SQL:</p>");
                pw.println("<pre>");
                atr.getException().printStackTrace(pw);
                pw.println("</pre>");
            }

            pw.println("<hr>");

            count++;
        }

        testResultsIter = testResults.iterator();

        while (testResultsIter.hasNext()) {
            AcceptanceTestResult atr = (AcceptanceTestResult) testResultsIter.next();

            Iterator trackerIdIter = atr.getTrackerMap().keySet().iterator();

            while (trackerIdIter.hasNext()) {
                Integer id = (Integer) trackerIdIter.next();
                List trackerRows = (List) atr.getTrackerMap().get(id);

                pw.println("<h2><a name=\"object" + id + "\">Tracker entries for "
                           + id + "</a></h2>");
                outputTable(pw, atr, null, trackerRows);
                pw.println("<hr>");
            }
        }

        pw.println("</ul></body></html>");
        pw.close();
    }

    private void outputTable(PrintWriter pw, AcceptanceTestResult atr, List columnHeadings,
                             List results) {
        pw.println("<table border=1>");
        if (columnHeadings != null) {
            pw.println("<tr>");
            Iterator columnHeadingsIter = columnHeadings.iterator();
            while (columnHeadingsIter.hasNext()) {
                pw.println("<th>" + columnHeadingsIter.next() + "</th>");
            }            
            pw.println("</tr>");
        }
        Iterator resultsIter = results.iterator();
        while (resultsIter.hasNext()) {
            List row = (List) resultsIter.next();
            pw.println("<tr>");
            Iterator rowIter = row.iterator();
            while (rowIter.hasNext()) {
                pw.println("<td>");
                Object o = rowIter.next();
                if (o != null) {
                    if (o instanceof Integer) {
                        Integer id = (Integer) o;
                        List trackerRows = (List) atr.getTrackerMap().get(id);
                        if (trackerRows == null) {
                            pw.println(id);
                        } else {
                            pw.println("<a href=\"#object" + id + "\">" + id + "</a>");
                        }
                    } else {
                        pw.println(o);
                    }
                } else {
                    pw.println("<font color=\"grey\" size=\"-1\">null</font>");
                }
                pw.println("</td>");
            }
            pw.println("</tr>");
        }
        pw.println("</table>");
    }

    /**
     * Return a hyperlinked version of the given note.
     * @param note the note
     * @return note with trac ticket number (eg. #123) changed to links to trac
     */
    public static String hyperLinkNote(String note) {
        String replacement = "<a href=\"" + TRAC_TICKET_URL_PREFIX + "$1\">#$1</a>";
        return note.replaceAll("#(\\d+)", replacement);
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
            if (line.matches("\\s*#.*|\\s*")) {
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
            Pattern linePattern = Pattern.compile("^\\s*(\\S+)\\s*:\\s*(\\S.*?)(;?)\\s*$");
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
        long startTime = (new Date()).getTime();
        try {
            con.setAutoCommit(false);
            sm = con.createStatement();
            sm.setFetchSize(1000);
            rs = sm.executeQuery(test.getSql());
            long endTime = (new Date()).getTime();
            long totalTime = endTime - startTime;
            AcceptanceTestResult atr = new AcceptanceTestResult(test, rs, totalTime, con);
            return atr;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException e2) {
                throw new RuntimeException("couldn't rollback() transaction", e);
            }
            long endTime = (new Date()).getTime();
            long totalTime = endTime - startTime;
            return new AcceptanceTestResult(test, e, totalTime);
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

    private String type = null;
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
    private AcceptanceTest test;
    private SQLException sqlException = null;
    private List results = null;
    private List columnLabels = null;
    private int resultCount = -1;
    // a Map from InterMine ID to the corresponding entries in the tracker table
    private Map trackerMap = new HashMap();
    private final long time;

    /**
     * Create a new AcceptanceTestResult object.
     * @param test the AcceptanceTest that generated this AcceptanceTestResult
     * @param rs the ResultSet generated by the query for this test
     * @param time the time in seconds that the test took
     * @param con the database Connection - used to lookup IDs in the tracker table
     */
    AcceptanceTestResult(AcceptanceTest test, ResultSet rs, long time, Connection con) {
        this.test = test;
        this.time = time;
        try {
            results = copyResults(rs, test.getMaxResults().intValue());

            resultCount = results.size();
            // count the remaining rows in rs
            while (rs.next()) {
                resultCount++;
            }
            
            ResultSetMetaData metadata = rs.getMetaData();
            columnLabels = new ArrayList();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                columnLabels.add(metadata.getColumnLabel(i));
            }
            
            DatabaseMetaData dbMetadata = con.getMetaData();
            ResultSet trackerTableResults = dbMetadata.getTables(null, null, "tracker", null);
            
            if (trackerTableResults.next()) {
                // we have a tracker table
            
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    if (metadata.getColumnType(i) == Types.INTEGER
                        && metadata.getColumnLabel(i).equals("id")) {
                        // look up each ID in the tracker table and save the results
                        Iterator rowIter = results.iterator();
                        while (rowIter.hasNext()) {
                            List row = (List) rowIter.next();
                            Integer id = (Integer) row.get(i - 1);
                            List trackerRows = getTrackerRows(id, con);
                        
                            trackerMap.put(id, trackerRows);                            
                        }
                    }
                }
            }
        } catch (SQLException e) {
            sqlException = e;
        }
    }

    /**
     * Return the Map from InterMine ID to the corresponding entries in the tracker table.  Only
     * IDs seen in query results will appear in the keySet of the Map.
     * @return the tracker Map
     */
    public Map getTrackerMap() {
        return trackerMap;
    }

    /**
     * Return the time taken in milliseconds to run the test
     * @return the time taken
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Return the number of rows the test produced.
     * @return the number of rows
     */
    public int getResultsCount() {
        return resultCount;
    }
    
    /**
     * Get the rows from the tracker table that refer to the given id
     * @return the results as a List of Lists or null if there is an SQLException (which is stored
     * in sqlException)
     */
    private List getTrackerRows(Integer id, Connection con) {
        Statement sm = null;
        ResultSet rs = null;
        try {
            sm = con.createStatement();
            rs = sm.executeQuery("select * from tracker where objectid = " + id);
            return copyResults(rs, 100);
        } catch (SQLException e) {
            sqlException = e;
            return null;
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

    /**
     * Create a new AcceptanceTestResult object.
     * @param test the AcceptanceTest that generated this AcceptanceTestResult
     * @param sqlException the exception that occurred when running the test
     * @param time the time in seconds that the test took
     */
    AcceptanceTestResult(AcceptanceTest test, SQLException sqlException, long time) {
        this.test = test;
        this.sqlException = sqlException;
        this.time = time;
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
    
    /**
     * Return a List of the column labels.
     * @return the column labels.
     */
    public List getColumnLabels() {
        return columnLabels;
    }
    
    /**
     * Return the SQLException exception (if any) that occurred when the test SQL was run.
     * @return the SQLException or null if there was no exception
     */
    public SQLException getException() {
        return sqlException;
    }

    private static List copyResults(ResultSet rs, int maxRows) throws SQLException {
        List returnList = new ArrayList();

        int columnCount = rs.getMetaData().getColumnCount();

        for (int rowIndex = 0; maxRows == -1 || rowIndex < maxRows; rowIndex++) {
            if (rs.next()) {
                List rowCopy = new ArrayList();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    rowCopy.add(rs.getObject(columnIndex));
                }
                returnList.add(rowCopy);
            } else {
                break;
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
