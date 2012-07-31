package org.intermine.sql;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.intermine.util.ShutdownHook;
import org.intermine.util.Shutdownable;
import org.intermine.util.StringUtil;
import org.postgresql.util.PSQLException;

/**
 * Class that represents a physical SQL database
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class Database implements Shutdownable
{
    private static final Logger LOG = Logger.getLogger(Database.class);

    protected DataSource datasource;
    protected String platform;
    protected String driver;
    /** The number of worker threads to use for background SQL statements */
    protected int parallel = 4;

    // Store all the properties this Database was configured with
    protected Properties settings;

    // protected Map createSituations = new HashMap();

    /**
     * No argument constructor for testing purposes
     */
    protected Database() {
        // empty
    }

    /**
     * Constructs a Database object from a set of properties
     *
     * @param props the properties by which this Database is configured
     * @throws ClassNotFoundException if there is a class in props that cannot be found
     */
    protected Database(Properties props) throws ClassNotFoundException {
        settings = props;
        configure(props);
        try {
            LOG.info("Creating new Database " + getURL() + "(" + toString() + ") with ClassLoader "
                    + getClass().getClassLoader() + " and parallelism " + parallel);
        } catch (Exception e) {
            LOG.info("Creating new invalid Database with ClassLoader "
                    + getClass().getClassLoader(), e);
        }
        ShutdownHook.registerObject(new WeakReference<Database>(this));
    }

    /**
     * Gets the DataSource object for this Database
     *
     * @return the datasource for this Database
     */
    public DataSource getDataSource() {
        return datasource;
    }

    /**
     * Gets a Connection to this Database
     *
     * @return a Connection to this Database
     * @throws SQLException if there is a problem in the underlying database
     */
    public Connection getConnection() throws SQLException {
        Connection retval;
        if (datasource == null) {
            throw new NullPointerException("Datasource is null. Properties are: " + settings);
        }
        try {
            retval = datasource.getConnection();
        } catch (PSQLException e) {
            throw new RuntimeException("can't open datasource for " + this, e);
        }
        /*
        Exception e = new Exception();
        e.fillInStackTrace();
        StringWriter message = new StringWriter();
        PrintWriter pw = new PrintWriter(message);
        e.printStackTrace(pw);
        pw.close();
        String createSituation = message.toString();
        int index = createSituation.indexOf("at junit.framework.TestCase.runBare");
        createSituation = (index < 0 ? createSituation : createSituation.substring(0, index));
        createSituations.put(retval, createSituation);
        */
        return retval;
    }

    /**
     * Logs stuff
     */
    public void shutdown() {
        /*int totalConnections = 0;
        int activeConnections = 0;
        Iterator iter = createSituations.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Connection con = (Connection) entry.getKey();
            String sit = (String) entry.getValue();
            String conDesc = con.toString();
            if (conDesc.indexOf("Pooled connection wrapping physical connection null") == -1) {
                LOG.info("Possibly active connection for Database " + getURL() + "(" + toString()
                        + "), connection: " + con + ", createSituation: " + sit);
                activeConnections++;
            }
            totalConnections++;
        }
        LOG.info("Database " + getURL() + "(" + toString() + ") has " + totalConnections
                + " connections, of which " + activeConnections + " are active");*/
        if (datasource instanceof org.postgresql.ds.PGPoolingDataSource) {
            LOG.info("Shutdown - Closing datasource for Database " + getURL() + "(" + toString()
                    + ") with ClassLoader " + getClass().getClassLoader());
            ((org.postgresql.ds.PGPoolingDataSource) datasource).close();
        } else if (datasource instanceof org.postgresql.jdbc2.optional.PoolingDataSource) {
            LOG.info("Shutdown - Closing datasource for Database " + getURL() + "(" + toString()
                    + ") with ClassLoader " + getClass().getClassLoader());
            ((org.postgresql.jdbc2.optional.PoolingDataSource) datasource).close();
        } else {
            LOG.warn("Shutdown - Could not close datasource for Database " + getURL() + "("
                    + toString() + ") with ClassLoader " + getClass().getClassLoader() + " - "
                    + datasource.getClass().toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        if (datasource instanceof org.postgresql.ds.PGPoolingDataSource) {
            LOG.info("Finalise - Closing datasource for Database " + getURL() + "(" + toString()
                    + ") with ClassLoader " + getClass().getClassLoader());
            ((org.postgresql.ds.PGPoolingDataSource) datasource).close();
        } else if (datasource instanceof org.postgresql.jdbc2.optional.PoolingDataSource) {
            LOG.info("Finalise - Closing datasource for Database " + getURL() + "(" + toString()
                    + ") with ClassLoader " + getClass().getClassLoader());
            ((org.postgresql.jdbc2.optional.PoolingDataSource) datasource).close();
        } else {
            LOG.warn("Finalise - Could not close datasource for Database " + getURL() + "("
                    + toString() + ") with ClassLoader " + getClass().getClassLoader() + " - "
                    + datasource.getClass().toString());
        }
    }

    /**
     * Gets the platform of this Database
     *
     * @return the datasource for this Database
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Gets the driver this Database
     *
     * @return the driver for this Database
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Gets the username for this Database
     *
     * @return the username for this Database
     */
    public String getUser() {
        return (String) settings.get("datasource.user");
    }

    /**
     * Gets the password for this Database
     *
     * @return the password for this Database
     */
    public String getPassword() {
        return (String) settings.get("datasource.password");
    }

    /**
     * Gets the URL from this database
     *
     * @return the URL for this database
     */
    public String getURL() {

        String url = "jdbc:" + platform.toLowerCase() + "://"
            + (String) settings.get("datasource.serverName")
            + "/" + (String) settings.get("datasource.databaseName");
//        if (platform.equalsIgnoreCase("oracle")) {
//            //jdbc:oracle:thin:@oracle.flymine.org:1521:XE
//            url = "jdbc:" + platform.toLowerCase() + ":"                    // oracle
//                  + (String) settings.get("datasource.driverType") + ":@"   // thin
//                  + (String) settings.get("datasource.serverName") + ":"    // oracle.flymine.org
//                  + (String) settings.get("datasource.portNumber") + ":"    // 1521
//                  + (String) settings.get("datasource.databaseName");       // XE
//        }
        return url;
    }

    /**
     * Gets the database name only, not the full URL.
     * @return the database name
     */
    public String getName() {
        return (String) settings.get("datasource.databaseName");
    }

    /**
     * Configures a datasource from a Properties object
     *
     * @param props the properties for configuring the Database
     * @throws ClassNotFoundException if the class given in the properties file cannot be found
     * @throws IllegalArgumentException if the configuration properties are empty
     * @throws NullPointerException if props is null
     */
    protected void configure(Properties props) throws ClassNotFoundException {
        if (props == null) {
            throw new NullPointerException("Props cannot be null");
        }

        if (props.size() == 0) {
            throw new IllegalArgumentException("No configuration details");
        }

        Properties subProps = new Properties();

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String propertyName = (String) entry.getKey();
            String propertyValue = (String) entry.getValue();
            Field field = null;

            // Get the first part of the string - this is the attribute we are taking about
            String attribute = propertyName;
            String subAttribute = "";
            int index = propertyName.indexOf(".");
            if (index != -1) {
                attribute = propertyName.substring(0, index);
                subAttribute = propertyName.substring(index + 1);
            }

            try {
                field = Database.class.getDeclaredField(attribute);
            } catch (Exception e) {
                LOG.warn("Ignoring field for Database: " + attribute);
                // Ignore this property - no such field
                continue;
            }

            if ("class".equals(subAttribute)) {
                // make a new instance of this class for this attribute
                Class<?> clazz = Class.forName(propertyValue.toString());
                Object obj;
                try {
                    obj = clazz.newInstance();
                } catch (Exception e) {
                    throw new ClassNotFoundException("Cannot instantiate class "
                                                     + clazz.getName() + " " + e.getMessage());
                }
                // Set the field to this newly instantiated class
                try {
                    field.set(this, obj);
                } catch (Exception e) {
                    continue;
                }
            } else if ("".equals(subAttribute)) {
                // Set this attribute directly
                try {
                    field.set(this, propertyValue);
                } catch (Exception e) {
                    continue;
                }
            } else {
                // Set parameters on the attribute
                Method m = null;
                // Set this configuration parameter on the DataSource;
                try {
                    // Strings first
                    Object o = field.get(this);
                    // Sometimes the class will not have been instantiated yet
                    if (o == null) {
                        subProps.put(propertyName, propertyValue);
                        continue;
                    }
                    Class<?> clazz = o.getClass();
                    m = clazz.getMethod("set" + StringUtil.capitalise(subAttribute),
                                        new Class[] {String.class});
                    if (m != null) {
                        m.invoke(field.get(this), new Object [] {propertyValue});
                    }
                    // now integers
                } catch (Exception e) {
                    // Don't do anything - either the method not found or cannot be invoked
                }
                try {
                    if (m == null) {
                        m = field.get(this).getClass().
                            getMethod("set" + StringUtil.capitalise(subAttribute),
                                      new Class[] {int.class});
                        if (m != null) {
                            m.invoke(field.get(this),
                                     new Object [] {Integer.valueOf(propertyValue.toString())});
                        }
                    }
                } catch (Exception e) {
                // Don't do anything - either the method not found or cannot be invoked
                }
            }
            if (subProps.size() > 0) {
                configure(subProps);
            }
        }

    }

    private static final Map<Class<?>, String> POSTGRESQL_TYPE_STRING_MAP
        = new HashMap<Class<?>, String>();

    static {
        POSTGRESQL_TYPE_STRING_MAP.put(Boolean.class, "boolean");
        POSTGRESQL_TYPE_STRING_MAP.put(Float.class, "real");
        POSTGRESQL_TYPE_STRING_MAP.put(Double.class, "double precision");
        POSTGRESQL_TYPE_STRING_MAP.put(Short.class, "smallint");
        POSTGRESQL_TYPE_STRING_MAP.put(Integer.class, "integer");
        POSTGRESQL_TYPE_STRING_MAP.put(Long.class, "bigint");
        POSTGRESQL_TYPE_STRING_MAP.put(BigDecimal.class, "numeric");
        POSTGRESQL_TYPE_STRING_MAP.put(Date.class, "bigint");
        POSTGRESQL_TYPE_STRING_MAP.put(String.class, "text");
    }

    /**
     * Return the SQL type used to store objects of the given Class.  eg. return "double precision"
     * for Double.class
     * @param c the Class representing the java type
     * @return the SQL type
     */
    public String getColumnTypeString(Class<?> c) {
        return POSTGRESQL_TYPE_STRING_MAP.get(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "" + settings + " " + driver + " " + platform;
    }

    private Set<SqlJob> pending = new HashSet<SqlJob>();
    private Set<Waiter> waiters = new HashSet<Waiter>();
    private ArrayBlockingQueue<SqlJob> queue = new ArrayBlockingQueue<SqlJob>(30);
    private Set<Worker> workers = new HashSet<Worker>();
    private Exception reportedException = null;
    private int threadNo = 1;

    /**
     * Executes an SQL statement on the database in a separate thread. A certain number of worker
     * threads controlled by the "parallel" property will operate simultaneously.
     *
     * @param sql an SQL string.
     * @throws SQLException if an error has been reported by a previous operation - however, this
     * does not cancel the current operation.
     */
    public void executeSqlInParallel(String sql) throws SQLException {
        SqlJob job = new SqlJob(sql);
        synchronized (this) {
            pending.add(job);
        }
        boolean notDone = true;
        while (notDone) {
            try {
                queue.put(job);
                notDone = false;
            } catch (InterruptedException e) {
                // Not done
            }
        }
        synchronized (this) {
            if (workers.size() < parallel) {
                Worker worker = new Worker(threadNo);
                Thread thread = new Thread(worker);
                thread.setDaemon(true);
                thread.setName("Database background thread " + threadNo);
                threadNo++;
                workers.add(worker);
                thread.start();
            }
            if (reportedException != null) {
                SQLException re = new SQLException("Error while executing SQL");
                re.initCause(reportedException);
                reportedException = null;
                throw re;
            }
        }
    }

    /**
     * Blocks until all the current pending jobs are finished. This will not block new jobs from
     * arriving, and those new jobs do not need to be finished for this method to return.
     *
     * @throws SQLException if an error has been reported by a previous operation
     */
    public void waitForCurrentJobs() throws SQLException {
        Waiter waiter;
        synchronized (this) {
            waiter = new Waiter(pending);
            waiters.add(waiter);
        }
        waiter.waitUntilFinished();
        synchronized (this) {
            waiters.remove(waiter);
            if (reportedException != null) {
                SQLException re = new SQLException("Error while executing SQL");
                re.initCause(reportedException);
                reportedException = null;
                throw re;
            }
        }
    }

    private synchronized void jobIsDone(SqlJob job) {
        pending.remove(job);
        for (Waiter waiter : waiters) {
            waiter.jobIsDone(job);
        }
    }

    private synchronized void reportException(Exception e) {
        if (reportedException == null) {
            reportedException = e;
        }
    }

    /**
     * Wrap String, so that we have default equals and hashcode.
     */
    private class SqlJob
    {
        String sql;

        public SqlJob(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    /**
     * Worker thread.
     */
    private class Worker implements Runnable
    {
        private int threadNo;

        /**
         * Create a new worker thread object.
         *
         * @param threadNo the thread index of this thread
         */
        public Worker(int threadNo) {
            this.threadNo = threadNo;
        }

        public void run() {
            try {
                while (true) {
                    SqlJob job = null;
                    Connection c = null;
                    try {
                        job = queue.take();
                        c = getConnection();
                        c.setAutoCommit(true);
                        Statement s = c.createStatement();
                        s.execute(job.getSql());
                    } catch (SQLException e) {
                        if (job != null) {
                            LOG.error("Exception while executing " + job.getSql(), e);
                        }
                        reportException(e);
                    } finally {
                        if (job != null) {
                            jobIsDone(job);
                        }
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.error("Database background thread " + threadNo + " exited unexpectedly", t);
            } finally {
                workers.remove(this);
            }
        }
    }

    /**
     * An object to facilitate waiting for a set of operations to be finished.
     */
    private class Waiter
    {
        private Set<SqlJob> waitingOn;

        public Waiter(Set<SqlJob> waitingOn) {
            this.waitingOn = new HashSet<SqlJob>(waitingOn);
        }

        public synchronized void jobIsDone(SqlJob job) {
            waitingOn.remove(job);
            if (waitingOn.isEmpty()) {
                notifyAll();
            }
        }

        public synchronized void waitUntilFinished() {
            while (!waitingOn.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }
    }
}
