package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;


/**
 * Task that sets the classpath, either via attribute or nested element.
 *
 * If a class is loaded by name (eg. using Class.forName(className)) then the
 * thread context ClassLoader is used. This class therefore sets that to be the
 * passed in classpath.
 *
 * If a class needs to be loaded normally (eg. MyClass clazz = new
 * MyClass()) then the class loader that was used to load the current
 * class is used to load the new class (and so on).
 *
 * Therefore, for the current need, we need to load a separate class
 * that is actually going to do the work by name, so that it uses the
 * ClassLoader using the classpath passed in. Any methods need to be
 * called by reflection, as it is impossible to cast to this class as
 * it uses a different class loader.
 *
 * @author Andrew Varley
 */
public class ClassPathTask extends Task
{

    protected Path classpath = null;

    private AntClassLoader loader = null;

    /**
     * Set the classpath for loading the driver.
     *
     * @param classpath the classpath
     */
    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Create the classpath for loading the driver.
     *
     * @return the classpath
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(project);
        }
        return this.classpath.createPath();
    }


    /**
     * Set the classpath for loading the driver using the classpath reference.
     *
     * @param r the classpath reference
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }


    /**
     * Set the ClassLoader to be used by this Task. Used only in testing.
     *
     * @param loader the ClassLoader to use
     */
    protected void setClassLoader(AntClassLoader loader) {
        this.loader = loader;
    }


    /**
     * Process the classpath. This involves setting the thread context
     * class loader and then loading the actual class by name, so that
     * its ClassLoader (and everything that it then loads) is the one
     * using this classpath.
     *
     * @param actualClass the class that will actually do the work
     * @return an instance of that class
     * @throws BuildException if any error occurs
     */
    public Object loadClass(String actualClass) throws BuildException {

        try {
            Class clazz;
//             if (classpath != null) {
//                 loader = new AntClassLoader(project, classpath);
//             }
//             if (loader != null) {
//                 clazz = loader.loadClass(actualClass);
//                 loader.setThreadContextLoader();
//             } else {
            //Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                clazz = Class.forName(actualClass);
                //}
            return clazz.newInstance();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}
