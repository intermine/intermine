package org.intermine.sql;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import java.math.BigDecimal;
import javax.sql.DataSource;

import org.intermine.util.ShutdownHook;
import org.intermine.util.Shutdownable;
import org.intermine.util.StringUtil;

import org.apache.log4j.Logger;
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

    // Store all the properties this Database was configured with
    protected Properties settings;

    protected Map createSituations = new HashMap();

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
                    + getClass().getClassLoader());
        } catch (Exception e) {
            LOG.info("Creating new invalid Database with ClassLoader "
                    + getClass().getClassLoader(), e);
        }
        ShutdownHook.registerObject(new WeakReference(this));
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
        if (datasource instanceof org.postgresql.jdbc3.Jdbc3PoolingDataSource) {
            LOG.info("Shutdown - Closing datasource for Database " + getURL() + "(" + toString()
                    + ") with ClassLoader " + getClass().getClassLoader());
            ((org.postgresql.jdbc3.Jdbc3PoolingDataSource) datasource).close();
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
     * @see Object#finalize
     */
    public void finalize() {
        if (datasource instanceof org.postgresql.jdbc3.Jdbc3PoolingDataSource) {
            LOG.info("Finalise - Closing datasource for Database " + getURL() + "(" + toString()
                    + ") with ClassLoader " + getClass().getClassLoader());
            ((org.postgresql.jdbc3.Jdbc3PoolingDataSource) datasource).close();
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
        return "jdbc:" + platform.toLowerCase() + "://"
            + (String) settings.get("datasource.serverName")
            + "/" + (String) settings.get("datasource.databaseName");
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

        Enumeration propsEnum = props.keys();
        while (propsEnum.hasMoreElements()) {
            String propertyName = (String) propsEnum.nextElement();
            Object propertyValue = props.get(propertyName);
            Field field = null;

            // Get the first part of the string - this is the attribute we are taking about
            String attribute = propertyName;
            String subAttribute = "";
            int index;
            if ((index = propertyName.indexOf(".")) != -1) {
                attribute = propertyName.substring(0, index);
                subAttribute = propertyName.substring(index + 1);
            }

            try {
                field = Database.class.getDeclaredField(attribute);
            } catch (Exception e) {
                // Ignore this property - no such field
                continue;
            }

            if (subAttribute.equals("class")) {
                // make a new instance of this class for this attribute
                Class clazz;
                Object obj;

                clazz = Class.forName(propertyValue.toString());
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
            } else if (subAttribute.equals("")) {
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
                    Class clazz = o.getClass();
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

    private static final Map POSTGRESQL_TYPE_STRING_MAP = new HashMap();

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
    public String getColumnTypeString(Class c) {
        return (String) POSTGRESQL_TYPE_STRING_MAP.get(c);
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        return "" + settings + " " + driver + " " + platform;
    }
}
