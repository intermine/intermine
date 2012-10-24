package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Given a class name and method name, just invokes a static method.
 * @author tom riley
 */
public class StaticMethodTask extends Task
{
    private String className;
    private String method;
    private String osAlias;

    /**
     * Set class name on which to call static method.
     * @param className class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Name of method to call.
     * @param method method to call
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Set the os alias.
     * @param alias os alias
     */
    public void setOsAlias(String alias) {
        osAlias = alias;
    }

    /**
     * @throws BuildException if a problem occurs
     * @see Task#execute
     */
    @Override
    public void execute() {
        try {
            Class<?> clazz = getClass().getClassLoader().loadClass(className);
            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);
            Method m = clazz.getMethod(method, new Class[]{ObjectStore.class});
            m.invoke(null, new Object[]{os});
        } catch (Exception err) {
            throw new BuildException(err);
        }
    }
}
