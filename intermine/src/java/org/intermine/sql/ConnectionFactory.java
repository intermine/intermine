package org.flymine.sql;

import java.sql.Connection;
import javax.sql.DataSource;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Properties;
import java.sql.SQLException;
import java.lang.reflect.Method;
import org.flymine.util.StringUtil;
import org.flymine.util.PropertiesUtil;


/**
 * Creates connections to a named database and stores them for future use
 *
 * @author Andrew Varley
 */

public class ConnectionFactory
{

    protected static Map dataSources = new HashMap();
    protected static Object lock = new Object();

    /**
     * Returns a connection to the named database
     *
     * @param instance the name of the database
     * @return a connection to that database
     * @throws SQLException if there is a problem with the underlying database
     * @throws ClassNotFoundException if the class that the instance uses cannot be found
     */
    public static Connection getConnection(String instance)
        throws SQLException, ClassNotFoundException {

        DataSource ds;

        // Only one thread to configure or test for a DataSource
        synchronized (lock) {
            // If we have this DataSource already configured
            if (dataSources.containsKey(instance)) {
                ds = (DataSource) dataSources.get(instance);
            } else {
                ds = configureDataSource(instance, PropertiesUtil.getPropertiesStartingWith(instance));
            }
        }
        return ds.getConnection();

    }

    /**
     * Configures a datasource from a Properties object
     *
     * @param instance the name of the DataSource
     * @param props the properties for configuring the DataSource
     * @return the configured DataSource
     * @throws ClassNotFoundException if the class given in the properties file cannot be found
     */
    protected static DataSource configureDataSource(String instance, Properties props)
        throws ClassNotFoundException {
        if (instance == null) {
            throw new NullPointerException("instance cannot be null");
        }


        String dataSourceKey = instance + ".dataSource";
        String dataSourceClassName = props.getProperty(dataSourceKey);

        if (dataSourceClassName == null) {
            throw new ClassNotFoundException("Cannot find a DataSource class for " + instance);
        }

        props.remove(dataSourceKey);

        Class dataSourceClass;
        DataSource ds;

        dataSourceClass = Class.forName(dataSourceClassName);
        try {
            ds = (DataSource) dataSourceClass.newInstance();
        } catch (Exception e) {
            throw new ClassNotFoundException("Cannot instantiate class "
                                             + dataSourceClass.getName() + " " + e.getMessage());
        }

        // Configure the DataSource
        Enumeration enum = props.keys();
        while (enum.hasMoreElements()) {
            String propertyName = (String) enum.nextElement();
            String propertyValue = (String) props.get(propertyName);
            String configName = propertyName.substring(propertyName.lastIndexOf(".") + 1);
            Method m = null;

            // Set this configuration parameter on the DataSource;
            try {
                // Strings first
                m = dataSourceClass.getMethod("set" + StringUtil.capitalise(configName),
                                          new Class[] {String.class});
                if (m != null) {
                    m.invoke(ds, new Object [] {propertyValue});
                }
                // now integers
            } catch (Exception e) {
                // Don't do anything - either the method not found or cannot be invoked
            }
            try {
                if (m == null) {
                    m = dataSourceClass.getMethod("set" + StringUtil.capitalise(configName),
                                                  new Class[] {int.class});
                    if (m != null) {
                        m.invoke(ds, new Object [] {Integer.valueOf(propertyValue)});
                    }
                }
            } catch (Exception e) {
                // Don't do anything - either the method not found or cannot be invoked
            }

        }

        dataSources.put(instance, ds);
        return ds;

    }

}
