package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.flymine.task.ClassPathTask;

/**
 * Uses an ObjectStoreWriter to insert data from an Ace dataase server
 *
 * @author Andrew Varley
 */
public class InsertAceDataTask extends ClassPathTask
{
    protected String integrationWriter;
    protected String user;
    protected String password;
    protected String host;
    protected int port;

    /**
     * Set the IntegrationWriter alias
     *
     * @param integrationWriter the name of the IntegrationWriter
     */
    public void setIntegrationWriter(String integrationWriter) {
        this.integrationWriter = integrationWriter;
    }

    /**
     * Set the username for the AceDB server
     *
     * @param user the name of the user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Set the password for the AceDB server
     *
     * @param password the password of the user
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set the host running the AceDB server
     *
     * @param host the name of the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Set the port for the AceDB server
     *
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.integrationWriter == null) {
            throw new BuildException("integrationWriter attribute is not set");
        }
        if (user == null) {
            throw new BuildException("user attribute is not set");
        }
        if (password == null) {
            throw new BuildException("password attribute is not set");
        }
        if (host == null) {
            throw new BuildException("host attribute is not set");
        }
        if (port <= 0) {
            throw new BuildException("port attribute is not set");
        }

        try {

            Object driver = loadClass("org.flymine.dataloader.AceDataLoaderDriver");

            // Have to execute the loadData method by reflection as
            // cannot cast to something that this class (which may use
            // a different ClassLoader) can see

            Method method = driver.getClass().getMethod("loadData", new Class[] {String.class,
                                                                                 String.class,
                                                                                 String.class,
                                                                                 String.class,
                                                                                 int.class });
            method.invoke(driver, new Object [] {integrationWriter,
                                                 user,
                                                 password,
                                                 host,
                                                 new Integer(port) });
        } catch (Exception e) {
            if (e.getMessage() != null) {
                log(e.getMessage(), Project.MSG_INFO);
            }
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

}
